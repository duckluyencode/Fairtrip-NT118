import {
  Controller,
  Delete,
  Get,
  HttpCode,
  HttpStatus,
  Param,
  ParseIntPipe,
  Query,
} from '@nestjs/common';
import { ApiBearerAuth, ApiTags } from '@nestjs/swagger';
import { CurrentUser, type AuthUser } from '../auth/auth.decorators.js';
import { HistoryList, HistoryQuery } from './search-history.dto.js';
import { SearchHistoryService } from './search-history.service.js';

/** Searches are saved by GET /places?q=…; this lists and deletes them. */
@ApiTags('me')
@ApiBearerAuth()
@Controller('me/search-history')
export class SearchHistoryController {
  constructor(private readonly history: SearchHistoryService) {}

  /** Newest first. */
  @Get()
  async list(
    @CurrentUser() me: AuthUser,
    @Query() query: HistoryQuery,
  ): Promise<HistoryList> {
    return { items: await this.history.list(me.id, query.limit) };
  }

  @Delete(':id')
  @HttpCode(HttpStatus.NO_CONTENT)
  remove(
    @CurrentUser() me: AuthUser,
    @Param('id', ParseIntPipe) id: number,
  ): Promise<void> {
    return this.history.remove(me.id, id);
  }

  /** Clears the whole history ("Xóa lịch sử"). */
  @Delete()
  @HttpCode(HttpStatus.NO_CONTENT)
  clear(@CurrentUser() me: AuthUser): Promise<void> {
    return this.history.clear(me.id);
  }
}
