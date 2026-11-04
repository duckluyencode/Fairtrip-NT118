import { Controller, Get, Param, ParseIntPipe, Query } from '@nestjs/common';
import { ApiBearerAuth, ApiTags } from '@nestjs/swagger';
import { CurrentUser, type AuthUser } from '../auth/auth.decorators.js';
import {
  OriginQuery,
  SearchPlacesQuery,
  SuggestionsQuery,
} from './dto/place.query.js';
import {
  PlaceDetail,
  PlacePage,
  PlaceSuggestion,
} from './dto/place.response.js';
import { PlacesService } from './places.service.js';

@ApiTags('places')
@ApiBearerAuth()
@Controller('places')
export class PlacesController {
  constructor(private readonly places: PlacesService) {}

  /** Search or browse places; a search with text is added to the search history. */
  @Get()
  search(
    @CurrentUser() me: AuthUser,
    @Query() query: SearchPlacesQuery,
  ): Promise<PlacePage> {
    return this.places.search(me.id, query);
  }

  /** Up to 5 names while the user is typing. */
  @Get('suggestions')
  suggestions(@Query() query: SuggestionsQuery): Promise<PlaceSuggestion[]> {
    return this.places.suggestions(query.q);
  }

  /** Details with price, opening hours and visit time. */
  @Get(':id')
  detail(
    @CurrentUser() me: AuthUser,
    @Param('id', ParseIntPipe) id: number,
    @Query() origin: OriginQuery,
  ): Promise<PlaceDetail> {
    return this.places.detail(me.id, id, origin);
  }
}
