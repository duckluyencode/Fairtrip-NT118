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
- `npm run bench:planner`: đánh giá thuật toán lập lộ trình (Greedy thuần, bản đầy đủ, lời giải tối ưu) trên dữ liệu ngẫu nhiên theo seed, in bảng Markdown; kết quả và cách đọc ở [docs/algorithm.md](../docs/algorithm.md). Script phát triển nằm trong `scripts/`, chạy bằng `tsx`, không nằm trong bản build.

## Database trên máy (MySQL 8.4 qua Docker)
- Lần đầu: copy `.env.example` thành `.env` và đặt mật khẩu.
- `docker compose up -d`: chạy MySQL (container `tripfair-mysql`, dữ liệu nằm trong volume `mysql-data`).
- `docker compose stop`: tắt MySQL, giữ dữ liệu. `docker compose down -v` xóa luôn dữ liệu: hỏi trước khi chạy.
- `npm run db:generate`: sinh lại Prisma Client sau khi sửa `prisma/schema.prisma` (tự chạy sau `npm install`).
- `npm run db:migrate -- --name <tên>`: tạo migration từ thay đổi trong `prisma/schema.prisma`, áp dụng vào database trên máy rồi sinh lại Prisma Client (Prisma 7 không tự sinh sau `migrate dev`). Cần ràng buộc CHECK thì tạo bằng `npx prisma migrate dev --create-only`, thêm SQL vào migration rồi mới áp dụng.
- `npm run db:seed`: nạp dữ liệu mẫu (danh mục địa điểm Đà Nẵng) cho database trên máy; không chạy ở production.
- Script trong `docker/mysql-init/` tạo thêm `tripfair_shadow` (cho `migrate dev`, biến `SHADOW_DATABASE_URL`) và `tripfair_test` (cho e2e) khi volume được tạo lần đầu, user `tripfair` chỉ có quyền trên các database này. Volume có từ trước thì chạy một lần: `docker exec tripfair-mysql sh /docker-entrypoint-initdb.d/01-create-extra-databases.sh`.
- `GET /health`: 200 khi backend kết nối được MySQL, 503 (`DATABASE_UNAVAILABLE`) khi không.

## Docker image (`Dockerfile`)
- `docker build -t tripfair-api .`: image chạy API (`NODE_ENV=production`, chỉ `dist/` và dependency production, chạy bằng user `node`, healthcheck gọi `/health`).
- `docker build --target migrate -t tripfair-migrate .`: image chạy `prisma migrate deploy` rồi thoát, cần `DATABASE_URL`. Không nạp danh mục mẫu ở production: `prisma/seed.ts` từ chối chạy khi `NODE_ENV=production`.
- Chạy thử trên máy: `docker run --rm --env-file .env -e MYSQL_HOST=host.docker.internal -e NODE_ENV=development -p 127.0.0.1:3100:3000 tripfair-api`. Ở production, thiếu biến bắt buộc (vd. `MYSQL_SSL_CA`) thì server dừng ngay lúc khởi động.
- Generator Prisma ghi rõ `moduleFormat` và `importFileExtension`; không bỏ đi, vì khi `npm ci` chạy trong Docker chưa có `tsconfig.json` và Prisma sẽ đoán sai đuôi import (`.ts`).
- CI build cả hai image cho mọi PR chạm `backend/`.
- Deploy lên AWS: script trong `deploy/` (`push-images.sh` build và đẩy image lên ECR với tag là mã commit, `deploy.sh` chạy trên EC2 qua SSM, `ssm-run.ps1` gửi lệnh lên máy, `create-app-user.sh` tạo user MySQL trên RDS). Các bước đầy đủ ở [docs/deploy.md](../docs/deploy.md).

## Xác thực (`src/auth/`, `src/users/`)
- Mọi route cần `Authorization: Bearer <accessToken>`, trừ route gắn `@Public()` (`AuthGuard` toàn cục); lấy người dùng hiện tại bằng `@CurrentUser()`.
- Access token là JWT HS256 sống 15 phút; refresh token là chuỗi ngẫu nhiên lưu dạng SHA-256, sống 30 ngày, đổi mới mỗi lần dùng; token reset mật khẩu là JWT 10 phút, mỗi token dùng một lần. `JWT_SECRET` (≥ 32 ký tự) ký JWT và làm khóa HMAC cho mã OTP.
- Mật khẩu hash bằng `scrypt` của Node (N = 2^17, r = 8, p = 1), không thêm thư viện hash.
- OTP 6 số: hiệu lực 15 phút, gửi lại sau 60 giây, khóa sau 5 lần sai. Route `auth/*` giới hạn 10 request/phút mỗi IP.
- Gửi mã qua `OtpDelivery`: email bằng SMTP (`SMTP_*` trong `.env`; Gmail dùng App Password), SMS chưa có nhà cung cấp. Kênh nào chưa có nhà cung cấp (SMS, hoặc email khi để trống `SMTP_HOST`) thì in mã ra log khi chạy development, còn ở production thì báo `CODE_DELIVERY_FAILED` và không bao giờ in mã. E2e test thay `OtpDelivery` bằng `FakeOtpDelivery` để đọc mã.
- Email/SĐT được chuẩn hóa như app Android (`src/auth/contact.ts`): email viết thường, SĐT lưu dạng `+84…`.

## Địa điểm, lịch sử tìm kiếm, yêu thích (`src/places/`, `src/search-history/`, `src/favorites/`)
- Dữ liệu thật lấy từ Google Places (New) qua `GooglePlacesClient`: có `GOOGLE_MAPS_API_KEY` thì tìm kiếm (Text Search, phân trang bằng `nextPageToken`, ưu tiên quanh vị trí người dùng), gợi ý khi gõ (Autocomplete, gửi cùng `sessionToken` tới lúc mở chi tiết) và chi tiết đều lấy từ Google. Không có key (phát triển, test) thì dùng danh mục mẫu.
- Chỉ lưu `place_id` của Google (`places.source = GOOGLE`), mọi dữ liệu khác lấy trực tiếp và trả kèm `attribution: "Google Maps"`. Field mask quyết định giá tiền mỗi lượt gọi: chỉ xin trường màn hình cần (`LIST_FIELDS`, `PLANNING_FIELDS`, `DETAIL_FIELDS` trong `http-google-places.client.ts`). Google lỗi thì trả 503 `GOOGLE_PLACES_UNAVAILABLE`.
- Google Routes (`RoutesClient`, `computeRoutes`): một request mỗi ngày của lộ trình để lấy thời gian lái xe thật giữa các điểm; không có key hoặc Google lỗi thì giữ ước tính đường chim bay.
- E2e test thay Google bằng `FakeGooglePlacesClient` và `FakeRoutesClient` (`test/utils/test-app.ts`), không gọi mạng.
- `npm run db:seed`: nạp danh mục mẫu Đà Nẵng (`prisma/seed/catalog.ts`), cập nhật theo `slug`, chạy lại nhiều lần được. Thêm điểm thì sửa file này; điểm nào cũng phải có giá (hoặc nằm trong khu có giá) và sở thích (seed kiểm tra).
- Tìm trong danh mục: không dấu qua `places.search_text` (`normalizeText` trong `src/places/text.ts`, đ → d); mỗi chữ gõ vào phải là đầu một từ. Xếp hạng trong `src/places/ranking.ts`.
- Danh sách (tìm kiếm, yêu thích) không trả giá; giá chỉ có ở `GET /places/:id` (quy tắc giá trong `src/places/pricing.ts`).
- Lịch sử được lưu khi `GET /places` có `q`; cùng từ khóa (không phân biệt hoa thường, dấu) chỉ một mục; giữ 50 mục mỗi người.
- Lưu/bỏ yêu thích (`PUT`/`DELETE /me/favorites/:placeId`) gọi lặp lại không lỗi. Dữ liệu của người khác luôn lọc theo `userId`, xóa mục không phải của mình trả 404.
- Plugin Swagger đọc class trong file `*.dto.ts`, `*.query.ts`, `*.response.ts`: đặt DTO vào các file này, không khai báo trong controller.

## Chuyến đi, nhóm, lộ trình (`src/trips/`, `src/planner/`)
- `src/planner/` là thuật toán thuần TypeScript (không NestJS, không database): Greedy theo tỉ lệ giá trị/chi phí (điểm miễn phí trước) → Local search (2-opt từng ngày, đổi điểm 1-swap, Greedy lấp chỗ trống) → Fairness check (đổi phần tham gia của từng người, thêm điểm cho người kém hài lòng). Ngân sách là giới hạn cứng. Chuyến một mình là nhóm một người. `exact.ts` là lời giải tối ưu bằng vét cạn (≤ 12 điểm) chỉ dùng để đánh giá. Mô tả đầy đủ: [docs/algorithm.md](../docs/algorithm.md). Test bằng `npm test`, dữ liệu mẫu trong `planner.fixtures.ts`.
- `src/trips/` nối thuật toán với dữ liệu: `TripPlacesService` đọc điểm (danh mục hoặc Google, kèm giá, giờ mở cửa theo ngày), `LodgingService` gợi ý 3 khách sạn thật từ Google (loại `lodging`, giá ước tính theo price level tới khi có Agoda), `PlanningService` lập lộ trình rồi tính lại giờ bằng Google Routes, `ItineraryService` trả lộ trình kèm tổng dồn, số tiền còn lại và cảnh báo, `VotesService` lo bình chọn.
- Ứng viên khi lập lộ trình: các điểm đã chọn hoặc bình chọn (`vote_options`); chưa có thì tìm điểm nổi tiếng và quán ăn của thành phố trên Google (2 lượt Text Search), không có key thì lấy danh mục mẫu cùng thành phố.
- Mọi thay đổi đầu vào (thành viên, ngân sách, sở thích, phiếu, chỗ ở) gọi `TripAccess.resetPlan`: xóa lộ trình, trip về DRAFT, phải lập lại.
- Ngân sách của người khác không bao giờ trả về; cả nhóm chỉ thấy phần chi (`spent`). Cảnh báo `MEMBER_OVER_BUDGET` không nói là ai.

## Liên hệ khẩn cấp, nhận diện ảnh (`src/emergency-contacts/`, `src/recognition/`)
- `GET/POST/PATCH/DELETE /me/emergency-contacts`: tối đa 5 người, SĐT lưu E.164 (số Việt Nam gõ kiểu `0905…` được, số nước ngoài cần `+mã nước`). App Android tự gửi SMS SOS; server chỉ giữ danh sách.
- `POST /places/recognize` (multipart, field `image`, JPEG/PNG/WebP, tối đa 4 MB, 10 lần/phút): Cloud Vision `LANDMARK_DETECTION` qua `VisionClient`, rồi tìm địa danh trên Google Places quanh tọa độ Vision trả về. Kiểu ảnh kiểm tra bằng byte đầu file, không tin `Content-Type`. Không lưu ảnh. Không có key thì trả 503 `RECOGNITION_UNAVAILABLE`; e2e test dùng `FakeVisionClient`.
- Một key Google Cloud (`GOOGLE_MAPS_API_KEY`) dùng chung cho Places, Routes và Vision; giới hạn key chỉ cho ba API này.

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
