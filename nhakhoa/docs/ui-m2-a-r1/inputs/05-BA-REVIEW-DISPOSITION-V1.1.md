# N08 — BA Disposition: 10 phát hiện MAJOR của bản rà soát 21 màn hình

**Ngày:** 19/10/2026  
**Tình trạng:** Quyết định đối chiếu BA V1.1 cho công việc tiếp theo; không phải chứng nhận kiểm thử trực quan 21 ảnh.  
**Baseline:** `01-BA-DECISIONS-V1.md`, `02-ROLE-MATRIX-V1.md`, `03-DASHBOARD-API-CONTRACT-V1.md` và mã nguồn GitHub `main` ở commit `4d25dbf88be6415a8fb77facf64b5d4ea9e1f704`.  
**Bằng chứng mới:** báo cáo tóm tắt do người dùng gửi; **không** có 21 ảnh và `BA-REVIEW-UI-ADMIN.md` đầy đủ trong tệp đã cung cấp. Đường dẫn Windows trong báo cáo không chứng minh hệ thống ở đây truy cập được.  
**Quy tắc áp dụng:** Khi không khớp code checkout thực tế, báo sai lệch; không sửa entity/schema để ép khớp mockup. Đây là bổ sung làm rõ, không bãi bỏ quyết định BA V1.

## Disposition theo từng điểm

| # | Phát hiện từ review | BA disposition | Quy tắc cuối và thao tác |
|---|---|---|---|
| 01 | "Bệnh nhân chờ khám" dù đếm phiếu | **ACCEPT nếu còn nhãn cũ** | Đổi thành **"Phiếu chờ khám"**; mỗi PhieuKham đếm 1 theo trạng thái và ngày tiếp nhận. Kiểm xem UI-M1 đã sửa chưa, không sửa hai lần. |
| 02 | Badge 9 / số mỗi dòng / badge sidebar nhập nhằng | **ACCEPT về giải thích UI; không đổi toán học** | 9 là số loại cảnh báo đang hiện; số từng hàng là số đối tượng của loại; badge sidebar là đếm theo màn hình, không phải số duy nhất. Tooltip/legend nói rõ. |
| 03 | 10 lịch gồm hủy và đổi | **PARTIAL — đối chiếu định nghĩa** | Dashboard BA V1 **cố ý đếm bản ghi lịch hẹn đủ 7 trạng thái** trong ngày `thoiGianHen`, gồm DA_HUY và DA_DOI_LICH. Không tự bỏ 2 trạng thái. Nhưng bản ghi có `thoiGianHen` ngoài ngày phải bị loại; chuỗi đổi lịch không được coi hai lần phục vụ thực. Có thể bổ sung "Lịch đang hoạt động" riêng. |
| 04 | Hiện 30/7/14 ngày là hardcoded | **PARTIAL — không phải xung đột BA V1** | BA V1 đã chọn *giá trị mặc định có cấu hình* 30 ngày HSD, 7 ngày kiểm tra cấp cứu, 14 ngày hạng mục điều trị. Giao diện fixture cần ghi "Dữ liệu mô phỏng"; LIVE phải hiển thị ngưỡng thực tế trả từ backend, không hardcode constant trên UI. Không buộc đổi thành từ "theo cấu hình" nếu số hiện hành được trả đúng. |
| 05 | Công nợ gồm hóa đơn chờ duyệt hủy | **REJECT như lỗi tính; ACCEPT nhãn trạng thái riêng** | BA V1 tính mọi HoaDon khác DA_HUY cho đến khi hủy được duyệt. Đơn chờ duyệt không tự xóa nghĩa vụ thanh toán. Có thể cảnh báo ``đang chờ xử lý hủy`` ở một cột riêng. Không âm thầm trừ công nợ chờ duyệt. |
| 06 | "Chờ duyệt hủy" nằm chung cột trạng thái thanh toán | **ACCEPT** | `HoaDon.trangThai` và `YeuCauHuyHoaDon.trangThai` là hai domain khác nhau. Hiển thị 2 cột/2 badge riêng (Trạng thái thanh toán, Yêu cầu hủy) hoặc badge phụ. |
| 07 | Cho check-in LichHen.CHO_XAC_NHAN | **ACCEPT, không cho phép ở V1** | Chỉ check-in trực tiếp khi `DA_XAC_NHAN`; lịch `CHO_XAC_NHAN` phải được xác nhận trước. Backend kiểm tra lại trong transaction; UI disable/giải thích. |
| 08 | "Ma trận quyền" + "Đổi vai trò" bị hiểu thành Permission | **PARTIAL** | Không tạo Permission/ACL. Có thể dùng bảng **"Vai trò và chức năng (tham khảo)"** read-only. **Đổi vai trò tài khoản** là một nghiệp vụ hợp lệ khi AD/quy trình được phép, khác với tạo quyền chi tiết; UI-M1 chưa thực hiện mutation. |
| 09 | Nhà cung cấp: ngừng hợp tác / mã số thuế / địa chỉ | **PARTIAL có xác minh entity** | `NhaCungCap.diaChi` tồn tại; `TrangThaiNhaCungCap.NGUNG_HOP_TAC` tồn tại, nên hỗ trợ hiển thị/thiết kế ngừng hợp tác trong milestone riêng. **Không có trường mã số thuế** ở entity main; loại cột khỏi LIVE hoặc ghi `CHƯA HỖ TRỢ`, không tự thêm DB. |
| 10 | Admin thiếu chọn bác sĩ chịu trách nhiệm ở các form | **PARTIAL — đặt đúng use case** | Thao tác *lâm sàng* với phiếu khám/kế hoạch điều trị cần bác sĩ chịu trách nhiệm thực tế và audit, AD không ký thay. Với hồ sơ bệnh nhân, phân biệt cập nhật nhân khẩu hành chính (không cần BS) và chỉnh sửa thông tin chuyên môn (cần vai trò/trách nhiệm hợp lệ). Không gắn chọn BS bắt buộc cho mọi thao tác đọc/hồ sơ. Đặc tả chi tiết trước mutation. |

## MINOR được báo cáo — xử lý có điều kiện

1. Bảo vệ ADMIN cuối cùng và tài khoản quản trị nền: không cho tự vô hiệu/khóa hoặc nghỉ việc khi vi phạm invariant; tài khoản `NV0000` từ legacy không mặc nhiên hiện diện ở DB mới.
2. SĐT chỉ hiển thị đầy đủ cho vai trò/tác vụ có nhu cầu liên hệ hợp lệ (ví dụ lễ tân tiếp nhận); không mask mù quáng cho mọi role.
3. Năm tiêu chí đánh giá tự đặt: đối chiếu danh mục/quy định BA thực tế trước khi LIVE; không biến nhãn fixture thành chuẩn cố định.
4. Nhật ký không ghi dữ liệu sức khỏe thô, mật khẩu, token, thông tin cá nhân không cần thiết; audit actor, action, object ID, timestamp và outcome tối thiểu.
5. Cột bị cắt: điều chỉnh minWidth/tooltip/scroll/truncation hợp lý, kiểm cả 1280×760 và scale DPI.
6. Bốn mục MINOR còn lại trong báo cáo đầy đủ **chưa được cung cấp**, không tự phỏng đoán.

## Phạm vi và cổng triển khai

- **UI-M1 vẫn là preview với 20 placeholder, chưa là 21 màn hình production.** Không diễn giải phát hiện từ ảnh concept thành bug code chưa tồn tại.
- **UI-M2-A:** triển khai read-only DTO/repository/service/dashboard tests; không làm CRUD, không chỉnh schema, không sửa AUTH-R1; chỉ expose REST endpoint sau khi có auth role AD thực sự đã kiểm thử.
- **UI-BA-G2** (task thiết kế riêng, sau): điều chỉnh bộ 21 ảnh/đặc tả theo disposition, nếu các ảnh đầy đủ thật sự có trong checkout; cung cấp ảnh trước/sau và danh sách màn hình bị ảnh hưởng. Không sửa preview ngoài phạm vi trừ lỗi trực tiếp có thể xác minh.
- **AUTH gate:** mã nguồn `main` quan sát không có AUTH-R1; không mở API admin không được bảo vệ. Build Maven `.m2` trước đó FAIL môi trường, không được kết luận backend PASS.
- Giữ nguyên source/test/fixture hoặc bản ghi đang có; mọi tác động lớn phải có danh sách file và bằng chứng kiểm thử.
