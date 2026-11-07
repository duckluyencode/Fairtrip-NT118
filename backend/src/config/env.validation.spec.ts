import { validateEnv } from './env.validation.js';

const valid = {
  MYSQL_HOST: 'localhost',
  MYSQL_PORT: '3306',
  MYSQL_USER: 'tripfair',
  MYSQL_PASSWORD: 'secret',
  MYSQL_DATABASE: 'tripfair',
  JWT_SECRET: 'x'.repeat(32),
};

describe('validateEnv', () => {
  it('accepts a complete configuration', () => {
    expect(validateEnv(valid)).toBe(valid);
  });

  it('lists every missing or invalid variable', () => {
    const { MYSQL_PASSWORD: _, ...withoutPassword } = valid;
    expect(() =>
      validateEnv({ ...withoutPassword, MYSQL_PORT: 'abc' }),
    ).toThrow(
      /MYSQL_PORT[\s\S]*MYSQL_PASSWORD|MYSQL_PASSWORD[\s\S]*MYSQL_PORT/,
    );
  });

  it('rejects a short JWT secret', () => {
    expect(() => validateEnv({ ...valid, JWT_SECRET: 'short' })).toThrow(
      /JWT_SECRET/,
    );
  });

  it('allows no SMTP in development but requires it in production', () => {
    expect(() => validateEnv({ ...valid, SMTP_HOST: '' })).not.toThrow();
    expect(() => validateEnv({ ...valid, NODE_ENV: 'production' })).toThrow(
      /SMTP_HOST[\s\S]*SMTP_PASSWORD/,
    );
  });

  it('requires the rest of SMTP once a host is set', () => {
    expect(() =>
      validateEnv({ ...valid, SMTP_HOST: 'smtp.gmail.com', SMTP_PORT: '465' }),
    ).toThrow(/SMTP_USER[\s\S]*SMTP_PASSWORD/);
  });
});
