import { HttpStatus, Injectable, Logger } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { AppError } from '../../common/errors/app-error.js';
import { ErrorCode } from '../../common/errors/error-code.js';
import {
  GooglePlacesClient,
  type DayHours,
  type GooglePlace,
} from './google-places.client.js';

const BASE_URL = 'https://places.googleapis.com/v1';
const TIMEOUT_MS = 5000;

// Field masks decide the billed SKU, so ask only for what the screens show.
const SEARCH_FIELDS = [
  'places.id',
  'places.displayName',
  'places.formattedAddress',
  'places.location',
  'places.types',
  'places.rating',
  'places.userRatingCount',
].join(',');

const DETAIL_FIELDS = [
  'id',
  'displayName',
  'formattedAddress',
  'location',
  'types',
  'rating',
  'userRatingCount',
  'priceLevel',
  'priceRange',
  'regularOpeningHours',
  'editorialSummary',
  'googleMapsUri',
].join(',');

interface Money {
  currencyCode?: string;
  units?: string;
}

interface OpeningPoint {
  day?: number;
  hour?: number;
  minute?: number;
}

interface ApiPlace {
  id: string;
  displayName?: { text?: string };
  formattedAddress?: string;
  location?: { latitude?: number; longitude?: number };
  types?: string[];
  rating?: number;
  userRatingCount?: number;
  priceLevel?: string;
  priceRange?: { startPrice?: Money; endPrice?: Money };
  regularOpeningHours?: {
    periods?: { open?: OpeningPoint; close?: OpeningPoint }[];
  };
  editorialSummary?: { text?: string };
  googleMapsUri?: string;
}

/** Vietnam is UTC+7 all year. */
function vietnamWeekday(now: Date): number {
  return new Date(now.getTime() + 7 * 60 * 60 * 1000).getUTCDay();
}

const minutes = (point: OpeningPoint) =>
  (point.hour ?? 0) * 60 + (point.minute ?? 0);

export function todayHours(
  periods: { open?: OpeningPoint; close?: OpeningPoint }[] | undefined,
  now = new Date(),
): DayHours | null {
  if (!periods || periods.length === 0) {
    return null;
  }
  // Google marks "open 24 hours" as one period opening on Sunday at 0:00 without a close.
  if (periods.length === 1 && !periods[0].close) {
    return { allDay: true };
  }
  const today = vietnamWeekday(now);
  const period = periods.find((p) => p.open?.day === today);
  if (!period?.open) {
    return { closed: true };
  }
  if (!period.close) {
    return { allDay: true };
  }
  return {
    allDay: false,
    open: minutes(period.open),
    close: minutes(period.close),
  };
}

export function toGooglePlace(place: ApiPlace, now = new Date()): GooglePlace {
  const start = place.priceRange?.startPrice;
  const end = place.priceRange?.endPrice;
  return {
    id: place.id,
    name: place.displayName?.text ?? '',
    address: place.formattedAddress ?? null,
    lat: place.location?.latitude ?? null,
    lng: place.location?.longitude ?? null,
    types: place.types ?? [],
    rating: place.rating ?? null,
    ratingCount: place.userRatingCount ?? null,
    priceLevel: place.priceLevel ?? null,
    priceRange:
      start?.currencyCode && start.units !== undefined
        ? {
            currencyCode: start.currencyCode,
            start: Number(start.units),
            end: end?.units !== undefined ? Number(end.units) : null,
          }
        : null,
    todayHours: todayHours(place.regularOpeningHours?.periods, now),
    summary: place.editorialSummary?.text ?? null,
    mapsUri: place.googleMapsUri ?? null,
  };
}

/** Calls Places API (New) with GOOGLE_MAPS_API_KEY; disabled when the key is not set. */
@Injectable()
export class HttpGooglePlacesClient extends GooglePlacesClient {
  private readonly logger = new Logger(HttpGooglePlacesClient.name);
  private readonly apiKey?: string;
  readonly enabled: boolean;

  constructor(config: ConfigService) {
    super();
    this.apiKey = config.get<string>('GOOGLE_MAPS_API_KEY') || undefined;
    this.enabled = Boolean(this.apiKey);
  }

  async searchText(query: string, maxResults: number): Promise<GooglePlace[]> {
    if (!this.apiKey) {
      return [];
    }
    try {
      const res = await fetch(`${BASE_URL}/places:searchText`, {
        method: 'POST',
        headers: this.headers(SEARCH_FIELDS),
        body: JSON.stringify({
          textQuery: query,
          languageCode: 'vi',
          regionCode: 'VN',
          pageSize: maxResults,
        }),
        signal: AbortSignal.timeout(TIMEOUT_MS),
      });
      if (!res.ok) {
        throw new Error(`HTTP ${res.status}: ${await res.text()}`);
      }
      const body = (await res.json()) as { places?: ApiPlace[] };
      return (body.places ?? []).map((place) => toGooglePlace(place));
    } catch (error) {
      // Search still answers from the catalog when Google fails.
      this.logger.warn(`Google text search failed: ${String(error)}`);
      return [];
    }
  }

  async getPlace(placeId: string): Promise<GooglePlace | null> {
    if (!this.apiKey) {
      throw unavailable();
    }
    let res: Response;
    try {
      res = await fetch(
        `${BASE_URL}/places/${encodeURIComponent(placeId)}?languageCode=vi&regionCode=VN`,
        {
          headers: this.headers(DETAIL_FIELDS),
          signal: AbortSignal.timeout(TIMEOUT_MS),
        },
      );
    } catch (error) {
      this.logger.warn(`Google place details failed: ${String(error)}`);
      throw unavailable();
    }
    if (res.status === 404) {
      return null;
    }
    if (!res.ok) {
      this.logger.warn(
        `Google place details failed: HTTP ${res.status}: ${await res.text()}`,
      );
      throw unavailable();
    }
    return toGooglePlace((await res.json()) as ApiPlace);
  }

  private headers(fieldMask: string): Record<string, string> {
    return {
      'Content-Type': 'application/json',
      'X-Goog-Api-Key': this.apiKey ?? '',
      'X-Goog-FieldMask': fieldMask,
    };
  }
}

function unavailable(): AppError {
  return new AppError(
    HttpStatus.SERVICE_UNAVAILABLE,
    ErrorCode.GooglePlacesUnavailable,
    'Google Places is not available right now',
  );
}
