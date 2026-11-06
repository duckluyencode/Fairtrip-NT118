import { Module } from '@nestjs/common';
import { PlacesModule } from '../places/places.module.js';
import { ItineraryService } from './itinerary.service.js';
import { LodgingService } from './lodging.service.js';
import { PlanningService } from './planning.service.js';
import { TripAccess } from './trip-access.js';
import { TripPlacesService } from './trip-places.js';
import { TripsController } from './trips.controller.js';
import { TripsService } from './trips.service.js';
import { VotesService } from './votes.service.js';

@Module({
  imports: [PlacesModule],
  controllers: [TripsController],
  providers: [
    TripAccess,
    TripPlacesService,
    TripsService,
    LodgingService,
    VotesService,
    PlanningService,
    ItineraryService,
  ],
})
export class TripsModule {}
