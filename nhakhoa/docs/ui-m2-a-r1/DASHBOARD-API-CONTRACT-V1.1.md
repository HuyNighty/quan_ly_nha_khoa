# N08 — Hợp đồng Dashboard chỉ đọc V1.1 / R1

Nguồn: BA V1, disposition V1.1 và `REVIEW-UI-M2-A-R1.md` ngày 19/10/2026.
Các trường mở rộng đã được BA chấp nhận vì API chưa phát hành. Hợp đồng này giữ mọi trường V1; không cấp quyền triển khai mutation.

## Cổng phát hành

Route dự kiến `GET /manage/api/v1/admin/dashboard` **CHƯA ĐƯỢC TẠO**. Chỉ phát hành khi AUTH-R1 trong cùng checkout đã chứng minh identity thực, tài khoản hoạt động và quyền `AD` bằng test 401/403/200. Không xem bind localhost là biện pháp thay authorization.
Khi có endpoint: `Cache-Control: no-store`; không log payload/token; lỗi lõi map 503 theo chính sách lỗi đã duyệt; không tạo JWT/ACL mới.
`ROLE_AD` chỉ là convention nếu security thực dùng `hasRole("AD")`; R1 không tự giả định cách ánh xạ identity.

`source="LIVE"` ở read model chỉ chỉ nguồn truy vấn backend; không phải quyền bật Swing LIVE. Preview vẫn là mô phỏng và chưa bị thay đổi.

## Cấu trúc JSON và tương thích

Tên field là tên record Java. Các section/field V1 giữ nguyên; bảng dưới liệt kê đầy đủ hình dạng và phần mở rộng.
Các số đếm dùng JSON number nguyên, tiền dùng chuỗi thập phân nguyên VND để tránh mất chính xác. Client phải đọc count lớn bằng kiểu phù hợp và không cộng các count có định nghĩa khác nhau.

| Đường dẫn | Kiểu / định nghĩa |
|---|---|
| `source`, `businessDate`, `zoneId`, `generatedAt` | `LIVE`, ngày ISO `yyyy-MM-dd`, `Asia/Ho_Chi_Minh`, thời điểm ISO có offset `+07:00` |
| `period.startInclusive`, `period.endExclusive` | Hai mốc ISO offset tạo khoảng ngày nửa mở `[D 00:00, D+1 00:00)` |
| `appointments.totalRecords` | Tổng bản ghi hẹn theo `thoiGianHen` trong ngày, gồm hủy/đổi lịch, không phải bệnh nhân duy nhất |
| `appointments.byStatus` | Đủ `CHO_XAC_NHAN, DA_XAC_NHAN, DA_DEN, HOAN_THANH, VANG_MAT, DA_HUY, DA_DOI_LICH`; thiếu bản ghi trả 0 |
| **`appointments.activeCount`** | **Mở rộng được duyệt:** `CHO_XAC_NHAN + DA_XAC_NHAN + DA_DEN`, không thay `totalRecords` |
| `exams.waiting`, `inProgress`, `concluded`, `unit` | Số phiếu tiếp nhận trong ngày theo 3 trạng thái; unit `PHIEU_KHAM_TIEP_NHAN_TRONG_NGAY`; nhãn “Phiếu chờ khám” |
| `collections.amountVnd`, `byMethodVnd`, `definition` | Tổng `ThanhToan.THANH_CONG` theo thời điểm trong ngày, đủ `TIEN_MAT/CHUYEN_KHOAN/THE_POS`; definition `THANH_TOAN_THANH_CONG_TRONG_NGAY_THEO_TRANG_THAI_HIEN_TAI` |
| `receivables.amountVnd`, `invoiceCount`, `scope` | Công nợ hiện tại mỗi hóa đơn khác `DA_HUY`, kể cả đang chờ duyệt hủy; scope `TAT_CA_HOA_DON_CON_HIEU_LUC_HIEN_TAI` |
| **`receivables.overpaidInvoiceCount`** | **Mở rộng được duyệt:** số hóa đơn thu vượt, không bù vào công nợ hóa đơn khác |
| `staffing.status`, `shifts[]` | Từng ca gồm `shiftCode/shiftName/startTime/endTime/overnight/state/required/assigned/missing`; từng bộ người gồm `bacSi/yTa/leTan` |
| **`staffing.understaffedShiftCount`** | **Mở rộng được duyệt:** số ca `THIEU_NHAN_SU`, không phải số người hoặc chấm công |
| `inventory.status`, `belowMinimumItemCount`, `nearExpiryLotCount`, `expiredLotCount`, `unknownExpiryLotCount`, `thresholdDays` | Phân biệt mã hàng và lô; HSD hôm nay là hết hạn; null HSD không tính vào tồn khả dụng |
| **`inventory.anomalyLotCount`** | **Mở rộng được duyệt:** lô tồn âm hoặc không gắn đúng một thuốc/vật tư; bị loại khỏi phép tính |
| `emergencyKit.status`, `lastCheckedAt`, `daysSinceLastCheck`, `message` | Metadata lần kiểm tra gần nhất `<= now`, không chứa chi tiết lâm sàng hoặc kết luận an toàn |
| **`emergencyKit.checkState`** | **Mở rộng được duyệt:** enum ở bảng dưới, có `CHUA_DU_THONG_TIN` để đáp ứng yêu cầu bao phủ R1 |
| **`emergencyKit.checkCycleDays`** | **Mở rộng được duyệt:** ngưỡng chu kỳ thực tế service đã giải quyết từ cấu hình/fallback |
| `treatmentDue.status`, `duePlanCount`, `overduePlanCount`, `horizonDays` | DISTINCT mã kế hoạch; `duePlanCount` **bao gồm** `overduePlanCount`; nhãn “Đến hạn (bao gồm quá hạn)” |
| `workQueue.workGroupCount`, `relatedObjectCount`, `items[]` | Số nhóm khác tổng lượt đối tượng liên quan; tổng có thể trùng, không phải unique, không dùng suy badge menu |
| `workQueue.items[]` | `{code,priority,title,objectCount,targetScreen,sourceMetric}`; chỉ xuất khi có nguồn hỗ trợ và count > 0; sắp KHAN → CHO_DUYET → THEO_DOI rồi code |
| **`warnings[]`** | **Hình dạng được duyệt:** `{code,section,message}`; thông báo Việt có dấu, không chứa SQL, tham số DB, PII/PHI hoặc nội dung exception |

`totalRecords = ΣbyStatus`; `amountVnd = ΣbyMethodVnd`. Công nợ aggregate tiền dòng và tiền thu **riêng từng hóa đơn**, rồi `max(billed-paid,0)`; không JOIN hai tập 1-N làm nhân tiền. Không làm tròn ngầm phần lẻ VND.

## Trạng thái và giá trị null

| `status` | Ý nghĩa |
|---|---|
| `AVAILABLE` | Nguồn truy vấn thành công; số 0 có ý nghĩa thật trong dữ liệu đang đọc |
| `UNAVAILABLE` | Section phụ không tính được do lỗi nguồn/thiếu dữ liệu; các count null, không giả bằng 0 |
| `UNCONFIGURED` | Chưa có ca hoặc danh mục chính thức để đánh giá; count null, không ngụ ý đủ/an toàn |

Các ngưỡng cấu hình đã giải quyết (`thresholdDays`, `checkCycleDays`, `horizonDays`) vẫn có thể có giá trị khi count null. `staffing.shifts=[]` khi không có dữ liệu hợp lệ; `emergencyKit.checkState=null` khi UNAVAILABLE hoặc UNCONFIGURED.
Section lõi (appointments/exams/collections/receivables) lỗi làm thất bại toàn read model bằng `DashboardDataUnavailableException`; tương lai HTTP 503, không trả payload toàn 0.

| `emergencyKit.checkState` khi AVAILABLE | Ý nghĩa / nhãn hiển thị |
|---|---|
| `CHUA_CO_KIEM_TRA` | Chưa có lần kiểm tra tại hoặc trước hiện tại — cần kiểm tra |
| `CHUA_DU_THONG_TIN` | Cần kiểm tra: thiếu bao phủ danh mục chính thức, thiếu chi tiết hoặc thông tin hạn dùng/số lượng chưa xác nhận |
| `CAN_XU_LY` | Đã có dữ liệu kiểm tra đầy đủ nhưng kết quả/mục có yêu cầu bổ sung hoặc hạn dùng không vượt ngưỡng |
| `QUA_CHU_KY` | Đã quá chu kỳ ngày kiểm tra — cần kiểm tra |
| `TRONG_CHU_KY` | Đã kiểm tra đủ danh mục hiện hành trong chu kỳ, không phát hiện điều kiện cần xử lý từ dữ liệu hỗ trợ; **không phải chứng nhận an toàn lâm sàng** |

Mục chính thức = `dangApDung=true, laDuLieuMau=false`. Không dùng chi tiết mục mẫu để lấp phần thiếu của mục chính thức.
Entity chưa có cờ miễn hạn dùng hoặc lịch sử phiên bản danh mục: HSD null **không được suy là không cần HSD**; không thể chứng minh lịch sử thay đổi cấu hình chỉ từ thời điểm kiểm tra. Khi thiếu thông tin phải giữ nhãn cần kiểm tra, không thêm schema để giả định.

**Gate mapping còn thiếu:** test SQL Server hiện phát hiện mapping `MapsId` sinh đồng thời cột camelCase và snake_case trong `ChiTietKiemTraCapCuu`. `EntityManager.persist` chi tiết chưa điền đủ các cột NOT NULL, nên không được coi việc đọc fixture thành công là nghiệm thu vòng đời dữ liệu thật. Bộ test đọc dùng hàng fixture điền đầy đủ các cột hiện có, giữ FK; gate persist độc lập phải được xử lý bằng task Entity/schema riêng được phê duyệt trước khi đóng tích hợp. R1 không sửa Entity hoặc database production.

## Khóa cấu hình được chấp nhận

| `ThamSoHeThong.ten` | Fallback ngày |
|---|---:|
| `SO_NGAY_CANH_BAO_HSD` | 30 |
| `CHU_KY_KIEM_TRA_CAP_CUU_NGAY` | 7 |
| `SO_NGAY_AN_TOAN_CAP_CUU` | 30 |
| **`SO_NGAY_KE_HOACH_DEN_HAN`** | **14 — BA R1 đã chấp nhận khóa này** |

Thiếu/blank/không hợp lệ/lỗi đọc dùng fallback và thêm `THAM_SO_CHUA_CAU_HINH`, `THAM_SO_KHONG_HOP_LE` hoặc `THAM_SO_KHONG_DOC_DUOC`. Chỉ đọc, không INSERT `ThamSoHeThong`. Biên cấu hình hiện hỗ trợ `1..3650` ngày.

## Phạm vi thời gian, overlap và consistency

- Thu trong ngày là **trạng thái hiện tại của giao dịch**, không phải sổ kế toán đã khóa sổ lịch sử.
- Cảnh báo `THU_TREN_HOA_DON_DA_HUY` đối soát **toàn hệ thống, không giới hạn ngày**; không phải số thu hoặc anomaly chỉ trong ngày đang xem.
- `duePlanCount` là tập bao gồm quá hạn; một kế hoạch có thể xuất ở cả `KE_HOACH_DEN_HAN` và `KE_HOACH_QUA_HAN`. Không cộng hai dòng để suy số kế hoạch duy nhất.
- `relatedObjectCount` là tổng **lượt liên quan**, không unique. Chưa có `uniqueSubjectCount`.
- R1 không hứa một snapshot nguyên tử cho toàn Dashboard. Các lần đọc READ_COMMITTED có thể quan sát các thời điểm commit khác nhau; `generatedAt` là mốc sinh báo cáo, không phải timestamp khóa sổ. Nếu cần snapshot nhất quán xuyên section phải chốt phương án isolation và vận hành DB ở task riêng; không tự bật snapshot isolation ở production.

## Khóa điều hướng và phần hoãn

Đối chiếu source UI-M1 hiện có, không bật HTTP/LIVE:

| Miền | `targetScreen` đúng trong MenuDefinition |
|---|---|
| Lịch hẹn | `LICH_HEN` |
| Phiếu khám | `PHIEU_KHAM` |
| Hóa đơn / công nợ / thu vượt | `HOA_DON` |
| Yêu cầu hủy chờ duyệt | `DUYET_HUY` |
| Phân công | `PHAN_CONG` |
| Thuốc / vật tư / hạn dùng | `THUOC_VAT_TU` |
| Điều chỉnh kho | `KIEM_KE` |
| Hộp cấp cứu | `CAP_CUU` |
| Kế hoạch điều trị đến/quá hạn | `KE_HOACH` |

Keys cũ giả định `PHAN_CONG_CA/TON_KHO/DIEU_CHINH_KHO/KE_HOACH_DIEU_TRI` không được gửi làm đích điều hướng.
Việc đối chiếu này là kiểm tra source; nghiệm thu UI-M2-B và luồng LIVE vẫn chờ gate AUTH/API. Không sửa UI-M1 trong R1.
Hoãn trường “Đã có lịch” của kế hoạch và nhóm bàn giao ca khỏi Dashboard V1.1. Không suy từ kế hoạch rằng mọi hạng mục đã đặt hẹn.
