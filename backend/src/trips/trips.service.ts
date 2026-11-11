import { HttpStatus, Injectable } from '@nestjs/common';
import { randomInt } from 'node:crypto';
import { AppError } from '../common/errors/app-error.js';
import { ErrorCode } from '../common/errors/error-code.js';
import {
  daysInclusive,
  fromDateOnly,
  fromHhmm,
  toDateOnly,
  toHhmm,
} from '../common/time.js';
import { Prisma } from '../generated/prisma/client.js';
import { Interest } from '../generated/prisma/enums.js';
import { PlacesService } from '../places/places.service.js';
import { PrismaService } from '../prisma/prisma.service.js';
import {
  MAX_TRIP_DAYS,
  type CreateTripDto,
  type JoinTripDto,
  type UpdateMembershipDto,
} from './dto/trip.dto.js';
import type {
  TripDetailResponse,
  TripSummaryResponse,
} from './dto/trip.response.js';
import { LodgingService } from './lodging.service.js';
import { TripAccess, type LoadedTrip } from './trip-access.js';

const INTEREST_ORDER = Object.values(Interest);

/** Interests in a fixed order (the enum's), whatever order the database returns. */
function inEnumOrder(interests: Interest[]): Interest[] {
  return [...interests].sort(
    (a, b) => INTEREST_ORDER.indexOf(a) - INTEREST_ORDER.indexOf(b),
  );
}

// No 0/O or 1/I, so a code read aloud or from a screenshot is not mistyped.
const CODE_ALPHABET = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';

function newInviteCode(): string {
  return Array.from(
    { length: 6 },
    () => CODE_ALPHABET[randomInt(CODE_ALPHABET.length)],
  ).join('');
}

function memberOf(trip: LoadedTrip, memberId: number) {
  const member = trip.members.find((m) => m.id === memberId);
  if (!member) {
    throw new AppError(
      HttpStatus.NOT_FOUND,
      ErrorCode.MemberNotFound,
      'No such member in this trip',
    );
  }
  return member;
}

@Injectable()
export class TripsService {
  constructor(
    private readonly prisma: PrismaService,
    private readonly access: TripAccess,
    private readonly places: PlacesService,
    private readonly lodging: LodgingService,
  ) {}

  async create(
    userId: number,
    dto: CreateTripDto,
  ): Promise<TripDetailResponse> {
    const startDate = fromDateOnly(dto.startDate);
    const endDate = fromDateOnly(dto.endDate);
    const days = daysInclusive(startDate, endDate);
    const dayStartMinute = fromHhmm(dto.dayStart ?? '08:00');
    const dayEndMinute = fromHhmm(dto.dayEnd ?? '21:00');
    if (days < 1 || days > MAX_TRIP_DAYS || dayEndMinute <= dayStartMinute) {
      throw new AppError(
        HttpStatus.BAD_REQUEST,
        ErrorCode.InvalidTripDates,
        `A trip lasts 1 to ${MAX_TRIP_DAYS} days and each day must end after it starts`,
      );
    }
    for (const placeId of dto.placeIds ?? []) {
      await this.places.assertExists(placeId);
    }
    // A 0 VND budget cannot pay for a room: "Tự lo chỗ ở" (rule in CLAUDE.md).
    const lodgingMode = dto.budget === 0 ? 'SELF' : (dto.lodging ?? 'SELF');

    for (let attempt = 0; ; attempt++) {
      try {
        const trip = await this.prisma.$transaction(async (tx) => {
          const created = await tx.trip.create({
            data: {
              ownerId: userId,
              type: dto.type,
              name: dto.name ?? null,
              city: dto.city,
              startDate,
              endDate,
              dayStartMinute,
              dayEndMinute,
              gapMinutes: dto.gapMinutes ?? 30,
              lodgingMode,
              inviteCode: dto.type === 'GROUP' ? newInviteCode() : null,
              voteDeadline: dto.voteDeadline
                ? new Date(dto.voteDeadline)
                : null,
              members: {
                create: {
                  userId,
                  role: 'OWNER',
                  budget: dto.budget,
                  interests: {
                    create: dto.interests.map((interest) => ({ interest })),
                  },
                },
              },
            },
            include: { members: true },
          });
          const owner = created.members[0];
          for (const placeId of dto.placeIds ?? []) {
            // Picks are options the owner has already voted for.
            await tx.voteOption.create({
              data: {
                tripId: created.id,
                placeId,
                addedById: owner.id,
                votes: { create: { memberId: owner.id } },
              },
            });
          }
          return created;
        });
        return this.detail(userId, trip.id);
      } catch (error) {
        // An invite code collision: try another code.
        const collision =
          error instanceof Prisma.PrismaClientKnownRequestError &&
          error.code === 'P2002';
        if (!collision || attempt >= 4) {
          throw error;
        }
      }
    }
  }

  async list(userId: number): Promise<TripSummaryResponse[]> {
    const memberships = await this.prisma.tripMember.findMany({
      where: { userId },
      include: { trip: { include: { _count: { select: { members: true } } } } },
      orderBy: { trip: { startDate: 'desc' } },
    });
    return memberships.map(({ trip, role }) => ({
      id: trip.id,
      type: trip.type,
      name: trip.name,
      city: trip.city,
      startDate: toDateOnly(trip.startDate),
      endDate: toDateOnly(trip.endDate),
      days: daysInclusive(trip.startDate, trip.endDate),
      status: trip.status,
      memberCount: trip._count.members,
      myRole: role,
    }));
  }

  async detail(userId: number, tripId: number): Promise<TripDetailResponse> {
    const { trip, me } = await this.access.load(userId, tripId);
    const spent = await this.spentByMember(trip);
    return {
      id: trip.id,
      type: trip.type,
      name: trip.name,
      city: trip.city,
      startDate: toDateOnly(trip.startDate),
      endDate: toDateOnly(trip.endDate),
      days: daysInclusive(trip.startDate, trip.endDate),
      status: trip.status,
      memberCount: trip.members.length,
      myRole: me.role,
      dayStart: toHhmm(trip.dayStartMinute),
      dayEnd: toHhmm(trip.dayEndMinute),
      gapMinutes: trip.gapMinutes,
      lodgingMode: trip.lodgingMode,
      inviteCode: trip.inviteCode,
      voteDeadline: trip.voteDeadline,
      lodging: await this.lodging.view(trip, me),
      members: trip.members.map((member) => ({
        memberId: member.id,
        fullName: member.user.fullName,
        role: member.role,
        isMe: member.id === me.id,
        interests: inEnumOrder(member.interests.map((i) => i.interest)),
        budget: member.id === me.id ? member.budget : null,
        spent: spent.get(member.id) ?? null,
        satisfaction: member.satisfaction,
        ballotSent: member.ballotSentAt !== null,
      })),
    };
  }

  /** Joins a group by its invite code. The trip has to be planned again afterwards. */
  async join(userId: number, dto: JoinTripDto): Promise<TripDetailResponse> {
    const trip = await this.prisma.trip.findUnique({
      where: { inviteCode: dto.inviteCode.toUpperCase() },
      include: { members: { select: { userId: true } } },
    });
    if (!trip) {
      throw new AppError(
        HttpStatus.NOT_FOUND,
        ErrorCode.InviteCodeNotFound,
        'No group uses this code',
      );
    }
    if (trip.members.some((m) => m.userId === userId)) {
      throw new AppError(
        HttpStatus.CONFLICT,
        ErrorCode.AlreadyMember,
        'You are already in this group',
      );
    }
    await this.prisma.$transaction(async (tx) => {
      await tx.tripMember.create({
        data: {
          tripId: trip.id,
          userId,
          budget: dto.budget,
          interests: {
            create: dto.interests.map((interest) => ({ interest })),
          },
        },
      });
      await this.access.resetPlan(tx, trip.id);
    });
    return this.detail(userId, trip.id);
  }

  async updateMe(
    userId: number,
    tripId: number,
    dto: UpdateMembershipDto,
  ): Promise<TripDetailResponse> {
    const { me } = await this.access.load(userId, tripId);
    await this.prisma.$transaction(async (tx) => {
      if (dto.budget !== undefined) {
        await tx.tripMember.update({
          where: { id: me.id },
          data: { budget: dto.budget },
        });
      }
      if (dto.interests !== undefined) {
        await tx.memberInterest.deleteMany({ where: { memberId: me.id } });
        await tx.memberInterest.createMany({
          data: dto.interests.map((interest) => ({
            memberId: me.id,
            interest,
          })),
        });
      }
      await this.access.resetPlan(tx, tripId);
    });
    return this.detail(userId, tripId);
  }

  async leave(userId: number, tripId: number): Promise<void> {
    const { me } = await this.access.load(userId, tripId);
    if (me.role === 'OWNER') {
      throw new AppError(
        HttpStatus.CONFLICT,
        ErrorCode.OwnerCannotLeave,
        'The owner cannot leave; delete the trip instead',
      );
    }
    await this.prisma.$transaction(async (tx) => {
      await tx.tripMember.delete({ where: { id: me.id } });
      await this.access.resetPlan(tx, tripId);
    });
  }

  /** Owner only: takes a member out of the group; the trip has to be planned again. */
  async removeMember(
    userId: number,
    tripId: number,
    memberId: number,
  ): Promise<void> {
    const { trip, me } = await this.access.loadAsOwner(userId, tripId);
    const member = memberOf(trip, memberId);
    if (member.id === me.id) {
      throw new AppError(
        HttpStatus.CONFLICT,
        ErrorCode.OwnerCannotLeave,
        'The owner cannot remove themselves; transfer ownership or delete the trip',
      );
    }
    await this.prisma.$transaction(async (tx) => {
      await tx.tripMember.delete({ where: { id: member.id } });
      await this.access.resetPlan(tx, tripId);
    });
  }

  /** Owner only: another member becomes the owner and you become a member. */
  async transferOwnership(
    userId: number,
    tripId: number,
    memberId: number,
  ): Promise<TripDetailResponse> {
    const { trip, me } = await this.access.loadAsOwner(userId, tripId);
    const heir = memberOf(trip, memberId);
    if (heir.id !== me.id) {
      await this.prisma.$transaction([
        this.prisma.tripMember.update({
          where: { id: me.id },
          data: { role: 'MEMBER' },
        }),
        this.prisma.tripMember.update({
          where: { id: heir.id },
          data: { role: 'OWNER' },
        }),
        this.prisma.trip.update({
          where: { id: tripId },
          data: { ownerId: heir.userId },
        }),
      ]);
    }
    return this.detail(userId, tripId);
  }

  /** Owner only: a new invite code, e.g. after the old one was shared too widely. */
  async renewInviteCode(
    userId: number,
    tripId: number,
  ): Promise<TripDetailResponse> {
    const { trip } = await this.access.loadAsOwner(userId, tripId);
    if (trip.type !== 'GROUP') {
      throw new AppError(
        HttpStatus.BAD_REQUEST,
        ErrorCode.NotAGroup,
        'Only group trips have an invite code',
      );
    }
    for (let attempt = 0; ; attempt++) {
      try {
        await this.prisma.trip.update({
          where: { id: tripId },
          data: { inviteCode: newInviteCode() },
        });
        return this.detail(userId, tripId);
      } catch (error) {
        const collision =
          error instanceof Prisma.PrismaClientKnownRequestError &&
          error.code === 'P2002';
        if (!collision || attempt >= 4) {
          throw error;
        }
      }
    }
  }

  async remove(userId: number, tripId: number): Promise<void> {
    await this.access.loadAsOwner(userId, tripId);
    await this.prisma.trip.delete({ where: { id: tripId } });
  }

  /** Lodging share plus places, per member; empty until the trip is planned. */
  private async spentByMember(trip: LoadedTrip): Promise<Map<number, number>> {
    const spent = new Map<number, number>();
    if (trip.status !== 'PLANNED') {
      return spent;
    }
    const sums = await this.prisma.stopParticipant.groupBy({
      by: ['memberId'],
      where: { stop: { tripId: trip.id } },
      _sum: { cost: true },
    });
    for (const member of trip.members) {
      const places = sums.find((s) => s.memberId === member.id)?._sum.cost ?? 0;
      spent.set(member.id, member.lodgingShare + places);
    }
    return spent;
  }
}
