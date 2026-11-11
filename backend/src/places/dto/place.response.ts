import type {
  PlaceCategory,
  PlaceSource,
  PriceSource,
} from '../../generated/prisma/enums.js';

export class LocationResponse {
  lat: number;
  lng: number;
}

export class OpeningHoursResponse {
  allDay: boolean;
  /** "HH:mm"; null when open all day or closed today. */
  opensAt: string | null;
  /** "HH:mm"; earlier than opensAt when the place closes after midnight. */
  closesAt: string | null;
  closedToday: boolean;
}

/** A place in a list (search, favorites). Lists never show prices. */
export class PlaceSummary {
  id: number;
  source: PlaceSource;
  name: string;
  /** e.g. "Bà Nà Hills · Hòa Vang", or the address for Google places. */
  area: string | null;
  city: string | null;
  /** Main category, shown as the label. */
  category: PlaceCategory;
  categories: PlaceCategory[];
  rating: number | null;
  ratingCount: number | null;
  imageUrl: string | null;
  location: LocationResponse | null;
  /** Distance from the lat/lng sent with the request, in km. */
  distanceKm: number | null;
  isFavorite: boolean;
  /** "Google Maps" when the data comes from Google and must be credited. */
  attribution: string | null;
}

export class PriceResponse {
  /** VND per person; equal when fixed, 0 when free. */
  min: number;
  max: number;
  currency: 'VND';
  source: PriceSource;
  provider: string;
  /** Show "~" before the price when true. */
  isEstimate: boolean;
  /** The ticketed area whose ticket covers this place. */
  includedIn: { id: number; name: string } | null;
}

export class PlaceDetail extends PlaceSummary {
  description: string | null;
  visitMinutes: number;
  openingHours: OpeningHoursResponse | null;
  price: PriceResponse;
  /** Link to the place on Google Maps, required when showing Google data. */
  mapsUri: string | null;
}

export class PlacePage {
  items: PlaceSummary[];
  page: number;
  limit: number;
  /** Matching places across all pages; null for Google results (use nextPageToken). */
  total: number | null;
  /** Pass as pageToken for the next Google results page; null when there is none. */
  nextPageToken: string | null;
}

export class PlaceSuggestion {
  id: number;
  name: string;
  area: string | null;
}

export class PlaceList {
  items: PlaceSummary[];
}
