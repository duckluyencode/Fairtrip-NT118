import { HttpStatus, Injectable } from '@nestjs/common';
import { AppError } from '../common/errors/app-error.js';
import { ErrorCode } from '../common/errors/error-code.js';
import { Prisma } from '../generated/prisma/client.js';
import { OtpPurpose } from '../generated/prisma/enums.js';
import { PrismaService } from '../prisma/prisma.service.js';
import { UserResponse } from '../users/user.response.js';
import { contactWhere, parseContact, type Contact } from './contact.js';
import type {
  CodeSentResponse,
  ResetTokenResponse,
  SessionResponse,
  TokensResponse,
} from './dto/auth.response.js';
import { OtpService } from './otp/otp.service.js';
import { hashPassword, verifyPassword } from './password.js';
import { RESET_TOKEN_TTL_SECONDS, TokenService } from './token.service.js';

/**
 * Sign-up, sign-in and password reset, following AuthRepository in the Android app.
 * A sign-up creates an unverified account; it can sign in once its code is confirmed.
 */
@Injectable()
export class AuthService {
  // Checked when the account does not exist, so a wrong contact takes as long as a
  // wrong password.
  private readonly dummyHash = hashPassword('timing-equalizer-1');

  constructor(
    private readonly prisma: PrismaService,
    private readonly otp: OtpService,
    private readonly tokens: TokenService,
  ) {}

  async register(
    fullName: string,
    rawContact: string,
    password: string,
  ): Promise<CodeSentResponse> {
    const contact = toContact(rawContact);
    const existing = await this.prisma.user.findUnique({
      where: contactWhere(contact),
    });
    if (existing?.verifiedAt) {
      throw new AppError(
        HttpStatus.CONFLICT,
        ErrorCode.AccountExists,
        'An account with this contact already exists',
      );
    }
    if (existing) {
      await this.otp.assertCanSend(existing.id, OtpPurpose.REGISTER);
    }
    const passwordHash = await hashPassword(password);
    // Signing up again before verifying replaces the name and password.
    const user = existing
      ? await this.prisma.user.update({
          where: { id: existing.id },
          data: { fullName, passwordHash },
        })
      : await this.createUser(contact, fullName, passwordHash);
    return this.otp.send(user.id, OtpPurpose.REGISTER, contact);
  }

  async verifyRegistration(
    rawContact: string,
    code: string,
  ): Promise<SessionResponse> {
    const contact = toContact(rawContact);
    const user = await this.prisma.user.findUnique({
      where: contactWhere(contact),
    });
    if (!user || user.verifiedAt) {
      throw noPendingCode();
    }
    await this.otp.verify(user.id, OtpPurpose.REGISTER, code);
    const verified = await this.prisma.user.update({
      where: { id: user.id },
      data: { verifiedAt: new Date() },
    });
    return this.session(verified.id, UserResponse.from(verified));
  }

  async resendCode(
    rawContact: string,
    purpose: OtpPurpose,
  ): Promise<CodeSentResponse> {
    const contact = toContact(rawContact);
    const user = await this.prisma.user.findUnique({
      where: contactWhere(contact),
    });
    // Registration codes go to unverified accounts, reset codes to verified ones.
    const eligible =
      user &&
      (purpose === OtpPurpose.REGISTER ? !user.verifiedAt : user.verifiedAt);
    if (!user || !eligible || !(await this.otp.hasCode(user.id, purpose))) {
      throw noPendingCode();
    }
    return this.otp.send(user.id, purpose, contact);
  }

  async login(rawContact: string, password: string): Promise<SessionResponse> {
    const contact = parseContact(rawContact);
    const user = contact
      ? await this.prisma.user.findUnique({ where: contactWhere(contact) })
      : null;
    let valid = false;
    if (user?.verifiedAt) {
      valid = await verifyPassword(password, user.passwordHash);
    } else {
      await verifyPassword(password, await this.dummyHash);
    }
    if (!user || !valid) {
      throw new AppError(
        HttpStatus.UNAUTHORIZED,
        ErrorCode.WrongCredentials,
        'Wrong contact or password',
      );
    }
    return this.session(user.id, UserResponse.from(user));
  }

  refresh(refreshToken: string): Promise<TokensResponse> {
    return this.tokens.rotate(refreshToken);
  }

  logout(refreshToken: string): Promise<void> {
    return this.tokens.revoke(refreshToken);
  }

  async forgotPassword(rawContact: string): Promise<CodeSentResponse> {
    const contact = toContact(rawContact);
    const user = await this.prisma.user.findUnique({
      where: contactWhere(contact),
    });
    if (!user?.verifiedAt) {
      throw new AppError(
        HttpStatus.NOT_FOUND,
        ErrorCode.AccountNotFound,
        'No account uses this contact',
      );
    }
    return this.otp.send(user.id, OtpPurpose.RESET_PASSWORD, contact);
  }

  async verifyPasswordReset(
    rawContact: string,
    code: string,
  ): Promise<ResetTokenResponse> {
    const contact = toContact(rawContact);
    const user = await this.prisma.user.findUnique({
      where: contactWhere(contact),
    });
    if (!user?.verifiedAt) {
      throw noPendingCode();
    }
    await this.otp.verify(user.id, OtpPurpose.RESET_PASSWORD, code);
    return {
      resetToken: await this.tokens.signPasswordReset(user.id),
      expiresInSeconds: RESET_TOKEN_TTL_SECONDS,
    };
  }

  /** Sets the new password and signs out every device. Each reset token works once. */
  async resetPassword(resetToken: string, password: string): Promise<void> {
    const { userId, issuedAt } =
      await this.tokens.verifyPasswordReset(resetToken);
    const passwordHash = await hashPassword(password);
    // Conditional update: a token issued before the last password change is spent.
    const { count } = await this.prisma.user.updateMany({
      where: {
        id: userId,
        OR: [
          { passwordChangedAt: null },
          { passwordChangedAt: { lt: issuedAt } },
        ],
      },
      data: { passwordHash, passwordChangedAt: new Date() },
    });
    if (count === 0) {
      throw new AppError(
        HttpStatus.UNAUTHORIZED,
        ErrorCode.InvalidToken,
        'Token is invalid or expired',
      );
    }
    await this.tokens.revokeAll(userId);
  }

  private async session(
    userId: number,
    user: UserResponse,
  ): Promise<SessionResponse> {
    return { ...(await this.tokens.issueSession(userId)), user };
  }

  private async createUser(
    contact: Contact,
    fullName: string,
    passwordHash: string,
  ) {
    try {
      return await this.prisma.user.create({
        data: { ...contactWhere(contact), fullName, passwordHash },
      });
    } catch (error) {
      // Another sign-up with the same contact won the race.
      if (
        error instanceof Prisma.PrismaClientKnownRequestError &&
        error.code === 'P2002'
      ) {
        throw new AppError(
          HttpStatus.CONFLICT,
          ErrorCode.AccountExists,
          'An account with this contact already exists',
        );
      }
      throw error;
    }
  }
}

// DTOs already validated the contact; this only narrows the type.
function toContact(raw: string): Contact {
  const contact = parseContact(raw);
  if (!contact) {
    throw new AppError(
      HttpStatus.BAD_REQUEST,
      ErrorCode.ValidationFailed,
      'contact must be an email or a Vietnamese mobile number',
    );
  }
  return contact;
}

function noPendingCode(): AppError {
  return new AppError(
    HttpStatus.BAD_REQUEST,
    ErrorCode.NoPendingCode,
    'No code is waiting to be checked',
  );
}
