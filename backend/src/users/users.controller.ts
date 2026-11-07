import { Body, Controller, Get, Patch } from '@nestjs/common';
import { ApiBearerAuth, ApiTags } from '@nestjs/swagger';
import { CurrentUser, type AuthUser } from '../auth/auth.decorators.js';
import { PrismaService } from '../prisma/prisma.service.js';
import { UpdateProfileDto } from './user.dto.js';
import { UserResponse } from './user.response.js';

/** The signed-in user's own profile. Changing email or phone needs a code and comes later. */
@ApiTags('me')
@ApiBearerAuth()
@Controller('me')
export class UsersController {
  constructor(private readonly prisma: PrismaService) {}

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
}
