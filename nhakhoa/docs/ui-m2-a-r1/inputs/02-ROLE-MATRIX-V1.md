# N08 — Ma trận vai trò và chức năng V1 (baseline BA)

**Quan trọng:** Đây là chính sách đích đề xuất cho các milestone; **UI-M2-A chỉ triển khai quyền đọc Dashboard của AD**. Không được coi bảng này là lệnh cài đặt toàn bộ 21 màn hình hoặc toàn bộ mutation.

Ký hiệu: `X` xem (có lọc dữ liệu); `T` tác nghiệp theo use case riêng; `Q` quản trị; `—` không có quyền chung. Mọi quyền **phải kiểm ở backend**, giao diện chỉ ẩn/hiện chức năng.

| Màn hình | AD | LT | BS | YT | QL | KHO |
|---|---|---|---|---|---|---|
| Tổng quan quản trị | Q | — | — | — | X* | — |
| Tiếp nhận bệnh nhân | Q | T | X* | T* | X | — |
| Lịch hẹn | Q | T | X* | X* | X | — |
| Hồ sơ bệnh nhân | Q | T* | T* | X* | X* | — |
| Phiếu khám | T* | X* | T* | T* | X* | — |
| Kế hoạch điều trị | T* | X* | T* | X* | X* | — |
| Tái khám | Q | T | T* | X* | X | — |
| Hóa đơn & thanh toán | Q | T | X* | — | Q | — |
| Duyệt hủy hóa đơn | Q* | T* | — | — | Q* | — |
| Bảng giá dịch vụ | Q | X | X | X | Q | X* |
| Hồ sơ nhân viên | Q | — | X* | X* | Q | X* |
| Phân công & lịch làm việc | Q | X* | X* | X* | Q | X* |
| Bàn giao ca | Q* | T* | T* | T* | Q* | T* |
| Đánh giá nhân viên | Q* | — | X* | X* | Q* | X* |
| Thuốc & vật tư | Q | X* | X* | X* | X | T |
| Nhập / xuất / hoàn trả | Q* | — | X* | X* | X* | T |
| Kiểm kê & hao hụt | Q* | — | — | — | Q* | T* |
| Hộp cấp cứu | Q* | — | X* | T* | X | T* |
| Nhà cung cấp | Q | — | — | — | Q | T |
| Tài khoản & vai trò | Q | — | — | — | T* | — |
| Nhật ký thao tác | Q | — | — | — | X* | — |

### Điều kiện dấu `*` không được bỏ qua

- **BS:** chỉ hồ sơ, phiếu khám, kế hoạch đã được giao. **AD có thể điều phối hoặc nhập liệu theo quy trình chuyên môn được xác nhận, phải gắn BS chịu trách nhiệm thực tế và lưu người thao tác thật; AD không được dùng tài khoản quản trị để tự ký thay bác sĩ.** `YT` chỉ hỗ trợ ghi nhận theo quy trình chuyên môn và trong phạm vi nhiệm vụ, **không** chẩn đoán/ký kết luận.
- **LT:** tiếp nhận, hẹn, hóa đơn, đề xuất hủy; không xem toàn bộ tiền sử bệnh chi tiết nếu không cần cho đăng ký khám. Có thể xem tên/lịch liên quan đúng phạm vi tiếp nhận.
- **QL:** xem Dashboard quản trị riêng sau này, quản lý nhân sự, tài chính và phê duyệt được phân công; không tự duyệt việc do mình tạo, không toàn quyền quản trị AD.
- **KHO:** thực hiện các nghiệp vụ kho; không xem hồ sơ lâm sàng ngoài dữ liệu tối thiểu để xuất thuốc đúng đơn.
- **Tài khoản & vai trò:** QL có thể vận hành tài khoản vai trò không phải AD khi có quy trình; chỉ AD có thể cấp/đổi vai trò AD, không thể vô hiệu ADMIN cuối cùng. Mở khóa không có nghĩa đặt lại mật khẩu.
- **Bàn giao/duyệt hủy/kiểm kê:** dù role hợp lệ, người tạo **không** được tự phê duyệt/xác nhận; mỗi hành động lưu tài khoản thật.
- **Nhật ký:** chỉ các trường cần thiết; không hiển thị bí mật, mật khẩu, token hay dữ liệu sức khỏe thô.

### Quyền của UI-M2-A

`GET /api/v1/admin/dashboard` (với context path `/manage` theo checkout) → chỉ tài khoản đã xác thực và hoạt động có `VaiTro.AD`. Thiếu/có token sai → 401, đã đăng nhập nhưng không có AD → 403. Nếu AUTH chưa có hoặc chưa kiểm thử, **không được tạo đường API live có thể truy cập trái phép**, kể cả chỉ bind `localhost`.
