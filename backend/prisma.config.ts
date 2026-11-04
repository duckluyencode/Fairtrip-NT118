import 'dotenv/config';
import { defineConfig, env } from 'prisma/config';

export default defineConfig({
  schema: 'prisma/schema.prisma',
  migrations: {
    path: 'prisma/migrations',
  },
  datasource: {
    url: env('DATABASE_URL'),
    // Only `prisma migrate dev` needs a shadow database; `migrate deploy` (RDS) does not.
    shadowDatabaseUrl: process.env.SHADOW_DATABASE_URL,
  },
});
