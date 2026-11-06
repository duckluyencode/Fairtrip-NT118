import request from 'supertest';
import type { GooglePlace } from '../src/places/google/google-places.client.js';
import {
  createTestApp,
  resetDatabase,
  signUp,
  type TestApp,
} from './utils/test-app.js';

// Smallest valid file headers: enough for the server's type check, Vision is faked.
const JPEG = Buffer.from([
  0xff, 0xd8, 0xff, 0xe0, 0, 0x10, 0x4a, 0x46, 0x49, 0x46,
]);
const NOT_AN_IMAGE = Buffer.from('%PDF-1.7 not a photo');

const DRAGON_BRIDGE: GooglePlace = {
  id: 'g-dragon-bridge',
  name: 'Cầu Rồng',
  address: 'Nguyễn Văn Linh, Đà Nẵng',
  lat: 16.0612,
  lng: 108.2272,
  types: ['bridge', 'tourist_attraction'],
  rating: 4.7,
  ratingCount: 30000,
  priceLevel: null,
  priceRange: null,
  todayHours: { allDay: true },
  weeklyHours: null,
  summary: null,
  mapsUri: null,
};

describe('Emergency contacts and photo recognition (e2e)', () => {
  let t: TestApp;
  let token: string;
  const api = () => request(t.app.getHttpServer());
  const auth = (r: request.Test, as = token) => r.auth(as, { type: 'bearer' });
  const contacts = '/api/v1/me/emergency-contacts';

  beforeAll(async () => {
    t = await createTestApp();
  });

  beforeEach(async () => {
    await resetDatabase(t.prisma);
    t.otp.sent.length = 0;
    t.google.reset();
    t.vision.reset();
    token = await signUp(t);
  });

  afterAll(async () => {
    await t.app.close();
  });

  describe('emergency contacts', () => {
    it('adds, lists, edits and removes contacts with phones in E.164', async () => {
      const mom = await auth(api().post(contacts))
        .send({
          name: '  Nguyễn  Thị Lan ',
          relation: 'Mẹ',
          phone: '0905 128 846',
        })
        .expect(201);
      expect(mom.body).toMatchObject({
        name: 'Nguyễn Thị Lan',
        relation: 'Mẹ',
        phone: '+84905128846',
        receivesSos: true,
      });
      await auth(api().post(contacts))
        .send({ name: 'Minh', phone: '+65 8123 4567', receivesSos: false })
        .expect(201);

      const list = await auth(api().get(contacts)).expect(200);
      expect(list.body.limit).toBe(5);
      expect(
        list.body.items.map((c: { phone: string; receivesSos: boolean }) => [
          c.phone,
          c.receivesSos,
        ]),
      ).toEqual([
        ['+84905128846', true],
        ['+6581234567', false],
      ]);

      const edited = await auth(api().patch(`${contacts}/${mom.body.id}`))
        .send({ relation: null, receivesSos: false })
        .expect(200);
      expect(edited.body).toMatchObject({ relation: null, receivesSos: false });

      await auth(api().delete(`${contacts}/${mom.body.id}`)).expect(204);
      await auth(api().delete(`${contacts}/${mom.body.id}`))
        .expect(404)
        .expect((res) => expect(res.body.code).toBe('CONTACT_NOT_FOUND'));
    });

    it('rejects bad numbers, duplicates and more than five contacts', async () => {
      for (const phone of ['12345', '+84 12', 'abc']) {
        await auth(api().post(contacts))
          .send({ name: 'An', phone })
          .expect(400)
          .expect((res) => expect(res.body.code).toBe('VALIDATION_FAILED'));
      }
      await auth(api().post(contacts))
        .send({ name: 'An', phone: '0905128846' })
        .expect(201);
      await auth(api().post(contacts))
        .send({ name: 'An again', phone: '+84905128846' })
        .expect(409)
        .expect((res) => expect(res.body.code).toBe('CONTACT_EXISTS'));
      for (let i = 1; i <= 4; i++) {
        await auth(api().post(contacts))
          .send({ name: `Bạn ${i}`, phone: `090512884${i}` })
          .expect(201);
      }
      await auth(api().post(contacts))
        .send({ name: 'Một người nữa', phone: '0911111111' })
        .expect(409)
        .expect((res) => expect(res.body.code).toBe('CONTACT_LIMIT_REACHED'));
    });

    it("keeps each user's contacts private", async () => {
      const mine = await auth(api().post(contacts))
        .send({ name: 'An', phone: '0905128846' })
        .expect(201);
      const other = await signUp(t, 'an@example.com');

      const list = await auth(api().get(contacts), other).expect(200);
      expect(list.body.items).toEqual([]);
      await auth(api().patch(`${contacts}/${mine.body.id}`), other)
        .send({ name: 'Hacked' })
        .expect(404);
      await auth(api().delete(`${contacts}/${mine.body.id}`), other).expect(
        404,
      );
    });
  });

  describe('photo recognition', () => {
    const recognize = () => auth(api().post('/api/v1/places/recognize'));

    it('finds the landmark on Google Places, without prices', async () => {
      t.vision.enabled = true;
      t.google.enabled = true;
      t.google.places = [DRAGON_BRIDGE];
      t.vision.found = [
        {
          name: 'Dragon Bridge',
          score: 0.874,
          location: { lat: 16.0611, lng: 108.2271 },
        },
        { name: 'Somewhere blurry', score: 0.1, location: null },
      ];

      const res = await recognize()
        .attach('image', JPEG, {
          filename: 'photo.jpg',
          contentType: 'image/jpeg',
        })
        .expect(200);

      expect(t.vision.received).toEqual([JPEG.length]);
      expect(t.google.searches[0]).toMatchObject({
        query: 'Dragon Bridge',
        near: { lat: 16.0611, lng: 108.2271, radiusMeters: 2000 },
      });
      expect(res.body.attribution).toBe('Google Maps');
      expect(res.body.items).toHaveLength(1);
      expect(res.body.items[0]).toMatchObject({
        landmark: 'Dragon Bridge',
        confidence: 0.87,
        place: { name: 'Cầu Rồng', source: 'GOOGLE', isFavorite: false },
      });
      expect(res.body.items[0].place).not.toHaveProperty('price');
    });

    it('returns no items when nothing is recognized', async () => {
      t.vision.enabled = true;
      t.google.enabled = true;
      const res = await recognize()
        .attach('image', JPEG, {
          filename: 'photo.jpg',
          contentType: 'image/jpeg',
        })
        .expect(200);
      expect(res.body).toEqual({ items: [], attribution: null });
    });

    it('rejects missing, wrong and oversized files', async () => {
      await recognize()
        .expect(400)
        .expect((res) => expect(res.body.code).toBe('INVALID_IMAGE'));
      await recognize()
        .attach('image', NOT_AN_IMAGE, {
          filename: 'photo.jpg',
          contentType: 'image/jpeg',
        })
        .expect(400)
        .expect((res) => expect(res.body.code).toBe('INVALID_IMAGE'));
      await recognize()
        .attach('image', Buffer.concat([JPEG, Buffer.alloc(4 * 1024 * 1024)]), {
          filename: 'big.jpg',
          contentType: 'image/jpeg',
        })
        .expect(413)
        .expect((res) => expect(res.body.code).toBe('PAYLOAD_TOO_LARGE'));
    });

    it('reports 503 when Cloud Vision is not configured', async () => {
      await recognize()
        .attach('image', JPEG, {
          filename: 'photo.jpg',
          contentType: 'image/jpeg',
        })
        .expect(503)
        .expect((res) => expect(res.body.code).toBe('RECOGNITION_UNAVAILABLE'));
    });
  });
});
