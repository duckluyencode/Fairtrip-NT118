import { Injectable, OnModuleDestroy } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { PrismaClient } from '../generated/prisma/client.js';
import { createMariaDbAdapter } from './adapter.js';

// One PrismaClient (and one connection pool) shared by the whole app.
@Injectable()
export class PrismaService extends PrismaClient implements OnModuleDestroy {
  constructor(config: ConfigService) {
    super({ adapter: createMariaDbAdapter((key) => config.get<string>(key)) });
  }

  async onModuleDestroy(): Promise<void> {
    await this.$disconnect();
  }
}
