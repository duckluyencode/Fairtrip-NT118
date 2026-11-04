import { normalizeText } from './text.js';

export interface Rankable {
  name: string;
  area?: string | null;
  rating: number | null;
  ratingCount: number | null;
}

/** Whether `query` starts at a word boundary inside `text` (both normalized). */
function hasPhrase(text: string, query: string): boolean {
  return ` ${text}`.includes(` ${query}`);
}

/**
 * How well a place matches a normalized query; lower is better:
 * exact name, name starts with it, a word of the name starts with it,
 * name contains it, the area contains it, then any other match.
 */
export function matchRank(
  query: string,
  name: string,
  area?: string | null,
): number {
  const normalized = normalizeText(name);
  if (normalized === query) return 0;
  if (normalized.startsWith(query)) return 1;
  if (hasPhrase(normalized, query)) return 2;
  if (normalized.includes(query)) return 3;
  if (area && hasPhrase(normalizeText(area), query)) return 4;
  return 5;
}

/** Best match first; ties go to the higher rating, then more ratings. */
export function rankByQuery<T extends Rankable>(
  query: string,
  places: T[],
): T[] {
  return places
    .map((place) => ({
      place,
      rank: matchRank(query, place.name, place.area),
    }))
    .sort(
      (a, b) =>
        a.rank - b.rank ||
        (b.place.rating ?? 0) - (a.place.rating ?? 0) ||
        (b.place.ratingCount ?? 0) - (a.place.ratingCount ?? 0) ||
        a.place.name.localeCompare(b.place.name, 'vi'),
    )
    .map(({ place }) => place);
}
