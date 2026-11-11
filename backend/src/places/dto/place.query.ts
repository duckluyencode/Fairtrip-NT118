import { Type } from 'class-transformer';
import {
  IsIn,
  IsInt,
  IsLatitude,
  IsLongitude,
  IsNotEmpty,
  IsOptional,
  IsString,
  IsUUID,
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

  /** Catalog search only; Google results page with pageToken instead. */
  @IsOptional()
  @Type(() => Number)
  @IsInt()
  @Min(1)
  page: number = 1;

  /** nextPageToken of the previous Google results page. */
  @IsOptional()
  @IsString()
  @MaxLength(2000)
  pageToken?: string;

  @IsOptional()
  @Type(() => Number)
  @IsInt()
  @Min(1)
  @Max(50)
  limit: number = 20;
}

export class SuggestionsQuery extends OriginQuery {
  @TrimSpaces()
  @IsString()
  @IsNotEmpty()
  @MaxLength(100)
  q: string;

  /** Same UUID for one typing session, then on the detail request of the chosen place. */
  @IsOptional()
  @IsUUID()
  sessionToken?: string;
}

export class PlaceDetailQuery extends OriginQuery {
  /** The typing session's token when the place was chosen from suggestions. */
  @IsOptional()
  @IsUUID()
  sessionToken?: string;
}

export class CategoryQuery {
  @IsOptional()
  @IsIn(Object.values(PlaceCategory))
  category?: PlaceCategory;
}
