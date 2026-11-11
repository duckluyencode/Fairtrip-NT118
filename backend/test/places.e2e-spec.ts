import request from 'supertest';
import { seedCatalog } from '../prisma/seed/catalog.js';
import type { GooglePlace } from '../src/places/google/google-places.client.js';
import {
  createTestApp,
  resetDatabase,
  signUp,
  type TestApp,
} from './utils/test-app.js';

interface Summary {
  id: number;
  name: string;
  source: string;
  isFavorite: boolean;
  distanceKm: number | null;
}

const googlePho: GooglePlace = {
  id: 'google-pho-hong',
  name: 'Phở Hồng',
  address: '10 Lê Duẩn, Hải Châu',
  lat: 16.07,
  lng: 108.22,
  types: ['vietnamese_restaurant', 'restaurant'],
  rating: 4.4,
  ratingCount: 820,
  priceLevel: 'PRICE_LEVEL_MODERATE',
  priceRange: null,
  todayHours: { allDay: false, open: 390, close: 1260 },
  weeklyHours: Array.from({ length: 7 }, () => ({
    allDay: false as const,
    open: 390,
    close: 1260,
  })),
  summary: null,
  mapsUri: 'https://maps.google.com/?cid=1',
};

const googleBeach: GooglePlace = {
  ...googlePho,
  id: 'google-pham-van-dong-beach',
  name: 'Bãi biển Phạm Văn Đồng',
  types: ['beach'],
  priceLevel: null,
  todayHours: { allDay: true },
  weeklyHours: Array.from({ length: 7 }, () => ({ allDay: true as const })),
};

describe('Places, search history and favorites (e2e)', () => {
  let t: TestApp;
  let token: string;
  const api = () => request(t.app.getHttpServer());
  const get = (path: string) =>
    api().get(`/api/v1${path}`).auth(token, { type: 'bearer' });

  async function idOf(slug: string): Promise<number> {
    const place = await t.prisma.place.findUniqueOrThrow({ where: { slug } });
    return place.id;
  }

  beforeAll(async () => {
    t = await createTestApp();
  });

  beforeEach(async () => {
    await resetDatabase(t.prisma);
    await seedCatalog(t.prisma);
    t.otp.sent.length = 0;
    t.google.reset();
    token = await signUp(t);
  });

  afterAll(async () => {
    await t.app.close();
  });

  it('requires signing in', async () => {
    await api().get('/api/v1/places').expect(401);
    await api().get('/api/v1/me/favorites').expect(401);
  });

  describe('search', () => {
    it('ignores diacritics, ranks the best match first and shows no prices', async () => {
      const res = await get('/places?q=cau').expect(200);

      expect(res.body.items.map((p: Summary) => p.name)).toEqual([
        'Cầu Vàng',
        'Cầu Rồng',
        'Cầu Tình Yêu',
      ]);
      expect(res.body).toMatchObject({ page: 1, limit: 20, total: 3 });
      expect(res.body.items[0]).toMatchObject({
        source: 'CATALOG',
        category: 'SCENIC',
        categories: ['SCENIC', 'FUN'],
        isFavorite: false,
        attribution: null,
      });
      expect(res.body.items[0]).not.toHaveProperty('price');
    });

    it('matches the start of words only', async () => {
      const res = await get('/places?q=ang').expect(200);
      expect(res.body.total).toBe(0);
    });

    it('finds places by area and city', async () => {
      const res = await get(`/places?q=${encodeURIComponent('Bà Nà')}`).expect(
        200,
      );
      expect(res.body.items.slice(0, 3).map((p: Summary) => p.name)).toEqual([
        'Bà Nà Hills',
        'Cầu Vàng',
        "Vườn hoa Le Jardin d'Amour",
      ]);
    });

    it('filters by category and pages through the results', async () => {
      const all = await get('/places?category=FOOD').expect(200);
      expect(all.body.items.map((p: Summary) => p.name)).toEqual([
        'Mì Quảng Bà Mua',
        'Chợ đêm Sơn Trà',
        'Bánh xèo Bà Dưỡng',
        'Chợ Hàn',
      ]);

      const second = await get('/places?category=FOOD&limit=2&page=2').expect(
        200,
      );
      expect(second.body).toMatchObject({ page: 2, limit: 2, total: 4 });
      expect(second.body.items.map((p: Summary) => p.name)).toEqual([
        'Bánh xèo Bà Dưỡng',
        'Chợ Hàn',
      ]);
    });

    it('lists the nearest places first when the position is sent', async () => {
      const res = await get('/places?lat=16.0544&lng=108.2469').expect(200);
      expect(res.body.items[0]).toMatchObject({
        name: 'Bãi biển Mỹ Khê',
        distanceKm: 0,
      });
      const distances = res.body.items.map((p: Summary) => p.distanceKm);
      expect(distances).toEqual([...distances].sort((a, b) => a - b));
    });

    it('rejects invalid filters', async () => {
      for (const query of [
        'lat=16.05',
        'lat=100&lng=108',
        'category=HOTEL',
        'limit=51',
        'page=0',
        'unknown=1',
      ]) {
        await get(`/places?${query}`)
          .expect(400)
          .expect((res) => expect(res.body.code).toBe('VALIDATION_FAILED'));
      }
    });

    it('suggests names while typing without saving them', async () => {
      const res = await get(
        `/places/suggestions?q=${encodeURIComponent('ba na')}`,
      ).expect(200);
      expect(res.body.length).toBeLessThanOrEqual(5);
      expect(res.body[0]).toMatchObject({
        name: 'Bà Nà Hills',
        area: 'Núi Chúa · Hòa Vang',
      });

      const history = await get('/me/search-history').expect(200);
      expect(history.body.items).toEqual([]);
    });
  });

  describe('place detail', () => {
    it('prices a place by the ticket of the area it is in', async () => {
      const res = await get(`/places/${await idOf('golden-bridge')}`).expect(
        200,
      );
      expect(res.body).toMatchObject({
        name: 'Cầu Vàng',
        visitMinutes: 60,
        openingHours: {
          allDay: false,
          opensAt: '07:00',
          closesAt: '22:00',
          closedToday: false,
        },
        price: {
          min: 900000,
          max: 900000,
          currency: 'VND',
          source: 'PARTNER_API',
          provider: 'Klook',
          isEstimate: false,
          includedIn: { id: await idOf('ba-na-hills'), name: 'Bà Nà Hills' },
        },
      });
    });

    it('marks free places and estimates', async () => {
      const beach = await get(`/places/${await idOf('my-khe-beach')}`).expect(
        200,
      );
      expect(beach.body.price).toMatchObject({
        min: 0,
        max: 0,
        isEstimate: true,
        includedIn: null,
      });
      expect(beach.body.openingHours).toMatchObject({ allDay: true });
    });

    it('returns PLACE_NOT_FOUND for an unknown place', async () => {
      await get('/places/999999')
        .expect(404)
        .expect((res) => expect(res.body.code).toBe('PLACE_NOT_FOUND'));
      await get('/places/abc').expect(400);
    });
  });

  describe('search history', () => {
    it('keeps one entry per query, newest first', async () => {
      await get(`/places?q=${encodeURIComponent('Đà Nẵng')}`).expect(200);
      await get(`/places?q=${encodeURIComponent('Cầu Vàng')}`).expect(200);
      // Same query without diacritics and with a filter: the entry moves to the top.
      await get('/places?q=da%20nang&category=FOOD').expect(200);

      const res = await get('/me/search-history').expect(200);
      expect(
        res.body.items.map((e: { query: string; category: string | null }) => [
          e.query,
          e.category,
        ]),
      ).toEqual([
        ['da nang', 'FOOD'],
        ['Cầu Vàng', null],
      ]);
    });

    it('keeps only the latest 50 entries', async () => {
      const user = await t.prisma.user.findFirstOrThrow();
      await t.prisma.searchHistory.createMany({
        data: Array.from({ length: 50 }, (_, i) => ({
          userId: user.id,
          query: `old ${i}`,
          normalizedQuery: `old ${i}`,
          searchedAt: new Date(Date.UTC(2026, 0, 1, 0, i)),
        })),
      });

      await get('/places?q=pho').expect(200);

      const res = await get('/me/search-history?limit=50').expect(200);
      expect(res.body.items).toHaveLength(50);
      expect(res.body.items[0].query).toBe('pho');
      expect(
        res.body.items.some((e: { query: string }) => e.query === 'old 0'),
      ).toBe(false);
    });

    it('deletes one entry or all of them, only for their owner', async () => {
      await get('/places?q=my%20khe').expect(200);
      await get('/places?q=cho%20han').expect(200);
      const [latest] = (await get('/me/search-history').expect(200)).body
        .items as { id: number }[];

      const otherToken = await signUp(t, 'an@example.com');
      await api()
        .delete(`/api/v1/me/search-history/${latest.id}`)
        .auth(otherToken, { type: 'bearer' })
        .expect(404);

      await api()
        .delete(`/api/v1/me/search-history/${latest.id}`)
        .auth(token, { type: 'bearer' })
        .expect(204);
      await api()
        .delete(`/api/v1/me/search-history/${latest.id}`)
        .auth(token, { type: 'bearer' })
        .expect(404);
      expect((await get('/me/search-history')).body.items).toHaveLength(1);

      await api()
        .delete('/api/v1/me/search-history')
        .auth(token, { type: 'bearer' })
        .expect(204);
      expect((await get('/me/search-history')).body.items).toEqual([]);
    });
  });

  describe('favorites', () => {
    const put = (id: number) =>
      api().put(`/api/v1/me/favorites/${id}`).auth(token, { type: 'bearer' });
    const del = (id: number) =>
      api()
        .delete(`/api/v1/me/favorites/${id}`)
        .auth(token, { type: 'bearer' });

    it('saves, lists, filters and removes favorites', async () => {
      const miQuang = await idOf('mi-quang-ba-mua');
      const myKhe = await idOf('my-khe-beach');

      await put(miQuang).expect(204);
      await put(myKhe).expect(204);
      await put(myKhe).expect(204); // saving twice keeps one

      const list = await get('/me/favorites').expect(200);
      expect(
        list.body.items.map((p: Summary) => [p.name, p.isFavorite]),
      ).toEqual([
        ['Bãi biển Mỹ Khê', true],
        ['Mì Quảng Bà Mua', true],
      ]);
      expect(list.body.items[0]).not.toHaveProperty('price');

      const food = await get('/me/favorites?category=FOOD').expect(200);
      expect(food.body.items.map((p: Summary) => p.name)).toEqual([
        'Mì Quảng Bà Mua',
      ]);

      const search = await get('/places?q=mi%20quang').expect(200);
      expect(search.body.items[0]).toMatchObject({
        name: 'Mì Quảng Bà Mua',
        isFavorite: true,
      });
      const detail = await get(`/places/${miQuang}`).expect(200);
      expect(detail.body.isFavorite).toBe(true);

      await del(miQuang).expect(204);
      await del(miQuang).expect(204); // removing twice is fine
      expect(
        (await get('/me/favorites')).body.items.map((p: Summary) => p.name),
      ).toEqual(['Bãi biển Mỹ Khê']);
    });

    it('keeps favorites per user', async () => {
      await put(await idOf('dragon-bridge')).expect(204);
      const otherToken = await signUp(t, 'an@example.com');

      const res = await api()
        .get('/api/v1/me/favorites')
        .auth(otherToken, { type: 'bearer' })
        .expect(200);
      expect(res.body.items).toEqual([]);
    });

    it('refuses to save a place that does not exist', async () => {
      await put(999999)
        .expect(404)
        .expect((res) => expect(res.body.code).toBe('PLACE_NOT_FOUND'));
    });
  });

  describe('with Google Places', () => {
    beforeEach(() => {
      t.google.enabled = true;
      t.google.places = [googlePho, googleBeach];
    });

    it('searches Google instead of the sample catalog and stores only place IDs', async () => {
      const first = await get('/places?q=pham%20van%20dong').expect(200);
      expect(first.body.items.map((p: Summary) => [p.name, p.source])).toEqual([
        ['Phở Hồng', 'GOOGLE'],
        ['Bãi biển Phạm Văn Đồng', 'GOOGLE'],
      ]);
      expect(first.body).toMatchObject({ total: null, nextPageToken: null });
      expect(first.body.items[0]).toMatchObject({
        attribution: 'Google Maps',
        category: 'FOOD',
      });
      expect(t.google.searches[0]).toMatchObject({
        query: 'pham van dong',
        pageSize: 20,
      });

      const stored = await t.prisma.place.findMany({
        where: { source: 'GOOGLE' },
      });
      expect(stored).toHaveLength(2);
      expect(stored.every((p) => p.name === null && p.lat === null)).toBe(true);

      // Searching again reuses the same TripFair ids.
      const again = await get('/places?q=pham%20van%20dong').expect(200);
      expect(again.body.items.map((p: Summary) => p.id)).toEqual(
        first.body.items.map((p: Summary) => p.id),
      );
      expect(await t.prisma.place.count({ where: { source: 'GOOGLE' } })).toBe(
        2,
      );
    });

    it('pages with nextPageToken and searches around the user', async () => {
      const res = await get('/places?q=pho&limit=1&lat=16.06&lng=108.2').expect(
        200,
      );
      expect(res.body.nextPageToken).toBe('next-page');

      await get('/places?q=pho&limit=1&pageToken=next-page').expect(200);
      expect(t.google.searches[0].near).toEqual({
        lat: 16.06,
        lng: 108.2,
        radiusMeters: 30000,
      });
      expect(t.google.searches[1].pageToken).toBe('next-page');
    });

    it('browses places around the user without a query', async () => {
      const res = await get('/places?lat=16.06&lng=108.2').expect(200);
      expect(res.body.items).toHaveLength(2);
      expect(t.google.searches[0].query).toBe('điểm tham quan');

      // Without text or a position there is nothing to ask Google.
      const empty = await get('/places').expect(200);
      expect(empty.body.items).toEqual([]);
      expect(t.google.searches).toHaveLength(1);
    });

    it('applies the category filter to Google results', async () => {
      const res = await get('/places?q=pham&category=SCENIC').expect(200);
      expect(res.body.items.map((p: Summary) => p.name)).toEqual([
        'Bãi biển Phạm Văn Đồng',
      ]);
    });

    it('finds places of the category near a searched name', async () => {
      // "pham" finds no place to have fun at, so the filter means "fun near pham".
      const park = {
        ...googleBeach,
        id: 'google-asia-park',
        name: 'Asia Park',
        types: ['amusement_park'],
      };
      t.google.placesForQuery.set('điểm vui chơi gần pham', [park, park]);
      const res = await get('/places?q=pham&category=FUN&limit=1').expect(200);
      expect(res.body.items.map((p: Summary) => p.name)).toEqual(['Asia Park']);
      // The name is searched first (a few pages), then the places near it.
      expect(t.google.searches[0].query).toBe('pham');
      expect(t.google.searches.at(-1)?.query).toBe('điểm vui chơi gần pham');
      expect(res.body.nextPageToken).toBe('near:next-page');

      // The next page goes to the nearby query, with Google's own token.
      await get(
        '/places?q=pham&category=FUN&limit=1&pageToken=near:next-page',
      ).expect(200);
      expect(t.google.searches.at(-1)).toMatchObject({
        query: 'điểm vui chơi gần pham',
        pageToken: 'next-page',
      });

      // A category the name itself matches is searched once, as typed.
      t.google.searches.length = 0;
      await get('/places?q=pham&category=SCENIC').expect(200);
      expect(t.google.searches.map((s) => s.query)).toEqual(['pham']);

      // The history keeps what the user typed.
      const history = await get('/me/search-history').expect(200);
      expect(
        history.body.items.every((h: { query: string }) => h.query === 'pham'),
      ).toBe(true);
    });

    it('suggests Google places while typing', async () => {
      const res = await get(
        `/places/suggestions?q=${encodeURIComponent('Phở')}`,
      ).expect(200);
      expect(res.body).toEqual([
        {
          id: expect.any(Number),
          name: 'Phở Hồng',
          area: '10 Lê Duẩn, Hải Châu',
        },
      ]);
    });

    it('fetches details live and prices them with the estimate chain', async () => {
      const search = await get('/places?q=pho').expect(200);
      const pho = search.body.items.find((p: Summary) => p.name === 'Phở Hồng');

      const res = await get(`/places/${pho.id}`).expect(200);
      expect(res.body).toMatchObject({
        source: 'GOOGLE',
        visitMinutes: 60,
        openingHours: { opensAt: '06:30', closesAt: '21:00' },
        price: {
          min: 50000,
          max: 150000,
          source: 'ESTIMATED',
          provider: 'TripFair',
          isEstimate: true,
        },
        mapsUri: 'https://maps.google.com/?cid=1',
        attribution: 'Google Maps',
      });

      await put(pho.id).expect(204);
      const favorites = await get('/me/favorites?category=FOOD').expect(200);
      expect(favorites.body.items.map((p: Summary) => p.name)).toEqual([
        'Phở Hồng',
      ]);
    });

    function put(id: number) {
      return api()
        .put(`/api/v1/me/favorites/${id}`)
        .auth(token, { type: 'bearer' });
    }
  });
});
