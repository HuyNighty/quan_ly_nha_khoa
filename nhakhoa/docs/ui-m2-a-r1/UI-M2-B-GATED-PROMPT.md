# Prompt cho task sau — UI-M2-B (chỉ khi gate đã đạt)

Đây là prompt bàn giao, **UI-M2-B chưa được thực thi trong UI-M2-A-R1**.

Trước khi code, đọc báo cáo R1, hợp đồng `DASHBOARD-API-CONTRACT-V1.1.md` và kiểm chứng các gate ngay trên checkout được triển khai:

- AUTH-R1 có code/identity/AD và test 401/403/200 thực sự PASS, không bypass.
- Truy vấn Dashboard qua Hibernate/SQL Server thật được đối soát với naming strategy đúng của Spring Boot và FK đang bật. Không lấy lỗi do cấu hình bộ test sai làm kết luận Entity production lỗi; đối chiếu báo cáo R1 và schema đích thực tế trước tích hợp.
- Gate `DashboardEntityMappingGateTest` phải được xử lý: hiện persist MapsId thật FAIL do cột khóa ghép camelCase/snake_case cùng tồn tại. PASS truy vấn đọc trên fixture điền đủ cột và contextLoads không thay thế gate này. Sửa Entity/schema chỉ ở task được cấp quyền riêng.
- Rủi ro `.env` tracked và `ddl-auto:update` đã có phương án được duyệt; quyết định yêu cầu snapshot/consistency của số liệu tài chính trước LIVE.
- Endpoint GET Dashboard được phê duyệt và đạt kiểm thử AD/no-store/error DTO; chưa có endpoint thì chưa bật `--live`.
- 21 khóa menu và quy tắc hiển thị/empty/error/401/403 đã khớp hợp đồng hiện hành. Giữ nguyên thay đổi người dùng.

Khi tất cả gate liên quan đạt, mới thêm adapter HTTP và mapper DTO cho Swing. Không thêm JDBC/Spring vào JVM desktop. Giữ `--fixture`/`--runtime` là mô phỏng, mode LIVE phải rõ nguồn. Không lưu token/mật khẩu ra file/log và không hard-code credential.

Ánh xạ đủ field V1.1; tooltip “đến hạn (bao gồm quá hạn)”; tổng đối tượng là lượt liên quan, không unique. Hộp cấp cứu thiếu coverage/HSD phải hiển thị cần kiểm tra/chưa đủ thông tin. Các ngưỡng lấy từ backend, không hardcode số trên UI LIVE.

HTTP chạy nền, timeout hữu hạn, EDT cập nhật UI, bỏ response cũ, xóa số LIVE cũ khi lỗi; không fallback sang mock. Kiểm 401/403/5xx/offline, retries, tải liên tiếp, chuyển trang khi đang tải, font/DPI và 1280×760.

Không thực hiện CRUD/hủy/duyệt/mở khóa/lâm sàng, không sửa Entity/schema/JWT hoặc thêm màn hình ngoài phạm vi. Báo cáo tiếng Việt với log/bằng chứng thực, file thay đổi và giới hạn; không commit/push/merge nếu chưa được yêu cầu.
