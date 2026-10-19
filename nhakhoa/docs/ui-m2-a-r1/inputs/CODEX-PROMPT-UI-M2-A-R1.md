# PROMPT CODEX — N08 UI-M2-A-R1: Review Fix & Real Verification

Repo: `HuyNighty/quan_ly_nha_khoa`.
Baseline được báo cáo: `main` SHA `4d25dbf88be6415a8fb77facf64b5d4ea9e1f704` (phải tự xác minh checkout thực tế).

## Đầu vào
- `UI-M2-A-dashboard-readonly.patch` (43 files mới); hai bản upload `.patch` là trùng nhau, CHỈ dùng một bản.
- `BAN-GIAO-UI-M2-A.md`.
- BA V1 `01-BA-DECISIONS-V1.md`, `03-DASHBOARD-API-CONTRACT-V1.md`, BA V1.1 và `REVIEW-UI-M2-A-R1.md` (nếu được cung cấp).

## Ủy quyền
Được phép thực hiện **UI-M2-A-R1**: áp dụng/kiểm tra patch trên nhánh riêng, sửa phần dashboard mới, tăng test, chứng minh compile/JPA/SQL Server test khi môi trường cho phép. Không có quyền thay Entity/schema/migration, `.env`, `application.yaml`, backend auth/JWT, Swing preview, các chức năng CRUD/hủy/duyệt. Không commit/push/merge nếu chưa được yêu cầu.

## Tiến trình bắt buộc
1. **Checkout/gate:** `git status`, HEAD, xác định patch có được áp dụng chưa; KHÔNG áp dụng hai lần; nếu có code tương đương phải so diff trước. Xác minh `admin-desktop-preview/`, AUTH-R1 tồn tại ở nhánh/checkout hiện tại hay không. Bảo vệ thay đổi người dùng.
2. **Build với dependency thật:** chạy JDK25 + Maven wrapper của `nhakhoa/` (không phải wrapper ở repo root). Chạy `compile`, `test-compile`, ba lớp test: `DashboardQueryServiceTest`, `DashboardDtoPrivacyTest`, `JpaDashboardReadRepositoryTest`. Không dùng stub để tuyên bố PASS. Nếu Maven bị chặn, báo ENV_BLOCKED với bằng chứng, không tự sửa settings toàn cục, không khai báo PASS.
3. **JPQL thật:** dùng SQL Server 2022 Testcontainers hoặc SQL Server test được phép; dựng fixture độc lập và chạy JPQL thực để xác nhận tập truy vấn trong JpaDashboardReadRepository. Không chạm production, không chỉnh `ddl-auto` production. Chỉ được bổ sung dependency test-scope vào `pom.xml` nếu Testcontainers là thực sự cần thiết, phải trình bày rõ thay đổi và không thay dependency production. Bao phủ aggregate, enum, JOIN, LocalDateTime nửa mở, kết quả rỗng/null; đối soát tính công nợ không bị nhân hàng.
4. **P0 transaction fallback:** thử gây lỗi có chủ đích ở mỗi nhóm staffing/inventory/emergency/treatment khi Spring Transaction đang chạy; chứng minh trả UNAVAILABLE với số liệu null và các KPI lõi vẫn giữ. Phải làm ít nhất một test gây lỗi đi qua Spring transaction proxy/JPA thật, không chỉ fake repository. Nếu xuất hiện UnexpectedRollbackException/rollback-only, sửa ranh giới transaction trong **feature dashboard** theo cách có test thật. Core query fail phải trả lỗi toàn Dashboard, không giả số 0.
5. **P0 emergency coverage:** kiểm chứng lần kiểm tra mới nhất **bao phủ đầy đủ tất cả mục chính thức còn áp dụng**; không dùng dữ liệu kiểm tra của mục mẫu để kết luận. Với mục mới, thiếu chi tiết, danh mục trộn mẫu/thật, HSD thiếu chưa thể xác nhận, không trả trạng thái gây hiểu nhầm; ghi rõ cần kiểm tra/chưa đủ thông tin. Không thay đổi quy tắc lâm sàng bên ngoài entity/BA. Thêm test regression.
6. **BA V1.1 contract:** cập nhật tài liệu contract có kiểm thử serialization cho các field mở rộng đã được BA chấp nhận: `appointments.activeCount`, `receivables.overpaidInvoiceCount`, `staffing.understaffedShiftCount`, `inventory.anomalyLotCount`, `emergencyKit.checkState`, `emergencyKit.checkCycleDays`, `warnings[] {code,section,message}`. Chấp nhận `SO_NGAY_KE_HOACH_DEN_HAN` với fallback 14; không tự ghi DB. `treatmentDue.duePlanCount` đã gồm quá hạn; làm rõ mô tả để tránh đếm trùng vô ý ở client.
7. **Defer:** không thêm trường `Đã có lịch` vào Dashboard tổng hợp nếu chưa có use case, không tự thêm `BanGiaoCa` work queue; chưa đối chiếu `targetScreen` khi không có UI-M1, phải báo PENDING.
8. **Security gate:** tuyệt đối chưa thêm REST controller nếu chưa có AUTH-R1 đã kiểm thử trong cùng checkout. Không tự triển khai JWT/`permitAll`/bypass security. `ROLE_AD` là convention Spring Security nếu sau này sử dụng `hasRole("AD")`, nhưng phải xác nhận security thực trước.
9. **Remediation riêng:** báo rủi ro `.env` tracked và `ddl-auto:update`, không chỉnh trong task này, không mở/hiển thị secrets.
10. **Bàn giao:** báo file changed, git diff, build logs chính xác, test case, P0 resolved/unresolved, integration gate, status PASS/FAIL/NOT VERIFIED/ENV_BLOCKED; cung cấp prompt riêng cho AUTH-R1 reconciliation và UI-M2-B nhưng không thực thi hai task đó.

## Acceptance
- Không có lỗi syntax/build trên dependencies thật; JPQL real test có bằng chứng hoặc gate UNVERIFIED trung thực.
- Giao dịch lỗi của section phụ không làm sập core Dashboard.
- Hộp cấp cứu không trả `TRONG_CHU_KY` chỉ vì có lần kiểm tra cũ của catalog mẫu/thiếu mục chính thức.
- Không để lộ PII/PHI và không làm endpoint không bảo vệ.
- Repository vẫn giữ nguyên module ngoài phạm vi.

**Bắt đầu UI-M2-A-R1 ngay; không nhảy thẳng sang UI-M2-B.**
