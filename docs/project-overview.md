# Mô tả tổng quan đồ án — App lộ trình du lịch (Android)

## 1. Ý tưởng gốc

App Android (native, Kotlin) — bản đồ: người dùng nhập tên các địa điểm muốn đi, app đề xuất quán ăn, địa điểm vui chơi, nơi cảnh đẹp. Sau khi người dùng chọn, kết hợp với ngân sách, thời gian đi chơi bao lâu, thời gian giữa các lần đi chơi, app tự sinh lộ trình phù hợp.

## 2. Tính năng

1. **User** — đăng ký, đăng nhập (hỗ trợ cả email và số điện thoại), quên mật khẩu, quản lý tài khoản
2. **Place** — tìm kiếm, gợi ý địa điểm theo khu vực và loại
3. **Trip/Itinerary** — tạo và tối ưu lộ trình theo ngân sách, thời gian (chi tiết mục 3)
4. **Group Trip** — lập kế hoạch nhóm: mời thành viên, bình chọn địa điểm, chia chi phí công bằng theo ngân sách từng người
5. **Favorite & History** — lưu địa điểm yêu thích, lịch sử tìm kiếm/lộ trình
6. **Review** (tùy chọn) — đánh giá địa điểm/lộ trình sau chuyến đi
7. **Nhận diện ảnh & gợi ý địa điểm tương tự** — chụp ảnh một địa điểm, phân tích ảnh (Google Vision API — Label Detection + Landmark Detection) rồi đề xuất các địa điểm du lịch gần đó
8. **SOS khẩn cấp** — nút gửi vị trí GPS kèm SMS cho người thân đã lưu, nút gọi nhanh tới số khẩn cấp, lưu danh sách liên hệ khẩn cấp (data)

## 3. Ngân sách & tính chi phí

- Người dùng nhập số tiền muốn đi du lịch — kể cả 0 đồng (đi bụi) — app vẫn phải đề xuất ra lộ trình được
- Người dùng cũng có thể tự thiết kế lộ trình riêng với budget tự chọn (không bắt buộc theo đề xuất của app)
- Sau khi lộ trình được thiết kế xong, kiểm tra thực tế xem lệch giá bao nhiêu so với ngân sách ban đầu
- App tự tính chi phí cho từng địa điểm ghé qua, không chỉ đưa ra một con số tổng — kèm tổng chi phí dồn (running total) và số tiền còn lại so với ngân sách
- Khi budget = 0: chỉ chọn các địa điểm miễn phí (`estimated_cost = 0`), không để thuật toán bị lỗi chia-cho-0 khi tính tỉ lệ giá trị/chi phí

## 4. Nguồn giá theo loại địa điểm — đảm bảo mọi địa điểm đều có giá

Nguyên tắc: mọi địa điểm gợi ý ra cho người dùng đều phải kèm giá — xử lý theo chuỗi ưu tiên (fallback chain), không có trường hợp nào thiếu giá.

- **Địa điểm vui chơi/tham quan** — ưu tiên 1: GetYourGuide Partner API hoặc Tiqets Partner API (giá vé thật, self-serve, tạo token ngay không cần chờ duyệt)
- **Quán ăn và các địa điểm còn lại** — chuỗi ưu tiên 3 bước:
  1. `priceRange` từ Google Places API (New) — field mới trả về số tiền thật (`startPrice`/`endPrice`, có đơn vị tiền tệ), khác hẳn `price_level` cũ chỉ là mức 1-4. Không phải địa điểm nào cũng có sẵn dữ liệu này
  2. Nếu không có — dùng giá do người dùng tự nhập sau khi ghé thăm (crowdsource), lưu dần vào DB
  3. Nếu vẫn chưa có — ước tính theo `price_level` bằng bảng quy đổi VND định sẵn (mức 1 ≈ 20-50k, mức 2 ≈ 50-150k...)

Mỗi giá trả về kèm theo `price_source` (`google_api` / `crowdsource` / `estimated`) để biết độ tin cậy của từng mức giá — minh bạch thay vì coi mọi giá là chính xác như nhau.

## 5. Công nghệ

**Client (Android app)**

- Kotlin + Jetpack Compose
- Google Maps SDK
- Retrofit/OkHttp
- Room (cache offline)
- CameraX (chụp ảnh địa điểm)
- SmsManager + FusedLocationProviderClient (SOS)

**Backend**

- Node.js (Express hoặc NestJS)
- Docker
- JWT (xác thực)
- Google Cloud Vision API (phân tích ảnh)
- Google Places API (New) — gợi ý địa điểm + field `priceRange` (giá thật)
- GetYourGuide/Tiqets API (giá vui chơi)

**Database**

- Amazon RDS — MySQL

**Hạ tầng**

- EC2 — chạy Backend (Docker container)
- RDS — chạy Database, tách riêng khỏi EC2 Backend
- S3 — lưu ảnh avatar, ảnh địa điểm người dùng upload
- Security Groups — chỉ EC2 Backend được phép kết nối RDS
- GitHub Actions — CI/CD tự động build & deploy khi push code

## 6. Thuật toán tối ưu lộ trình nhóm có ràng buộc công bằng ngân sách

**Bài toán:** biến thể của Team Orienteering Problem kết hợp ràng buộc công bằng nhiều tác nhân — NP-hard.

**Input:** n thành viên (ngân sách riêng, sở thích), m địa điểm ứng viên (chi phí, thời gian ghé thăm, tọa độ), tổng thời gian chuyến đi T.

**Output:** tập địa điểm + thứ tự ghé thăm + cách chia chi phí, không ai vượt ngân sách cá nhân, tổng thời gian ≤ T, mức hài lòng công bằng nhất có thể.

**Pipeline 4 bước:**

1. Greedy construction — chọn theo tỉ lệ giá trị/chi phí (địa điểm miễn phí luôn ưu tiên cao nhất)
2. Local search — 2-opt hoặc Simulated Annealing
3. Fairness check — kiểm tra & điều chỉnh để không ai vượt ngân sách cá nhân
4. Output — lộ trình cuối cùng kèm chi phí từng điểm và cách chia

**Đánh giá:** so sánh Greedy thuần, Greedy + Local search + Fairness, và ILP tối ưu tuyệt đối (OR-Tools, baseline trên instance nhỏ). Metric: tổng mức hài lòng, độ lệch ngân sách, chỉ số công bằng (Jain's Fairness Index), thời gian chạy.

## 7. Database — schema chính

- **User** — id, email, phone, password_hash, name, avatar, created_at
- **Place** — id, google_place_id, name, type, lat, lng, price_level, estimated_cost (VND, 0 = miễn phí), price_source (`google_api` / `crowdsource` / `estimated`), rating
- **Trip** — id, user_id, budget, duration, created_at
- **TripPlace** — trip_id, place_id, thứ tự, thời gian dự kiến, chi phí tại điểm này
- **TripGroup** — trip_id, user_id, vai trò, ngân sách cá nhân
- **Favorite** — user_id, place_id
- **SearchHistory** — user_id, keyword, timestamp
- **Review** — id, user_id, place_id/trip_id, rating, comment
- **EmergencyContact** — id, user_id, tên, số điện thoại
