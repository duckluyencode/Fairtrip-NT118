import { validateEnv } from './env.validation.js';

const valid = {
  MYSQL_HOST: 'localhost',
  MYSQL_PORT: '3306',
  MYSQL_USER: 'tripfair',
  MYSQL_PASSWORD: 'secret',
  MYSQL_DATABASE: 'tripfair',
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
});
