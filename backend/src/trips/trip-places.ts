import { Injectable, Logger } from '@nestjs/common';
import { addDays } from '../common/time.js';
import type { Prisma } from '../generated/prisma/client.js';
import type {
  Interest,
  PlaceCategory,
  PlaceSource,
} from '../generated/prisma/enums.js';
import type { LatLng } from '../places/geo.js';
import {
  DEFAULT_VISIT_MINUTES,
  googleCategories,
  googleInterests,
  isMealPlace,
  visitMinutesFor,
} from '../places/google/google-categories.js';
import {
  GooglePlacesClient,
  type DayHours,
  type GooglePlace,
} from '../places/google/google-places.client.js';
import { PlacesService } from '../places/places.service.js';
import {
  catalogPrice,
  googlePrice,
  type PriceInfo,
} from '../places/pricing.js';
import { normalizeText } from '../places/text.js';
import { PrismaService } from '../prisma/prisma.service.js';

/** A stored place with everything trips need, whether it is from the catalog or Google. */
export interface TripPlace {
  id: number;
  source: PlaceSource;
  name: string;
  area: string | null;
  category: PlaceCategory;
  imageUrl: string | null;
  location: LatLng | null;
  rating: number | null;
  visitMinutes: number;
  price: PriceInfo;
  interests: Interest[];
  /** Restaurants: planned at lunch or dinner time only. */
  meal: boolean;
  /** Catalog places inside a ticketed area (Cầu Vàng in Bà Nà Hills). */
  parentId: number | null;
  /** Hours for each weekday, 0 = Sunday; null when unknown (treated as always open). */
  weeklyHours: (DayHours | null)[] | null;
  types: string[];
  /** "Google Maps" for Google data, which must be credited. */
  attribution: string | null;
}

const GOOGLE_ATTRIBUTION = 'Google Maps';
// Two Text Search calls per planning: sights and places to eat. Results are read with
// prices and opening hours (PLANNING field mask), so no detail call per place is needed.
const SIGHTS_QUERY = 'điểm tham quan nổi tiếng';
const FOOD_QUERY = 'quán ăn ngon nổi tiếng';
const SIGHTS_COUNT = 20;
const FOOD_COUNT = 10;
// Hotels and the like are planned as lodging, not as places to visit.
const NOT_A_SIGHT = new Set(['lodging', 'hotel', 'motel', 'hostel']);

const catalogInclude = {
  categories: true,
  interests: true,
  parent: true,
} satisfies Prisma.PlaceInclude;

/** Opening hours the planner understands, for a trip starting on `startDate`. */
export function hoursForTrip(
  weeklyHours: TripPlace['weeklyHours'],
  startDate: Date,
  days: number,
): {
  openMinute: number | null;
  closeMinute: number | null;
  closedDays: number[];
} {
  const closedDays: number[] = [];
  let window: { open: number; close: number } | null = null;
  if (weeklyHours) {
    for (let day = 1; day <= days; day++) {
      const hours = weeklyHours[addDays(startDate, day - 1).getUTCDay()];
      if (!hours) continue;
      if ('closed' in hours) {
        closedDays.push(day);
      } else if (!hours.allDay && !window) {
        // The planner keeps one window per place: the first open day's hours.
        window = { open: hours.open, close: hours.close };
      }
    }
  }
  return {
    openMinute: window?.open ?? null,
    closeMinute: window?.close ?? null,
    closedDays,
  };
}

/** Hours of a trip day for one place, or null when unknown. */
export function hoursOnDay(
  place: TripPlace,
  startDate: Date,
  day: number,
): DayHours | null {
  return place.weeklyHours?.[addDays(startDate, day - 1).getUTCDay()] ?? null;
}

export function fromGoogle(id: number, place: GooglePlace): TripPlace {
  const { category } = googleCategories(place.types);
  return {
    id,
    source: 'GOOGLE',
    name: place.name,
    area: place.address,
    category,
    imageUrl: null,
    location:
      place.lat !== null && place.lng !== null
        ? { lat: place.lat, lng: place.lng }
        : null,
    rating: place.rating,
    visitMinutes: visitMinutesFor(place.types, category),
    price: googlePrice(place, category),
    interests: googleInterests(place.types),
    meal: isMealPlace(place.types),
    parentId: null,
    weeklyHours: place.weeklyHours,
    types: place.types,
    attribution: GOOGLE_ATTRIBUTION,
  };
}

type CatalogTripRow = Prisma.PlaceGetPayload<{
  include: typeof catalogInclude;
}>;

function fromCatalog(row: CatalogTripRow): TripPlace {
  const price = catalogPrice(row, row.parent);
  if (!price) {
    // The seed script refuses such data, so this is a bug, not a user error.
    throw new Error(`Catalog place ${row.id} has no price`);
  }
  const category = row.category ?? 'FUN';
  const hours: DayHours =
    row.openMinute === null || row.closeMinute === null
      ? { allDay: true }
      : { allDay: false, open: row.openMinute, close: row.closeMinute };
  return {
    id: row.id,
    source: 'CATALOG',
    name: row.name ?? '',
    area: row.area,
    category,
    imageUrl: row.imageUrl,
    location:
      row.lat !== null && row.lng !== null
        ? { lat: row.lat, lng: row.lng }
        : null,
    rating: row.rating,
    visitMinutes: row.visitMinutes ?? DEFAULT_VISIT_MINUTES[category],
    price,
    interests: row.interests.map((i) => i.interest),
    // Catalog food places are restaurants and markets: both make a meal.
    meal: category === 'FOOD',
    parentId: row.parentId,
    weeklyHours: Array.from({ length: 7 }, () => hours),
    types: [],
    attribution: null,
  };
}

/**
 * Reads places for trips. Catalog places come from the database; Google places are
 * stored as a place ID only, so their name, location, price and hours are fetched live
 * every time (Google Maps terms), and a place Google cannot return is left out.
 */
@Injectable()
export class TripPlacesService {
  private readonly logger = new Logger(TripPlacesService.name);

  constructor(
    private readonly prisma: PrismaService,
    private readonly google: GooglePlacesClient,
    private readonly places: PlacesService,
  ) {}

  /** The places with these ids that can be read right now, by id. */
  async resolve(ids: number[]): Promise<Map<number, TripPlace>> {
    const unique = [...new Set(ids)];
    const rows = await this.prisma.place.findMany({
      where: { id: { in: unique } },
      include: catalogInclude,
    });
    const resolved = await Promise.all(
      rows.map(async (row): Promise<TripPlace | null> => {
        if (row.source === 'CATALOG') {
          return fromCatalog(row);
        }
        if (!row.googlePlaceId || !this.google.enabled) {
          return null;
        }
        try {
          const place = await this.google.getPlace(row.googlePlaceId);
          return place ? fromGoogle(row.id, place) : null;
        } catch (error) {
          this.logger.warn(`Skipped Google place ${row.id}: ${String(error)}`);
          return null;
        }
      }),
    );
    return new Map(
      resolved.filter((p) => p !== null).map((p) => [p.id, p] as const),
    );
  }

  /**
   * Places to plan a trip from when nobody picked any: popular sights and places to eat
   * in the city from Google, or the city's sample catalog in development.
   */
  async discover(city: string): Promise<TripPlace[]> {
    if (!this.google.enabled) {
      return this.catalogOf(city);
    }
    const [sights, food] = await Promise.all([
      this.google.search({
        query: `${SIGHTS_QUERY} ${city}`,
        pageSize: SIGHTS_COUNT,
        withDetails: true,
      }),
      this.google.search({
        query: `${FOOD_QUERY} ${city}`,
        pageSize: FOOD_COUNT,
        includedType: 'restaurant',
        withDetails: true,
      }),
    ]);
    const found = [...sights.places, ...food.places].filter(
      (place, index, all) =>
        !place.types.some((t) => NOT_A_SIGHT.has(t)) &&
        all.findIndex((p) => p.id === place.id) === index,
    );
    const ids = await this.places.storeGoogleIds(found.map((p) => p.id));
    return found.flatMap((place) => {
      const id = ids.get(place.id);
      return id ? [fromGoogle(id, place)] : [];
    });
  }

  private async catalogOf(city: string): Promise<TripPlace[]> {
    const wanted = normalizeText(city);
    const rows = await this.prisma.place.findMany({
      where: { source: 'CATALOG' },
      include: catalogInclude,
    });
    return rows
      .filter((row) => row.city && normalizeText(row.city) === wanted)
      .map(fromCatalog);
  }
}
