import type { PlaceCategory, PriceSource } from '../generated/prisma/enums.js';

export interface PriceInfo {
  /** VND per person; equal when the price is fixed, 0 when free. */
  min: number;
  max: number;
  currency: 'VND';
  source: PriceSource;
  provider: string;
  /** True unless this is an official ticket price; the app shows "~" before estimates. */
  isEstimate: boolean;
  /** The ticketed area whose ticket covers this place, e.g. Bà Nà Hills for Cầu Vàng. */
  includedIn: { id: number; name: string } | null;
}

interface OwnPrice {
  priceMin: number | null;
  priceMax: number | null;
  priceSource: PriceSource | null;
  priceProvider: string | null;
}

/** Estimates made by TripFair itself, as opposed to a partner or Google. */
export const ESTIMATE_PROVIDER = 'TripFair';

function info(
  min: number,
  max: number,
  source: PriceSource,
  provider: string,
  includedIn: PriceInfo['includedIn'] = null,
): PriceInfo {
  return {
    min,
    max,
    currency: 'VND',
    source,
    provider,
    isEstimate: source !== 'PARTNER_API',
    includedIn,
  };
}

function ownPrice(place: OwnPrice): PriceInfo | null {
  if (
    place.priceMin === null ||
    place.priceMax === null ||
    place.priceSource === null
  ) {
    return null;
  }
  return info(
    place.priceMin,
    place.priceMax,
    place.priceSource,
    place.priceProvider ?? ESTIMATE_PROVIDER,
  );
}

/**
 * Price of a catalog place: its own price, or else the ticket of the area it is in.
 * Whether the area's ticket is already paid for in a trip is decided by the trip planner.
 */
export function catalogPrice(
  place: OwnPrice,
  parent: (OwnPrice & { id: number; name: string | null }) | null,
): PriceInfo | null {
  const own = ownPrice(place);
  if (own) {
    return own;
  }
  const parentPrice = parent ? ownPrice(parent) : null;
  if (!parent || !parentPrice) {
    return null;
  }
  return {
    ...parentPrice,
    includedIn: { id: parent.id, name: parent.name ?? '' },
  };
}

export interface GooglePriceInput {
  types: string[];
  priceLevel: string | null;
  /** Google priceRange; amounts in the currency's main unit. */
  priceRange: {
    currencyCode: string;
    start: number;
    end: number | null;
  } | null;
}

// Place types that are normally free to visit.
const FREE_TYPES = new Set([
  'beach',
  'park',
  'city_park',
  'natural_feature',
  'hiking_area',
  'bridge',
  'plaza',
  'place_of_worship',
  'church',
  'hindu_temple',
  'buddhist_temple',
  'mosque',
  'synagogue',
]);

// VND per person for each Google price level.
const PRICE_LEVEL_VND: Record<string, [number, number]> = {
  PRICE_LEVEL_FREE: [0, 0],
  PRICE_LEVEL_INEXPENSIVE: [20_000, 50_000],
  PRICE_LEVEL_MODERATE: [50_000, 150_000],
  PRICE_LEVEL_EXPENSIVE: [150_000, 500_000],
  PRICE_LEVEL_VERY_EXPENSIVE: [500_000, 1_500_000],
};

// Last resort when Google gives no price information at all.
const CATEGORY_DEFAULT_VND: Record<PlaceCategory, [number, number]> = {
  FOOD: [50_000, 150_000],
  FUN: [50_000, 200_000],
  SCENIC: [0, 50_000],
};

/**
 * Price chain for places found through Google (no partner ticket):
 * Google priceRange, then free place types, then the price level table,
 * then a default for the category. Every place ends up with a price.
 */
export function googlePrice(
  input: GooglePriceInput,
  category: PlaceCategory,
): PriceInfo {
  const range = input.priceRange;
  if (range && range.currencyCode === 'VND') {
    const max = Math.max(range.start, range.end ?? range.start);
    return info(range.start, max, 'GOOGLE_API', 'Google');
  }
  if (input.types.some((type) => FREE_TYPES.has(type))) {
    return info(0, 0, 'ESTIMATED', ESTIMATE_PROVIDER);
  }
  const level = input.priceLevel
    ? PRICE_LEVEL_VND[input.priceLevel]
    : undefined;
  if (level) {
    return info(level[0], level[1], 'ESTIMATED', ESTIMATE_PROVIDER);
  }
  const [min, max] = CATEGORY_DEFAULT_VND[category];
  return info(min, max, 'ESTIMATED', ESTIMATE_PROVIDER);
}
