import { createPublicKey, verify, type JsonWebKey } from 'node:crypto';
import { HttpStatus, Injectable, Logger } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { AppError } from '../../common/errors/app-error.js';
import { ErrorCode } from '../../common/errors/error-code.js';

const KEYS_URL = 'https://www.googleapis.com/oauth2/v3/certs';
const TIMEOUT_MS = 10_000;
const ISSUERS = ['accounts.google.com', 'https://accounts.google.com'];
/** Used when Google's answer does not say how long its keys may be kept. */
const DEFAULT_KEYS_TTL_MS = 60 * 60 * 1000;
/** Unknown key ids trigger a new download at most this often. */
const MIN_REFETCH_MS = 60 * 1000;
/** Allowed difference between Google's clock and this server's. */
const CLOCK_SKEW_SECONDS = 60;

/** The Google account an ID token proves. */
export interface GoogleIdentity {
  /** Google's stable id of the account; the email can change, this cannot. */
  sub: string;
  /** Lower case; Google has verified it. */
  email: string;
  name: string | null;
  picture: string | null;
}

/** Checks Google ID tokens. Injected as a token so tests can replace it. */
export abstract class GoogleIdTokenVerifier {
  abstract readonly enabled: boolean;
  /**
   * The account behind the token, or null when the token is not a valid Google ID token
   * for this app. Throws GOOGLE_SIGN_IN_UNAVAILABLE when it cannot be checked.
   */
  abstract verify(idToken: string): Promise<GoogleIdentity | null>;
}

export function googleSignInUnavailable(): AppError {
  return new AppError(
    HttpStatus.SERVICE_UNAVAILABLE,
    ErrorCode.GoogleSignInUnavailable,
    'Google sign-in is not available right now',
  );
}

interface GoogleJwk extends JsonWebKey {
  kid?: string;
}

interface IdTokenPayload {
  iss?: unknown;
  aud?: unknown;
  exp?: unknown;
  sub?: unknown;
  email?: unknown;
  email_verified?: unknown;
  name?: unknown;
  picture?: unknown;
}

/**
 * Verifies the token's RS256 signature with Google's public keys (downloaded and kept
 * for as long as Google allows), then its issuer, audience and expiry. The audience is
 * GOOGLE_CLIENT_ID, the OAuth web client the Android app asks tokens for; without it
 * Google sign-in is disabled.
 */
@Injectable()
export class HttpGoogleIdTokenVerifier extends GoogleIdTokenVerifier {
  private readonly logger = new Logger(HttpGoogleIdTokenVerifier.name);
  private readonly clientId?: string;
  readonly enabled: boolean;

  private keys = new Map<string, GoogleJwk>();
  private keysExpireAt = 0;
  private keysFetchedAt = 0;

  constructor(config: ConfigService) {
    super();
    this.clientId = config.get<string>('GOOGLE_CLIENT_ID') || undefined;
    this.enabled = Boolean(this.clientId);
  }

  async verify(idToken: string): Promise<GoogleIdentity | null> {
    if (!this.clientId) {
      throw googleSignInUnavailable();
    }
    const parts = idToken.split('.');
    if (parts.length !== 3) {
      return null;
    }
    const header = decodeJson<{ alg?: unknown; kid?: unknown }>(parts[0]);
    const payload = decodeJson<IdTokenPayload>(parts[1]);
    if (
      !header ||
      !payload ||
      header.alg !== 'RS256' ||
      typeof header.kid !== 'string'
    ) {
      return null;
    }
    const key = await this.key(header.kid);
    if (!key) {
      return null;
    }
    let signed: boolean;
    try {
      signed = verify(
        'RSA-SHA256',
        Buffer.from(`${parts[0]}.${parts[1]}`),
        createPublicKey({ key, format: 'jwk' }),
        Buffer.from(parts[2], 'base64url'),
      );
    } catch {
      signed = false;
    }
    if (!signed) {
      return null;
    }
    const now = Date.now() / 1000;
    if (
      typeof payload.iss !== 'string' ||
      !ISSUERS.includes(payload.iss) ||
      payload.aud !== this.clientId ||
      typeof payload.exp !== 'number' ||
      payload.exp + CLOCK_SKEW_SECONDS < now ||
      typeof payload.sub !== 'string' ||
      !payload.sub ||
      typeof payload.email !== 'string' ||
      !payload.email ||
      // Accounts are joined by email, so only an email Google has verified counts.
      payload.email_verified !== true
    ) {
      return null;
    }
    return {
      sub: payload.sub,
      email: payload.email.toLowerCase(),
      name: typeof payload.name === 'string' ? payload.name : null,
      picture: typeof payload.picture === 'string' ? payload.picture : null,
    };
  }

  /** The key with this id; Google rotates keys, so an unknown id downloads them again. */
  private async key(kid: string): Promise<GoogleJwk | null> {
    const now = Date.now();
    const known = this.keys.get(kid);
    if (known && now < this.keysExpireAt) {
      return known;
    }
    if (!known && now - this.keysFetchedAt < MIN_REFETCH_MS) {
      return null;
    }
    await this.fetchKeys();
    return this.keys.get(kid) ?? null;
  }

  private async fetchKeys(): Promise<void> {
    try {
      const res = await fetch(KEYS_URL, {
        signal: AbortSignal.timeout(TIMEOUT_MS),
      });
      if (!res.ok) {
        throw new Error(`HTTP ${res.status}`);
      }
      const body = (await res.json()) as { keys?: GoogleJwk[] };
      const keys = new Map<string, GoogleJwk>();
      for (const key of body.keys ?? []) {
        if (key.kid && key.kty === 'RSA') {
          keys.set(key.kid, key);
        }
      }
      const maxAge = /max-age=(\d+)/.exec(
        res.headers.get('cache-control') ?? '',
      );
      this.keys = keys;
      this.keysFetchedAt = Date.now();
      this.keysExpireAt =
        this.keysFetchedAt +
        (maxAge ? Number(maxAge[1]) * 1000 : DEFAULT_KEYS_TTL_MS);
    } catch (error) {
      this.logger.warn(`Google keys could not be read: ${String(error)}`);
      throw googleSignInUnavailable();
    }
  }
}

function decodeJson<T>(part: string): T | null {
  try {
    const value: unknown = JSON.parse(
      Buffer.from(part, 'base64url').toString('utf8'),
    );
    return typeof value === 'object' && value !== null ? (value as T) : null;
  } catch {
    return null;
  }
}
