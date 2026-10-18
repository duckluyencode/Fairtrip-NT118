import { Injectable, OnModuleDestroy } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { PrismaMariaDb } from '@prisma/adapter-mariadb';
import { PrismaClient } from '../generated/prisma/client.js';

// One PrismaClient (and one connection pool) shared by the whole app.
@Injectable()
export class PrismaService extends PrismaClient implements OnModuleDestroy {
  constructor(config: ConfigService) {
    super({
      adapter: new PrismaMariaDb({
        host: config.getOrThrow<string>('MYSQL_HOST'),
        port: Number(config.getOrThrow<string>('MYSQL_PORT')),
        user: config.getOrThrow<string>('MYSQL_USER'),
        password: config.getOrThrow<string>('MYSQL_PASSWORD'),
        database: config.getOrThrow<string>('MYSQL_DATABASE'),
        connectionLimit: 5,
        // Fail fast (so /health returns 503) instead of the 10 s default wait.
        acquireTimeout: 5000,
        // MySQL 8.4 uses caching_sha2_password: without TLS, the first login after
        // a server restart needs the server's RSA public key. Enable only for the
        // local Docker database; RDS connections use TLS instead.
        allowPublicKeyRetrieval:
          config.get<string>('MYSQL_ALLOW_PUBLIC_KEY_RETRIEVAL') === 'true',
      }),
    });
  }

  async onModuleDestroy(): Promise<void> {
    await this.$disconnect();
  }
}
