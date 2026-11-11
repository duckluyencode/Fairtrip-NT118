import {
  Body,
  Controller,
  Delete,
  Get,
  HttpCode,
  HttpStatus,
  Patch,
} from '@nestjs/common';
import { ApiBearerAuth, ApiTags } from '@nestjs/swagger';
import { CurrentUser, type AuthUser } from '../auth/auth.decorators.js';
import { PrismaService } from '../prisma/prisma.service.js';
import { AccountDeletionService } from './account-deletion.service.js';
import { DeleteAccountDto, UpdateProfileDto } from './user.dto.js';
import { UserResponse } from './user.response.js';

/** The signed-in user's own profile. Changing email or phone needs a code and comes later. */
@ApiTags('me')
@ApiBearerAuth()
@Controller('me')
export class UsersController {
  constructor(
    private readonly prisma: PrismaService,
    private readonly deletion: AccountDeletionService,
  ) {}

  @Get()
  async profile(@CurrentUser() me: AuthUser): Promise<UserResponse> {
    const user = await this.prisma.user.findUniqueOrThrow({
      where: { id: me.id },
    });
    return UserResponse.from(user);
  }

  @Patch()
  async updateProfile(
    @CurrentUser() me: AuthUser,
    @Body() dto: UpdateProfileDto,
  ): Promise<UserResponse> {
    const user = await this.prisma.user.update({
      where: { id: me.id },
      data: { fullName: dto.fullName },
    });
    return UserResponse.from(user);
  }

  /**
   * Deletes the account after checking the password (or a Google sign-in) again. Groups others are still in
   * pass to the member who joined first; everything else of the account is deleted.
   */
  @Delete()
  @HttpCode(HttpStatus.NO_CONTENT)
  deleteAccount(
    @CurrentUser() me: AuthUser,
    @Body() dto: DeleteAccountDto,
  ): Promise<void> {
    return this.deletion.delete(me.id, dto);
  }
}
