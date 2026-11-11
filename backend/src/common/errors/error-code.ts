// Machine-readable error codes returned in every error response (`code`).
// The Android app maps these to its own messages, so never rename a published code.
export const ErrorCode = {
  BadRequest: 'BAD_REQUEST',
  ValidationFailed: 'VALIDATION_FAILED',
  Unauthorized: 'UNAUTHORIZED',
  Forbidden: 'FORBIDDEN',
  NotFound: 'NOT_FOUND',
  Conflict: 'CONFLICT',
  TooManyRequests: 'TOO_MANY_REQUESTS',
  PayloadTooLarge: 'PAYLOAD_TOO_LARGE',
  Internal: 'INTERNAL_ERROR',
  DatabaseUnavailable: 'DATABASE_UNAVAILABLE',

  // Accounts and codes; mirror AuthError in the Android app.
  WrongCredentials: 'WRONG_CREDENTIALS',
  AccountExists: 'ACCOUNT_EXISTS',
  AccountNotFound: 'ACCOUNT_NOT_FOUND',
  WrongCode: 'WRONG_CODE',
  CodeExpired: 'CODE_EXPIRED',
  /** Too many wrong guesses; a new code must be requested. */
  CodeLocked: 'CODE_LOCKED',
  NoPendingCode: 'NO_PENDING_CODE',
  ResendTooSoon: 'RESEND_TOO_SOON',
  CodeDeliveryFailed: 'CODE_DELIVERY_FAILED',
  /** Refresh or password reset token is unknown, expired, revoked or already used. */
  InvalidToken: 'INVALID_TOKEN',

  // Places
  PlaceNotFound: 'PLACE_NOT_FOUND',
  GooglePlacesUnavailable: 'GOOGLE_PLACES_UNAVAILABLE',

  // Trips and groups
  /** Also returned to non-members, so trip ids cannot be probed. */
  TripNotFound: 'TRIP_NOT_FOUND',
  NotTripOwner: 'NOT_TRIP_OWNER',
  InvalidTripDates: 'INVALID_TRIP_DATES',
  InviteCodeNotFound: 'INVITE_CODE_NOT_FOUND',
  AlreadyMember: 'ALREADY_MEMBER',
  OwnerCannotLeave: 'OWNER_CANNOT_LEAVE',
  HotelNotFound: 'HOTEL_NOT_FOUND',
  /** Someone's share of the room is more than their budget. */
  LodgingOverBudget: 'LODGING_OVER_BUDGET',
  /** A one-day trip has no night to book. */
  LodgingNotNeeded: 'LODGING_NOT_NEEDED',
  /** The trip is set to stay in a hotel but none has been chosen yet. */
  LodgingNotChosen: 'LODGING_NOT_CHOSEN',
  OptionNotFound: 'OPTION_NOT_FOUND',
  /** The vote deadline has passed. */
  VotingClosed: 'VOTING_CLOSED',
  /** The member already sent their ballot; votes cannot change any more. */
  BallotAlreadySent: 'BALLOT_ALREADY_SENT',
  /** No place could be found to plan from (none picked, none found for the city). */
  NoPlacesToPlan: 'NO_PLACES_TO_PLAN',
  /** A self-designed itinerary uses a day outside the trip or a place twice. */
  InvalidItinerary: 'INVALID_ITINERARY',
  /** The trip has no itinerary yet (or its inputs changed since it was planned). */
  ItineraryNotReady: 'ITINERARY_NOT_READY',

  // Emergency contacts
  ContactNotFound: 'CONTACT_NOT_FOUND',
  ContactExists: 'CONTACT_EXISTS',
  ContactLimitReached: 'CONTACT_LIMIT_REACHED',

  // Photo recognition
  /** No photo, or not a JPEG, PNG or WebP image. */
  InvalidImage: 'INVALID_IMAGE',
  /** Cloud Vision is not configured or cannot answer right now. */
  RecognitionUnavailable: 'RECOGNITION_UNAVAILABLE',
} as const;

export type ErrorCode = (typeof ErrorCode)[keyof typeof ErrorCode];
