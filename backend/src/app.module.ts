import { Module } from '@nestjs/common';
import { ConfigModule, ConfigService } from '@nestjs/config';
import { APP_GUARD } from '@nestjs/core';
import { ThrottlerGuard, ThrottlerModule } from '@nestjs/throttler';
import { AuthGuard } from './auth/auth.guard.js';
import { AuthModule } from './auth/auth.module.js';
import { validateEnv } from './config/env.validation.js';
import { FavoritesModule } from './favorites/favorites.module.js';
import { HealthModule } from './health/health.module.js';
import { PlacesModule } from './places/places.module.js';
import { PrismaModule } from './prisma/prisma.module.js';
import { SearchHistoryModule } from './search-history/search-history.module.js';
import { UsersModule } from './users/users.module.js';

@Module({
  imports: [
    ConfigModule.forRoot({ isGlobal: true, validate: validateEnv }),
    ThrottlerModule.forRootAsync({
      inject: [ConfigService],
      useFactory: (config: ConfigService) => ({
        // Per client IP; routes that send codes set stricter limits.
        throttlers: [{ ttl: 60_000, limit: 120 }],
        // e2e tests send many requests from one address.
        skipIf: () => config.get<string>('NODE_ENV') === 'test',
      }),
    }),
    PrismaModule,
    HealthModule,
    AuthModule,
    UsersModule,
    PlacesModule,
    SearchHistoryModule,
    FavoritesModule,
  ],
  // Guards run in this order: rate limit first, then the access token check.
  providers: [
    { provide: APP_GUARD, useClass: ThrottlerGuard },
    { provide: APP_GUARD, useExisting: AuthGuard },
  ],
})
export class AppModule {}
