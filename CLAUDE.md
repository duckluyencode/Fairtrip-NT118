# App lộ trình du lịch

## Tổng quan
Đồ án sinh viên: Android app (Kotlin) gợi ý địa điểm và tự sinh lộ trình du lịch theo ngân sách, thời gian; hỗ trợ lộ trình nhóm chia chi phí công bằng. Backend NestJS deploy trên AWS.
Mô tả chi tiết: [docs/project-overview.md](docs/project-overview.md). Đây là bản tham khảo, chưa chốt. Khi bản mô tả và CLAUDE.md mâu thuẫn, CLAUDE.md được ưu tiên; khi cả hai chưa rõ thì hỏi tôi.

## Tech stack
- Android: Kotlin, Jetpack Compose, Google Maps SDK, Retrofit/OkHttp, Room, CameraX, FusedLocationProviderClient, SmsManager
- Backend: NestJS (Node.js), JWT, Docker
- Database: MySQL trên Amazon RDS
- Dịch vụ ngoài: Google Places API (New), Google Cloud Vision API, GetYourGuide/Tiqets API
- Hạ tầng: EC2 (chạy backend container), RDS, S3 (ảnh), Security Groups, GitHub Actions (CI/CD)

## Cấu trúc repo
Monorepo. Thư mục được tạo dần khi thực sự cần, không scaffold trước.
Khi đề xuất tạo thư mục hoặc module mới: giải thích vai trò của nó trước khi tạo.
Khi có `android/` và `backend/`: lệnh build/test và quy ước riêng của từng phần đặt trong `android/CLAUDE.md` và `backend/CLAUDE.md`; file này chỉ giữ phần dùng chung.

```
docs/        Tài liệu dự án (mô tả tổng quan)
CLAUDE.md
README.md
```

## Lệnh thường dùng
[Điền khi đã thiết lập: build Android, chạy backend local, test, lint, docker compose]

## Cách làm việc với tôi
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
- Mọi địa điểm gợi ý đều phải có giá, kèm price_source (google_api / crowdsource / estimated).
  - Vui chơi/tham quan: GetYourGuide/Tiqets API.
  - Địa điểm còn lại: priceRange (Google Places API New) → giá crowdsource trong DB → ước tính theo price_level bằng bảng quy đổi VND.
- Budget = 0 vẫn phải sinh được lộ trình: chỉ chọn địa điểm có estimated_cost = 0; xử lý chi phí = 0 khi tính tỉ lệ giá trị/chi phí, không để lỗi chia cho 0.
- Lộ trình trả về chi phí từng điểm, tổng dồn (running total) và số tiền còn lại so với ngân sách.
- Lộ trình nhóm: Greedy (theo tỉ lệ giá trị/chi phí, địa điểm miễn phí ưu tiên cao nhất) → Local search (2-opt hoặc Simulated Annealing) → Fairness check (không ai vượt ngân sách cá nhân) → Output kèm chi phí từng điểm và cách chia. Ràng buộc: tổng thời gian ≤ T.
- Đánh giá thuật toán: so sánh Greedy thuần, Greedy + Local search + Fairness, và ILP (OR-Tools) trên instance nhỏ. Metric: tổng mức hài lòng, độ lệch ngân sách, Jain's Fairness Index, thời gian chạy.

## Quyết định đã chốt
- 2026-10-17: Backend dùng NestJS (bản mô tả để ngỏ Express hoặc NestJS).

## Câu hỏi mở
Mỗi mục ghi rõ cần chốt trước khi làm phần nào. Chưa tới phần đó thì để nguyên.
- [Giá] Giá vé lấy từ GetYourGuide/Tiqets mang price_source gì? Enum hiện tại (google_api / crowdsource / estimated) chưa có giá trị cho nguồn này.
- [Giá] Điểm tham quan không có trên GetYourGuide/Tiqets (chùa, công viên, bãi biển…) thì lấy giá theo chuỗi nào?
- [User] Đăng nhập bằng số điện thoại và quên mật khẩu cần dịch vụ gửi OTP hoặc email. Tech stack hiện chưa có dịch vụ này.
- [Trip] Lộ trình cá nhân dùng chung pipeline với nhóm (n = 1) hay thuật toán riêng? "Thời gian giữa các lần đi chơi" nghĩa là gì?
- [Database] Thuật toán cần thời gian ghé thăm và khoảng giá (min/max, đơn vị tiền tệ), nhưng bảng Place chưa có các cột này. Cũng chưa có bảng lưu giá crowdsource, bình chọn và cách chia chi phí.
- [Rủi ro] Kiểm tra sớm điều kiện cấp Partner API của GetYourGuide/Tiqets và họ có bao nhiêu địa điểm ở Việt Nam. Bản mô tả ghi "tạo token ngay", nhưng điều này chưa được xác minh.

## Ngôn ngữ
Giải thích bằng tiếng Việt. Code, tên biến, comment và commit message bằng tiếng Anh.
