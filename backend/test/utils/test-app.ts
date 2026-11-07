import { INestApplication } from '@nestjs/common';
import { Test } from '@nestjs/testing';
import request from 'supertest';
import { App } from 'supertest/types.js';
import { AppModule } from '../../src/app.module.js';
import { configureApp } from '../../src/app.setup.js';
import {
  OtpDelivery,
  type OtpMessage,
} from '../../src/auth/otp/otp-delivery.js';
import {
  GooglePlacesClient,
  type GooglePlace,
} from '../../src/places/google/google-places.client.js';
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

/** Google Places stand-in; disabled until a test turns it on and adds places. */
export class FakeGooglePlacesClient extends GooglePlacesClient {
  enabled = false;
  places: GooglePlace[] = [];

  searchText(): Promise<GooglePlace[]> {
    return Promise.resolve(this.enabled ? this.places : []);
  }

  getPlace(placeId: string): Promise<GooglePlace | null> {
    if (!this.enabled) {
      return Promise.reject(new Error('Google is disabled in this test'));
    }
    return Promise.resolve(this.places.find((p) => p.id === placeId) ?? null);
  }
}

export interface TestApp {
  app: INestApplication<App>;
  prisma: PrismaService;
  otp: FakeOtpDelivery;
  google: FakeGooglePlacesClient;
}

/**
 * Starts the whole app with the same HTTP setup as main.ts, with fakes for sending
 * codes and for Google Places so tests never reach the network.
 */
export async function createTestApp(): Promise<TestApp> {
  const otp = new FakeOtpDelivery();
  const google = new FakeGooglePlacesClient();
  const moduleRef = await Test.createTestingModule({ imports: [AppModule] })
    .overrideProvider(OtpDelivery)
    .useValue(otp)
    .overrideProvider(GooglePlacesClient)
    .useValue(google)
    .compile();
  const app = moduleRef.createNestApplication<INestApplication<App>>();
  configureApp(app);
  await app.init();
  return { app, prisma: app.get(PrismaService), otp, google };
}

/** Signs up and verifies an account; returns its access token. */
export async function signUp(
  t: TestApp,
  contact = 'duc@example.com',
): Promise<string> {
  await request(t.app.getHttpServer())
    .post('/api/v1/auth/register')
    .send({ fullName: 'Duy Đức', contact, password: 'matkhau123' })
    .expect(202);
  const res = await request(t.app.getHttpServer())
    .post('/api/v1/auth/register/verify')
    .send({ contact, code: t.otp.lastCode(contact) })
    .expect(200);
  return (res.body as { accessToken: string }).accessToken;
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
