import { Module } from '@nestjs/common';
import { PlacesModule } from '../places/places.module.js';
import { FavoritesController } from './favorites.controller.js';
import { FavoritesService } from './favorites.service.js';

@Module({
  imports: [PlacesModule],
  controllers: [FavoritesController],
  providers: [FavoritesService],
})
export class FavoritesModule {}
