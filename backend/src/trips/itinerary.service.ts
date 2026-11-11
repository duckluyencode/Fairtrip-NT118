import { HttpStatus, Injectable } from '@nestjs/common';
import { AppError } from '../common/errors/app-error.js';
import { ErrorCode } from '../common/errors/error-code.js';
import { addDays, daysInclusive, toDateOnly, toHhmm } from '../common/time.js';
import { jainIndex } from '../planner/scoring.js';
import { PrismaService } from '../prisma/prisma.service.js';
import type {
  ItineraryDayResponse,
  ItineraryResponse,
  ItineraryWarning,
} from './dto/trip.response.js';
import { LodgingService } from './lodging.service.js';
import { TripAccess } from './trip-access.js';
import {
  hoursOnDay,
  TripPlacesService,
  type TripPlace,
} from './trip-places.js';

/** What the budget covers, shown with every itinerary (transport is not included yet). */
export const BUDGET_INCLUDES = ['TICKETS', 'FOOD', 'LODGING'];

/** True when a visit from start to end is outside the place's hours on that day. */
function outsideHours(
  place: TripPlace | undefined,
  startDate: Date,
  day: number,
  start: number,
  end: number,
): boolean {
  if (!place) return false;
  const hours = hoursOnDay(place, startDate, day);
  if (!hours) return false;
  if ('closed' in hours) return true;
  if (hours.allDay) return false;
  const close = hours.close <= hours.open ? hours.close + 1440 : hours.close;
  return start < hours.open || end > close;
}

@Injectable()
export class ItineraryService {
  constructor(
    private readonly prisma: PrismaService,
    private readonly access: TripAccess,
    private readonly tripPlaces: TripPlacesService,
    private readonly lodging: LodgingService,
  ) {}

  /**
   * The itinerary as every member sees it: each stop with its time, price and source,
   * what you pay there, your running total (room included) and what is left of your
   * budget; what each member spends; and warnings for anything that breaks a rule.
   */
  async view(userId: number, tripId: number): Promise<ItineraryResponse> {
    const { trip, me } = await this.access.load(userId, tripId);
    if (trip.status !== 'PLANNED') {
      throw new AppError(
        HttpStatus.CONFLICT,
        ErrorCode.ItineraryNotReady,
        'This trip has not been planned yet',
      );
    }
    const stops = await this.prisma.tripStop.findMany({
      where: { tripId },
      include: { participants: true },
      orderBy: [{ day: 'asc' }, { position: 'asc' }],
    });
    const [places, lodging] = await Promise.all([
      this.tripPlaces.resolve(stops.map((s) => s.placeId)),
      this.lodging.view(trip, me),
    ]);
    const names = new Map(trip.members.map((m) => [m.id, m.user.fullName]));
    const warnings: ItineraryWarning[] = [];
    const days = daysInclusive(trip.startDate, trip.endDate);

    let running = me.lodgingShare;
    const dayViews: ItineraryDayResponse[] = Array.from(
      { length: days },
      (_, index) => ({
        day: index + 1,
        date: toDateOnly(addDays(trip.startDate, index)),
        stops: [],
      }),
    );
    for (const stop of stops) {
      const place = places.get(stop.placeId);
      const mine = stop.participants.find((p) => p.memberId === me.id);
      running += mine?.cost ?? 0;
      const end = stop.startMinute + stop.visitMinutes;
      if (
        outsideHours(place, trip.startDate, stop.day, stop.startMinute, end)
      ) {
        warnings.push({
          code: 'PLACE_CLOSED',
          day: stop.day,
          placeId: stop.placeId,
        });
      }
      dayViews[stop.day - 1]?.stops.push({
        stopId: stop.id,
        position: stop.position,
        time: toHhmm(stop.startMinute),
        travelMinutes: stop.travelMinutes,
        visitMinutes: stop.visitMinutes,
        placeId: stop.placeId,
        name: place?.name ?? null,
        area: place?.area ?? null,
        category: place?.category ?? null,
        location: place?.location ?? null,
        attribution: place?.attribution ?? null,
        price: {
          perPerson: stop.costPerPerson,
          source: stop.priceSource,
          provider: stop.priceProvider,
          isEstimate: stop.priceSource !== 'PARTNER_API',
        },
        myCost: mine ? mine.cost : null,
        participants: stop.participants.map((p) => ({
          memberId: p.memberId,
          fullName: names.get(p.memberId) ?? '',
        })),
        runningTotal: running,
        remaining: me.budget - running,
      });
    }
    for (const day of dayViews) {
      const last = stops.filter((s) => s.day === day.day).at(-1);
      if (last && last.startMinute + last.visitMinutes > trip.dayEndMinute) {
        warnings.push({ code: 'DAY_TOO_LONG', day: day.day });
      }
    }

    const spent = new Map(
      trip.members.map((m) => [
        m.id,
        m.lodgingShare +
          stops.reduce(
            (sum, s) =>
              sum +
              (s.participants.find((p) => p.memberId === m.id)?.cost ?? 0),
            0,
          ),
      ]),
    );
    const myTotal = spent.get(me.id) ?? 0;
    if (myTotal > me.budget) {
      warnings.push({ code: 'OVER_BUDGET' });
    }
    // Others' budgets stay private: only that someone is over, not who or by how much.
    if (
      trip.members.some(
        (m) => m.id !== me.id && (spent.get(m.id) ?? 0) > m.budget,
      )
    ) {
      warnings.push({ code: 'MEMBER_OVER_BUDGET' });
    }
    const scores = trip.members.map((m) => m.satisfaction);
    return {
      tripId: trip.id,
      status: trip.status,
      budgetIncludes: BUDGET_INCLUDES,
      lodging,
      days: dayViews,
      me: {
        budget: me.budget,
        lodgingShare: me.lodgingShare,
        spentOnPlaces: myTotal - me.lodgingShare,
        total: myTotal,
        remaining: me.budget - myTotal,
      },
      members: trip.members.map((m) => ({
        memberId: m.id,
        fullName: m.user.fullName,
        spent: spent.get(m.id) ?? 0,
        satisfaction: m.satisfaction,
      })),
      fairness: scores.every((s) => s !== null)
        ? Math.round(jainIndex(scores.map((s) => s / 100)) * 1000) / 1000
        : null,
      warnings,
    };
  }
}
