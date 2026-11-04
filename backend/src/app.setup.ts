import { INestApplication, RequestMethod } from '@nestjs/common';
import { HttpExceptionFilter } from './common/errors/http-exception.filter.js';
import { createValidationPipe } from './common/validation/validation-pipe.js';

export const API_PREFIX = 'api/v1';

/** App-wide HTTP setup, shared by main.ts and the e2e tests so both run the same way. */
export function configureApp(app: INestApplication): void {
  // /health stays outside the versioned prefix for load balancers and uptime checks.
  app.setGlobalPrefix(API_PREFIX, {
    exclude: [{ path: 'health', method: RequestMethod.GET }],
  });
  app.useGlobalPipes(createValidationPipe());
  app.useGlobalFilters(new HttpExceptionFilter());
  app.enableShutdownHooks();
}
