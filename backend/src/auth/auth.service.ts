import { HttpStatus, Injectable } from '@nestjs/common';
import { AppError } from '../common/errors/app-error.js';
import { ErrorCode } from '../common/errors/error-code.js';
import { Prisma } from '../generated/prisma/client.js';
import { OtpPurpose } from '../generated/prisma/enums.js';
import { PrismaService } from '../prisma/prisma.service.js';
import { UserResponse } from '../users/user.response.js';
import { contactWhere, parseContact, type Contact } from './contact.js';
import {
  GoogleIdTokenVerifier,
  type GoogleIdentity,
} from './google/google-id-token.verifier.js';
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
    private readonly google: GoogleIdTokenVerifier,
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
      throw accountExists();
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
    // An account that only signs in with Google has no password to match.
    if (user?.verifiedAt && user.passwordHash) {
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

  /**
   * Signs in with a Google ID token. A Google account seen before signs in to its
   * account; a new one joins the account that uses its email (Google has verified the
   * email), or gets a new account without a password.
   */
  async googleSignIn(idToken: string): Promise<SessionResponse> {
    const identity = await this.google.verify(idToken);
    if (!identity) {
      throw new AppError(
        HttpStatus.UNAUTHORIZED,
        ErrorCode.InvalidGoogleToken,
        'Google sign-in could not be verified',
      );
    }
    const linked = await this.prisma.user.findUnique({
      where: { googleSub: identity.sub },
    });
    if (linked) {
      return this.session(linked.id, UserResponse.from(linked));
    }
    const existing = await this.prisma.user.findUnique({
      where: { email: identity.email },
    });
    if (!existing) {
      const created = await this.createGoogleUser(identity);
      return this.session(created.id, UserResponse.from(created));
    }
    if (existing.googleSub) {
      // The email now belongs to another Google account than the one linked before.
      throw accountExists();
    }
    const joined = await this.prisma.$transaction(async (tx) => {
      if (!existing.verifiedAt) {
        await tx.otpCode.deleteMany({ where: { userId: existing.id } });
      }
      return tx.user.update({
        where: { id: existing.id },
        data: {
          googleSub: identity.sub,
          avatarUrl: existing.avatarUrl ?? avatarOf(identity),
          // A sign-up that was never verified did not prove it owns the email, so its
          // password may be someone else's: it is dropped and Google's proof takes over.
          ...(existing.verifiedAt
            ? {}
            : { verifiedAt: new Date(), passwordHash: null }),
        },
      });
    });
    return this.session(joined.id, UserResponse.from(joined));
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
        throw accountExists();
      }
      throw error;
    }
  }

  private async createGoogleUser(identity: GoogleIdentity) {
    const name = identity.name?.trim() || identity.email.split('@')[0];
    try {
      return await this.prisma.user.create({
        data: {
          email: identity.email,
          googleSub: identity.sub,
          fullName: name.slice(0, FULL_NAME_MAX),
          avatarUrl: avatarOf(identity),
          verifiedAt: new Date(),
        },
      });
    } catch (error) {
      // The same Google account signed in twice at once; the other request created it.
      if (
        error instanceof Prisma.PrismaClientKnownRequestError &&
        error.code === 'P2002'
      ) {
        const user = await this.prisma.user.findUnique({
          where: { googleSub: identity.sub },
        });
        if (user) {
          return user;
        }
        throw accountExists();
      }
      throw error;
    }
  }
}

/** Column sizes of users.full_name and users.avatar_url. */
const FULL_NAME_MAX = 100;
const AVATAR_URL_MAX = 2048;

function avatarOf(identity: GoogleIdentity): string | null {
  return identity.picture && identity.picture.length <= AVATAR_URL_MAX
    ? identity.picture
    : null;
}

function accountExists(): AppError {
  return new AppError(
    HttpStatus.CONFLICT,
    ErrorCode.AccountExists,
    'An account with this contact already exists',
  );
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
