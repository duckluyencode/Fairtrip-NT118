import type { PriceSource } from '../generated/prisma/enums.js';
import type { GooglePlace } from '../places/google/google-places.client.js';
import { ESTIMATE_PROVIDER } from '../places/pricing.js';

/** People per room when TripFair books for a group. */
export const GUESTS_PER_ROOM = 2;

/** Rooms needed so that no room has more than guestsPerRoom people. */
export function roomsNeeded(people: number, guestsPerRoom: number): number {
  return Math.max(1, Math.ceil(people / guestsPerRoom));
}

/**
 * Splits the room cost: members fill rooms in joining order, and each room's cost is
 * shared equally by the people in it (rounding remainders go to the first of them).
 * Someone alone in a room pays the whole room.
 */
export function roomShares(
  memberIds: number[],
  guestsPerRoom: number,
  pricePerNight: number,
  nights: number,
): Map<number, number> {
  const shares = new Map<number, number>();
  const roomCost = pricePerNight * nights;
  for (let start = 0; start < memberIds.length; start += guestsPerRoom) {
    const room = memberIds.slice(start, start + guestsPerRoom);
    const each = Math.floor(roomCost / room.length);
    room.forEach((id, index) => {
      shares.set(id, index === 0 ? roomCost - each * (room.length - 1) : each);
    });
  }
  return shares;
}

export interface NightlyPrice {
  /** VND for one room and one night. */
  pricePerNight: number;
  source: PriceSource;
  provider: string;
}

// VND per room per night for each Google price level, until the Agoda API is available.
// The planner uses the top of the range so that a member's budget is never overrun.
const LODGING_LEVEL_VND: Record<string, [number, number]> = {
  PRICE_LEVEL_INEXPENSIVE: [300_000, 500_000],
  PRICE_LEVEL_MODERATE: [500_000, 1_000_000],
  PRICE_LEVEL_EXPENSIVE: [1_000_000, 2_500_000],
  PRICE_LEVEL_VERY_EXPENSIVE: [2_500_000, 6_000_000],
};
const LODGING_DEFAULT_VND: [number, number] = [400_000, 800_000];

/**
 * Nightly price of a hotel found through Google: Google's priceRange when it is in VND,
 * otherwise an estimate from its price level (a mid-range default when it has none).
 */
export function lodgingPrice(
  place: Pick<GooglePlace, 'priceLevel' | 'priceRange'>,
): NightlyPrice {
  const range = place.priceRange;
  if (range && range.currencyCode === 'VND') {
    return {
      pricePerNight: Math.max(range.start, range.end ?? range.start),
      source: 'GOOGLE_API',
      provider: 'Google',
    };
  }
  const [, max] =
    (place.priceLevel && LODGING_LEVEL_VND[place.priceLevel]) ||
    LODGING_DEFAULT_VND;
  return {
    pricePerNight: max,
    source: 'ESTIMATED',
    provider: ESTIMATE_PROVIDER,
  };
}
