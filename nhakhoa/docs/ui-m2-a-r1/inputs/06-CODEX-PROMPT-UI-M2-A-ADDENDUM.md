# PROMPT CODEX — N08 UI-M2-A, có BA audit disposition V1.1

Bạn là Senior Java/Spring Engineer. Bạn được phép **triển khai UI-M2-A read-only** theo bộ hồ sơ dưới đây. Không triển khai rộng 21 trang hoặc hành động ghi dữ liệu.

**Đầu vào bắt buộc:**
1. `01-BA-DECISIONS-V1.md`
2. `02-ROLE-MATRIX-V1.md`
3. `03-DASHBOARD-API-CONTRACT-V1.md`
4. `04-CODEX-PROMPT-UI-M2-A.md`
5. `05-BA-REVIEW-DISPOSITION-V1.1.md` (bổ sung mới)
6. `admin-desktop-preview/HANDOVER.md` và `BA-OPEN-QUESTIONS.md` nếu có ở checkout.

## Phần A — Tiền kiểm

1. Ghi rõ nhánh, HEAD, dirty files, phiên bản Java/Maven, module thực tế. Không ghi đè thay đổi có sẵn.
2. Rà soát AUTH-R1 chỉ từ **code checkout hiện tại**; nếu chưa xác minh role-only auth/endpoint protection, **không tạo hoặc expose endpoint dashboard LIVE** có thể truy cập trái phép. Không tự viết JWT.
3. Kiểm tra backend Maven với kho cache có quyền ghi phù hợp nếu sẵn có; không thay đổi global config, không xóa cache, không sửa `.env`, không đọc/ghi secrets ra report. Nếu không thể test, đánh dấu NOT VERIFIED.
4. Không dùng `nhakhoa/docs/ARCHITECTURE.md` legacy làm source code thật; không tạo JDBC/DAO trong Swing.

## Phần B — Implement read model

1. Repository / Dashboard Query Service / DTO immutable, chỉ **đọc** dữ liệu, dựa trên Entity/enum hiện hành và BA V1.
2. `Clock` inject + `Asia/Ho_Chi_Minh`, biên ngày `[startInclusive, endExclusive)`.
3. Quy tắc lịch hẹn: trả đủ 7 trạng thái theo `thoiGianHen` thuộc ngày; **không loại DA_HUY/DA_DOI_LICH khỏi tổng bản ghi**; `activeCount` nếu có phải là trường riêng.
4. Phiếu khám: dùng từ **Phiếu chờ khám**, đếm phiếu, không gọi bệnh nhân duy nhất.
5. Thanh toán: `THANH_CONG` theo thời điểm giao dịch, phân ba phương thức; `DA_HUY` không tính.
6. Công nợ: mỗi hóa đơn khác `DA_HUY`, tổng chi tiết minus thanh toán thành công; hóa đơn chờ duyệt hủy **vẫn tính** cho tới khi hủy có hiệu lực. Nếu query join gây nhân dòng phải tách subaggregate; BigDecimal scale 0.
7. Kho: tồn theo lô còn HSD > ngày nghiệp vụ, mặt hàng unique; ngưỡng 30 ngày lấy từ cấu hình nếu có; fallback đã công bố nếu chưa cấu hình, cảnh báo provenance, không tự ghi DB.
8. Ca làm, cấp cứu và kế hoạch điều trị: bám BA V1; không dùng dữ liệu mẫu lâm sàng để đánh dấu an toàn thật. Ngưỡng 7/14 theo cấu hình/fallback từ service, không hardcode vào Swing LIVE.
9. WorkQueue: phân biệt `workGroupCount`, `relatedObjectCount`, `objectCount` và badge từng menu. Chỉ trả mục có query hỗ trợ thật.
10. Status từng section phải phân biệt AVAILABLE, UNAVAILABLE, UNCONFIGURED; không đổi lỗi thành zero. DTO không có PII/PHI.
11. Nếu gặp `NhaCungCap`, nhớ `diaChi` và `NGUNG_HOP_TAC` có trong code, nhưng không có `maSoThue`; **không liên quan UI-M2-A** nên không sửa.

## Phần C — API / Swing

- Nếu AUTH-R1 đã xác minh và có test bảo vệ vai trò AD: tạo `GET /manage/api/v1/admin/dashboard` theo hợp đồng, kiểm 401/403 và no-store; không dùng stub/permitAll để đi tắt.
- Nếu chưa có AUTH: chỉ implement/query/test *internal*; đề xuất API contract nhưng **không expose route**. Không bật `--live` trên Swing. Preview `--fixture` / `--runtime` vẫn dùng mock và ghi rõ.
- Không sửa 21 ảnh concept trong task này; những điều chỉnh UI concept nằm ở UI-BA-G2 riêng. Nếu có nhãn "Bệnh nhân chờ khám" trong preview đang chạy, nêu phát hiện và ghi task, chỉ sửa khi không làm lan phạm vi.

## Phần D — Kiểm thử/bàn giao

- Test 7 trạng thái lịch, đổi lịch cùng/khác ngày, ngày giới hạn.
- Test hóa đơn có nhiều dòng + nhiều thanh toán, có yêu cầu hủy CHO_DUYET, trạng thái DA_HUY, tiền thừa anomaly.
- Test kho dưới định mức, lô HSD hôm nay/null/hết hàng; clock cố định.
- Test nguồn trống, lỗi DB, số tiền lớn, trạng thái unavailable; không có dữ liệu nhạy cảm trong DTO/log.
- Test REST auth 401/403/200 **chỉ nếu endpoint được phép bật**.
- Báo PASS/FAIL/NOT VERIFIED, commands/logs, danh sách file thêm/sửa, sai lệch code với BA contract, mốc sẵn sàng UI-M2-B.
- Không sửa Entity/schema, database migration, AUTH/JWT, CRUD nghiệp vụ, không commit/push/merge.

**Đầu ra:** thực thi kỹ thuật trong phạm vi đã cấp, có báo cáo bàn giao tiếng Việt. Nếu gate auth không đạt, không coi toàn bộ UI-M2-A là BLOCKED: hoàn thành tầng query service/test và báo endpoint/live vẫn BLOCKED.
