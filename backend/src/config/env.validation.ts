import { plainToInstance } from 'class-transformer';
import {
  IsBooleanString,
  IsIn,
  IsNotEmpty,
  IsOptional,
  IsPort,
  IsString,
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
