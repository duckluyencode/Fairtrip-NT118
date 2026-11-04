import { HttpStatus, Injectable } from '@nestjs/common';
import { JwtService } from '@nestjs/jwt';
import { createHash, randomBytes } from 'node:crypto';
import { AppError } from '../common/errors/app-error.js';
import { ErrorCode } from '../common/errors/error-code.js';
import { PrismaService } from '../prisma/prisma.service.js';

export const ACCESS_TOKEN_TTL_SECONDS = 15 * 60;
export const REFRESH_TOKEN_TTL_SECONDS = 30 * 24 * 60 * 60;
export const RESET_TOKEN_TTL_SECONDS = 10 * 60;

type TokenType = 'access' | 'password_reset';

interface TokenPayload {
  sub: number;
  typ: TokenType;
  iat: number;
}

export interface SessionTokens {
  accessToken: string;
  refreshToken: string;
  /** Seconds until the access token expires. */
  expiresIn: number;
}

export function sha256(value: string): string {
  return createHash('sha256').update(value).digest('hex');
}

function invalidToken(): AppError {
  return new AppError(
    HttpStatus.UNAUTHORIZED,
    ErrorCode.InvalidToken,
    'Token is invalid or expired',
  );
}

/**
 * Access and password-reset tokens are short-lived JWTs told apart by `typ`.
 * Refresh tokens are random strings stored as SHA-256 hashes so they can be revoked.
 */
@Injectable()
export class TokenService {
  constructor(
    private readonly jwt: JwtService,
    private readonly prisma: PrismaService,
  ) {}

  async issueSession(userId: number): Promise<SessionTokens> {
    const accessToken = await this.sign(
      userId,
      'access',
      ACCESS_TOKEN_TTL_SECONDS,
    );
    const refreshToken = randomBytes(32).toString('base64url');
    await this.prisma.refreshToken.create({
      data: {
        userId,
        tokenHash: sha256(refreshToken),
        expiresAt: new Date(Date.now() + REFRESH_TOKEN_TTL_SECONDS * 1000),
      },
    });
    return { accessToken, refreshToken, expiresIn: ACCESS_TOKEN_TTL_SECONDS };
  }

  /**
   * Swaps a refresh token for a new session. Presenting a token that was already
   * rotated means it leaked, so every session of that user is revoked.
   */
  async rotate(refreshToken: string): Promise<SessionTokens> {
    const stored = await this.prisma.refreshToken.findUnique({
      where: { tokenHash: sha256(refreshToken) },
    });
    if (!stored) {
      throw invalidToken();
    }
    if (stored.revokedAt) {
      await this.revokeAll(stored.userId);
      throw invalidToken();
    }
    if (stored.expiresAt <= new Date()) {
      throw invalidToken();
    }
    // Conditional update so only one of two concurrent refreshes can win.
    const { count } = await this.prisma.refreshToken.updateMany({
      where: { id: stored.id, revokedAt: null },
      data: { revokedAt: new Date() },
    });
    if (count === 0) {
      await this.revokeAll(stored.userId);
      throw invalidToken();
    }
    return this.issueSession(stored.userId);
  }

  /** Logout; unknown or already revoked tokens are ignored. */
  async revoke(refreshToken: string): Promise<void> {
    await this.prisma.refreshToken.updateMany({
      where: { tokenHash: sha256(refreshToken), revokedAt: null },
      data: { revokedAt: new Date() },
    });
  }

  async revokeAll(userId: number): Promise<void> {
    await this.prisma.refreshToken.updateMany({
      where: { userId, revokedAt: null },
      data: { revokedAt: new Date() },
    });
  }

  /** User id of a valid access token, or null. */
  async verifyAccessToken(token: string): Promise<number | null> {
    const payload = await this.verify(token, 'access');
    return payload?.sub ?? null;
  }

  signPasswordReset(userId: number): Promise<string> {
    return this.sign(userId, 'password_reset', RESET_TOKEN_TTL_SECONDS);
  }

  /** The user and issue time of a valid reset token; throws INVALID_TOKEN otherwise. */
  async verifyPasswordReset(
    token: string,
  ): Promise<{ userId: number; issuedAt: Date }> {
    const payload = await this.verify(token, 'password_reset');
    if (!payload) {
      throw invalidToken();
    }
    return { userId: payload.sub, issuedAt: new Date(payload.iat * 1000) };
  }

  private sign(
    userId: number,
    typ: TokenType,
    expiresIn: number,
  ): Promise<string> {
    return this.jwt.signAsync({ sub: userId, typ }, { expiresIn });
  }

  private async verify(
    token: string,
    typ: TokenType,
  ): Promise<TokenPayload | null> {
    try {
      const payload = await this.jwt.verifyAsync<TokenPayload>(token);
      return payload.typ === typ && Number.isInteger(payload.sub)
        ? payload
        : null;
    } catch {
      return null;
    }
  }
}
