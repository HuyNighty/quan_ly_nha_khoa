# CODEX IMPLEMENTATION PROMPT — N08/UI-M2-A

**Vai trò:** Senior Java 25 / Spring Boot 4.1.1 / SQL Server Engineer + reviewer Swing UI.

**Repository:** `HuyNighty/quan_ly_nha_khoa`, backend Maven tại `nhakhoa/`, giao diện preview tại `admin-desktop-preview/` nếu có trong checkout.

**Mục tiêu duy nhất của lượt này:** **triển khai tầng truy vấn read-only Dashboard của backend theo BA V1, kiểm thử và chuẩn bị tích hợp Swing an toàn**. Chỉ phát hành endpoint REST và chạy LIVE nếu đã chứng minh được authentication/authorization role-only trên checkout hiện hành. **Không** triển khai 20 màn hình hay các mutation nghiệp vụ ở lượt này.

## Tài liệu nguồn (đọc theo thứ tự)

1. `01-BA-DECISIONS-V1.md`: baseline Q01–Q16; nguyên tắc cho dashboard; quy tắc tính.
2. `02-ROLE-MATRIX-V1.md`: `AD` và các giới hạn clinical/self-approval.
3. `03-DASHBOARD-API-CONTRACT-V1.md`: hợp đồng DTO/API, null vs 0, trạng thái, tests.
4. `admin-desktop-preview/HANDOVER.md` và `BA-OPEN-QUESTIONS.md` nếu tồn tại trong checkout; tôn trọng các chứng cứ UI-M1.
5. Source hiện hành của repository và `menu-admin.zip` nếu đã được cung cấp. Docs legacy chỉ tham khảo, không áp dụng hướng Swing + JDBC.

## AUTHORIZATION / phạm vi code

**ĐƯỢC LÀM:** Khảo sát read-only Git checkout; tạo lớp DTO response bất biến, repository query chuyên dụng, read aggregation service và unit/integration test phù hợp; thêm tài liệu API và báo cáo. Thêm bộ đọc HTTP ở Swing dưới dạng adapter chưa kích hoạt nếu bảo vệ backend chưa sẵn sàng. Nếu security có hiệu lực thật và tests 401/403/200 PASS, được thêm GET read-only Controller; không thêm POST/PATCH/PUT/DELETE.

**KHÔNG ĐƯỢC LÀM:** sửa Entity/enum hiện có, schema/migration SQL Server, sửa hàng loạt frontend, tái kiến trúc dự án, tạo JWT/login mới, tự reset ADMIN hoặc tài khoản SQL, thêm Permission ACL, thay mật khẩu, xóa file, update .env, tự commit/push/merge, tự chạy demo với dữ liệu bệnh nhân thật, hard-code secret. Bất cứ ngoại lệ nào phải dừng và báo cáo, không tự mở rộng scope.

## A. Khảo sát/gate trước khi viết code

1. `git status --short --branch`, HEAD, nhánh, untracked files. Không phá uncommitted work; UI-M1 có thể đang untracked.
2. Đọc `nhakhoa/pom.xml`, `application.yaml`, `VaiTro`, `TaiKhoan`, `NhanVien`, các Entity liên quan và toàn bộ controller/security nếu có trong **checkout thật**.
3. Xác minh AUTH-R1 thực tế: Spring Security dependency/config, BCrypt, ADMIN bootstrap, cơ chế identity, test về quyền endpoint. **Không được suy ra từ báo cáo ở cuộc trò chuyện hoặc README legacy.** `main` GitHub tại HEAD 4d25dbf được ghi nhận chưa có code security; nhánh local có thể khác.
4. Xác minh có `admin-desktop-preview/` và vị trí JAR/DTO hiện tại, nếu không có thì không tự dựng lại từ đầu.
5. Repo đang theo dõi `nhakhoa/.env` ở main theo metadata; **không in/đọc/đẩy nội dung secret**. Báo rủi ro nếu tiếp tục tracked. Không tự xoay credential hoặc rewrite history.
6. Ghi `GATE_AUTH=PASS|BLOCKED`, `GATE_UI_M1=AVAILABLE|MISSING`, `GATE_MAVEN=PASS|ENV_BLOCKED|FAIL` dựa trên bằng chứng.

## B. Cài đặt tầng đọc

Tổ chức theo package `com.manage.nhakhoa` đang có. Không tạo `com.quanlynhakhoa` song song. Có thể tạo `features/dashboard/` cho `DashboardQueryService`, `dto/response`, `repository` read model; repository JPA ở các feature phù hợp. DTO chỉ gồm aggregate, KHÔNG chứa tên bệnh nhân, SĐT, ngày sinh, bệnh án hoặc thuốc kê cho một cá nhân.

1. Inject `Clock` và `ZoneId` cấu hình (`Asia/Ho_Chi_Minh`); ngày `D` và `[D 00:00, D+1 00:00)`; không dùng trực tiếp `LocalDate.now()`/`LocalDateTime.now()` trong query service.
2. Appointment 7 states đầy đủ theo `LichHen.thoiGianHen`, mỗi row là một bản ghi; trả total/sum 7 khớp nhau.
3. Exam theo `PhieuKham.thoiGianTiepNhan` và `CHO_KHAM`, `DANG_KHAM`, `DA_KET_LUAN`, đếm phiếu.
4. Collections theo `ThanhToan.trangThai = THANH_CONG`, thời điểm trong ngày, tách 3 phương thức; `BigDecimal` không dùng floating point.
5. Receivables: **aggregate line items/paid riêng theo hóa đơn**, loại hóa đơn `DA_HUY`, `max(total-paid,0)` trên từng hóa đơn, đếm hóa đơn `>0`; overpaid/missing data ghi anomaly. Không làm join 1-N khiến nhân tiền.
6. Staffing: số người phân công hợp lệ, `DISTINCT NhanVien`, người còn làm việc, theo ca/ngày bắt đầu/role `BS,YT,LT`, trả thiếu dự kiến từ định mức; không coi là điểm danh.
7. Inventory: tồn khả dụng lô `HSD > businessDate`; lô HSD `<= businessDate` quá hạn; cận hạn `(D,D+30]`; null HSD là Unknown, không tính vào tồn khả dụng; phân biệt SKU và lô; chỉ lô số lượng >0 vào thống kê HSD. Ngưỡng fallback 30 nếu config không có + warning.
8. Emergency kit: kiểm tra hợp lệ gần nhất; ngày kiểm tra quá 7 ngày hoặc `CAN_BO_SUNG` → cần xử lý. Danh mục toàn mock hoặc không có mục chính thức → `UNCONFIGURED`, không tuyên bố an toàn. Không thay DanhMucCapCuu.
9. Treatment due: `ChiTietDieuTri.CHUA_THUC_HIEN` thuộc `KeHoachDieuTri.DANG_THUC_HIEN`, `ngayDuKien<=D+14`; đếm `DISTINCT maKeHoach`, có `overduePlanCount` riêng, không gọi là số lịch tái khám đã đặt.
10. Work queue: chỉ phát các nhóm warning mà nguồn/công thức đã truy vấn chính xác; count đối tượng có thể trùng, groupCount đếm code nhóm không rỗng, không tự ép thành fixture 9/18; sidebar badge không tính từ tổng.
11. Bất thường: null HSD, thiếu danh mục thực tế, dữ liệu âm, số dư trả thừa, cấu hình sai... biểu diễn có chủ đích qua `status` + `warnings`; không nuốt exception và trả 0. Tránh N+1 và eager loading toàn entity không cần thiết.

Nếu không thể tính một section chính xác từ entity đang có: đánh dấu `UNAVAILABLE` **có lý do**, giữ API contract, thêm test cho tình huống đó. Không tạo cột/migration để làm đẹp số.

## C. Controller và HTTP integration — security gate nghiêm ngặt

- Nếu `GATE_AUTH=PASS`: tạo `GET /api/v1/admin/dashboard` read-only, chỉ `AD`, với context path `/manage` theo yaml. Kiểm thử unauthenticated=401, authenticated non-AD=403, AD=200. Ẩn dữ liệu nhạy cảm, `Cache-Control: no-store`, no secrets in logs. Không dùng `permitAll`, không dùng security bypass, không tạo hard-coded API key.
- Nếu `GATE_AUTH=BLOCKED`: **KHÔNG tạo hoặc expose endpoint REST LIVE**. Chỉ triển khai tầng `DashboardQueryService`, repository/DTO/tests độc lập với HTTP và ghi `AUTH BLOCKER`; không thay Security/JWT để vượt gate. Đề xuất checklist đưa AUTH-R1 vào đúng checkout như tác vụ riêng.
- DTO lỗi chuẩn hợp đồng thực tế nếu đã có; nếu chưa có response exception policy, chuẩn hóa mô tả contract trong docs nhưng không tự triển khai toàn hệ thống ErrorHandler mới chỉ để chạy Dashboard.

## D. Swing adapter — chỉ nếu an toàn

- Giữ `--fixture` bất biến ngày 12/10/2026 và `--runtime` là dữ liệu mô phỏng có nhãn; không đổi demo thành LIVE.
- Nếu `GATE_AUTH=PASS` và endpoint đã đạt 401/403/200, có thể tạo `HttpDashboardDataProvider` cho `--live` với base URL ngoài code; phải có identity/token từ cơ chế auth đã được nghiệm thu, không tự tạo màn hình đăng nhập/JWT để nối cho xong.
- Nếu không có luồng đăng nhập hợp lệ, **chỉ tạo adapter/mapper test bằng stub offline**, không cho người dùng chạy `--live` tới endpoint thật. Không lưu bearer token ra file hoặc log.
- 401→“Phiên đăng nhập chưa hợp lệ”, 403→“Không có quyền truy cập”, lỗi mạng→“Không thể kết nối”, 5xx→“Không tải được dữ liệu”. Không tự rơi về mock, không giữ số cũ với nhãn LIVE; mọi cập nhật Swing trên EDT, không chặn UI.

## E. Kiểm thử bắt buộc

- Unit tests deterministic với Clock cố định: ngày ranh giới 23:59/00:00, 7 enums, thời gian lịch cũ/mới, phiếu chờ/đang/đã kết luận.
- Công nợ: 1 invoice 2 dòng + 2 lần thu (chống tích Descartes), invoice hủy, payment hủy, thanh toán một phần, trả thừa anomaly, BigDecimal rất lớn.
- Kho: hết hạn ngày hôm nay, trước/sau +30d, thiếu HSD, hết hàng, dưới mức distinct, một mặt hàng nhiều lô.
- Ca: nghỉ việc, DA_HUY, nhiều phân công cùng ca, thiếu người; không gọi là chấm công.
- Cấp cứu: chưa cấu hình, quá chu kỳ, CAN_BO_SUNG, kết quả DAT hợp lệ; không lấy danh mục `laDuLieuMau` làm an toàn.
- Kế hoạch: 2 hạng mục cùng một kế hoạch chỉ đếm 1, hủy/hoàn thành loại khỏi count.
- Null/empty/exception: không nhầm 0 với unavailable; values invariant; 401/403/200 nếu có Controller.
- Chạy `mvn -B -f nhakhoa/pom.xml test` hoặc lệnh tương ứng nếu được; môi trường Maven `.m2` Access Denied phải ghi **ENV_BLOCKED**, không tự xóa cache hoặc tuyên bố PASS. Không ép kết nối production database; integration SQL Server chỉ khi có môi trường test thực sự.
- UI không regression build `admin-desktop-preview/build.ps1 -Test` nếu module hiện diện và môi trường Windows cho phép; không bắt Linux giả chạy PowerShell.

## F. Bàn giao kết quả

Báo cáo bằng **tiếng Việt** theo thứ tự:

1. Baseline branch/HEAD + working tree ban đầu/cuối.
2. Gate table AUTH, UI-M1, Maven, SQL Server tests.
3. Danh sách chính xác file tạo/sửa và ranh giới không đụng đến.
4. Từng metric: nguồn Entity, query logic, status `AVAILABLE/UNAVAILABLE/UNCONFIGURED` và caveat.
5. Tests PASS/FAIL/NOT RUN, lệnh chạy, lỗi môi trường.
6. Các rủi ro/blocked (đặc biệt auth, `.env`, chưa có schema test).
7. Hướng đi tiếp: nếu auth thiếu, AUTH-R1 reconciliation; nếu pass, nghiệm thu REST AD + Swing `--live`.

**Không tự commit/push/merge; không tuyên bố UI-M2 DONE nếu chưa có endpoint bảo vệ và đối soát dữ liệu thật.**

## ĐIỀU CẤM CUỐI CÙNG

Không tăng phạm vi vì muốn “hoàn thành đủ 21 màn hình”. Không mô phỏng số LIVE. Không thay đổi bảo mật để vượt gate. Không mạo danh BS, không thêm permission engine, không sửa Entity/database. Nếu có xung đột tài liệu, giải thích sai lệch trong report và giữ nguyên dữ liệu thật.
