# Google Cloud và Gmail cho backend

Backend lấy dữ liệu thật từ ba API của Google Cloud và gửi mã OTP qua Gmail. Tài liệu này hướng dẫn tạo key, giới hạn key, đặt quota và đưa secret vào backend, theo thứ tự làm.

## 1. Backend dùng gì và ở đâu

| API | Dùng cho | Code |
| --- | --- | --- |
| Places API (New) | Tìm kiếm, gợi ý khi gõ, chi tiết địa điểm, ứng viên khi lập lộ trình, khách sạn, khớp địa danh nhận diện | [http-google-places.client.ts](../backend/src/places/google/http-google-places.client.ts) |
| Routes API | Thời gian lái xe thật giữa các điểm của lộ trình đã lập | [routes.client.ts](../backend/src/places/google/routes.client.ts) |
| Cloud Vision API | Nhận diện địa danh trong ảnh (`LANDMARK_DETECTION`) | [vision.client.ts](../backend/src/recognition/vision.client.ts) |

Cả ba dùng chung một key trong biến `GOOGLE_MAPS_API_KEY`. Không có key thì backend chạy bằng danh mục mẫu (chỉ dùng khi phát triển và test); nhận diện ảnh báo `RECOGNITION_UNAVAILABLE`.

Mỗi thao tác của người dùng tốn bao nhiêu lượt gọi (để ước tính chi phí):

| Thao tác | Lượt gọi Google |
| --- | --- |
| Tìm kiếm một trang | 1 Text Search |
| Gõ trong ô tìm kiếm | Autocomplete theo phiên (`sessionToken`), cả phiên tính cùng lượt mở chi tiết |
| Mở chi tiết một điểm Google | 1 Place Details |
| Mở danh sách yêu thích | 1 Place Details cho mỗi điểm Google trong danh sách |
| Lập lộ trình khi chưa chọn điểm | 2 Text Search (điểm tham quan, quán ăn; đã gồm giá và giờ mở cửa) |
| Lập lộ trình từ các điểm đã chọn | 1 Place Details mỗi điểm Google |
| Tính lại giờ lộ trình | 1 Compute Routes mỗi ngày (tới 10 điểm/ngày) |
| Gợi ý khách sạn | 1 Text Search, cộng Place Details cho các điểm đã chọn (để lấy tâm lộ trình) |
| Xem chuyến đi, xem lộ trình | 1 Place Details cho khách sạn và cho mỗi điểm Google |
| Nhận diện một ảnh | 1 Vision, cộng 1 Text Search mỗi địa danh tìm thấy (tối đa 3) |

Giá mỗi lượt phụ thuộc vào trường được xin (field mask): xin thêm đánh giá, giá, giờ mở cửa thì lượt gọi thuộc mức giá cao hơn. Vì vậy client chỉ xin trường màn hình cần (`LIST_FIELDS`, `PLANNING_FIELDS`, `DETAIL_FIELDS`). Xem bảng giá chính thức: [Places API](https://developers.google.com/maps/billing-and-pricing/pricing), [Cloud Vision](https://cloud.google.com/vision/pricing).

Điều khoản Google Maps: chỉ được lưu lâu dài `place_id`; tên, địa chỉ, giá, giờ mở cửa phải lấy trực tiếp mỗi lần hiển thị và ghi nguồn "Google Maps" (API trả `attribution: "Google Maps"` để app hiển thị).

## 2. Tạo project và bật API

1. Vào [Google Cloud Console](https://console.cloud.google.com/), tạo project mới, vd. `tripfair`.
2. Billing → liên kết tài khoản thanh toán (cần thẻ; tài khoản mới thường có tín dụng dùng thử). Không có billing thì các API trên không chạy.
3. APIs & Services → Library, bật lần lượt: **Places API (New)**, **Routes API**, **Cloud Vision API**. Chú ý chọn "Places API (New)", không phải "Places API" bản cũ.

Hoặc bằng gcloud CLI:

```bash
gcloud services enable places.googleapis.com routes.googleapis.com vision.googleapis.com --project tripfair
```

## 3. Tạo và giới hạn key

1. APIs & Services → Credentials → Create credentials → API key.
2. Edit key:
   - **API restrictions** → Restrict key → chọn đúng ba API: Places API (New), Routes API, Cloud Vision API. Key lộ ra cũng không dùng được cho API khác.
   - **Application restrictions** → IP addresses → thêm Elastic IP của EC2 (vd. `47.128.180.54`) và IP công khai của máy phát triển. Key chỉ dùng ở server nên giới hạn theo IP là đúng loại. IP nhà mạng thay đổi thì cập nhật lại.
3. Đặt quota theo ngày để chặn hóa đơn bất ngờ: APIs & Services → chọn từng API → Quotas & System Limits → sửa "requests per day". Gợi ý cho đồ án: Text Search 500, Place Details 2000, Autocomplete 2000, Compute Routes 500, Vision 200.
4. Billing → Budgets & alerts: ngân sách tháng (vd. 10 USD) gửi email khi đạt 50%, 90%, 100%.

## 4. Đưa key vào backend

Trên máy, trong `backend/.env` (file này không bao giờ được commit, đã có trong `.gitignore`):

```
GOOGLE_MAPS_API_KEY=<key>
```

Khởi động lại `npm run start:dev`. Thử key trực tiếp với Google (thay `<key>`):

```bash
curl -s -X POST https://places.googleapis.com/v1/places:searchText -H "Content-Type: application/json" -H "X-Goog-Api-Key: <key>" -H "X-Goog-FieldMask: places.id,places.displayName" -d '{"textQuery":"Cầu Rồng Đà Nẵng","languageCode":"vi"}'
```

Kết quả đúng có `places[0].displayName.text`. Lỗi `PERMISSION_DENIED` thường do chưa bật API, key bị giới hạn sai API, hoặc IP máy không nằm trong danh sách cho phép.

Trên AWS: Systems Manager → Parameter Store → Create parameter, tên `/tripfair/prod/GOOGLE_MAPS_API_KEY`, kiểu **SecureString**, dán key. Nhập bằng Console để key không nằm trong lịch sử terminal. Rồi deploy lại (xem [deploy.md](deploy.md), mục 10.4) để API đọc giá trị mới.

## 5. Gmail App Password (gửi OTP qua email)

Backend gửi mã OTP **từ một tài khoản Gmail của dự án** tới email của người dùng; người dùng đăng ký bằng email nào cũng nhận được mã, họ không cần làm gì với Gmail. App Password là mật khẩu riêng cho ứng dụng của tài khoản gửi, không phải mật khẩu Gmail thật.

1. Đăng nhập tài khoản Gmail dùng để gửi (nên tạo tài khoản riêng cho TripFair).
2. Google Account → Security → bật **2-Step Verification** (bắt buộc để có App Password).
3. Google Account → Security → **App passwords** (hoặc vào [myaccount.google.com/apppasswords](https://myaccount.google.com/apppasswords)) → đặt tên "TripFair" → Create → chép chuỗi 16 ký tự.
4. Trên máy, trong `backend/.env`:

   ```
   SMTP_HOST=smtp.gmail.com
   SMTP_PORT=465
   SMTP_USER=<địa chỉ gmail gửi>
   SMTP_PASSWORD=<app password 16 ký tự, bỏ dấu cách>
   SMTP_FROM=TripFair <địa chỉ gmail gửi>
   ```

5. Trên AWS: tạo `/tripfair/prod/SMTP_USER` và `/tripfair/prod/SMTP_FROM` (String), `/tripfair/prod/SMTP_PASSWORD` (SecureString) trong Parameter Store; `SMTP_HOST`, `SMTP_PORT` đã có. Deploy lại.

Gmail giới hạn khoảng 500 email/ngày cho tài khoản thường, đủ cho đồ án. Khi cần gửi nhiều hơn, đổi sang Amazon SES hoặc nhà cung cấp khác chỉ cần sửa các biến `SMTP_*`.
