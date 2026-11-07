import { randomBytes, scrypt, timingSafeEqual } from 'node:crypto';
import {
  buildMessage,
  ValidateBy,
  type ValidationOptions,
} from 'class-validator';

// OWASP minimum for scrypt: N = 2^17, r = 8, p = 1. The parameters are stored with
// each hash, so they can be raised later without breaking existing passwords.
const LOG2_N = 17;
const BLOCK_SIZE = 8;
const PARALLELISM = 1;
const KEY_LENGTH = 32;
const SALT_LENGTH = 16;

export const PASSWORD_MIN_LENGTH = 8;
export const PASSWORD_MAX_LENGTH = 128;

/** Returns `scrypt$<log2N>$<r>$<p>$<salt>$<key>` (base64 salt and key). */
export async function hashPassword(password: string): Promise<string> {
  const salt = randomBytes(SALT_LENGTH);
  const key = await derive(
    password,
    salt,
    LOG2_N,
    BLOCK_SIZE,
    PARALLELISM,
    KEY_LENGTH,
  );
  return [
    'scrypt',
    LOG2_N,
    BLOCK_SIZE,
    PARALLELISM,
    salt.toString('base64'),
    key.toString('base64'),
  ].join('$');
}

export async function verifyPassword(
  password: string,
  stored: string,
): Promise<boolean> {
  const [algorithm, log2N, r, p, salt, key] = stored.split('$');
  if (algorithm !== 'scrypt' || !salt || !key) {
    return false;
  }
  const expected = Buffer.from(key, 'base64');
  const actual = await derive(
    password,
    Buffer.from(salt, 'base64'),
    Number(log2N),
    Number(r),
    Number(p),
    expected.length,
  );
  return timingSafeEqual(actual, expected);
}

function derive(
  password: string,
  salt: Buffer,
  log2N: number,
  r: number,
  p: number,
  keyLength: number,
): Promise<Buffer> {
  const N = 2 ** log2N;
  return new Promise((resolve, reject) => {
    // NFKC so the same Vietnamese text typed on different keyboards hashes the same.
    // scrypt needs 128 * N * r bytes; Node's default limit (32 MB) is too low for N = 2^17.
    scrypt(
      password.normalize('NFKC'),
      salt,
      keyLength,
      { N, r, p, maxmem: 256 * N * r },
      (error, key) => (error ? reject(error) : resolve(key)),
    );
  });
}

/** DTO rule from the Android app: at least 8 characters with both letters and digits. */
export function IsPassword(options?: ValidationOptions): PropertyDecorator {
  return ValidateBy(
    {
      name: 'isPassword',
      validator: {
        validate: (value) =>
          typeof value === 'string' &&
          value.length >= PASSWORD_MIN_LENGTH &&
          value.length <= PASSWORD_MAX_LENGTH &&
          /\p{L}/u.test(value) &&
          /\d/.test(value),
        defaultMessage: buildMessage(
          (each) =>
            `${each}$property must be ${PASSWORD_MIN_LENGTH}-${PASSWORD_MAX_LENGTH} characters with letters and digits`,
          options,
        ),
      },
    },
    options,
  );
}
