import { travelMinutes } from './travel.js';
import type { LatLng, PlannerInput, PlannerPlace } from './types.js';

export type DaySettings = Pick<
  PlannerInput,
  'dayStartMinute' | 'dayEndMinute' | 'gapMinutes' | 'base'
>;

export interface TimedVisit {
  place: PlannerPlace;
  startMinute: number;
  travelMinutes: number;
}

export interface TimedDay {
  visits: TimedVisit[];
  /** When the day is over, including the way back to the base. */
  finishMinute: number;
}

/** Opening hours as minutes, with a close after midnight moved past 1440. */
function openingWindow(
  place: PlannerPlace,
): { open: number; close: number } | null {
  if (place.openMinute === null || place.closeMinute === null) {
    return null;
  }
  const close =
    place.closeMinute <= place.openMinute
      ? place.closeMinute + 1440
      : place.closeMinute;
  return { open: place.openMinute, close };
}

/** When meals may start, minutes after midnight: lunch 11:00–13:30, dinner 17:30–20:30. */
export const MEAL_WINDOWS = [
  { name: 'LUNCH', from: 660, lastStart: 810 },
  { name: 'DINNER', from: 1050, lastStart: 1230 },
] as const;

export interface SimulatedDay extends TimedDay {
  /** The day (with the way back to the base) ends after dayEndMinute. */
  endsLate: boolean;
  /**
   * Places whose visit would end after their closing time, and meals that would not
   * start at a free lunch or dinner time.
   */
  closedPlaceIds: number[];
}

/**
 * Times every visit of a day in the given order: travel, the pause between visits,
 * waiting for opening time. Reports, instead of refusing, visits that end after closing
 * time and a day that ends too late; used as is for itineraries users design themselves.
 *
 * `legs` gives real travel minutes (from Google Routes) instead of the straight-line
 * estimate: legs[i] leads to visit i (from the base, or the previous visit), and with a
 * base the last leg goes back to it.
 */
export function simulateDay(
  order: PlannerPlace[],
  settings: DaySettings,
  legs?: number[],
): SimulatedDay {
  let clock = settings.dayStartMinute;
  let previous: LatLng | null = settings.base;
  const visits: TimedVisit[] = [];
  const closedPlaceIds: number[] = [];
  const mealsTaken = new Set<string>();
  let endsLate = false;
  for (const [index, place] of order.entries()) {
    const travel =
      legs?.[index] ?? (previous ? travelMinutes(previous, place) : 0);
    clock += travel + (index > 0 ? settings.gapMinutes : 0);
    const window = openingWindow(place);
    if (place.meal) {
      // The first meal of the day still free that can start in time, opening hours too.
      const opens = window?.open ?? 0;
      const slot = MEAL_WINDOWS.find(
        (w) =>
          !mealsTaken.has(w.name) &&
          Math.max(clock, w.from, opens) <= w.lastStart,
      );
      if (slot) {
        mealsTaken.add(slot.name);
        clock = Math.max(clock, slot.from);
      } else {
        closedPlaceIds.push(place.id);
      }
    }
    if (window && clock < window.open) {
      clock = window.open;
    }
    const end = clock + place.visitMinutes;
    if (window && end > window.close) {
      closedPlaceIds.push(place.id);
    }
    if (end > settings.dayEndMinute) {
      endsLate = true;
    }
    visits.push({ place, startMinute: clock, travelMinutes: travel });
    clock = end;
    previous = place;
  }
  if (previous && settings.base) {
    clock += legs?.[order.length] ?? travelMinutes(previous, settings.base);
  }
  if (clock > settings.dayEndMinute) {
    endsLate = true;
  }
  return { visits, finishMinute: clock, endsLate, closedPlaceIds };
}

/**
 * Like simulateDay, but null when a visit would end after closing time or the day
 * would end after dayEndMinute. The planner only builds days this accepts.
 */
export function scheduleDay(
  order: PlannerPlace[],
  settings: DaySettings,
): TimedDay | null {
  const day = simulateDay(order, settings);
  if (day.endsLate || day.closedPlaceIds.length > 0) {
    return null;
  }
  return { visits: day.visits, finishMinute: day.finishMinute };
}

export interface Insertion {
  /** 0-based day index. */
  day: number;
  /** 0-based position within the day. */
  position: number;
  /** Minutes the day grows by. */
  addedMinutes: number;
}

/** The feasible day and position that make the plan grow the least, or null. */
export function cheapestInsertion(
  days: PlannerPlace[][],
  place: PlannerPlace,
  settings: DaySettings,
): Insertion | null {
  let best: Insertion | null = null;
  for (const [day, order] of days.entries()) {
    if (place.closedDays?.includes(day + 1)) {
      continue;
    }
    const before = scheduleDay(order, settings);
    const base = before ? before.finishMinute : settings.dayStartMinute;
    for (let position = 0; position <= order.length; position++) {
      const candidate = [
        ...order.slice(0, position),
        place,
        ...order.slice(position),
      ];
      const after = scheduleDay(candidate, settings);
      if (!after) {
        continue;
      }
      const addedMinutes = after.finishMinute - base;
      if (!best || addedMinutes < best.addedMinutes) {
        best = { day, position, addedMinutes };
      }
    }
  }
  return best;
}
