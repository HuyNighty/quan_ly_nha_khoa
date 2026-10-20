# Thanh toán — Lập hóa đơn (bản mô phỏng, 20/10/2026)

## Phạm vi triển khai

Chức năng này được thêm vào **admin-desktop-preview**, màn **Thanh toán → Hóa đơn & thu tiền**. Đây là Swing preview độc lập: **không ghi SQL Server, không gọi REST, không có phân quyền hoặc in hóa đơn điện tử thật**. Không thay đổi các Entity JPA trong `nhakhoa/`. Tính năng chỉ tồn tại trong phiên ứng dụng; chọn **Khôi phục dữ liệu mẫu** sẽ làm mất hóa đơn vừa tạo.

## Hướng dẫn thao tác

1. Mở **Thanh toán → Hóa đơn & thu tiền**, chọn **Lập hóa đơn**.
2. Chọn **bệnh nhân** từ danh sách hồ sơ trong store.
3. Chọn **phiếu khám** nếu có. Chỉ liệt kê phiếu **Đã kết luận**, thuộc bệnh nhân đó và chưa có hóa đơn đang hiệu lực; cũng có thể chọn **Không gắn phiếu khám**.
4. Chọn **dịch vụ** đang áp dụng và nhập **số lượng từ 1–99**, nhấn **Thêm dịch vụ**. Có thể thêm nhiều dòng (tối đa 20 dòng), chọn dòng trong bảng để bỏ khỏi hóa đơn.
5. Kiểm tra **Tổng tạm tính**, nhấn **Lưu hóa đơn**, xác nhận. Hóa đơn tự sinh mã `HDxxxxxx`, xuất hiện trong danh sách và được chọn; trạng thái khởi tạo **Chưa thanh toán**.
6. Sau khi lập có thể **Ghi nhận thu tiền** (tiền mặt / chuyển khoản / POS theo quy tắc bản demo). Có thể thu một phần, nhưng không vượt số nợ.

## Ràng buộc đang có

- Không lập cho bệnh nhân không tồn tại; phiếu khám phải đúng bệnh nhân, đã kết luận, không có hóa đơn chưa hủy.
- Hóa đơn tối thiểu một dịch vụ; không có mã dịch vụ trùng; không nhận số lượng 0, âm, >99, hoặc dịch vụ đã ngừng.
- Khi lưu, tên dịch vụ/đơn giá được **chụp lại theo bảng giá hiện hành** và cố định trong `Invoice.lines`. Điều chỉnh bảng giá sau đó không làm thay đổi hóa đơn.
- Lập hóa đơn **không tự động tạo giao dịch thu tiền**. Công nợ bằng tổng dòng trừ tiền giao dịch thành công. Hóa đơn hủy không tính công nợ.
- Ghi hành động lập hóa đơn vào **Nhật ký thao tác** (mẫu).

## Kiểm thử

Các trường hợp kiểm thử được bổ sung trong `DemoWave2Tests`: dữ liệu thiếu/sai, trùng phiếu khám, dịch vụ ngừng/trùng, tính tiền, đơn giá chốt, công nợ, quá số dư, thanh toán một phần và thao tác tạo→thu trên Swing. Chạy trên máy có **JDK 25**:

```powershell
cd admin-desktop-preview
./build.ps1 -Test
```

**Trạng thái kiểm thử:** đã thêm ca kiểm thử vào source, **chưa chạy JDK 25 trong môi trường chỉnh sửa từ xa**. Cần chạy và kiểm tra giao diện thủ công trước khi merge PR.

## Chưa triển khai (backend thật)

Để đưa nghiệp vụ này vào `nhakhoa/` cần đặc tả riêng và triển khai: API, phân quyền role-only (AD/LT phù hợp), service `@Transactional`, repository JPA cho `HoaDon`/`ChiTietHoaDon` và phiếu khám, ràng buộc chống trùng trên SQL Server, sinh mã an toàn đồng thời, xử lý trạng thái và kiểm toán, tích hợp UI gọi API, cùng xuất/in hóa đơn theo yêu cầu của môn học. **Không lấy trạng thái demo làm bằng chứng hoàn thành backend.**
