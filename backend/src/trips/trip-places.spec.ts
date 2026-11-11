import { fromDateOnly } from '../common/time.js';
import type { DayHours } from '../places/google/google-places.client.js';
import { hoursForTrip } from './trip-places.js';

// 2026-11-30 is a Monday.
const MONDAY = fromDateOnly('2026-11-30');

describe('hoursForTrip', () => {
  it('lists the trip days a place is closed and keeps its usual hours', () => {
    const week: DayHours[] = Array.from({ length: 7 }, (_, weekday) =>
      weekday === 1
        ? { closed: true }
        : { allDay: false, open: 480, close: 1020 },
    );
    // Monday, Tuesday, Wednesday; then the next Monday is day 8.
    expect(hoursForTrip(week, MONDAY, 3)).toEqual({
      openMinute: 480,
      closeMinute: 1020,
      closedDays: [1],
    });
    expect(hoursForTrip(week, MONDAY, 8).closedDays).toEqual([1, 8]);
  });

  it('treats unknown hours and places open all day as always open', () => {
    expect(hoursForTrip(null, MONDAY, 2)).toEqual({
      openMinute: null,
      closeMinute: null,
      closedDays: [],
    });
    expect(
      hoursForTrip(
        Array.from({ length: 7 }, () => ({ allDay: true as const })),
        MONDAY,
        2,
      ),
    ).toEqual({ openMinute: null, closeMinute: null, closedDays: [] });
  });
});
