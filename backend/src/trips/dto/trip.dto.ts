import { Type } from 'class-transformer';
import {
  ArrayMaxSize,
  ArrayUnique,
  IsArray,
  IsDateString,
  IsIn,
  IsInt,
  IsISO8601,
  IsNotEmpty,
  IsOptional,
  IsString,
  Matches,
  Max,
  MaxLength,
  Min,
  ValidateIf,
  ValidateNested,
} from 'class-validator';
import { HHMM_PATTERN } from '../../common/time.js';
import { TrimSpaces } from '../../common/validation/trim-spaces.js';
import {
  Interest,
  LodgingMode,
  TripType,
} from '../../generated/prisma/enums.js';

export const MAX_TRIP_DAYS = 14;
export const MAX_BUDGET = 1_000_000_000;
export const MAX_GUESTS_PER_ROOM = 4;

const INTERESTS = Object.values(Interest);

export class CreateTripDto {
  @IsIn(Object.values(TripType))
  type: TripType;

  /** Group name, required for group trips, e.g. "Nhóm Đà Nẵng". */
  @ValidateIf(
    (dto: CreateTripDto) => dto.type === 'GROUP' || dto.name !== undefined,
  )
  @TrimSpaces()
  @IsString()
  @IsNotEmpty()
  @MaxLength(100)
  name?: string;

  /** Destination city, e.g. "Đà Nẵng". */
  @TrimSpaces()
  @IsString()
  @IsNotEmpty()
  @MaxLength(100)
  city: string;

  /** YYYY-MM-DD */
  @IsDateString({ strict: true })
  @Matches(/^\d{4}-\d{2}-\d{2}$/, { message: 'startDate must be YYYY-MM-DD' })
  startDate: string;

  /** YYYY-MM-DD, at most 14 days after startDate. */
  @IsDateString({ strict: true })
  @Matches(/^\d{4}-\d{2}-\d{2}$/, { message: 'endDate must be YYYY-MM-DD' })
  endDate: string;

  /** The creator's own budget in VND: tickets, food and lodging, not transport. */
  @IsInt()
  @Min(0)
  @Max(MAX_BUDGET)
  budget: number;

  @IsArray()
  @ArrayUnique()
  @ArrayMaxSize(INTERESTS.length)
  @IsIn(INTERESTS, { each: true })
  interests: Interest[];

  /** HOTEL asks for hotel suggestions; SELF is "Tự lo chỗ ở". A 0 VND budget forces SELF. */
  @IsOptional()
  @IsIn(Object.values(LodgingMode))
  lodging?: LodgingMode;

  /** Daily window, "HH:mm"; default 08:00–21:00. */
  @IsOptional()
  @Matches(HHMM_PATTERN, { message: 'dayStart must be HH:mm' })
  dayStart?: string;

  @IsOptional()
  @Matches(HHMM_PATTERN, { message: 'dayEnd must be HH:mm' })
  dayEnd?: string;

  /** Minimum pause between two visits, minutes; default 30. */
  @IsOptional()
  @IsInt()
  @Min(0)
  @Max(240)
  gapMinutes?: number;

  /** Places to plan from (search results, favorites). Without them the city's catalog is used. */
  @IsOptional()
  @IsArray()
  @ArrayUnique()
  @ArrayMaxSize(50)
  @IsInt({ each: true })
  placeIds?: number[];

  /** Group trips: when voting closes, ISO 8601. */
  @IsOptional()
  @IsISO8601()
  voteDeadline?: string;
}

export class JoinTripDto {
  @TrimSpaces()
  @IsString()
  @Matches(/^[A-Za-z0-9]{6}$/, {
    message: 'inviteCode must be 6 letters or digits',
  })
  inviteCode: string;

  @IsInt()
  @Min(0)
  @Max(MAX_BUDGET)
  budget: number;

  @IsArray()
  @ArrayUnique()
  @IsIn(INTERESTS, { each: true })
  interests: Interest[];
}

export class UpdateMembershipDto {
  @IsOptional()
  @IsInt()
  @Min(0)
  @Max(MAX_BUDGET)
  budget?: number;

  @IsOptional()
  @IsArray()
  @ArrayUnique()
  @IsIn(INTERESTS, { each: true })
  interests?: Interest[];
}

export class TransferOwnershipDto {
  /** The member (memberId in the trip's members) who becomes the owner. */
  @IsInt()
  memberId: number;
}

export class ChooseLodgingDto {
  @IsIn(Object.values(LodgingMode))
  mode: LodgingMode;

  /** Required when mode is HOTEL: the placeId of a suggested hotel. */
  @ValidateIf((dto: ChooseLodgingDto) => dto.mode === 'HOTEL')
  @IsInt()
  placeId?: number;

  /** People sharing a room, 1–4; default 2. Use the value the options were listed with. */
  @IsOptional()
  @IsInt()
  @Min(1)
  @Max(MAX_GUESTS_PER_ROOM)
  guestsPerRoom?: number;
}

export class AddOptionDto {
  @IsInt()
  placeId: number;
}

export class ItineraryDayDto {
  /** 1-based day of the trip. */
  @IsInt()
  @Min(1)
  @Max(MAX_TRIP_DAYS)
  day: number;

  /** Places in visiting order. */
  @IsArray()
  @ArrayMaxSize(20)
  @IsInt({ each: true })
  placeIds: number[];
}

/** A self-designed itinerary ("Tự thiết kế lộ trình"): the places of each day, in order. */
export class UpdateItineraryDto {
  @IsArray()
  @ArrayMaxSize(MAX_TRIP_DAYS)
  @ValidateNested({ each: true })
  @Type(() => ItineraryDayDto)
  days: ItineraryDayDto[];
}
