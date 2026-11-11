import { Module } from '@nestjs/common';
import { SearchHistoryModule } from '../search-history/search-history.module.js';
import { GooglePlacesClient } from './google/google-places.client.js';
import { HttpGooglePlacesClient } from './google/http-google-places.client.js';
import { HttpRoutesClient, RoutesClient } from './google/routes.client.js';
import { PlacesController } from './places.controller.js';
import { PlacesService } from './places.service.js';

@Module({
  imports: [SearchHistoryModule],
  controllers: [PlacesController],
  providers: [
    PlacesService,
    { provide: GooglePlacesClient, useClass: HttpGooglePlacesClient },
    { provide: RoutesClient, useClass: HttpRoutesClient },
  ],
  exports: [PlacesService, GooglePlacesClient, RoutesClient],
})
export class PlacesModule {}
