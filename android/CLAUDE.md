# Android (Kotlin + Jetpack Compose)

App Android TripFair. Giao diện viết bằng Jetpack Compose (Material 3), theo thiết kế Figma ở [CLAUDE.md gốc](../CLAUDE.md).
Package `com.tripfair.app`, minSdk 26, compileSdk/targetSdk 37. AGP 9.4.1 (Kotlin tích hợp sẵn trong AGP, bản 2.2.10), Gradle 9.6.0. Phiên bản thư viện khai báo trong `gradle/libs.versions.toml`.

## Thiết lập lần đầu
- Mở thư mục `android/` bằng Android Studio; Studio tự tạo `local.properties` chứa `sdk.dir`.
- Chạy Gradle từ dòng lệnh cần `JAVA_HOME` trỏ tới JDK 17 trở lên (vd. JDK đi kèm Android Studio, thư mục `jbr`) và `local.properties` có `sdk.dir`.
- Địa chỉ backend: thêm `tripfair.apiBaseUrl=...` vào `local.properties` (kết thúc bằng `/api/v1/`). Không khai báo thì dùng `http://localhost:3000/api/v1/`, tức backend đang chạy trên chính máy tính; trước khi mở app chạy `adb reverse tcp:3000 tcp:3000` (dùng được cho cả máy ảo và điện thoại cắm USB, chạy lại mỗi lần khởi động thiết bị). Không dùng `10.0.2.2`: Android 17 chặn app gọi địa chỉ mạng nội bộ khi chưa có quyền mạng cục bộ, còn `localhost` thì không bị chặn. Bản debug cho phép HTTP tới `localhost` và `10.0.2.2`; mọi địa chỉ khác phải là HTTPS.

## Lệnh thường dùng (chạy trong `android/`)
- `./gradlew assembleDebug`: build APK debug ra `app/build/outputs/apk/debug/`
- `./gradlew installDebug`: cài APK debug lên máy ảo hoặc điện thoại đang kết nối
- `./gradlew lint`: kiểm tra code bằng Android Lint
- `./gradlew testDebugUnitTest`: chạy unit test (JUnit 4) trong `app/src/test/`

## Cấu trúc code (`app/src/main/java/com/tripfair/app/`)
- `data/`: repository theo từng loại dữ liệu, mỗi cái gồm interface và bản `Remote…` gọi backend bằng Retrofit: `auth/` (tài khoản), `places/` (tìm kiếm, chi tiết, lịch sử, yêu thích), `contacts/` (liên hệ khẩn cấp, kèm bản sao trong DataStore để SOS dùng khi mất mạng), `trips/` (chuyến đi, chỗ ở, bình chọn, lộ trình). `AppContainer.kt` tạo repository một lần (giữ trong `TripFairApplication`).
- `data/network/`: tạo OkHttp/Retrofit, `ApiResult` và `apiCall` đổi lỗi HTTP thành `ApiError` theo `code` của backend (mất mạng là `NETWORK_UNAVAILABLE`). Màn hình chọn thông báo theo `code`.
- `data/session/`: phiên đăng nhập. Token lưu trong DataStore, mã hóa bằng khóa Android Keystore; `AuthInterceptor` gắn access token, `TokenAuthenticator` tự làm mới token khi gặp 401 và đăng xuất khi refresh token bị từ chối.
- `navigation/`: route của từng màn (`Routes.kt`, các màn của một chuyến đi mang `tripId`) và `TripFairNavDisplay.kt` nối nút với màn đích. Màn nào cần đăng nhập thì tự quay về Đăng nhập khi phiên kết thúc.
- `ui/AppViewModelProvider.kt`: factory tạo mọi ViewModel kèm repository chúng cần.
- `ui/theme/`: màu, chữ, bo góc, `@ThemePreviews` (Preview sáng + tối).
- `ui/components/`: component dùng chung (nút chính, ô nhập, checkbox…).
- `ui/screens/<nhóm>/`: màn hình theo nhóm Figma: `auth`, `home`, `explore`, `profile`, `legal`, `group`, `vote`, `trip`, `camera`, `sos`.
- `ui/sample/`: dữ liệu mẫu cho `@Preview` và cho các màn chưa nối backend (nhận diện ảnh, đánh giá địa điểm, phần vị trí và gửi tin của SOS).

## Quy ước
- Màn hình và component viết bằng Compose; mỗi màn hình có `@Preview` với dữ liệu mẫu.
- Màu, chữ, bo góc lấy từ `MaterialTheme` (`ui/theme/`); không viết mã màu trực tiếp trong màn hình. Màu ngoài vai trò M3 dùng `MaterialTheme.extendedColors`.
- Chữ hiển thị đặt trong `res/values/strings.xml`. Icon là `res/drawable/ic_*.xml` chuyển từ SVG trong Figma.
- Màn hình nhận dữ liệu và callback (`onBack`, `onLogin`…) qua tham số; việc chuyển màn chỉ nằm trong `TripFairNavDisplay.kt`.
- Logic màn hình nằm trong `…ViewModel`, composable màn hình chỉ nhận trạng thái và callback để Preview chạy được. Màn tải dữ liệu có ba trạng thái: đang tải, lỗi kèm nút thử lại, và rỗng (`ui/components/LoadStates.kt`). Trạng thái màn hình giữ bằng `mutableStateOf` của Compose để ô nhập chữ cập nhật đồng bộ (theo khuyến nghị của Compose); repository vẫn dùng `StateFlow`. Hàm `…Route` nối ViewModel với màn hình; ViewModel thuộc về từng màn và bị xóa khi rời màn.
- Chỉ xin quyền hệ thống khi tính năng cần (hiện có: vị trí cho SOS). Gọi điện dùng `ACTION_DIAL`, chọn liên hệ dùng bộ chọn danh bạ của hệ thống, để không phải xin `CALL_PHONE` hay `READ_CONTACTS`.
- API key đặt trong `local.properties`, không đưa vào source code.
