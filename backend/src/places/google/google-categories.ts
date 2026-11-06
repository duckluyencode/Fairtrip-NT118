import type { Interest, PlaceCategory } from '../../generated/prisma/enums.js';

const FOOD_TYPES = new Set([
  'restaurant',
  'food',
  'cafe',
  'coffee_shop',
  'bakery',
  'bar',
  'meal_takeaway',
  'food_court',
  'ice_cream_shop',
  'tea_house',
]);

const FUN_TYPES = new Set([
  'amusement_park',
  'water_park',
  'zoo',
  'aquarium',
  'museum',
  'art_gallery',
  'night_club',
  'shopping_mall',
  'market',
  'bowling_alley',
  'movie_theater',
  'casino',
  'amusement_center',
]);

const SCENIC_TYPES = new Set([
  'tourist_attraction',
  'beach',
  'park',
  'city_park',
  'national_park',
  'natural_feature',
  'hiking_area',
  'garden',
  'botanical_garden',
  'observation_deck',
  'bridge',
  'historical_landmark',
  'monument',
  'church',
  'hindu_temple',
  'buddhist_temple',
  'place_of_worship',
]);

function categoryOf(type: string): PlaceCategory | null {
  if (FOOD_TYPES.has(type) || type.endsWith('_restaurant')) return 'FOOD';
  if (FUN_TYPES.has(type)) return 'FUN';
  if (SCENIC_TYPES.has(type)) return 'SCENIC';
  return null;
}

/**
 * Maps Google place types to the app's filters. Google lists the most specific
 * type first, so the first type that maps decides the main category.
 */
export function googleCategories(types: string[]): {
  category: PlaceCategory;
  categories: PlaceCategory[];
} {
  const categories = [
    ...new Set(types.map(categoryOf).filter((c) => c !== null)),
  ];
  return categories.length > 0
    ? { category: categories[0], categories }
    : { category: 'FUN', categories: ['FUN'] };
}

// Google place types for each of the app's travel preferences.
const INTEREST_TYPES: Record<Interest, string[]> = {
  FOOD: [
    'restaurant',
    'food',
    'cafe',
    'coffee_shop',
    'bakery',
    'food_court',
    'market',
  ],
  BEACH: ['beach'],
  SCENERY: [
    'tourist_attraction',
    'natural_feature',
    'park',
    'national_park',
    'bridge',
    'observation_deck',
    'garden',
  ],
  RELAX: ['spa', 'park', 'beach', 'garden', 'cafe'],
  FUN: [
    'amusement_park',
    'water_park',
    'night_club',
    'bowling_alley',
    'aquarium',
    'zoo',
    'shopping_mall',
  ],
  PHOTO: [
    'tourist_attraction',
    'observation_deck',
    'bridge',
    'historical_landmark',
    'monument',
  ],
  CULTURE: [
    'museum',
    'art_gallery',
    'historical_landmark',
    'monument',
    'church',
    'hindu_temple',
    'buddhist_temple',
    'place_of_worship',
  ],
  EXPLORE: ['tourist_attraction', 'market', 'hiking_area', 'national_park'],
};

/** The app's travel preferences a Google place suits, from its place types. */
export function googleInterests(types: string[]): Interest[] {
  // "vietnamese_restaurant", "seafood_restaurant"... all count as restaurants.
  const all = types.some((t) => t.endsWith('_restaurant'))
    ? [...types, 'restaurant']
    : types;
  return (Object.keys(INTEREST_TYPES) as Interest[]).filter((interest) =>
    INTEREST_TYPES[interest].some((t) => all.includes(t)),
  );
}

/** Visit time when nothing more specific is known, by category. */
export const DEFAULT_VISIT_MINUTES: Record<PlaceCategory, number> = {
  FOOD: 60,
  FUN: 120,
  SCENIC: 60,
};

// Typical visit times by Google place type, in minutes.
const VISIT_MINUTES_BY_TYPE: Record<string, number> = {
  amusement_park: 240,
  water_park: 240,
  zoo: 150,
  aquarium: 120,
  national_park: 180,
  hiking_area: 180,
  beach: 120,
  museum: 90,
  art_gallery: 60,
  shopping_mall: 90,
  market: 60,
  night_club: 120,
  movie_theater: 150,
  bowling_alley: 90,
  park: 60,
  city_park: 60,
  garden: 60,
  botanical_garden: 90,
  historical_landmark: 45,
  buddhist_temple: 45,
  hindu_temple: 45,
  church: 30,
  place_of_worship: 45,
  monument: 30,
  bridge: 30,
  observation_deck: 45,
  cafe: 45,
  coffee_shop: 45,
  tea_house: 45,
  bakery: 30,
  ice_cream_shop: 30,
  restaurant: 60,
  food_court: 45,
};

/**
 * How long people usually stay, from the most specific Google type that has a value
 * (Google lists the most specific type first), else the category's default.
 */
export function visitMinutesFor(
  types: string[],
  category: PlaceCategory,
): number {
  for (const type of types) {
    const minutes =
      VISIT_MINUTES_BY_TYPE[type] ??
      (type.endsWith('_restaurant')
        ? VISIT_MINUTES_BY_TYPE.restaurant
        : undefined);
    if (minutes !== undefined) return minutes;
  }
  return DEFAULT_VISIT_MINUTES[category];
}

const MEAL_TYPES = new Set([
  'restaurant',
  'food_court',
  'meal_takeaway',
  'diner',
  'bar_and_grill',
]);

/** A place for a meal (planned at lunch or dinner time); cafés and bakeries are not. */
export function isMealPlace(types: string[]): boolean {
  return types.some((t) => MEAL_TYPES.has(t) || t.endsWith('_restaurant'));
}
