# N08 — Bản xem thử Admin UI-M1

Bản xem thử **Swing / Java2D độc lập**, Java 25, không có dependency ngoài JDK.
Không chạy Spring, không cần SQL Server hay biến môi trường chứa thông tin đăng nhập.
Đây là thử nghiệm giao diện; kiến trúc frontend production chưa được phê duyệt.

## Build và chạy

Tại thư mục gốc repository, trong PowerShell đã có `java`, `javac`, `jar` của **JDK 25**:

```powershell
.\admin-desktop-preview\build.ps1 -Test
java -jar admin-desktop-preview/build/admin-desktop-preview.jar --runtime
```

Điểm vào: `com.manage.nhakhoa.desktop.AdminPreview`. Không có Maven parent hoặc module liên kết với backend.
Script biên dịch cả mã chính và kiểm thử với `--release 25 -encoding UTF-8 -Xlint:all -Werror`.
Thư mục `build/` được bỏ qua trong Git, kể cả JAR, lớp biên dịch và ảnh kiểm thử.

Để tái hiện mốc của ảnh `01-tong-quan.png`:

```powershell
java -jar admin-desktop-preview/build/admin-desktop-preview.jar --fixture
```

- `--runtime` (mặc định): Clock hệ thống tại `Asia/Ho_Chi_Minh`, số liệu **vẫn là bộ mẫu** áp dụng cho ngày mô phỏng hiện tại.
- `--fixture`: `Clock.fixed` tại **12/10/2026 07:45** giờ Việt Nam; luôn hiện nhãn “Ảnh mẫu cố định”.
- `Hôm nay` tải lại ngày đang mô phỏng theo Clock của chế độ đã chọn. Trong chế độ ảnh mẫu, tooltip nêu rõ ngày 12/10/2026.
- `Tải lại` đọc lại nguồn mẫu trên SwingWorker và cập nhật thời điểm trả dữ liệu. Clock cố định sẽ giữ nguyên thời gian theo định nghĩa.

Nguồn fixture là `C:/Users/huy/Downloads/menu-admin.zip`, ảnh `menu-admin/ui-xanh-ngoc/01-tong-quan.png`, được đối soát theo hai tài liệu BA ngày 19/10/2026. ZIP không có Java source; toàn bộ Java trong thư mục này được viết mới.
`DemoDashboardDataProvider` là nguồn duy nhất của số mẫu. Không có số nào là kết quả truy vấn SQL Server.
Lịch kiểm tra hộp cấp cứu là ngày mô phỏng trừ 7 ngày **chỉ để minh họa ảnh**; không phải chu kỳ kiểm tra nghiệp vụ được duyệt.

## UI-DEMO-1 (19/10/2026)

Đã có giao diện mô phỏng cho **toàn bộ 21/21 màn** theo menu nghiệp vụ (Tổ chức → Lập kế hoạch → Tiếp nhận → Khám → Điều trị → Thanh toán → Cung ứng → Tổng kết), dùng chung một kho dữ liệu mẫu trong bộ nhớ (`demo/DemoClinicStore` + các slice Staff/Clinical/Billing/Supply).
Dữ liệu dùng chung qua `demo/DemoClinicStore` trong bộ nhớ, không JDBC/REST. Chi tiết, giả định BA và gate nghiệm thu:
[docs/UI-DEMO-1-HANDOVER.md](docs/UI-DEMO-1-HANDOVER.md). Tổng quan vẫn dùng fixture riêng; số liệu seed của store được đối chiếu khớp với Tổng quan.

## Tương tác

- Menu có 21 mục (Tổng quan + 20), **8 nhóm theo giai đoạn nghiệp vụ**: Tổ chức → Lập kế hoạch → Tiếp nhận → Khám → Điều trị → Thanh toán → Cung ứng → Tổng kết; nhấp nhóm để thu/mở. Khi mở nhiều nhóm, menu cuộn riêng để tất cả mục đều tiếp cận được.
- Tìm `lich hen`, `cong no`, `thuoc`, `quan ly`, hoặc mã như `UC14`; tìm không phân biệt dấu và chữ hoa.
- Tab di chuyển focus; mũi tên lên/xuống di chuyển trong menu; Enter/Space kích hoạt; Esc xóa khi focus ở ô tìm.
- KPI, CTA và liên kết chỉ điều hướng trong một CardLayout. Có tooltip đầy đủ cho nhãn bị rút gọn.
- Hai mục `Đổi mật khẩu` và `Đăng xuất` bị vô hiệu hóa, kèm lý do “Chỉ khả dụng khi tích hợp đăng nhập”. Chuông chỉ hiển thị.
- Chọn tình huống trên đầu Dashboard: đầy đủ, rỗng, lỗi, không có việc chờ hoặc không có lịch hẹn. Lỗi/rỗng có nút `Thử lại`; đổi về “Đầy đủ dữ liệu” để phục hồi sau lỗi có chủ đích.
- Đang tải/lỗi/rỗng đều bỏ các KPI cũ; chỉ lần tải mới nhất được render. Tải xong không tự đổi màn hình đang xem.

Badge Dashboard là **9 nhóm việc**, tổng `affectedCount` là **18 đối tượng liên quan**, không bảo đảm 18 hồ sơ duy nhất.
Badge nhóm menu = số nhóm việc trên Tổng quan trỏ tới màn hình trong nhóm: `1 / 1 / 0 / 0 / 1 / 1 / 4 / 1`, tổng 9 (khớp "9 nhóm việc"); vẫn khác 18 lượt đối tượng.

## Kiểm thử

`build.ps1 -Test` chạy kiểm thử không cần màn hình. Để kiểm thêm vòng đời cửa sổ Swing thật, sau khi build, chạy **trong thư mục `admin-desktop-preview`**:

```powershell
java '-Dstdout.encoding=UTF-8' '-Dstderr.encoding=UTF-8' -cp 'build/classes;build/test-classes' com.manage.nhakhoa.desktop.PreviewTests --desktop
```

Kiểm thử có assertion về dữ liệu, router, tìm kiếm, key binding, Clock, lỗi/rỗng, hủy tải cũ, điều hướng trong lúc tải và render ảnh.
Ảnh sinh ra từ `printAll` của component Swing thật; ảnh cửa sổ kiểm thử là phần nội dung JFrame, không bao gồm viền cửa sổ do Windows vẽ.
Không dùng ảnh HTML làm bằng chứng. Các ảnh đã lưu và ma trận nghiệm thu ở [báo cáo bàn giao](HANDOVER.md).

## Ranh giới giai đoạn

20 màn hình ngoài Tổng quan là giao diện MÔ PHỎNG: mọi thao tác (duyệt, hủy, thu tiền, nhập/xuất kho, bàn giao…) chỉ đổi dữ liệu trong bộ nhớ phiên, luôn có nhãn “DỮ LIỆU MÔ PHỎNG”.
Không có duyệt/hủy/mở khóa, tiếp nhận, kê đơn, lâm sàng, đăng nhập hoặc đăng xuất thật.
Không có JPA, JDBC, HTTP, JWT, Spring context hoặc ghi dữ liệu trong preview.
DTO được định nghĩa riêng; Theme không phụ thuộc backend hoặc mô hình nghiệp vụ.

Các đề xuất ở [BA-OPEN-QUESTIONS.md](BA-OPEN-QUESTIONS.md) đều mang nhãn **CHƯA DUYỆT**.
Không dùng tài liệu legacy `README.md`, `ARCHITECTURE.md`, `M1_CONFIGURATION.md` của checkout làm bằng chứng rằng service/security/UI mà chúng mô tả đã tồn tại.
