# N08 — Câu hỏi BA trước UI-M2

Ngày ghi nhận: **19/10/2026**. Tất cả mục dưới đây **CHƯA DUYỆT**.
Các phương án là đề xuất để BA/PO lựa chọn, không phải quy tắc production; chưa được viết thành query, service hoặc database.
Số liệu UI-M1 chỉ đến từ ảnh 01 và không xác nhận các phương án này.

| ID / liên hệ BA review | Cần xác nhận | Đề xuất ban đầu — CHƯA DUYỆT |
|---|---|---|
| Q01 / BA-01 | Frontend production dùng Swing, web hay hướng khác? Desktop gọi REST hay cùng JVM với Spring? | Giữ preview độc lập để đánh giá UX; chỉ tích hợp sau khi có quyết định kiến trúc và hợp đồng DTO/error chính thức. |
| Q02 / BA-04, BA-19 | Múi giờ phòng khám, ranh giới ngày và cách cập nhật khi qua nửa đêm? | Dùng `Asia/Ho_Chi_Minh`, khoảng ngày nửa mở; UI thật cần ngày nghiệp vụ và thời điểm chốt từ nguồn dữ liệu. Clock hiện chỉ phục vụ mô phỏng. |
| Q03 / BA-05 | Badge menu là số hồ sơ, số nhóm hay hạng mục? Chống trùng đối tượng giữa các cảnh báo ra sao? | Tách chỉ số theo từng nguồn; Dashboard ghi số nhóm và số đối tượng liên quan, chưa xem tổng đó là số hồ sơ duy nhất. Không suy ra badge menu từ Dashboard. |
| Q04 / BA-06 | Đếm phiếu/lượt hay bệnh nhân duy nhất? | Đếm phiếu `CHO_KHAM`, loại `DA_HUY`; nếu đếm người phải định nghĩa điều kiện duy nhất và đổi label theo hợp đồng API được duyệt. |
| Q05 / lịch hẹn | Lịch hôm nay theo `thoiGianHen` hay thời điểm tiếp nhận? Chuỗi đổi lịch đếm lượt cũ/mới thế nào? | Dựa ngày hẹn tại phòng khám; trả đủ 7 trạng thái. BA cần duyệt việc giữ lịch cũ `DA_DOI_LICH` và chống đếm trùng lượt trước khi triển khai. |
| Q06 / BA-08 | Đã thu lấy ngày giao dịch hay ngày hóa đơn? Hoàn, đảo, hủy, trùng giao dịch và đa phương thức xử lý ra sao? | Tổng giao dịch `THANH_CONG` trong ngày, phân tiền mặt/chuyển khoản/thẻ POS; chưa chọn quy tắc bù trừ hoàn tiền hay giao dịch hồi tố. |
| Q07 / BA-07 | Công nợ theo hóa đơn nào, thời điểm nào? Ngoại lệ hủy, giảm giá, chênh lệch, thanh toán một phần và trả thừa? | Xem xét tổng dòng hóa đơn hợp lệ trừ các giao dịch được công nhận, loại hóa đơn hủy; BA phải chốt có chặn âm và xử lý tín dụng/điều chỉnh thế nào. Không có cột công nợ trực tiếp để lấy nguyên xi. |
| Q08 / BA-10 | Ca qua đêm tính ngày nào, phân công trạng thái nào được tính? Định mức BS/YT/LT, một người nhiều vai trò và chống trùng? | So số người phân công có hiệu lực theo vai trò với định mức từng ca; cần duyệt lịch hiệu lực và cách đếm từng người. Preview chỉ có sáng đủ, chiều/tối thiếu 1 y tá. |
| Q09 / BA-09 | Dưới định mức dùng tổng tồn hay tồn khả dụng? Một mặt hàng nhiều lô, lô không có HSD, lô hết hàng và đúng ngày hết hạn? | Chỉ cảnh báo lô còn tồn; trình BA hai phương án tổng tồn/tồn còn dùng được, ngưỡng so sánh và cách đếm mặt hàng riêng biệt. Chưa chọn cách tính thật. |
| Q10 / BA-09 | Bao nhiêu ngày là sắp hết hạn? Cảnh báo ngày hết hạn và ngưỡng có cấu hình theo mặt hàng? | Ngưỡng cấu hình do BA phê duyệt; không mặc định 30 ngày chỉ vì tài liệu prototype cũ có nêu. |
| Q11 / BA-09 | Bản kiểm tra hộp cấp cứu nào còn hiệu lực? Thiếu kiểm tra có phải cần bổ sung? Ai chịu trách nhiệm danh mục và chu kỳ? | Xem lần kiểm tra hợp lệ gần nhất, phân biệt “chưa kiểm tra” và `CAN_BO_SUNG`. Không mặc định chu kỳ 7 ngày hoặc danh mục thuốc từ prototype. |
| Q12 / BA-11 | Tái khám đến/quá hạn sinh từ hạng mục kế hoạch hay lịch hẹn? Hạng mục hủy/hoàn thành, đổi lịch và cửa sổ đến hạn? | Duyệt nguồn và khóa liên kết trước khi đếm; không mặc định cửa sổ 14 ngày từ tài liệu cũ. |
| Q13 / BA-12, BA-20 | Vai trò AD/LT/BS/YT/QL/KHO được xem và thực hiện từng use case nào? AUTH-R1 ở nhánh nào, đã nghiệm thu phần gì? | Dùng role-only theo checkout. Đợi hợp đồng bảo vệ hành động ở backend; ẩn menu chỉ hỗ trợ UX, không thay authorization. Không tạo Permission/ACL mới. |
| Q14 / BA-13 | State machine, người lập/người duyệt, xác nhận, lý do, audit và xử lý đồng thời cho hủy/duyệt/mở khóa? | Dashboard chỉ điều hướng; chỉ cho phép mutation sau khi BA duyệt state machine và backend bảo vệ. Không lấy nhãn nút trong ảnh làm đủ quy tắc. |
| Q15 / BA-14, BA-17 | Ai chịu trách nhiệm thao tác lâm sàng? Quyền dữ liệu bệnh nhân, trạng thái bị từ chối và dữ liệu được phép hiển thị/log? | Backend xác thực vai trò và bác sĩ chịu trách nhiệm; không cho Admin làm thay chỉ vì vào được màn hình. Chốt error/forbidden DTO, ẩn dữ liệu nhạy cảm và audit trước kết nối thật. |
| Q16 / BA-18 | Use case tiếp nhận/lịch hẹn cần các trường, trạng thái, cảnh báo trùng và chống tiếp nhận trùng nào? | Đặc tả riêng cho UI-M2; ảnh 02/03 chỉ tham khảo component, chưa cấp quyền CRUD hoặc mặc định các quy trình từ tài liệu legacy. |

Thứ tự gợi ý UI-M2: duyệt Q01–Q03 và phạm vi quyền → chốt định nghĩa chỉ số, nguồn và DTO/error → tích hợp đọc Dashboard → nghiệm thu dữ liệu thật → đặc tả riêng từng màn hình/hành động có ghi dữ liệu.
