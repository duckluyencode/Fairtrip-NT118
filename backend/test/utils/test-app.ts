import { INestApplication } from '@nestjs/common';
import { Test } from '@nestjs/testing';
import { App } from 'supertest/types.js';
import { AppModule } from '../../src/app.module.js';
import { configureApp } from '../../src/app.setup.js';
import {
  OtpDelivery,
  type OtpMessage,
} from '../../src/auth/otp/otp-delivery.js';
import { PrismaService } from '../../src/prisma/prisma.service.js';

/** Keeps sent codes in memory so tests can read them instead of an inbox. */
export class FakeOtpDelivery extends OtpDelivery {
  readonly sent: OtpMessage[] = [];

  send(message: OtpMessage): Promise<void> {
    this.sent.push(message);
    return Promise.resolve();
  }

  /** The latest code sent to a normalized contact. */
  lastCode(contact: string): string {
    const message = this.sent.findLast((m) => m.contact.value === contact);
    if (!message) {
      throw new Error(`No code was sent to ${contact}`);
    }
    return message.code;
  }
}

export interface TestApp {
  app: INestApplication<App>;
  prisma: PrismaService;
  otp: FakeOtpDelivery;
}

/** Starts the whole app with the same HTTP setup as main.ts and a fake code sender. */
export async function createTestApp(): Promise<TestApp> {
  const otp = new FakeOtpDelivery();
  const moduleRef = await Test.createTestingModule({ imports: [AppModule] })
    .overrideProvider(OtpDelivery)
    .useValue(otp)
    .compile();
  const app = moduleRef.createNestApplication<INestApplication<App>>();
  configureApp(app);
  await app.init();
  return { app, prisma: app.get(PrismaService), otp };
}

/** Empties every table except Prisma's migration history. */
export async function resetDatabase(prisma: PrismaService): Promise<void> {
  const tables = await prisma.$queryRaw<{ name: string }[]>`
    SELECT TABLE_NAME AS name FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME <> '_prisma_migrations'`;
  // TRUNCATE needs foreign key checks off; one connection keeps the setting for all statements.
  await prisma.$transaction([
    prisma.$executeRawUnsafe('SET FOREIGN_KEY_CHECKS = 0'),
    ...tables.map(({ name }) =>
      prisma.$executeRawUnsafe(`TRUNCATE TABLE \`${name}\``),
    ),
    prisma.$executeRawUnsafe('SET FOREIGN_KEY_CHECKS = 1'),
  ]);
}
