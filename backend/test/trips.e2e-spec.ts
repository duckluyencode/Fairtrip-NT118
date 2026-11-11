import request from 'supertest';
import { seedCatalog } from '../prisma/seed/catalog.js';
import type {
  DayHours,
  GooglePlace,
} from '../src/places/google/google-places.client.js';
import {
  createTestApp,
  resetDatabase,
  signUp,
  type TestApp,
} from './utils/test-app.js';

// 2026-12-01 is a Tuesday.
const START = '2026-12-01';
const END = '2026-12-02';

interface Stop {
  placeId: number;
  name: string | null;
  time: string;
  travelMinutes: number;
  myCost: number | null;
  price: { perPerson: number; source: string; isEstimate: boolean };
  participants: { memberId: number }[];
  runningTotal: number;
  remaining: number;
}
interface Itinerary {
  status: string;
  budgetIncludes: string[];
  days: { day: number; date: string; stops: Stop[] }[];
  me: {
    budget: number;
    lodgingShare: number;
    spentOnPlaces: number;
    total: number;
    remaining: number;
  };
  members: { memberId: number; spent: number; satisfaction: number | null }[];
  fairness: number | null;
  warnings: { code: string; day?: number; placeId?: number }[];
  lodging: { name: string | null; myShare: number } | null;
}

const everyDay = (hours: DayHours) => Array.from({ length: 7 }, () => hours);

function googlePlace(
  id: string,
  name: string,
  types: string[],
  extra: Partial<GooglePlace> = {},
): GooglePlace {
  return {
    id,
    name,
    address: `${name}, Đà Nẵng`,
    lat: 16.06,
    lng: 108.22,
    types,
    rating: 4.5,
    ratingCount: 1000,
    priceLevel: null,
    priceRange: null,
    todayHours: { allDay: true },
    weeklyHours: everyDay({ allDay: true }),
    summary: null,
    mapsUri: null,
    ...extra,
  };
}

const HOTEL_CHEAP = googlePlace('g-hotel-cheap', 'Nhà nghỉ An', ['lodging'], {
  priceLevel: 'PRICE_LEVEL_INEXPENSIVE', // ~500.000đ a night
  rating: 3.8,
  lat: 16.03,
  lng: 108.25,
});
const HOTEL_MID = googlePlace(
  'g-hotel-mid',
  'Khách sạn Biển Xanh',
  ['lodging', 'hotel'],
  {
    priceLevel: 'PRICE_LEVEL_MODERATE', // ~1.000.000đ
    rating: 4.7,
    lat: 16.055,
    lng: 108.24,
  },
);
const HOTEL_LUXURY = googlePlace(
  'g-hotel-luxury',
  'Resort Sơn Trà',
  ['lodging'],
  {
    priceLevel: 'PRICE_LEVEL_VERY_EXPENSIVE', // ~6.000.000đ
    rating: 4.9,
    lat: 16.06,
    lng: 108.225,
  },
);
const BEACH = googlePlace(
  'g-beach',
  'Bãi biển Mỹ An',
  ['beach', 'tourist_attraction'],
  {
    lat: 16.05,
    lng: 108.247,
  },
);
// Closed on Tuesdays (the first day of the trip).
const MUSEUM = googlePlace('g-museum', 'Bảo tàng Đà Nẵng', ['museum'], {
  priceLevel: 'PRICE_LEVEL_INEXPENSIVE',
  lat: 16.07,
  lng: 108.223,
  weeklyHours: everyDay({ allDay: false, open: 480, close: 1020 }).map(
    (hours, weekday) => (weekday === 2 ? { closed: true as const } : hours),
  ),
});
const RESTAURANT = googlePlace(
  'g-pho',
  'Phở Hồng',
  ['vietnamese_restaurant', 'restaurant'],
  {
    priceLevel: 'PRICE_LEVEL_MODERATE', // ~150.000đ a person
    lat: 16.068,
    lng: 108.215,
    weeklyHours: everyDay({ allDay: false, open: 390, close: 1260 }),
  },
);

describe('Trips, groups, voting, lodging and itineraries (e2e)', () => {
  let t: TestApp;
  let owner: string;
  let friend: string;
  const api = () => request(t.app.getHttpServer());
  const as = (token: string) => ({
    get: (path: string) =>
      api().get(`/api/v1${path}`).auth(token, { type: 'bearer' }),
    post: (path: string, body?: object) =>
      api().post(`/api/v1${path}`).auth(token, { type: 'bearer' }).send(body),
    put: (path: string, body?: object) =>
      api().put(`/api/v1${path}`).auth(token, { type: 'bearer' }).send(body),
    patch: (path: string, body?: object) =>
      api().patch(`/api/v1${path}`).auth(token, { type: 'bearer' }).send(body),
    delete: (path: string) =>
      api().delete(`/api/v1${path}`).auth(token, { type: 'bearer' }),
  });

  async function idOf(slug: string): Promise<number> {
    return (await t.prisma.place.findUniqueOrThrow({ where: { slug } })).id;
  }

  async function createTrip(token: string, body: object = {}) {
    const res = await as(token)
      .post('/trips', {
        type: 'SOLO',
        city: 'Đà Nẵng',
        startDate: START,
        endDate: END,
        budget: 1_500_000,
        interests: ['BEACH', 'FOOD'],
        ...body,
      })
      .expect(201);
    return res.body as {
      id: number;
      inviteCode: string | null;
      members: { memberId: number; isMe: boolean }[];
    };
  }

  async function createGroup(body: object = {}) {
    const group = await createTrip(owner, {
      type: 'GROUP',
      name: 'Nhóm Đà Nẵng',
      budget: 3_000_000,
      ...body,
    });
    await as(friend)
      .post('/trips/join', {
        inviteCode: group.inviteCode,
        budget: 600_000,
        interests: ['CULTURE'],
      })
      .expect(201);
    return group;
  }

  /** Every rule an itinerary must keep, from the member's point of view. */
  function expectConsistent(itinerary: Itinerary) {
    let running = itinerary.me.lodgingShare;
    for (const day of itinerary.days) {
      for (const stop of day.stops) {
        running += stop.myCost ?? 0;
        expect(stop.runningTotal).toBe(running);
        expect(stop.remaining).toBe(itinerary.me.budget - running);
      }
    }
    expect(itinerary.me.total).toBe(running);
    expect(itinerary.me.remaining).toBe(itinerary.me.budget - running);
    expect(itinerary.budgetIncludes).toEqual(['TICKETS', 'FOOD', 'LODGING']);
  }

  const stopsOf = (itinerary: Itinerary) =>
    itinerary.days.flatMap((d) => d.stops);

  beforeAll(async () => {
    t = await createTestApp();
  });

  beforeEach(async () => {
    await resetDatabase(t.prisma);
    await seedCatalog(t.prisma);
    t.otp.sent.length = 0;
    t.google.reset();
    t.routes.reset();
    owner = await signUp(t, 'duc@example.com');
    friend = await signUp(t, 'an@example.com');
  });

  afterAll(async () => {
    await t.app.close();
  });

  describe('trips', () => {
    it('creates a solo trip with default times', async () => {
      const trip = await createTrip(owner);
      const res = await as(owner).get(`/trips/${trip.id}`).expect(200);
      expect(res.body).toMatchObject({
        type: 'SOLO',
        city: 'Đà Nẵng',
        startDate: START,
        endDate: END,
        days: 2,
        status: 'DRAFT',
        myRole: 'OWNER',
        dayStart: '08:00',
        dayEnd: '21:00',
        gapMinutes: 30,
        lodgingMode: 'SELF',
        inviteCode: null,
        lodging: null,
      });
      expect(res.body.members).toEqual([
        expect.objectContaining({
          isMe: true,
          budget: 1_500_000,
          interests: ['FOOD', 'BEACH'],
          spent: null,
        }),
      ]);
      const list = await as(owner).get('/trips').expect(200);
      expect(list.body.map((x: { id: number }) => x.id)).toEqual([trip.id]);
    });

    it('rejects impossible dates and day windows', async () => {
      for (const body of [
        { startDate: END, endDate: START },
        { endDate: '2026-12-15' }, // 15 days
        { dayStart: '20:00', dayEnd: '09:00' },
      ]) {
        await as(owner)
          .post('/trips', {
            type: 'SOLO',
            city: 'Đà Nẵng',
            startDate: START,
            endDate: END,
            budget: 0,
            interests: [],
            ...body,
          })
          .expect(400)
          .expect((res) => expect(res.body.code).toBe('INVALID_TRIP_DATES'));
      }
      await as(owner)
        .post('/trips', {
          type: 'GROUP',
          city: 'Đà Nẵng',
          startDate: START,
          endDate: END,
          budget: 0,
          interests: [],
        })
        .expect(400)
        .expect((res) => expect(res.body.code).toBe('VALIDATION_FAILED'));
    });

    it('hides trips from people outside them', async () => {
      const trip = await createTrip(owner);
      await as(friend)
        .get(`/trips/${trip.id}`)
        .expect(404)
        .expect((res) => expect(res.body.code).toBe('TRIP_NOT_FOUND'));
      await as(friend).delete(`/trips/${trip.id}`).expect(404);
      await as(owner).delete(`/trips/${trip.id}`).expect(204);
      await as(owner).get(`/trips/${trip.id}`).expect(404);
    });
  });

  describe('groups', () => {
    it('joins by invite code, keeps budgets private and lets members leave', async () => {
      const group = await createGroup();
      expect(group.inviteCode).toMatch(/^[A-HJ-NP-Z2-9]{6}$/);

      const seen = await as(friend).get(`/trips/${group.id}`).expect(200);
      expect(seen.body.myRole).toBe('MEMBER');
      expect(
        seen.body.members.map((m: { isMe: boolean; budget: number | null }) => [
          m.isMe,
          m.budget,
        ]),
      ).toEqual([
        [false, null],
        [true, 600_000],
      ]);

      await as(friend)
        .post('/trips/join', {
          inviteCode: group.inviteCode?.toLowerCase(),
          budget: 0,
          interests: [],
        })
        .expect(409)
        .expect((res) => expect(res.body.code).toBe('ALREADY_MEMBER'));
      await as(friend)
        .post('/trips/join', { inviteCode: 'ZZZZZZ', budget: 0, interests: [] })
        .expect(404)
        .expect((res) => expect(res.body.code).toBe('INVITE_CODE_NOT_FOUND'));

      await as(owner)
        .delete(`/trips/${group.id}/me`)
        .expect(409)
        .expect((res) => expect(res.body.code).toBe('OWNER_CANNOT_LEAVE'));
      await as(friend)
        .delete(`/trips/${group.id}`)
        .expect(403)
        .expect((res) => expect(res.body.code).toBe('NOT_TRIP_OWNER'));
      await as(friend).delete(`/trips/${group.id}/me`).expect(204);
      await as(friend).get(`/trips/${group.id}`).expect(404);
    });
  });

  describe('managing a group', () => {
    it('lets the owner remove a member, hand over the group and renew the code', async () => {
      const group = await createGroup();
      const detail = (await as(owner).get(`/trips/${group.id}`).expect(200))
        .body as { members: { memberId: number; isMe: boolean }[] };
      const mine = detail.members.find((m) => m.isMe)!.memberId;
      const theirs = detail.members.find((m) => !m.isMe)!.memberId;

      await as(friend)
        .post(`/trips/${group.id}/invite-code`)
        .expect(403)
        .expect((res) => expect(res.body.code).toBe('NOT_TRIP_OWNER'));
      const renewed = await as(owner)
        .post(`/trips/${group.id}/invite-code`)
        .expect(200);
      expect(renewed.body.inviteCode).not.toBe(group.inviteCode);
      await as(friend)
        .post('/trips/join', {
          inviteCode: group.inviteCode,
          budget: 0,
          interests: [],
        })
        .expect(404);

      await as(owner)
        .delete(`/trips/${group.id}/members/${mine}`)
        .expect(409)
        .expect((res) => expect(res.body.code).toBe('OWNER_CANNOT_LEAVE'));
      await as(owner)
        .put(`/trips/${group.id}/owner`, { memberId: 999999 })
        .expect(404)
        .expect((res) => expect(res.body.code).toBe('MEMBER_NOT_FOUND'));

      const handed = await as(owner)
        .put(`/trips/${group.id}/owner`, { memberId: theirs })
        .expect(200);
      expect(handed.body.myRole).toBe('MEMBER');
      await as(owner)
        .delete(`/trips/${group.id}/members/${theirs}`)
        .expect(403);

      // The new owner takes the old one out of the group.
      await as(friend).delete(`/trips/${group.id}/members/${mine}`).expect(204);
      await as(owner).get(`/trips/${group.id}`).expect(404);
      const solo = await createTrip(owner);
      await as(owner)
        .post(`/trips/${solo.id}/invite-code`)
        .expect(400)
        .expect((res) => expect(res.body.code).toBe('NOT_A_GROUP'));
    });

    it('hands a group over to the next member when its owner deletes their account', async () => {
      const group = await createGroup();
      const solo = await createTrip(owner);
      await as(owner).post(`/trips/${group.id}/plan`).expect(200);

      await as(owner)
        .delete('/me')
        .send({ password: 'matkhau123' })
        .expect(204);

      // The owner's solo trip is gone; the group lives on with the friend as owner.
      expect(await t.prisma.trip.count({ where: { id: solo.id } })).toBe(0);
      const left = await as(friend).get(`/trips/${group.id}`).expect(200);
      expect(left.body).toMatchObject({
        myRole: 'OWNER',
        memberCount: 1,
        status: 'DRAFT',
      });
    });
  });

  describe('voting', () => {
    it('proposes places, counts votes and locks a sent ballot', async () => {
      const group = await createGroup();
      const museum = await idOf('cham-museum');
      const beach = await idOf('my-khe-beach');
      const base = `/trips/${group.id}/options`;

      await as(friend).post(base, { placeId: museum }).expect(201);
      await as(friend).post(base, { placeId: museum }).expect(201); // no duplicate
      const list = await as(owner).post(base, { placeId: beach }).expect(201);
      expect(list.body.items).toHaveLength(2);

      const museumOption = list.body.items.find(
        (o: { placeId: number }) => o.placeId === museum,
      );
      expect(museumOption).toMatchObject({
        name: 'Bảo tàng Điêu khắc Chăm',
        votes: 1,
        votedByMe: false,
        addedByMe: false,
        price: { max: 60000, source: 'ESTIMATED', isEstimate: true },
      });

      const voted = await as(owner)
        .put(`${base}/${museumOption.id}/vote`)
        .expect(200);
      await as(owner).put(`${base}/${museumOption.id}/vote`).expect(200);
      expect(voted.body.items[0]).toMatchObject({
        placeId: museum,
        votes: 2,
        votedByMe: true,
      });

      // Only who proposed it (or the owner) can take it back.
      const beachOption = voted.body.items.find(
        (o: { placeId: number }) => o.placeId === beach,
      );
      await as(friend)
        .delete(`${base}/${beachOption.id}`)
        .expect(403)
        .expect((res) => expect(res.body.code).toBe('NOT_TRIP_OWNER'));

      const sent = await as(friend)
        .post(`/trips/${group.id}/ballot`)
        .expect(200);
      expect(sent.body).toMatchObject({ memberCount: 2, ballotsSent: 1 });
      await as(friend)
        .delete(`${base}/${museumOption.id}/vote`)
        .expect(409)
        .expect((res) => expect(res.body.code).toBe('BALLOT_ALREADY_SENT'));

      await as(owner).delete(`${base}/${beachOption.id}`).expect(204);
      await as(owner)
        .put(`${base}/${beachOption.id}/vote`)
        .expect(404)
        .expect((res) => expect(res.body.code).toBe('OPTION_NOT_FOUND'));
    });

    it('closes voting after the deadline', async () => {
      const group = await createGroup({ voteDeadline: '2026-01-01T00:00:00Z' });
      await as(friend)
        .post(`/trips/${group.id}/options`, {
          placeId: await idOf('my-khe-beach'),
        })
        .expect(409)
        .expect((res) => expect(res.body.code).toBe('VOTING_CLOSED'));
    });
  });

  describe('planning (sample catalog)', () => {
    it('plans a solo trip with prices, running totals and what is left', async () => {
      const trip = await createTrip(owner);
      await as(owner)
        .get(`/trips/${trip.id}/itinerary`)
        .expect(409)
        .expect((res) => expect(res.body.code).toBe('ITINERARY_NOT_READY'));

      const res = await as(owner).post(`/trips/${trip.id}/plan`).expect(200);
      const itinerary = res.body as Itinerary;

      expect(itinerary.status).toBe('PLANNED');
      expect(itinerary.days.map((d) => d.date)).toEqual([START, END]);
      expect(stopsOf(itinerary).length).toBeGreaterThan(3);
      expectConsistent(itinerary);
      expect(itinerary.me.remaining).toBeGreaterThanOrEqual(0);
      expect(itinerary.warnings).toEqual([]);
      expect(itinerary.fairness).toBe(1);
      for (const day of itinerary.days) {
        const times = day.stops.map((s) => s.time);
        expect(times).toEqual([...times].sort());
        expect(times.every((time) => time >= '08:00')).toBe(true);
      }

      const detail = await as(owner).get(`/trips/${trip.id}`).expect(200);
      expect(detail.body.status).toBe('PLANNED');
      expect(detail.body.members[0].spent).toBe(itinerary.me.total);
      expect(detail.body.members[0].satisfaction).toBeGreaterThan(0);
    });

    it('plans a 0 VND trip with free places only', async () => {
      const trip = await createTrip(owner, { budget: 0, lodging: 'HOTEL' });
      const detail = await as(owner).get(`/trips/${trip.id}`).expect(200);
      expect(detail.body.lodgingMode).toBe('SELF');

      const res = await as(owner).post(`/trips/${trip.id}/plan`).expect(200);
      const stops = stopsOf(res.body as Itinerary);
      expect(stops.length).toBeGreaterThan(0);
      expect(
        stops.every((s) => s.price.perPerson === 0 && s.myCost === 0),
      ).toBe(true);
    });

    it('plans from the picked places and keeps every member within budget', async () => {
      const group = await createGroup();
      const picks = await Promise.all(
        [
          'ba-na-hills',
          'golden-bridge',
          'cham-museum',
          'my-khe-beach',
          'mi-quang-ba-mua',
        ].map(idOf),
      );
      for (const placeId of picks) {
        await as(owner)
          .post(`/trips/${group.id}/options`, { placeId })
          .expect(201);
      }

      await as(friend)
        .post(`/trips/${group.id}/plan`)
        .expect(403)
        .expect((res) => expect(res.body.code).toBe('NOT_TRIP_OWNER'));
      const planned = (
        await as(owner).post(`/trips/${group.id}/plan`).expect(200)
      ).body as Itinerary;

      const stops = stopsOf(planned);
      expect(stops.every((s) => picks.includes(s.placeId))).toBe(true);
      expectConsistent(planned);
      const friendView = (
        await as(friend).get(`/trips/${group.id}/itinerary`).expect(200)
      ).body as Itinerary;
      expectConsistent(friendView);
      expect(friendView.me.total).toBeLessThanOrEqual(600_000);
      // 900.000đ Bà Nà Hills is over the friend's budget: the owner goes alone.
      const baNa = stops.find((s) => s.placeId === picks[0]);
      expect(baNa).toBeDefined();
      if (baNa) {
        expect(baNa.participants).toHaveLength(1);
        const friendBaNa = stopsOf(friendView).find(
          (s) => s.placeId === picks[0],
        );
        expect(friendBaNa?.myCost).toBeNull();
      }
      // Cầu Vàng is inside Bà Nà Hills: free for whoever has the Bà Nà ticket.
      const goldenBridge = stops.find((s) => s.placeId === picks[1]);
      expect(goldenBridge?.myCost).toBe(0);
      expect(planned.members.map((m) => m.spent)).toEqual([
        planned.me.total,
        friendView.me.total,
      ]);
      expect(planned.fairness).toBeGreaterThan(0);
      expect(planned.fairness).toBeLessThanOrEqual(1);

      // A change of budget makes the plan stale.
      await as(friend)
        .patch(`/trips/${group.id}/me`, { budget: 800_000 })
        .expect(200);
      await as(owner).get(`/trips/${group.id}/itinerary`).expect(409);
    });
  });

  describe('self-designed itineraries', () => {
    it('saves the given order and warns instead of refusing', async () => {
      const trip = await createTrip(owner, {
        budget: 100_000,
        dayEnd: '18:00',
      });
      const baNa = await idOf('ba-na-hills');
      const golden = await idOf('golden-bridge');
      const asiaPark = await idOf('asia-park');
      const res = await as(owner)
        .put(`/trips/${trip.id}/itinerary`, {
          days: [{ day: 1, placeIds: [baNa, golden, asiaPark] }],
        })
        .expect(200);
      const itinerary = res.body as Itinerary;

      expect(stopsOf(itinerary).map((s) => s.placeId)).toEqual([
        baNa,
        golden,
        asiaPark,
      ]);
      expect(stopsOf(itinerary)[1].myCost).toBe(0); // covered by the Bà Nà ticket
      expectConsistent(itinerary);
      expect(itinerary.warnings.map((w) => w.code).sort()).toEqual([
        'DAY_TOO_LONG',
        'OVER_BUDGET',
      ]);
    });

    it('rejects days outside the trip and repeated places', async () => {
      const trip = await createTrip(owner);
      const beach = await idOf('my-khe-beach');
      for (const days of [
        [{ day: 3, placeIds: [beach] }],
        [
          { day: 1, placeIds: [beach] },
          { day: 2, placeIds: [beach] },
        ],
      ]) {
        await as(owner)
          .put(`/trips/${trip.id}/itinerary`, { days })
          .expect(400)
          .expect((res) => expect(res.body.code).toBe('INVALID_ITINERARY'));
      }
      await as(owner)
        .put(`/trips/${trip.id}/itinerary`, {
          days: [{ day: 1, placeIds: [999999] }],
        })
        .expect(404)
        .expect((res) => expect(res.body.code).toBe('PLACE_NOT_FOUND'));
    });
  });

  describe('with Google Places and Routes', () => {
    beforeEach(() => {
      t.google.enabled = true;
      t.google.places = [
        HOTEL_CHEAP,
        HOTEL_MID,
        HOTEL_LUXURY,
        BEACH,
        MUSEUM,
        RESTAURANT,
      ];
      t.routes.enabled = true;
    });

    it('suggests hotels everyone can afford, then plans from the chosen one', async () => {
      const group = await createGroup({ lodging: 'HOTEL' });

      await as(owner)
        .post(`/trips/${group.id}/plan`)
        .expect(409)
        .expect((res) => expect(res.body.code).toBe('LODGING_NOT_CHOSEN'));

      const options = await as(friend)
        .get(`/trips/${group.id}/lodging/options`)
        .expect(200);
      // The resort (6.000.000đ a night, 3.000.000đ each) is over the friend's 600.000đ.
      expect(
        options.body.map((o: { tag: string; name: string }) => [o.tag, o.name]),
      ).toEqual([
        ['CHEAPEST', 'Nhà nghỉ An'],
        ['BALANCED', 'Khách sạn Biển Xanh'],
      ]);
      expect(options.body[0]).toMatchObject({
        pricePerNight: 500_000,
        priceSource: 'ESTIMATED',
        isEstimate: true,
        nights: 1,
        rooms: 1,
        myShare: 250_000,
        leftForPlaces: 350_000,
        attribution: 'Google Maps',
      });

      const luxury = await t.prisma.place.findUniqueOrThrow({
        where: { googlePlaceId: HOTEL_LUXURY.id },
      });
      await as(owner)
        .put(`/trips/${group.id}/lodging`, {
          mode: 'HOTEL',
          placeId: luxury.id,
        })
        .expect(409)
        .expect((res) => expect(res.body.code).toBe('LODGING_OVER_BUDGET'));
      const beachId = (
        await as(owner).get('/places?q=bien').expect(200)
      ).body.items.find(
        (p: { name: string }) => p.name === 'Bãi biển Mỹ An',
      ).id;
      await as(owner)
        .put(`/trips/${group.id}/lodging`, { mode: 'HOTEL', placeId: beachId })
        .expect(404)
        .expect((res) => expect(res.body.code).toBe('HOTEL_NOT_FOUND'));

      const chosen = await as(owner)
        .put(`/trips/${group.id}/lodging`, {
          mode: 'HOTEL',
          placeId: options.body[0].placeId,
        })
        .expect(200);
      expect(chosen.body).toMatchObject({
        lodgingMode: 'HOTEL',
        lodging: {
          name: 'Nhà nghỉ An',
          checkIn: START,
          checkOut: END,
          nights: 1,
          rooms: 1,
          pricePerNight: 500_000,
          myShare: 250_000,
        },
      });

      const planned = (
        await as(owner).post(`/trips/${group.id}/plan`).expect(200)
      ).body as Itinerary;
      expectConsistent(planned);
      expect(planned.me.lodgingShare).toBe(250_000);
      expect(planned.lodging).toMatchObject({
        name: 'Nhà nghỉ An',
        myShare: 250_000,
      });

      // No places were picked: popular places of the city were searched, hotels left out.
      expect(t.google.searches.map((s) => s.query)).toContain(
        'điểm tham quan nổi tiếng Đà Nẵng',
      );
      const names = stopsOf(planned).map((s) => s.name);
      expect(names).not.toContain('Nhà nghỉ An');
      expect(names).toContain('Bãi biển Mỹ An');
      // The museum is closed on Tuesdays (day 1).
      const museumDay = planned.days.find((d) =>
        d.stops.some((s) => s.name === 'Bảo tàng Đà Nẵng'),
      );
      expect(museumDay?.day).toBe(2);

      // Days start and end at the hotel, timed with Google Routes.
      expect(t.routes.requests.length).toBeGreaterThan(0);
      for (const points of t.routes.requests) {
        expect(points[0]).toEqual({
          lat: HOTEL_CHEAP.lat,
          lng: HOTEL_CHEAP.lng,
        });
        expect(points.at(-1)).toEqual(points[0]);
      }
      expect(stopsOf(planned).every((s) => s.travelMinutes === 20)).toBe(true);

      const friendView = (
        await as(friend).get(`/trips/${group.id}/itinerary`).expect(200)
      ).body as Itinerary;
      expectConsistent(friendView);
      expect(friendView.me.total).toBeLessThanOrEqual(600_000);
    });

    it('prices rooms for the chosen number of people per room', async () => {
      const group = await createGroup({ lodging: 'HOTEL' });

      // Alone in a room, each pays the whole room: only the 500.000đ one fits 600.000đ.
      const single = await as(friend)
        .get(`/trips/${group.id}/lodging/options?guestsPerRoom=1`)
        .expect(200);
      expect(single.body.map((o: { name: string }) => o.name)).toEqual([
        'Nhà nghỉ An',
      ]);
      expect(single.body[0]).toMatchObject({
        rooms: 2,
        guestsPerRoom: 1,
        myShare: 500_000,
      });
      await as(friend)
        .get(`/trips/${group.id}/lodging/options?guestsPerRoom=5`)
        .expect(400);

      const chosen = await as(owner)
        .put(`/trips/${group.id}/lodging`, {
          mode: 'HOTEL',
          placeId: single.body[0].placeId,
          guestsPerRoom: 1,
        })
        .expect(200);
      expect(chosen.body.lodging).toMatchObject({
        rooms: 2,
        guestsPerRoom: 1,
        myShare: 500_000,
      });
    });

    it('drops late stops when real driving times no longer fit the day', async () => {
      const trip = await createTrip(owner, {
        dayStart: '08:00',
        dayEnd: '12:00',
      });
      t.routes.minutesPerLeg = 90;

      const planned = (
        await as(owner).post(`/trips/${trip.id}/plan`).expect(200)
      ).body as Itinerary;

      expect(planned.warnings).toEqual([]);
      for (const day of planned.days) {
        for (const stop of day.stops) {
          expect(stop.travelMinutes).toBe(stop === day.stops[0] ? 0 : 90);
        }
      }
    });

    it('suggests no hotel for a one-day trip', async () => {
      const trip = await createTrip(owner, { endDate: START });
      const options = await as(owner)
        .get(`/trips/${trip.id}/lodging/options`)
        .expect(200);
      expect(options.body).toEqual([]);
      const hotel = await t.prisma.place.create({
        data: { source: 'GOOGLE', googlePlaceId: HOTEL_CHEAP.id },
      });
      await as(owner)
        .put(`/trips/${trip.id}/lodging`, { mode: 'HOTEL', placeId: hotel.id })
        .expect(400)
        .expect((res) => expect(res.body.code).toBe('LODGING_NOT_NEEDED'));
    });
  });
});
