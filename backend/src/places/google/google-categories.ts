import type { PlaceCategory } from '../../generated/prisma/enums.js';

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

/** Visit time used for Google places, which have no TripFair estimate. */
export const DEFAULT_VISIT_MINUTES: Record<PlaceCategory, number> = {
  FOOD: 60,
  FUN: 120,
  SCENIC: 60,
};
