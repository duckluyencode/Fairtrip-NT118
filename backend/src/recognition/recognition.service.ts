import { HttpStatus, Injectable } from '@nestjs/common';
import { AppError } from '../common/errors/app-error.js';
import { ErrorCode } from '../common/errors/error-code.js';
import { GooglePlacesClient } from '../places/google/google-places.client.js';
import { googleSummary } from '../places/places.mapper.js';
import { PlacesService } from '../places/places.service.js';
import { PrismaService } from '../prisma/prisma.service.js';
import type { RecognitionResponse } from './recognition.response.js';
import { recognitionUnavailable, VisionClient } from './vision.client.js';

/** Landmarks Vision is less sure about than this are not shown. */
const MIN_SCORE = 0.3;
const MAX_ITEMS = 3;
// Vision's coordinates point at the landmark; the place must be close to them.
const MATCH_RADIUS_METERS = 2_000;

export interface UploadedImage {
  buffer: Buffer;
  mimetype: string;
  size: number;
}

/** JPEG, PNG or WebP, judged by the file's first bytes rather than its declared type. */
export function isSupportedImage(buffer: Buffer): boolean {
  const jpeg = buffer.subarray(0, 3).equals(Buffer.from([0xff, 0xd8, 0xff]));
  const png = buffer
    .subarray(0, 8)
    .equals(Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]));
  const webp =
    buffer.subarray(0, 4).toString('latin1') === 'RIFF' &&
    buffer.subarray(8, 12).toString('latin1') === 'WEBP';
  return jpeg || png || webp;
}

/**
 * "Nhận diện địa điểm": Cloud Vision finds landmarks in a photo, then each landmark is
 * looked up on Google Places near the coordinates Vision gives, so the user lands on a
 * real place they can open, save or add to a trip. Photos are not stored.
 */
@Injectable()
export class RecognitionService {
  constructor(
    private readonly prisma: PrismaService,
    private readonly vision: VisionClient,
    private readonly google: GooglePlacesClient,
    private readonly places: PlacesService,
  ) {}

  async recognize(
    userId: number,
    image: UploadedImage | undefined,
  ): Promise<RecognitionResponse> {
    if (!image || image.size === 0 || !isSupportedImage(image.buffer)) {
      throw new AppError(
        HttpStatus.BAD_REQUEST,
        ErrorCode.InvalidImage,
        'Send a JPEG, PNG or WebP photo in the "image" field',
      );
    }
    if (!this.vision.enabled || !this.google.enabled) {
      throw recognitionUnavailable();
    }
    const landmarks = (await this.vision.landmarks(image.buffer))
      .filter((l) => l.score >= MIN_SCORE)
      .slice(0, MAX_ITEMS);

    const matches = await Promise.all(
      landmarks.map(async (landmark) => {
        const found = await this.google.search({
          query: landmark.name,
          pageSize: 1,
          near: landmark.location
            ? { ...landmark.location, radiusMeters: MATCH_RADIUS_METERS }
            : undefined,
        });
        return { landmark, place: found.places[0] ?? null };
      }),
    );
    const ids = await this.places.storeGoogleIds(
      matches.flatMap((m) => (m.place ? [m.place.id] : [])),
    );
    const favorites = new Set(
      (
        await this.prisma.favorite.findMany({
          where: { userId, placeId: { in: [...ids.values()] } },
          select: { placeId: true },
        })
      ).map((f) => f.placeId),
    );
    const items = matches.map(({ landmark, place }) => {
      const id = place ? ids.get(place.id) : undefined;
      return {
        landmark: landmark.name,
        confidence: Math.round(landmark.score * 100) / 100,
        place:
          place && id
            ? googleSummary(id, place, landmark.location, favorites.has(id))
            : null,
      };
    });
    // Two landmarks can lead to the same place: keep its first (most likely) match.
    const seen = new Set<number>();
    const unique = items.filter((item) => {
      if (!item.place) return true;
      if (seen.has(item.place.id)) return false;
      seen.add(item.place.id);
      return true;
    });
    return {
      items: unique,
      attribution: unique.some((i) => i.place) ? 'Google Maps' : null,
    };
  }
}
