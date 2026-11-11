import { HttpStatus, Injectable, Logger } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { AppError } from '../common/errors/app-error.js';
import { ErrorCode } from '../common/errors/error-code.js';
import type { LatLng } from '../places/geo.js';

const ANNOTATE_URL = 'https://vision.googleapis.com/v1/images:annotate';
const TIMEOUT_MS = 10_000;
const MAX_RESULTS = 5;

/** A landmark Cloud Vision sees in a photo. */
export interface Landmark {
  /** e.g. "Dragon Bridge" (Vision names landmarks in English). */
  name: string;
  /** 0–1, how sure Vision is. */
  score: number;
  location: LatLng | null;
}

/** Google Cloud Vision. Injected as a token so tests and keyless setups can replace it. */
export abstract class VisionClient {
  abstract readonly enabled: boolean;
  /** Landmarks in the image, most likely first; throws RECOGNITION_UNAVAILABLE on failure. */
  abstract landmarks(image: Buffer): Promise<Landmark[]>;
}

interface AnnotateResponse {
  responses?: {
    landmarkAnnotations?: {
      description?: string;
      score?: number;
      locations?: { latLng?: { latitude?: number; longitude?: number } }[];
    }[];
    error?: { code?: number; message?: string };
  }[];
}

export function recognitionUnavailable(): AppError {
  return new AppError(
    HttpStatus.SERVICE_UNAVAILABLE,
    ErrorCode.RecognitionUnavailable,
    'Photo recognition is not available right now',
  );
}

/**
 * Calls images:annotate (LANDMARK_DETECTION) with GOOGLE_MAPS_API_KEY, the same Google
 * Cloud key as Places and Routes (restricted to those three APIs). Disabled without it.
 */
@Injectable()
export class HttpVisionClient extends VisionClient {
  private readonly logger = new Logger(HttpVisionClient.name);
  private readonly apiKey?: string;
  readonly enabled: boolean;

  constructor(config: ConfigService) {
    super();
    this.apiKey = config.get<string>('GOOGLE_MAPS_API_KEY') || undefined;
    this.enabled = Boolean(this.apiKey);
  }

  async landmarks(image: Buffer): Promise<Landmark[]> {
    if (!this.apiKey) {
      throw recognitionUnavailable();
    }
    let body: AnnotateResponse;
    try {
      const res = await fetch(ANNOTATE_URL, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'X-Goog-Api-Key': this.apiKey,
        },
        body: JSON.stringify({
          requests: [
            {
              image: { content: image.toString('base64') },
              features: [
                { type: 'LANDMARK_DETECTION', maxResults: MAX_RESULTS },
              ],
            },
          ],
        }),
        signal: AbortSignal.timeout(TIMEOUT_MS),
      });
      if (!res.ok) {
        throw new Error(`HTTP ${res.status}: ${await res.text()}`);
      }
      body = (await res.json()) as AnnotateResponse;
    } catch (error) {
      this.logger.warn(`Cloud Vision failed: ${String(error)}`);
      throw recognitionUnavailable();
    }
    const result = body.responses?.[0];
    if (result?.error) {
      // e.g. an image Vision cannot decode.
      this.logger.warn(`Cloud Vision error: ${result.error.message ?? ''}`);
      throw recognitionUnavailable();
    }
    return (result?.landmarkAnnotations ?? [])
      .filter((l) => l.description)
      .map((l) => {
        const point = l.locations?.[0]?.latLng;
        return {
          name: l.description ?? '',
          score: l.score ?? 0,
          location:
            point?.latitude !== undefined && point.longitude !== undefined
              ? { lat: point.latitude, lng: point.longitude }
              : null,
        };
      })
      .sort((a, b) => b.score - a.score);
  }
}
