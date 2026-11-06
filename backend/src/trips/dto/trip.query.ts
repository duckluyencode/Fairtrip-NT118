import { Type } from 'class-transformer';
import { IsInt, IsOptional, Max, Min } from 'class-validator';
import { MAX_GUESTS_PER_ROOM } from './trip.dto.js';

export class LodgingOptionsQuery {
  /** People sharing a room, 1–4; default 2. Room shares and prices follow it. */
  @IsOptional()
  @Type(() => Number)
  @IsInt()
  @Min(1)
  @Max(MAX_GUESTS_PER_ROOM)
  guestsPerRoom?: number;
}
