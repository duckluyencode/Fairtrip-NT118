import type { GooglePriceInput } from '../pricing.js';

/** Hours of one day, in minutes after midnight (close < open means after midnight). */
export type DayHours =
  | { allDay: true }
  | { allDay: false; open: number; close: number }
  | { closed: true };

/** Place data read from Google. Only `id` may be stored; the rest is shown and discarded. */
export interface GooglePlace {
  id: string;
  name: string;
  address: string | null;
  lat: number | null;
  lng: number | null;
  types: string[];
  rating: number | null;
  ratingCount: number | null;
  priceLevel: GooglePriceInput['priceLevel'];
  priceRange: GooglePriceInput['priceRange'];
  /** Today's hours in Vietnam, or null when Google has none. */
  todayHours: DayHours | null;
  summary: string | null;
  mapsUri: string | null;
}

/** Google Places API (New). Injected as a token so tests and keyless setups can replace it. */
export abstract class GooglePlacesClient {
  /** False without GOOGLE_MAPS_API_KEY; search then uses the catalog only. */
  abstract readonly enabled: boolean;
  abstract searchText(
    query: string,
    maxResults: number,
  ): Promise<GooglePlace[]>;
  /** Null when Google does not know the place (any more). */
  abstract getPlace(placeId: string): Promise<GooglePlace | null>;
}
