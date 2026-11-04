/**
 * Lowercase ASCII without diacritics or punctuation, so "Cầu  Vàng!" and "cau vang"
 * compare equal. Vietnamese đ has no Unicode decomposition and is mapped by hand.
 */
export function normalizeText(input: string): string {
  return input
    .normalize('NFD')
    .replace(/\p{M}/gu, '')
    .toLowerCase()
    .replace(/đ/g, 'd')
    .replace(/[^a-z0-9]+/g, ' ')
    .trim();
}

/** Text stored in places.search_text. */
export function searchTextOf(place: {
  name: string;
  area?: string | null;
  city?: string | null;
}): string {
  return normalizeText(
    [place.name, place.area, place.city].filter(Boolean).join(' '),
  );
}

export function words(normalized: string): string[] {
  return normalized.split(' ').filter(Boolean);
}
