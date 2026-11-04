import { distanceKm } from './geo.js';

describe('distanceKm', () => {
  it('is 0 for the same point', () => {
    const myKhe = { lat: 16.0544, lng: 108.2469 };
    expect(distanceKm(myKhe, myKhe)).toBe(0);
  });

  it('measures Mỹ Khê to Cầu Rồng at about 2 km', () => {
    expect(
      distanceKm(
        { lat: 16.0544, lng: 108.2469 },
        { lat: 16.0612, lng: 108.2272 },
      ),
    ).toBeCloseTo(2.2, 0);
  });
});
