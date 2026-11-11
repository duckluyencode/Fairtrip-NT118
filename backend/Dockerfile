# syntax=docker/dockerfile:1
# Backend images, built from backend/:
#   runtime (default)  runs the API          docker build -t tripfair-api .
#   migrate            applies migrations    docker build --target migrate -t tripfair-migrate .

ARG NODE_IMAGE=node:24.22-alpine

# Full install and compile. The Prisma Client is generated here and compiled into dist/.
FROM ${NODE_IMAGE} AS build
WORKDIR /app
COPY package.json package-lock.json prisma.config.ts ./
COPY prisma ./prisma
# postinstall runs `prisma generate`; the config needs a DATABASE_URL to load, but generate
# never connects, so a placeholder is set for this command only.
RUN DATABASE_URL=mysql://build:build@localhost:3306/build npm ci
COPY tsconfig.json tsconfig.build.json nest-cli.json ./
COPY src ./src
RUN npm run build

# One-off job: applies pending migrations, then exits. Needs DATABASE_URL at run time.
# The sample catalog can be loaded with the command `npx prisma db seed` (needs MYSQL_*).
FROM build AS migrate
ENV NODE_ENV=production
CMD ["npx", "prisma", "migrate", "deploy"]

# The API: compiled code and production dependencies only, run as a non-root user.
FROM ${NODE_IMAGE} AS runtime
ENV NODE_ENV=production
WORKDIR /app
COPY package.json package-lock.json ./
# Install scripts are skipped: the only one generates the Prisma Client, which is already
# compiled into dist/ and needs the dev-only Prisma CLI. @prisma/client lists the Prisma CLI
# and TypeScript as optional peers, which npm keeps unless optional packages are omitted too;
# the API needs neither.
RUN npm ci --omit=dev --omit=optional --omit=peer --ignore-scripts && npm cache clean --force
COPY --from=build /app/dist ./dist
USER node
EXPOSE 3000
HEALTHCHECK --interval=30s --timeout=5s --start-period=20s --retries=3 \
  CMD ["node", "-e", "fetch('http://127.0.0.1:' + (process.env.PORT || 3000) + '/health').then((r) => process.exit(r.ok ? 0 : 1), () => process.exit(1))"]
CMD ["node", "dist/main.js"]
