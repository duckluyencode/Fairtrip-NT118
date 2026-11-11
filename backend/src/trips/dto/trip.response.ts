import type {
  Interest,
  LodgingMode,
  PlaceCategory,
  PriceSource,
  TripRole,
  TripStatus,
  TripType,
} from '../../generated/prisma/enums.js';
import type { PriceResponse } from '../../places/dto/place.response.js';

export class TripMemberResponse {
  memberId: number;
  fullName: string;
  role: TripRole;
  isMe: boolean;
  interests: Interest[];
  /** Only for yourself: the group never sees anyone else's budget. */
  budget: number | null;
  /** Lodging plus places, once the trip is planned; visible to the whole group. */
  spent: number | null;
  /** 0–100, once the trip is planned. */
  satisfaction: number | null;
  ballotSent: boolean;
}

export class LodgingResponse {
  /** The hotel's place id (GET /places/:id shows its details). */
  placeId: number;
  /** Read live from Google; null when Google cannot answer right now. */
  name: string | null;
  address: string | null;
  location: { lat: number; lng: number } | null;
  /** YYYY-MM-DD */
  checkIn: string;
  checkOut: string;
  nights: number;
  rooms: number;
  guestsPerRoom: number;
  /** VND for one room and one night, copied when the hotel was chosen. */
  pricePerNight: number;
  priceSource: PriceSource;
  provider: string;
  /** Show "~" before the price when true. */
  isEstimate: boolean;
  /** Your part of the room cost for the whole stay. */
  myShare: number;
  attribution: string | null;
}

export class TripSummaryResponse {
  id: number;
  type: TripType;
  name: string | null;
  city: string;
  startDate: string;
  endDate: string;
  days: number;
  status: TripStatus;
  memberCount: number;
  myRole: TripRole;
}

export class TripDetailResponse extends TripSummaryResponse {
  dayStart: string;
  dayEnd: string;
  gapMinutes: number;
  lodgingMode: LodgingMode;
  /** Group trips: the code friends type to join. */
  inviteCode: string | null;
  voteDeadline: Date | null;
  members: TripMemberResponse[];
  lodging: LodgingResponse | null;
}

export class LodgingOptionResponse {
  /** CHEAPEST, BALANCED (rating, price and distance together) or NEAREST (to the places). */
  tag: 'CHEAPEST' | 'BALANCED' | 'NEAREST';
  /** Pass as placeId to PUT /trips/:id/lodging. */
  placeId: number;
  name: string;
  address: string | null;
  location: { lat: number; lng: number } | null;
  rating: number | null;
  /** km from the middle of the trip's places. */
  distanceKm: number | null;
  /** VND for one room and one night. */
  pricePerNight: number;
  priceSource: PriceSource;
  provider: string;
  isEstimate: boolean;
  nights: number;
  rooms: number;
  /** Your part of the room cost for the whole stay. */
  myShare: number;
  /** Your budget left for places after the room. */
  leftForPlaces: number;
  attribution: string;
}

export class VoteOptionResponse {
  id: number;
  placeId: number;
  /** Null when Google cannot return the place right now. */
  name: string | null;
  area: string | null;
  category: PlaceCategory | null;
  imageUrl: string | null;
  /** Prices are shown in voting, with their source. */
  price: PriceResponse | null;
  attribution: string | null;
  votes: number;
  votedByMe: boolean;
  addedByMe: boolean;
}

export class VoteOptionList {
  items: VoteOptionResponse[];
  memberCount: number;
  ballotsSent: number;
  mySentAt: Date | null;
}

export class StopParticipantResponse {
  memberId: number;
  fullName: string;
}

export class ItineraryStopResponse {
  stopId: number;
  position: number;
  /** "HH:mm" */
  time: string;
  travelMinutes: number;
  visitMinutes: number;
  placeId: number;
  /** Null when Google cannot return the place right now. */
  name: string | null;
  area: string | null;
  category: PlaceCategory | null;
  location: { lat: number; lng: number } | null;
  attribution: string | null;
  /** Price per person when planned, with its source. */
  price: {
    perPerson: number;
    source: PriceSource;
    provider: string;
    isEstimate: boolean;
  };
  /** What you pay here (0 when your area ticket covers it); null if you skip this stop. */
  myCost: number | null;
  participants: StopParticipantResponse[];
  /** Your spending so far, lodging included, after this stop. */
  runningTotal: number;
  /** Your budget minus runningTotal. */
  remaining: number;
}

export class ItineraryDayResponse {
  day: number;
  date: string;
  stops: ItineraryStopResponse[];
}

export class ItineraryWarning {
  /** DAY_TOO_LONG, PLACE_CLOSED, OVER_BUDGET (yours), MEMBER_OVER_BUDGET (someone else's). */
  code: string;
  day?: number;
  placeId?: number;
}

export class ItineraryResponse {
  tripId: number;
  status: TripStatus;
  /** Always tickets, food and lodging; transport is not included. */
  budgetIncludes: string[];
  lodging: LodgingResponse | null;
  days: ItineraryDayResponse[];
  me: {
    budget: number;
    lodgingShare: number;
    spentOnPlaces: number;
    total: number;
    remaining: number;
  };
  members: {
    memberId: number;
    fullName: string;
    spent: number;
    satisfaction: number | null;
  }[];
  /** Jain's fairness index of members' satisfaction, 0–1. */
  fairness: number | null;
  warnings: ItineraryWarning[];
}
