# N08 — BA Decision Baseline V1 (Q01–Q16)

**Ngày:** 19/10/2026  
**Phạm vi:** Admin Swing preview `UI-M1` → tầng đọc nghiệp vụ Dashboard `UI-M2-A`; định hướng cho các màn hình còn lại.  
**Trạng thái:** Quy tắc V1 được BA đề xuất/chốt làm **baseline triển khai kỹ thuật theo yêu cầu ủy quyền của người dùng**, **không phải bằng chứng nhóm/phòng khám đã nghiệm thu sản phẩm**.  
**Nguồn:** nội dung `menu-admin.zip`; bàn giao UI-M1 và 16 câu hỏi BA; entity/enums nhánh `main` (GitHub HEAD `4d25dbf88be6415a8fb77facf64b5d4ea9e1f704` kiểm ngày 19/10/2026); `nhakhoa/docs/THIET_KE_DU_LIEU.md` là **tài liệu legacy tham khảo**, không phải mô tả code production mới.  
**Quy tắc ưu tiên:** code checkout thực tế > quyết định V1 trong tài liệu này khi phát hiện sai cấu trúc (báo sai lệch, không tự sửa schema) > legacy docs tham khảo > screenshot. Không được dùng số liệu fixture làm business rule.

## Nguyên tắc hệ thống

- **UI production dự kiến là Swing desktop client qua REST API, Spring Boot thực thi Service + JPA, SQL Server nằm sau backend.** Desktop không dùng JDBC và không giữ mật khẩu SQL Server.
- **Tiếng Việt có dấu** ở nhãn/chức năng/lỗi nghiệp vụ. Tên enum và field Java giữ đúng mã hiện hành.
- `AD` là vai trò Admin thực tế, không phải `ADMIN`. Vai trò: `AD`, `LT`, `BS`, `YT`, `QL`, `KHO`. Không tạo Permission/ACL trong V1.
- Admin xem/điều phối toàn hệ thống nhưng **không được mạo danh bác sĩ** hoặc tự duyệt yêu cầu do chính mình lập. Hành vi nhạy cảm có thêm bất biến nghiệp vụ ngoài role.
- Dữ liệu mock/fixture tuyệt đối tách khỏi live. Không có dữ liệu **không tự động có nghĩa** giá trị 0; phải phân biệt số 0 hợp lệ, nguồn chưa cấu hình và truy vấn lỗi.
- V1 **chỉ cấp quyền triển khai code đọc Dashboard**, không cấp quyền cho chức năng ghi/CRUD/lâm sàng/hủy/duyệt. Phần mô tả quy trình ghi dưới đây là baseline để làm milestone sau, không phải giấy phép code mutation ngay.

## Q01 — Kiến trúc production

**Quyết định:** Chọn mô hình **Swing client → HTTPS REST API → Spring Boot 4.1.1 (Java 25) → Spring Data JPA → SQL Server 2022**. Giữ `admin-desktop-preview/` làm UI shell; thêm adapter HTTP tách khỏi `DemoDashboardDataProvider`. Backend vẫn trong `nhakhoa/`, package `com.manage.nhakhoa`. Không ghép cả Spring Boot vào JVM của Swing, không dựng DAO/JDBC ở UI. Chỉ chuyển sang LIVE khi xác minh authentication/authorization và hợp đồng API; chưa có auth thì chỉ phát triển tầng truy vấn nội bộ + tests.

## Q02 — Múi giờ / ranh giới ngày / đồng hồ

**Quyết định:** `ZoneId.of("Asia/Ho_Chi_Minh")` cho ngày nghiệp vụ. Backend inject `Clock`; ngày `D = LocalDate.now(clock.withZone(zone))`; ngày giao dịch là khoảng `[D 00:00, D+1 00:00)` (**lower inclusive, upper exclusive**). Các entity `LocalDateTime` lưu theo giờ địa phương phòng khám (legacy schema không mang timezone): **không** diễn giải đó là UTC. `generatedAt` trả ISO-8601 kèm offset, `businessDate` trả `yyyy-MM-dd`. Không truy vấn lịch sử công nợ 'as-of' từ một trường trạng thái hiện tại vì dữ liệu không lưu lịch sử trạng thái đầy đủ. Chế độ fixture giữ ngày 12/10/2026 nhưng dán nhãn mô phỏng.

**Ca qua đêm:** Quy thuộc `ngayLam` là ngày bắt đầu ca; khi tính số ca theo ngày dùng khóa `(maCa, ngayLam)`, không đếm ca lần thứ hai sau 00:00. Sự kiện phát sinh sau 00:00 được ghi theo ngày giao dịch thực tế, không theo ngày ca. Nếu thực tế thiếu quy tắc lịch ca hoặc schema không cho ca qua đêm, báo `NOT_SUPPORTED` và không tự thay entity.

## Q03 — Badge / 9 nhóm / 18 đối tượng

**Quyết định:** `workGroupCount` = số **loại cảnh báo có count > 0** được hiển thị; `relatedObjectCount` = tổng số đối tượng/lượt thuộc các dòng cảnh báo (có thể trùng giữa các dòng); `uniqueSubjectCount` chỉ được xuất khi có khóa danh tính tương thích để khử trùng. Badge sidebar là chỉ số theo nghiệp vụ màn hình, **không** lấy trực tiếp từ 9/18 và không tổng hợp các badge lẫn nhau. Dashboard LIVE số nhóm được tính động, không cố định 9; fixture ảnh vẫn 9 dòng, 18 đối tượng. Thứ tự: khẩn → chờ duyệt → theo dõi; mỗi task cần source, count, targetScreen và định nghĩa.

## Q04 — Phiếu chờ khám

**Quyết định:** Tên UI **“Phiếu chờ khám”** thay “Bệnh nhân chờ khám”. Tính theo **số bản ghi PhieuKham** có `thoiGianTiepNhan` trong ngày D, nhóm trạng thái `CHO_KHAM`, `DANG_KHAM`, `DA_KET_LUAN`; `DA_HUY` không thuộc ba nhóm. Mỗi phiếu đếm 1, kể cả cùng bệnh nhân có nhiều lượt; tuyệt đối không gọi là người duy nhất. Đây là các lượt tiếp nhận trong ngày, không phải toàn bộ hàng đợi tồn đọng qua ngày khác; nếu muốn hàng đợi liên ngày cần KPI tách biệt.

## Q05 — Lịch hẹn hôm nay

**Quyết định:** `LichHen.thoiGianHen` trong ngày D. Trả đủ 7 enum `CHO_XAC_NHAN, DA_XAC_NHAN, DA_DEN, HOAN_THANH, VANG_MAT, DA_HUY, DA_DOI_LICH`, kể cả giá trị 0; tổng = tổng số **bản ghi lịch hẹn** theo thời gian hẹn, không phải số bệnh nhân duy nhất. Lịch đổi: bản cũ `DA_DOI_LICH` giữ lịch sử, bản mới tạo với `lichHenGoc`, trạng thái ban đầu `CHO_XAC_NHAN`. Hai bản nếu cùng ngày được tính riêng như hai bản ghi; UI chú thích rõ không đồng nghĩa hai lượt thực tế. Các thống kê lịch **đang hoạt động** chỉ tính các trạng thái mở (`CHO_XAC_NHAN`, `DA_XAC_NHAN`, `DA_DEN`); không dùng tổng 7 trạng thái để kết luận công suất thực tế. Quy tắc chuyển trạng thái cho thao tác ghi ở Q16.

## Q06 — Đã thu hôm nay

**Quyết định:** Tổng `ThanhToan.soTien` các giao dịch `THANH_CONG`, `thoiGian` thuộc ngày D; phân nhóm đủ ba phương thức `TIEN_MAT`, `CHUYEN_KHOAN`, `THE_POS`, 0 nếu không phát sinh. `DA_HUY` không được cộng. Tên KPI **“Thu hợp lệ trong ngày”**, không gọi doanh thu thuần/kế toán. Nếu giao dịch của ngày cũ bị hủy hôm sau, số ngày cũ thay đổi khi truy vấn lại — mô hình hiện tại là **số đã đối soát theo trạng thái hiện tại**, không phải snapshot đã khóa sổ. Hiện chưa có Refund/CreditNote rõ ràng; **không** tự tính bù trừ khoản hoàn tiền/chênh lệch không có dữ liệu. Nếu phát hiện hóa đơn DA_HUY nhưng có thu thành công, ghi anomaly chứ không âm thầm sửa.

## Q07 — Công nợ

**Quyết định:** Với từng hóa đơn không có `DA_HUY`:

`invoiceTotal = SUM(ChiTietHoaDon.soLuong * donGiaApDung)`;
`paid = SUM(ThanhToan.soTien WHERE trangThai = THANH_CONG)`;
`receivable = max(invoiceTotal - paid, 0)`;
`invoiceHasDebt = receivable > 0`.

Dashboard `congNo = SUM(receivable)` và `soHoaDonConNo = COUNT(invoiceHasDebt)` trên **tất cả hóa đơn còn hiệu lực tại thời điểm query**, không gắn ngày D. Tính từng hóa đơn trước rồi mới cộng; không nhân chéo dòng chi tiết và dòng thanh toán (aggregate riêng). Giá VND `BigDecimal` scale 0, không `float`/`double`. Trả thừa và `paid > invoiceTotal` là **anomaly** cần xử lý, không trở thành công nợ âm. Chiết khấu/giảm giá/chứng từ điều chỉnh chưa được entity hỗ trợ: chưa phản ánh, phải hiển thị hạn chế này trong tài liệu; không tự thêm cột/logic.

## Q08 — Phân công và thiếu nhân sự

**Quyết định:** `CaLamViec` xác định định mức `soBacSi`, `soYTa`, `soLeTan`. Với mỗi `PhanCong.ngayLam = D`, trạng thái `CHO_HIEU_LUC`, `DANG_THUC_HIEN`, `HOAN_THANH`, nhân viên `DANG_LAM_VIEC`, tính **DISTINCT maNhanVien** theo `NhanVien.chucDanh` là `BS`/`YT`/`LT`; loại `DA_HUY`. `missing = max(required - assigned, 0)`. Đếm theo **lịch phân công**, không gọi là chấm công/đã có mặt. Ca không có phân công vẫn báo thiếu nếu định mức dương. Nếu phân công một người đa ca hợp lệ, người đó được tính ở từng ca, không đếm trùng trong cùng ca. Ca qua đêm gán theo ngày bắt đầu như Q02. Nếu giờ ca bất hợp lệ, đánh dấu lỗi cấu hình.

## Q09 — Tồn kho dưới định mức / số lô

**Quyết định:** Tồn khả dụng của **mỗi mã Thuoc/VatTu** = tổng `LoHang.soLuongTon > 0` với `hanSuDung > D` (ngày hết hạn D **không** còn sử dụng), và có quan hệ chính xác một trong `thuoc`/`vatTu`. Lô không có HSD được phân loại **“Chưa xác định hạn dùng”** và *không tính vào tồn khả dụng* trong V1 bảo thủ; nhóm quản trị có thể bổ sung phân loại hàng không yêu cầu HSD ở phiên bản sau. Mặt hàng dưới định mức khi `dinhMucToiThieu > 0 AND availableStock < dinhMucToiThieu`; đếm **mã mặt hàng duy nhất** (không phải lô). Vật tư/thuốc không có lô = tồn 0. Dữ liệu âm hay lô trỏ đồng thời thuốc/vật tư là bất thường, không làm tròn thành 0. Số lô cận hạn/hết hạn chỉ tính lô có `soLuongTon > 0`.

## Q10 — Cận hạn / hết hạn

**Quyết định:** Dùng tham số `SO_NGAY_CANH_BAO_HSD` (tên khóa V1 đề nghị) mặc định **30 ngày** từ baseline legacy, lưu trong `ThamSoHeThong` khi nghiệp vụ cấu hình được triển khai; nếu thiếu khóa, service dùng fallback 30 ngày và **ghi cảnh báo cấu hình**, không tự ghi database. `expired`: `hanSuDung <= D`; `nearExpiry`: `D < hanSuDung <= D.plusDays(30)`. Số lô là số `maLo` distinct. Không tính lô rỗng, null HSD tách trạng thái Unknown. Ngày HSD đúng hôm nay được xếp **hết hạn** để tránh sử dụng sản phẩm quá hạn trong ngày theo quy định V1 bảo thủ.

## Q11 — Hộp cấp cứu

**Quyết định:** Chu kỳ kiểm tra mặc định **7 ngày** (`CHU_KY_KIEM_TRA_CAP_CUU_NGAY`); yêu cầu hạn dùng còn **hơn 30 ngày** (`SO_NGAY_AN_TOAN_CAP_CUU`); cả hai là tham số có thể cấu hình trong milestone sau. Lấy bản `KiemTraCapCuu.thoiGian` gần nhất <= hiện tại, trạng thái `CAN_BO_SUNG` → cần xử lý; thiếu bản kiểm tra hoặc quá 7 ngày → **CẦN KIỂM TRA**, không gọi `DAT`. Nếu danh mục không có mục `dangApDung=true, laDuLieuMau=false`, trạng thái **CHƯA CẤU HÌNH DANH MỤC THẬT**, không hiển thị “đạt”. Những mục mẫu có `laDuLieuMau=true` chỉ được xem ở demo, không dùng chứng nhận an toàn lâm sàng. Chưa cho phép giao diện kết luận hộp cấp cứu thực sự an toàn dựa trên kết quả kiểm tra mẫu.

## Q12 — Tái khám / điều trị đến hạn

**Quyết định:** V1 gắn nhãn chính xác **“Kế hoạch điều trị đến hạn”**, không tự khẳng định đó là cuộc hẹn tái khám đã đặt. Nguồn `ChiTietDieuTri.trangThai=CHUA_THUC_HIEN`, `KeHoachDieuTri.trangThai=DANG_THUC_HIEN` với `ngayDuKien <= D.plusDays(14)` (gồm hạng mục quá hạn). Đếm **DISTINCT maKeHoach** theo kế hoạch có ít nhất một hạng mục phù hợp, không phải số dòng hay số lịch; `overduePlans` khi tồn tại hạng mục `ngayDuKien < D`. Có lịch còn hiệu lực `LichHen.keHoach=...` thì hiển thị nhãn “Đã có lịch”, không tự xóa khỏi danh sách vì dữ liệu hiện tại không liên kết tới từng `ChiTietDieuTri`. Ngưỡng 14 ngày dùng từ giả định legacy, về sau cấu hình được. Không gửi thông báo y tế hay tự thay đổi trạng thái hẹn.

## Q13 — Vai trò và giới hạn quyền

**Quyết định:** Role-only V1, quản lý menu theo role chỉ là UX. Backend yêu cầu xác thực và kiểm soát quyền trên **từng endpoint**. V1 Dashboard Admin cần `AD`; `QL` chỉ được mở dashboard quản lý nếu hợp đồng riêng được duyệt sau. Các màn hình còn lại dùng ma trận trong `02-ROLE-MATRIX-V1.md`. AD được mở và quản trị các miền nghiệp vụ nhưng **không được ký kết luận chuyên môn thay BS**, không tự duyệt, không vượt qua ràng buộc nguồn. Bác sĩ chịu trách nhiệm phải được xác thực và liên kết thực tế, không chỉ điền tên vào form. Mọi truy cập hồ sơ nhạy cảm tuân thủ tối thiểu dữ liệu và audit.

## Q14 — State machine / bất biến giao dịch ghi (để UI-M3+)

- **Không tự duyệt:** `YeuCauHuyHoaDon` / phiếu điều chỉnh kho / xác nhận bàn giao: người duyệt khác người tạo, bất kể `AD`; transaction + so khớp trạng thái hiện hành + audit.
- **Hủy lịch:** chỉ lịch còn mở (`CHO_XAC_NHAN`, `DA_XAC_NHAN`) được hủy trực tiếp với lý do; `DA_DEN` phải chuyển sang quy trình xử lý check-in/phiếu khám, không tùy tiện hủy; trạng thái cuối (`HOAN_THANH, VANG_MAT, DA_HUY, DA_DOI_LICH`) không hủy lần nữa.
- **Đổi lịch:** chỉ lịch còn mở; ghi bản cũ `DA_DOI_LICH`, tạo bản mới `CHO_XAC_NHAN` + `lichHenGoc`; cùng transaction, không làm mất lịch sử.
- **Hủy phiếu khám:** chỉ `CHO_KHAM`, có lý do; không xóa vật lý.
- **Thanh toán:** `THANH_CONG -> DA_HUY` theo quy trình hủy có quyền/lý do/audit; không xóa giao dịch; không thu vượt công nợ; mã CK/POS bắt buộc và unique cho giao dịch hợp lệ. Chưa phát triển quy trình refund tự động.
- **Tài khoản:** `HOAT_DONG <-> BI_KHOA`; `VO_HIEU` không tự mở khóa. Không tự khóa/vô hiệu tài khoản hiện tại; bảo vệ admin cuối cùng khỏi tự vô hiệu; mọi thay đổi role/khóa ghi audit.
- **Concurrency:** xác nhận trạng thái có điều kiện khi UPDATE; 409 cho conflict, rollback atomic, request trùng không gây tạo hai bản; audit chỉ sau khi giao dịch thành công. Đây là baseline để viết thêm đặc tả chi tiết trước khi code ghi.

## Q15 — Dữ liệu bệnh nhân và hành vi nhạy cảm

**Quyết định:** Không gửi tên/số điện thoại/ngày sinh/chẩn đoán/đơn thuốc vào Dashboard aggregate. Hạn chế DTO theo chức năng, không trả nguyên Entity JPA. Không log request/response có PII/PHI, mật khẩu, token; log correlationId + loại lỗi ẩn danh. Lỗi 401/403 không lộ dữ liệu, UI phân biệt 401 cần đăng nhập lại và 403 không có quyền, không fallback sang mock. Mọi thao tác lâm sàng phải do `BS` được phân công thực hiện (hoặc qua quy trình ủy quyền chuyên môn được xác nhận riêng); `AD` không được mạo danh chuyên môn chỉ vì có quyền quản trị hệ thống. Không đưa hồ sơ thật vào ảnh minh họa/test snapshot.

## Q16 — Đặc tả Tiếp nhận và Lịch hẹn V1 cho UI-M3, chưa code mutation

**Tiếp nhận:** tìm bệnh nhân bằng mã/SĐT/họ tên; cảnh báo nghi trùng nếu trùng SĐT **hoặc** họ tên đã chuẩn hóa + ngày sinh, nhưng không tự gộp (người thân có thể chung số). Tạo mới `BenhNhan` + `HoSoBenhAn` trong một transaction nếu chưa tồn tại; giới hạn dữ liệu bắt buộc theo Entity; `PhieuKham` liên kết bệnh nhân, hồ sơ, bác sĩ và lịch hẹn nếu có. Check-in chỉ từ lịch `DA_XAC_NHAN` chuyển thành `DA_DEN`, không tạo thêm phiếu khám cho cùng `maLichHen` nếu đã có phiếu đang hiệu lực. Tiếp nhận trực tiếp cho bệnh nhân không có lịch được cho phép nếu đã chọn bác sĩ phù hợp; chặn tạo phiếu khám trùng cho cùng bệnh nhân/bác sĩ/ngày khi phiếu trước còn `CHO_KHAM` hoặc `DANG_KHAM`, phải giải quyết rõ trước (không chặn nhiều lần khám hợp lệ đã hoàn thành). Nếu bác sĩ chưa phân công được thì chưa tạo `PhieuKham` vì Entity bắt buộc có bác sĩ.

**Lịch hẹn:** tạo với bác sĩ đang làm việc, ngày giờ không ở quá khứ, trạng thái `CHO_XAC_NHAN`; xác nhận → `DA_XAC_NHAN`, đến khám → `DA_DEN`, kết thúc lượt khám mới → `HOAN_THANH`; vắng mặt chỉ từ `CHO_XAC_NHAN` / `DA_XAC_NHAN` sau thời điểm hẹn có ngưỡng vắng mặt do nghiệp vụ cấu hình (V1 đề xuất **30 phút** sau lịch hẹn). Tạm quy định **slot 30 phút** cho mỗi lịch hẹn (do entity chưa có duration); ngăn xung đột bác sĩ theo slot nửa mở `[t,t+30p)`, không dùng chỉ so sánh đúng một timestamp; giải pháp production đòi kiểm tra/ràng buộc transaction chống race condition, và có thể cần migration được duyệt riêng. Lịch đổi giữ bản cũ như Q05, không sửa `thoiGianHen` trên bản cũ. Hủy yêu cầu lý do. Không mặc định hành vi bệnh nhân tự đặt hẹn vì chưa có kênh patient portal.

## Điều kiện trước khi chuyển sang production

1. **AUTH-R1 phải được xác minh trên checkout triển khai.** Nhánh GitHub main `4d25dbf` hiện không có Spring Security/JWT/login; báo cáo ADMIN bootstrap PASS ở một checkout khác không đủ để suy ra đã tích hợp. Không mở endpoint dashboard trả số liệu thật khi thiếu kiểm soát `AD`.
2. **Kiểm tra file `.env` đang được Git theo dõi** (nhánh main có `nhakhoa/.env`). Không đọc/ghi lại nội dung secrets vào log/bàn giao; nếu file từng chứa credential thực, người quản lý phải xoay khóa/mật khẩu và xử lý lịch sử Git theo quy trình riêng, không đẩy thêm secret.
3. Chỉ đánh dấu Dashboard LIVE nếu SQL Server đọc thành công, nguồn định nghĩa có thật, identity từ backend đã được kiểm chứng. Không tự dán nhãn LIVE cho fixture.
4. UI-M1 chờ PO kiểm tra trực quan; báo cáo Codex 105/105 chỉ là bằng chứng do Codex cung cấp, không phải nghiệm thu độc lập.
