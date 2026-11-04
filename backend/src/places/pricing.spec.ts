import { catalogPrice, googlePrice } from './pricing.js';

const noPrice = {
  priceMin: null,
  priceMax: null,
  priceSource: null,
  priceProvider: null,
};

const baNaHills = {
  id: 1,
  name: 'Bà Nà Hills',
  priceMin: 900000,
  priceMax: 900000,
  priceSource: 'PARTNER_API' as const,
  priceProvider: 'Klook',
};

describe('catalogPrice', () => {
  it('uses the place’s own price', () => {
    expect(
      catalogPrice(
        {
          priceMin: 40000,
          priceMax: 60000,
          priceSource: 'GOOGLE_API',
          priceProvider: 'Google',
        },
        null,
      ),
    ).toEqual({
      min: 40000,
      max: 60000,
      currency: 'VND',
      source: 'GOOGLE_API',
      provider: 'Google',
      isEstimate: true,
      includedIn: null,
    });
  });

  it('falls back to the ticket of the area the place is in', () => {
    expect(catalogPrice(noPrice, baNaHills)).toMatchObject({
      min: 900000,
      isEstimate: false,
      includedIn: { id: 1, name: 'Bà Nà Hills' },
    });
  });

  it('has no price when neither the place nor its area has one', () => {
    expect(catalogPrice(noPrice, null)).toBeNull();
    expect(catalogPrice(noPrice, { ...baNaHills, ...noPrice })).toBeNull();
  });
});

describe('googlePrice', () => {
  const restaurant = {
    types: ['restaurant'],
    priceLevel: null,
    priceRange: null,
  };

  it('prefers the price range from Google', () => {
    expect(
      googlePrice(
        {
          ...restaurant,
          priceRange: { currencyCode: 'VND', start: 100000, end: 200000 },
        },
        'FOOD',
      ),
    ).toMatchObject({
      min: 100000,
      max: 200000,
      source: 'GOOGLE_API',
      provider: 'Google',
    });
  });

  it('uses the start price when the range is open-ended', () => {
    expect(
      googlePrice(
        {
          ...restaurant,
          priceRange: { currencyCode: 'VND', start: 100000, end: null },
        },
        'FOOD',
      ),
    ).toMatchObject({ min: 100000, max: 100000 });
  });

  it('treats beaches, parks and temples as free', () => {
    expect(
      googlePrice(
        { types: ['beach'], priceLevel: null, priceRange: null },
        'SCENIC',
      ),
    ).toMatchObject({ min: 0, max: 0, source: 'ESTIMATED', isEstimate: true });
  });

  it('estimates from the price level', () => {
    expect(
      googlePrice(
        { ...restaurant, priceLevel: 'PRICE_LEVEL_MODERATE' },
        'FOOD',
      ),
    ).toMatchObject({
      min: 50000,
      max: 150000,
      source: 'ESTIMATED',
      provider: 'TripFair',
    });
  });

  it('ignores a price range in another currency', () => {
    expect(
      googlePrice(
        {
          ...restaurant,
          priceRange: { currencyCode: 'USD', start: 10, end: 20 },
        },
        'FOOD',
      ),
    ).toMatchObject({ source: 'ESTIMATED', min: 50000, max: 150000 });
  });

  it('always ends with a price for the category', () => {
    expect(
      googlePrice(
        { types: ['museum'], priceLevel: null, priceRange: null },
        'FUN',
      ),
    ).toMatchObject({ min: 50000, max: 200000, source: 'ESTIMATED' });
  });
});
