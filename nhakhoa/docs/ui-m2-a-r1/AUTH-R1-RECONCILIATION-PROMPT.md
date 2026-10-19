# Prompt cho task riêng — Đối chiếu AUTH-R1

Đây là prompt bàn giao, **không phải chỉ dẫn đang được thực thi trong UI-M2-A-R1**.

Tiếp tục repository `HuyNighty/quan_ly_nha_khoa`. Đọc báo cáo UI-M2-A-R1 và xác minh checkout, HEAD, branch, working tree trước khi thay đổi.

1. Tìm implementation AUTH-R1 đã được PO báo PASS bằng branch/commit/worktree/PR thực tế. Không lấy README legacy hoặc báo cáo ở checkout khác thay bằng chứng code. Nếu không tìm thấy đúng nguồn, báo cần ref/PR, không viết lại auth để nối Dashboard cho xong.
2. So sánh dependency Security, SecurityFilterChain, identity principal, trạng thái tài khoản, ánh xạ `AD`/`ROLE_AD` và test bảo mật. Không log/open secret, không reset ADMIN, không tạo Permission/ACL.
3. Bảo vệ toàn bộ thay đổi UI-M1/UI-M2-A-R1 chưa commit. Nếu tích hợp code được ủy quyền, làm nhánh riêng và tránh ghi đè. Không commit/push/merge nếu chưa được yêu cầu.
4. Chạy Java 25/Maven với dependency thật; xác minh anonymous hoặc identity sai nhận 401, authenticated non-AD nhận 403, AD hợp lệ nhận 200 trong hợp đồng được duyệt; tài khoản khóa/vô hiệu không được truy cập. Kiểm convention Spring thực tế, không mặc định chỉ từ tên enum.
5. Không mở Dashboard trước khi gate này đạt. Khi endpoint được phê duyệt riêng: GET chỉ đọc, không `permitAll`, no-store, lỗi lõi không lộ SQL/PII/PHI, không trả payload LIVE cho 401/403.
6. Tách remediation `.env` tracked, lịch sử secret và rủi ro ddl-auto thành task được duyệt riêng; không tự xoay credential hoặc sửa lịch sử Git. Đọc nội dung `.env` không cần thiết cho đối chiếu code.

Bàn giao tiếng Việt: nguồn AUTH chính xác, diff thực tế, lệnh/log security test, PASS/FAIL/NOT VERIFIED/ENV_BLOCKED và các gate còn thiếu. Không đồng thời triển khai UI-M2-B.
