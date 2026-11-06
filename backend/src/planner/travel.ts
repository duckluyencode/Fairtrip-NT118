import { distanceKm } from '../places/geo.js';
import type { LatLng } from './types.js';

// Straight-line distance times a detour factor, at an average city speed. Google Routes
// would be more accurate but is billed per request; see the roadmap's provisional decisions.
export const DETOUR_FACTOR = 1.4;
export const CITY_SPEED_KMH = 25;

/** Estimated minutes to travel between two points, rounded up. */
export function travelMinutes(from: LatLng, to: LatLng): number {
  const km = distanceKm(from, to) * DETOUR_FACTOR;
  return Math.ceil((km / CITY_SPEED_KMH) * 60);
}
