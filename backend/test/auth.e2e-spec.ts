import request from 'supertest';
import {
  createTestApp,
  resetDatabase,
  type TestApp,
} from './utils/test-app.js';

const EMAIL = 'duc@example.com';
const PASSWORD = 'matkhau123';

describe('Auth (e2e)', () => {
  let t: TestApp;
  const api = () => request(t.app.getHttpServer());

  /** Signs up and verifies an email account; returns the session body. */
  async function signUp(contact = EMAIL, password = PASSWORD) {
    await api()
      .post('/api/v1/auth/register')
      .send({ fullName: 'Duy Đức', contact, password })
      .expect(202);
    const res = await api()
      .post('/api/v1/auth/register/verify')
      .send({ contact, code: t.otp.lastCode(contact) })
      .expect(200);
    return res.body as { accessToken: string; refreshToken: string };
  }

  function wrongCode(contact: string): string {
    return t.otp.lastCode(contact) === '000000' ? '111111' : '000000';
  }

  beforeAll(async () => {
    t = await createTestApp();
  });

  beforeEach(async () => {
    await resetDatabase(t.prisma);
    t.otp.sent.length = 0;
  });

  afterAll(async () => {
    await t.app.close();
  });

  describe('sign-up', () => {
    it('sends a code, then verifies it and signs in', async () => {
      await api()
        .post('/api/v1/auth/register')
        .send({
          fullName: '  Duy   Đức ',
          contact: 'Duc@Example.com',
          password: PASSWORD,
        })
        .expect(202)
        .expect({
          channel: 'EMAIL',
          sentTo: EMAIL,
          expiresInSeconds: 900,
          resendInSeconds: 60,
        });

      // Not verified yet, so the account cannot sign in.
      await api()
        .post('/api/v1/auth/login')
        .send({ contact: EMAIL, password: PASSWORD })
        .expect(401)
        .expect((res) => expect(res.body.code).toBe('WRONG_CREDENTIALS'));

      const verified = await api()
        .post('/api/v1/auth/register/verify')
        .send({ contact: EMAIL, code: t.otp.lastCode(EMAIL) })
        .expect(200);
      expect(verified.body).toMatchObject({
        expiresIn: 900,
        user: { fullName: 'Duy Đức', email: EMAIL, phone: null },
      });

      await api()
        .get('/api/v1/me')
        .auth(verified.body.accessToken, { type: 'bearer' })
        .expect(200)
        .expect((res) => expect(res.body.email).toBe(EMAIL));

      await api()
        .post('/api/v1/auth/login')
        .send({ contact: ' DUC@example.com', password: PASSWORD })
        .expect(200);

      await api()
        .post('/api/v1/auth/register')
        .send({ fullName: 'Someone', contact: EMAIL, password: 'khac12345' })
        .expect(409)
        .expect((res) => expect(res.body.code).toBe('ACCOUNT_EXISTS'));
    });

    it('matches phone numbers written in any format', async () => {
      await api()
        .post('/api/v1/auth/register')
        .send({
          fullName: 'Duy Đức',
          contact: '090 512 8846',
          password: PASSWORD,
        })
        .expect(202)
        .expect((res) =>
          expect(res.body).toMatchObject({
            channel: 'SMS',
            sentTo: '+84905128846',
          }),
        );
      await api()
        .post('/api/v1/auth/register/verify')
        .send({
          contact: '+84905128846',
          code: t.otp.lastCode('+84905128846'),
        })
        .expect(200);

      await api()
        .post('/api/v1/auth/login')
        .send({ contact: '0905128846', password: PASSWORD })
        .expect(200);
    });

    it('lists every invalid field', async () => {
      const res = await api()
        .post('/api/v1/auth/register')
        .send({ fullName: '   ', contact: 'abc', password: 'short' })
        .expect(400);

      expect(res.body.code).toBe('VALIDATION_FAILED');
      expect(res.body.details.map((d: { field: string }) => d.field)).toEqual([
        'fullName',
        'contact',
        'password',
      ]);
    });
  });

  describe('codes', () => {
    it('locks a code after 5 wrong guesses and allows a resend after 60 s', async () => {
      await api()
        .post('/api/v1/auth/register')
        .send({ fullName: 'Duy Đức', contact: EMAIL, password: PASSWORD })
        .expect(202);
      const wrong = wrongCode(EMAIL);

      await api()
        .post('/api/v1/auth/register/verify')
        .send({ contact: EMAIL, code: wrong })
        .expect(400)
        .expect((res) =>
          expect(res.body).toMatchObject({
            code: 'WRONG_CODE',
            details: { attemptsLeft: 4 },
          }),
        );

      await api()
        .post('/api/v1/auth/code/resend')
        .send({ contact: EMAIL, purpose: 'REGISTER' })
        .expect(429)
        .expect((res) => {
          expect(res.body.code).toBe('RESEND_TOO_SOON');
          expect(res.body.details.retryAfterSeconds).toBeGreaterThan(0);
        });

      for (let i = 0; i < 3; i++) {
        await api()
          .post('/api/v1/auth/register/verify')
          .send({ contact: EMAIL, code: wrong })
          .expect(400);
      }
      await api()
        .post('/api/v1/auth/register/verify')
        .send({ contact: EMAIL, code: wrong })
        .expect(429)
        .expect((res) => expect(res.body.code).toBe('CODE_LOCKED'));
      // Even the right code is refused once the code is locked.
      await api()
        .post('/api/v1/auth/register/verify')
        .send({ contact: EMAIL, code: t.otp.lastCode(EMAIL) })
        .expect(429);

      await t.prisma.otpCode.updateMany({
        data: { createdAt: new Date(Date.now() - 61_000) },
      });
      await api()
        .post('/api/v1/auth/code/resend')
        .send({ contact: EMAIL, purpose: 'REGISTER' })
        .expect(202);
      await api()
        .post('/api/v1/auth/register/verify')
        .send({ contact: EMAIL, code: t.otp.lastCode(EMAIL) })
        .expect(200);
    });

    it('refuses an expired code', async () => {
      await api()
        .post('/api/v1/auth/register')
        .send({ fullName: 'Duy Đức', contact: EMAIL, password: PASSWORD })
        .expect(202);
      await t.prisma.otpCode.updateMany({
        data: { expiresAt: new Date(Date.now() - 1000) },
      });

      await api()
        .post('/api/v1/auth/register/verify')
        .send({ contact: EMAIL, code: t.otp.lastCode(EMAIL) })
        .expect(400)
        .expect((res) => expect(res.body.code).toBe('CODE_EXPIRED'));
    });

    it('has no code to check for an unknown contact', async () => {
      await api()
        .post('/api/v1/auth/register/verify')
        .send({ contact: 'nobody@example.com', code: '123456' })
        .expect(400)
        .expect((res) => expect(res.body.code).toBe('NO_PENDING_CODE'));
    });
  });

  describe('sessions', () => {
    it('rotates refresh tokens and revokes everything when an old one is reused', async () => {
      const session = await signUp();

      const rotated = await api()
        .post('/api/v1/auth/refresh')
        .send({ refreshToken: session.refreshToken })
        .expect(200);
      expect(rotated.body.refreshToken).not.toBe(session.refreshToken);

      await api()
        .post('/api/v1/auth/refresh')
        .send({ refreshToken: session.refreshToken })
        .expect(401)
        .expect((res) => expect(res.body.code).toBe('INVALID_TOKEN'));
      // The reuse looked like a stolen token, so the newer token is revoked too.
      await api()
        .post('/api/v1/auth/refresh')
        .send({ refreshToken: rotated.body.refreshToken })
        .expect(401);
    });

    it('logs out by revoking the refresh token', async () => {
      const session = await signUp();

      await api()
        .post('/api/v1/auth/logout')
        .send({ refreshToken: session.refreshToken })
        .expect(204);
      await api()
        .post('/api/v1/auth/refresh')
        .send({ refreshToken: session.refreshToken })
        .expect(401);
    });

    it('requires a valid access token on private routes', async () => {
      await api()
        .get('/api/v1/me')
        .expect(401)
        .expect((res) => expect(res.body.code).toBe('UNAUTHORIZED'));
      await api()
        .get('/api/v1/me')
        .auth('not-a-token', { type: 'bearer' })
        .expect(401);
    });
  });

  describe('password reset', () => {
    it('resets the password once and signs out every device', async () => {
      const session = await signUp();

      await api()
        .post('/api/v1/auth/password/forgot')
        .send({ contact: 'nobody@example.com' })
        .expect(404)
        .expect((res) => expect(res.body.code).toBe('ACCOUNT_NOT_FOUND'));

      await api()
        .post('/api/v1/auth/password/forgot')
        .send({ contact: EMAIL })
        .expect(202);
      const verified = await api()
        .post('/api/v1/auth/password/verify')
        .send({ contact: EMAIL, code: t.otp.lastCode(EMAIL) })
        .expect(200);
      const { resetToken } = verified.body as { resetToken: string };

      // A reset token is not an access token.
      await api()
        .get('/api/v1/me')
        .auth(resetToken, { type: 'bearer' })
        .expect(401);

      await api()
        .post('/api/v1/auth/password/reset')
        .send({ resetToken, password: 'matkhaumoi456' })
        .expect(204);

      await api()
        .post('/api/v1/auth/login')
        .send({ contact: EMAIL, password: PASSWORD })
        .expect(401);
      await api()
        .post('/api/v1/auth/login')
        .send({ contact: EMAIL, password: 'matkhaumoi456' })
        .expect(200);

      await api()
        .post('/api/v1/auth/password/reset')
        .send({ resetToken, password: 'matkhaukhac789' })
        .expect(401)
        .expect((res) => expect(res.body.code).toBe('INVALID_TOKEN'));
      await api()
        .post('/api/v1/auth/refresh')
        .send({ refreshToken: session.refreshToken })
        .expect(401);
    });
  });

  describe('profile', () => {
    it('updates the name and rejects unknown fields', async () => {
      const { accessToken } = await signUp();

      await api()
        .patch('/api/v1/me')
        .auth(accessToken, { type: 'bearer' })
        .send({ fullName: '  Đỗ   Duy Đức ' })
        .expect(200)
        .expect((res) => expect(res.body.fullName).toBe('Đỗ Duy Đức'));

      await api()
        .patch('/api/v1/me')
        .auth(accessToken, { type: 'bearer' })
        .send({ fullName: 'Đức', email: 'other@example.com' })
        .expect(400);
    });
  });
});
