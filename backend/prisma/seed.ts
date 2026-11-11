// `npm run db:seed`: loads the sample Đà Nẵng catalog into the database in .env.
// Development and tests only: production uses real places from Google Places and never
// runs this seed.
import 'dotenv/config';
import { PrismaClient } from '../src/generated/prisma/client.js';
import { createMariaDbAdapter } from '../src/prisma/adapter.js';
import { seedCatalog } from './seed/catalog.js';

if (process.env.NODE_ENV === 'production') {
  throw new Error('The sample catalog is not loaded into production');
}

const prisma = new PrismaClient({
  adapter: createMariaDbAdapter((key) => process.env[key]),
});

try {
  const places = await seedCatalog(prisma);
  console.log(`Seeded ${places} sample catalog places`);
} finally {
  await prisma.$disconnect();
}
