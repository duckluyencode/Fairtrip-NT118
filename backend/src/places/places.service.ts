import { HttpStatus, Injectable, Logger } from '@nestjs/common';
import { AppError } from '../common/errors/app-error.js';
import { ErrorCode } from '../common/errors/error-code.js';
import type { Place } from '../generated/prisma/client.js';
import type { PlaceCategory } from '../generated/prisma/enums.js';
import { PrismaService } from '../prisma/prisma.service.js';
import { SearchHistoryService } from '../search-history/search-history.service.js';
import type {
  PlaceDetail,
  PlacePage,
  PlaceSuggestion,
  PlaceSummary,
} from './dto/place.response.js';
import type { SearchPlacesQuery, SuggestionsQuery } from './dto/place.query.js';
import { distanceKm, type LatLng } from './geo.js';
import { googleCategories } from './google/google-categories.js';
import {
  GooglePlacesClient,
  type GooglePlace,
} from './google/google-places.client.js';
import {
  catalogDetail,
  catalogSummary,
  googleDetail,
  googleSummary,
  type CatalogRow,
} from './places.mapper.js';
import { rankByQuery } from './ranking.js';
import { normalizeText, words } from './text.js';

// The catalog is per city and small; matches are ranked in memory.
const CANDIDATE_LIMIT = 500;
const SUGGESTION_LIMIT = 5;
// Text used to browse places around the user when there is no search text.
const DISCOVER_QUERY = 'điểm tham quan';
const NEAR_RADIUS_METERS = 30_000;
// Google pages read at most per search when the category filter leaves a page empty.
const MAX_EXTRA_PAGES = 2;
// What a category means next to a searched name: "<this> gần <name>".
const NEARBY_QUERY: Record<PlaceCategory, string> = {
  FOOD: 'quán ăn',
  FUN: 'điểm vui chơi',
  SCENIC: 'điểm tham quan',
};
const NEARBY_TOKEN_PREFIX = 'near:';

export function placeNotFound(): AppError {
  return new AppError(
    HttpStatus.NOT_FOUND,
    ErrorCode.PlaceNotFound,
    'No such place',
  );
}

/**
 * Every query word must start a word of the place's search text, so "cau v" finds
 * "cầu vàng" while "ang" does not.
 */
function everyWordStartsAWord(text: string) {
  return words(text).map((word) => ({
    OR: [
      { searchText: { startsWith: word } },
      { searchText: { contains: ` ${word}` } },
    ],
  }));
}

function originOf(query: { lat?: number; lng?: number }): LatLng | null {
  return query.lat !== undefined && query.lng !== undefined
    ? { lat: query.lat, lng: query.lng }
    : null;
}

@Injectable()
export class PlacesService {
  private readonly logger = new Logger(PlacesService.name);

  constructor(
    private readonly prisma: PrismaService,
    private readonly google: GooglePlacesClient,
    private readonly history: SearchHistoryService,
  ) {}

  /**
   * With Google Places configured (production), results come from Google Text Search:
   * places anywhere in the world, near the user's position when it is sent, paged with
   * nextPageToken. Without it (development and tests), the sample catalog is searched.
   * A search with text is saved to the user's history either way.
   */
  async search(userId: number, query: SearchPlacesQuery): Promise<PlacePage> {
    const page = this.google.enabled
      ? await this.searchGoogle(query)
      : await this.searchCatalog(query);
    const text = query.q?.trim();
    if (text) {
      await this.history.record(userId, text, query.category ?? null);
    }
    page.items = await this.markFavorites(userId, page.items);
    return page;
  }

  private async searchGoogle(query: SearchPlacesQuery): Promise<PlacePage> {
    const origin = originOf(query);
    const text = query.q?.trim();
    if (!text && !origin) {
      return {
        items: [],
        page: 1,
        limit: query.limit,
        total: 0,
        nextPageToken: null,
      };
    }
    const inCategory = (place: GooglePlace) =>
      !query.category ||
      googleCategories(place.types).categories.includes(query.category);
    // Google cannot filter FUN or SCENIC itself, so a page may hold none of them: read
    // a few more pages before answering with an empty one.
    const run = async (searchText: string, pageToken: string | undefined) => {
      const request = {
        query: searchText,
        pageSize: query.limit,
        includedType: query.category === 'FOOD' ? 'restaurant' : undefined,
        near: origin
          ? { ...origin, radiusMeters: NEAR_RADIUS_METERS }
          : undefined,
      };
      let result = await this.google.search({ ...request, pageToken });
      let places = result.places.filter(inCategory);
      for (
        let extra = 0;
        places.length === 0 && result.nextPageToken && extra < MAX_EXTRA_PAGES;
        extra++
      ) {
        result = await this.google.search({
          ...request,
          pageToken: result.nextPageToken,
        });
        places = result.places.filter(inCategory);
      }
      return { places, nextPageToken: result.nextPageToken };
    };
    // A name with a category ("Cầu Rồng" + FOOD) finds the place itself, which the
    // filter then removes. The category then means "places of this kind near it".
    const nearbyText =
      text && query.category
        ? `${NEARBY_QUERY[query.category]} gần ${text}`
        : null;
    // Google only accepts a page token with the query that produced it, so the token
    // of a nearby search is marked.
    let nearby = Boolean(
      nearbyText && query.pageToken?.startsWith(NEARBY_TOKEN_PREFIX),
    );
    let { places: found, nextPageToken } = await run(
      nearby && nearbyText ? nearbyText : text || DISCOVER_QUERY,
      nearby
        ? query.pageToken?.slice(NEARBY_TOKEN_PREFIX.length)
        : query.pageToken,
    );
    if (found.length === 0 && nearbyText && !query.pageToken) {
      nearby = true;
      ({ places: found, nextPageToken } = await run(nearbyText, undefined));
    }
    const ids = await this.storeGoogleIds(found.map((place) => place.id));
    return {
      items: found.flatMap((place) => {
        const id = ids.get(place.id);
        return id ? [googleSummary(id, place, origin, false)] : [];
      }),
      page: 1,
      limit: query.limit,
      // Google does not say how many results there are; follow nextPageToken instead.
      total: null,
      nextPageToken:
        nearby && nextPageToken
          ? NEARBY_TOKEN_PREFIX + nextPageToken
          : nextPageToken,
    };
  }

  /**
   * Sample catalog places matching every word of `q` (ignoring diacritics), best match
   * first. Without `q`: the whole catalog, nearest first when the user's position is
   * sent, otherwise best rated.
   */
  private async searchCatalog(query: SearchPlacesQuery): Promise<PlacePage> {
    const text = normalizeText(query.q ?? '');
    const origin = originOf(query);
    const rows = await this.prisma.place.findMany({
      where: {
        source: 'CATALOG',
        ...(query.category && {
          categories: { some: { category: query.category } },
        }),
        ...(text && { AND: everyWordStartsAWord(text) }),
      },
      include: { categories: true },
      take: CANDIDATE_LIMIT,
    });
    const ordered = this.order(rows, text, origin);
    const start = (query.page - 1) * query.limit;
    return {
      items: ordered
        .slice(start, start + query.limit)
        .map((row) => catalogSummary(row, origin, false)),
      page: query.page,
      limit: query.limit,
      total: ordered.length,
      nextPageToken: null,
    };
  }

  /**
   * Names to show while typing; not saved to history. With Google: Place Autocomplete,
   * billed per typing session when the app sends the same sessionToken until it opens a
   * place (and sends that token with the detail request too).
   */
  async suggestions(query: SuggestionsQuery): Promise<PlaceSuggestion[]> {
    if (this.google.enabled) {
      const found = await this.google.autocomplete(
        query.q,
        query.sessionToken,
        originOf(query),
      );
      const ids = await this.storeGoogleIds(found.map((s) => s.placeId));
      return found.flatMap((s) => {
        const id = ids.get(s.placeId);
        return id ? [{ id, name: s.mainText, area: s.secondaryText }] : [];
      });
    }
    const text = normalizeText(query.q);
    if (!text) {
      return [];
    }
    const rows = await this.prisma.place.findMany({
      where: { source: 'CATALOG', AND: everyWordStartsAWord(text) },
      select: {
        id: true,
        name: true,
        area: true,
        rating: true,
        ratingCount: true,
      },
      take: CANDIDATE_LIMIT,
    });
    return rankByQuery(
      text,
      rows.map((row) => ({ ...row, name: row.name ?? '' })),
    )
      .slice(0, SUGGESTION_LIMIT)
      .map(({ id, name, area }) => ({ id, name, area }));
  }

  /**
   * Gives Google places a TripFair id by storing only their place ID (allowed by the
   * Google Maps terms). Returns Google place ID -> TripFair id.
   */
  async storeGoogleIds(googleIds: string[]): Promise<Map<string, number>> {
    if (googleIds.length === 0) {
      return new Map();
    }
    await this.prisma.place.createMany({
      data: googleIds.map((googlePlaceId) => ({
        source: 'GOOGLE' as const,
        googlePlaceId,
      })),
      skipDuplicates: true,
    });
    const stored = await this.prisma.place.findMany({
      where: { googlePlaceId: { in: googleIds } },
      select: { id: true, googlePlaceId: true },
    });
    return new Map(
      stored.map((row) => [row.googlePlaceId ?? '', row.id] as const),
    );
  }

  async detail(
    userId: number,
    id: number,
    origin: { lat?: number; lng?: number },
    sessionToken?: string,
  ): Promise<PlaceDetail> {
    const row = await this.prisma.place.findUnique({
      where: { id },
      include: { categories: true, parent: true },
    });
    if (!row) {
      throw placeNotFound();
    }
    const isFavorite =
      (await this.prisma.favorite.count({
        where: { userId, placeId: id },
      })) > 0;
    if (row.source === 'CATALOG') {
      return catalogDetail(row, originOf(origin), isFavorite);
    }
    const place = await this.google.getPlace(
      row.googlePlaceId ?? '',
      sessionToken,
    );
    if (!place) {
      throw placeNotFound();
    }
    return googleDetail(row.id, place, originOf(origin), isFavorite);
  }

  /**
   * Summaries for stored places, in the same order. Google places are fetched live;
   * any that Google cannot return right now are left out.
   */
  async summaries(
    rows: CatalogRow[],
    isFavorite: (id: number) => boolean,
  ): Promise<PlaceSummary[]> {
    const results = await Promise.all(
      rows.map(async (row) => {
        if (row.source === 'CATALOG') {
          return catalogSummary(row, null, isFavorite(row.id));
        }
        if (!this.google.enabled || !row.googlePlaceId) {
          return null;
        }
        try {
          const place = await this.google.getPlace(row.googlePlaceId);
          return place
            ? googleSummary(row.id, place, null, isFavorite(row.id))
            : null;
        } catch (error) {
          this.logger.warn(`Skipped Google place ${row.id}: ${String(error)}`);
          return null;
        }
      }),
    );
    return results.filter((summary) => summary !== null);
  }

  /** Throws PLACE_NOT_FOUND unless the place exists. */
  async assertExists(id: number): Promise<Place> {
    const place = await this.prisma.place.findUnique({ where: { id } });
    if (!place) {
      throw placeNotFound();
    }
    return place;
  }

  private order(
    rows: CatalogRow[],
    text: string,
    origin: LatLng | null,
  ): CatalogRow[] {
    if (text) {
      return rankByQuery(
        text,
        rows.map((row) => ({
          row,
          name: row.name ?? '',
          area: row.area,
          rating: row.rating,
          ratingCount: row.ratingCount,
        })),
      ).map(({ row }) => row);
    }
    if (origin) {
      const distance = (row: CatalogRow) =>
        row.lat !== null && row.lng !== null
          ? distanceKm(origin, { lat: row.lat, lng: row.lng })
          : Number.POSITIVE_INFINITY;
      return [...rows].sort((a, b) => distance(a) - distance(b) || a.id - b.id);
    }
    return [...rows].sort(
      (a, b) =>
        (b.rating ?? 0) - (a.rating ?? 0) ||
        (b.ratingCount ?? 0) - (a.ratingCount ?? 0) ||
        a.id - b.id,
    );
  }

  private async markFavorites(
    userId: number,
    items: PlaceSummary[],
  ): Promise<PlaceSummary[]> {
    if (items.length === 0) {
      return items;
    }
    const favorites = await this.prisma.favorite.findMany({
      where: { userId, placeId: { in: items.map((item) => item.id) } },
      select: { placeId: true },
    });
    const ids = new Set(favorites.map((f) => f.placeId));
    for (const item of items) {
      item.isFavorite = ids.has(item.id);
    }
    return items;
  }
}
