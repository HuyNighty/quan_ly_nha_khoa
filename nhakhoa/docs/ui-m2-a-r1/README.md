# N08 — Kiểm chứng Dashboard UI-M2-A-R1

Đọc [báo cáo bàn giao](HANDOVER-UI-M2-A-R1.md) để biết kết quả thực tế; tài liệu này là cách chạy lại, không phải tuyên bố mọi gate đã PASS.
Phần đọc Dashboard nội bộ, hợp đồng V1.1 và test nằm trong backend `nhakhoa/`. REST/LIVE chưa được phát hành và UI-M2-B chưa bắt đầu.

## Điều kiện và lệnh

JDK **25**, Docker Desktop Linux có thể chạy SQL Server 2022, Maven wrapper nằm trong **`nhakhoa/`**. Chạy các lệnh dưới đây tại thư mục đó, tuần tự; không chạy hai Maven build cùng lúc trên cùng `target/`.
Maven sử dụng dependency thật và Testcontainers được quản lý phiên bản bởi Spring Boot BOM. Không dùng stub Spring/JUnit/JPA.

```powershell
java -version
.\mvnw.cmd -version
.\mvnw.cmd -B compile
.\mvnw.cmd -B test-compile
.\mvnw.cmd -B '-Dtest=DashboardQueryServiceTest,DashboardDtoPrivacyTest,JpaDashboardReadRepositoryTest,DashboardContractSerializationTest,EmergencyCoverageRegressionTest' test
.\mvnw.cmd -B '-Dtest=DashboardSqlServerIntegrationTest' test
.\mvnw.cmd -B '-Dtest=DashboardApplicationSqlServerTest' test
```

`JpaDashboardReadRepositoryTest` là kiểm tra cấu trúc chuỗi bằng EntityManager proxy, **không** chứng minh JPQL hợp lệ. `DashboardSqlServerIntegrationTest` mới chạy các truy vấn qua Hibernate/driver/SQL Server thật, có Spring transaction proxy.

`DashboardApplicationSqlServerTest` kế thừa chính `contextLoads` của test baseline, nhưng cung cấp datasource container riêng bằng DynamicPropertySource, kiểm full Boot wiring/route và metadata FK. Không chỉnh test baseline hoặc YAML production.
Không dùng `mvn test` không chọn lớp trên môi trường có thông tin kết nối thật: `NhakhoaApplicationTests` gốc chưa được ràng buộc vào DB test riêng. Các lệnh phía trên chọn rõ bộ kiểm chứng an toàn.

## Gate mapping bắt buộc chạy riêng

```powershell
.\mvnw.cmd -B '-Dtest=DashboardEntityMappingGateTest' test
```

Gate này được viết như tiêu chí cần đạt, **hiện FAIL** khi persist `ChiTietKiemTraCapCuu` với mapping baseline. Không sửa assertion để coi lỗi là thành công.
Nó ghi fixture hoàn toàn nhân tạo qua EntityManager thật trong container riêng; không triển khai mutation ứng dụng.
Không được dùng PASS của phần đọc hoặc contextLoads thay thế gate persist.

## Database test

- Testcontainers tạo SQL Server từ `mcr.microsoft.com/mssql/server:2022-latest`, cổng ngẫu nhiên, mật khẩu dùng một lần sinh khi chạy.
- Database riêng `N08_Dashboard_R1_Test` hoặc `N08_Dashboard_App_R1_Test`. Bộ fixture kiểm tên DB trước khi xóa dữ liệu test.
- Không lấy URL/credential từ `.env` hoặc DBMS_* của người dùng để seed. Container được dừng khi context/test kết thúc; không bật reuse.
- Test dùng naming strategy mặc định thực tế của Spring Boot và tạo schema có FK, `halt_on_error=true`. Không tắt constraint trong cấu hình test cuối cùng.
- `create-only` chỉ áp dụng database mới dùng một lần. `application.yaml` với `ddl-auto:update` giữ nguyên và vẫn cần kế hoạch vận hành riêng.
- Riêng fixture chi tiết cấp cứu điền các cột khóa ghép hiện có bằng SQL để chứng minh query đọc; lỗi ORM persist được giữ ở gate riêng, không che giấu hoặc nhận là PASS.

## Nguồn và bảo toàn

Bản vá gốc `Downloads/UI-M2-A-dashboard-readonly.patch` được kiểm tra rồi áp dụng **một lần**, 43 file mới. Mã R1 sửa giới hạn trong feature dashboard, test và một dependency **test-scope** trong POM.
`inputs/` giữ tài liệu BA tham chiếu. Hợp đồng hiện hành ở [DASHBOARD-API-CONTRACT-V1.1.md](DASHBOARD-API-CONTRACT-V1.1.md).
`evidence/` giữ log các lần chạy, cả red/green và lỗi thiết lập test đã được phân biệt trong báo cáo.

`admin-desktop-preview/` và ZIP có sẵn được bảo toàn theo SHA-256; regression được build trong bản sao dưới `nhakhoa/target/ui-m1-regression/` vì JAR gốc đang bị tiến trình khác giữ. Không dừng tiến trình của người dùng.

## Công việc tiếp theo, chưa thực thi

- [Prompt đối chiếu AUTH-R1](AUTH-R1-RECONCILIATION-PROMPT.md).
- [Prompt UI-M2-B có gate](UI-M2-B-GATED-PROMPT.md).
- Mapping `MapsId` và dữ liệu/schema đích cần task riêng có quyền sửa Entity/schema; xác nhận nguyên nhân bằng log và test gate trước khi chọn cách sửa.
- `.env` đang tracked: quản lý repo kiểm tra lịch sử theo quy trình bảo mật, rotate nếu từng lộ credential, rồi bỏ tracking/ignore bằng task riêng. R1 không đọc hoặc in nội dung.
- Chốt yêu cầu consistency/snapshot của Dashboard và kế hoạch bỏ rủi ro `ddl-auto:update` trước production; R1 không tự đổi isolation hoặc schema thật.
