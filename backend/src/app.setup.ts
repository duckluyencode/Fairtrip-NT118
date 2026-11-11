import { INestApplication, RequestMethod } from '@nestjs/common';
import type { Express } from 'express';
import { HttpExceptionFilter } from './common/errors/http-exception.filter.js';
import { createValidationPipe } from './common/validation/validation-pipe.js';

export const API_PREFIX = 'api/v1';

/** App-wide HTTP setup, shared by main.ts and the e2e tests so both run the same way. */
export function configureApp(app: INestApplication): void {
  // Behind Caddy every request comes from the proxy; trusting its X-Forwarded-For gives
  // the real client IP, which the rate limit is keyed on.
  const proxies = Number(process.env.TRUST_PROXY ?? 0);
  if (proxies > 0) {
    const express = app.getHttpAdapter().getInstance() as Express;
    express.set('trust proxy', proxies);
  }
  // /health stays outside the versioned prefix for load balancers and uptime checks.
  app.setGlobalPrefix(API_PREFIX, {
    exclude: [{ path: 'health', method: RequestMethod.GET }],
  });
  app.useGlobalPipes(createValidationPipe());
  app.useGlobalFilters(new HttpExceptionFilter());
  app.enableShutdownHooks();
}
