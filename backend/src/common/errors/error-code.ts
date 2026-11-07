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
} as const;

export type ErrorCode = (typeof ErrorCode)[keyof typeof ErrorCode];
