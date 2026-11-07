// `npm run db:seed`: loads the sample place catalog into the database in .env.
import 'dotenv/config';
import { PrismaClient } from '../src/generated/prisma/client.js';
import { createMariaDbAdapter } from '../src/prisma/adapter.js';
import { seedCatalog } from './seed/catalog.js';

const prisma = new PrismaClient({
  adapter: createMariaDbAdapter((key) => process.env[key]),
});

try {
  const count = await seedCatalog(prisma);
  console.log(`Seeded ${count} catalog places`);
} finally {
  await prisma.$disconnect();
}
