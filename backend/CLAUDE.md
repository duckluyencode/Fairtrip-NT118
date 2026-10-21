# Backend (NestJS)

API server cho app Android TripFair: xác thực, nguồn giá, thuật toán lộ trình và truy cập MySQL.
NestJS 12 + TypeScript (ESM), chạy trên Node.js 24 LTS. Quy tắc chung của dự án nằm ở [CLAUDE.md gốc](../CLAUDE.md).

## Lệnh thường dùng (chạy trong `backend/`)
- `npm install`: cài dependency
- `npm run start:dev`: chạy server có tự reload, mặc định ở http://localhost:3000
- `npm run build`: build ra `dist/`
- `npm test`: unit test (Vitest, file `*.spec.ts` đặt cạnh code)
- `npm run test:e2e`: e2e test (Vitest + supertest, thư mục `test/`)
- `npm run lint`: kiểm tra code bằng oxlint
- `npm run format`: định dạng code bằng Prettier

## Database trên máy (MySQL 8.4 qua Docker)
- Lần đầu: copy `.env.example` thành `.env` và đặt mật khẩu.
- `docker compose up -d`: chạy MySQL (container `tripfair-mysql`, dữ liệu nằm trong volume `mysql-data`).
- `docker compose stop`: tắt MySQL, giữ dữ liệu. `docker compose down -v` xóa luôn dữ liệu: hỏi trước khi chạy.
- `npm run db:generate`: sinh lại Prisma Client sau khi sửa `prisma/schema.prisma` (tự chạy sau `npm install`).
- `npm run db:migrate`: tạo và áp dụng migration (dùng từ bước A4).
- `GET /health`: 200 khi backend kết nối được MySQL, 503 khi không.

## Quy ước
- Code là ESM: import file nội bộ phải có đuôi `.js` (vd. `import { AppService } from './app.service.js'`).
- Test dùng Vitest (`describe`, `it`, `expect` là global), không dùng Jest.
- Cổng mặc định 3000, đổi bằng biến môi trường `PORT`.
- Prisma ghim đúng 7.10.1 cho `prisma`, `@prisma/client`, `@prisma/adapter-mariadb`; không nâng lên Prisma 8 khi chưa thống nhất.
- Truy cập database qua `PrismaService` (`src/prisma/`), dùng chung một PrismaClient cho cả app.
- Prisma Client sinh vào `src/generated/prisma/`: không commit, không sửa tay.
- `MYSQL_ALLOW_PUBLIC_KEY_RETRIEVAL=true` chỉ dùng cho MySQL trên máy; kết nối RDS dùng TLS.
