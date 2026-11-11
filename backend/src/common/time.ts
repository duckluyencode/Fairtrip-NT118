// Small helpers for the minute-of-day and date-only values the API exchanges.

/** 0–1439 minutes after midnight as "HH:mm". */
export function toHhmm(minutes: number): string {
  const h = Math.floor(minutes / 60) % 24;
  const m = minutes % 60;
  return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}`;
}

/** "HH:mm" as minutes after midnight; "24:00" is allowed as the end of a day. */
export function fromHhmm(value: string): number {
  const [h, m] = value.split(':').map(Number);
  return h * 60 + m;
}

export const HHMM_PATTERN = /^(?:[01]\d|2[0-3]):[0-5]\d$|^24:00$/;

/** "YYYY-MM-DD" as a Date at 00:00 UTC, matching how MySQL DATE columns come back. */
export function fromDateOnly(value: string): Date {
  return new Date(`${value}T00:00:00.000Z`);
}

export function toDateOnly(date: Date): string {
  return date.toISOString().slice(0, 10);
}

/** Calendar days from start to end, both included. */
export function daysInclusive(start: Date, end: Date): number {
  return Math.round((end.getTime() - start.getTime()) / 86_400_000) + 1;
}

export function addDays(date: Date, days: number): Date {
  return new Date(date.getTime() + days * 86_400_000);
}
