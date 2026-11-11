# Thiết kế database

MySQL 8.4, truy cập qua Prisma ([backend/prisma/schema.prisma](../backend/prisma/schema.prisma)). Bảng được tạo dần theo từng module của backend; mục "Tạo ở" cho biết bước nào thêm bảng đó. Bảng chưa tạo có thể được chỉnh lại khi làm module tương ứng.

## Quy ước

- Tên bảng và cột dùng `snake_case`; model và field trong Prisma dùng `PascalCase`/`camelCase` (ánh xạ bằng `@@map`, `@map`).
- Khóa chính là số nguyên tự tăng. Quyền truy cập luôn được kiểm tra ở API, không dựa vào việc id khó đoán.
- Tiền là số nguyên VND, tính cho một người. Giá từ nguồn dùng ngoại tệ được đổi sang VND lúc lấy giá.
- Thời gian lưu theo UTC. Số điện thoại lưu theo E.164 (vd. `+84905128846`).
- Không lưu số liệu tính lại được (tổng dồn, số tiền còn lại, quãng đường): API tính khi trả kết quả.

## Sơ đồ

```mermaid
erDiagram
    users ||--o{ otp_codes : "nhận mã"
    users ||--o{ refresh_tokens : "đăng nhập"
    users ||--o{ search_history : "tìm kiếm"
    users ||--o{ favorites : "yêu thích"
    users ||--o{ emergency_contacts : "lưu"
    users ||--o{ trips : "tạo"
    users ||--o{ trip_members : "tham gia"
    places ||--o{ places : "nằm trong"
    places ||--o{ place_categories : "thuộc nhóm"
    places ||--o{ place_interests : "phù hợp"
    places ||--o{ favorites : ""
    places ||--o{ trip_stops : ""
    places ||--o{ vote_options : ""
    places ||--o{ trip_lodgings : "khách sạn"
    trips ||--o{ trip_members : "gồm"
    trips ||--o| trip_lodgings : "ở tại"
    trips ||--o{ trip_stops : "ghé"
    trips ||--o{ vote_options : "bình chọn"
    trip_members ||--o{ member_interests : "thích"
    trip_members ||--o{ stop_participants : "đi cùng"
    trip_stops ||--o{ stop_participants : ""
    trip_members ||--o{ votes : "tick"
    vote_options ||--o{ votes : ""
```

## Tài khoản

### `users` · tạo ở A4

| Cột | Kiểu | Ghi chú |
| --- | --- | --- |
| id | INT | Khóa chính |
| email | VARCHAR(254), unique | Có thể trống nếu đăng ký bằng số điện thoại |
| phone | VARCHAR(16), unique | E.164; có thể trống nếu đăng ký bằng email |
| password_hash | VARCHAR(255) | Mật khẩu đã hash, không lưu mật khẩu gốc |
| full_name | VARCHAR(100) | |
| avatar_url | VARCHAR(2048) | Ảnh trên S3 (giai đoạn B) |
| verified_at | DATETIME | Trống khi chưa nhập đúng mã OTP đăng ký; chưa xác thực thì chưa đăng nhập được |
| password_changed_at | DATETIME | Lần đổi mật khẩu gần nhất; token đặt lại mật khẩu cấp trước thời điểm này hết hiệu lực (mỗi token chỉ dùng một lần). Thêm ở A6 |
| created_at, updated_at | DATETIME | |

Ràng buộc `CHECK`: phải có ít nhất một trong `email`, `phone`.

### `otp_codes` · A6

| Cột | Kiểu | Ghi chú |
| --- | --- | --- |
| id | INT | |
| user_id | INT → users | Xóa user thì xóa luôn mã |
| purpose | ENUM(REGISTER, RESET_PASSWORD) | |
| destination | VARCHAR(254) | Email hoặc số điện thoại (E.164) đã gửi mã |
| code_hash | CHAR(64) | HMAC-SHA256 của mã 6 số với khóa bí mật của server, để database bị lộ cũng không dò ra mã |
| expires_at | DATETIME | 15 phút sau khi gửi |
| attempts | INT | Số lần đã thử; tới 5 lần thì khóa mã, phải xin mã mới |
| used_at | DATETIME | Trống khi chưa dùng |
| created_at | DATETIME | Gửi lại được sau 60 giây kể từ mã gần nhất |

Chỉ mã mới nhất của một user và một mục đích có hiệu lực. Chỉ mục (user_id, purpose, created_at).

### `refresh_tokens` · A6

| Cột | Kiểu | Ghi chú |
| --- | --- | --- |
| id | INT | |
| user_id | INT → users | Xóa user thì xóa luôn token |
| token_hash | CHAR(64), unique | SHA-256 của token ngẫu nhiên 32 byte; token gốc chỉ gửi cho app |
| expires_at | DATETIME | 30 ngày sau khi cấp |
| revoked_at | DATETIME | Đổi token, đăng xuất hoặc đặt lại mật khẩu thì thu hồi |
| created_at | DATETIME | |

Mỗi lần làm mới, token cũ bị thu hồi và cấp token mới. Nếu một token đã thu hồi bị dùng lại (dấu hiệu bị lộ), mọi token của user đó bị thu hồi.

## Địa điểm

### `places` · tạo ở A7

Hai loại dòng:

- **CATALOG**: danh mục riêng của TripFair (hiện là dữ liệu mẫu Đà Nẵng nạp bằng `npm run db:seed`), lưu đủ mọi cột.
- **GOOGLE**: điểm tìm thấy qua Google Places. Điều khoản Google chỉ cho lưu lâu dài `place_id`, nên dòng này chỉ có `google_place_id`; tên, tọa độ, rating, giờ mở cửa được lấy trực tiếp từ Google mỗi lần hiển thị. Dòng này cho điểm Google một id của TripFair để dùng trong chi tiết, yêu thích và lộ trình.

| Cột | Kiểu | Ghi chú |
| --- | --- | --- |
| id | INT | |
| source | ENUM(CATALOG, GOOGLE) | |
| slug | VARCHAR(100), unique | Khóa ổn định của điểm trong danh mục, vd. `golden-bridge`; seed cập nhật theo cột này |
| google_place_id | VARCHAR(255), unique | Bắt buộc với dòng GOOGLE |
| name, area, city | VARCHAR | `area` là dòng dưới tên, vd. "Bà Nà Hills · Hòa Vang" |
| category | ENUM(FOOD, FUN, SCENIC) | Nhóm chính, dùng làm nhãn: Ăn uống, Vui chơi, Cảnh đẹp |
| lat, lng | DOUBLE | Tọa độ, dùng tính khoảng cách |
| rating, rating_count | DOUBLE, INT | |
| open_minute, close_minute | INT | Giờ mở/đóng cửa tính bằng phút từ 0:00; cả hai trống là mở cả ngày; đóng nhỏ hơn mở là đóng sau nửa đêm |
| description, image_url | TEXT, VARCHAR | |
| visit_minutes | INT | Thời gian ghé thăm dự kiến |
| price_min, price_max | INT | VND/người; bằng nhau khi là giá cố định; 0 là miễn phí; trống khi vé của khu bao ngoài đã gồm điểm này |
| price_source | ENUM(PARTNER_API, GOOGLE_API, ESTIMATED) | Giá khác PARTNER_API hiển thị kèm dấu ~ |
| price_provider | VARCHAR(100) | Klook, GetYourGuide, Tiqets, Google, TripFair (ước tính) |
| price_updated_at | DATETIME | |
| parent_id | INT → places | Khu có bán vé chứa điểm này, vd. Cầu Vàng nằm trong Bà Nà Hills |
| search_text | VARCHAR(600) | Tên, khu vực, thành phố viết thường, bỏ dấu (đ → d), để tìm kiếm không dấu |
| created_at, updated_at | DATETIME | |

Ràng buộc `CHECK`: dòng GOOGLE phải có `google_place_id`; dòng CATALOG phải có slug, tên, thành phố, nhóm, tọa độ, thời gian ghé và `search_text`; khoảng giá hợp lệ (`0 ≤ min ≤ max`); giờ mở cửa trong khoảng 0–1439. MySQL không cho CHECK dùng cột có khóa ngoại kèm hành động (`parent_id`), nên quy tắc "có giá hoặc có khu cha có giá" do script seed kiểm tra.

**Giá** (theo thứ tự):

1. Điểm trong danh mục: giá riêng của điểm; nếu không có thì lấy giá vé của khu bao ngoài và ghi rõ "đã gồm trong vé …". Khi lập lộ trình (A8), điểm như vậy tính 0đ nếu vé khu đã có trong lộ trình.
2. Điểm từ Google (không có vé đối tác): `priceRange` của Google → loại điểm thường miễn phí (bãi biển, công viên, chùa, nhà thờ, cầu…) = 0đ ước tính → bảng quy đổi `priceLevel` (mức 1: 20–50k, mức 2: 50–150k, mức 3: 150–500k, mức 4: 500k–1,5 triệu) → mức mặc định theo nhóm. Điểm nào cũng có giá.

Thuật toán dùng `price_max` làm chi phí của điểm để không ai vượt ngân sách khi giá thật cao; giao diện hiện cả khoảng giá.

### `place_categories` · tạo ở A7

Khóa chính (place_id, category). Mọi nhóm của một điểm (gồm cả nhóm chính) để lọc, vd. Chợ đêm Sơn Trà thuộc cả Vui chơi và Ăn uống. Điểm GOOGLE không có dòng ở đây; nhóm của chúng được suy ra từ loại địa điểm Google trả về.

### `place_interests` · tạo ở A8

Khóa chính (place_id, interest). `interest` là một trong 8 sở thích của app: FOOD (ẩm thực), BEACH (biển), SCENERY (cảnh đẹp), RELAX (thư giãn), FUN (vui chơi), PHOTO (chụp ảnh), CULTURE (văn hóa), EXPLORE (khám phá). Thuật toán so sở thích của thành viên với sở thích của điểm để tính mức hài lòng.

### `search_history` · tạo ở A7

| Cột | Kiểu | Ghi chú |
| --- | --- | --- |
| id | INT | |
| user_id | INT → users | Xóa user thì xóa luôn lịch sử |
| query | VARCHAR(100) | Từ khóa theo cách gõ gần nhất |
| normalized_query | VARCHAR(100) | Viết thường, bỏ dấu; unique theo (user_id, normalized_query) nên "Đà Nẵng" và "da nang" là một mục |
| category | ENUM, trống được | Bộ lọc dùng kèm từ khóa |
| searched_at | DATETIME | Tìm lại cùng từ khóa thì cập nhật thời điểm, mục đó lên đầu |

Lịch sử được lưu khi gọi `GET /places?q=…` có từ khóa (gợi ý khi đang gõ thì không lưu); mỗi người giữ 50 mục gần nhất.

## Chuyến đi

Tạo ở A8/A9 (migration `trips`). Ràng buộc CHECK được thêm tay trong migration: ngày về không trước ngày đi, khung giờ hợp lệ, tiền không âm, mức hài lòng 0–100.

### `trips` · tạo ở A8

| Cột | Kiểu | Ghi chú |
| --- | --- | --- |
| id | INT | |
| owner_id | INT → users | Người tạo chuyến; xóa user thì xóa chuyến |
| type | ENUM(SOLO, GROUP) | Chuyến một mình là nhóm một người, dùng chung thuật toán |
| name | VARCHAR(100), trống được | Tên nhóm, bắt buộc với GROUP |
| city | VARCHAR(100) | Thành phố đến, dùng để tìm điểm và khách sạn trên Google |
| start_date, end_date | DATE | Tối đa 14 ngày |
| day_start_minute, day_end_minute | INT | Khung giờ đi chơi mỗi ngày, phút từ 0:00; mặc định 08:00–21:00 |
| gap_minutes | INT | Nghỉ tối thiểu giữa hai điểm ("thời gian giữa các lần đi chơi"), mặc định 30, 0–240 |
| lodging_mode | ENUM(HOTEL, SELF) | SELF là "Tự lo chỗ ở"; chuyến một mình ngân sách 0đ luôn SELF |
| status | ENUM(DRAFT, PLANNED) | Về DRAFT (và xóa lộ trình) mỗi khi dữ liệu đầu vào đổi |
| invite_code | VARCHAR(8), unique | 6 ký tự không có 0/O/1/I; trống với chuyến một mình |
| vote_deadline | DATETIME, trống được | Sau hạn này thành viên không đổi phiếu được |
| planned_at | DATETIME, trống được | Lần lập lộ trình gần nhất |
| created_at, updated_at | DATETIME | |

Mỗi nhóm lập đúng một chuyến nên thông tin nhóm nằm luôn trong `trips`.

### `trip_members` · tạo ở A8

| Cột | Kiểu | Ghi chú |
| --- | --- | --- |
| id | INT | |
| trip_id, user_id | INT | Unique theo cặp |
| role | ENUM(OWNER, MEMBER) | Chỉ OWNER lập lộ trình, chọn chỗ ở, xóa chuyến |
| budget | INT | Ngân sách tối đa; API chỉ trả cho chính người đó, cả nhóm chỉ thấy phần chi |
| lodging_share | INT | Phần tiền phòng cả chuyến, ghi lúc lập lộ trình |
| satisfaction | INT, trống được | Mức hài lòng (%) sau khi lập lộ trình |
| ballot_sent_at | DATETIME, trống được | Thời điểm "Gửi phiếu"; sau đó phiếu không đổi được |
| joined_at | DATETIME | Thứ tự vào nhóm cũng là thứ tự xếp phòng |

### `member_interests` · tạo ở A8

Khóa chính (member_id, interest), cùng danh sách sở thích với `place_interests`. Lưu theo từng chuyến vì mỗi chuyến có thể chọn sở thích khác.

### `trip_lodgings` · tạo ở A8

Khách sạn là địa điểm thật tìm qua Google Places (loại `lodging`), nên không có bảng `hotels` riêng: khách sạn là một dòng `places` nguồn GOOGLE (chỉ lưu place ID); tên, địa chỉ, vị trí lấy trực tiếp từ Google mỗi lần hiển thị.

| Cột | Kiểu | Ghi chú |
| --- | --- | --- |
| trip_id | INT → trips | Khóa chính; mỗi chuyến tối đa một nơi ở |
| place_id | INT → places | Khách sạn đã chọn |
| check_in, check_out | DATE | Ngày đầu và ngày cuối chuyến |
| rooms, guests_per_room | INT | Số người mỗi phòng 1–4 do nhóm chọn (mặc định 2); số phòng tính lại theo số thành viên lúc lập lộ trình |
| price_per_night | INT | Giá một phòng một đêm, chụp lại lúc chọn |
| price_source, price_provider, priced_at | ENUM, VARCHAR, DATETIME | Nguồn giá: GOOGLE_API khi Google có khoảng giá VND, còn lại ESTIMATED theo price_level (cho tới khi có Agoda API) |

### `trip_stops` · tạo ở A8

| Cột | Kiểu | Ghi chú |
| --- | --- | --- |
| id | INT | |
| trip_id | INT → trips | |
| place_id | INT → places | |
| day, position | INT | Ngày thứ mấy và thứ tự trong ngày; unique (trip_id, day, position) |
| start_minute | INT | Giờ bắt đầu, phút từ 0:00 |
| travel_minutes | INT | Thời gian đi tới điểm này (Google Routes, hoặc ước tính đường chim bay) |
| visit_minutes | INT | |
| cost_per_person | INT | Giá chụp lại lúc lập lộ trình (mức cao của khoảng giá) |
| price_source, price_provider | ENUM, VARCHAR | Chụp lại cùng với giá |

Giá được chụp lại vì giá đối tác thay đổi, còn lộ trình đã lập phải giữ đúng số đã tính. Điểm lưu trú trên dòng thời gian được API ghép từ `trip_lodgings`.

## Nhóm

### `vote_options` · tạo ở A9

id, trip_id → trips, place_id → places (unique theo cặp), added_by_id → trip_members (trống khi người thêm rời nhóm), created_at. Các địa điểm thành viên đưa vào bình chọn; điểm người dùng tự chọn khi tạo chuyến một mình cũng là lựa chọn ở đây, nên thuật toán luôn lập từ `vote_options` khi có.

### `votes` · tạo ở A9

Khóa chính (option_id, member_id), created_at. Mỗi dòng là một thành viên tick đồng ý một địa điểm; điểm được tick có giá trị cao hơn với người đã tick.

### `stop_participants` · tạo ở A9

Khóa chính (stop_id, member_id), cost. Ai đi điểm nào và trả bao nhiêu ở đó (0đ khi vé khu bao trọn, vd. Cầu Vàng với người đã có vé Bà Nà Hills). Phần chi của một người là tổng `cost` cộng `lodging_share`. Vd. một thành viên không đủ tiền đi Bà Nà Hills thì không có dòng ở điểm đó và không trả vé 900.000đ.

## Cá nhân

### `favorites` · tạo ở A7

Khóa chính (user_id, place_id), created_at; chỉ mục place_id. Lưu lại một điểm đã lưu hoặc bỏ lưu điểm chưa lưu đều không lỗi. Xóa user hoặc điểm thì xóa luôn dòng yêu thích. Điểm GOOGLE trong danh sách yêu thích được lấy chi tiết trực tiếp từ Google khi hiển thị.

### `emergency_contacts` · tạo ở A10

| Cột | Kiểu | Ghi chú |
| --- | --- | --- |
| id | INT | |
| user_id | INT → users | Xóa user thì xóa luôn liên hệ |
| name | VARCHAR(100) | |
| relation | VARCHAR(50), trống được | vd. "Mẹ" |
| phone | VARCHAR(16) | E.164; unique theo (user_id, phone) |
| receives_sos | BOOLEAN | Có nhận tin SOS không, mặc định có |
| created_at, updated_at | DATETIME | |

Mỗi người tối đa 5 liên hệ (mỗi lần SOS gửi một SMS cho từng người). Lưu trên server để giữ được khi đổi điện thoại; app Android giữ bản sao để SOS vẫn chạy khi mất mạng, và tự gửi SMS bằng SmsManager.

## Không lưu trong database

- **Đánh giá địa điểm**: lấy từ Google Places API (New), không có bảng `reviews`.
- **Ảnh và kết quả nhận diện**: ảnh chỉ được gửi tới Cloud Vision rồi bỏ, không lưu; địa điểm tìm được lưu như mọi điểm Google (chỉ place ID).
- **Gợi ý khách sạn**: tìm trên Google Places lúc gọi API; chỉ khách sạn đã chọn mới được lưu (place ID và giá chụp lại).
- **Tổng dồn, số tiền còn lại, cảnh báo của lộ trình**: tính khi trả lộ trình.
