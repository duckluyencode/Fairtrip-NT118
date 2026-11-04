import { HttpStatus, Injectable, Logger } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { createHmac, randomInt, timingSafeEqual } from 'node:crypto';
import { AppError } from '../../common/errors/app-error.js';
import { ErrorCode } from '../../common/errors/error-code.js';
import type { OtpPurpose } from '../../generated/prisma/enums.js';
import { PrismaService } from '../../prisma/prisma.service.js';
import type { Contact } from '../contact.js';
import { OtpDelivery } from './otp-delivery.js';

// Same timings as the Android app: valid 15 minutes, resend after 60 seconds.
export const OTP_TTL_SECONDS = 15 * 60;
export const OTP_RESEND_SECONDS = 60;
export const OTP_MAX_ATTEMPTS = 5;

export interface CodeSent {
  channel: 'EMAIL' | 'SMS';
  sentTo: string;
  expiresInSeconds: number;
  resendInSeconds: number;
}

/** Issues and checks 6-digit codes. Only the latest code of a user and purpose counts. */
@Injectable()
export class OtpService {
  private readonly logger = new Logger(OtpService.name);
  private readonly secret: string;

  constructor(
    private readonly prisma: PrismaService,
    private readonly delivery: OtpDelivery,
    config: ConfigService,
  ) {
    this.secret = config.getOrThrow<string>('JWT_SECRET');
  }

  /** Throws RESEND_TOO_SOON while the previous code is younger than the resend delay. */
  async assertCanSend(userId: number, purpose: OtpPurpose): Promise<void> {
    const latest = await this.latest(userId, purpose);
    if (!latest) {
      return;
    }
    const elapsed = (Date.now() - latest.createdAt.getTime()) / 1000;
    const wait = Math.ceil(OTP_RESEND_SECONDS - elapsed);
    if (wait > 0) {
      throw new AppError(
        HttpStatus.TOO_MANY_REQUESTS,
        ErrorCode.ResendTooSoon,
        `Wait ${wait} seconds before requesting a new code`,
        { retryAfterSeconds: wait },
      );
    }
  }

  async hasCode(userId: number, purpose: OtpPurpose): Promise<boolean> {
    return (await this.latest(userId, purpose)) !== null;
  }

  async send(
    userId: number,
    purpose: OtpPurpose,
    contact: Contact,
  ): Promise<CodeSent> {
    await this.assertCanSend(userId, purpose);
    const code = randomInt(0, 1_000_000).toString().padStart(6, '0');
    const otp = await this.prisma.otpCode.create({
      data: {
        userId,
        purpose,
        destination: contact.value,
        codeHash: this.hash(userId, purpose, code),
        expiresAt: new Date(Date.now() + OTP_TTL_SECONDS * 1000),
      },
    });
    try {
      await this.delivery.send({
        contact,
        code,
        purpose,
        validMinutes: OTP_TTL_SECONDS / 60,
      });
    } catch (error) {
      // Drop the unsent code so the user can retry at once instead of waiting.
      await this.prisma.otpCode.delete({ where: { id: otp.id } });
      this.logger.error(
        `Could not send a ${purpose} code`,
        error instanceof Error ? error.stack : error,
      );
      throw new AppError(
        HttpStatus.SERVICE_UNAVAILABLE,
        ErrorCode.CodeDeliveryFailed,
        'Could not send the code, try again later',
      );
    }
    return {
      channel: contact.kind === 'email' ? 'EMAIL' : 'SMS',
      sentTo: contact.value,
      expiresInSeconds: OTP_TTL_SECONDS,
      resendInSeconds: OTP_RESEND_SECONDS,
    };
  }

  /** Checks a code and marks it used; throws the matching error code otherwise. */
  async verify(
    userId: number,
    purpose: OtpPurpose,
    code: string,
  ): Promise<void> {
    const otp = await this.latest(userId, purpose);
    if (!otp || otp.usedAt) {
      throw new AppError(
        HttpStatus.BAD_REQUEST,
        ErrorCode.NoPendingCode,
        'No code is waiting to be checked',
      );
    }
    if (otp.expiresAt <= new Date()) {
      throw new AppError(
        HttpStatus.BAD_REQUEST,
        ErrorCode.CodeExpired,
        'The code has expired',
      );
    }
    // Count the attempt before comparing, atomically, so parallel guesses cannot
    // get past the limit.
    const { count } = await this.prisma.otpCode.updateMany({
      where: { id: otp.id, usedAt: null, attempts: { lt: OTP_MAX_ATTEMPTS } },
      data: { attempts: { increment: 1 } },
    });
    if (count === 0) {
      throw codeLocked();
    }
    if (!this.matches(otp.codeHash, userId, purpose, code)) {
      const attemptsLeft = OTP_MAX_ATTEMPTS - (otp.attempts + 1);
      if (attemptsLeft <= 0) {
        throw codeLocked();
      }
      throw new AppError(
        HttpStatus.BAD_REQUEST,
        ErrorCode.WrongCode,
        'The code is not correct',
        { attemptsLeft },
      );
    }
    const used = await this.prisma.otpCode.updateMany({
      where: { id: otp.id, usedAt: null },
      data: { usedAt: new Date() },
    });
    if (used.count === 0) {
      throw new AppError(
        HttpStatus.BAD_REQUEST,
        ErrorCode.NoPendingCode,
        'No code is waiting to be checked',
      );
    }
  }

  private latest(userId: number, purpose: OtpPurpose) {
    return this.prisma.otpCode.findFirst({
      where: { userId, purpose },
      orderBy: { id: 'desc' },
    });
  }

  // HMAC with the server secret: a 6-digit code is too easy to brute-force from a
  // plain hash if the database leaks.
  private hash(userId: number, purpose: OtpPurpose, code: string): string {
    return createHmac('sha256', this.secret)
      .update(`${userId}:${purpose}:${code}`)
      .digest('hex');
  }

  private matches(
    storedHash: string,
    userId: number,
    purpose: OtpPurpose,
    code: string,
  ): boolean {
    const expected = Buffer.from(storedHash, 'hex');
    const actual = Buffer.from(this.hash(userId, purpose, code), 'hex');
    return (
      expected.length === actual.length && timingSafeEqual(expected, actual)
    );
  }
}

function codeLocked(): AppError {
  return new AppError(
    HttpStatus.TOO_MANY_REQUESTS,
    ErrorCode.CodeLocked,
    'Too many wrong codes; request a new code',
  );
}
