import { HttpStatus, Injectable } from '@nestjs/common';
import { AppError } from '../common/errors/app-error.js';
import { ErrorCode } from '../common/errors/error-code.js';
import { PlacesService } from '../places/places.service.js';
import { PrismaService } from '../prisma/prisma.service.js';
import type { VoteOptionList } from './dto/trip.response.js';
import {
  TripAccess,
  type LoadedMember,
  type LoadedTrip,
} from './trip-access.js';
import { TripPlacesService } from './trip-places.js';

function optionNotFound(): AppError {
  return new AppError(
    HttpStatus.NOT_FOUND,
    ErrorCode.OptionNotFound,
    'No such option in this trip',
  );
}

/**
 * Group voting ("Bình chọn"): members propose places, tick the ones they like and send
 * their ballot. Votes make a place worth more to the planner for the members who voted.
 * Any change makes the itinerary stale, so the trip has to be planned again.
 */
@Injectable()
export class VotesService {
  constructor(
    private readonly prisma: PrismaService,
    private readonly access: TripAccess,
    private readonly places: PlacesService,
    private readonly tripPlaces: TripPlacesService,
  ) {}

  /** Options with their price and votes, most voted first. */
  async list(userId: number, tripId: number): Promise<VoteOptionList> {
    const { trip, me } = await this.access.load(userId, tripId);
    const options = await this.prisma.voteOption.findMany({
      where: { tripId },
      include: { votes: { select: { memberId: true } } },
      orderBy: { createdAt: 'asc' },
    });
    const places = await this.tripPlaces.resolve(options.map((o) => o.placeId));
    const items = options
      .map((option) => {
        const place = places.get(option.placeId);
        return {
          id: option.id,
          placeId: option.placeId,
          name: place?.name ?? null,
          area: place?.area ?? null,
          category: place?.category ?? null,
          imageUrl: place?.imageUrl ?? null,
          price: place?.price ?? null,
          attribution: place?.attribution ?? null,
          votes: option.votes.length,
          votedByMe: option.votes.some((v) => v.memberId === me.id),
          addedByMe: option.addedById === me.id,
        };
      })
      .sort((a, b) => b.votes - a.votes || a.id - b.id);
    return {
      items,
      memberCount: trip.members.length,
      ballotsSent: trip.members.filter((m) => m.ballotSentAt !== null).length,
      mySentAt: me.ballotSentAt,
    };
  }

  /** Proposes a place (and votes for it); proposing it again changes nothing. */
  async add(
    userId: number,
    tripId: number,
    placeId: number,
  ): Promise<VoteOptionList> {
    const { trip, me } = await this.access.load(userId, tripId);
    // Deadlines and sent ballots lock members' votes; the owner still runs the list.
    if (me.role !== 'OWNER') {
      this.assertCanVote(trip, me);
    }
    await this.places.assertExists(placeId);
    const existing = await this.prisma.voteOption.findUnique({
      where: { tripId_placeId: { tripId, placeId } },
    });
    if (!existing) {
      await this.prisma.$transaction(async (tx) => {
        await tx.voteOption.create({
          data: {
            tripId,
            placeId,
            addedById: me.id,
            votes: { create: { memberId: me.id } },
          },
        });
        await this.access.resetPlan(tx, tripId);
      });
    }
    return this.list(userId, tripId);
  }

  /** Only the member who proposed an option, or the owner, can take it back. */
  async remove(
    userId: number,
    tripId: number,
    optionId: number,
  ): Promise<void> {
    const { trip, me } = await this.access.load(userId, tripId);
    const option = await this.optionOf(tripId, optionId);
    if (option.addedById !== me.id && me.role !== 'OWNER') {
      throw new AppError(
        HttpStatus.FORBIDDEN,
        ErrorCode.NotTripOwner,
        'Only who proposed this place or the trip owner can remove it',
      );
    }
    if (me.role !== 'OWNER') {
      this.assertCanVote(trip, me);
    }
    await this.prisma.$transaction(async (tx) => {
      await tx.voteOption.delete({ where: { id: option.id } });
      await this.access.resetPlan(tx, tripId);
    });
  }

  /** Ticks or unticks an option; repeating the same choice changes nothing. */
  async setVote(
    userId: number,
    tripId: number,
    optionId: number,
    agree: boolean,
  ): Promise<VoteOptionList> {
    const { trip, me } = await this.access.load(userId, tripId);
    this.assertCanVote(trip, me);
    const option = await this.optionOf(tripId, optionId);
    const key = { optionId_memberId: { optionId: option.id, memberId: me.id } };
    const current = await this.prisma.vote.findUnique({ where: key });
    if (agree !== (current !== null)) {
      await this.prisma.$transaction(async (tx) => {
        if (agree) {
          await tx.vote.create({
            data: { optionId: option.id, memberId: me.id },
          });
        } else {
          await tx.vote.delete({ where: key });
        }
        await this.access.resetPlan(tx, tripId);
      });
    }
    return this.list(userId, tripId);
  }

  /** "Gửi phiếu": the member's votes are final. Sending again changes nothing. */
  async send(userId: number, tripId: number): Promise<VoteOptionList> {
    const { me } = await this.access.load(userId, tripId);
    if (!me.ballotSentAt) {
      await this.prisma.tripMember.update({
        where: { id: me.id },
        data: { ballotSentAt: new Date() },
      });
    }
    return this.list(userId, tripId);
  }

  private async optionOf(tripId: number, optionId: number) {
    const option = await this.prisma.voteOption.findUnique({
      where: { id: optionId },
    });
    if (!option || option.tripId !== tripId) {
      throw optionNotFound();
    }
    return option;
  }

  private assertCanVote(trip: LoadedTrip, me: LoadedMember): void {
    if (trip.voteDeadline && trip.voteDeadline.getTime() < Date.now()) {
      throw new AppError(
        HttpStatus.CONFLICT,
        ErrorCode.VotingClosed,
        'Voting for this trip has closed',
      );
    }
    if (me.ballotSentAt) {
      throw new AppError(
        HttpStatus.CONFLICT,
        ErrorCode.BallotAlreadySent,
        'You already sent your ballot',
      );
    }
  }
}
