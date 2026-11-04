import type { Place, PlaceCategoryLink } from '../generated/prisma/client.js';
import type { PlaceCategory } from '../generated/prisma/enums.js';
import type {
  LocationResponse,
  OpeningHoursResponse,
  PlaceDetail,
  PlaceSummary,
} from './dto/place.response.js';
import { distanceKm, type LatLng } from './geo.js';
import {
  DEFAULT_VISIT_MINUTES,
  googleCategories,
} from './google/google-categories.js';
import type { DayHours, GooglePlace } from './google/google-places.client.js';
import { catalogPrice, googlePrice } from './pricing.js';

export type CatalogRow = Place & { categories: PlaceCategoryLink[] };
export type CatalogDetailRow = CatalogRow & { parent: Place | null };

const GOOGLE_ATTRIBUTION = 'Google Maps';
const CATEGORY_ORDER: PlaceCategory[] = ['FOOD', 'FUN', 'SCENIC'];

function hhmm(minutes: number): string {
  const h = Math.floor(minutes / 60);
  const m = minutes % 60;
  return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}`;
}

function locationOf(
  lat: number | null,
  lng: number | null,
): LocationResponse | null {
  return lat === null || lng === null ? null : { lat, lng };
}

function distanceFrom(
  origin: LatLng | null,
  location: LocationResponse | null,
): number | null {
  return origin && location ? distanceKm(origin, location) : null;
}

/** Main category first, then the others in a fixed order. */
function categoriesOf(row: CatalogRow): PlaceCategory[] {
  const main = row.category ?? 'FUN';
  const others = CATEGORY_ORDER.filter(
    (c) => c !== main && row.categories.some((link) => link.category === c),
  );
  return [main, ...others];
}

export function catalogSummary(
  row: CatalogRow,
  origin: LatLng | null,
  isFavorite: boolean,
): PlaceSummary {
  const location = locationOf(row.lat, row.lng);
  return {
    id: row.id,
    source: row.source,
    name: row.name ?? '',
    area: row.area,
    city: row.city,
    category: row.category ?? 'FUN',
    categories: categoriesOf(row),
    rating: row.rating,
    ratingCount: row.ratingCount,
    imageUrl: row.imageUrl,
    location,
    distanceKm: distanceFrom(origin, location),
    isFavorite,
    attribution: null,
  };
}

function catalogHours(row: Place): OpeningHoursResponse {
  if (row.openMinute === null || row.closeMinute === null) {
    return { allDay: true, opensAt: null, closesAt: null, closedToday: false };
  }
  return {
    allDay: false,
    opensAt: hhmm(row.openMinute),
    closesAt: hhmm(row.closeMinute),
    closedToday: false,
  };
}

export function catalogDetail(
  row: CatalogDetailRow,
  origin: LatLng | null,
  isFavorite: boolean,
): PlaceDetail {
  const summary = catalogSummary(row, origin, isFavorite);
  const price = catalogPrice(row, row.parent);
  if (!price) {
    // The seed script refuses such data, so this is a bug, not a user error.
    throw new Error(`Catalog place ${row.id} has no price`);
  }
  return Object.assign(summary, {
    description: row.description,
    visitMinutes: row.visitMinutes ?? DEFAULT_VISIT_MINUTES[summary.category],
    openingHours: catalogHours(row),
    price,
    mapsUri: null,
  });
}

export function googleSummary(
  id: number,
  place: GooglePlace,
  origin: LatLng | null,
  isFavorite: boolean,
): PlaceSummary {
  const { category, categories } = googleCategories(place.types);
  const location = locationOf(place.lat, place.lng);
  return {
    id,
    source: 'GOOGLE',
    name: place.name,
    area: place.address,
    city: null,
    category,
    categories,
    rating: place.rating,
    ratingCount: place.ratingCount,
    imageUrl: null,
    location,
    distanceKm: distanceFrom(origin, location),
    isFavorite,
    attribution: GOOGLE_ATTRIBUTION,
  };
}

function googleHours(hours: DayHours | null): OpeningHoursResponse | null {
  if (!hours) {
    return null;
  }
  if ('closed' in hours) {
    return { allDay: false, opensAt: null, closesAt: null, closedToday: true };
  }
  if (hours.allDay) {
    return { allDay: true, opensAt: null, closesAt: null, closedToday: false };
  }
  return {
    allDay: false,
    opensAt: hhmm(hours.open),
    closesAt: hhmm(hours.close),
    closedToday: false,
  };
}

export function googleDetail(
  id: number,
  place: GooglePlace,
  origin: LatLng | null,
  isFavorite: boolean,
): PlaceDetail {
  const summary = googleSummary(id, place, origin, isFavorite);
  return Object.assign(summary, {
    description: place.summary,
    visitMinutes: DEFAULT_VISIT_MINUTES[summary.category],
    openingHours: googleHours(place.todayHours),
    price: googlePrice(place, summary.category),
    mapsUri: place.mapsUri,
  });
}
