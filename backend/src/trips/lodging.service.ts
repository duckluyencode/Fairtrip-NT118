import { HttpStatus, Injectable, Logger } from '@nestjs/common';
import { AppError } from '../common/errors/app-error.js';
import { ErrorCode } from '../common/errors/error-code.js';
import { daysInclusive, toDateOnly } from '../common/time.js';
import { distanceKm, type LatLng } from '../places/geo.js';
import {
  GooglePlacesClient,
  type GooglePlace,
} from '../places/google/google-places.client.js';
import { PlacesService } from '../places/places.service.js';
import { PrismaService } from '../prisma/prisma.service.js';
import type { ChooseLodgingDto } from './dto/trip.dto.js';
import type {
  LodgingOptionResponse,
  LodgingResponse,
} from './dto/trip.response.js';
import {
  GUESTS_PER_ROOM,
  lodgingPrice,
  roomShares,
  roomsNeeded,
  type NightlyPrice,
} from './lodging-shares.js';
import {
  TripAccess,
  type LoadedMember,
  type LoadedTrip,
} from './trip-access.js';
import { TripPlacesService } from './trip-places.js';

const GOOGLE_ATTRIBUTION = 'Google Maps';
const HOTEL_QUERY = 'khách sạn';
const HOTEL_RESULTS = 20;
const NEAR_RADIUS_METERS = 15_000;

/** Nights between check-in on the first day and check-out on the last. */
export function nightsOf(trip: Pick<LoadedTrip, 'startDate' | 'endDate'>) {
  return daysInclusive(trip.startDate, trip.endDate) - 1;
}

/** Each member's part of the chosen room cost for the whole stay, by member id. */
export function lodgingShares(trip: LoadedTrip): Map<number, number> {
  if (trip.lodgingMode !== 'HOTEL' || !trip.lodging) {
    return new Map();
  }
  return roomShares(
    trip.members.map((m) => m.id),
    trip.lodging.guestsPerRoom,
    trip.lodging.pricePerNight,
    nightsOf(trip),
  );
}

function isLodging(place: GooglePlace): boolean {
  return place.types.includes('lodging') || place.types.includes('hotel');
}

function locationOf(place: GooglePlace): LatLng | null {
  return place.lat !== null && place.lng !== null
    ? { lat: place.lat, lng: place.lng }
    : null;
}

function centroid(points: LatLng[]): LatLng | null {
  if (points.length === 0) return null;
  return {
    lat: points.reduce((s, p) => s + p.lat, 0) / points.length,
    lng: points.reduce((s, p) => s + p.lng, 0) / points.length,
  };
}

export interface Candidate {
  placeId: number;
  place: GooglePlace;
  price: NightlyPrice;
  distanceKm: number | null;
  shares: Map<number, number>;
}

/**
 * Hotels for a trip ("Lưu trú"). Hotels are real places from Google Places (type
 * lodging), stored by place ID only; their nightly price is estimated from Google's
 * price level until the Agoda partner API is available, and copied into the trip when
 * chosen. The room cost is taken out of each member's budget before places are planned.
 */
@Injectable()
export class LodgingService {
  private readonly logger = new Logger(LodgingService.name);

  constructor(
    private readonly prisma: PrismaService,
    private readonly access: TripAccess,
    private readonly google: GooglePlacesClient,
    private readonly places: PlacesService,
    private readonly tripPlaces: TripPlacesService,
  ) {}

  /**
   * Up to three hotels every member can afford: the cheapest, the best balance of rating,
   * price and distance, and the nearest to the trip's places. Empty for a one-day trip,
   * without Google Places, or when no hotel fits everyone's budget ("Tự lo chỗ ở").
   */
  async options(
    userId: number,
    tripId: number,
    guestsPerRoom = GUESTS_PER_ROOM,
  ): Promise<LodgingOptionResponse[]> {
    const { trip, me } = await this.access.load(userId, tripId);
    const nights = nightsOf(trip);
    if (nights < 1 || !this.google.enabled) {
      return [];
    }
    const center = await this.centerOf(trip);
    const found = await this.google.search({
      query: `${HOTEL_QUERY} ${trip.city}`,
      pageSize: HOTEL_RESULTS,
      includedType: 'lodging',
      near: center
        ? { ...center, radiusMeters: NEAR_RADIUS_METERS }
        : undefined,
      withDetails: true,
    });
    const hotels = found.places.filter(isLodging);
    const ids = await this.places.storeGoogleIds(hotels.map((h) => h.id));
    const middle =
      center ?? centroid(hotels.map(locationOf).filter((p) => p !== null));
    const memberIds = trip.members.map((m) => m.id);
    const candidates: Candidate[] = hotels.flatMap((place) => {
      const placeId = ids.get(place.id);
      if (!placeId) return [];
      const price = lodgingPrice(place);
      const shares = roomShares(
        memberIds,
        guestsPerRoom,
        price.pricePerNight,
        nights,
      );
      const location = locationOf(place);
      return [
        {
          placeId,
          place,
          price,
          distanceKm: middle && location ? distanceKm(middle, location) : null,
          shares,
        },
      ];
    });
    // Fairness: a hotel is only suggested when nobody's share is over their budget.
    const affordable = candidates.filter((c) =>
      trip.members.every((m) => (c.shares.get(m.id) ?? 0) <= m.budget),
    );
    return pickThree(affordable).map(({ tag, candidate }) =>
      this.toOption(tag, candidate, trip, me, nights, guestsPerRoom),
    );
  }

  /** "Tự lo chỗ ở", or one hotel for the whole group; the trip must be planned again. */
  async choose(
    userId: number,
    tripId: number,
    dto: ChooseLodgingDto,
  ): Promise<void> {
    const { trip } = await this.access.loadAsOwner(userId, tripId);
    if (dto.mode === 'SELF') {
      await this.prisma.$transaction(async (tx) => {
        await tx.tripLodging.deleteMany({ where: { tripId } });
        await tx.trip.update({
          where: { id: tripId },
          data: { lodgingMode: 'SELF' },
        });
        await this.access.resetPlan(tx, tripId);
      });
      return;
    }
    const nights = nightsOf(trip);
    if (nights < 1) {
      throw new AppError(
        HttpStatus.BAD_REQUEST,
        ErrorCode.LodgingNotNeeded,
        'A one-day trip has no night to book',
      );
    }
    const hotel = await this.readHotel(dto.placeId ?? 0);
    const price = lodgingPrice(hotel);
    const guestsPerRoom = dto.guestsPerRoom ?? GUESTS_PER_ROOM;
    const shares = roomShares(
      trip.members.map((m) => m.id),
      guestsPerRoom,
      price.pricePerNight,
      nights,
    );
    if (trip.members.some((m) => (shares.get(m.id) ?? 0) > m.budget)) {
      throw lodgingOverBudget();
    }
    const data = {
      placeId: dto.placeId ?? 0,
      checkIn: trip.startDate,
      checkOut: trip.endDate,
      rooms: roomsNeeded(trip.members.length, guestsPerRoom),
      guestsPerRoom,
      pricePerNight: price.pricePerNight,
      priceSource: price.source,
      priceProvider: price.provider,
      pricedAt: new Date(),
    };
    await this.prisma.$transaction(async (tx) => {
      await tx.tripLodging.upsert({
        where: { tripId },
        create: { tripId, ...data },
        update: data,
      });
      await tx.trip.update({
        where: { id: tripId },
        data: { lodgingMode: 'HOTEL' },
      });
      await this.access.resetPlan(tx, tripId);
    });
  }

  /** The chosen hotel as shown in the trip, with the hotel's name read live from Google. */
  async view(
    trip: LoadedTrip,
    me: LoadedMember,
  ): Promise<LodgingResponse | null> {
    const lodging = trip.lodging;
    if (!lodging || trip.lodgingMode !== 'HOTEL') {
      return null;
    }
    const place = await this.liveHotel(lodging.place.googlePlaceId);
    return {
      placeId: lodging.placeId,
      name: place?.name ?? null,
      address: place?.address ?? null,
      location: place ? locationOf(place) : null,
      checkIn: toDateOnly(lodging.checkIn),
      checkOut: toDateOnly(lodging.checkOut),
      nights: nightsOf(trip),
      rooms: roomsNeeded(trip.members.length, lodging.guestsPerRoom),
      guestsPerRoom: lodging.guestsPerRoom,
      pricePerNight: lodging.pricePerNight,
      priceSource: lodging.priceSource,
      provider: lodging.priceProvider,
      isEstimate: lodging.priceSource !== 'PARTNER_API',
      myShare: lodgingShares(trip).get(me.id) ?? 0,
      attribution: GOOGLE_ATTRIBUTION,
    };
  }

  /** Where the chosen hotel is, for planning each day from it. */
  async hotelLocation(trip: LoadedTrip): Promise<LatLng | null> {
    if (!trip.lodging) return null;
    const place = await this.liveHotel(trip.lodging.place.googlePlaceId);
    return place ? locationOf(place) : null;
  }

  private async liveHotel(
    googlePlaceId: string | null,
  ): Promise<GooglePlace | null> {
    if (!googlePlaceId || !this.google.enabled) return null;
    try {
      return await this.google.getPlace(googlePlaceId, undefined, 'trip');
    } catch (error) {
      this.logger.warn(`Hotel ${googlePlaceId} unavailable: ${String(error)}`);
      return null;
    }
  }

  private async readHotel(placeId: number): Promise<GooglePlace> {
    const row = await this.prisma.place.findUnique({ where: { id: placeId } });
    const place =
      row?.source === 'GOOGLE' && row.googlePlaceId && this.google.enabled
        ? await this.google.getPlace(row.googlePlaceId, undefined, 'trip')
        : null;
    if (!place || !isLodging(place)) {
      throw new AppError(
        HttpStatus.NOT_FOUND,
        ErrorCode.HotelNotFound,
        'No such hotel',
      );
    }
    return place;
  }

  /** Middle of the planned stops, else of the picked places; null when there are none. */
  private async centerOf(trip: LoadedTrip): Promise<LatLng | null> {
    const stops = await this.prisma.tripStop.findMany({
      where: { tripId: trip.id },
      select: { placeId: true },
    });
    const ids =
      stops.length > 0
        ? stops.map((s) => s.placeId)
        : (
            await this.prisma.voteOption.findMany({
              where: { tripId: trip.id },
              select: { placeId: true },
            })
          ).map((o) => o.placeId);
    if (ids.length === 0) return null;
    const places = await this.tripPlaces.resolve(ids);
    return centroid(
      [...places.values()].map((p) => p.location).filter((p) => p !== null),
    );
  }

  private toOption(
    tag: LodgingOptionResponse['tag'],
    c: Candidate,
    trip: LoadedTrip,
    me: LoadedMember,
    nights: number,
    guestsPerRoom: number,
  ): LodgingOptionResponse {
    const myShare = c.shares.get(me.id) ?? 0;
    return {
      tag,
      placeId: c.placeId,
      name: c.place.name,
      address: c.place.address,
      location: locationOf(c.place),
      rating: c.place.rating,
      distanceKm: c.distanceKm,
      pricePerNight: c.price.pricePerNight,
      priceSource: c.price.source,
      provider: c.price.provider,
      isEstimate: c.price.source !== 'PARTNER_API',
      nights,
      rooms: roomsNeeded(trip.members.length, guestsPerRoom),
      guestsPerRoom,
      myShare,
      leftForPlaces: me.budget - myShare,
      attribution: GOOGLE_ATTRIBUTION,
    };
  }
}

export function lodgingOverBudget(): AppError {
  return new AppError(
    HttpStatus.CONFLICT,
    ErrorCode.LodgingOverBudget,
    "The room costs more than a member's budget; choose a cheaper hotel or self-arranged lodging",
  );
}

/**
 * Picks three different hotels: the cheapest; then the best balance (rating 50%,
 * cheapness 30%, closeness 20%, each scaled to 0–1 across the candidates); then the
 * nearest of the rest. Fewer when there are fewer candidates.
 */
export function pickThree(
  candidates: Candidate[],
): { tag: LodgingOptionResponse['tag']; candidate: Candidate }[] {
  const left = [...candidates];
  const picked: { tag: LodgingOptionResponse['tag']; candidate: Candidate }[] =
    [];
  const take = (
    tag: LodgingOptionResponse['tag'],
    better: (a: Candidate, b: Candidate) => number,
  ) => {
    if (left.length === 0) return;
    left.sort((a, b) => better(a, b) || a.placeId - b.placeId);
    const [best] = left.splice(0, 1);
    picked.push({ tag, candidate: best });
  };
  const maxPrice = Math.max(1, ...candidates.map((c) => c.price.pricePerNight));
  const maxDistance = Math.max(
    0.1,
    ...candidates.map((c) => c.distanceKm ?? 0),
  );
  const balance = (c: Candidate) =>
    0.5 * ((c.place.rating ?? 3) / 5) +
    0.3 * (1 - c.price.pricePerNight / maxPrice) +
    0.2 * (1 - (c.distanceKm ?? maxDistance) / maxDistance);
  const distance = (c: Candidate) => c.distanceKm ?? Number.POSITIVE_INFINITY;

  take(
    'CHEAPEST',
    (a, b) =>
      a.price.pricePerNight - b.price.pricePerNight ||
      (b.place.rating ?? 0) - (a.place.rating ?? 0),
  );
  take('BALANCED', (a, b) => balance(b) - balance(a));
  take('NEAREST', (a, b) => distance(a) - distance(b));
  return picked;
}
