# UI-DEMO-1 — Báo cáo bàn giao (Tiếp nhận & lịch hẹn)

**Ngày:** 19/10/2026 · **Phạm vi:** chỉ `admin-desktop-preview/` · **Trạng thái:** DONE-MOCK 3/3 màn của wave 1

> Toàn bộ là **DỮ LIỆU MÔ PHỎNG**. Không JDBC, không HTTP/REST, không Entity JPA, không ghi DB.
> Mọi thao tác chỉ đổi bộ nhớ của phiên preview và báo `Đã cập nhật dữ liệu mô phỏng — không lưu vào hệ thống thật`.

## 1. Báo cáo theo mẫu file 02 §10.2

| Mục | Nội dung |
|---|---|
| Branch/HEAD ban đầu | `main @ 0f60ad9` (đã merge PR #1 `codex/ui-m2-a-r1`). Working tree: 123 file `M` chỉ khác CRLF/LF; thay đổi nội dung duy nhất là `nhakhoa/src/main/resources/application.yaml` (cấu hình DB đã sửa trước đó, không thuộc wave này). |
| Branch/HEAD sau khi làm | Không đổi. **Không commit / push / merge.** Chỉ thêm/sửa file trong `admin-desktop-preview/`. |
| Số màn DONE-MOCK | Trước: **1/21** (Tổng quan) · Sau: **4/21** (Tổng quan + `TIEP_NHAN`, `LICH_HEN`, `BENH_NHAN`). Còn **17** placeholder. |
| Key xác minh từ `MenuDefinition.java` | `TIEP_NHAN` (Tiếp nhận bệnh nhân), `LICH_HEN` (Lịch hẹn), `BENH_NHAN` (Hồ sơ bệnh nhân). Không đổi key nào. |
| Build/test | `javac --release 25 -Xlint:all -Werror` + JAR: **PASS**. `PreviewTests` (headless): **207/207 PASS** = 101 baseline UI-M1 + 106 kiểm tra UI-DEMO-1. Chạy bằng OpenJDK 25.0.4.1 (Linux headless). |
| Kiểm hình | Ảnh Swing thật (printAll) 1382×840 và 1280×760 cho 3 màn: `docs/evidence/ui-demo-1/`. Ở 1280×760 bảng hẹp hiện thanh cuộn ngang thay vì cắt chữ. |
| NOT VERIFIED | `build.ps1 -Test` trên Windows của PO; `--desktop` (vòng đời JFrame thật); DPI 125%/150%; font Segoe UI thật (môi trường build dùng font fallback). |

## 2. File thêm / sửa

**Thêm mới**
- `demo/DemoClinicStore.java` — kho in-memory: bác sĩ, bệnh nhân, lịch hẹn, phiếu khám, tóm tắt hồ sơ bệnh án (chỉ đọc), nhật ký kỹ thuật; quy tắc mô phỏng; `reset()`.
- `screens/ScreenContext.java`, `screens/SimpleDocumentListener.java`
- `screens/ReceptionPanel.java` (#02), `screens/AppointmentsPanel.java` (#03), `screens/PatientsPanel.java` (#04)
- `ui/DemoTable.java` (bảng sort/selection/empty state, cuộn ngang khi hẹp), `ui/DemoWidgets.java` (banner, form, feedback, timeline), `ui/DemoDialogs.java` (xác nhận, tách interface để test)
- `src/test/.../DemoWave1Tests.java`, tài liệu này, ảnh `docs/evidence/ui-demo-1/*.png`

**Sửa (tối thiểu)**
- `AdminShell.java`: thêm constructor nhận `DemoClinicStore` + `DemoDialogs`; đăng ký 3 màn vào CardLayout; key chưa làm vẫn về placeholder cũ.
- `UiComponents.java`: nút bị khóa vẽ nền trung tính (trước đó trông như còn bấm được).
- `AdminPreviewFrame.java`, `SidebarMenu.java`: nhãn phiên bản "UI-DEMO-1".
- `PreviewTests.java`: gọi `DemoWave1Tests.run()`; **không sửa/bỏ assertion baseline nào**.

## 3. Dữ liệu mẫu và liên kết đã kiểm

- Mốc thời gian `--fixture`: **12/10/2026 07:45** Asia/Ho_Chi_Minh (giống Tổng quan). `--runtime`: seed theo ngày hiện tại, vẫn là mock.
- Bác sĩ: `NV002`, `NV003` trực; `NV004` không trực (để thử chặn).
- Bệnh nhân `BN000001–BN000012` (tên/SĐT `09000001xx`/địa chỉ đều tổng hợp). Cặp nghi trùng có sẵn `BN000007` ↔ `BN000011` (cùng họ tên + ngày sinh).
- Lịch `LH000001–LH000010` trong ngày = **2 chờ xác nhận / 2 đã xác nhận / 1 đã đến / 2 hoàn thành / 1 vắng / 1 hủy / 1 đổi lịch**; `LH000009 → LH000011` (D+2) có liên kết 2 chiều. Còn hiệu lực = 8.
- Phiếu `PK000001–PK000003`: **0 chờ / 1 đang khám / 2 đã kết luận**, đều gắn đúng lịch và bệnh nhân.
- Test đối soát: 7 trạng thái lịch và 0/1/2 phiếu **khớp fixture Tổng quan**.
- **Tổng quan vẫn là fixture độc lập**: thao tác ở 3 màn mới không cập nhật KPI Tổng quan (có ghi trên banner). Ghép Dashboard vào store là việc riêng.

## 4. Luồng tương tác và state đã triển khai

**#02 Tiếp nhận bệnh nhân (`TIEP_NHAN`)**
- Tìm theo mã / họ tên không dấu / SĐT; card bệnh nhân; lịch hôm nay; chọn chế độ *Theo lịch hẹn* hoặc *Tiếp nhận trực tiếp*; chọn bác sĩ trực; timeline thao tác.
- Check-in **chỉ với lịch `DA_XAC_NHAN`**, đúng ngày hôm nay, bác sĩ đang trực. Lịch `CHO_XAC_NHAN` bị chặn, có nút "Mở Lịch hẹn để xác nhận". Chống tạo trùng phiếu theo mã lịch. Sau check-in: phiếu `CHO_KHAM`, lịch → `DA_DEN`.
- Tiếp nhận trực tiếp: bắt buộc lý do; chặn khi bệnh nhân đang có phiếu chưa kết thúc.
- Thêm bệnh nhân có validate tiếng Việt. Nghi trùng (cùng SĐT hoặc họ tên + ngày sinh) phải xác nhận mới tạo hồ sơ **riêng**; **không tự gộp**.
- State: chưa chọn BN, không tìm thấy, nghi trùng, lịch chờ xác nhận, lịch đã xác nhận, đã check-in, không có bác sĩ, form thiếu dữ liệu.

**#03 Lịch hẹn (`LICH_HEN`)**
- Điều hướng ngày (trước / sau / hôm nay); 9 ô thống kê (tổng, còn hiệu lực, 7 trạng thái); lọc theo bác sĩ / trạng thái / từ khóa; xóa bộ lọc; bảng sort được; panel chi tiết.
- Xác nhận `CHO_XAC_NHAN → DA_XAC_NHAN`. Hủy (bắt buộc lý do 5–200 ký tự). **Đổi lịch tạo bản mới `CHO_XAC_NHAN`**, bản cũ → `DA_DOI_LICH`, có liên kết gốc ↔ mới và nút mở lịch liên kết. Tạo lịch mới với các kiểm tra: sau hiện tại, trong 07:00–20:00, bước 15 phút, không trùng khung giờ của bác sĩ hoặc bệnh nhân.
- Lịch hoàn thành / vắng / hủy / đã đổi: nút khóa, có tooltip lý do. Ngày không có lịch hoặc không khớp bộ lọc: hiện empty state có hướng dẫn.

**#04 Hồ sơ bệnh nhân (`BENH_NHAN`)**
- Danh sách + lọc (Tất cả / Có lịch hôm nay / Nghi trùng hồ sơ). 4 tab: *Hành chính*, *Lịch & phiếu*, *Bệnh án (tóm tắt)*, *Hoạt động*.
- SĐT che mặc định (`090****109`), chỉ hiện khi người dùng chủ động bấm. Sửa hành chính (SĐT / địa chỉ / người liên hệ) có xác nhận + validate; SĐT trùng phải xác nhận thêm. Họ tên / ngày sinh không sửa ở demo.
- Tab chuyên môn **chỉ đọc**, ghi rõ "Quản trị viên không ghi chẩn đoán".

**Chung**
- Nhãn `DỮ LIỆU MÔ PHỎNG · Không kết nối hệ thống thật` trên mỗi màn và trong mọi hộp thoại xác nhận.
- Nút "Khôi phục dữ liệu mẫu" có xác nhận.

## 5. Giả định / vấn đề BA còn mở (cần PO/BA chốt)

1. Lịch mới tạo bằng **đổi lịch** đang ở trạng thái `CHO_XAC_NHAN` (BA Q16 chỉ nói "tạo bản mới giữ bản cũ").
2. Khung giờ **07:00–20:00, bước 15 phút**, trùng slot = cùng giờ bắt đầu: đây là giả định demo, chưa có trong BA.
3. Hủy lịch được phép ở `CHO_XAC_NHAN` / `DA_XAC_NHAN`; lý do 5–200 ký tự.
4. Tiêu chí nghi trùng: cùng SĐT, **hoặc** cùng họ tên (bỏ dấu) + ngày sinh.
5. Che SĐT áp dụng cho vai trò Quản trị trong demo. Chính sách hiển thị theo từng vai trò chưa có BA.

## 6. Những gì KHÔNG triển khai

REST / JWT / Spring Security, JDBC / SQL, Entity / migration, `--live`, gửi SMS / email, gộp hồ sơ, sửa họ tên / ngày sinh, ghép KPI Tổng quan vào store demo, 17 màn của wave 2–6.

## 7. Gate PO cần nghiệm thu

Mở `--fixture` trên Windows, kiểm tra 3 màn ở 1382×840 và 1280×760:
- **Tiếp nhận:** check-in `LH000005`, thử `LH000008` (phải bị chặn), thêm bệnh nhân trùng SĐT `0900000101`.
- **Lịch hẹn:** xác nhận / hủy / đổi lịch.
- **Hồ sơ bệnh nhân:** sửa SĐT, kiểm tra cặp nghi trùng `BN000007` / `BN000011`.

Ngoài ra kiểm bàn phím, tooltip và DPI 125%/150%.

## 8. Làm lại UI/UX (bản 2 — 19/10/2026 tối)

Lý do: bản đầu chạy trên Windows hiện ký tự ✓/⚠ thành ô vuông (Segoe UI thiếu glyph), dùng control Metal thô (combo, radio, tab, scrollbar) và bố cục chưa theo luồng thao tác.

- **Bộ component phẳng mới `ui/Flat.java`:** ô nhập có placeholder và icon tìm kiếm, combo bo góc, segmented control (thay radio / tab), thanh cuộn mảnh, avatar chữ cái đầu, viên trạng thái, khối thông báo có icon Java2D tự xuống dòng. **Không còn dùng ký tự biểu tượng Unicode.**
- **`DemoTable`:** header phẳng chữ hoa, dòng xen kẽ + hover, trạng thái vẽ dạng viên màu, chiều cao co theo số dòng, cột mã/giờ không bị cắt, empty state có icon.
- **Tiếp nhận:** bố cục 3 bước (Tìm bệnh nhân → Bệnh nhân → Tiếp nhận), chọn hình thức bằng segmented "Theo lịch hẹn / Trực tiếp (không hẹn)", hiện tuổi và avatar.
- **Lịch hẹn:** điều hướng ngày gọn, thẻ thống kê **bấm để lọc nhanh** theo trạng thái, panel chi tiết dạng lưới nhãn–giá trị có viên trạng thái, form tạo / đổi lịch thay chỗ panel chi tiết (không đẩy trang dài xuống).
- **Hồ sơ bệnh nhân:** lọc nhanh bằng segmented, header hồ sơ có avatar + tuổi, 4 tab segmented, form sửa hành chính đặt trong khung riêng.
- Test: vẫn **207/207 PASS** (cập nhật selector cho segmented); ảnh mới trong `docs/evidence/ui-demo-1/`.

## 9. Bản 3 — thanh tìm kiếm & danh sách avatar (19/10/2026 tối)

- **Thanh tìm kiếm mới (`Flat.SearchBar`):** cao 46px, icon, viền sáng khi focus, nút xóa nhanh khi có chữ, gợi ý **Ctrl+F**. Enter hoặc mũi tên xuống chọn kết quả đầu tiên; Esc xóa từ khóa.
- **Danh sách bệnh nhân dạng thẻ (`CardList`):** avatar tròn màu ổn định theo mã BN, họ tên đậm, dòng mô tả mã · giới tính · tuổi · SĐT che. Viên trạng thái lịch hôm nay hoặc "Nghi trùng" nằm cùng dòng tên. Dòng đang chọn có vạch nhấn trái. Áp dụng cho cả Tiếp nhận và Hồ sơ bệnh nhân.
- **Tiếp nhận:** lọc nhanh Tất cả / Lịch hôm nay / Nghi trùng. Thêm bệnh nhân hiện thay chỗ danh sách. Hồ sơ tóm tắt có avatar 68px và các nhãn (mã, giới tính · tuổi, SĐT, số lịch hôm nay). Lịch hôm nay dạng thẻ có khối giờ lớn. Nút check-in lớn đặt cạnh ô chọn bác sĩ.
- **Hồ sơ bệnh nhân:** header avatar 68px + nhãn mã / tuổi / mã HSBA.
- Test: **211/211 PASS** (thêm kiểm tra Enter chọn kết quả, nút xóa nhanh, lọc nghi trùng).

## 10. Menu hướng nghiệp vụ (19/10/2026, theo yêu cầu PO)

Nhóm menu đổi từ "theo bộ phận" sang **theo giai đoạn hoạt động thực tế**, giống mô hình Tổ chức → Kế hoạch → Tiếp nhận → Hoạt động → Tổng kết. **Khóa màn hình giữ nguyên**, nên router, Tổng quan và các CTA không đổi.

| # | Nhóm (giai đoạn) | Màn hình |
|---|---|---|
| — | (độc lập) | Tổng quan |
| 1 | Tổ chức | Nhân viên · Tài khoản & vai trò · Bảng giá dịch vụ · Nhà cung cấp |
| 2 | Lập kế hoạch | Lịch hẹn · Phân công & lịch làm việc |
| 3 | Tiếp nhận | Tiếp nhận bệnh nhân · Hồ sơ bệnh nhân |
| 4 | Khám | Phiếu khám |
| 5 | Điều trị | Kế hoạch điều trị · Tái khám |
| 6 | Thanh toán | Hóa đơn & thu tiền · Duyệt hủy hóa đơn |
| 7 | Cung ứng | Thuốc & vật tư · Nhập / xuất / hoàn trả · Kiểm kê & hao hụt · Hộp cấp cứu |
| 8 | Tổng kết | Bàn giao ca · Đánh giá nhân viên · Nhật ký thao tác |

- Tổng: 4 + 2 + 2 + 1 + 2 + 2 + 4 + 3 = **20** + Tổng quan = **21**.
- Badge nhóm = số nhóm việc Tổng quan trỏ tới: 1/1/0/0/1/1/4/1, tổng **9**.
- Baseline test "6 nhóm" đổi thành "8 nhóm" theo quyết định PO. Thêm 11 kiểm tra cho thứ tự nhóm, tách nhóm và badge. Tổng **222/222 PASS**.

**Còn thiếu / cần PO quyết:** nhóm Tổng kết chưa có báo cáo số liệu (doanh thu theo kỳ, công nợ, tồn kho / hết hạn, thống kê lượt khám). Thêm các màn này sẽ vượt khung 21 màn đã duyệt, cần duyệt riêng.


## 11. Làm toàn bộ 21/21 màn theo menu nghiệp vụ (19/10/2026)

Không còn placeholder. Mọi màn dùng chung `DemoClinicStore` (thêm 4 slice: `StaffSlice`, `ClinicalSlice`, `BillingSlice`, `SupplySlice`), có nút **Khôi phục dữ liệu mẫu**, nhãn **DỮ LIỆU MÔ PHỎNG**, mọi thao tác ghi vào Nhật ký.

| Nhóm | Màn (class) | Điểm nghiệp vụ chính đã mô phỏng |
|---|---|---|
| Tổ chức | Nhân viên (`StaffPanel`), Tài khoản & vai trò (`AccountsPanel`), Bảng giá (`PricePanel`), Nhà cung cấp (`SupplierPanel`) | Chặn nghỉ việc khi còn lịch/ca; giữ ≥ 1 quản trị hoạt động; đổi giá có lý do, hóa đơn cũ giữ giá đã chốt; NCC ngừng hợp tác không nhập được |
| Lập kế hoạch | Lịch hẹn, Phân công ca (`ShiftPanel`) | Định mức BS/YT/LT theo ca; ca chiều, ca tối thiếu 1 YT |
| Tiếp nhận | Tiếp nhận, Hồ sơ bệnh nhân | (wave 1) |
| Khám | Phiếu khám (`ExamPanel`) | ADMIN không ký kết luận thay bác sĩ; chỉ định CLS, kết luận theo bác sĩ phụ trách |
| Điều trị | Kế hoạch điều trị (`PlanPanel`), Tái khám (`FollowUpPanel`) | Q12: đến hạn 14 ngày gồm quá hạn = 6 mục / 5 kế hoạch; dời ngày cần lý do |
| Thanh toán | Hóa đơn & thu tiền (`InvoicePanel`), Duyệt hủy (`CancelApprovalPanel`) | Q07: hóa đơn chờ hủy vẫn tính nợ; không thu quá nợ; hủy giao dịch giữ dấu vết; người lập ≠ người duyệt |
| Cung ứng | Thuốc & vật tư (`StockPanel`), Nhập/xuất/hoàn trả (`VoucherPanel`), Kiểm kê & hao hụt (`AdjustPanel`), Hộp cấp cứu (`KitPanel`) | Q09: khả dụng chỉ tính lô còn hạn sau D; xuất FEFO, không xuất lô hết hạn; hoàn trả ≤ số đã xuất; điều chỉnh có snapshot, người lập ≠ người duyệt; hộp cấp cứu chu kỳ 7 ngày, không bao giờ báo “đạt an toàn” |
| Tổng kết | Bàn giao ca (`HandoverPanel`), Đánh giá nhân viên (`EvaluationPanel`), Nhật ký thao tác (`AuditPanel`) | Chỉ người nhận ca xác nhận; có chênh lệch phải ghi chú; 5 tiêu chí MẪU 1–5, không tự đánh giá; nhật ký lọc người/loại/ngày, không chứa dữ liệu sức khỏe |

Các màn có thao tác “duyệt / xác nhận” dùng ô **Thao tác với tư cách** để giả lập người thao tác (không có đăng nhập thật).

Kiểm thử: thêm `DemoWave2Tests` (quy tắc store, router 20 màn, luồng Cung ứng / Tổng kết, ảnh `build/screenshots/demo2-*.png`). Tổng **309 kiểm tra ĐẠT**. Tổng quan vẫn là fixture độc lập; seed store khớp số liệu (công nợ 3.600.000 đ / 5 hóa đơn, thu 700.000 đ, dưới định mức 2, cận hạn 2, hết hạn 1, hộp cấp cứu kiểm tra lần cuối D−7).
