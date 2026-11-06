import { planTrip } from './plan.js';
import {
  BA_NA,
  DA_NANG,
  GOLDEN_BRIDGE,
  input,
  MY_KHE,
  NIGHT_MARKET,
  place,
} from './planner.fixtures.js';
import { scheduleDay } from './schedule.js';
import type { PlannerInput, PlannerResult } from './types.js';

const byId = new Map(DA_NANG.map((p) => [p.id, p]));

/** Every rule a plan must keep, checked on the result. */
function expectValidPlan(trip: PlannerInput, result: PlannerResult) {
  for (const member of trip.members) {
    const spent = result.stops.reduce(
      (s, stop) => s + (stop.costByMember[member.id] ?? 0),
      0,
    );
    expect(spent).toBeLessThanOrEqual(member.available);
    expect(result.members.find((m) => m.memberId === member.id)?.spent).toBe(
      spent,
    );
  }
  for (let day = 1; day <= trip.days; day++) {
    const order = result.stops
      .filter((s) => s.day === day)
      .sort((a, b) => a.position - b.position)
      .map((s) => trip.places.find((p) => p.id === s.placeId)!);
    expect(scheduleDay(order, trip)).not.toBeNull();
  }
  expect(new Set(result.stops.map((s) => s.placeId)).size).toBe(
    result.stops.length,
  );
}

describe('planTrip', () => {
  it('plans a solo trip within budget, time window and opening hours', () => {
    const trip = input();
    const result = planTrip(trip);

    expect(result.stops.length).toBeGreaterThan(3);
    expectValidPlan(trip, result);
    for (const stop of result.stops) {
      const p = byId.get(stop.placeId)!;
      if (p.openMinute !== null) {
        expect(stop.startMinute).toBeGreaterThanOrEqual(p.openMinute);
      }
    }
    expect(result.fairness).toBe(1);
  });

  it('with a 0 VND budget picks only free places and does not divide by zero', () => {
    const trip = input({
      members: [{ id: 1, available: 0, interests: ['FOOD'] }],
    });
    const result = planTrip(trip);

    expect(result.stops.length).toBeGreaterThan(0);
    for (const stop of result.stops) {
      expect(byId.get(stop.placeId)!.cost).toBe(0);
    }
    expect(Number.isFinite(result.totalSatisfaction)).toBe(true);
  });

  it('puts free places before paid ones', () => {
    const trip = input({ days: 1, places: [NIGHT_MARKET, MY_KHE] });
    const result = planTrip(trip, { localSearch: false, fairness: false });
    // Both fit; the free beach was picked first, so the evening market follows it.
    expect(result.stops.map((s) => s.placeId)).toEqual([
      MY_KHE.id,
      NIGHT_MARKET.id,
    ]);
  });

  it('never schedules a visit outside opening hours', () => {
    const trip = input({ days: 1, places: [NIGHT_MARKET] });
    const [stop] = planTrip(trip).stops;
    expect(stop.startMinute).toBeGreaterThanOrEqual(18 * 60);
  });

  it('lets members skip a stop they cannot afford and charges the rest', () => {
    const trip = input({
      days: 1,
      places: [BA_NA, MY_KHE],
      members: [
        { id: 1, available: 1_000_000, interests: ['FUN'] },
        { id: 2, available: 100_000, interests: ['FUN'] },
      ],
    });
    const result = planTrip(trip);
    const baNa = result.stops.find((s) => s.placeId === BA_NA.id)!;

    expect(baNa.participants).toEqual([1]);
    expect(baNa.costByMember).toEqual({ 1: 900_000 });
    expect(result.members.find((m) => m.memberId === 2)!.spent).toBe(0);
    expectValidPlan(trip, result);
    expect(result.fairness).toBeLessThan(1);
  });

  it('makes a place free for members who visit the ticketed area around it', () => {
    const trip = input({
      days: 1,
      places: [BA_NA, GOLDEN_BRIDGE],
      members: [{ id: 1, available: 1_000_000, interests: ['SCENERY'] }],
    });
    const result = planTrip(trip);
    const ids = result.stops.map((s) => s.placeId).sort((a, b) => a - b);

    expect(ids).toEqual([BA_NA.id, GOLDEN_BRIDGE.id]);
    expect(result.members[0].spent).toBe(900_000);
    expect(
      result.stops.find((s) => s.placeId === GOLDEN_BRIDGE.id)!.costByMember[1],
    ).toBe(0);
  });

  it('keeps every day inside its time window', () => {
    const many = Array.from({ length: 30 }, (_, i) =>
      place(100 + i, { lat: 16.06 + i * 0.001, visitMinutes: 90 }),
    );
    const trip = input({ days: 1, places: many });
    const result = planTrip(trip);

    expect(result.stops.length).toBeLessThan(30);
    expectValidPlan(trip, result);
  });

  it('prefers places the members voted for', () => {
    const a = place(201, {
      lat: 16.05,
      interests: ['FOOD'],
      visitMinutes: 600,
    });
    const b = place(202, {
      lat: 16.05,
      interests: ['FOOD'],
      visitMinutes: 600,
      votes: [1],
    });
    const result = planTrip(input({ days: 1, places: [a, b] }));
    expect(result.stops.map((s) => s.placeId)).toEqual([b.id]);
  });

  it('local search does not make a plan worse', () => {
    const trip = input({
      members: [
        { id: 1, available: 1_500_000, interests: ['FOOD', 'BEACH'] },
        { id: 2, available: 900_000, interests: ['CULTURE', 'SCENERY'] },
        { id: 3, available: 300_000, interests: ['FUN'] },
      ],
    });
    const greedyOnly = planTrip(trip, { localSearch: false, fairness: false });
    const full = planTrip(trip);

    expectValidPlan(trip, greedyOnly);
    expectValidPlan(trip, full);
    expect(full.stops.length).toBeGreaterThanOrEqual(greedyOnly.stops.length);
  });

  it('never puts a place on a day it is closed', () => {
    const museum = { ...DA_NANG.find((p) => p.id === 7)!, closedDays: [1] };
    const trip = input({
      members: [{ id: 1, available: 2_000_000, interests: ['CULTURE'] }],
      places: DA_NANG.map((p) => (p.id === 7 ? museum : p)),
    });
    const result = planTrip(trip);

    const visit = result.stops.find((s) => s.placeId === 7);
    expect(visit?.day).toBe(2);
    expectValidPlan(trip, result);
  });

  it('returns an empty plan for a trip with no days', () => {
    expect(planTrip(input({ days: 0 })).stops).toEqual([]);
  });
});
