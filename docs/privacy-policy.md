# Chính sách quyền riêng tư của TripFair (bản nháp)

> **Bản nháp, chưa công bố.** Trước khi đưa lên Google Play, nhóm cần: điền các mục `<…>`, đối chiếu với quy định bảo vệ dữ liệu cá nhân của Việt Nam (vd. Nghị định 13/2023/NĐ-CP), và đăng lên một địa chỉ web công khai (Google Play yêu cầu đường dẫn tới chính sách này và tới trang hướng dẫn xóa tài khoản). Nội dung dưới đây mô tả đúng những gì backend hiện tại làm; khi app thay đổi cách dùng dữ liệu thì phải sửa chính sách này.

Cập nhật lần cuối: `<ngày công bố>`

TripFair là ứng dụng gợi ý địa điểm và lập lộ trình du lịch theo ngân sách, do `<tên nhóm / người phát triển>` phát triển. Chính sách này giải thích TripFair thu thập dữ liệu gì, dùng vào việc gì, chia sẻ với ai, lưu bao lâu và bạn có quyền gì.

## 1. Dữ liệu chúng tôi thu thập

| Dữ liệu | Khi nào | Dùng để |
| --- | --- | --- |
| Họ tên, email hoặc số điện thoại | Khi bạn đăng ký | Tạo tài khoản, đăng nhập, gửi mã xác minh |
| Mật khẩu | Khi bạn đăng ký hoặc đổi mật khẩu | Đăng nhập. Chỉ lưu dạng băm (scrypt), không ai đọc được mật khẩu gốc |
| Mã xác minh (OTP) | Khi đăng ký hoặc quên mật khẩu | Xác minh bạn là chủ email/số điện thoại. Lưu dạng băm, hết hạn sau 15 phút |
| Từ khóa tìm kiếm | Khi bạn tìm kiếm có nhập chữ | Hiện lịch sử tìm kiếm cho bạn. Giữ tối đa 50 mục gần nhất; bạn xóa được từng mục hoặc tất cả |
| Địa điểm yêu thích | Khi bạn lưu một địa điểm | Hiện danh sách yêu thích |
| Chuyến đi: thành phố, ngày, ngân sách, sở thích, phiếu bình chọn, chỗ ở, lộ trình | Khi bạn tạo hoặc tham gia chuyến đi | Lập lộ trình và chia chi phí. **Ngân sách tối đa của bạn chỉ bạn thấy**; thành viên cùng nhóm thấy tên, sở thích, phần chi và mức hài lòng của bạn |
| Liên hệ khẩn cấp: tên, mối quan hệ, số điện thoại | Khi bạn thêm liên hệ | Gửi tin SOS khi bạn cần. Hãy chỉ thêm người đã đồng ý nhận tin |
| Vị trí | Khi bạn tìm địa điểm gần đây hoặc dùng SOS | Tìm kết quả quanh bạn; gửi vị trí trong tin SOS. Server không lưu vị trí của bạn |
| Ảnh chụp | Khi bạn dùng nhận diện địa điểm | Gửi tới Google Cloud Vision để nhận diện địa danh. Ảnh không được lưu |
| Địa chỉ IP, thời điểm truy cập | Mỗi lần app gọi server | Chống lạm dụng (giới hạn số yêu cầu) và tìm lỗi. Nhật ký server được xoay vòng, chỉ giữ phần gần nhất |

TripFair không thu thập danh bạ, không đọc tin nhắn, không dùng dữ liệu của bạn cho quảng cáo và không bán dữ liệu.

## 2. Chia sẻ với bên thứ ba

| Bên nhận | Nhận gì | Để làm gì |
| --- | --- | --- |
| Google (Places API, Routes API, Cloud Vision API) | Từ khóa tìm kiếm, tọa độ khi tìm gần đây, tọa độ các điểm trong lộ trình, ảnh nhận diện | Tìm địa điểm, tính thời gian di chuyển, nhận diện địa danh. Xem [Chính sách quyền riêng tư của Google](https://policies.google.com/privacy) |
| Google Gmail (SMTP) | Email của bạn và mã xác minh | Gửi email chứa mã OTP |
| Amazon Web Services (khu vực Sydney, Úc) | Toàn bộ dữ liệu ở mục 1 được lưu trên máy chủ và cơ sở dữ liệu thuê của AWS | Chạy dịch vụ TripFair |
| `<nhà cung cấp SMS, khi có>` | Số điện thoại và mã xác minh | Gửi mã OTP qua SMS |

Tin SOS được điện thoại của bạn gửi trực tiếp tới liên hệ khẩn cấp bằng SMS, không đi qua server TripFair.

## 3. Lưu trữ và bảo mật

- Dữ liệu được lưu trên AWS tại Sydney (Úc). Kết nối giữa app và server dùng HTTPS; kết nối giữa server và cơ sở dữ liệu được mã hóa (TLS); ổ đĩa cơ sở dữ liệu được mã hóa.
- Cơ sở dữ liệu không mở ra Internet, chỉ máy chủ TripFair kết nối được.
- Dữ liệu được giữ cho tới khi bạn xóa (lịch sử, yêu thích, liên hệ) hoặc xóa tài khoản. Bản sao lưu tự động của cơ sở dữ liệu được giữ tối đa 7 ngày rồi tự xóa, nên dữ liệu đã xóa có thể còn trong bản sao lưu tới 7 ngày.

## 4. Quyền của bạn

- **Xem và sửa** hồ sơ (họ tên) trong app.
- **Xóa** từng mục lịch sử tìm kiếm, yêu thích, liên hệ khẩn cấp; rời nhóm bất cứ lúc nào.
- **Xóa tài khoản** trong app: `<đường dẫn trong app, vd. Tài khoản → Xóa tài khoản>`, nhập lại mật khẩu để xác nhận. Tài khoản, lịch sử, yêu thích, liên hệ khẩn cấp, phiên đăng nhập và các chuyến đi chỉ có bạn bị xóa ngay. Nhóm còn người khác vẫn tiếp tục; nếu bạn là chủ nhóm, người vào nhóm sớm nhất trở thành chủ nhóm. Không đăng nhập được nữa thì gửi yêu cầu xóa tới `<email liên hệ>` từ email hoặc số điện thoại của tài khoản.
- **Hỏi về dữ liệu** của bạn qua `<email liên hệ>`; chúng tôi trả lời trong `<số ngày>` ngày.

## 5. Trẻ em

TripFair không dành cho trẻ dưới `<độ tuổi, vd. 13>` tuổi và không cố ý thu thập dữ liệu của trẻ em. Nếu phát hiện tài khoản của trẻ em, chúng tôi sẽ xóa tài khoản đó.

## 6. Thay đổi chính sách

Khi chính sách thay đổi, ngày cập nhật ở đầu trang sẽ đổi và app sẽ thông báo nếu thay đổi ảnh hưởng tới cách dùng dữ liệu của bạn.

## 7. Liên hệ

`<tên nhóm / người phát triển>`, `<email liên hệ>`.
