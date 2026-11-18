# Lộ trình và tiến độ

Nơi duy nhất theo dõi kế hoạch của TripFair. Tài liệu kỹ thuật: [backend-guide.md](backend-guide.md) (xây backend từng bước), [database.md](database.md), [algorithm.md](algorithm.md), [deploy.md](deploy.md), [google-cloud.md](google-cloud.md). Mỗi PR cập nhật trạng thái bước của nó; kế hoạch thay đổi thì sửa file này trước rồi mới làm.

Cập nhật lần cuối: 2026-11-07 (xong A10, A11).

Trạng thái: ✅ Xong · 🔄 Đang làm · ⏳ Chưa làm · ⛔ Bị chặn (chờ quyết định hoặc tài khoản)

## Tổng quan

| Mảng | Trạng thái | Đang ở đâu |
| --- | --- | --- |
| Thiết kế UI (Figma) | ✅ | Đã rà soát và bổ sung các màn còn thiếu; còn vài mục "nên có" |
| Android | 🔄 | Đủ màn hình theo Figma với dữ liệu mẫu; logic form tài khoản ở PR #7; chưa nối backend |
| Backend | 🔄 | A1–A11 xong (dữ liệu thật từ Google Places, Routes, Vision); A12 gần xong; chưa deploy |
| Hạ tầng AWS | 🔄 | B2 (Docker) xong; chuyển sang tài khoản AWS mới, tài nguyên trên tài khoản cũ đã xóa hết; B1–B4 làm lại |

## Bước tiếp theo

Dữ liệu người dùng phải nằm trên cloud chứ không trên máy phát triển, nên B1–B4 được làm trước A8. Database trên máy chỉ còn dùng để phát triển và chạy test.

Thứ tự làm hết phần backend, theo cách tốn ít tiền AWS nhất: viết và test toàn bộ logic trên máy trước, rồi mới bật EC2/RDS một lần để deploy và kiểm tra, xong thì tắt ngay.

1. ~~**A8, A9:** lộ trình cá nhân, lưu trú, nhóm, bình chọn, chia chi phí.~~ Xong 2026-11-06.
2. ~~**A7:** tìm kiếm toàn cầu lấy Google Places làm nguồn chính, gợi ý khi gõ bằng Autocomplete.~~ Xong 2026-11-06.
3. ~~**A10:** liên hệ khẩn cấp, nhận diện ảnh bằng Cloud Vision.~~ Xong 2026-11-07.
4. ~~**A11:** chương trình đánh giá thuật toán.~~ Xong 2026-11-07, kết quả ở [algorithm.md](algorithm.md).
5. **B1–B4 trên tài khoản AWS mới:** bạn tự dựng theo [deploy.md](deploy.md) mục 3–10, deploy, kiểm tra HTTPS, rồi tắt máy.
6. **A12:** phần còn lại (xem bảng giai đoạn A).
7. **Android AN5:** nối backend trên EC2.

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
| Phòng khách sạn | Nhóm chọn 1–4 người một phòng (mặc định 2), xếp theo thứ tự vào nhóm; khách sạn chỉ được gợi ý khi phần tiền phòng của mọi người đều trong ngân sách | `GUESTS_PER_ROOM` |
| Region AWS | Sydney (`ap-southeast-2`), vì tài khoản AWS mới được gắn với region này; Singapore nhanh hơn khoảng 100 ms mỗi request từ Việt Nam. Script đọc region từ cấu hình, không ghi cứng | `aws configure set region`, `$region` trong [deploy.md](deploy.md) |
| A11: ILP bằng OR-Tools cần Python | Dùng lời giải tối ưu bằng tìm kiếm vét cạn trên bộ dữ liệu nhỏ (cùng kết quả với ILP), viết bằng TypeScript, không thêm Python | `src/planner/exact.ts` |
| Production chưa có SMTP | API vẫn khởi động; đăng ký bằng email báo `CODE_DELIVERY_FAILED` cho tới khi có SMTP, giống SMS. Mã OTP không bao giờ in ra log ở production | `DefaultOtpDelivery` |

## Việc đang chờ

| Việc | Cần cho | Ghi chú |
| --- | --- | --- |
| Gmail App Password (`SMTP_*`) trong `backend/.env` và SSM | Gửi OTP qua email thật | Hướng dẫn ở [google-cloud.md](google-cloud.md), mục 5. Chưa có thì trên máy mã in ra log, trên AWS đăng ký bằng email báo `CODE_DELIVERY_FAILED` |
| Google Cloud API key (`GOOGLE_MAPS_API_KEY`) | Dữ liệu thật: tìm kiếm, lộ trình, khách sạn, nhận diện ảnh | Hướng dẫn ở [google-cloud.md](google-cloud.md). Không có key thì trên máy chạy bằng danh mục mẫu, trên AWS tìm kiếm trả rỗng |
| OAuth client ID của Google (`GOOGLE_CLIENT_ID` trong `backend/.env`, và client Android theo package + SHA-1) | Đăng nhập bằng Google (A13, AN7) | Hướng dẫn ở [google-cloud.md](google-cloud.md), mục 6. Chưa có thì `POST /auth/google` trả 503 `GOOGLE_SIGN_IN_UNAVAILABLE` |
| Tài khoản AWS mới | B1–B4 | Bật MFA cho root, tạo IAM user, đặt cảnh báo chi phí, rồi dựng hạ tầng theo [deploy.md](deploy.md) |
| Review và merge PR #7 | Android AN3 | Khi merge sẽ phải gộp tay một dòng trong "Quyết định đã chốt" của CLAUDE.md |

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
| AN3 Logic form tài khoản | ViewModel + repository mẫu, kiểm tra input, đếm ngược gửi lại mã | ✅ | #7, #13 | 2026-11-11 |
| AN4 Logic các màn còn lại | ViewModel cho tìm kiếm, nhóm, bình chọn, lộ trình | ➡️ gộp vào AN5: ViewModel viết thẳng trên dữ liệu backend, không qua bản mẫu | | |
| AN5 Nối backend | Retrofit thay repository mẫu: tài khoản, tìm kiếm, lịch sử, yêu thích trước; lộ trình, nhóm sau khi backend xong | ✅ code xong mọi mảng (tài khoản, địa điểm, liên hệ khẩn cấp, chuyến đi, chỗ ở, lộ trình kể cả thêm điểm khi tự thiết kế, nhóm, bình chọn, xóa/rời chuyến), 78 unit test đạt, lint không lỗi, đã chạy thử trên máy ảo Android 17 với backend trên máy. Còn lại: backend của chuyến đi nằm ở nhánh `feat/backend-readiness`, chưa merge; đổi email/số điện thoại bằng OTP (cần đổi schema); ảnh địa điểm (cần thư viện tải ảnh) | #13 | 2026-11-11 |
| AN6 Tính năng thiết bị | Google Maps SDK, vị trí cho SOS, camera thật, lưu lộ trình offline | 🔄 bản đồ Google ở kết quả tìm kiếm, chi tiết địa điểm, lộ trình và SOS; tìm kiếm theo vị trí của người dùng. SOS gửi vị trí thật qua ứng dụng tin nhắn; nhận diện ảnh chụp bằng camera của máy. Đã kiểm tra với khóa Google thật trên máy ảo: tìm kiếm và gợi ý toàn quốc, bản đồ, khách sạn, lập lộ trình cho thành phố ngoài danh mục mẫu. Còn lại: thử nhận diện với ảnh địa danh thật (Cloud Vision đã trả lời nhưng chưa có ca nhận ra); khung ngắm trong app ⛔ [Camera]; lưu lộ trình offline ⛔ [Offline] | #13 | |

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
| A13 Đăng nhập bằng Google | `POST /auth/google` nhận Google ID token và tự xác minh; cột `google_sub`, `password_hash` cho phép trống; gộp tài khoản theo email; tài khoản chỉ dùng Google xóa tài khoản bằng cách đăng nhập Google lại | ✅ backend; Android thuộc AN7 | | 2026-11-12 |
| A12 Chuẩn bị đưa vào sử dụng | Xóa tài khoản (yêu cầu của Google Play); chủ nhóm mời ra, chuyển quyền, đổi mã mời; quán ăn đúng giờ ăn, thời gian ghé theo loại điểm; giờ mở cửa theo giờ địa phương; chọn số người mỗi phòng; giới hạn route tốn phí Google; vá lỗ hổng dependency; bản nháp chính sách quyền riêng tư | 🔄 | | |

Việc còn lại của A12: đổi email/số điện thoại bằng OTP (cần thêm một loại mã vào schema, chờ bạn đồng ý); cho thử lại refresh token vừa đổi khi app mất phản hồi (cần cột `replaced_by_id`, chờ bạn đồng ý đổi schema); chính sách quyền riêng tư cần bạn xem lại, điền thông tin liên hệ và đăng lên web; giá ở nước ngoài vẫn dùng bảng quy đổi VND của Việt Nam (chờ quyết định: giới hạn ở Việt Nam hay thêm tỉ giá và bảng giá theo nước).

Việc còn lại của các bước đang làm:

- **A6:** gửi email thật cần Gmail App Password; SMS chưa có nhà cung cấp ([User]), hiện chỉ in log.
- **A7:** giá vé đối tác thật (Klook, GetYourGuide, Tiqets) chờ điều kiện Partner API ([Rủi ro]); điểm từ Google dùng chuỗi giá priceRange → loại miễn phí → price_level → mặc định.
- **A8, A9:** giá phòng thật từ Agoda chờ Partner API; hiện ước tính theo price level của Google. Chưa thử với key Google thật (chờ key).
- **A10:** chưa thử Cloud Vision với key thật (chờ key). Camera thật trên Android vẫn chờ [Camera].

## Hạ tầng: giai đoạn B (AWS)

| Bước | Mục tiêu | Trạng thái |
| --- | --- | --- |
| B1 Tài khoản AWS | MFA cho root, IAM quyền tối thiểu, cảnh báo chi phí, region Sydney (`ap-southeast-2`) | ⏳ chuyển sang tài khoản mới: bật MFA root, IAM user, cảnh báo chi phí |
| B2 Đóng gói Docker | Dockerfile multi-stage (image chạy API và image chạy migration), chạy thử trên máy, CI build cả hai image | ✅ |
| B3 RDS MySQL | Subnet riêng, chỉ nhận kết nối từ EC2, TLS, backup tự động | ✅ 2026-11-12 |
| B4 EC2 chạy backend | Docker, IAM role, secrets trong SSM Parameter Store, HTTPS | ✅ 2026-11-12: chạy bản `f9acf14` ở `https://52-65-234-138.sslip.io`, có key Google và SMTP; chỉ bật khi cần |
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
- **2026-11-07:** A12 (chuẩn bị đưa vào sử dụng): xóa tài khoản, quản lý nhóm, quán ăn đúng giờ ăn, thời gian ghé theo loại điểm, giờ mở cửa theo giờ địa phương, chọn số người mỗi phòng, giới hạn route tốn phí Google, `npm audit fix` (0 lỗ hổng), bản nháp chính sách quyền riêng tư.
- **2026-11-07:** Chuyển sang tài khoản AWS mới. Trên tài khoản cũ đã xóa toàn bộ tài nguyên TripFair (EC2, Elastic IP, RDS và sao lưu, ECR, SSM, IAM role, security group, subnet), không còn phí; chi phí từ đầu tháng của TripFair khoảng 0,8 USD, đã được tín dụng trừ. Cách gỡ ghi ở [deploy.md](deploy.md) mục 13.
- **2026-11-08:** Bắt đầu AN5 trên nhánh `feat/android-backend`, xây chồng lên PR #7: lớp mạng Retrofit/OkHttp, đọc lỗi theo `code` của backend, địa chỉ backend cấu hình trong `local.properties`.
- **2026-11-10:** AN5: app Android gọi backend cho mọi mảng. Phiên đăng nhập lưu mã hóa và tự làm mới token; liên hệ khẩn cấp có bản sao trên máy để SOS dùng khi mất mạng; chuyến đi một mình và nhóm đi trọn luồng tạo → chỗ ở → lập lộ trình → xem và sửa lộ trình; nhóm có mã mời, ngân sách riêng tư và bình chọn. Sơ đồ ngày vẽ theo tọa độ điểm, chưa phải bản đồ thật (Google Maps SDK thuộc AN6).
- **2026-11-10:** Chạy thử app trên máy ảo Android 17 với backend trên máy: đăng nhập, tìm kiếm, tạo chuyến và lập lộ trình chạy trọn luồng. Sửa hai lỗi lộ ra khi chạy thật (app không gọi được `10.0.2.2` trên Android 17; hộp chọn ngày hiện tiếng Anh). Bắt đầu AN6: thêm Google Maps SDK và vị trí.
- **2026-11-10:** AN6: SOS đọc vị trí thật và mở ứng dụng tin nhắn với tin soạn sẵn (Android 17 không cho app gửi SMS trực tiếp); nhận diện ảnh qua camera của máy; thêm địa điểm khi tự thiết kế lộ trình; xóa liên hệ, rời nhóm, xóa chuyến đi.
- **2026-11-11:** Có khóa Google (server và Android): app tìm được địa điểm thật trên cả nước, bản đồ hiện đường phố, chuyến Hà Nội lập được lộ trình với khách sạn và điểm tham quan từ Google.
- **2026-11-11:** Thêm CI cho Android: unit test, lint và build debug cho mọi PR chạm `android/`.
- **2026-11-11:** Merge vào main: backend (#14 Docker và deploy, #15 chuyến đi và lộ trình, #16 chuẩn bị đưa vào sử dụng) và app Android nối backend (#13, gồm cả #7).
- **2026-11-11:** Sửa lỗi sau khi rà soát code (A12, AN6). Backend: lập lộ trình theo giờ mở cửa của từng ngày trong chuyến; Google lỗi khi lập hoặc thiết kế lộ trình thì báo `GOOGLE_PLACES_UNAVAILABLE` thay vì lặng lẽ bỏ điểm; tìm theo nhóm Vui chơi/Cảnh đẹp đọc thêm tối đa 2 trang khi trang đầu không có điểm hợp; chi tiết địa điểm cho chuyến đi dùng bộ trường rẻ hơn; tính lại giờ mỗi ngày chỉ gọi Routes một lần; ngân sách 0đ luôn "Tự lo chỗ ở" cả cho nhóm; cảnh báo ngày quá dài tính cả đường về khách sạn. Android: SOS chỉ dùng vị trí đo trong 2 phút gần nhất, cũ hơn thì đo lại với độ chính xác cao; màn chỗ ở chọn sẵn khách sạn đã chọn cho chuyến. Còn lại: cho thử lại refresh token vừa đổi khi mất phản hồi, cần thêm cột `replaced_by_id` vào `refresh_tokens` (chờ đồng ý đổi schema).
- **2026-11-11:** A7: tab Ăn uống/Vui chơi/Cảnh đẹp ở kết quả tìm kiếm không còn trống khi tìm theo tên một địa điểm. Trước đây tab chỉ lọc kết quả của đúng tên đó (tìm "Cầu Rồng" rồi chọn Ăn uống thì Google chỉ trả cây cầu, bị lọc hết); nay khi lọc không còn gì, backend tìm tiếp địa điểm cùng loại quanh nơi đó ("quán ăn gần Cầu Rồng"). Token trang sau của lượt tìm này mang tiền tố `near:`.
- **2026-11-12:** A13: backend đăng nhập bằng Google. `POST /auth/google` kiểm tra chữ ký ID token bằng khóa công khai của Google (không thêm thư viện), đúng ứng dụng (`GOOGLE_CLIENT_ID`), còn hạn và email đã được Google xác minh. Lần đầu thì tạo tài khoản không mật khẩu hoặc gộp vào tài khoản cùng email; lượt đăng ký chưa xác minh OTP bị thay thế và mất mật khẩu cũ, để người đăng ký trước bằng email của người khác không vào được tài khoản. `GET /me` trả thêm `hasPassword`, `googleLinked`; `DELETE /me` nhận `password` hoặc `googleIdToken`. Phần Android (nút đăng nhập Google, Credential Manager) làm ở AN7.
- **2026-11-12:** B3, B4: backend `f9acf14` chạy trên AWS (EC2 + RDS ở Sydney) với key Google và gửi OTP qua Gmail SMTP; migration `google_sign_in` đã áp dụng trên RDS. Thêm [setup.md](setup.md): thành viên trong nhóm chạy app bằng Android Studio với backend dùng chung trên AWS, không cần chạy backend trên máy.
