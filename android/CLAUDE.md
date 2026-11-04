# Android (Kotlin + Jetpack Compose)

App Android TripFair. Giao diện viết bằng Jetpack Compose (Material 3), theo thiết kế Figma ở [CLAUDE.md gốc](../CLAUDE.md).
Package `com.tripfair.app`, minSdk 26, compileSdk/targetSdk 37. AGP 9.4.1 (Kotlin tích hợp sẵn trong AGP, bản 2.2.10), Gradle 9.6.0. Phiên bản thư viện khai báo trong `gradle/libs.versions.toml`.

## Thiết lập lần đầu
- Mở thư mục `android/` bằng Android Studio; Studio tự tạo `local.properties` chứa `sdk.dir`.
- Chạy Gradle từ dòng lệnh cần `JAVA_HOME` trỏ tới JDK 17 trở lên (vd. JDK đi kèm Android Studio, thư mục `jbr`) và `local.properties` có `sdk.dir`.

## Lệnh thường dùng (chạy trong `android/`)
- `./gradlew assembleDebug`: build APK debug ra `app/build/outputs/apk/debug/`
- `./gradlew installDebug`: cài APK debug lên máy ảo hoặc điện thoại đang kết nối
- `./gradlew lint`: kiểm tra code bằng Android Lint
- `./gradlew testDebugUnitTest`: chạy unit test (JUnit 4) trong `app/src/test/`

## Cấu trúc code (`app/src/main/java/com/tripfair/app/`)
- `data/`: repository theo từng loại dữ liệu (`data/auth/`…), mỗi cái gồm interface và bản `Sample…` giữ dữ liệu mẫu trong bộ nhớ. `AppContainer.kt` tạo repository một lần (giữ trong `TripFairApplication`); khi nối backend chỉ thay bản `Sample…` bằng bản dùng Retrofit/Room.
- `navigation/`: route của từng màn (`Routes.kt`) và `TripFairNavDisplay.kt` nối nút với màn đích. Dữ liệu chưa chuyển sang `data/` (lộ trình đã lưu, liên hệ SOS) tạm giữ trong `TripFairNavDisplay.kt`.
- `ui/AppViewModelProvider.kt`: factory tạo mọi ViewModel kèm repository chúng cần.
- `ui/theme/`: màu, chữ, bo góc, `@ThemePreviews` (Preview sáng + tối).
- `ui/components/`: component dùng chung (nút chính, ô nhập, checkbox…).
- `ui/screens/<nhóm>/`: màn hình theo nhóm Figma: `auth`, `home`, `explore`, `profile`, `legal`, `group`, `vote`, `trip`, `camera`, `sos`.
- `ui/sample/`: dữ liệu mẫu khi chưa nối backend.

## Quy ước
- Màn hình và component viết bằng Compose; mỗi màn hình có `@Preview` với dữ liệu mẫu.
- Màu, chữ, bo góc lấy từ `MaterialTheme` (`ui/theme/`); không viết mã màu trực tiếp trong màn hình. Màu ngoài vai trò M3 dùng `MaterialTheme.extendedColors`.
- Chữ hiển thị đặt trong `res/values/strings.xml`. Icon là `res/drawable/ic_*.xml` chuyển từ SVG trong Figma.
- Màn hình nhận dữ liệu và callback (`onBack`, `onLogin`…) qua tham số; việc chuyển màn chỉ nằm trong `TripFairNavDisplay.kt`.
- Logic màn hình nằm trong `…ViewModel`, composable màn hình chỉ nhận trạng thái và callback để Preview chạy được. Trạng thái màn hình giữ bằng `mutableStateOf` của Compose để ô nhập chữ cập nhật đồng bộ (theo khuyến nghị của Compose); repository vẫn dùng `StateFlow`. Hàm `…Route` nối ViewModel với màn hình; ViewModel thuộc về từng màn và bị xóa khi rời màn.
- Chỉ xin quyền hệ thống khi tính năng cần (hiện có: vị trí cho SOS). Gọi điện dùng `ACTION_DIAL`, chọn liên hệ dùng bộ chọn danh bạ của hệ thống, để không phải xin `CALL_PHONE` hay `READ_CONTACTS`.
- API key đặt trong `local.properties`, không đưa vào source code.
