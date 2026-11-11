# Xây dựng backend TripFair từng bước

Tài liệu này kể lại backend được dựng như thế nào, theo đúng thứ tự các bước A1–A11 và B2 trong [roadmap](roadmap.md): mỗi bước làm gì, vì sao, lệnh nào, file nào. Đọc từ trên xuống là dựng lại được từ đầu. Quy ước chi tiết (lệnh thường dùng, cách viết code) nằm ở [backend/CLAUDE.md](../backend/CLAUDE.md); thiết kế bảng ở [database.md](database.md); thuật toán ở [algorithm.md](algorithm.md); hạ tầng AWS ở [deploy.md](deploy.md); key Google và Gmail ở [google-cloud.md](google-cloud.md).

## Tổng quan

```
backend/
  prisma/              schema.prisma, migrations/, seed (danh mục mẫu cho phát triển)
  src/
    main.ts            khởi động server
    app.module.ts      ghép các module
    app.setup.ts       cấu hình HTTP chung (prefix /api/v1, pipe, filter), dùng cả cho e2e
    config/            kiểm tra biến môi trường lúc khởi động
    common/            AppError + mã lỗi, filter, validation, tiện ích thời gian
    prisma/            PrismaService (một PrismaClient cho cả app), adapter MariaDB + TLS
    health/            GET /health
    auth/, users/      đăng ký, OTP, đăng nhập, JWT, refresh token, hồ sơ
    places/            Google Places, Routes, giá, danh mục mẫu, tìm kiếm
    search-history/, favorites/
    trips/             chuyến đi, nhóm, bình chọn, lưu trú, lập và xem lộ trình
    planner/           thuật toán (thuần TypeScript)
    emergency-contacts/, recognition/
  test/                e2e test (Vitest + supertest), fakes cho dịch vụ ngoài
  scripts/             script phát triển (benchmark thuật toán)
  deploy/              script và cấu hình chạy trên EC2
  Dockerfile           image API và image migration
```

Luồng của một request: `AuthGuard` kiểm tra access token (trừ route `@Public()`) → `ValidationPipe` kiểm tra DTO → controller gọi service → service dùng Prisma và các client (Google) → lỗi nghiệp vụ ném `AppError` với `code` ổn định → `HttpExceptionFilter` trả `{ statusCode, code, message, details? }`.

## A1. Công cụ

Node.js 24 LTS, Docker Desktop (Windows cần WSL 2), Git. Kiểm tra: `node -v`, `docker version`.

## A2. Khung NestJS (ESM, Vitest, oxlint)

1. `npx @nestjs/cli new backend --strict --package-manager npm`.
2. Chuyển sang ESM: `"type": "module"` trong `package.json`, `tsconfig.json` dùng `"module": "nodenext"`. Từ đó mọi import nội bộ phải có đuôi `.js` (vd. `import { AppService } from './app.service.js'`), vì Node chạy ESM cần đường dẫn file thật sau khi biên dịch.
3. Thay Jest bằng Vitest: `vitest.config.ts` (unit test `*.spec.ts` cạnh code, `globals: true`) và `vitest.config.e2e.ts` (thư mục `test/`).
4. Lint bằng `oxlint --type-aware` (nhanh, có kiểm tra theo kiểu), định dạng bằng Prettier.

Kiểm tra: `npm run build`, `npm test`, `npm run lint` đều chạy.

## A3. MySQL và Prisma

1. [docker-compose.yml](../backend/docker-compose.yml): MySQL 8.4, chỉ mở ở `127.0.0.1`, dữ liệu trong volume `mysql-data`. Script trong `docker/mysql-init/` tạo thêm `tripfair_shadow` (cho `prisma migrate dev`) và `tripfair_test` (cho e2e).
2. Copy `.env.example` thành `.env`, đặt mật khẩu, `docker compose up -d`.
3. Prisma 7.10.1 (ghim đúng bản) với adapter MariaDB: [prisma.config.ts](../backend/prisma.config.ts) đọc `DATABASE_URL`; generator ghi Prisma Client vào `src/generated/prisma/` với `moduleFormat = "esm"` và `importFileExtension = "js"` (thiếu hai dòng này thì build trong Docker sẽ hỏng).
4. `PrismaService` ([src/prisma/](../backend/src/prisma/)) dùng `createMariaDbAdapter`: khi có `MYSQL_SSL_CA` thì bật TLS và xác minh chứng chỉ (bắt buộc ở production để nói chuyện với RDS).
5. `GET /health` trả 200 khi query được database, 503 `DATABASE_UNAVAILABLE` khi không.

## A4. Thiết kế database

Thiết kế cả app trước ([database.md](database.md)), rồi tạo bảng dần theo từng module. Quy ước: `snake_case` trong MySQL, `camelCase` trong Prisma (`@map`, `@@map`); tiền là số nguyên VND; thời gian UTC; không lưu số tính lại được.

Quy trình mỗi lần đổi schema:

```bash
npm run db:migrate -- --name <tên>
```

Lệnh này tạo migration từ thay đổi trong `schema.prisma`, áp dụng vào database trên máy và sinh lại Prisma Client (Prisma 7 không tự sinh sau `migrate dev`, nên script nối thêm `prisma generate`). Cần ràng buộc CHECK (Prisma không mô tả được): `npx prisma migrate dev --create-only --name <tên>`, thêm `ALTER TABLE ... ADD CONSTRAINT ... CHECK (...)` vào file SQL vừa tạo, rồi `npm run db:migrate`. Migration đã commit không sửa lại; cần đổi thì tạo migration mới.

## A5. Nền tảng API và CI

1. [app.setup.ts](../backend/src/app.setup.ts): prefix `/api/v1` (trừ `/health`), `ValidationPipe` toàn cục (`whitelist`, `forbidNonWhitelisted`: field lạ bị từ chối), `HttpExceptionFilter`, `trust proxy` khi chạy sau Caddy.
2. Lỗi: [error-code.ts](../backend/src/common/errors/error-code.ts) liệt kê mọi `code`; service ném `new AppError(HttpStatus.X, ErrorCode.Y, 'message')`. App Android dựa vào `code`, nên không đổi tên code đã dùng.
3. Swagger ở `/docs` (tắt ở production). Plugin Swagger của Nest CLI tự đọc class trong file `*.dto.ts`, `*.query.ts`, `*.response.ts` ([nest-cli.json](../backend/nest-cli.json)), nên DTO phải nằm trong các file đó.
4. Biến môi trường được kiểm tra lúc khởi động ([env.validation.ts](../backend/src/config/env.validation.ts)): thiếu biến bắt buộc thì server dừng ngay, không chạy với cấu hình sai.
5. E2e test chạy cả app trên database `tripfair_test`: global setup áp dụng migration, `resetDatabase` xóa dữ liệu trước mỗi test, `createTestApp()` thay các dịch vụ ngoài (gửi mã, Google Places, Routes, Vision) bằng bản giả trong [test/utils/test-app.ts](../backend/test/utils/test-app.ts), nên test không gọi mạng.
6. GitHub Actions ([backend-ci.yml](../.github/workflows/backend-ci.yml)): MySQL trong service container, chạy lint, build, unit test, e2e test và build cả hai image Docker cho mọi PR chạm `backend/`.

## A6. Tài khoản

- Đăng ký → gửi OTP 6 số (HMAC-SHA256 với `JWT_SECRET`, chỉ lưu hash) → xác minh → đăng nhập. Mã sống 15 phút, gửi lại sau 60 giây, khóa sau 5 lần sai (đếm bằng một câu `UPDATE` nguyên tử để hai request song song không vượt giới hạn).
- Mật khẩu hash bằng `scrypt` có sẵn trong Node (N = 2^17), không thêm thư viện.
- Xóa tài khoản (`DELETE /me`, nhập lại mật khẩu, hoặc đăng nhập Google lại với tài khoản không có mật khẩu): chuyển quyền các nhóm còn người khác, xóa phần còn lại; `AuthGuard` từ chối token của tài khoản đã xóa.
- Access token JWT HS256 15 phút; refresh token là chuỗi ngẫu nhiên, lưu SHA-256, đổi mới mỗi lần dùng; dùng lại token cũ thì thu hồi mọi phiên của người đó (dấu hiệu bị lộ).
- `AuthGuard` toàn cục: route nào cũng cần `Authorization: Bearer`, trừ route gắn `@Public()`; `@CurrentUser()` lấy người dùng.
- Giới hạn 120 request/phút mỗi IP, route `auth/*` 10 request/phút.
- Gửi mã qua `OtpDelivery`: email bằng SMTP (nodemailer, Gmail App Password); SMS chưa có nhà cung cấp. Development không có SMTP thì in mã ra log; production không bao giờ in mã.

## A7. Địa điểm và giá

- `GooglePlacesClient` là lớp trừu tượng, `HttpGooglePlacesClient` gọi Places API (New): Text Search (phân trang `nextPageToken`, ưu tiên quanh vị trí người dùng), Autocomplete (theo phiên `sessionToken`), Place Details. Field mask chọn đúng trường cần vì nó quyết định giá mỗi lượt gọi.
- Chỉ lưu `place_id` của Google trong bảng `places` (`source = GOOGLE`) để có id TripFair ổn định cho yêu thích, bình chọn, lộ trình; mọi dữ liệu khác lấy trực tiếp và trả kèm `attribution: "Google Maps"`.
- Không có key thì tìm trong danh mục mẫu Đà Nẵng (`prisma/seed/catalog.ts`, nạp bằng `npm run db:seed`): tìm không dấu, mỗi chữ gõ vào phải là đầu một từ, xếp hạng trong `ranking.ts`.
- Giá ([pricing.ts](../backend/src/places/pricing.ts)): danh mục có giá riêng hoặc giá vé của khu bao trọn; điểm Google đi theo chuỗi `priceRange` → loại thường miễn phí = 0đ → bảng quy đổi `priceLevel` → mức mặc định theo nhóm. Danh sách không trả giá; giá chỉ có ở chi tiết.
- Lịch sử tìm kiếm (50 mục, gộp từ khóa không phân biệt dấu) và yêu thích (gọi lặp lại không lỗi).

## A8, A9. Chuyến đi, nhóm, lộ trình

- Thuật toán viết riêng trong `src/planner/` (thuần TypeScript, test bằng dữ liệu nhỏ, không cần database) rồi mới nối vào API; xem [algorithm.md](algorithm.md).
- `TripsService`: tạo chuyến (một mình hoặc nhóm với mã mời 6 ký tự không có 0/O/1/I), tham gia, sửa ngân sách và sở thích của mình, rời nhóm, xóa chuyến; chủ nhóm mời thành viên ra, chuyển quyền, đổi mã mời. `TripAccess` chỉ cho thành viên đọc chuyến (người ngoài nhận `TRIP_NOT_FOUND` để không dò được id) và chỉ chủ chuyến lập lộ trình, chọn chỗ ở.
- Mọi thay đổi đầu vào gọi `TripAccess.resetPlan`: xóa lộ trình, chuyến về `DRAFT`.
- `VotesService`: đề xuất điểm, tick/bỏ tick (gọi lặp lại không lỗi), gửi phiếu (sau đó không đổi được), hạn bình chọn.
- `LodgingService`: tìm khách sạn thật trên Google (`includedType: lodging`), giá một đêm ước tính theo `priceLevel` cho tới khi có Agoda, chỉ gợi ý khách sạn mọi người đủ tiền, chọn 3 cái rẻ nhất / cân bằng / gần nhất; chọn thì chụp lại giá vào `trip_lodgings`.
- `PlanningService`: gom ứng viên, chạy thuật toán, tính lại giờ bằng Google Routes, lưu `trip_stops` và `stop_participants`. Tự thiết kế lộ trình (`PUT /trips/:id/itinerary`) dùng cùng cách tính giờ và giá nhưng không từ chối, chỉ cảnh báo.
- `ItineraryService`: trả lộ trình với giờ, giá kèm nguồn (dấu ~ khi là ước tính), phần mình trả, tổng dồn và số tiền còn lại, phần chi của từng người (không lộ ngân sách của người khác), mức hài lòng, chỉ số Jain, cảnh báo.

## A10. Liên hệ khẩn cấp, nhận diện ảnh

- Liên hệ khẩn cấp: CRUD đơn giản, tối đa 5, SĐT chuẩn hóa E.164. SMS SOS do app Android tự gửi (chạy được khi mất mạng).
- Nhận diện: `POST /places/recognize` nhận ảnh multipart (Nest `FileInterceptor`, giới hạn 4 MB), kiểm tra kiểu ảnh bằng byte đầu file, gửi Cloud Vision `LANDMARK_DETECTION`, rồi tìm từng địa danh trên Google Places quanh tọa độ Vision trả về. Không lưu ảnh.

## A11. Đánh giá thuật toán

`src/planner/exact.ts` tìm lời giải tối ưu bằng vét cạn có cắt nhánh; `npm run bench:planner` so ba thuật toán trên dữ liệu ngẫu nhiên theo seed. Kết quả và cách đọc ở [algorithm.md](algorithm.md).

## B2. Docker

[Dockerfile](../backend/Dockerfile) nhiều stage: `build` (cài đủ dependency, sinh Prisma Client, `nest build`), `runtime` (chỉ `dist/` và dependency production, user `node`, healthcheck gọi `/health`), `migrate` (chạy `prisma migrate deploy` rồi thoát). Chạy thử trên máy:

```bash
docker build -t tripfair-api .
```

```bash
docker run --rm --env-file .env -e MYSQL_HOST=host.docker.internal -e NODE_ENV=development -p 127.0.0.1:3100:3000 tripfair-api
```

Deploy lên AWS: [deploy.md](deploy.md).

## Thêm một module mới

Ví dụ thêm tính năng "ghi chú chuyến đi":

1. **Thiết kế bảng** trong [database.md](database.md), thêm model vào `schema.prisma`, `npm run db:migrate -- --name trip_notes`.
2. **Thư mục** `src/trip-notes/` gồm:
   - `trip-note.dto.ts`: class input với decorator `class-validator` (`@IsString()`, `@MaxLength()`…). Field không khai báo sẽ bị từ chối.
   - `trip-note.response.ts`: class output (Swagger tự đọc).
   - `trip-notes.service.ts`: logic, dùng `PrismaService`; luôn lọc theo người dùng hiện tại; lỗi nghiệp vụ ném `AppError` với code mới khai báo trong `error-code.ts`.
   - `trip-notes.controller.ts`: route mỏng, lấy người dùng bằng `@CurrentUser()`, gắn `@ApiTags` và `@ApiBearerAuth()`.
   - `trip-notes.module.ts`, rồi thêm vào `imports` của `app.module.ts`.
3. **Dịch vụ ngoài** (nếu có): khai báo lớp trừu tượng làm token (như `GooglePlacesClient`), bản thật gọi HTTP, bản giả trong `test/utils/test-app.ts`; key mới khai báo trong `env.validation.ts` và `.env.example`.
4. **Test**: unit test cho logic thuần (`*.spec.ts` cạnh code), e2e test cho route (`test/*.e2e-spec.ts`, mỗi test bắt đầu bằng `resetDatabase`).
5. **Kiểm tra trước khi commit**: `npm run lint`, `npm run build`, `npm test`, `npm run test:e2e`.
6. **Tài liệu**: cập nhật `backend/CLAUDE.md` (cách module hoạt động), `database.md`, roadmap.

## Danh sách API

Mọi route dưới `/api/v1` và cần access token, trừ nhóm auth và `/health`. Chi tiết request/response xem Swagger ở `/docs` khi chạy trên máy.

| Nhóm | Route |
| --- | --- |
| Sức khỏe | `GET /health` |
| Tài khoản (public) | `POST /auth/register`, `/auth/register/verify`, `/auth/code/resend`, `/auth/login`, `/auth/google`, `/auth/refresh`, `/auth/logout`, `/auth/password/forgot`, `/auth/password/verify`, `/auth/password/reset` |
| Hồ sơ | `GET /me`, `PATCH /me`, `DELETE /me` (xóa tài khoản) |
| Lịch sử tìm kiếm | `GET /me/search-history`, `DELETE /me/search-history/:id`, `DELETE /me/search-history` |
| Yêu thích | `GET /me/favorites`, `PUT /me/favorites/:placeId`, `DELETE /me/favorites/:placeId` |
| Liên hệ khẩn cấp | `GET /me/emergency-contacts`, `POST /me/emergency-contacts`, `PATCH /me/emergency-contacts/:id`, `DELETE /me/emergency-contacts/:id` |
| Địa điểm | `GET /places`, `GET /places/suggestions`, `GET /places/:id`, `POST /places/recognize` |
| Chuyến đi | `POST /trips`, `GET /trips`, `POST /trips/join`, `GET /trips/:id`, `DELETE /trips/:id`, `PATCH /trips/:id/me`, `DELETE /trips/:id/me` |
| Quản lý nhóm | `DELETE /trips/:id/members/:memberId`, `PUT /trips/:id/owner`, `POST /trips/:id/invite-code` |
| Lưu trú | `GET /trips/:id/lodging/options`, `PUT /trips/:id/lodging` |
| Bình chọn | `GET /trips/:id/options`, `POST /trips/:id/options`, `DELETE /trips/:id/options/:optionId`, `PUT /trips/:id/options/:optionId/vote`, `DELETE /trips/:id/options/:optionId/vote`, `POST /trips/:id/ballot` |
| Lộ trình | `POST /trips/:id/plan`, `GET /trips/:id/itinerary`, `PUT /trips/:id/itinerary` |
