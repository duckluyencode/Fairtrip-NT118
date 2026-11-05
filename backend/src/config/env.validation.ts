import { plainToInstance } from 'class-transformer';
import {
  IsBooleanString,
  IsIn,
  IsNotEmpty,
  IsNumberString,
  IsOptional,
  IsPort,
  IsString,
  MinLength,
  ValidateIf,
  validateSync,
} from 'class-validator';

// Environment variables the app needs at startup; values stay strings, as in process.env.
class EnvironmentVariables {
  @IsOptional()
  @IsIn(['development', 'production', 'test'])
  NODE_ENV?: string;

  @IsOptional()
  @IsPort()
  PORT?: string;

  @IsString()
  @IsNotEmpty()
  MYSQL_HOST: string;

  @IsPort()
  MYSQL_PORT: string;

  @IsString()
  @IsNotEmpty()
  MYSQL_USER: string;

  @IsString()
  @IsNotEmpty()
  MYSQL_PASSWORD: string;

  @IsString()
  @IsNotEmpty()
  MYSQL_DATABASE: string;

  @IsOptional()
  @IsBooleanString()
  MYSQL_ALLOW_PUBLIC_KEY_RETRIEVAL?: string;

  /** Path to the RDS CA bundle; turns on verified TLS. Required in production. */
  @ValidateIf((env: EnvironmentVariables) => env.NODE_ENV === 'production')
  @IsString()
  @IsNotEmpty()
  MYSQL_SSL_CA?: string;

  /** Number of reverse proxies in front of the API (1 behind Caddy), so client IPs are right. */
  @IsOptional()
  @IsNumberString({ no_symbols: true })
  TRUST_PROXY?: string;

  @IsString()
  @MinLength(32)
  JWT_SECRET: string;

  // SMTP is optional in development (codes are logged) but required in production.
  @ValidateIf((env: EnvironmentVariables) => smtpRequired(env))
  @IsString()
  @IsNotEmpty()
  SMTP_HOST?: string;

  @ValidateIf((env: EnvironmentVariables) => smtpRequired(env))
  @IsPort()
  SMTP_PORT?: string;

  @ValidateIf((env: EnvironmentVariables) => smtpRequired(env))
  @IsString()
  @IsNotEmpty()
  SMTP_USER?: string;

  @ValidateIf((env: EnvironmentVariables) => smtpRequired(env))
  @IsString()
  @IsNotEmpty()
  SMTP_PASSWORD?: string;

  /** Optional: without it, search uses the TripFair catalog only. */
  @IsOptional()
  @IsString()
  GOOGLE_MAPS_API_KEY?: string;
}

function smtpRequired(env: EnvironmentVariables): boolean {
  return env.NODE_ENV === 'production' || Boolean(env.SMTP_HOST);
}

/** Fails startup with every missing or invalid variable listed, instead of failing on first use. */
export function validateEnv(
  config: Record<string, unknown>,
): Record<string, unknown> {
  const errors = validateSync(plainToInstance(EnvironmentVariables, config));
  if (errors.length > 0) {
    const lines = errors.map(
      (e) =>
        `- ${e.property}: ${Object.values(e.constraints ?? {}).join(', ')}`,
    );
    throw new Error(`Invalid environment variables:\n${lines.join('\n')}`);
  }
  return config;
}
