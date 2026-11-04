# Backend (NestJS)

API server cho app Android TripFair: xác thực, nguồn giá, thuật toán lộ trình và truy cập MySQL.
NestJS 12 + TypeScript (ESM), chạy trên Node.js 24 LTS. Quy tắc chung của dự án nằm ở [CLAUDE.md gốc](../CLAUDE.md).

## Lệnh thường dùng (chạy trong `backend/`)
- `npm install`: cài dependency
- `npm run start:dev`: chạy server có tự reload, mặc định ở http://localhost:3000
- `npm run build`: build ra `dist/`
- `npm test`: unit test (Vitest, file `*.spec.ts` đặt cạnh code), không cần database
- `npm run test:e2e`: e2e test (Vitest + supertest, thư mục `test/`) chạy cả app trên database `tripfair_test` (cần MySQL đang chạy); tự áp dụng migration trước khi chạy, mỗi test tự xóa dữ liệu bằng `resetDatabase`
- `npm run lint`: kiểm tra code bằng oxlint
- `npm run format`: định dạng code bằng Prettier

## Database trên máy (MySQL 8.4 qua Docker)
- Lần đầu: copy `.env.example` thành `.env` và đặt mật khẩu.
- `docker compose up -d`: chạy MySQL (container `tripfair-mysql`, dữ liệu nằm trong volume `mysql-data`).
- `docker compose stop`: tắt MySQL, giữ dữ liệu. `docker compose down -v` xóa luôn dữ liệu: hỏi trước khi chạy.
- `npm run db:generate`: sinh lại Prisma Client sau khi sửa `prisma/schema.prisma` (tự chạy sau `npm install`).
- `npm run db:migrate -- --name <tên>`: tạo migration từ thay đổi trong `prisma/schema.prisma`, áp dụng vào database trên máy rồi sinh lại Prisma Client (Prisma 7 không tự sinh sau `migrate dev`).
- Script trong `docker/mysql-init/` tạo thêm `tripfair_shadow` (cho `migrate dev`, biến `SHADOW_DATABASE_URL`) và `tripfair_test` (cho e2e) khi volume được tạo lần đầu, user `tripfair` chỉ có quyền trên các database này. Volume có từ trước thì chạy một lần: `docker exec tripfair-mysql sh /docker-entrypoint-initdb.d/01-create-extra-databases.sh`.
- `GET /health`: 200 khi backend kết nối được MySQL, 503 (`DATABASE_UNAVAILABLE`) khi không.

## Xác thực (`src/auth/`, `src/users/`)
- Mọi route cần `Authorization: Bearer <accessToken>`, trừ route gắn `@Public()` (`AuthGuard` toàn cục); lấy người dùng hiện tại bằng `@CurrentUser()`.
- Access token là JWT HS256 sống 15 phút; refresh token là chuỗi ngẫu nhiên lưu dạng SHA-256, sống 30 ngày, đổi mới mỗi lần dùng; token reset mật khẩu là JWT 10 phút, mỗi token dùng một lần. `JWT_SECRET` (≥ 32 ký tự) ký JWT và làm khóa HMAC cho mã OTP.
- Mật khẩu hash bằng `scrypt` của Node (N = 2^17, r = 8, p = 1), không thêm thư viện hash.
- OTP 6 số: hiệu lực 15 phút, gửi lại sau 60 giây, khóa sau 5 lần sai. Route `auth/*` giới hạn 10 request/phút mỗi IP.
- Gửi mã qua `OtpDelivery`: email bằng SMTP (`SMTP_*` trong `.env`; Gmail dùng App Password), SMS chưa có nhà cung cấp nên chỉ in log ngoài production. Để trống `SMTP_HOST` thì email cũng in ra log (chỉ khi development). E2e test thay `OtpDelivery` bằng `FakeOtpDelivery` để đọc mã.
- Email/SĐT được chuẩn hóa như app Android (`src/auth/contact.ts`): email viết thường, SĐT lưu dạng `+84…`.
## Quy ước
- Code là ESM: import file nội bộ phải có đuôi `.js` (vd. `import { AppService } from './app.service.js'`).
- Test dùng Vitest (`describe`, `it`, `expect` là global), không dùng Jest.
- Cổng mặc định 3000, đổi bằng biến môi trường `PORT`. Biến môi trường được kiểm tra lúc khởi động trong `src/config/env.validation.ts`; thêm biến mới thì khai báo ở đó và trong `.env.example`.
- Mọi route nằm dưới `/api/v1` (trừ `/health`). Cấu hình HTTP chung (prefix, pipe, filter) ở `src/app.setup.ts`, dùng cho cả `main.ts` và e2e test.
- Input nhận qua DTO có decorator class-validator; field không khai báo trong DTO bị từ chối.
- Lỗi luôn có dạng `{ statusCode, code, message, details? }`. Lỗi nghiệp vụ ném `AppError` với `code` ổn định (app Android dựa vào `code`, không đổi tên code đã dùng); lỗi không lường trước trả 500 và không lộ chi tiết.
- Tài liệu API (Swagger) ở `/docs` khi không chạy production; plugin Swagger tự đọc DTO lúc `nest build`.
- Giới hạn 120 request/phút mỗi IP (`@nestjs/throttler`), tắt khi `NODE_ENV=test`.
- CI: `.github/workflows/backend-ci.yml` chạy lint, build, unit test và e2e test (MySQL trong service container) cho mọi PR chạm `backend/`.
- Prisma ghim đúng 7.10.1 cho `prisma`, `@prisma/client`, `@prisma/adapter-mariadb`; không nâng lên Prisma 8 khi chưa thống nhất.
- Truy cập database qua `PrismaService` (`src/prisma/`), dùng chung một PrismaClient cho cả app.
- Thiết kế bảng nằm ở [docs/database.md](../docs/database.md): thêm hoặc sửa bảng thì cập nhật tài liệu đó trong cùng thay đổi. Migration đã commit không sửa lại; cần đổi thì tạo migration mới.
- Prisma Client sinh vào `src/generated/prisma/`: không commit, không sửa tay.
- `MYSQL_ALLOW_PUBLIC_KEY_RETRIEVAL=true` chỉ dùng cho MySQL trên máy; kết nối RDS dùng TLS.
