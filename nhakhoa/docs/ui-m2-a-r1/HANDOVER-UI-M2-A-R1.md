# N08 — Bàn giao UI-M2-A-R1: Verification & Refinement

**Ngày:** 19/10/2026, Asia/Bangkok. **Kết luận:** phần refinement và kiểm chứng đọc nội bộ đã thực hiện; **chưa nghiệm thu tích hợp, chưa phát hành REST/LIVE, chưa triển khai UI-M2-B**.

**77 kiểm tra trong bộ xác minh đọc/contract/context cuối cùng PASS; một gate persist Entity riêng FAIL.** Hai kết quả này không thay thế nhau. Không tuyên bố toàn bộ integration đã PASS.

## 1. Checkout, đầu vào và bảo toàn

| Mục | Quan sát |
|---|---|
| Nhánh ban đầu / HEAD | `main` / `4d25dbf88be6415a8fb77facf64b5d4ea9e1f704` |
| Working tree ban đầu | `?? admin-desktop-preview/`, `?? admin-desktop-preview.zip`; chưa có feature dashboard backend |
| Nhánh làm việc | `codex/ui-m2-a-r1`, tạo từ checkout hiện tại; HEAD giữ nguyên |
| Patch đầu vào | `UI-M2-A-dashboard-readonly.patch`, 143.392 byte, SHA-256 `B732E304A61CD8DB7F31659096D2C5C31A3CD7B93D50116FCB8F3E3FA8B98A06` |
| Áp dụng | `git apply --check` đạt; áp dụng **một lần**, 43 file mới. Không áp dụng ZIP review như một bản vá thứ hai |
| UI-M1 | Có source Swing và 21 khóa menu thực tế; khác với báo cáo UI-M2-A cũ nói chưa có UI-M1 |
| AUTH-R1 | Không có SecurityFilterChain, implementation JWT/login, Spring Security dependency hoặc test bảo vệ endpoint trong checkout. Nhánh/worktree local không có implementation AUTH-R1 để tích hợp |
| Java / Maven thực thi | Temurin JDK/javac `25.0.4.1`; wrapper **`nhakhoa/mvnw.cmd`**, Maven `3.10.0` |
| Dependency thực | Spring Boot `4.1.1`, Hibernate `7.4.5.Final`, JUnit Jupiter `6.0.3`, Jackson `3.1.5`, JDBC SQL Server `13.4`, Testcontainers `2.0.5` |
| SQL Server thực | SQL Server 2022, `16.0.4295.3`, Linux container dùng một lần, không phải H2/Derby/stub |

Đã đọc review/prompt R1, bàn giao cũ và đầy đủ bộ BA V1/V1.1 trong ZIP handoff được tìm thấy ở Downloads. Các bản tham chiếu nằm ở `inputs/`; chỉ áp dụng chỉ dẫn trong phạm vi yêu cầu R1 mới nhất. Báo cáo 31 test dùng stub trước đây không được coi là bằng chứng tích hợp.

File đã tồn tại trong Git chỉ thay **`nhakhoa/pom.xml`**, thêm 7 dòng cho dependency `testcontainers-mssqlserver` **scope test**. Dependency production giữ nguyên. Feature dashboard và test mới vẫn untracked để người dùng review; không stage/commit/push/merge.

`git diff --exit-code HEAD -- . ':(exclude)nhakhoa/pom.xml'` đạt: không sửa Entity/enum baseline, YAML, `.env`, JWT/security, source backend cũ hoặc test `NhakhoaApplicationTests` gốc.
Đối chiếu SHA-256 **34 file có sẵn** của preview/ZIP đạt, không mất hoặc đổi file: [preservation-result.json](evidence/preservation-result.json). Chỉ các đầu ra build không theo dõi được sinh trong quá trình test. Không dừng tiến trình preview của người dùng.

## 2. Các thay đổi R1

### P0 — Transaction fallback: đã xác nhận lỗi và sửa

Test gửi SQL cố ý sai cột qua **EntityManager, SQL Server và Spring transaction proxy thật**, không dùng fake repository để chứng minh failure isolation.
Với bản vá gốc: **4/7 test FAIL**, cả bốn section `staffing/inventory/emergencyKit/treatmentDue` làm toàn response ném:

```text
UnexpectedRollbackException: Transaction silently rolled back because it has been marked as rollback-only
```

Bằng chứng: [sql-baseline-red.log](evidence/sql-baseline-red.log).

Sửa `DashboardQueryService` sang `@Transactional(propagation = NOT_SUPPORTED)`. Các lời gọi repository vẫn dùng transaction **read-only**; lỗi query được rollback trước khi service bắt để trả fallback. Transaction có sẵn của caller được tạm ngưng thay vì bị nhiễm rollback-only.
Test đã bao phủ cả bốn section, caller đang có transaction, lỗi đọc tham số, lỗi hai nhóm chờ duyệt và lỗi lõi. Section phụ lỗi trả UNAVAILABLE/count null; core vẫn đúng; core lỗi vẫn ném `DashboardDataUnavailableException`, không tạo số 0 giả.

Đổi lại, Dashboard không hứa một snapshot nguyên tử xuyên mọi query. READ_COMMITTED có thể quan sát các thời điểm commit khác nhau; không tự thay isolation/database để tạo cảm giác khóa sổ.

### P0 — Bao phủ hộp cấp cứu

Bổ sung LEFT JOIN **từng mục chính thức đang áp dụng** với chi tiết trong lần kiểm tra mới nhất `<= now`; không chỉ kiểm có danh mục rồi suy từ kết quả tổng.
`EmergencyCoverageCalculator` yêu cầu đủ số mục, khóa tương ứng, không trùng mục, có kết quả/quantity/minimum/HSD để xác nhận. Thiếu hoặc mâu thuẫn trả `CHUA_DU_THONG_TIN`, thông báo cần kiểm tra, warning cố định và công việc kiểm tra mức khẩn.

Đã kiểm mixed sample/real, mục mới thêm, zero details, thiếu HSD, thiếu quantity ở mức unit, lần mới thiếu dữ liệu không fallback về lần cũ, biên 7/8 ngày, biên hạn dùng, mẫu không lấp phần thiếu mục thật và khóa không nhất quán.
HSD null không được suy là “không cần hạn dùng” vì Entity chưa có cờ phân loại đó. `TRONG_CHU_KY` không phải chứng nhận an toàn lâm sàng; lịch sử phiên bản danh mục chưa được model hỗ trợ.

### P1 — Hợp đồng và phạm vi

Đồng bộ [hợp đồng V1.1](DASHBOARD-API-CONTRACT-V1.1.md), có serialization qua **Jackson do Spring Boot cấu hình thật** cho:

- `appointments.activeCount`;
- `receivables.overpaidInvoiceCount`;
- `staffing.understaffedShiftCount`;
- `inventory.anomalyLotCount`;
- `emergencyKit.checkState`, `checkCycleDays`;
- `warnings[] {code,section,message}`.

Giữ các field V1 và phân biệt JSON null với 0; tiền vẫn là chuỗi số nguyên. Khóa `SO_NGAY_KE_HOACH_DEN_HAN` đã được chấp nhận, fallback 14 và warning khi chưa cấu hình; không INSERT tham số.
Nhãn đến hạn ghi **“bao gồm quá hạn”**; `relatedObjectCount` là lượt liên quan, có thể trùng. Cảnh báo thu trên hóa đơn hủy ghi rõ **đối soát toàn hệ thống, không giới hạn ngày**, khác KPI thu trong ngày theo trạng thái giao dịch hiện tại.

Đối chiếu source MenuDefinition thực: sửa giá trị đích giả định thành `PHAN_CONG`, `THUOC_VAT_TU`, `KIEM_KE`, `KE_HOACH`; yêu cầu hủy đi `DUYET_HUY`. Không bật điều hướng LIVE, không sửa Swing.
Không thêm “Đã có lịch” hoặc nhóm bàn giao ca, không thêm Controller, không thực hiện duyệt/hủy/mở khóa/lâm sàng/CRUD.

## 3. Gate mapping mới: FAIL, chưa được phép sửa trong R1

Spring Boot context sinh schema thành công, metadata xác nhận **74 FK đang bật và trusted**. Nhưng contextLoads không chứng minh Entity có thể persist.
SQL Server test ghi nhận `ChiTietKiemTraCapCuu` có cả bốn cột:

```text
ma_kiem_tra varchar(10) NOT NULL
ma_muc     varchar(10) NOT NULL
maKiemTra  varchar(255) NOT NULL
maMuc      varchar(255) NOT NULL
```

Khi `EntityManager.persist` Entity hiện hành, SQL insert chỉ điền bộ camelCase, dẫn đến:

```text
Cannot insert the value NULL into column 'ma_kiem_tra' ... INSERT fails.
insert into chi_tiet_kiem_tra_cap_cuu
  (dat,han_su_dung,so_luong_thuc_te,maKiemTra,maMuc) values (?,?,?,?,?)
```

Đã lưu test độc lập **`DashboardEntityMappingGateTest`**, assertion yêu cầu persist thành công và hiện **1/1 FAIL**. Không đổi assertion thành PASS khi có exception. [Log gate](evidence/entity-mapping-gate.log).

Bộ test đọc cuối cùng dùng naming strategy thực tế của Boot, FK đầy đủ và fixture cấp cứu **điền cả các cột hiện có bằng SQL**; không ALTER bảng, không tắt constraint. Điều này chứng minh JPQL trên hàng dữ liệu đủ, **không** chứng minh ORM persist hoặc database triển khai thật đã đúng.
Entity và production schema bị cấm sửa trong R1, nên gate này được giữ mở cho task riêng; cần kiểm tra mapping/DDL/dữ liệu đích và phương án sửa được phê duyệt. Không kết luận mọi Entity khóa ghép khác đều lỗi chỉ từ một trường hợp.

Hai bước thiết lập test ban đầu cũng được lưu: lỗi drop bảng chưa tồn tại khi dùng create-drop; lỗi naming khi tự cấu hình EMF khác Boot. Không nhầm chúng với lỗi production đã xác nhận. Cấu hình test cuối cùng dùng create-only trên DB mới, naming Boot và FK, không còn NO_CONSTRAINT. Lỗi persist nêu trên được tái hiện **sau** khi đã dùng Boot thực tế.

## 4. Bằng chứng kiểm thử

Tất cả lệnh Maven dưới đây chạy từ `nhakhoa/`, bằng JDK 25 và dependency thật. Không sửa global settings, không xóa cache, không thay cờ bảo mật.

| Kiểm tra | Trạng thái | Kết quả / bằng chứng |
|---|---|---|
| `mvnw.cmd -B compile` | **PASS** | [compile-final.log](evidence/compile-final.log) |
| `mvnw.cmd -B test-compile` | **PASS** | [test-compile-final.log](evidence/test-compile-final.log) |
| Ba lớp JUnit gốc trước refinement | **PASS** | 31/31 với JUnit thật; [unit-baseline.log](evidence/unit-baseline.log). Repo proxy vẫn chỉ là test cấu trúc |
| `DashboardQueryServiceTest` | **PASS** | 24/24 trong log cuối |
| `DashboardDtoPrivacyTest` | **PASS** | 1/1: cây DTO không Entity/PII/PHI |
| `JpaDashboardReadRepositoryTest` | **PASS** | 7/7: cấu trúc JPQL, không nhận là integration |
| `DashboardContractSerializationTest` | **PASS** | 5/5: field V1/V1.1, nonzero, null, enum mới, JSON, keys UI |
| `EmergencyCoverageRegressionTest` | **PASS** | 9/9 unit regression |
| `DashboardSqlServerIntegrationTest` | **PASS có giới hạn fixture nêu trên** | 28/28: 20 query statements qua Hibernate/SQL Server, aggregate, enum, biên ngày, công nợ không nhân dòng, null/empty, tiền lớn, DISTINCT, kho, coverage và lỗi transaction |
| `DashboardApplicationSqlServerTest` | **PASS** | 3/3: contextLoads kế thừa test gốc, full Boot wiring/route vắng mặt, metadata FK |
| Tổng bộ xác minh đọc/contract/context | **PASS** | **77/77**, 0 fail/error/skip; [verification-final.log](evidence/verification-final.log) |
| `DashboardEntityMappingGateTest` | **FAIL** | 1/1 không persist được MapsId thật; tách riêng khỏi bộ 77 và vẫn chặn nghiệm thu |
| REST 401/403/200, Swing LIVE | **NOT VERIFIED** | Không có AUTH-R1/endpoint để nghiệm thu; đã kiểm route Dashboard không tồn tại |
| Schema/dữ liệu/consistency của DB production | **NOT VERIFIED** | Không kết nối production; READ_COMMITTED không phải snapshot xuyên section |
| Build trực tiếp preview có sẵn | **ENV_BLOCKED** | JAR đang bị tiến trình khác giữ trên Windows; [preview-regression.log](evidence/preview-regression.log) |
| Build/test source preview trên bản sao tạm | **PASS** | Java 25, `-Xlint:all -Werror`, **101 kiểm tra**; [preview-isolated-regression.log](evidence/preview-isolated-regression.log) |
| Bảo toàn 34 file preview/ZIP, tracked files ngoài POM | **PASS** | SHA-256 không đổi; git diff kiểm tra phạm vi đạt |

77 là 46 unit/contract + 28 SQL + 3 Boot; không cộng các lần chạy lặp để tăng số test. Gate Entity riêng là một ca thất bại thực, không nằm trong số 77 PASS.

Lệnh bộ cuối:

```powershell
.\mvnw.cmd -B '-Dtest=DashboardQueryServiceTest,DashboardDtoPrivacyTest,JpaDashboardReadRepositoryTest,DashboardContractSerializationTest,EmergencyCoverageRegressionTest,DashboardSqlServerIntegrationTest,DashboardApplicationSqlServerTest' test
```

Output:

```text
BOOT_SCHEMA_ENABLED_FOREIGN_KEYS=74
SQL_SERVER_TEST_VERSION=16.0.4295.3
Tests run: 77, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Lệnh gate chưa đạt:

```powershell
.\mvnw.cmd -B '-Dtest=DashboardEntityMappingGateTest' test
# Tests run: 1, Failures: 1, Errors: 0, Skipped: 0 — BUILD FAILURE
```

JDBC, Hibernate và Testcontainers đều thật; không H2/stub API trong đường kiểm chứng tích hợp. Data fixture là nhân tạo, không có hồ sơ bệnh nhân thật.
Image: `mcr.microsoft.com/mssql/server:2022-latest`, digest `sha256:4402d880dd4c34bfa7d8705e56a86cd6c88da80a1f6bbbe741f999e76264a090`. Database chỉ là container test có tên được kiểm tra; container N08 của tác vụ đã dừng sau test.
Các warning SQL 207 là lỗi chủ động trong test fallback; Lombok Unsafe, Mockito agent và driver prelogin lúc container khởi động được giữ trong log, không bị che bằng cờ JVM/security.

## 5. File và diff

43 file từ patch gốc nằm trong package `com.manage.nhakhoa.features.dashboard` và test tương ứng; danh sách gốc ở [bàn giao đầu vào](inputs/BAN-GIAO-UI-M2-A.md). R1 thêm 8 file Java và sửa 11 file của patch cùng POM: **20 file source/config khác baseline UI-M2-A**.

Quy ước: `D = nhakhoa/src/main/java/com/manage/nhakhoa/features/dashboard/`, `T = nhakhoa/src/test/java/com/manage/nhakhoa/features/dashboard/`.

| File R1 | Lý do |
|---|---|
| `nhakhoa/pom.xml` | Một dependency SQL Server Testcontainers test-scope |
| `D/dto/response/EmergencyCheckState.java` | Trạng thái chưa đủ thông tin |
| `D/dto/response/TreatmentDueSummary.java` | Ghi rõ tập đến hạn gồm quá hạn |
| `D/dto/response/WorkQueueDto.java` | Ghi rõ lượt liên quan không unique |
| `D/repository/DashboardReadRepository.java` | Hợp đồng đọc coverage |
| `D/repository/EmergencyCheckItemRow.java` **mới** | Projection mục chính thức và chi tiết có thể thiếu |
| `D/repository/JpaDashboardReadRepository.java` | LEFT JOIN coverage; loại chi tiết mẫu khỏi count kiểm tra |
| `D/service/DashboardQueryService.java` | Isolation lỗi, coverage, nhãn/scope cảnh báo, đích điều hướng |
| `D/service/DashboardTargetScreens.java` | Đối chiếu key từ UI thật |
| `D/service/DashboardThresholds.java` | Khóa horizon đã được BA chấp nhận |
| `D/service/EmergencyCoverageCalculator.java` **mới** | Đánh giá đầy đủ/thiếu thông tin, không suy an toàn |
| `T/repository/JpaDashboardReadRepositoryTest.java` | Test query coverage và SELECT-only |
| `T/service/DashboardQueryServiceTest.java` | Fixture kiểm tra có chi tiết đầy đủ, nhãn overlap |
| `T/service/InMemoryDashboardReadRepository.java` | Hỗ trợ catalog ID và chi tiết thiếu trong unit test |
| `T/service/DashboardContractSerializationTest.java` **mới** | Jackson Boot thật, field/keys V1.1 |
| `T/service/EmergencyCoverageRegressionTest.java` **mới** | Regression P0 coverage |
| `T/integration/DashboardSqlServerTestConfig.java` **mới** | Container/schema Spring transaction, lỗi SQL chủ động |
| `T/integration/DashboardSqlServerIntegrationTest.java` **mới** | 28 ca đọc/transaction trên SQL Server |
| `nhakhoa/src/test/java/com/manage/nhakhoa/DashboardApplicationSqlServerTest.java` **mới** | Test Boot/contextLoads/route/schema |
| `nhakhoa/src/test/java/com/manage/nhakhoa/DashboardEntityMappingGateTest.java` **mới** | Giữ gate persist thực đang FAIL |

Tài liệu mới: README, báo cáo này, hợp đồng V1.1, hai prompt kế tiếp, `inputs/`, log và manifest trong `evidence/`.
Danh sách máy đọc: [r1-source-changes.json](evidence/r1-source-changes.json). Diff R1 riêng: [R1-refinement.patch](R1-refinement.patch), **20 file, 1.003 dòng thêm / 23 dòng xóa**, không gồm tài liệu/log.
Đã kiểm `git apply --reverse --check` trên checkout cuối mà không áp dụng reverse; diff phù hợp với mã hiện tại.

**Checkout hiện tại đã có cả patch gốc và R1. Không áp dụng lại.** Khi dựng checkout khác, phải kiểm baseline và working tree rồi mới dùng đúng một patch gốc + một refinement. Không có commit/PR được tạo trong tác vụ này.

## 6. Gate trước khi làm UI-M2-B

| Gate còn thiếu | Trạng thái / việc cần làm |
|---|---|
| Mapping dữ liệu cấp cứu | **FAIL**; tái hiện bằng test gate, task riêng có quyền sửa Entity/schema và đối soát dữ liệu đích, không chỉ nhìn contextLoads |
| AUTH-R1 trên checkout triển khai | **NOT VERIFIED / blocked**; tìm đúng ref/code đã kiểm thử, đối chiếu identity/AD/tài khoản hoạt động, không viết auth trùng |
| REST AD/no-store/error DTO | **NOT VERIFIED / blocked**; chỉ tạo endpoint sau AUTH và xử lý các gate dữ liệu liên quan |
| `.env` tracked và vận hành schema | Chưa remediation; không đọc/in secret. Kiểm tra lịch sử/rotate nếu từng lộ bằng task riêng; chốt thay thế rủi ro ddl-auto:update |
| Yêu cầu consistency của số tài chính | Chưa nghiệm thu snapshot/đối soát production; cần quyết định BA/kiến trúc nếu phải nhất quán xuyên query |
| Swing HTTP/LIVE/end-to-end | Chưa thực hiện; chỉ bắt đầu sau khi các gate liên quan đạt, không đổi mock thành LIVE ngầm |

Đã cung cấp [prompt đối chiếu AUTH-R1](AUTH-R1-RECONCILIATION-PROMPT.md) và [prompt UI-M2-B có gate](UI-M2-B-GATED-PROMPT.md), **không thực thi hai task đó**.
Giới hạn schema/Entity, `.env`, YAML, JWT và mutation được giữ nguyên; ưu tiên bảo toàn repository và trạng thái nghiệp vụ trung thực.
