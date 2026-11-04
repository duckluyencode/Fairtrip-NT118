import { UserResponse } from '../../users/user.response.js';

export class CodeSentResponse {
  channel: 'EMAIL' | 'SMS';
  /** Normalized email or E.164 phone the code went to. */
  sentTo: string;
  expiresInSeconds: number;
  /** Seconds before another code can be requested. */
  resendInSeconds: number;
}

export class TokensResponse {
  /** Send as `Authorization: Bearer <accessToken>`. */
  accessToken: string;
  /** Exchange at POST /auth/refresh when the access token expires. */
  refreshToken: string;
  /** Seconds until the access token expires. */
  expiresIn: number;
}

export class SessionResponse extends TokensResponse {
  user: UserResponse;
}

export class ResetTokenResponse {
  resetToken: string;
  expiresInSeconds: number;
}
