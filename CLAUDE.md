# TripFair — App lộ trình du lịch

## Tổng quan
Đồ án sinh viên: Android app (Kotlin) gợi ý địa điểm và tự sinh lộ trình du lịch theo ngân sách, thời gian; hỗ trợ lộ trình nhóm chia chi phí công bằng. Backend NestJS deploy trên AWS.
Mô tả chi tiết: [docs/project-overview.md](docs/project-overview.md). Đây là bản tham khảo, chưa chốt. Khi bản mô tả và CLAUDE.md mâu thuẫn, CLAUDE.md được ưu tiên; khi cả hai chưa rõ thì hỏi tôi.
Thiết kế UI: [Figma](https://www.figma.com/design/o0VGZR03n46tcJonTQ8nQ9/NT118.R13_Group12A).

## Tech stack
- Android: Kotlin, Jetpack Compose, Google Maps SDK, Retrofit/OkHttp, DataStore, FusedLocationProviderClient; Room và CameraX chưa dùng (xem Câu hỏi mở)
- Backend: NestJS (Node.js), JWT, Docker
- Database: MySQL trên Amazon RDS
- Dịch vụ ngoài: Google Places API (New), Google Cloud Vision API, Klook/GetYourGuide/Tiqets API (vé), Agoda API (lưu trú)
- Hạ tầng: EC2 (chạy backend container), RDS, S3 (ảnh), Security Groups, GitHub Actions (CI/CD)

## Cấu trúc repo
Monorepo. Thư mục được tạo dần khi thực sự cần, không scaffold trước.
Khi đề xuất tạo thư mục hoặc module mới: giải thích vai trò của nó trước khi tạo.
Khi có `android/` và `backend/`: lệnh build/test và quy ước riêng của từng phần đặt trong `android/CLAUDE.md` và `backend/CLAUDE.md`; file này chỉ giữ phần dùng chung.

```
.github/     GitHub Actions (CI backend, CI Android)
android/     App Android Kotlin + Jetpack Compose, xem android/CLAUDE.md
backend/     API server NestJS (TypeScript), xem backend/CLAUDE.md
docs/        Tài liệu dự án (mô tả tổng quan, thiết kế database, lộ trình và tiến độ)
CLAUDE.md
README.md
```

## Lệnh thường dùng
- Android: xem [android/CLAUDE.md](android/CLAUDE.md).
- Backend: xem [backend/CLAUDE.md](backend/CLAUDE.md).
- Database trên máy: `docker compose up -d` trong `backend/` (MySQL 8.4), chi tiết trong backend/CLAUDE.md.

## Cách làm việc với tôi
- Kế hoạch và tiến độ theo dõi ở [docs/roadmap.md](docs/roadmap.md). Đầu mỗi task, nói rõ task thuộc bước nào trong roadmap. PR của bước nào thì cập nhật luôn trạng thái, ngày xong, việc còn lại và nhật ký của bước đó trong cùng PR. Kế hoạch thay đổi thì sửa roadmap trước rồi mới làm.
- Tôi muốn hiểu mọi thứ được tạo ra. Trước khi viết code cho một task: nêu kế hoạch ngắn (file sẽ tạo/sửa, cách tiếp cận) và chờ tôi đồng ý.
- Làm theo bước nhỏ, mỗi bước là một thay đổi kiểm tra được; sau mỗi bước giải thích ngắn đã làm gì và vì sao.
- Chỉ làm đúng phạm vi task; không tự refactor hoặc thêm tính năng ngoài yêu cầu. Thấy vấn đề ngoài phạm vi thì ghi chú để tôi quyết định.
- Trước khi báo hoàn thành: chạy build/test liên quan và báo kết quả thật, kể cả khi lỗi.
- Hỏi trước khi: thêm dependency lớn, thay đổi schema database, git push, chạy lệnh tác động lên AWS hoặc xóa dữ liệu.
- CLAUDE.md được cập nhật dần. Khi chốt một quyết định, tạo thư mục/module mới hoặc thiết lập lệnh build/test, đề xuất sửa CLAUDE.md ngay trong bước đó và chờ tôi đồng ý: chuyển câu hỏi đã chốt sang "Quyết định đã chốt", cập nhật cây thư mục và lệnh thường dùng.
- Khi task chạm tới một mục trong "Câu hỏi mở", hỏi tôi để chốt trước khi viết code.

## Bảo mật
- Không commit secrets. Backend dùng biến môi trường (.env, kèm .env.example); Android đặt API key trong local.properties, không đưa vào source code. Bảo đảm .gitignore bao các file này.
- Mật khẩu lưu dạng hash; JWT secret lấy từ biến môi trường.
- RDS chỉ nhận kết nối từ EC2 backend; IAM và Security Groups theo least privilege.

## Quy tắc nghiệp vụ
- Mọi địa điểm gợi ý đều có giá trong dữ liệu, kèm price_source (partner_api / google_api / estimated) và tên nhà cung cấp.
  - Vé tham quan: Klook/GetYourGuide/Tiqets API. Lưu trú: Agoda API.
  - Quán ăn và địa điểm còn lại: priceRange (Google Places API New) → ước tính theo price_level bằng bảng quy đổi VND.
  - Người dùng không phải nhập giá. Khi chưa có API key đối tác, dùng dữ liệu mẫu qua cùng lớp nguồn giá.
- Hiển thị giá: danh sách địa điểm (Trang chủ, tìm kiếm, nhận diện) không hiện giá. Giá hiện ở Chi tiết địa điểm, Bình chọn, Lộ trình và Kết quả tối ưu, luôn kèm nguồn; giá không phải giá vé chính thức có dấu ~.
- Budget = 0 vẫn phải sinh được lộ trình: chỉ chọn địa điểm có estimated_cost = 0; xử lý chi phí = 0 khi tính tỉ lệ giá trị/chi phí, không để lỗi chia cho 0.
- Lộ trình trả về chi phí từng điểm, tổng dồn (running total) và số tiền còn lại so với ngân sách.
- Ngân sách gồm vé, ăn uống và lưu trú; chưa tính di chuyển; giao diện ghi rõ điều này.
- Lộ trình nhóm: mỗi thành viên tự nhập ngân sách của mình. Ngân sách tối đa chỉ người đó (và thuật toán) thấy; cả nhóm thấy phần chi của từng người.
- Lưu trú: app gợi ý 3 khách sạn hợp ngân sách (rẻ nhất, cân bằng, gần lộ trình nhất) hoặc "Tự lo chỗ ở". Tiền phòng trừ khỏi ngân sách trước, phần còn lại dành cho các điểm. Trong nhóm, tiền phòng chia đều cho người ở chung phòng. Ngân sách 0đ thì mặc định "Tự lo chỗ ở".
- Chế độ offline: phát hiện mất mạng thì báo cho người dùng, hiển thị lộ trình lưu trong Room, bản đồ dùng cache sẵn của Google Maps SDK. Không có chức năng tải bản đồ; các tính năng cần mạng bị khóa kèm giải thích.
- Lộ trình nhóm: Greedy (theo tỉ lệ giá trị/chi phí, địa điểm miễn phí ưu tiên cao nhất) → Local search (2-opt hoặc Simulated Annealing) → Fairness check (không ai vượt ngân sách cá nhân) → Output kèm chi phí từng điểm và cách chia. Ràng buộc: tổng thời gian ≤ T.
- Đánh giá thuật toán: so sánh Greedy thuần, Greedy + Local search + Fairness, và ILP (OR-Tools) trên instance nhỏ. Metric: tổng mức hài lòng, độ lệch ngân sách, Jain's Fairness Index, thời gian chạy.

## Quyết định đã chốt
- 2026-10-17: Backend dùng NestJS (bản mô tả để ngỏ Express hoặc NestJS).
- 2026-10-17: Tên app là TripFair.
- 2026-10-17: Xác minh khi đăng ký và quên mật khẩu dùng OTP 6 số, gửi qua email hoặc SMS tùy tài khoản.
- 2026-10-17: Giao diện app bằng tiếng Việt.
- 2026-10-17: Backend phát triển và chạy trên máy trước, lên AWS sau.
- 2026-10-17: Giữ lộ trình cá nhân và lộ trình tự thiết kế như bản mô tả.
- 2026-10-17: Ngân sách chưa tính chi phí di chuyển (khó ước tính đúng, làm thuật toán phức tạp); để làm phần mở rộng sau.
- 2026-10-17: Không tải bản đồ offline (Google Maps SDK không hỗ trợ); chế độ offline theo quy tắc nghiệp vụ ở trên.
- 2026-10-18: Bỏ giá crowdsource; người dùng không phải nhập giá.
- 2026-10-18: Thêm lưu trú (khách sạn qua Agoda) vào app và vào ngân sách.
- 2026-10-18: Nguồn giá vé thêm Klook bên cạnh GetYourGuide/Tiqets; dùng dữ liệu mẫu khi chưa được duyệt đối tác.
- 2026-10-18: Giá chỉ hiện ở Chi tiết địa điểm, Bình chọn, Lộ trình và Kết quả tối ưu.
- 2026-10-18: Backend dùng NestJS 12 (ESM) trên Node.js 24 LTS; test bằng Vitest, lint bằng oxlint.
- 2026-10-18: ORM là Prisma 7.10.1 (ghim phiên bản) với adapter MariaDB; database MySQL 8.4 LTS, trên máy chạy bằng Docker Compose (`backend/docker-compose.yml`).
- 2026-10-19: App Android: package com.tripfair.app, minSdk 26, compileSdk/targetSdk 37; AGP 9.4.1 (Kotlin tích hợp, 2.2.10), Gradle 9.6.0, Compose BOM 2026.09.00 (Material 3).
- 2026-10-19: Giao diện có chế độ sáng (theo Figma) và tối (tự thiết kế từ màu Figma, đạt WCAG AA), theo cài đặt của điện thoại; không dùng màu động Material You. Font Inter đóng gói trong app (file biến thiên, giấy phép OFL).
- 2026-10-20: Điều hướng Android dùng Navigation 3 (1.2.0) + kotlinx-serialization. Icon lấy từ Figma (bộ Lucide) chuyển sang VectorDrawable, không dùng thư viện icon. Màn hình làm theo Figma với dữ liệu mẫu trước, nối backend sau.
- 2026-10-21: Android dùng ViewModel (lifecycle 2.11.0) và repository; dữ liệu mẫu qua repository giả lập trong bộ nhớ cho tới khi nối backend; unit test bằng JUnit 4.
- 2026-11-04: Thiết kế database ở [docs/database.md](docs/database.md); bảng được tạo dần theo từng module backend. Tiền lưu bằng số nguyên VND theo người; thuật toán dùng price_max khi điểm có khoảng giá. Đánh giá địa điểm lấy từ Google Places (không có bảng reviews). Liên hệ khẩn cấp lưu trên server, Android giữ bản sao để SOS chạy khi mất mạng.
- 2026-11-04: API backend nằm dưới `/api/v1`; lỗi trả dạng `{ statusCode, code, message, details? }` với `code` ổn định để Android xử lý; tài liệu Swagger ở `/docs`. GitHub Actions chạy lint, build, unit test và e2e test cho mọi PR chạm `backend/`.
- 2026-11-04: OTP email gửi qua SMTP (Gmail App Password khi chạy trên máy; đổi nhà cung cấp, vd. Amazon SES, chỉ cần sửa `.env`). Đăng nhập trả access token JWT 15 phút và refresh token 30 ngày (đổi mới mỗi lần dùng); mật khẩu hash bằng scrypt.
- 2026-11-04: Địa điểm kết hợp danh mục riêng của TripFair (dữ liệu mẫu Đà Nẵng, có thời gian ghé và giá) với Google Places (New): tìm kiếm bổ sung kết quả Google khi có `GOOGLE_MAPS_API_KEY`; điểm từ Google chỉ lưu `place_id` (theo điều khoản Google), chi tiết lấy trực tiếp và ghi nguồn "Google Maps".
- 2026-11-04: Giá điểm không có vé đối tác: `priceRange` của Google → loại điểm thường miễn phí (bãi biển, công viên, chùa, cầu…) = 0đ ước tính → bảng quy đổi theo price_level → mức mặc định theo nhóm. Điểm nằm trong khu có bán vé (vd. Le Jardin trong Bà Nà Hills) dùng giá vé của khu; trong lộ trình tính 0đ nếu vé khu đã có trong lộ trình.
- 2026-11-10: Bản đồ trong app dùng Google Maps SDK qua maps-compose 8.3.1 (bản mới nhất còn build bằng Kotlin 2.3, đọc được bởi Kotlin 2.2.10 của dự án) và vị trí qua play-services-location 21.4.0. Khóa Android đặt trong `android/local.properties` (`MAPS_API_KEY`), khóa server trong `backend/.env` (`GOOGLE_MAPS_API_KEY`); hai khóa tách riêng và giới hạn theo đúng API.
- 2026-11-10: Khi phát triển, app gọi backend trên máy tính qua `http://localhost:3000` cùng `adb reverse tcp:3000 tcp:3000`, vì Android 17 chặn app gọi địa chỉ mạng nội bộ (`10.0.2.2`) khi chưa có quyền mạng cục bộ.
- 2026-11-10: SOS gửi tin bằng cách mở ứng dụng tin nhắn của máy với người nhận và nội dung (tên, liên kết bản đồ, tọa độ) soạn sẵn, không dùng `SmsManager`: Android 17 từ chối app không phải ứng dụng SMS dù đã cấp quyền `SEND_SMS`, và cách này không cần quyền nào.
- 2026-11-10: Nhận diện ảnh chụp bằng ứng dụng camera của máy hoặc chọn bằng bộ chọn ảnh của hệ thống (không cần quyền camera, không thêm CameraX), rồi gửi lên `POST /places/recognize`.

## Câu hỏi mở
Mỗi mục ghi rõ cần chốt trước khi làm phần nào. Chưa tới phần đó thì để nguyên.
- [User] Chọn dịch vụ gửi OTP qua SMS (eSMS/SpeedSMS cần đăng ký brandname và nạp tiền; Firebase Phone Auth cần gói Blaze và đổi luồng Android). Hiện SMS chỉ in ra log. Chốt khi có ngân sách hoặc trước khi chạy thật.
- [Trip] Lộ trình cá nhân dùng chung pipeline với nhóm (n = 1) hay thuật toán riêng? "Thời gian giữa các lần đi chơi" nghĩa là gì?
- [Trip] Thời gian và quãng đường giữa các điểm lấy bằng ước tính đường chim bay hay Google Routes API? Cần cho ràng buộc tổng thời gian ≤ T.
- [Rủi ro] Kiểm tra sớm điều kiện cấp Partner API của Klook, GetYourGuide, Tiqets, Agoda và độ phủ ở Việt Nam. Tất cả đều cần duyệt đối tác; đồ án sinh viên có thể không được duyệt.
- [Camera] Có thêm CameraX (dependency lớn) để có khung ngắm trực tiếp trong app không? Hiện app mở ứng dụng camera của máy để chụp. Chốt khi muốn khung ngắm riêng.
- [Offline] Giao diện offline trong Figma (mất mạng, cần mạng, có mạng lại) đang tạm bỏ qua. Chốt phạm vi trước khi làm lưu lộ trình bằng Room.

## Ngôn ngữ
- Giải thích bằng tiếng Việt. Code, tên biến, comment và commit message bằng tiếng Anh.
- Tên file, tên nhánh và README bằng tiếng Anh; tài liệu mô tả trong docs/ có thể bằng tiếng Việt.
- README và tài liệu trong repo chỉ chứa nội dung dự án, không đưa nội dung trao đổi với Claude vào.
- Commit message không kèm dòng Co-Authored-By.
