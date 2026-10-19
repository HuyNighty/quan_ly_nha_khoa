# N08 — Dashboard Read-Only API Contract V1

**Phạm vi:** Data query/DTO/read-only. Không có mutation, không JWT mới. **Endpoint chỉ được kích hoạt khi auth role-only có kiểm thử thực tế.**

## 1. HTTP và bảo mật

- `GET /manage/api/v1/admin/dashboard` (vì `server.servlet.context-path=/manage`, route Controller `/api/v1/admin/dashboard`).
- Không nhận ngày tùy ý ở V1; ngày nghiệp vụ tính tại backend bằng `Clock + Asia/Ho_Chi_Minh` để tránh ngụ ý có khả năng báo cáo công nợ lịch sử chính xác.
- `Authorization: Bearer ...` hoặc cơ chế đã được AUTH chính thức chấp nhận; không tạo JWT, refresh-token hay ghi token trong desktop khi chưa xác định.
- `AD` đã xác thực/hoạt động mới được đọc; 401 unauthenticated, 403 authenticated wrong role, 503 backend/SQL lỗi. **Không thể xác thực = không phát hành endpoint LIVE.**
- `Cache-Control: no-store`; không log response payload hay sensitive headers.

## 2. Response đề xuất

```json
{
  "source": "LIVE",
  "businessDate": "2026-10-19",
  "zoneId": "Asia/Ho_Chi_Minh",
  "generatedAt": "2026-10-19T15:38:00+07:00",
  "period": { "startInclusive": "2026-10-19T00:00:00+07:00", "endExclusive": "2026-10-20T00:00:00+07:00" },
  "appointments": {
    "totalRecords": 0,
    "byStatus": {
      "CHO_XAC_NHAN": 0, "DA_XAC_NHAN": 0, "DA_DEN": 0,
      "HOAN_THANH": 0, "VANG_MAT": 0, "DA_HUY": 0, "DA_DOI_LICH": 0
    }
  },
  "exams": { "waiting": 0, "inProgress": 0, "concluded": 0, "unit": "PHIEU_KHAM_TIEP_NHAN_TRONG_NGAY" },
  "collections": {
    "amountVnd": "0",
    "byMethodVnd": { "TIEN_MAT": "0", "CHUYEN_KHOAN": "0", "THE_POS": "0" },
    "definition": "THANH_TOAN_THANH_CONG_TRONG_NGAY_THEO_TRANG_THAI_HIEN_TAI"
  },
  "receivables": { "amountVnd": "0", "invoiceCount": 0, "scope": "TAT_CA_HOA_DON_CON_HIEU_LUC_HIEN_TAI" },
  "staffing": { "status": "AVAILABLE", "shifts": [] },
  "inventory": { "status": "AVAILABLE", "belowMinimumItemCount": 0, "nearExpiryLotCount": 0, "expiredLotCount": 0, "unknownExpiryLotCount": 0, "thresholdDays": 30 },
  "emergencyKit": { "status": "UNCONFIGURED", "lastCheckedAt": null, "daysSinceLastCheck": null, "message": "Chưa có danh mục cấp cứu chính thức" },
  "treatmentDue": { "status": "AVAILABLE", "duePlanCount": 0, "overduePlanCount": 0, "horizonDays": 14 },
  "workQueue": { "workGroupCount": 0, "relatedObjectCount": 0, "items": [] },
  "warnings": []
}
```

**Đây là ví dụ schema với toàn số 0 minh họa**, không phải kết quả LIVE từ SQL Server. Mỗi mục `status=AVAILABLE` chỉ khi query đã thực hiện thành công; `UNAVAILABLE` khi thiếu dữ liệu cấu hình; `UNCONFIGURED` khi nghiệp vụ chưa có danh mục thực; `ERROR` khi lỗi truy vấn toàn trang thì trả HTTP lỗi, **không trả payload toàn 0**. `amountVnd` là decimal string số nguyên để bảo toàn chính xác khi UI parse bằng `BigDecimal`.

### WorkQueue Item

```json
{
  "code": "LICH_HEN_CHO_XAC_NHAN",
  "priority": "CHO_DUYET",
  "title": "Lịch hẹn chờ xác nhận",
  "objectCount": 2,
  "targetScreen": "LICH_HEN",
  "sourceMetric": "appointments.byStatus.CHO_XAC_NHAN"
}
```

Mỗi task phải có code định danh ổn định, nhãn Việt, nguồn tính và đích điều hướng trong 21 khóa menu; `objectCount > 0` mới xuất hiện. Không cho client tự dựng truy vấn từ `targetScreen`. Các tác vụ chưa đủ quy tắc xuất từ DB thì **không tạo dòng việc giả**.

## 3. Quy tắc tính và giới hạn

- All counts integer nonnegative, `totalRecords == sum(byStatus[*])`; `amountVnd == sum(byMethodVnd[*])`.
- Số phiếu theo `thoiGianTiepNhan` trong ngày, số lịch theo `thoiGianHen` trong ngày, số thu theo `ThanhToan.thoiGian` trong ngày. Tất cả `[startInclusive, endExclusive)` local.
- Tổng công nợ là **hiện tại**, không phải lịch sử ngày D; cộng per-invoice đúng, không nhân chéo khi JOIN. Thanh toán hủy không tính.
- Mọi `List` cần sort ổn định để test; không trả Entity JPA; không có thông tin cá nhân trong Dashboard.
- Nếu chưa tạo repository hoặc service nào, Codex tạo tối thiểu trong feature hiện hành và một use case `dashboard` cho read aggregation; không duplicate entity.
- Queries tính nhân sự tồn kho/cấp cứu/tái khám cần sử dụng chính xác enum và quan hệ đã tồn tại, không tự tạo field; section nào chưa thể chứng minh chính xác → `UNAVAILABLE` + `warnings[]` mô tả không nhạy cảm, không trả 0 thay.
- Cần xét dữ liệu trống: **database truy vấn thành công với 0 bản ghi → các count = 0 là hợp lệ**; chưa cấu hình danh mục/threshold → `UNCONFIGURED/UNAVAILABLE`.
- Khối tài chính `BigDecimal` và `COUNT(DISTINCT ...)` đúng. Tránh nhiều join 1–N gây nhân dữ liệu.

## 4. Trạng thái UI, tương thích UI-M1

- Provider hiện có `DemoDashboardDataProvider` tiếp tục chạy `--fixture` và `--runtime` theo nghĩa **mock runtime**, không đổi thành LIVE ngầm.
- Khi auth/API được bật: thêm `HttpDashboardDataProvider` (hoặc adapter tương đương) với các mode rõ `--live` và cấu hình base URL từ môi trường, không hard-code localhost/password/token trong source.
- Live offline/401/403/503 hiển thị tương ứng “Mất kết nối”, “Cần đăng nhập”, “Không có quyền”, “Không tải được số liệu”; xóa số LIVE cũ khi tải fail, không fallback mock. `source` ở UI phải đúng nguồn.
- Không block EDT, bỏ phản hồi HTTP cũ sau khi user rời Dashboard, timeout hữu hạn và xử lý lỗi UTF-8 tiếng Việt; không cache token/file nhạy cảm.

## 5. Kiểm thử chấp nhận

1. Empty database sạch có 0 lịch/phiếu/thu hợp lệ → trả 0 có nguồn LIVE, không giả fixture.
2. 7 trạng thái `LichHen` tổng chính xác; lịch đổi là hai bản riêng; 0 trạng thái vẫn có khóa.
3. 2 dòng chi tiết + 2 lần thu một hóa đơn → không double count; hủy thanh toán loại khỏi tổng.
4. Thu trong ngày không gộp hôm qua và nửa đêm tiếp theo; BigDecimal 18 chữ số không tràn.
5. Nhân sự phân ca không đếm trùng, lọc nhân viên nghỉ việc, báo đủ/thiếu; không nhầm số phân công với số hiện diện.
6. Tồn khả dụng: lô HSD hôm nay tính hết hạn, lô rỗng bỏ qua, lô HSD null cảnh báo Unknown, mặt hàng dưới định mức distinct.
7. Cấp cứu thiếu danh mục chính thức không báo đạt; tái khám đếm DISTINCT kế hoạch chứ không đếm dòng.
8. Không auth/không AD không nhận payload LIVE; không có JWT tự chế; không lộ PII/PHI.
9. UI fixture 12/10/2026 giữ nguyên, adapter LIVE chỉ sau auth; loading/empty/error không giữ KPI cũ.
10. Không sửa `.env`, schema/Entity hoặc migration nếu không có quyết định thay đổi phạm vi riêng.
