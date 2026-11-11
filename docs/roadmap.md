# Lộ trình và tiến độ

Nơi duy nhất theo dõi kế hoạch của TripFair. Tài liệu kỹ thuật: [backend-guide.md](backend-guide.md) (xây backend từng bước), [database.md](database.md), [algorithm.md](algorithm.md), [deploy.md](deploy.md), [google-cloud.md](google-cloud.md). Mỗi PR cập nhật trạng thái bước của nó; kế hoạch thay đổi thì sửa file này trước rồi mới làm.

Cập nhật lần cuối: 2026-11-07 (xong A10, A11).

Trạng thái: ✅ Xong · 🔄 Đang làm · ⏳ Chưa làm · ⛔ Bị chặn (chờ quyết định hoặc tài khoản)

## Tổng quan

| Mảng | Trạng thái | Đang ở đâu |
| --- | --- | --- |
| Thiết kế UI (Figma) | ✅ | Đã rà soát và bổ sung các màn còn thiếu; còn vài mục "nên có" |
| Android | 🔄 | Đủ màn hình theo Figma với dữ liệu mẫu; logic form tài khoản ở PR #7; chưa nối backend |
| Backend | 🔄 | A1–A11 xong (dữ liệu thật từ Google Places, Routes, Vision); còn deploy lên AWS (B3, B4) |
| Hạ tầng AWS | 🔄 | B2 (Docker) xong; B3, B4 đã tạo tài nguyên (RDS, EC2 đang tắt), chờ deploy |

## Bước tiếp theo

Dữ liệu người dùng phải nằm trên cloud chứ không trên máy phát triển, nên B1–B4 được làm trước A8. Database trên máy chỉ còn dùng để phát triển và chạy test.

Thứ tự làm hết phần backend, theo cách tốn ít tiền AWS nhất: viết và test toàn bộ logic trên máy trước, rồi mới bật EC2/RDS một lần để deploy và kiểm tra, xong thì tắt ngay.

1. ~~**A8, A9:** lộ trình cá nhân, lưu trú, nhóm, bình chọn, chia chi phí.~~ Xong 2026-11-06.
2. ~~**A7:** tìm kiếm toàn cầu lấy Google Places làm nguồn chính, gợi ý khi gõ bằng Autocomplete.~~ Xong 2026-11-06.
3. ~~**A10:** liên hệ khẩn cấp, nhận diện ảnh bằng Cloud Vision.~~ Xong 2026-11-07.
4. ~~**A11:** chương trình đánh giá thuật toán.~~ Xong 2026-11-07, kết quả ở [algorithm.md](algorithm.md).
5. **B3, B4:** deploy lên RDS và EC2, kiểm tra HTTPS, rồi tắt máy.
6. **Android AN5:** nối backend trên EC2.

## Quyết định tạm chốt (chờ xác nhận)

Các câu hỏi mở dưới đây đang chặn việc làm backend, nên được tạm chốt theo phương án ít rủi ro nhất. Mỗi phương án tách thành cấu hình hoặc module riêng để đổi lại dễ.

| Câu hỏi | Tạm chốt | Đổi lại ở đâu |
| --- | --- | --- |
| [Trip] Lộ trình cá nhân dùng chung pipeline với nhóm? | Dùng chung: chuyến một mình là nhóm có 1 người | `src/planner/` |
| [Trip] "Thời gian giữa các lần đi chơi" là gì? | Khoảng nghỉ tối thiểu giữa hai điểm, mặc định 30 phút, mỗi chuyến tự chỉnh (`gapMinutes`) | Cột `trips.gap_minutes` |
| [Trip] Thời gian, quãng đường giữa các điểm | Thuật toán dùng ước tính đường chim bay × 1,4 với 25 km/h (chạy nhanh, không tốn phí); lộ trình đã lập được tính lại giờ bằng Google Routes `computeRoutes`, một request mỗi ngày (~5 USD/1000 request), điểm cuối ngày bị bỏ nếu không còn kịp | `src/planner/travel.ts`, `PlanningService.fitDays` |
| Tìm kiếm toàn cầu dùng API nào? | Google Places (New) là nguồn chính (dữ liệu thật, toàn thế giới); danh mục mẫu chỉ dùng khi không có key (phát triển, test). Thay quyết định 2026-11-04 "tìm kiếm bổ sung kết quả Google" trong CLAUDE.md | `PlacesService.search` |
| Lưu trú khi chưa có Agoda API | Khách sạn thật từ Google Places (loại `lodging`); giá một đêm ước tính theo `priceLevel` (lấy mức cao), nguồn ESTIMATED, hiện dấu ~ | `src/trips/lodging-shares.ts` |
| Ứng viên khi chưa chọn điểm nào | Tìm "điểm tham quan nổi tiếng" và "quán ăn ngon nổi tiếng" của thành phố trên Google (2 lượt Text Search, đã gồm giá và giờ mở cửa) | `TripPlacesService.discover` |
| Phòng khách sạn | 2 người một phòng, xếp theo thứ tự vào nhóm; khách sạn chỉ được gợi ý khi phần tiền phòng của mọi người đều trong ngân sách | `GUESTS_PER_ROOM` |
| A11: ILP bằng OR-Tools cần Python | Dùng lời giải tối ưu bằng tìm kiếm vét cạn trên bộ dữ liệu nhỏ (cùng kết quả với ILP), viết bằng TypeScript, không thêm Python | `src/planner/exact.ts` |
| Production chưa có SMTP | API vẫn khởi động; đăng ký bằng email báo `CODE_DELIVERY_FAILED` cho tới khi có SMTP, giống SMS. Mã OTP không bao giờ in ra log ở production | `DefaultOtpDelivery` |

## Việc đang chờ

| Việc | Cần cho | Ghi chú |
| --- | --- | --- |
| Gmail App Password (`SMTP_*`) trong `backend/.env` và SSM | Gửi OTP qua email thật | Hướng dẫn ở [google-cloud.md](google-cloud.md), mục 5. Chưa có thì trên máy mã in ra log, trên AWS đăng ký bằng email báo `CODE_DELIVERY_FAILED` |
| Google Cloud API key (`GOOGLE_MAPS_API_KEY`) | Dữ liệu thật: tìm kiếm, lộ trình, khách sạn, nhận diện ảnh | Hướng dẫn ở [google-cloud.md](google-cloud.md). Không có key thì trên máy chạy bằng danh mục mẫu, trên AWS tìm kiếm trả rỗng |
| Đăng nhập lại AWS CLI (`aws login`) | Deploy B3, B4 | Phiên đăng nhập hết hạn; các bước còn lại ở [deploy.md](deploy.md), mục 10 |
| Review và merge PR #7 | Android AN3 | Khi merge sẽ phải gộp tay một dòng trong "Quyết định đã chốt" của CLAUDE.md |
| Quyết định chạy `npm audit fix` | Bảo mật backend | 4 lỗ hổng (3 high) trong body-parser, multer, fast-uri; sửa được trong phạm vi phiên bản hiện tại |

## Thiết kế UI

| Bước | Mục tiêu | Trạng thái | Xong ngày |
| --- | --- | --- | --- |
| Rà soát Figma | Sửa từng mục theo mức cần thiết, tạo các màn còn thiếu (lưu trú, đang tối ưu, offline, SOS, nhận diện…) | ✅ | 2026-10-18 |
| Mục "nên có" còn lại | Status bar iOS ở màn xác thực, icon thừa, màn Tài khoản & bảo mật, design token, kiểm tra màn 360dp, ảnh khách sạn thật | ⏳ | |

## Android

Các bước AN4–AN6 là đề xuất, chỉnh lại khi tới.

| Bước | Mục tiêu | Trạng thái | PR | Xong ngày |
| --- | --- | --- | --- | --- |
| AN1 Khung app | Compose, theme sáng/tối theo Figma, font Inter | ✅ | #6 | 2026-11-04 |
| AN2 Màn hình theo Figma | Navigation 3, mọi màn với dữ liệu mẫu | ✅ | #6 | 2026-11-04 |
| AN3 Logic form tài khoản | ViewModel + repository mẫu, kiểm tra input, đếm ngược gửi lại mã | 🔄 chờ review | #7 | |
| AN4 Logic các màn còn lại | ViewModel cho tìm kiếm, nhóm, bình chọn, lộ trình với dữ liệu mẫu | ⏳ | | |
| AN5 Nối backend | Retrofit thay repository mẫu: tài khoản, tìm kiếm, lịch sử, yêu thích trước; lộ trình, nhóm sau khi backend xong | ⏳ | | |
| AN6 Tính năng thiết bị | Google Maps SDK, vị trí cho SOS, camera thật, lưu lộ trình offline | ⛔ [Camera], [Offline] | | |

## Backend: giai đoạn A (chạy trên máy)

| Bước | Mục tiêu | Trạng thái | PR | Xong ngày |
| --- | --- | --- | --- | --- |
| A1 Công cụ | Node.js 24, Docker Desktop, WSL | ✅ | | 2026-10-18 |
| A2 Khung NestJS | NestJS 12 (ESM), Vitest, oxlint | ✅ | #4 | 2026-10-18 |
| A3 MySQL + Prisma | MySQL 8.4 qua Docker, Prisma 7, `/health` | ✅ | #5 | 2026-10-19 |
| A4 Thiết kế database | ERD toàn app ([database.md](database.md)), bảng `users`, shadow DB | ✅ | #8 | 2026-11-04 |
| A5 Nền tảng API + CI | `/api/v1`, dạng lỗi chung, kiểm tra input, Swagger, e2e trên DB riêng, GitHub Actions | ✅ | #9 | 2026-11-04 |
| A6 Tài khoản | Đăng ký + OTP, đăng nhập JWT + refresh token, quên mật khẩu, `/me` | ✅ | #10 | 2026-11-04 |
| A7 Địa điểm và giá | Tìm kiếm Google Places toàn cầu, gợi ý Autocomplete, chi tiết có giá, lịch sử tìm kiếm | ✅ | #11 | 2026-11-06 |
| A8 Lộ trình cá nhân và lưu trú | Tạo chuyến, gợi ý 3 khách sạn, Greedy + Local search, chi phí từng điểm, ngân sách 0đ, tự thiết kế lộ trình | ✅ | | 2026-11-06 |
| A9 Nhóm, bình chọn, chia chi phí | Mã mời, ngân sách ẩn, bình chọn, Fairness check, chia tiền phòng | ✅ | | 2026-11-06 |
| A10 Yêu thích, SOS, nhận diện ảnh | Yêu thích; liên hệ khẩn cấp; Cloud Vision nhận diện địa danh | ✅ | #11 | 2026-11-07 |
| A11 Đánh giá thuật toán | So Greedy, Greedy + Local search + Fairness, lời giải tối ưu (thay ILP OR-Tools) | ✅ | | 2026-11-07 |

Việc còn lại của các bước đang làm:

- **A6:** gửi email thật cần Gmail App Password; SMS chưa có nhà cung cấp ([User]), hiện chỉ in log.
- **A7:** giá vé đối tác thật (Klook, GetYourGuide, Tiqets) chờ điều kiện Partner API ([Rủi ro]); điểm từ Google dùng chuỗi giá priceRange → loại miễn phí → price_level → mặc định.
- **A8, A9:** giá phòng thật từ Agoda chờ Partner API; hiện ước tính theo price level của Google. Chưa thử với key Google thật (chờ key).
- **A10:** chưa thử Cloud Vision với key thật (chờ key). Camera thật trên Android vẫn chờ [Camera].

## Hạ tầng: giai đoạn B (AWS)

| Bước | Mục tiêu | Trạng thái |
| --- | --- | --- |
| B1 Tài khoản AWS | MFA cho root, IAM quyền tối thiểu, cảnh báo chi phí, region Singapore | 🔄 có tài khoản, IAM user, region Singapore; MFA root và cảnh báo chi phí do chủ tài khoản kiểm tra |
| B2 Đóng gói Docker | Dockerfile multi-stage (image chạy API và image chạy migration), chạy thử trên máy, CI build cả hai image | ✅ |
| B3 RDS MySQL | Subnet riêng, chỉ nhận kết nối từ EC2, TLS, backup tự động | 🔄 đã tạo, đang tắt; còn tạo user ứng dụng và chạy migration |
| B4 EC2 chạy backend | Docker, IAM role, secrets trong SSM Parameter Store, HTTPS | 🔄 đã tạo, đang tắt; còn deploy và kiểm tra HTTPS |
| B5 S3 cho ảnh | Bucket private, upload bằng pre-signed URL | ⏳ |
| B6 Tự động deploy | Merge vào main thì build image, deploy lên EC2, kiểm tra `/health` | ⏳ |
| B7 Vận hành | Log trên CloudWatch, cảnh báo khi `/health` lỗi, kiểm soát chi phí | ⏳ |

## Câu hỏi mở đang chặn

Nội dung đầy đủ ở mục "Câu hỏi mở" của [CLAUDE.md](../CLAUDE.md).

| Câu hỏi | Chặn bước |
| --- | --- |
| [Trip] Lộ trình cá nhân dùng chung pipeline với nhóm không; "thời gian giữa các lần đi chơi" nghĩa là gì | Tạm chốt, chờ xác nhận |
| [Trip] Khoảng cách giữa các điểm: đường chim bay hay Google Routes API | Tạm chốt, chờ xác nhận |
| [User] Dịch vụ gửi OTP qua SMS | SMS thật (A6) |
| [Rủi ro] Điều kiện cấp Partner API của Klook, GetYourGuide, Tiqets, Agoda | Giá đối tác thật (A7, A8) |
| [Camera] Có dùng CameraX không | AN6 |
| [Offline] Phạm vi giao diện offline | AN6 |

## Nhật ký

- **2026-10-17:** Chốt tên TripFair, tài liệu mô tả và CLAUDE.md (#1, #2).
- **2026-10-18:** Rà soát Figma; chốt quy tắc giá và lưu trú (#3); khung backend NestJS (#4).
- **2026-10-19:** Backend kết nối MySQL bằng Prisma, có `/health` (#5).
- **2026-10-21:** Android có đủ màn hình theo Figma (#6) và logic form tài khoản (#7).
- **2026-11-04:** Backend xong A4–A6, tìm kiếm, giá, lịch sử và yêu thích (#8–#11); CI chạy cho mọi PR backend.
- **2026-11-05:** Thêm file lộ trình này (#12). Đưa B1–B4 lên trước A8 để dữ liệu người dùng nằm trên AWS. B2: image Docker cho API và migration, chạy thử được với MySQL trên máy.
- **2026-11-06:** Chuyển sang dữ liệu thật: tìm kiếm, gợi ý, chi tiết lấy từ Google Places; danh mục mẫu chỉ còn cho phát triển và test, không nạp vào production. Xong A8, A9: tạo chuyến, nhóm và mã mời, bình chọn, gợi ý 3 khách sạn thật từ Google, lập lộ trình (Greedy → 2-opt → Fairness) rồi tính lại giờ bằng Google Routes, tự thiết kế lộ trình kèm cảnh báo.
- **2026-11-07:** Xong A10: liên hệ khẩn cấp (tối đa 5, SĐT E.164) và nhận diện địa danh từ ảnh bằng Cloud Vision, khớp với địa điểm thật trên Google Places; ảnh không được lưu.
- **2026-11-07:** Xong A11: lời giải tối ưu bằng vét cạn và chương trình đánh giá (`npm run bench:planner`). Đánh giá cho thấy Local search và Fairness ban đầu không cải thiện gì so với Greedy, nên thêm đổi điểm (1-swap) và đổi phần tham gia; bản đầy đủ đạt 97–99% tối ưu trong dưới 2 ms ([algorithm.md](algorithm.md)).
