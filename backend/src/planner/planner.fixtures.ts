import type { PlannerInput, PlannerPlace } from './types.js';

// Small Đà Nẵng-like instance shared by the planner tests and the benchmark.

export function place(
  id: number,
  overrides: Partial<PlannerPlace> = {},
): PlannerPlace {
  return {
    id,
    lat: 16.06,
    lng: 108.22,
    visitMinutes: 60,
    cost: 0,
    coveredBy: null,
    interests: [],
    rating: 4.5,
    openMinute: null,
    closeMinute: null,
    votes: [],
    ...overrides,
  };
}

export const BA_NA = place(1, {
  lat: 15.9977,
  lng: 107.9881,
  visitMinutes: 240,
  cost: 900_000,
  interests: ['FUN', 'SCENERY', 'PHOTO'],
  rating: 4.6,
  openMinute: 420,
  closeMinute: 1320,
});
export const GOLDEN_BRIDGE = place(2, {
  lat: 15.9952,
  lng: 107.9966,
  visitMinutes: 60,
  cost: 900_000,
  coveredBy: 1,
  interests: ['SCENERY', 'PHOTO'],
  rating: 4.8,
});
export const MY_KHE = place(3, {
  lat: 16.0544,
  lng: 108.2469,
  visitMinutes: 120,
  interests: ['BEACH', 'RELAX'],
  rating: 4.7,
});
export const DRAGON_BRIDGE = place(4, {
  lat: 16.0612,
  lng: 108.2272,
  visitMinutes: 45,
  interests: ['SCENERY', 'PHOTO'],
  rating: 4.7,
});
export const MI_QUANG = place(5, {
  lat: 16.0679,
  lng: 108.2186,
  visitMinutes: 45,
  cost: 60_000,
  interests: ['FOOD'],
  rating: 4.6,
  openMinute: 360,
  closeMinute: 1260,
});
export const NIGHT_MARKET = place(6, {
  lat: 16.0614,
  lng: 108.2311,
  visitMinutes: 120,
  cost: 250_000,
  interests: ['FOOD', 'FUN'],
  rating: 4.5,
  openMinute: 1080,
  closeMinute: 1410,
});
export const CHAM_MUSEUM = place(7, {
  lat: 16.0603,
  lng: 108.2233,
  visitMinutes: 90,
  cost: 60_000,
  interests: ['CULTURE'],
  rating: 4.5,
  openMinute: 450,
  closeMinute: 1020,
});
export const MARBLE_MOUNTAINS = place(8, {
  lat: 16.0034,
  lng: 108.2637,
  visitMinutes: 150,
  cost: 40_000,
  interests: ['SCENERY', 'CULTURE', 'EXPLORE'],
  rating: 4.6,
  openMinute: 420,
  closeMinute: 1050,
});

export const DA_NANG = [
  BA_NA,
  GOLDEN_BRIDGE,
  MY_KHE,
  DRAGON_BRIDGE,
  MI_QUANG,
  NIGHT_MARKET,
  CHAM_MUSEUM,
  MARBLE_MOUNTAINS,
];

export function input(overrides: Partial<PlannerInput> = {}): PlannerInput {
  return {
    members: [{ id: 1, available: 2_000_000, interests: ['FOOD', 'BEACH'] }],
    places: DA_NANG,
    days: 2,
    dayStartMinute: 480,
    dayEndMinute: 1260,
    gapMinutes: 30,
    base: { lat: 16.0544, lng: 108.244 },
    ...overrides,
  };
}
