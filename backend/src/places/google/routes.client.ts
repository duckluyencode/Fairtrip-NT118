import { Injectable, Logger } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import type { LatLng } from '../geo.js';

const ROUTES_URL = 'https://routes.googleapis.com/directions/v2:computeRoutes';
const TIMEOUT_MS = 8000;
// Up to 10 stops between origin and destination keep the request in the Basic SKU.
const MAX_INTERMEDIATES = 10;

/** Google Routes API. Injected as a token so tests and keyless setups can replace it. */
export abstract class RoutesClient {
  abstract readonly enabled: boolean;
  /**
   * Driving minutes for each consecutive leg (points[i] -> points[i + 1]), or null when
   * Google cannot answer, so callers keep their estimate.
   */
  abstract legMinutes(points: LatLng[]): Promise<number[] | null>;
}

interface RoutesResponse {
  routes?: { legs?: { duration?: string }[] }[];
}

const waypoint = (p: LatLng) => ({
  location: { latLng: { latitude: p.lat, longitude: p.lng } },
});

/**
 * Calls computeRoutes with GOOGLE_MAPS_API_KEY; disabled when the key is not set.
 * One request covers a whole day: origin, the stops as intermediates, destination,
 * and Google returns one leg per pair of consecutive points.
 */
@Injectable()
export class HttpRoutesClient extends RoutesClient {
  private readonly logger = new Logger(HttpRoutesClient.name);
  private readonly apiKey?: string;
  readonly enabled: boolean;

  constructor(config: ConfigService) {
    super();
    this.apiKey = config.get<string>('GOOGLE_MAPS_API_KEY') || undefined;
    this.enabled = Boolean(this.apiKey);
  }

  async legMinutes(points: LatLng[]): Promise<number[] | null> {
    if (points.length < 2) {
      return [];
    }
    if (!this.apiKey) {
      return null;
    }
    // Long days are split so each request stays within the cheaper SKU.
    const legs: number[] = [];
    for (let start = 0; start < points.length - 1;) {
      const end = Math.min(points.length - 1, start + MAX_INTERMEDIATES + 1);
      const part = await this.request(points.slice(start, end + 1));
      if (!part) {
        return null;
      }
      legs.push(...part);
      start = end;
    }
    return legs;
  }

  private async request(points: LatLng[]): Promise<number[] | null> {
    try {
      const res = await fetch(ROUTES_URL, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'X-Goog-Api-Key': this.apiKey ?? '',
          'X-Goog-FieldMask': 'routes.legs.duration',
        },
        body: JSON.stringify({
          origin: waypoint(points[0]),
          destination: waypoint(points[points.length - 1]),
          intermediates: points.slice(1, -1).map(waypoint),
          travelMode: 'DRIVE',
          routingPreference: 'TRAFFIC_UNAWARE',
        }),
        signal: AbortSignal.timeout(TIMEOUT_MS),
      });
      if (!res.ok) {
        throw new Error(`HTTP ${res.status}: ${await res.text()}`);
      }
      const body = (await res.json()) as RoutesResponse;
      const legs = body.routes?.[0]?.legs ?? [];
      if (legs.length !== points.length - 1) {
        throw new Error(
          `Expected ${points.length - 1} legs, got ${legs.length}`,
        );
      }
      return legs.map((leg) =>
        Math.ceil(Number((leg.duration ?? '0s').replace('s', '')) / 60),
      );
    } catch (error) {
      this.logger.warn(`Google routes failed: ${String(error)}`);
      return null;
    }
  }
}
