import { distanceKm } from '../places/geo.js';
import type { LatLng } from './types.js';

// Straight-line distance times a detour factor, at an average city speed. The planner
// calls this thousands of times, so it stays an estimate; the saved itinerary is timed
// again with Google Routes (PlanningService), one billed request per day.
export const DETOUR_FACTOR = 1.4;
export const CITY_SPEED_KMH = 25;

/** Estimated minutes to travel between two points, rounded up. */
export function travelMinutes(from: LatLng, to: LatLng): number {
  const km = distanceKm(from, to) * DETOUR_FACTOR;
  return Math.ceil((km / CITY_SPEED_KMH) * 60);
}
