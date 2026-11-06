import type { GooglePlace } from '../places/google/google-places.client.js';
import { lodgingPrice, roomShares, roomsNeeded } from './lodging-shares.js';
import { pickThree, type Candidate } from './lodging.service.js';

function candidate(
  placeId: number,
  pricePerNight: number,
  rating: number | null,
  distanceKm: number | null,
): Candidate {
  return {
    placeId,
    place: { rating } as GooglePlace,
    price: { pricePerNight, source: 'ESTIMATED', provider: 'TripFair' },
    distanceKm,
    shares: new Map(),
  };
}

describe('roomShares', () => {
  it('fills rooms in joining order and splits each room equally', () => {
    // Three people, two per room: 1 and 2 share, 3 is alone.
    expect([...roomShares([1, 2, 3], 2, 500_000, 2)]).toEqual([
      [1, 500_000],
      [2, 500_000],
      [3, 1_000_000],
    ]);
    expect(roomsNeeded(3, 2)).toBe(2);
    expect(roomsNeeded(0, 2)).toBe(1);
  });

  it('gives the rounding remainder to the first person of the room', () => {
    expect([...roomShares([1, 2, 3], 3, 100_000, 1)]).toEqual([
      [1, 33_334],
      [2, 33_333],
      [3, 33_333],
    ]);
  });
});

describe('lodgingPrice', () => {
  it("uses Google's VND price range, then the price level, then a default", () => {
    expect(
      lodgingPrice({
        priceLevel: 'PRICE_LEVEL_EXPENSIVE',
        priceRange: { currencyCode: 'VND', start: 700_000, end: 900_000 },
      }),
    ).toEqual({
      pricePerNight: 900_000,
      source: 'GOOGLE_API',
      provider: 'Google',
    });
    expect(
      lodgingPrice({ priceLevel: 'PRICE_LEVEL_MODERATE', priceRange: null }),
    ).toEqual({
      pricePerNight: 1_000_000,
      source: 'ESTIMATED',
      provider: 'TripFair',
    });
    expect(lodgingPrice({ priceLevel: null, priceRange: null })).toMatchObject({
      pricePerNight: 800_000,
      source: 'ESTIMATED',
    });
  });
});

describe('pickThree', () => {
  it('picks three different hotels: cheapest, balanced, nearest', () => {
    const picked = pickThree([
      candidate(1, 300_000, 3.0, 8), // cheapest
      candidate(2, 600_000, 4.8, 3), // good rating for its price
      candidate(3, 2_000_000, 4.9, 0.5), // nearest
      candidate(4, 900_000, 3.5, 6),
    ]);
    expect(picked.map((p) => [p.tag, p.candidate.placeId])).toEqual([
      ['CHEAPEST', 1],
      ['BALANCED', 2],
      ['NEAREST', 3],
    ]);
  });

  it('returns fewer when there are fewer hotels', () => {
    expect(pickThree([candidate(1, 300_000, null, null)])).toHaveLength(1);
    expect(pickThree([])).toEqual([]);
  });
});
