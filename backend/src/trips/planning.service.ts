import { HttpStatus, Injectable } from '@nestjs/common';
import { AppError } from '../common/errors/app-error.js';
import { ErrorCode } from '../common/errors/error-code.js';
import { daysInclusive } from '../common/time.js';
import type { LatLng } from '../places/geo.js';
import { RoutesClient } from '../places/google/routes.client.js';
import { placeNotFound } from '../places/places.service.js';
import { planTrip } from '../planner/plan.js';
import {
  simulateDay,
  type DaySettings,
  type SimulatedDay,
} from '../planner/schedule.js';
import { satisfaction } from '../planner/scoring.js';
import type { PlannerMember, PlannerPlace } from '../planner/types.js';
import { PrismaService } from '../prisma/prisma.service.js';
import type { UpdateItineraryDto } from './dto/trip.dto.js';
import { roomsNeeded } from './lodging-shares.js';
import {
  LodgingService,
  lodgingOverBudget,
  lodgingShares,
} from './lodging.service.js';
import { TripAccess, type LoadedTrip } from './trip-access.js';
import {
  hoursForTrip,
  TripPlacesService,
  type TripPlace,
} from './trip-places.js';

/** A stop ready to be saved: when, and what each participant pays. */
interface StopDraft {
  place: TripPlace;
  day: number;
  position: number;
  startMinute: number;
  travelMinutes: number;
  /** Member id -> VND, for the members who go. */
  costByMember: Map<number, number>;
}

function invalidItinerary(message: string): AppError {
  return new AppError(
    HttpStatus.BAD_REQUEST,
    ErrorCode.InvalidItinerary,
    message,
  );
}

function plannerPlace(
  place: TripPlace & { location: LatLng },
  trip: LoadedTrip,
  days: number,
  inPlan: Set<number>,
  votes: number[],
  meals: boolean,
): PlannerPlace {
  return {
    id: place.id,
    lat: place.location.lat,
    lng: place.location.lng,
    visitMinutes: place.visitMinutes,
    // The top of the price range, so that nobody's budget can be overrun.
    cost: place.price.max,
    coveredBy:
      place.parentId !== null && inPlan.has(place.parentId)
        ? place.parentId
        : null,
    interests: place.interests,
    rating: place.rating,
    ...hoursForTrip(place.weeklyHours, trip.startDate, days),
    votes,
    // Self-designed itineraries keep the order and times the owner chose.
    meal: meals && place.meal,
  };
}

function plannerMembers(
  trip: LoadedTrip,
  shares: Map<number, number>,
): PlannerMember[] {
  return trip.members.map((m) => ({
    id: m.id,
    available: m.budget - (shares.get(m.id) ?? 0),
    interests: m.interests.map((i) => i.interest),
  }));
}

/**
 * Builds and saves itineraries: the automatic plan (Greedy -> Local search -> Fairness
 * in src/planner) and itineraries the owner designs. Travel times start as straight-line
 * estimates; when Google Routes is configured, each saved day is timed again with real
 * driving times (one request per day), dropping the last stops of a day that no longer
 * fits its time window.
 */
@Injectable()
export class PlanningService {
  constructor(
    private readonly prisma: PrismaService,
    private readonly access: TripAccess,
    private readonly tripPlaces: TripPlacesService,
    private readonly lodging: LodgingService,
    private readonly routes: RoutesClient,
  ) {}

  async plan(userId: number, tripId: number): Promise<void> {
    const { trip } = await this.access.loadAsOwner(userId, tripId);
    const days = daysInclusive(trip.startDate, trip.endDate);
    const { shares, base } = await this.lodgingFor(trip);

    // Places picked or voted for; without any, popular places of the city.
    const options = await this.prisma.voteOption.findMany({
      where: { tripId },
      include: { votes: { select: { memberId: true } } },
    });
    const candidates =
      options.length > 0
        ? [
            ...(
              await this.tripPlaces.resolve(options.map((o) => o.placeId))
            ).values(),
          ]
        : await this.tripPlaces.discover(trip.city);
    const located = candidates.filter(
      (p): p is TripPlace & { location: LatLng } => p.location !== null,
    );
    if (located.length === 0) {
      throw new AppError(
        HttpStatus.UNPROCESSABLE_ENTITY,
        ErrorCode.NoPlacesToPlan,
        'No place could be found to plan this trip from',
      );
    }
    const inPlan = new Set(located.map((p) => p.id));
    const votesFor = (placeId: number) =>
      options
        .find((o) => o.placeId === placeId)
        ?.votes.map((v) => v.memberId) ?? [];
    const places = located.map((p) =>
      plannerPlace(p, trip, days, inPlan, votesFor(p.id), true),
    );
    const settings: DaySettings = {
      dayStartMinute: trip.dayStartMinute,
      dayEndMinute: trip.dayEndMinute,
      gapMinutes: trip.gapMinutes,
      base,
    };
    const members = plannerMembers(trip, shares);
    const result = planTrip({ members, places, days, ...settings });

    // Days in visiting order, then timed again with real driving times.
    const byId = new Map(places.map((p) => [p.id, p]));
    const order: PlannerPlace[][] = Array.from({ length: days }, (_, d) =>
      result.stops
        .filter((s) => s.day === d + 1)
        .sort((a, b) => a.position - b.position)
        .map((s) => byId.get(s.placeId)!),
    );
    const timed = await this.fitDays(order, settings);
    const kept = new Set(
      timed.flatMap((day) => day.visits.map((v) => v.place.id)),
    );
    const drafts: StopDraft[] = timed.flatMap((day, index) =>
      day.visits.map((visit, position) => {
        const stop = result.stops.find((s) => s.placeId === visit.place.id)!;
        return {
          place: located.find((p) => p.id === visit.place.id)!,
          day: index + 1,
          position: position + 1,
          startMinute: visit.startMinute,
          travelMinutes: visit.travelMinutes,
          costByMember: new Map(
            stop.participants.map((id) => [id, stop.costByMember[id] ?? 0]),
          ),
        };
      }),
    );
    const visited = (memberId: number) =>
      places.filter(
        (p) =>
          kept.has(p.id) &&
          drafts.some(
            (d) => d.place.id === p.id && d.costByMember.has(memberId),
          ),
      );
    const scores = new Map(
      members.map((m) => [
        m.id,
        satisfaction(m, visited(m.id), places, kept.size),
      ]),
    );
    await this.save(trip, drafts, shares, scores);
  }

  /**
   * "Tự thiết kế lộ trình": the owner gives the places of each day in order; the whole
   * group goes everywhere. Unlike the automatic plan, nothing is refused for being late,
   * closed or over budget: the itinerary shows warnings instead.
   */
  async design(
    userId: number,
    tripId: number,
    dto: UpdateItineraryDto,
  ): Promise<void> {
    const { trip } = await this.access.loadAsOwner(userId, tripId);
    const days = daysInclusive(trip.startDate, trip.endDate);
    const ids = dto.days.flatMap((d) => d.placeIds);
    if (dto.days.some((d) => d.day > days)) {
      throw invalidItinerary(`This trip has ${days} days`);
    }
    if (new Set(dto.days.map((d) => d.day)).size !== dto.days.length) {
      throw invalidItinerary('Each day can be given only once');
    }
    if (new Set(ids).size !== ids.length) {
      throw invalidItinerary('Each place can be visited only once');
    }
    const resolved = await this.tripPlaces.resolve(ids);
    const located = new Map<number, TripPlace & { location: LatLng }>();
    for (const id of ids) {
      const place = resolved.get(id);
      if (!place?.location) {
        throw placeNotFound();
      }
      located.set(id, place as TripPlace & { location: LatLng });
    }
    const { shares, base } = await this.lodgingFor(trip, {
      allowOverBudget: true,
    });
    const inPlan = new Set(ids);
    const places = new Map(
      [...located.values()].map((p) => [
        p.id,
        plannerPlace(p, trip, days, inPlan, [], false),
      ]),
    );
    const settings: DaySettings = {
      dayStartMinute: trip.dayStartMinute,
      dayEndMinute: trip.dayEndMinute,
      gapMinutes: trip.gapMinutes,
      base,
    };
    const drafts: StopDraft[] = [];
    for (const { day, placeIds } of dto.days) {
      const timed = await this.timeDay(
        placeIds.map((id) => places.get(id)!),
        settings,
      );
      timed.visits.forEach((visit, index) => {
        // Everyone goes; a place inside a ticketed area on the itinerary is free.
        const cost = visit.place.coveredBy !== null ? 0 : visit.place.cost;
        drafts.push({
          place: located.get(visit.place.id)!,
          day,
          position: index + 1,
          startMinute: visit.startMinute,
          travelMinutes: visit.travelMinutes,
          costByMember: new Map(trip.members.map((m) => [m.id, cost])),
        });
      });
    }
    const members = plannerMembers(trip, shares);
    const all = [...places.values()];
    const scores = new Map(
      members.map((m) => [m.id, satisfaction(m, all, all, all.length)]),
    );
    await this.save(trip, drafts, shares, scores);
  }

  /** Room shares and the hotel to start each day from; checks nobody overpays the room. */
  private async lodgingFor(
    trip: LoadedTrip,
    { allowOverBudget = false } = {},
  ): Promise<{ shares: Map<number, number>; base: LatLng | null }> {
    if (trip.lodgingMode !== 'HOTEL') {
      return { shares: new Map(), base: null };
    }
    if (!trip.lodging) {
      throw new AppError(
        HttpStatus.CONFLICT,
        ErrorCode.LodgingNotChosen,
        'Choose a hotel or self-arranged lodging first',
      );
    }
    const shares = lodgingShares(trip);
    // Someone may have joined since the hotel was chosen, changing the shares.
    if (
      !allowOverBudget &&
      trip.members.some((m) => (shares.get(m.id) ?? 0) > m.budget)
    ) {
      throw lodgingOverBudget();
    }
    return { shares, base: await this.lodging.hotelLocation(trip) };
  }

  /**
   * Times each day with real driving times, removing the last visit of a day while the
   * day ends too late or reaches a place after it closes. A removed ticketed area also
   * removes the places inside it, since their price assumed its ticket.
   */
  private async fitDays(
    days: PlannerPlace[][],
    settings: DaySettings,
  ): Promise<SimulatedDay[]> {
    const order = days.map((d) => [...d]);
    const timed: (SimulatedDay | null)[] = order.map(() => null);
    for (;;) {
      const dropped: PlannerPlace[] = [];
      for (const [index, day] of order.entries()) {
        if (timed[index]) continue;
        let current = await this.timeDay(day, settings);
        while (current.endsLate || current.closedPlaceIds.length > 0) {
          const last = day.pop();
          if (!last) break;
          dropped.push(last);
          current = await this.timeDay(day, settings);
        }
        timed[index] = current;
      }
      const covered = new Set(dropped.map((p) => p.id));
      let changed = false;
      for (const [index, day] of order.entries()) {
        const before = day.length;
        day.splice(
          0,
          day.length,
          ...day.filter(
            (p) => p.coveredBy === null || !covered.has(p.coveredBy),
          ),
        );
        if (day.length !== before) {
          timed[index] = null;
          changed = true;
        }
      }
      if (!changed) {
        return timed.map((day) => day!);
      }
    }
  }

  /** One day timed with Google Routes when available, else with the estimate. */
  private async timeDay(
    order: PlannerPlace[],
    settings: DaySettings,
  ): Promise<SimulatedDay> {
    if (!this.routes.enabled || order.length === 0) {
      return simulateDay(order, settings);
    }
    const stops = order.map((p) => ({ lat: p.lat, lng: p.lng }));
    const points = settings.base
      ? [settings.base, ...stops, settings.base]
      : stops;
    const legs = await this.routes.legMinutes(points);
    if (!legs) {
      return simulateDay(order, settings);
    }
    // Without a hotel the day starts at the first place: no leg leads to it.
    return simulateDay(order, settings, settings.base ? legs : [0, ...legs]);
  }

  /** Replaces the trip's itinerary, room shares and satisfaction; the trip is PLANNED. */
  private async save(
    trip: LoadedTrip,
    drafts: StopDraft[],
    shares: Map<number, number>,
    scores: Map<number, number>,
  ): Promise<void> {
    await this.prisma.$transaction(async (tx) => {
      await this.access.resetPlan(tx, trip.id);
      for (const draft of drafts) {
        await tx.tripStop.create({
          data: {
            tripId: trip.id,
            placeId: draft.place.id,
            day: draft.day,
            position: draft.position,
            startMinute: draft.startMinute,
            travelMinutes: draft.travelMinutes,
            visitMinutes: draft.place.visitMinutes,
            costPerPerson: draft.place.price.max,
            priceSource: draft.place.price.source,
            priceProvider: draft.place.price.provider,
            participants: {
              create: [...draft.costByMember].map(([memberId, cost]) => ({
                memberId,
                cost,
              })),
            },
          },
        });
      }
      for (const member of trip.members) {
        await tx.tripMember.update({
          where: { id: member.id },
          data: {
            lodgingShare: shares.get(member.id) ?? 0,
            satisfaction: Math.round((scores.get(member.id) ?? 0) * 100),
          },
        });
      }
      if (trip.lodging && trip.lodgingMode === 'HOTEL') {
        await tx.tripLodging.update({
          where: { tripId: trip.id },
          data: {
            rooms: roomsNeeded(trip.members.length, trip.lodging.guestsPerRoom),
          },
        });
      }
      await tx.trip.update({
        where: { id: trip.id },
        data: { status: 'PLANNED', plannedAt: new Date() },
      });
    });
  }
}
