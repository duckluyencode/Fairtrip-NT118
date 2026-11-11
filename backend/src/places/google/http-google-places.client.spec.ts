import { ConfigService } from '@nestjs/config';
import { googleCategories } from './google-categories.js';
import {
  HttpGooglePlacesClient,
  todayHours,
  toGooglePlace,
} from './http-google-places.client.js';

// Wednesday 2026-11-04 10:00 in Vietnam (03:00 UTC).
const WEDNESDAY = new Date('2026-11-04T03:00:00Z');

const apiPlace = {
  id: 'ChIJ-pho',
  displayName: { text: 'Phở Hồng' },
  formattedAddress: '10 Lê Duẩn, Hải Châu, Đà Nẵng',
  location: { latitude: 16.07, longitude: 108.22 },
  types: ['vietnamese_restaurant', 'restaurant', 'food'],
  rating: 4.4,
  userRatingCount: 820,
  priceLevel: 'PRICE_LEVEL_INEXPENSIVE',
  priceRange: {
    startPrice: { currencyCode: 'VND', units: '40000' },
    endPrice: { currencyCode: 'VND', units: '80000' },
  },
  regularOpeningHours: {
    periods: [
      {
        open: { day: 3, hour: 6, minute: 30 },
        close: { day: 3, hour: 21, minute: 0 },
      },
    ],
  },
  googleMapsUri: 'https://maps.google.com/?cid=1',
};

function client(apiKey?: string) {
  return new HttpGooglePlacesClient(
    new ConfigService(apiKey ? { GOOGLE_MAPS_API_KEY: apiKey } : {}),
  );
}

describe('toGooglePlace', () => {
  it('maps the Places API response', () => {
    expect(toGooglePlace(apiPlace, WEDNESDAY)).toEqual({
      id: 'ChIJ-pho',
      name: 'Phở Hồng',
      address: '10 Lê Duẩn, Hải Châu, Đà Nẵng',
      lat: 16.07,
      lng: 108.22,
      types: ['vietnamese_restaurant', 'restaurant', 'food'],
      rating: 4.4,
      ratingCount: 820,
      priceLevel: 'PRICE_LEVEL_INEXPENSIVE',
      priceRange: { currencyCode: 'VND', start: 40000, end: 80000 },
      todayHours: { allDay: false, open: 390, close: 1260 },
      weeklyHours: [
        { closed: true },
        { closed: true },
        { closed: true },
        { allDay: false, open: 390, close: 1260 },
        { closed: true },
        { closed: true },
        { closed: true },
      ],
      summary: null,
      mapsUri: 'https://maps.google.com/?cid=1',
    });
  });
});

describe('todayHours', () => {
  it('recognizes places open 24 hours', () => {
    expect(
      todayHours([{ open: { day: 0, hour: 0, minute: 0 } }], WEDNESDAY),
    ).toEqual({
      allDay: true,
    });
  });

  it('reports closed when there is no period for today', () => {
    expect(
      todayHours(
        [{ open: { day: 1, hour: 8 }, close: { day: 1, hour: 17 } }],
        WEDNESDAY,
      ),
    ).toEqual({ closed: true });
  });

  it('is unknown without periods', () => {
    expect(todayHours(undefined, WEDNESDAY)).toBeNull();
  });
});

describe('googleCategories', () => {
  it('takes the main category from the first type that maps', () => {
    expect(googleCategories(['beach', 'tourist_attraction'])).toEqual({
      category: 'SCENIC',
      categories: ['SCENIC'],
    });
    expect(googleCategories(['night_market', 'market', 'food'])).toEqual({
      category: 'FUN',
      categories: ['FUN', 'FOOD'],
    });
    expect(googleCategories(['point_of_interest'])).toEqual({
      category: 'FUN',
      categories: ['FUN'],
    });
  });
});

describe('HttpGooglePlacesClient', () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('is disabled without an API key', async () => {
    const google = client();
    expect(google.enabled).toBe(false);
    await expect(
      google.search({ query: 'phở', pageSize: 5 }),
    ).rejects.toMatchObject({ code: 'GOOGLE_PLACES_UNAVAILABLE' });
    await expect(google.getPlace('x')).rejects.toMatchObject({
      code: 'GOOGLE_PLACES_UNAVAILABLE',
    });
  });

  it('sends the key, a field mask and the search options, and maps the results', async () => {
    const fetch = vi
      .fn()
      .mockResolvedValue(
        new Response(
          JSON.stringify({ places: [apiPlace], nextPageToken: 'next' }),
          { status: 200 },
        ),
      );
    vi.stubGlobal('fetch', fetch);

    const result = await client('test-key').search({
      query: 'phở',
      pageSize: 5,
      pageToken: 'page-2',
      includedType: 'restaurant',
      near: { lat: 16.07, lng: 108.22, radiusMeters: 90_000 },
    });

    expect(result.places.map((p) => p.name)).toEqual(['Phở Hồng']);
    expect(result.nextPageToken).toBe('next');
    const [url, init] = fetch.mock.calls[0] as [string, RequestInit];
    expect(url).toBe('https://places.googleapis.com/v1/places:searchText');
    expect(init.headers).toMatchObject({ 'X-Goog-Api-Key': 'test-key' });
    const mask = (init.headers as Record<string, string>)['X-Goog-FieldMask'];
    expect(mask).toContain('places.displayName');
    expect(mask).not.toContain('regularOpeningHours');
    expect(JSON.parse(init.body as string)).toEqual({
      textQuery: 'phở',
      languageCode: 'vi',
      pageSize: 5,
      pageToken: 'page-2',
      includedType: 'restaurant',
      // Google accepts at most 50 km.
      locationBias: {
        circle: {
          center: { latitude: 16.07, longitude: 108.22 },
          radius: 50_000,
        },
      },
    });
  });

  it('reads prices and opening hours in the same call when planning', async () => {
    const fetch = vi
      .fn()
      .mockResolvedValue(
        new Response(JSON.stringify({ places: [apiPlace] }), { status: 200 }),
      );
    vi.stubGlobal('fetch', fetch);

    await client('test-key').search({
      query: 'điểm tham quan Đà Nẵng',
      pageSize: 20,
      withDetails: true,
    });

    const [, init] = fetch.mock.calls[0] as [string, RequestInit];
    const mask = (init.headers as Record<string, string>)['X-Goog-FieldMask'];
    expect(mask).toContain('places.priceLevel');
    expect(mask).toContain('places.regularOpeningHours');
  });

  it('fails the search with 503 when Google errors', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(new Response('quota', { status: 429 })),
    );
    await expect(
      client('test-key').search({ query: 'phở', pageSize: 5 }),
    ).rejects.toMatchObject({ code: 'GOOGLE_PLACES_UNAVAILABLE' });
  });

  it('maps autocomplete suggestions and sends the session token', async () => {
    const fetch = vi.fn().mockResolvedValue(
      new Response(
        JSON.stringify({
          suggestions: [
            {
              placePrediction: {
                placeId: 'ChIJ-gold',
                structuredFormat: {
                  mainText: { text: 'Cầu Vàng' },
                  secondaryText: { text: 'Hòa Vang, Đà Nẵng' },
                },
              },
            },
            { queryPrediction: { text: { text: 'cầu vàng đà nẵng' } } },
          ],
        }),
        { status: 200 },
      ),
    );
    vi.stubGlobal('fetch', fetch);

    const suggestions = await client('test-key').autocomplete(
      'cau va',
      'session-1',
      null,
    );

    expect(suggestions).toEqual([
      {
        placeId: 'ChIJ-gold',
        mainText: 'Cầu Vàng',
        secondaryText: 'Hòa Vang, Đà Nẵng',
      },
    ]);
    const [url, init] = fetch.mock.calls[0] as [string, RequestInit];
    expect(url).toBe('https://places.googleapis.com/v1/places:autocomplete');
    expect(JSON.parse(init.body as string)).toMatchObject({
      input: 'cau va',
      sessionToken: 'session-1',
    });
  });

  it('returns null for an unknown place and 503 for other errors', async () => {
    vi.stubGlobal(
      'fetch',
      vi
        .fn()
        .mockResolvedValueOnce(new Response('', { status: 404 }))
        .mockResolvedValueOnce(new Response('boom', { status: 500 })),
    );
    const google = client('test-key');
    await expect(google.getPlace('gone')).resolves.toBeNull();
    await expect(google.getPlace('x')).rejects.toMatchObject({
      code: 'GOOGLE_PLACES_UNAVAILABLE',
    });
  });
});
