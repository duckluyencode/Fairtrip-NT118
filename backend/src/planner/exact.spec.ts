import { solveExact } from './exact.js';
import { planTrip } from './plan.js';
import { BA_NA, DA_NANG, GOLDEN_BRIDGE, input } from './planner.fixtures.js';
import { scheduleDay } from './schedule.js';
import type { PlannerInput, PlannerResult } from './types.js';

function expectValid(trip: PlannerInput, result: PlannerResult) {
  for (const member of trip.members) {
    const spent = result.stops.reduce(
      (s, stop) => s + (stop.costByMember[member.id] ?? 0),
      0,
    );
    expect(spent).toBeLessThanOrEqual(member.available);
  }
  for (let day = 1; day <= trip.days; day++) {
    const order = result.stops
      .filter((s) => s.day === day)
      .sort((a, b) => a.position - b.position)
      .map((s) => trip.places.find((p) => p.id === s.placeId)!);
    expect(scheduleDay(order, trip)).not.toBeNull();
  }
}

describe('solveExact', () => {
  it('is never worse than the heuristic on total value', () => {
    const trips = [
      input(),
      input({
        members: [
          { id: 1, available: 2_000_000, interests: ['FUN', 'SCENERY'] },
          { id: 2, available: 100_000, interests: ['FOOD', 'BEACH'] },
          { id: 3, available: 0, interests: ['CULTURE'] },
        ],
      }),
      input({ days: 1, dayEndMinute: 900 }),
    ];
    for (const trip of trips) {
      const exact = solveExact(trip);
      const heuristic = planTrip(trip);
      expectValid(trip, exact);
      expect(exact.totalValue).toBeGreaterThanOrEqual(
        heuristic.totalValue - 1e-9,
      );
    }
  });

  it('applies ticket coverage like the heuristic', () => {
    const trip = input({
      days: 1,
      places: [BA_NA, GOLDEN_BRIDGE],
      members: [{ id: 1, available: 1_000_000, interests: ['SCENERY'] }],
    });
    const result = solveExact(trip);
    expect(result.stops.map((s) => s.placeId).sort((a, b) => a - b)).toEqual([
      BA_NA.id,
      GOLDEN_BRIDGE.id,
    ]);
    expect(result.members[0].spent).toBe(BA_NA.cost);
  });

  it('plans nothing for a trip with no days and refuses large instances', () => {
    expect(solveExact(input({ days: 0 })).stops).toEqual([]);
    const many = Array.from({ length: 13 }, (_, i) => ({
      ...DA_NANG[0],
      id: 100 + i,
    }));
    expect(() => solveExact(input({ places: many }))).toThrow();
  });
});
