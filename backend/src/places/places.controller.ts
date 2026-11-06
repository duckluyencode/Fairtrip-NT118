import { Controller, Get, Param, ParseIntPipe, Query } from '@nestjs/common';
import { ApiBearerAuth, ApiTags } from '@nestjs/swagger';
import { Throttle } from '@nestjs/throttler';
import { CurrentUser, type AuthUser } from '../auth/auth.decorators.js';
import {
  PlaceDetailQuery,
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
  // Each page is a billed Google Text Search.
  @Throttle({ default: { limit: 60, ttl: 60_000 } })
  search(
    @CurrentUser() me: AuthUser,
    @Query() query: SearchPlacesQuery,
  ): Promise<PlacePage> {
    return this.places.search(me.id, query);
  }

  /** Names while the user is typing (Google Autocomplete, or the sample catalog). */
  @Get('suggestions')
  suggestions(@Query() query: SuggestionsQuery): Promise<PlaceSuggestion[]> {
    return this.places.suggestions(query);
  }

  /** Details with price, opening hours and visit time. */
  @Get(':id')
  detail(
    @CurrentUser() me: AuthUser,
    @Param('id', ParseIntPipe) id: number,
    @Query() query: PlaceDetailQuery,
  ): Promise<PlaceDetail> {
    return this.places.detail(me.id, id, query, query.sessionToken);
  }
}
