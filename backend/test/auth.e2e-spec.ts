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
    t.googleSignIn.accounts.clear();
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

  describe('Google sign-in', () => {
    const GOOGLE = {
      sub: 'google-1',
      email: EMAIL,
      name: 'Đỗ Duy Đức',
      picture: 'https://lh3.googleusercontent.com/a/photo',
    };
    const google = (idToken: string) =>
      api().post('/api/v1/auth/google').send({ idToken });

    beforeEach(() => {
      t.googleSignIn.accounts.set('token-1', GOOGLE);
    });

    it('creates an account without a password the first time, then signs in to it', async () => {
      const first = await google('token-1').expect(200);
      expect(first.body.user).toMatchObject({
        email: EMAIL,
        fullName: 'Đỗ Duy Đức',
        avatarUrl: GOOGLE.picture,
        hasPassword: false,
        googleLinked: true,
      });
      expect(first.body.accessToken).toEqual(expect.any(String));
      expect(first.body.refreshToken).toEqual(expect.any(String));

      const again = await google('token-1').expect(200);
      expect(again.body.user.id).toBe(first.body.user.id);
      expect(await t.prisma.user.count()).toBe(1);

      await api()
        .get('/api/v1/me')
        .auth(again.body.accessToken, { type: 'bearer' })
        .expect(200);
    });

    it('rejects a token Google did not issue for this app', async () => {
      await google('forged')
        .expect(401)
        .expect((res) => expect(res.body.code).toBe('INVALID_GOOGLE_TOKEN'));
      await api().post('/api/v1/auth/google').send({}).expect(400);
      expect(await t.prisma.user.count()).toBe(0);
    });

    it('joins the account that already uses the email and keeps its password', async () => {
      await signUp();
      const res = await google('token-1').expect(200);
      expect(res.body.user).toMatchObject({
        fullName: 'Duy Đức',
        hasPassword: true,
        googleLinked: true,
      });
      expect(await t.prisma.user.count()).toBe(1);

      await api()
        .post('/api/v1/auth/login')
        .send({ contact: EMAIL, password: PASSWORD })
        .expect(200);
    });

    it('takes over a sign-up that never proved it owns the email, dropping its password', async () => {
      await api()
        .post('/api/v1/auth/register')
        .send({ fullName: 'Kẻ lạ', contact: EMAIL, password: 'matkhau-la1' })
        .expect(202);

      const res = await google('token-1').expect(200);
      expect(res.body.user).toMatchObject({
        hasPassword: false,
        googleLinked: true,
      });
      await api()
        .post('/api/v1/auth/login')
        .send({ contact: EMAIL, password: 'matkhau-la1' })
        .expect(401);
      // The code sent to the stranger's sign-up no longer works.
      await api()
        .post('/api/v1/auth/register/verify')
        .send({ contact: EMAIL, code: t.otp.lastCode(EMAIL) })
        .expect(400);
    });

    it('does not let a password sign in to an account that only uses Google', async () => {
      await google('token-1').expect(200);
      await api()
        .post('/api/v1/auth/login')
        .send({ contact: EMAIL, password: PASSWORD })
        .expect(401)
        .expect((res) => expect(res.body.code).toBe('WRONG_CREDENTIALS'));
    });

    it('lets an account that only uses Google set a password with a reset code', async () => {
      await google('token-1').expect(200);
      await api()
        .post('/api/v1/auth/password/forgot')
        .send({ contact: EMAIL })
        .expect(202);
      const verified = await api()
        .post('/api/v1/auth/password/verify')
        .send({ contact: EMAIL, code: t.otp.lastCode(EMAIL) })
        .expect(200);
      await api()
        .post('/api/v1/auth/password/reset')
        .send({ resetToken: verified.body.resetToken, password: PASSWORD })
        .expect(204);

      const res = await api()
        .post('/api/v1/auth/login')
        .send({ contact: EMAIL, password: PASSWORD })
        .expect(200);
      expect(res.body.user).toMatchObject({
        hasPassword: true,
        googleLinked: true,
      });
    });

    it('refuses an email that is linked to another Google account', async () => {
      await google('token-1').expect(200);
      t.googleSignIn.accounts.set('token-2', { ...GOOGLE, sub: 'google-2' });
      await google('token-2')
        .expect(409)
        .expect((res) => expect(res.body.code).toBe('ACCOUNT_EXISTS'));
    });

    it('deletes an account without a password after a new Google sign-in', async () => {
      const { body } = await google('token-1').expect(200);
      const del = () =>
        api().delete('/api/v1/me').auth(body.accessToken, { type: 'bearer' });
      t.googleSignIn.accounts.set('token-other', {
        ...GOOGLE,
        sub: 'google-2',
        email: 'other@example.com',
      });

      await del()
        .send({ password: PASSWORD })
        .expect(403)
        .expect((res) => expect(res.body.code).toBe('WRONG_CREDENTIALS'));
      await del().send({ googleIdToken: 'forged' }).expect(403);
      await del().send({ googleIdToken: 'token-other' }).expect(403);
      expect(await t.prisma.user.count()).toBe(1);

      await del().send({ googleIdToken: 'token-1' }).expect(204);
      expect(await t.prisma.user.count()).toBe(0);
    });

    it('does not accept a Google token to delete an account never linked to Google', async () => {
      const { accessToken } = await signUp();
      await api()
        .delete('/api/v1/me')
        .auth(accessToken, { type: 'bearer' })
        .send({ googleIdToken: 'token-1' })
        .expect(403);
      expect(await t.prisma.user.count()).toBe(1);
    });
  });

  describe('account deletion', () => {
    it('deletes the account after checking the password, and its token stops working', async () => {
      const { accessToken } = await signUp();
      const del = () =>
        api().delete('/api/v1/me').auth(accessToken, { type: 'bearer' });

      await del()
        .send({ password: 'wrong-password' })
        .expect(403)
        .expect((res) => expect(res.body.code).toBe('WRONG_CREDENTIALS'));
      await del().send({}).expect(400);

      await del().send({ password: PASSWORD }).expect(204);

      // The access token has not expired yet, but the account is gone.
      await api()
        .get('/api/v1/me')
        .auth(accessToken, { type: 'bearer' })
        .expect(401);
      await api()
        .post('/api/v1/auth/login')
        .send({ contact: EMAIL, password: PASSWORD })
        .expect(401)
        .expect((res) => expect(res.body.code).toBe('WRONG_CREDENTIALS'));
      expect(await t.prisma.refreshToken.count()).toBe(0);
      expect(await t.prisma.otpCode.count()).toBe(0);

      // The same email can sign up again.
      await signUp();
    });
  });
});
