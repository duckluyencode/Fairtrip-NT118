import { Type } from 'class-transformer';
import { IsInt, IsOptional, Max, Min } from 'class-validator';
import type { PlaceCategory } from '../generated/prisma/enums.js';

/** Entries kept per user; older ones are dropped. */
export const HISTORY_LIMIT = 50;

export class HistoryQuery {
  @IsOptional()
  @Type(() => Number)
  @IsInt()
  @Min(1)
  @Max(HISTORY_LIMIT)
  limit: number = 20;
}

export class HistoryEntryResponse {
  id: number;
  query: string;
  /** Filter used with the query, if any. */
  category: PlaceCategory | null;
  searchedAt: Date;
}

export class HistoryList {
  items: HistoryEntryResponse[];
}
