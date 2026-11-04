import { execSync } from 'node:child_process';
import { config as loadEnv } from 'dotenv';

/** Vitest global setup: applies every migration to the e2e database before the tests run. */
export default function migrateTestDatabase(): void {
  loadEnv({ quiet: true });
  const { MYSQL_USER, MYSQL_PASSWORD, MYSQL_HOST, MYSQL_PORT, MYSQL_DATABASE } =
    process.env;
  const database = process.env.MYSQL_TEST_DATABASE ?? `${MYSQL_DATABASE}_test`;
  const url = new URL(`mysql://${MYSQL_HOST}:${MYSQL_PORT}/${database}`);
  url.username = MYSQL_USER ?? '';
  url.password = MYSQL_PASSWORD ?? '';

  try {
    execSync('npx prisma migrate deploy', {
      env: { ...process.env, DATABASE_URL: url.toString() },
      stdio: 'pipe',
    });
  } catch (error) {
    const stderr = (error as { stderr?: Buffer }).stderr?.toString() ?? '';
    throw new Error(
      `Could not migrate the e2e database "${database}":\n${stderr}`,
    );
  }
}
