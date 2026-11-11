import { HttpStatus, Injectable } from '@nestjs/common';
import { GoogleIdTokenVerifier } from '../auth/google/google-id-token.verifier.js';
import { verifyPassword } from '../auth/password.js';
import { AppError } from '../common/errors/app-error.js';
import { ErrorCode } from '../common/errors/error-code.js';
import { PrismaService } from '../prisma/prisma.service.js';
import { resetPlan } from '../trips/trip-access.js';

/**
 * Deletes an account and everything that belongs only to it (required by Google Play:
 * users who can create an account must be able to delete it from the app).
 *
 * Groups are shared, so they are not deleted with their owner while others are still in
 * them: the member who joined first becomes the owner. Trips nobody else is in are
 * deleted. Every trip the user was in has to be planned again, since a member left.
 * Codes, sessions, history, favorites and emergency contacts go with the account
 * (ON DELETE CASCADE).
 */
@Injectable()
export class AccountDeletionService {
  constructor(
    private readonly prisma: PrismaService,
    private readonly google: GoogleIdTokenVerifier,
  ) {}

  /** The account's password, or an ID token of the Google account linked to it. */
  private async proves(
    user: { passwordHash: string | null; googleSub: string | null },
    proof: { password?: string; googleIdToken?: string },
  ): Promise<boolean> {
    if (proof.password !== undefined) {
      return (
        user.passwordHash !== null &&
        (await verifyPassword(proof.password, user.passwordHash))
      );
    }
    if (proof.googleIdToken === undefined || user.googleSub === null) {
      return false;
    }
    const identity = await this.google.verify(proof.googleIdToken);
    return identity?.sub === user.googleSub;
  }

  async delete(
    userId: number,
    proof: { password?: string; googleIdToken?: string },
  ): Promise<void> {
    const user = await this.prisma.user.findUniqueOrThrow({
      where: { id: userId },
    });
    // Asked again so a phone left unlocked cannot delete the account.
    if (!(await this.proves(user, proof))) {
      throw new AppError(
        HttpStatus.FORBIDDEN,
        ErrorCode.WrongCredentials,
        'Wrong password or Google account',
      );
    }
    const memberships = await this.prisma.tripMember.findMany({
      where: { userId },
      include: {
        trip: {
          include: {
            members: { orderBy: [{ joinedAt: 'asc' }, { id: 'asc' }] },
          },
        },
      },
    });
    await this.prisma.$transaction(async (tx) => {
      for (const { trip, role } of memberships) {
        const others = trip.members.filter((m) => m.userId !== userId);
        if (others.length === 0) {
          await tx.trip.delete({ where: { id: trip.id } });
          continue;
        }
        if (role === 'OWNER') {
          const heir = others[0];
          await tx.tripMember.update({
            where: { id: heir.id },
            data: { role: 'OWNER' },
          });
          await tx.trip.update({
            where: { id: trip.id },
            data: { ownerId: heir.userId },
          });
        }
        await resetPlan(tx, trip.id);
      }
      await tx.user.delete({ where: { id: userId } });
    });
  }
}
