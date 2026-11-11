import type { GooglePriceInput } from '../pricing.js';

/** Which fields getPlace reads: the place screen's, or the cheaper set trips need. */
export type PlaceFields = 'detail' | 'trip';

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
  /** Hours for each weekday, 0 = Sunday; null when Google has none. */
  weeklyHours: (DayHours | null)[] | null;
  summary: string | null;
  mapsUri: string | null;
}

export interface TextSearchRequest {
  query: string;
  /** 1–20 results. */
  pageSize: number;
  /** From the previous page's nextPageToken. */
  pageToken?: string;
  /** Only this Google place type, e.g. "lodging" or "restaurant". */
  includedType?: string;
  /** Prefer results around this point. */
  near?: { lat: number; lng: number; radiusMeters: number };
  /** Also read price and opening hours (a pricier field mask, used for trip planning). */
  withDetails?: boolean;
}

export interface TextSearchResult {
  places: GooglePlace[];
  nextPageToken: string | null;
}

export interface AutocompleteSuggestion {
  placeId: string;
  /** e.g. "Cầu Vàng" */
  mainText: string;
  /** e.g. "Hòa Vang, Đà Nẵng, Việt Nam" */
  secondaryText: string | null;
}

/** Google Places API (New). Injected as a token so tests and keyless setups can replace it. */
export abstract class GooglePlacesClient {
  /** False without GOOGLE_MAPS_API_KEY; the app then uses the sample catalog (development). */
  abstract readonly enabled: boolean;
  /** Text Search; throws GOOGLE_PLACES_UNAVAILABLE when Google cannot answer. */
  abstract search(request: TextSearchRequest): Promise<TextSearchResult>;
  /**
   * Suggestions while typing. Pass the same sessionToken until the user opens a place, so
   * Google bills the typing session together with that place's details.
   */
  abstract autocomplete(
    input: string,
    sessionToken: string | undefined,
    near: { lat: number; lng: number } | null,
  ): Promise<AutocompleteSuggestion[]>;
  /**
   * Null when Google does not know the place (any more). `fields: 'trip'` leaves out the
   * description and Maps link, which only the place screen shows and which cost more.
   */
  abstract getPlace(
    placeId: string,
    sessionToken?: string,
    fields?: PlaceFields,
  ): Promise<GooglePlace | null>;
}
