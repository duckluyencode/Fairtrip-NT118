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
import type { LatLng } from '../../src/places/geo.js';
import {
  GooglePlacesClient,
  type AutocompleteSuggestion,
  type GooglePlace,
  type TextSearchRequest,
  type TextSearchResult,
} from '../../src/places/google/google-places.client.js';
import { RoutesClient } from '../../src/places/google/routes.client.js';
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

/**
 * Google Places stand-in; disabled until a test turns it on and adds places. Text Search
 * returns every place (of the included type) in the order given, so tests decide what
 * Google "found".
 */
export class FakeGooglePlacesClient extends GooglePlacesClient {
  enabled = false;
  places: GooglePlace[] = [];
  /** Every Text Search request, to check what was asked. */
  readonly searches: TextSearchRequest[] = [];

  search(request: TextSearchRequest): Promise<TextSearchResult> {
    if (!this.enabled) {
      return Promise.reject(new Error('Google is disabled in this test'));
    }
    this.searches.push(request);
    const found = this.places.filter(
      (p) => !request.includedType || p.types.includes(request.includedType),
    );
    return Promise.resolve({
      places: found.slice(0, request.pageSize),
      nextPageToken: found.length > request.pageSize ? 'next-page' : null,
    });
  }

  autocomplete(input: string): Promise<AutocompleteSuggestion[]> {
    const text = input.toLowerCase();
    return Promise.resolve(
      this.places
        .filter((p) => p.name.toLowerCase().includes(text))
        .map((p) => ({
          placeId: p.id,
          mainText: p.name,
          secondaryText: p.address,
        })),
    );
  }

  getPlace(placeId: string): Promise<GooglePlace | null> {
    if (!this.enabled) {
      return Promise.reject(new Error('Google is disabled in this test'));
    }
    return Promise.resolve(this.places.find((p) => p.id === placeId) ?? null);
  }

  reset(): void {
    this.enabled = false;
    this.places = [];
    this.searches.length = 0;
  }
}

/** Google Routes stand-in: off by default, or every leg takes `minutesPerLeg`. */
export class FakeRoutesClient extends RoutesClient {
  enabled = false;
  minutesPerLeg = 20;
  /** The points of every request. */
  readonly requests: LatLng[][] = [];

  legMinutes(points: LatLng[]): Promise<number[] | null> {
    if (!this.enabled) {
      return Promise.resolve(null);
    }
    this.requests.push(points);
    return Promise.resolve(points.slice(1).map(() => this.minutesPerLeg));
  }

  reset(): void {
    this.enabled = false;
    this.minutesPerLeg = 20;
    this.requests.length = 0;
  }
}

export interface TestApp {
  app: INestApplication<App>;
  prisma: PrismaService;
  otp: FakeOtpDelivery;
  google: FakeGooglePlacesClient;
  routes: FakeRoutesClient;
}

/**
 * Starts the whole app with the same HTTP setup as main.ts, with fakes for sending
 * codes, Google Places and Google Routes so tests never reach the network.
 */
export async function createTestApp(): Promise<TestApp> {
  const otp = new FakeOtpDelivery();
  const google = new FakeGooglePlacesClient();
  const routes = new FakeRoutesClient();
  const moduleRef = await Test.createTestingModule({ imports: [AppModule] })
    .overrideProvider(OtpDelivery)
    .useValue(otp)
    .overrideProvider(GooglePlacesClient)
    .useValue(google)
    .overrideProvider(RoutesClient)
    .useValue(routes)
    .compile();
  const app = moduleRef.createNestApplication<INestApplication<App>>();
  configureApp(app);
  await app.init();
  return { app, prisma: app.get(PrismaService), otp, google, routes };
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
