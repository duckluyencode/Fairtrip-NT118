import { planTrip } from './plan.js';
import { input, place } from './planner.fixtures.js';
import { MEAL_WINDOWS, scheduleDay, simulateDay } from './schedule.js';

// All at the base, so travel takes 0 minutes and only the clock rules matter.
const settings = {
  dayStartMinute: 480,
  dayEndMinute: 1260,
  gapMinutes: 0,
  base: { lat: 16.06, lng: 108.22 },
};
const meal = (id: number) => place(id, { meal: true, visitMinutes: 60 });
const sight = (id: number, visitMinutes: number) => place(id, { visitMinutes });

describe('meals', () => {
  it('waits for lunch, then for dinner', () => {
    const day = simulateDay([meal(1), sight(2, 60), meal(3)], settings);
    expect(day.visits.map((v) => v.startMinute)).toEqual([660, 720, 1050]);
    expect(day.closedPlaceIds).toEqual([]);
  });

  it('moves a meal to dinner when lunch time is over', () => {
    const day = simulateDay([sight(1, 360), meal(2)], settings);
    // The sight ends at 14:00, after the last lunch start (13:30).
    expect(day.visits[1].startMinute).toBe(MEAL_WINDOWS[1].from);
  });

  it('refuses a third meal in one day and a meal after dinner time', () => {
    expect(scheduleDay([meal(1), meal(2), meal(3)], settings)).toBeNull();
    expect(
      scheduleDay([sight(1, 780), meal(2)], {
        ...settings,
        dayEndMinute: 1440,
      }),
    ).toBeNull();
  });

  it('respects the restaurant opening hours within meal times', () => {
    // Opens at 17:00: no lunch there, dinner from 17:30.
    const evening = place(1, {
      meal: true,
      openMinute: 1020,
      closeMinute: 1320,
    });
    expect(simulateDay([evening], settings).visits[0].startMinute).toBe(1050);
  });

  it('plans restaurants only at meal times, at most one per meal and day', () => {
    const restaurants = [1, 2, 3, 4, 5].map((id) =>
      place(id, {
        lng: 108.22 + id / 1000,
        meal: true,
        interests: ['FOOD'],
        cost: 50_000,
      }),
    );
    const sights = [6, 7, 8].map((id) =>
      place(id, { lng: 108.22 + id / 1000, interests: ['SCENERY'] }),
    );
    const result = planTrip(
      input({
        days: 2,
        places: [...restaurants, ...sights],
        members: [{ id: 1, available: 1_000_000, interests: ['FOOD'] }],
      }),
    );
    const meals = result.stops.filter((s) => s.placeId <= 5);
    expect(meals.length).toBeGreaterThan(0);
    for (const stop of meals) {
      const inWindow = MEAL_WINDOWS.some(
        (w) => stop.startMinute >= w.from && stop.startMinute <= w.lastStart,
      );
      expect(inWindow).toBe(true);
    }
    for (const day of [1, 2]) {
      expect(meals.filter((s) => s.day === day).length).toBeLessThanOrEqual(2);
    }
  });
});
