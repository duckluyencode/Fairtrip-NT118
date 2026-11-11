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
} from '@nestjs/common';
import { ApiBearerAuth, ApiTags } from '@nestjs/swagger';
import { CurrentUser, type AuthUser } from '../auth/auth.decorators.js';
import {
  CreateEmergencyContactDto,
  UpdateEmergencyContactDto,
} from './emergency-contact.dto.js';
import {
  EmergencyContactList,
  EmergencyContactResponse,
} from './emergency-contact.response.js';
import { EmergencyContactsService } from './emergency-contacts.service.js';

@ApiTags('me')
@ApiBearerAuth()
@Controller('me/emergency-contacts')
export class EmergencyContactsController {
  constructor(private readonly contacts: EmergencyContactsService) {}

  /** Oldest first; the app keeps a copy so SOS works offline. */
  @Get()
  list(@CurrentUser() me: AuthUser): Promise<EmergencyContactList> {
    return this.contacts.list(me.id);
  }

  @Post()
  create(
    @CurrentUser() me: AuthUser,
    @Body() dto: CreateEmergencyContactDto,
  ): Promise<EmergencyContactResponse> {
    return this.contacts.create(me.id, dto);
  }

  @Patch(':id')
  update(
    @CurrentUser() me: AuthUser,
    @Param('id', ParseIntPipe) id: number,
    @Body() dto: UpdateEmergencyContactDto,
  ): Promise<EmergencyContactResponse> {
    return this.contacts.update(me.id, id, dto);
  }

  @Delete(':id')
  @HttpCode(HttpStatus.NO_CONTENT)
  remove(
    @CurrentUser() me: AuthUser,
    @Param('id', ParseIntPipe) id: number,
  ): Promise<void> {
    return this.contacts.remove(me.id, id);
  }
}
