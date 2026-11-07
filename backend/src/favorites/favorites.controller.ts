import {
  Controller,
  Delete,
  Get,
  HttpCode,
  HttpStatus,
  Param,
  ParseIntPipe,
  Put,
  Query,
} from '@nestjs/common';
import { ApiBearerAuth, ApiTags } from '@nestjs/swagger';
import { CurrentUser, type AuthUser } from '../auth/auth.decorators.js';
import { CategoryQuery } from '../places/dto/place.query.js';
import { PlaceList } from '../places/dto/place.response.js';
import { FavoritesService } from './favorites.service.js';

@ApiTags('me')
@ApiBearerAuth()
@Controller('me/favorites')
export class FavoritesController {
  constructor(private readonly favorites: FavoritesService) {}

  @Get()
  async list(
    @CurrentUser() me: AuthUser,
    @Query() query: CategoryQuery,
  ): Promise<PlaceList> {
    return { items: await this.favorites.list(me.id, query.category) };
  }

  /** Saves a place; saving it again changes nothing. */
  @Put(':placeId')
  @HttpCode(HttpStatus.NO_CONTENT)
  add(
    @CurrentUser() me: AuthUser,
    @Param('placeId', ParseIntPipe) placeId: number,
  ): Promise<void> {
    return this.favorites.add(me.id, placeId);
  }

  /** Unsaves a place; unsaving one that is not saved changes nothing. */
  @Delete(':placeId')
  @HttpCode(HttpStatus.NO_CONTENT)
  remove(
    @CurrentUser() me: AuthUser,
    @Param('placeId', ParseIntPipe) placeId: number,
  ): Promise<void> {
    return this.favorites.remove(me.id, placeId);
  }
}
