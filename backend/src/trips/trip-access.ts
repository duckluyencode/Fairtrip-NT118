import { HttpStatus, Injectable } from '@nestjs/common';
import { AppError } from '../common/errors/app-error.js';
import { ErrorCode } from '../common/errors/error-code.js';
import type { Prisma } from '../generated/prisma/client.js';
import { PrismaService } from '../prisma/prisma.service.js';

export const tripInclude = {
  members: {
    include: { user: { select: { fullName: true } }, interests: true },
    orderBy: [{ joinedAt: 'asc' }, { id: 'asc' }],
  },
  lodging: { include: { place: { include: { categories: true } } } },
} satisfies Prisma.TripInclude;

export type LoadedTrip = Prisma.TripGetPayload<{ include: typeof tripInclude }>;
export type LoadedMember = LoadedTrip['members'][number];

/**
 * Any change to what the planner uses (members, budgets, interests, places, votes,
 * lodging) makes the itinerary stale, so it is removed and the trip goes back to DRAFT.
 */
export async function resetPlan(
  tx: Prisma.TransactionClient,
  tripId: number,
): Promise<void> {
  await tx.tripStop.deleteMany({ where: { tripId } });
  await tx.tripMember.updateMany({
    where: { tripId },
    data: { lodgingShare: 0, satisfaction: null },
  });
  await tx.trip.update({
    where: { id: tripId },
    data: { status: 'DRAFT', plannedAt: null },
  });
}

export function tripNotFound(): AppError {
  return new AppError(
    HttpStatus.NOT_FOUND,
    ErrorCode.TripNotFound,
    'No such trip',
  );
}

/** Loads trips for their members only; everyone else gets TRIP_NOT_FOUND. */
@Injectable()
export class TripAccess {
  constructor(private readonly prisma: PrismaService) {}

  async load(
    userId: number,
    tripId: number,
  ): Promise<{ trip: LoadedTrip; me: LoadedMember }> {
    const trip = await this.prisma.trip.findUnique({
      where: { id: tripId },
      include: tripInclude,
    });
    const me = trip?.members.find((m) => m.userId === userId);
    if (!trip || !me) {
      throw tripNotFound();
    }
    return { trip, me };
  }

  /** Planning, lodging and deleting the trip are the owner's. */
  async loadAsOwner(
    userId: number,
    tripId: number,
  ): Promise<{ trip: LoadedTrip; me: LoadedMember }> {
    const loaded = await this.load(userId, tripId);
    if (loaded.me.role !== 'OWNER') {
      throw new AppError(
        HttpStatus.FORBIDDEN,
        ErrorCode.NotTripOwner,
        'Only the trip owner can do this',
      );
    }
    return loaded;
  }

  /** See resetPlan. */
  resetPlan(tx: Prisma.TransactionClient, tripId: number): Promise<void> {
    return resetPlan(tx, tripId);
  }
}
