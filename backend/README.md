# TripFair Backend

REST API for the TripFair Android app, built with NestJS and TypeScript.

**Status:** early development

## Requirements

- Node.js 24 LTS
- npm 11
- Docker Desktop (for the local MySQL database)

## Getting started

1. Copy `.env.example` to `.env` and set the passwords.
2. Start MySQL 8.4 and install dependencies:

   ```bash
   docker compose up -d
   npm install
   ```

3. Start the API:

   ```bash
   npm run start:dev
   ```

The server runs at http://localhost:3000. `GET /health` returns `200` when the database is reachable and `503` when it is not.

## Scripts

| Command | Description |
| --- | --- |
| `npm run start:dev` | Start the server in watch mode |
| `npm run build` | Compile to `dist/` |
| `npm run start:prod` | Run the compiled build |
| `npm test` | Run unit tests (Vitest) |
| `npm run test:e2e` | Run end-to-end tests |
| `npm run lint` | Lint with oxlint |
| `npm run format` | Format with Prettier |
| `npm run db:generate` | Regenerate the Prisma Client after schema changes |
| `npm run db:migrate` | Create and apply a database migration |
