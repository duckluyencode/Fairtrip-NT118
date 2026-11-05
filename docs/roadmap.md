# Lộ trình và tiến độ

Nơi duy nhất theo dõi kế hoạch của TripFair. Mỗi PR cập nhật trạng thái bước của nó; kế hoạch thay đổi thì sửa file này trước rồi mới làm.

Cập nhật lần cuối: 2026-11-05 (đưa B1–B4 lên trước A8).

Trạng thái: ✅ Xong · 🔄 Đang làm · ⏳ Chưa làm · ⛔ Bị chặn (chờ quyết định hoặc tài khoản)

## Tổng quan

| Mảng | Trạng thái | Đang ở đâu |
| --- | --- | --- |
| Thiết kế UI (Figma) | ✅ | Đã rà soát và bổ sung các màn còn thiếu; còn vài mục "nên có" |
| Android | 🔄 | Đủ màn hình theo Figma với dữ liệu mẫu; logic form tài khoản ở PR #7; chưa nối backend |
| Backend | 🔄 | A1–A6 xong; tìm kiếm, giá, lịch sử, yêu thích xong; tiếp theo A8 (lộ trình) |
| Hạ tầng AWS | 🔄 | B2 (Docker) xong; B1 chờ tài khoản AWS |

## Bước tiếp theo

Dữ liệu người dùng phải nằm trên cloud chứ không trên máy phát triển, nên B1–B4 được làm trước A8. Database trên máy chỉ còn dùng để phát triển và chạy test.

1. **B1–B4: đưa backend lên AWS** (Docker, RDS, EC2, HTTPS). B1 cần tài khoản AWS.
2. **Android AN5: nối backend trên EC2** cho đăng nhập, tìm kiếm, lịch sử và yêu thích. Merge PR #7 trước.
3. **Backend A8: lộ trình cá nhân và lưu trú.** Cần chốt hai câu hỏi [Trip] trước khi viết code.

## Việc đang chờ

| Việc | Cần cho | Ghi chú |
| --- | --- | --- |
| Tài khoản AWS (B1) | B3, B4 | Cần thẻ; bật MFA, tạo user IAM, đặt cảnh báo chi phí |
| Gmail App Password điền vào `backend/.env` (`SMTP_*`) | Gửi OTP qua email thật | Chưa có thì mã OTP in ra log server; trên AWS sẽ chuyển vào SSM Parameter Store |
| Chọn API tìm kiếm toàn cầu (Google Places hay API dựa trên OpenStreetMap) | A7 | Danh mục mẫu chỉ dùng cho phát triển và test, không đủ cho người dùng thật |
| Google Maps API key (`GOOGLE_MAPS_API_KEY`) | Kết quả Google trong tìm kiếm | Cần bật thanh toán, giới hạn key cho Places API (New), đặt quota theo ngày; không có key vẫn chạy bằng danh mục mẫu |
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
| A7 Địa điểm và giá | Tìm kiếm không dấu, gợi ý, chi tiết có giá, lịch sử tìm kiếm, Google Places | 🔄 | #11 | |
| A8 Lộ trình cá nhân và lưu trú | Tạo chuyến, gợi ý 3 khách sạn, Greedy + Local search, chi phí từng điểm, ngân sách 0đ | ⛔ [Trip] | | |
| A9 Nhóm, bình chọn, chia chi phí | Mã mời, ngân sách ẩn, bình chọn, Fairness check, chia tiền phòng | ⏳ | | |
| A10 Yêu thích, SOS, nhận diện ảnh | Yêu thích; liên hệ khẩn cấp; Cloud Vision nhận diện địa danh | 🔄 | #11 | |
| A11 Đánh giá thuật toán | So Greedy, Greedy + Local search + Fairness, ILP (OR-Tools) | ⏳ | | |

Việc còn lại của các bước đang làm:

- **A6:** gửi email thật cần Gmail App Password; SMS chưa có nhà cung cấp ([User]), hiện chỉ in log.
- **A7:** giá vé đối tác thật (Klook, GetYourGuide, Tiqets) chờ điều kiện Partner API ([Rủi ro]), hiện dùng dữ liệu mẫu; bảng `place_interests` làm cùng A8.
- **A10:** yêu thích đã xong; còn liên hệ khẩn cấp và nhận diện ảnh.

## Hạ tầng: giai đoạn B (AWS)

| Bước | Mục tiêu | Trạng thái |
| --- | --- | --- |
| B1 Tài khoản AWS | MFA cho root, IAM quyền tối thiểu, cảnh báo chi phí, region Singapore | ⛔ chờ tài khoản |
| B2 Đóng gói Docker | Dockerfile multi-stage (image chạy API và image chạy migration), chạy thử trên máy, CI build cả hai image | ✅ |
| B3 RDS MySQL | Subnet riêng, chỉ nhận kết nối từ EC2, TLS, backup tự động | ⏳ |
| B4 EC2 chạy backend | Docker, IAM role, secrets trong SSM Parameter Store, HTTPS | ⏳ |
| B5 S3 cho ảnh | Bucket private, upload bằng pre-signed URL | ⏳ |
| B6 Tự động deploy | Merge vào main thì build image, deploy lên EC2, kiểm tra `/health` | ⏳ |
| B7 Vận hành | Log trên CloudWatch, cảnh báo khi `/health` lỗi, kiểm soát chi phí | ⏳ |

## Câu hỏi mở đang chặn

Nội dung đầy đủ ở mục "Câu hỏi mở" của [CLAUDE.md](../CLAUDE.md).

| Câu hỏi | Chặn bước |
| --- | --- |
| [Trip] Lộ trình cá nhân dùng chung pipeline với nhóm không; "thời gian giữa các lần đi chơi" nghĩa là gì | A8 |
| [Trip] Khoảng cách giữa các điểm: đường chim bay hay Google Routes API | A8 |
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
