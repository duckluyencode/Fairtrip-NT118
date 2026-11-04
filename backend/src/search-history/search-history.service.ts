import { HttpStatus, Injectable } from '@nestjs/common';
import { AppError } from '../common/errors/app-error.js';
import { ErrorCode } from '../common/errors/error-code.js';
import type { PlaceCategory } from '../generated/prisma/enums.js';
import { normalizeText } from '../places/text.js';
import { PrismaService } from '../prisma/prisma.service.js';
import { HISTORY_LIMIT } from './search-history.dto.js';

export interface HistoryEntry {
  id: number;
  query: string;
  category: PlaceCategory | null;
  searchedAt: Date;
}

@Injectable()
export class SearchHistoryService {
  constructor(private readonly prisma: PrismaService) {}

  /**
   * Saves a search. The same query (ignoring case and diacritics) keeps one entry,
   * updated with the latest spelling, filter and time.
   */
  async record(
    userId: number,
    query: string,
    category: PlaceCategory | null,
  ): Promise<void> {
    const normalizedQuery = normalizeText(query);
    if (!normalizedQuery) {
      return;
    }
    await this.prisma.searchHistory.upsert({
      where: { userId_normalizedQuery: { userId, normalizedQuery } },
      create: { userId, query, normalizedQuery, category },
      update: { query, category, searchedAt: new Date() },
    });
    const stale = await this.prisma.searchHistory.findMany({
      where: { userId },
      orderBy: [{ searchedAt: 'desc' }, { id: 'desc' }],
      skip: HISTORY_LIMIT,
      select: { id: true },
    });
    if (stale.length > 0) {
      await this.prisma.searchHistory.deleteMany({
        where: { id: { in: stale.map((entry) => entry.id) } },
      });
    }
  }

  /** Newest first. */
  list(userId: number, limit: number): Promise<HistoryEntry[]> {
    return this.prisma.searchHistory.findMany({
      where: { userId },
      orderBy: [{ searchedAt: 'desc' }, { id: 'desc' }],
      take: limit,
      select: { id: true, query: true, category: true, searchedAt: true },
    });
  }

  async remove(userId: number, id: number): Promise<void> {
    // Scoped by user, so nobody can delete another user's entry by guessing its id.
    const { count } = await this.prisma.searchHistory.deleteMany({
      where: { id, userId },
    });
    if (count === 0) {
      throw new AppError(
        HttpStatus.NOT_FOUND,
        ErrorCode.NotFound,
        'No such search history entry',
      );
    }
  }

  async clear(userId: number): Promise<void> {
    await this.prisma.searchHistory.deleteMany({ where: { userId } });
  }
}
