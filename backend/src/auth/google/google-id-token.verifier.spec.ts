import { generateKeyPairSync, sign, type KeyObject } from 'node:crypto';
import { ConfigService } from '@nestjs/config';
import { HttpGoogleIdTokenVerifier } from './google-id-token.verifier.js';

const CLIENT_ID = 'tripfair-test.apps.googleusercontent.com';
const KID = 'key-1';

const google = generateKeyPairSync('rsa', { modulusLength: 2048 });
const stranger = generateKeyPairSync('rsa', { modulusLength: 2048 });

function part(value: object): string {
  return Buffer.from(JSON.stringify(value)).toString('base64url');
}

/** An ID token as Google would sign it; `claims` replaces or removes the defaults. */
function token(
  claims: Record<string, unknown> = {},
  { key = google.privateKey, kid = KID, alg = 'RS256' } = {} as {
    key?: KeyObject;
    kid?: string;
    alg?: string;
  },
): string {
  const body = `${part({ alg, kid, typ: 'JWT' })}.${part({
    iss: 'https://accounts.google.com',
    aud: CLIENT_ID,
    exp: Math.floor(Date.now() / 1000) + 3600,
    sub: '1234567890',
    email: 'Duc@Example.com',
    email_verified: true,
    name: 'Duy Đức',
    picture: 'https://lh3.googleusercontent.com/a/photo',
    ...claims,
  })}`;
  const signature = sign('RSA-SHA256', Buffer.from(body), key);
  return `${body}.${signature.toString('base64url')}`;
}

function keysResponse(): Response {
  return new Response(
    JSON.stringify({
      keys: [{ ...google.publicKey.export({ format: 'jwk' }), kid: KID }],
    }),
    { headers: { 'cache-control': 'public, max-age=3600' } },
  );
}

function verifier(clientId: string | undefined = CLIENT_ID) {
  return new HttpGoogleIdTokenVerifier(
    new ConfigService(clientId ? { GOOGLE_CLIENT_ID: clientId } : {}),
  );
}

describe('HttpGoogleIdTokenVerifier', () => {
  let fetch: ReturnType<typeof vi.fn>;

  beforeEach(() => {
    fetch = vi.fn().mockImplementation(() => Promise.resolve(keysResponse()));
    vi.stubGlobal('fetch', fetch);
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('is disabled without a client id', async () => {
    const off = verifier('');
    expect(off.enabled).toBe(false);
    await expect(off.verify(token())).rejects.toMatchObject({
      code: 'GOOGLE_SIGN_IN_UNAVAILABLE',
    });
    expect(fetch).not.toHaveBeenCalled();
  });

  it('accepts a token Google signed for this app and reads the account', async () => {
    expect(await verifier().verify(token())).toEqual({
      sub: '1234567890',
      email: 'duc@example.com',
      name: 'Duy Đức',
      picture: 'https://lh3.googleusercontent.com/a/photo',
    });
  });

  it('downloads the keys once and reuses them', async () => {
    const v = verifier();
    await v.verify(token());
    await v.verify(token());
    expect(fetch).toHaveBeenCalledTimes(1);
  });

  it('rejects a token signed by someone else', async () => {
    expect(
      await verifier().verify(token({}, { key: stranger.privateKey })),
    ).toBeNull();
  });

  it('rejects a token whose content was changed after signing', async () => {
    const [header, , signature] = token().split('.');
    const forged = part({
      iss: 'https://accounts.google.com',
      aud: CLIENT_ID,
      exp: Math.floor(Date.now() / 1000) + 3600,
      sub: 'someone-else',
      email: 'victim@example.com',
      email_verified: true,
    });
    expect(
      await verifier().verify(`${header}.${forged}.${signature}`),
    ).toBeNull();
  });

  it.each([
    ['made for another app', { aud: 'other.apps.googleusercontent.com' }],
    ['from another issuer', { iss: 'https://accounts.example.com' }],
    ['that has expired', { exp: Math.floor(Date.now() / 1000) - 600 }],
    ['with an unverified email', { email_verified: false }],
    ['without an email', { email: undefined }],
    ['without an account id', { sub: undefined }],
  ])('rejects a token %s', async (_, claims) => {
    expect(await verifier().verify(token(claims))).toBeNull();
  });

  it('rejects unsigned tokens and text that is not a token', async () => {
    const v = verifier();
    expect(await v.verify(token({}, { alg: 'none' }))).toBeNull();
    expect(await v.verify(token({}, { alg: 'HS256' }))).toBeNull();
    expect(await v.verify('not-a-token')).toBeNull();
    expect(await v.verify('a.b.c')).toBeNull();
  });

  it('does not download the keys again for every unknown key id', async () => {
    const v = verifier();
    expect(await v.verify(token({}, { kid: 'unknown' }))).toBeNull();
    expect(await v.verify(token({}, { kid: 'unknown' }))).toBeNull();
    expect(fetch).toHaveBeenCalledTimes(1);
  });

  it('reports sign-in as unavailable when the keys cannot be read', async () => {
    fetch.mockRejectedValue(new Error('offline'));
    await expect(verifier().verify(token())).rejects.toMatchObject({
      code: 'GOOGLE_SIGN_IN_UNAVAILABLE',
    });
  });
});
