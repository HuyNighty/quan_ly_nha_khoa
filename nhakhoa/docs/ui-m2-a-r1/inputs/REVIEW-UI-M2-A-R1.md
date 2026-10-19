# N08 — Independent review UI-M2-A / R1
Ngày: 19/10/2026

## Căn cứ và giới hạn
- Đã đọc `BAN-GIAO-UI-M2-A.md`, kiểm tra 2 patch (SHA-256 giống nhau), giải nén bản vá bằng `git apply --check` vào thư mục kiểm tra sạch và đọc code main/test của `features/dashboard`.
- Đối chiếu BA V1 và REST contract đã cấp trước đó. Không có bằng chứng thực hiện Maven/Spring/Hibernate/SQL Server thực trong phiên review này. Không giả định patch đã được merge hoặc có trong checkout mới.
- `31/31 PASS` là test do người thực hiện báo cáo, chạy trên JDK25 với các API stub tối giản. `JpaDashboardReadRepositoryTest` dùng proxy EntityManager kiểm tra cấu trúc chuỗi, không xác nhận cú pháp Hibernate/SQL Server.

## Disposition
`UI-M2-A IMPLEMENTED — STATIC REVIEW WITH REQUIRED FOLLOW-UPS — INTEGRATION NOT VERIFIED`. Có thể giữ bản vá làm ứng viên R1, không đóng UI-M2-A production và không bật REST LIVE.

## Điều đúng nên giữ
1. `Clock` Asia/Ho_Chi_Minh và ngày nửa mở `[00:00, ngày kế tiếp 00:00)`.
2. Lịch hẹn 7 trạng thái + `activeCount`; KPI `PhieuKham` dựa trên phiếu; thanh toán chỉ `THANH_CONG` theo 3 phương thức.
3. Công nợ aggregate riêng tiền hóa đơn và tiền đã thu theo từng hóa đơn, không nhân JOIN; hủy hóa đơn loại, chờ duyệt hủy vẫn tính; thu vượt được báo bất thường.
4. Phân công distinct nhân viên theo ca/vai trò, tách tồn kho không rõ HSD, kế hoạch distinct theo mã kế hoạch.
5. DTO bất biến, không đưa Entity/PII/PHI lên Dashboard; không tự tạo Controller, JWT hoặc mở quyền public.

## Các việc cần xử lý trước khi xác nhận tích hợp

### P0-1. Build thật và JPQL integration
- Ở máy JDK25+Maven có dependencies: thực thi `mvnw.cmd` / `mvn` với 3 lớp test. Tách rõ `compile`, `test-compile`, `test`, và `contextLoads` (DB thật).
- Chạy từng query trong `JpaDashboardReadRepository` qua Hibernate thật + SQL Server test (không dùng DB production). Bao phủ query `sum(ct.donGiaApDung * ct.soLuong)`, `count(distinct...)`, joins, enum, LocalDateTime, null, empty aggregates. Các test proxy chỉ kiểm string.
- Cấm thay Spring/Hibernate/JPA bằng stub trong kết quả nghiệm thu thật.

### P0-2. Xác nhận failure isolation của các section phụ
- `DashboardQueryService` và `JpaDashboardReadRepository` đều khai báo `@Transactional(readOnly=true)`; section phụ bắt `RuntimeException` và định trả `UNAVAILABLE`.
- Nếu repository tham gia transaction chung rồi ném RuntimeException, transaction có thể đã `rollback-only`; response `UNAVAILABLE` có thể bị `UnexpectedRollbackException` khi commit. Đây là RỦI RO CẦN TEST, chưa kết luận đã lỗi ở runtime.
- Viết integration test Spring thật: ép lỗi query staffing/inventory/emergency/treatment; kiểm tra response section UNAVAILABLE mà các core KPI vẫn trả đúng. Nếu lỗi, thiết kế lại ranh giới read-transaction thay vì bắt ngoại lệ trong cùng transaction. Lỗi core vẫn phải fail toàn trang.

### P0-3. Độ phủ kiểm tra hộp cấp cứu
- Hiện code chỉ xác nhận có ít nhất 1 mục danh mục chính thức đang áp dụng, rồi lấy `KiemTraCapCuu` mới nhất toàn hệ thống và đếm các chi tiết `dat=false`/cận HSD.
- Có nguy cơ trả `TRONG_CHU_KY` dựa trên lần kiểm tra cũ chỉ chứa danh mục mẫu hoặc bỏ sót một mục chính thức mới bổ sung. Không được coi đây là bằng chứng danh mục chính thức đã được kiểm hết.
- Yêu cầu kiểm tra bao phủ tất cả mục chính thức `dangApDung=true, laDuLieuMau=false` trong lần kiểm tra, phù hợp dữ liệu/thời điểm; thiếu coverage phải hiển thị `CẦN KIỂM TRA`/`CHƯA ĐỦ THÔNG TIN`, tuyệt đối không suy thành kết luận an toàn. Xem xét HSD null theo loại mục và dữ liệu được hỗ trợ. Viết test mixed mock/real, newly-added item, zero details, missing expiry.

### P1-1. Ghi rõ overlap của KPI
- `duePlanCount` bao gồm `overduePlanCount`; work queue có thể hiển thị cả hai nhóm với cùng kế hoạch, tổng `relatedObjectCount` có thể trùng. Đây là hợp đồng BA được phép, nhưng yêu cầu tooltip/label 'đến hạn (bao gồm quá hạn)' và ghi rõ tổng đối tượng là lượt liên quan, không phải unique.

### P1-2. Chuẩn hóa contract/keys (BA quyết định)
- **Chấp nhận ở V1.1** những DTO bổ sung sau (vì API chưa phát hành): `appointments.activeCount`, `receivables.overpaidInvoiceCount`, `staffing.understaffedShiftCount`, `inventory.anomalyLotCount`, `emergencyKit.checkState`, `emergencyKit.checkCycleDays`, `warnings[]` với `{code,section,message}`. Duy trì tính tương thích với trường V1, bổ sung vào contract và test serialization.
- **Chấp nhận** khóa cấu hình `SO_NGAY_KE_HOACH_DEN_HAN`, fallback 14 ngày + warning khi chưa cấu hình. Không tự insert ThamSoHeThong.
- `Đã có lịch` của Q12: hoãn tính đến milestone hiển thị danh sách kế hoạch/màn hình riêng; không suy từ `KeHoachDieuTri` rằng mọi hạng mục đã được đặt hẹn. Nếu thêm sau, dùng cờ riêng do backend tính từ lịch còn hiệu lực; không sửa schema tùy tiện.
- `targetScreen`: **chưa xác nhận** các khóa cho đến khi có `MenuDefinition.java` thực tế của Swing preview; không dùng nguyên bộ khóa giả định để bật điều hướng LIVE.
- `BanGiaoCa` chờ xác nhận: **defer** khỏi Dashboard V1 do chưa có định nghĩa/bằng chứng UI-M2-A.

### P1-3. Bộ đếm và phạm vi
- `countSuccessfulPaymentsOnCancelledInvoices` hiện là truy vấn toàn hệ thống, không giới hạn ngày; thông điệp cảnh báo phải thể hiện đây là cảnh báo đối soát toàn hệ thống, khác KPI số thu trong ngày.
- Tổng tiền thu trong ngày là trạng thái hiện tại của giao dịch, không phải sổ sách khóa sổ lịch sử.
- Cân nhắc consistency của dữ liệu Dashboard từ nhiều query dưới isolation SQL Server READ_COMMITTED, đặc biệt khi giao dịch đang thay đổi; test đối soát/phạm vi nếu cần snapshot.

## Security / phối hợp module
- `main` tại `4d25dbf` trong báo cáo chưa có AUTH-R1; trước đó AUTH-R1 được PO báo PASS trên checkout khác. Phải tìm đúng nhánh/commit/công việc trước khi triển khai auth; không nhân đôi code.
- Tuyệt đối không tạo `/api/v1/admin/dashboard` khi chưa có SecurityFilterChain, identity thực, vai trò `AD` gắn `ROLE_AD` đúng convention, và test 401/403/200.
- UI-M1 độc lập chưa có trong `main`, không xác nhận key hay xem mock là LIVE.
- `.env` được theo dõi trong Git: ghi task remediation riêng (kiểm tra lịch sử, rotate nếu từng có bí mật, loại khỏi Git tracking). `ddl-auto:update` yêu cầu kế hoạch giảm rủi ro trước SQL test/production; không sửa bừa trong task này.

## Điều kiện đóng UI-M2-A
1. Maven compile/test-compile/test bằng libraries thật PASS.
2. JPQL/Hibernate/SQL Server test trên dữ liệu kiểm thử PASS (hoặc ghi chính xác gate còn blocked, không tuyên bố đóng tích hợp).
3. Test lỗi section phụ cho thấy không fail toàn response hoặc lỗi đã được sửa có bằng chứng.
4. Emergency catalog coverage và các trạng thái thiếu thông tin được xử lý đúng.
5. Hợp đồng mở rộng và tham số cấu hình được đồng bộ tài liệu + test.
6. Không sửa Entity/schema/security/UI-M1 trừ task tách riêng. Không expose REST nếu chưa đạt Gate AUTH.
