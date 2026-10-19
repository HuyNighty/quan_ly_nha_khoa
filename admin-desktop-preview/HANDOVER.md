# N08 — Bàn giao Admin UI-M1

Ngày thực hiện: **19/10/2026**, giờ người dùng Asia/Bangkok. Thời gian trong giao diện: **Asia/Ho_Chi_Minh** theo đặc tả.

## 1. Khảo sát checkout trước triển khai

| Nội dung | Quan sát thực tế |
|---|---|
| Repository / remote | `HuyNighty/quan_ly_nha_khoa` / `https://github.com/HuyNighty/quan_ly_nha_khoa.git` |
| Nhánh / HEAD | `main` / `4d25dbf88be6415a8fb77facf64b5d4ea9e1f704` |
| Working tree ban đầu | `git status --short` rỗng; không có thay đổi người dùng phải di chuyển hoặc ghi đè |
| Module thực tế | Một Maven project tại `nhakhoa/pom.xml`; không có aggregator ở root |
| Java / Spring | `java.version=25`, Spring Boot parent `4.1.1`, WebMVC, Data JPA, Validation, SQL Server driver, Lombok |
| JDK / Maven máy hiện tại | Temurin OpenJDK `25.0.4.1`, `javac 25.0.4.1`, Maven `3.9.11` |
| Package / điểm vào | `com.manage.nhakhoa`, `NhakhoaApplication` chỉ khởi động Spring |
| Backend hiện có | Các entity/enums theo feature tiếng Việt; test Spring context; `application.yaml` cấu hình datasource từ biến môi trường |
| UI thực tế | Không có source Swing, JavaFX, web frontend, template hoặc package UI hiện hữu |
| Bảo mật / AUTH-R1 | Có enum vai trò `AD, LT, BS, YT, QL, KHO` và trạng thái tài khoản. Không thấy implementation security/JWT/login/AUTH-R1 hoặc dependency Spring Security trong checkout này. Không kết luận trạng thái ở nhánh/máy khác. |

Tài liệu `README.md` mô tả prototype Swing/JDBC cũ; `nhakhoa/docs/ARCHITECTURE.md` và `M1_CONFIGURATION.md` mô tả một kiến trúc `com.quanlynhakhoa`, Swing cùng JVM, service phân quyền mà source hiện tại không có.
Các báo cáo Derby/UI legacy cũng không phải kết quả kiểm thử checkout này. Không áp dụng các chỉ dẫn commit, migration, ACL hoặc database cũ vào UI-M1.

Đã đọc đầy đủ prompt BA và review được cung cấp, ba Markdown trong `Downloads/menu-admin.zip` và xem cả bốn PNG.
`N08_Admin_UI_M1_BA_Codex_VI.zip` chỉ có hai Markdown BA; bộ ảnh nằm riêng trong `menu-admin.zip` ở Downloads.
Nguồn ưu tiên: yêu cầu người dùng → prompt đã rà soát BA → review BA → thiết kế ZIP. Ảnh chuẩn là **01-tong-quan.png**; ảnh 02/03 chỉ tham khảo component; ảnh A không dùng để trộn layout.
ZIP không có `AdminMenu.java`, `SidebarMenu.java` hoặc source Java để tái sử dụng.

Vì checkout chưa có frontend, triển khai thư mục **`admin-desktop-preview/` độc lập**. Không xác lập kiến trúc frontend production và không thêm Spring vào preview.

## 2. Kết quả và phạm vi dữ liệu

Đã có khung Admin, menu dữ liệu 21 khóa / 6 nhóm, tìm không dấu/tên nhóm/mã nghiệp vụ, thu/mở nhóm, lựa chọn và key binding bàn phím.
Topbar có breadcrumb, nhãn mô phỏng, người dùng/role demo và hai mục xác thực bị vô hiệu hóa có lý do. Các icon tự vẽ Java2D.
Dashboard gồm 4 KPI, 9 dòng cảnh báo trong 3 nhóm ưu tiên, thanh/chú giải đủ 7 trạng thái lịch hẹn, 3 ca làm, 3 chỉ số thuốc/vật tư và cảnh báo hộp cấp cứu.

Bộ mẫu đối soát: **10 = 2+2+1+2+1+1+1** lịch hẹn; phiếu **0/1/2**; đã thu **700.000 đ** toàn bộ tiền mặt; công nợ **3.600.000 đ / 5 hóa đơn**; **9 nhóm việc / 18 đối tượng liên quan**; ca chiều/tối thiếu 1 y tá; kho **2/2/1**.
Badge menu `0/6/2/2/1/1` là số mẫu độc lập, có tooltip giải nghĩa.

Provider có kiểu riêng, trả DTO bất biến không liên hệ entity JPA. Có nguồn mẫu đầy đủ/rỗng/lỗi/không có việc/không có lịch.
`Clock` được inject; chế độ ảnh cố định là **12/10/2026 07:45**. Runtime lấy ngày hiện tại nhưng vẫn ghi rõ số liệu mẫu.
Tải lại chạy nền; gỡ số cũ khi tải/lỗi/rỗng; hủy yêu cầu cũ và chỉ render kết quả mới nhất trên EDT. Chuyển trang trong lúc tải không bị đưa lại Dashboard khi tải xong.

20 trang khác chỉ hiển thị chính xác **“Màn hình này chưa được triển khai ở giai đoạn UI-M1”**, có breadcrumb và đường về Tổng quan.
CTA hủy/kiểm kê/tài khoản chỉ mở trang chuyên trách. Không có thao tác duyệt, hủy, mở khóa, lâm sàng, CRUD tiếp nhận/lịch hẹn, hoặc báo thành công giả.

## 3. File tạo mới

Tất cả đường dẫn dưới đây tương đối với `admin-desktop-preview/`; **không sửa file đã tồn tại trong repository**.

| File | Mục đích |
|---|---|
| `.gitignore` | Bỏ qua đầu ra `build/` |
| `build.ps1` | Build JDK 25, kiểm warning như lỗi, đóng gói JAR và tùy chọn kiểm thử |
| `README.md` | Cách chạy, nguồn dữ liệu, thao tác và giới hạn |
| `BA-OPEN-QUESTIONS.md` | 16 mục CHƯA DUYỆT trước UI-M2 |
| `HANDOVER.md` | Khảo sát, danh sách file, ma trận nghiệm thu và giới hạn |
| `src/main/java/com/manage/nhakhoa/desktop/AdminPreview.java` | Điểm vào, chọn Clock runtime/fixture |
| `src/main/java/com/manage/nhakhoa/desktop/AdminPreviewFrame.java` | JFrame và vòng đời riêng |
| `src/main/java/com/manage/nhakhoa/desktop/AdminShell.java` | CardLayout/router, vùng cuộn, trang chưa triển khai |
| `src/main/java/com/manage/nhakhoa/desktop/MenuDefinition.java` | 21 khóa màn hình, 6 nhóm và tìm không dấu |
| `src/main/java/com/manage/nhakhoa/desktop/DashboardPanel.java` | Ghép Dashboard và render bốn trạng thái tải |
| `src/main/java/com/manage/nhakhoa/desktop/DashboardLoadController.java` | SwingWorker, chống ghi đè bởi tải cũ, đóng/hủy |
| `src/main/java/com/manage/nhakhoa/desktop/data/DashboardDataProvider.java` | Hợp đồng nguồn đọc độc lập |
| `src/main/java/com/manage/nhakhoa/desktop/data/DemoDashboardDataProvider.java` | Bộ mẫu ảnh 01 và các tình huống kiểm thử |
| `src/main/java/com/manage/nhakhoa/desktop/model/DashboardViewModel.java` | DTO, enum UI và kiểm tra tính nhất quán của mẫu |
| `src/main/java/com/manage/nhakhoa/desktop/ui/Theme.java` | Token, font Segoe UI → Dialog, định dạng, màu 7 trạng thái |
| `src/main/java/com/manage/nhakhoa/desktop/ui/UiComponents.java` | RoundedPanel, UiCard, ScreenHeader, nút, badge, tile, KPI, TaskRow, nhóm ưu tiên, thanh tỷ lệ, ca, chỉ số, cảnh báo |
| `src/main/java/com/manage/nhakhoa/desktop/ui/UiIcons.java` | Icon Java2D |
| `src/main/java/com/manage/nhakhoa/desktop/ui/SidebarMenu.java` | Tìm/thu/mở/chọn, focus và sự kiện điều hướng |
| `src/main/java/com/manage/nhakhoa/desktop/ui/TopBar.java` | Breadcrumb, danh tính mẫu, menu chưa tích hợp xác thực |
| `src/test/java/com/manage/nhakhoa/desktop/PreviewTests.java` | Kiểm thử model, provider, concurrency, Swing/router/key binding, ảnh và vòng đời JFrame |
| `docs/evidence/build-test.txt` | Output build và 101 kiểm tra không cần màn hình |
| `docs/evidence/desktop-test.txt` | Output 105 kiểm tra, gồm 4 kiểm tra JFrame thực |
| `docs/evidence/*.png` | 11 ảnh render được liệt kê ở mục 5 |

Backend `nhakhoa/pom.xml`, YAML, Entity, database/JPA, backend service, security/JWT và mọi file cũ giữ nguyên. Không kết nối database, chạy migration hay dùng secret.
`jdeps -s build/admin-desktop-preview.jar` chỉ liệt kê **java.base**, **java.desktop**.
Không tạo commit, push, merge hoặc thay nhánh.

## 4. Kiểm thử và ma trận nghiệm thu

Chạy tại repository root:

```powershell
.\admin-desktop-preview\build.ps1 -Test
```

Output thực tế:

```text
ĐẠT: biên dịch Java 25 với -Xlint:all -Werror và đóng gói JAR độc lập
ĐẠT: 101 kiểm tra; không có lỗi. Ảnh Swing tại build/screenshots.
```

Chạy tiếp trong `admin-desktop-preview/`, trên desktop Windows có màn hình:

```powershell
java '-Dstdout.encoding=UTF-8' '-Dstderr.encoding=UTF-8' -cp 'build/classes;build/test-classes' com.manage.nhakhoa.desktop.PreviewTests --desktop
```

```text
ĐẠT · JFrame thật mở đúng kích thước trên desktop
ĐẠT · Cửa sổ thật 1280×760 giữ nguyên hàng điều khiển và vùng cuộn
ĐẠT · Điều hướng không tạo cửa sổ mới
ĐẠT · Đóng JFrame giải phóng bản xem thử
ĐẠT: 105 kiểm tra; không có lỗi. Ảnh Swing tại build/screenshots.
```

105 là **101 kiểm tra chung + 4 kiểm tra desktop**, không phải 206 kiểm tra khác nhau.
Hai log đầy đủ nằm trong `docs/evidence/`. Ảnh đã được mở và kiểm tra trực quan; đã sửa nhãn menu bị trống khi render, ký hiệu thiếu glyph và nhãn bị giới hạn bề ngang.

| Tiêu chí mục 7 của prompt BA | Kết quả | Bằng chứng / giới hạn |
|---|---|---|
| 1. Chạy độc lập, không DB, tiếng Việt, nhãn mô phỏng | PASS | JAR chỉ phụ thuộc JDK; JFrame thực mở/đóng; ảnh Dashboard và label assertions |
| 2. Menu 21 mục / 6 nhóm, tìm/thu/mở/selection/keyboard | PASS | Kiểm đếm khóa, 4 truy vấn tìm thực tế, mã UC14, tìm không khớp, key binding Esc/Enter/Space/↑/↓; router đủ 21 trang |
| 3. KPI và link cùng LICH_HEN, task đúng đích, placeholder/breadcrumb | PASS | Click component thật, 9 CTA và 21 router assertions; không tạo thêm JFrame |
| 4. 10 lịch/7 trạng thái, zero/missing | PASS | Tổng mẫu, normalize thiếu thành 0, render 0 lịch không chia cho 0, mẫu 14 tỷ không tràn 32 bit |
| 5. 9 nhóm/18 đối tượng, không lẫn badge menu | PASS | Assertion model/label, tooltip giải nghĩa và nguồn badge menu riêng |
| 6. Đang tải/rỗng/lỗi/thử lại | PASS | Provider giả + provider demo, gỡ KPI cũ, nút thử lại phát lần tải mới; ảnh cả 3 trạng thái |
| 7. Fake Clock/runtime, tiền/ngày/múi giờ | PASS | 12/10 07:45 và 19/10 15:15; lời chào sáng/chiều/tối, ranh giới ngày, `700.000 đ`, `3.600.000 đ` |
| 8. Layout 1382×840 và 1280×760 | PASS tại môi trường hiện tại | Ảnh canvas Swing và nội dung JFrame thực; topbar 52, nội dung cuộn riêng, tooltip cho nhãn dài. Không khẳng định khớp từng pixel. DPI/font khác: CHƯA KIỂM CHỨNG |
| 9. Không mutation nguy hiểm hoặc xác thực giả | PASS trong phạm vi preview | Hai mục auth disabled; 9 CTA chỉ gọi router; không JDBC/HTTP/JPA/Spring. Không xác nhận authorization production |
| 10. Zero/long/large/error/concurrency/navigation/keyboard | PASS ở mức component và cửa sổ | Provider cũ cố ý bỏ qua interruption vẫn không ghi đè; close không cập nhật muộn; chuyển trang lúc tải; key binding thật. Chưa kiểm bằng thao tác bàn phím vật lý hoặc trình đọc màn hình |
| 11. Build/test, không đổi cờ bảo mật, không warning UI | PASS preview; backend CHƯA KIỂM CHỨNG | `javac -Xlint:all -Werror` và JAR đạt. Maven baseline thất bại vì môi trường cache bên dưới; không thay cấu hình để vượt qua |

Lệnh kiểm tra baseline backend **đã chạy trước khi thêm code UI**:

```powershell
mvn -B -f nhakhoa/pom.xml test-compile
```

Kết quả **FAIL do môi trường**, tại dependency Maven compiler:

```text
[INFO] BUILD FAILURE
[ERROR] java.io.FileNotFoundException:
C:\Users\huy\.m2\repository\org\codehaus\plexus\plexus-compiler-api\2.16.2\plexus-compiler-api-2.16.2.jar.lastUpdated (Access is denied)
```

Không xác nhận build/test Spring backend hoặc SQL Server PASS. Maven còn in warning `sun.misc.Unsafe` từ Guice của Maven; không phải warning do UI mới.
Không chạy `contextLoads` với cấu hình DB thật hoặc thay cờ security/JPA.

`git diff --check` và `git diff --exit-code HEAD -- nhakhoa README.md` không có diff. Các file mới được kiểm tra khoảng trắng cuối dòng riêng bằng `rg`; working tree chỉ thêm thư mục preview. HEAD/nhánh giữ nguyên.

## 5. Xem preview và ảnh bằng chứng

Tại repository root, sau build:

```powershell
java -jar admin-desktop-preview/build/admin-desktop-preview.jar --runtime
java -jar admin-desktop-preview/build/admin-desktop-preview.jar --fixture
```

Mỗi lệnh mở một lần chạy độc lập; đóng bằng nút đóng cửa sổ. Cửa sổ thử nghiệm trong test tự đóng khi kiểm tra xong.

| Ảnh trong `docs/evidence/` | Nội dung |
|---|---|
| [dashboard-1382x840.png](docs/evidence/dashboard-1382x840.png) | Canvas Swing chuẩn 1382×840 |
| [dashboard-1280x760.png](docs/evidence/dashboard-1280x760.png) | Canvas nhỏ, đầu nội dung |
| [dashboard-1280x760-bottom.png](docs/evidence/dashboard-1280x760-bottom.png) | Cuộn xuống cuối, hàng việc và CTA vẫn tiếp cận được |
| [desktop-1382x840-content.png](docs/evidence/desktop-1382x840-content.png) | Nội dung JFrame thực ở kích thước cửa sổ 1382×840 |
| [desktop-1280x760-content.png](docs/evidence/desktop-1280x760-content.png) | Nội dung JFrame thực ở kích thước cửa sổ 1280×760 |
| [loading-1280x760.png](docs/evidence/loading-1280x760.png) | Đang tải |
| [empty-1280x760.png](docs/evidence/empty-1280x760.png) | Không có dữ liệu |
| [error-1280x760.png](docs/evidence/error-1280x760.png) | Lỗi nguồn mẫu, thử lại |
| [placeholder-1280x760.png](docs/evidence/placeholder-1280x760.png) | Trang chưa triển khai và breadcrumb |
| [zero-appointments-1280x760.png](docs/evidence/zero-appointments-1280x760.png) | Đủ 7 trạng thái với 0 lịch |
| [long-labels.png](docs/evidence/long-labels.png) | Nhãn dài, 14 tỷ lịch, số tiền lớn, ellipsis/tooltip |

Các PNG là `printAll` của Swing, không phải ảnh chụp toàn màn hình Windows hoặc ảnh HTML. Ảnh `desktop-*` có kích thước nội dung nhỏ hơn kích thước cửa sổ vì không chứa viền và thanh tiêu đề do Windows vẽ.

## 6. Giới hạn và bước tiếp theo

UI-M1 hoàn thành ở mức **giao diện xem thử với dữ liệu mô phỏng**; chưa hoàn thành nghiệp vụ thật hoặc tích hợp dữ liệu.
Không có trạng thái quyền bị từ chối từ server vì chưa có server/auth trong preview; cần đặc tả và nghiệm thu khi tích hợp.
Không có animation thu/mở nhóm, không triển khai chức năng của ảnh 02/03, không mở rộng sang ma trận Permission.
Cửa sổ nhỏ cần cuộn; nhãn quá dài được rút gọn và có tooltip. Chưa kiểm toàn bộ DPI/font fallback trên hệ điều hành khác, công nghệ hỗ trợ hoặc thao tác vật lý từng phím.

16 câu hỏi **CHƯA DUYỆT** ở [BA-OPEN-QUESTIONS.md](BA-OPEN-QUESTIONS.md), gồm kiến trúc production, ngày/múi giờ, định nghĩa badge/KPI, chuỗi đổi lịch, hoàn/hủy thanh toán, công nợ, ca/phân công, tồn/HSD, hiệu lực kiểm tra cấp cứu, tái khám, role/use case, state machine/audit và bảo vệ dữ liệu bệnh nhân.

UI-M2 đề xuất: duyệt kiến trúc và quyền → chốt chỉ số/nguồn/DTO/error → tích hợp đọc Dashboard và đối soát trên dữ liệu thật → thiết kế riêng tiếp nhận/lịch hẹn → chỉ triển khai mutation sau khi duyệt state machine và authorization backend.
