import {
  createParamDecorator,
  SetMetadata,
  type ExecutionContext,
} from '@nestjs/common';

export const IS_PUBLIC_KEY = 'isPublic';

/** Lets a route through without an access token; every other route requires one. */
export const Public = () => SetMetadata(IS_PUBLIC_KEY, true);

export interface AuthUser {
  id: number;
}

export interface AuthenticatedRequest {
  user?: AuthUser;
}

/** The signed-in user, set by AuthGuard. */
export const CurrentUser = createParamDecorator(
  (_data: unknown, context: ExecutionContext): AuthUser => {
    const request = context.switchToHttp().getRequest<AuthenticatedRequest>();
    if (!request.user) {
      throw new Error('CurrentUser used on a public route');
    }
    return request.user;
  },
);
