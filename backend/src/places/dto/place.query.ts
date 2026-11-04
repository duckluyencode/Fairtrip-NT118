import { Type } from 'class-transformer';
import {
  IsIn,
  IsInt,
  IsLatitude,
  IsLongitude,
  IsNotEmpty,
  IsOptional,
  IsString,
  Max,
  MaxLength,
  Min,
  ValidateIf,
} from 'class-validator';
import { TrimSpaces } from '../../common/validation/trim-spaces.js';
import { PlaceCategory } from '../../generated/prisma/enums.js';

/** Optional position of the user; both or neither. Used for distances. */
export class OriginQuery {
  @ValidateIf((q: OriginQuery) => q.lat !== undefined || q.lng !== undefined)
  @Type(() => Number)
  @IsLatitude()
  lat?: number;

  @ValidateIf((q: OriginQuery) => q.lat !== undefined || q.lng !== undefined)
  @Type(() => Number)
  @IsLongitude()
  lng?: number;
}

export class SearchPlacesQuery extends OriginQuery {
  /** Search text; without it the catalog is listed (nearest first when lat/lng are sent). */
  @IsOptional()
  @TrimSpaces()
  @IsString()
  @MaxLength(100)
  q?: string;

  @IsOptional()
  @IsIn(Object.values(PlaceCategory))
  category?: PlaceCategory;

  @IsOptional()
  @Type(() => Number)
  @IsInt()
  @Min(1)
  page: number = 1;

  @IsOptional()
  @Type(() => Number)
  @IsInt()
  @Min(1)
  @Max(50)
  limit: number = 20;
}

export class SuggestionsQuery {
  @TrimSpaces()
  @IsString()
  @IsNotEmpty()
  @MaxLength(100)
  q: string;
}

export class CategoryQuery {
  @IsOptional()
  @IsIn(Object.values(PlaceCategory))
  category?: PlaceCategory;
}
