import request from 'supertest';
import { createTestApp, TestApp } from './utils/test-app.js';

describe('App (e2e)', () => {
  let t: TestApp;

  beforeAll(async () => {
    t = await createTestApp();
  });

  afterAll(async () => {
    await t.app.close();
  });

  it('GET /health reports the database is up', () => {
    return request(t.app.getHttpServer())
      .get('/health')
      .expect(200)
      .expect({ status: 'ok', database: 'up' });
  });

  it('unknown routes return the standard error body', () => {
    return request(t.app.getHttpServer())
      .get('/api/v1/does-not-exist')
      .expect(404)
      .expect((res) => {
        expect(res.body).toMatchObject({ statusCode: 404, code: 'NOT_FOUND' });
      });
  });
});
