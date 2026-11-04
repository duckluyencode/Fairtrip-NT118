import { config as loadEnv } from 'dotenv';
import { defineConfig } from 'vitest/config';

// e2e tests run the real app against a separate database (default: <MYSQL_DATABASE>_test),
// so they never touch development data.
loadEnv({ quiet: true });
const testDatabase =
  process.env.MYSQL_TEST_DATABASE ?? `${process.env.MYSQL_DATABASE}_test`;

export default defineConfig({
  resolve: { tsconfigPaths: true },
  test: {
    globals: true,
    root: './',
    include: ['**/*.e2e-spec.ts'],
    env: { NODE_ENV: 'test', MYSQL_DATABASE: testDatabase },
    globalSetup: ['./test/setup/migrate-test-database.ts'],
    // Test files share one database and reset it between tests.
    fileParallelism: false,
  },
});
