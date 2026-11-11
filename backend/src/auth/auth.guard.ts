import {
  HttpStatus,
  Injectable,
  type CanActivate,
  type ExecutionContext,
} from '@nestjs/common';
import { Reflector } from '@nestjs/core';
import type { Request } from 'express';
import { AppError } from '../common/errors/app-error.js';
import { ErrorCode } from '../common/errors/error-code.js';
import { PrismaService } from '../prisma/prisma.service.js';
import { IS_PUBLIC_KEY, type AuthenticatedRequest } from './auth.decorators.js';
import { TokenService } from './token.service.js';

/**
 * Global guard: routes need `Authorization: Bearer <accessToken>` unless marked @Public().
 * The account must still exist, so a deleted account loses access at once instead of
 * when its access token expires.
 */
@Injectable()
export class AuthGuard implements CanActivate {
  constructor(
    private readonly reflector: Reflector,
    private readonly tokens: TokenService,
    private readonly prisma: PrismaService,
  ) {}

  async canActivate(context: ExecutionContext): Promise<boolean> {
    const isPublic = this.reflector.getAllAndOverride<boolean>(IS_PUBLIC_KEY, [
      context.getHandler(),
      context.getClass(),
    ]);
    if (isPublic) {
      return true;
    }
    const request = context
      .switchToHttp()
      .getRequest<Request & AuthenticatedRequest>();
    const [scheme, token] = request.headers.authorization?.split(' ') ?? [];
    const userId =
      scheme === 'Bearer' && token
        ? await this.tokens.verifyAccessToken(token)
        : null;
    const exists =
      userId !== null &&
      (await this.prisma.user.count({ where: { id: userId } })) > 0;
    if (userId === null || !exists) {
      throw new AppError(
        HttpStatus.UNAUTHORIZED,
        ErrorCode.Unauthorized,
        'A valid access token is required',
      );
    }
    request.user = { id: userId };
    return true;
  }
}
