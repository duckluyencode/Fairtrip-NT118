# Chạy app TripFair trên máy

Hướng dẫn cho thành viên mới: clone repo, mở bằng Android Studio và chạy app trên máy ảo hoặc điện thoại. App gọi backend dùng chung trên AWS, nên **không cần** cài Docker, Node.js hay chạy backend trên máy.

Muốn chạy backend trên máy (khi sửa code backend), xem [backend/CLAUDE.md](../backend/CLAUDE.md) và mục 5 ở cuối trang này.

## 1. Cài đặt

1. **Android Studio** bản ổn định mới nhất (dự án dùng AGP 9.4.1 và Gradle 9.6.0; nhóm đang chạy được với Android Studio 2026.2). Bản cũ hơn báo lỗi sync Gradle kiểu "requires a newer version of Android Studio".
2. Trong Android Studio: **Settings → Languages & Frameworks → Android SDK**
   - Tab **SDK Platforms**: tick **Android API 37**.
   - Tab **SDK Tools**: tick **Android SDK Build-Tools**, **Android Emulator**, **Android SDK Platform-Tools**.
   - Bấm **Apply** để tải.
3. **Máy ảo**: **Device Manager → Create Virtual Device**, chọn một máy Pixel có **Google Play** (biểu tượng Play Store) và system image API 37. Cần bản có Google Play để Google Maps và vị trí chạy được.

## 2. Lấy code

```bash
git clone https://github.com/duckluyencode/Fairtrip-NT118.git
cd Fairtrip-NT118
git checkout main
git pull
```

Mở thư mục **`android/`** (không phải thư mục gốc của repo) bằng **File → Open** trong Android Studio. Lần đầu Studio sẽ sync Gradle và tự tạo `android/local.properties` chứa `sdk.dir`.

## 3. Cấu hình `android/local.properties`

File này không nằm trong git (chứa khóa và đường dẫn riêng của từng máy). Mở file và thêm vào cuối:

```properties
# Backend dùng chung trên AWS
tripfair.apiBaseUrl=https://52-65-234-138.sslip.io/api/v1/

# Khóa Google Maps cho Android (xin trưởng nhóm, gửi qua kênh riêng, không đưa lên git)
MAPS_API_KEY=...
```

- Thiếu `tripfair.apiBaseUrl` thì app gọi `http://localhost:3000` (backend trên chính máy mình) và báo lỗi mạng khi đăng nhập.
- Thiếu `MAPS_API_KEY` thì app vẫn chạy, chỉ chỗ bản đồ hiện ô thông báo.
- Sửa `local.properties` xong thì bấm **Sync Project with Gradle Files** (biểu tượng con voi) rồi chạy lại.

### Bản đồ cần SHA-1 của máy bạn

Khóa Maps chỉ cho phép app được ký bằng chứng chỉ đã đăng ký. Mỗi máy tính có một chứng chỉ debug riêng, nên phải gửi SHA-1 cho trưởng nhóm thêm vào khóa trên Google Cloud ([google-cloud.md](google-cloud.md)). Lấy SHA-1 trong thư mục `android/`:

```bash
./gradlew :app:signingReport
```

Chép dòng `SHA1:` của `Variant: debug`. Chưa được thêm thì bản đồ trống (màu xám), các chức năng khác vẫn dùng được.

## 4. Chạy app

1. Chọn máy ảo ở thanh công cụ, bấm **Run ▶** (cấu hình `app`).
2. Đăng ký tài khoản bằng email thật: mã OTP 6 số gửi về email (xem cả mục Spam). Đăng ký bằng số điện thoại chưa gửi SMS được.
3. Trên máy ảo, đặt vị trí ở Việt Nam để gợi ý quanh mình đúng chỗ: nút **⋯ (Extended controls) → Location**, tìm một địa điểm rồi **Set location**.

Lưu ý:

- Server AWS chỉ bật khi nhóm cần test hoặc demo để tiết kiệm chi phí. Kiểm tra server đang bật: mở <https://52-65-234-138.sslip.io/health>, đúng là `{"status":"ok","database":"up"}`. Không mở được thì báo trưởng nhóm bật.
- Mọi người dùng chung một database: dữ liệu (tài khoản, chuyến đi) là dữ liệu thật của nhóm, không tạo hàng loạt tài khoản rác.
- Gõ tiếng Việt trên máy ảo: bàn phím Gboard của máy ảo có thể đang ở kiểu gõ Telex.

## 5. Lỗi thường gặp

| Hiện tượng | Nguyên nhân | Cách xử lý |
| --- | --- | --- |
| Sync Gradle báo cần Android Studio mới hơn | Studio cũ không hỗ trợ AGP 9.4 | Cập nhật Android Studio (mục 1) |
| `Failed to find target with hash string 'android-37'` | Chưa cài SDK Platform 37 | Cài trong SDK Manager (mục 1) |
| Build báo không tìm thấy SDK | `local.properties` thiếu `sdk.dir` | Mở thư mục `android/` bằng Studio để Studio tự tạo, hoặc thêm `sdk.dir=` trỏ tới thư mục Android SDK |
| Đăng nhập/đăng ký báo không có mạng | Thiếu `tripfair.apiBaseUrl`, chưa Sync lại, hoặc server AWS đang tắt | Kiểm tra mục 3 và trang `/health` ở mục 4 |
| Bản đồ xám, không có đường phố | Thiếu `MAPS_API_KEY` hoặc SHA-1 chưa được thêm vào khóa | Mục 3 |
| Không nhận được mã OTP | Email vào Spam, hoặc gửi lại quá sớm (phải chờ 60 giây) | Xem Spam, chờ rồi bấm gửi lại |

### Chạy backend trên máy thay vì AWS

Chỉ cần khi sửa code backend. Cần Docker Desktop và Node.js 24; làm theo [backend/CLAUDE.md](../backend/CLAUDE.md) (copy `backend/.env.example` thành `backend/.env`, `docker compose up -d`, `npm install`, `npx prisma migrate deploy`, `npm run start:dev`). Sau đó bỏ dòng `tripfair.apiBaseUrl` trong `local.properties` và chạy `adb reverse tcp:3000 tcp:3000` mỗi lần mở máy ảo.
