import { PrismaMariaDb } from '@prisma/adapter-mariadb';

/** Builds the MySQL driver adapter from MYSQL_* settings; shared by the app and scripts. */
export function createMariaDbAdapter(
  get: (key: string) => string | undefined,
): PrismaMariaDb {
  const required = (key: string): string => {
    const value = get(key);
    if (!value) {
      throw new Error(`Missing environment variable ${key}`);
    }
    return value;
  };
  return new PrismaMariaDb({
    host: required('MYSQL_HOST'),
    port: Number(required('MYSQL_PORT')),
    user: required('MYSQL_USER'),
    password: required('MYSQL_PASSWORD'),
    database: required('MYSQL_DATABASE'),
    connectionLimit: 5,
    // Fail fast (so /health returns 503) instead of the 10 s default wait.
    acquireTimeout: 5000,
    // MySQL 8.4 uses caching_sha2_password: without TLS, the first login after
    // a server restart needs the server's RSA public key. Enable only for the
    // local Docker database; RDS connections use TLS instead.
    allowPublicKeyRetrieval: get('MYSQL_ALLOW_PUBLIC_KEY_RETRIEVAL') === 'true',
  });
}
