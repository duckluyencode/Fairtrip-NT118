import { HttpStatus, Injectable, Logger } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { AppError } from '../../common/errors/app-error.js';
import { ErrorCode } from '../../common/errors/error-code.js';
import {
  GooglePlacesClient,
  type AutocompleteSuggestion,
  type DayHours,
  type GooglePlace,
  type TextSearchRequest,
  type TextSearchResult,
} from './google-places.client.js';

const BASE_URL = 'https://places.googleapis.com/v1';
const TIMEOUT_MS = 5000;

// Field masks decide the billed SKU, so ask only for what the screens show.
const LIST_FIELDS = [
  'id',
  'displayName',
  'formattedAddress',
  'location',
  'types',
  'rating',
  'userRatingCount',
];
const SEARCH_FIELDS = [
  ...LIST_FIELDS.map((f) => `places.${f}`),
  'nextPageToken',
].join(',');
// Trip planning needs prices and opening hours for every candidate: reading them in the
// search costs one call instead of one detail call per place.
const PLANNING_FIELDS = [
  ...[...LIST_FIELDS, 'priceLevel', 'priceRange', 'regularOpeningHours'].map(
    (f) => `places.${f}`,
  ),
  'nextPageToken',
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
  'utcOffsetMinutes',
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
  /** The place's offset from UTC now, e.g. 420 in Vietnam. */
  utcOffsetMinutes?: number;
}

/** Vietnam (UTC+7, no daylight saving time), used when Google gives no offset. */
const VIETNAM_OFFSET_MINUTES = 7 * 60;

/** The weekday where the place is (0 = Sunday), so "open today" uses its local date. */
function localWeekday(now: Date, offsetMinutes: number): number {
  return new Date(now.getTime() + offsetMinutes * 60 * 1000).getUTCDay();
}

const minutes = (point: OpeningPoint) =>
  (point.hour ?? 0) * 60 + (point.minute ?? 0);

type Periods = { open?: OpeningPoint; close?: OpeningPoint }[] | undefined;

/** Hours on a weekday (0 = Sunday), or null when Google has no hours. */
export function hoursForWeekday(
  periods: Periods,
  weekday: number,
): DayHours | null {
  if (!periods || periods.length === 0) {
    return null;
  }
  // Google marks "open 24 hours" as one period opening on Sunday at 0:00 without a close.
  if (periods.length === 1 && !periods[0].close) {
    return { allDay: true };
  }
  const period = periods.find((p) => p.open?.day === weekday);
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

export function todayHours(
  periods: Periods,
  now = new Date(),
  offsetMinutes = VIETNAM_OFFSET_MINUTES,
): DayHours | null {
  return hoursForWeekday(periods, localWeekday(now, offsetMinutes));
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
    todayHours: todayHours(
      place.regularOpeningHours?.periods,
      now,
      place.utcOffsetMinutes,
    ),
    weeklyHours: place.regularOpeningHours?.periods?.length
      ? Array.from({ length: 7 }, (_, weekday) =>
          hoursForWeekday(place.regularOpeningHours?.periods, weekday),
        )
      : null,
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

  async search(request: TextSearchRequest): Promise<TextSearchResult> {
    const body: Record<string, unknown> = {
      textQuery: request.query,
      languageCode: 'vi',
      pageSize: Math.min(20, Math.max(1, request.pageSize)),
    };
    if (request.pageToken) body.pageToken = request.pageToken;
    if (request.includedType) body.includedType = request.includedType;
    if (request.near) {
      body.locationBias = {
        circle: {
          center: { latitude: request.near.lat, longitude: request.near.lng },
          radius: Math.min(50_000, request.near.radiusMeters),
        },
      };
    }
    const result = await this.post<{
      places?: ApiPlace[];
      nextPageToken?: string;
    }>(
      'places:searchText',
      body,
      request.withDetails ? PLANNING_FIELDS : SEARCH_FIELDS,
    );
    return {
      places: (result.places ?? []).map((place) => toGooglePlace(place)),
      nextPageToken: result.nextPageToken ?? null,
    };
  }

  async autocomplete(
    input: string,
    sessionToken: string | undefined,
    near: { lat: number; lng: number } | null,
  ): Promise<AutocompleteSuggestion[]> {
    const body: Record<string, unknown> = { input, languageCode: 'vi' };
    if (sessionToken) body.sessionToken = sessionToken;
    if (near) {
      body.locationBias = {
        circle: {
          center: { latitude: near.lat, longitude: near.lng },
          radius: 50_000,
        },
      };
    }
    const result = await this.post<{
      suggestions?: {
        placePrediction?: {
          placeId?: string;
          structuredFormat?: {
            mainText?: { text?: string };
            secondaryText?: { text?: string };
          };
          text?: { text?: string };
        };
      }[];
    }>(
      'places:autocomplete',
      body,
      'suggestions.placePrediction.placeId,suggestions.placePrediction.structuredFormat,suggestions.placePrediction.text',
    );
    return (result.suggestions ?? []).flatMap(({ placePrediction: p }) =>
      p?.placeId
        ? [
            {
              placeId: p.placeId,
              mainText:
                p.structuredFormat?.mainText?.text ?? p.text?.text ?? '',
              secondaryText: p.structuredFormat?.secondaryText?.text ?? null,
            },
          ]
        : [],
    );
  }

  async getPlace(
    placeId: string,
    sessionToken?: string,
  ): Promise<GooglePlace | null> {
    if (!this.apiKey) {
      throw unavailable();
    }
    const params = new URLSearchParams({ languageCode: 'vi' });
    if (sessionToken) params.set('sessionToken', sessionToken);
    let res: Response;
    try {
      res = await fetch(
        `${BASE_URL}/places/${encodeURIComponent(placeId)}?${params.toString()}`,
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

  private async post<T>(
    path: string,
    body: Record<string, unknown>,
    fieldMask: string,
  ): Promise<T> {
    if (!this.apiKey) {
      throw unavailable();
    }
    try {
      const res = await fetch(`${BASE_URL}/${path}`, {
        method: 'POST',
        headers: this.headers(fieldMask),
        body: JSON.stringify(body),
        signal: AbortSignal.timeout(TIMEOUT_MS),
      });
      if (!res.ok) {
        throw new Error(`HTTP ${res.status}: ${await res.text()}`);
      }
      return (await res.json()) as T;
    } catch (error) {
      this.logger.warn(`Google ${path} failed: ${String(error)}`);
      throw unavailable();
    }
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
