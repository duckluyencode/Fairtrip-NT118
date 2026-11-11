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

/** Visit time used for Google places, which have no TripFair estimate. */
export const DEFAULT_VISIT_MINUTES: Record<PlaceCategory, number> = {
  FOOD: 60,
  FUN: 120,
  SCENIC: 60,
};
