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
      dayHours: [null, { open: 480, close: 1020 }, { open: 480, close: 1020 }],
    });
    expect(hoursForTrip(week, MONDAY, 8).closedDays).toEqual([1, 8]);
  });

  it('treats unknown hours and places open all day as always open', () => {
    expect(hoursForTrip(null, MONDAY, 2)).toEqual({
      openMinute: null,
      closeMinute: null,
      closedDays: [],
      dayHours: [null, null],
    });
    expect(
      hoursForTrip(
        Array.from({ length: 7 }, () => ({ allDay: true as const })),
        MONDAY,
        2,
      ),
    ).toEqual({
      openMinute: null,
      closeMinute: null,
      closedDays: [],
      dayHours: [null, null],
    });
  });

  it('keeps the hours of each trip day', () => {
    // Open 08:00–17:00 except Sunday, 08:00–12:00.
    const week: DayHours[] = Array.from({ length: 7 }, (_, weekday) =>
      weekday === 0
        ? { allDay: false, open: 480, close: 720 }
        : { allDay: false, open: 480, close: 1020 },
    );
    const saturday = fromDateOnly('2026-11-28');
    expect(hoursForTrip(week, saturday, 2).dayHours).toEqual([
      { open: 480, close: 1020 },
      { open: 480, close: 720 },
    ]);
  });
});
