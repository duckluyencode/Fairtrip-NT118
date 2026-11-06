import {
  Body,
  Controller,
  Delete,
  Get,
  HttpCode,
  HttpStatus,
  Param,
  ParseIntPipe,
  Patch,
  Post,
  Put,
} from '@nestjs/common';
import { ApiBearerAuth, ApiTags } from '@nestjs/swagger';
import { CurrentUser, type AuthUser } from '../auth/auth.decorators.js';
import {
  AddOptionDto,
  ChooseLodgingDto,
  CreateTripDto,
  JoinTripDto,
  UpdateItineraryDto,
  UpdateMembershipDto,
} from './dto/trip.dto.js';
import {
  ItineraryResponse,
  LodgingOptionResponse,
  TripDetailResponse,
  TripSummaryResponse,
  VoteOptionList,
} from './dto/trip.response.js';
import { ItineraryService } from './itinerary.service.js';
import { LodgingService } from './lodging.service.js';
import { PlanningService } from './planning.service.js';
import { TripsService } from './trips.service.js';
import { VotesService } from './votes.service.js';

@ApiTags('trips')
@ApiBearerAuth()
@Controller('trips')
export class TripsController {
  constructor(
    private readonly trips: TripsService,
    private readonly lodging: LodgingService,
    private readonly votes: VotesService,
    private readonly planning: PlanningService,
    private readonly itinerary: ItineraryService,
  ) {}

  /** Creates a solo trip or a group (with an invite code); you are its owner. */
  @Post()
  create(
    @CurrentUser() me: AuthUser,
    @Body() dto: CreateTripDto,
  ): Promise<TripDetailResponse> {
    return this.trips.create(me.id, dto);
  }

  /** Your trips, latest first. */
  @Get()
  list(@CurrentUser() me: AuthUser): Promise<TripSummaryResponse[]> {
    return this.trips.list(me.id);
  }

  /** Joins a group with its invite code and your own budget and interests. */
  @Post('join')
  join(
    @CurrentUser() me: AuthUser,
    @Body() dto: JoinTripDto,
  ): Promise<TripDetailResponse> {
    return this.trips.join(me.id, dto);
  }

  @Get(':id')
  detail(
    @CurrentUser() me: AuthUser,
    @Param('id', ParseIntPipe) id: number,
  ): Promise<TripDetailResponse> {
    return this.trips.detail(me.id, id);
  }

  /** Owner only. */
  @Delete(':id')
  @HttpCode(HttpStatus.NO_CONTENT)
  remove(
    @CurrentUser() me: AuthUser,
    @Param('id', ParseIntPipe) id: number,
  ): Promise<void> {
    return this.trips.remove(me.id, id);
  }

  /** Changes your budget or interests in this trip; it has to be planned again. */
  @Patch(':id/me')
  updateMe(
    @CurrentUser() me: AuthUser,
    @Param('id', ParseIntPipe) id: number,
    @Body() dto: UpdateMembershipDto,
  ): Promise<TripDetailResponse> {
    return this.trips.updateMe(me.id, id, dto);
  }

  /** Leaves a group (the owner deletes the trip instead). */
  @Delete(':id/me')
  @HttpCode(HttpStatus.NO_CONTENT)
  leave(
    @CurrentUser() me: AuthUser,
    @Param('id', ParseIntPipe) id: number,
  ): Promise<void> {
    return this.trips.leave(me.id, id);
  }

  /** Three hotels everyone can afford: CHEAPEST, BALANCED and NEAREST. */
  @Get(':id/lodging/options')
  lodgingOptions(
    @CurrentUser() me: AuthUser,
    @Param('id', ParseIntPipe) id: number,
  ): Promise<LodgingOptionResponse[]> {
    return this.lodging.options(me.id, id);
  }

  /** Owner only: a suggested hotel, or SELF ("Tự lo chỗ ở"). */
  @Put(':id/lodging')
  async chooseLodging(
    @CurrentUser() me: AuthUser,
    @Param('id', ParseIntPipe) id: number,
    @Body() dto: ChooseLodgingDto,
  ): Promise<TripDetailResponse> {
    await this.lodging.choose(me.id, id, dto);
    return this.trips.detail(me.id, id);
  }

  /** Places proposed for the trip, with prices and votes. */
  @Get(':id/options')
  options(
    @CurrentUser() me: AuthUser,
    @Param('id', ParseIntPipe) id: number,
  ): Promise<VoteOptionList> {
    return this.votes.list(me.id, id);
  }

  @Post(':id/options')
  addOption(
    @CurrentUser() me: AuthUser,
    @Param('id', ParseIntPipe) id: number,
    @Body() dto: AddOptionDto,
  ): Promise<VoteOptionList> {
    return this.votes.add(me.id, id, dto.placeId);
  }

  @Delete(':id/options/:optionId')
  @HttpCode(HttpStatus.NO_CONTENT)
  removeOption(
    @CurrentUser() me: AuthUser,
    @Param('id', ParseIntPipe) id: number,
    @Param('optionId', ParseIntPipe) optionId: number,
  ): Promise<void> {
    return this.votes.remove(me.id, id, optionId);
  }

  /** "Đồng ý" with an option. */
  @Put(':id/options/:optionId/vote')
  vote(
    @CurrentUser() me: AuthUser,
    @Param('id', ParseIntPipe) id: number,
    @Param('optionId', ParseIntPipe) optionId: number,
  ): Promise<VoteOptionList> {
    return this.votes.setVote(me.id, id, optionId, true);
  }

  @Delete(':id/options/:optionId/vote')
  unvote(
    @CurrentUser() me: AuthUser,
    @Param('id', ParseIntPipe) id: number,
    @Param('optionId', ParseIntPipe) optionId: number,
  ): Promise<VoteOptionList> {
    return this.votes.setVote(me.id, id, optionId, false);
  }

  /** "Gửi phiếu": your votes become final. */
  @Post(':id/ballot')
  @HttpCode(HttpStatus.OK)
  sendBallot(
    @CurrentUser() me: AuthUser,
    @Param('id', ParseIntPipe) id: number,
  ): Promise<VoteOptionList> {
    return this.votes.send(me.id, id);
  }

  /**
   * Owner only: plans the trip (Greedy, Local search, Fairness check) from the proposed
   * places, or from popular places of the city when there are none.
   */
  @Post(':id/plan')
  @HttpCode(HttpStatus.OK)
  async plan(
    @CurrentUser() me: AuthUser,
    @Param('id', ParseIntPipe) id: number,
  ): Promise<ItineraryResponse> {
    await this.planning.plan(me.id, id);
    return this.itinerary.view(me.id, id);
  }

  @Get(':id/itinerary')
  viewItinerary(
    @CurrentUser() me: AuthUser,
    @Param('id', ParseIntPipe) id: number,
  ): Promise<ItineraryResponse> {
    return this.itinerary.view(me.id, id);
  }

  /** Owner only: "Tự thiết kế lộ trình", the places of each day in order. */
  @Put(':id/itinerary')
  async designItinerary(
    @CurrentUser() me: AuthUser,
    @Param('id', ParseIntPipe) id: number,
    @Body() dto: UpdateItineraryDto,
  ): Promise<ItineraryResponse> {
    await this.planning.design(me.id, id, dto);
    return this.itinerary.view(me.id, id);
  }
}
