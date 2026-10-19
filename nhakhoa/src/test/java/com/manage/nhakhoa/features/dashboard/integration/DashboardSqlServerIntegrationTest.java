package com.manage.nhakhoa.features.dashboard.integration;

import com.manage.nhakhoa.features.dashboard.dto.response.*;
import com.manage.nhakhoa.features.dashboard.repository.DashboardReadRepository;
import com.manage.nhakhoa.features.dashboard.service.DashboardDataUnavailableException;
import com.manage.nhakhoa.features.dashboard.service.DashboardQueryService;
import com.manage.nhakhoa.features.hoadon.enums.PhuongThucThanhToan;
import com.manage.nhakhoa.features.lichhen.enums.TrangThaiLichHen;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.PhysicalNamingStrategySnakeCaseImpl;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** JPQL/Hibernate/Spring transaction thật trên SQL Server 2022 riêng, không thay thế bằng H2 hoặc proxy EntityManager. */
@SpringJUnitConfig(DashboardSqlServerTestConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class DashboardSqlServerIntegrationTest {
    static final LocalDate D = LocalDate.of(2026, 10, 19);
    @Autowired DashboardReadRepository repository;
    @Autowired DashboardQueryService service;
    @Autowired DashboardSqlServerTestConfig.FailureSwitch failures;
    @Autowired DataSource dataSource;
    @Autowired PlatformTransactionManager transactionManager;
    JdbcTemplate jdbc;

    @BeforeEach
    void reset() {
        failures.reset(); jdbc = new JdbcTemplate(dataSource);
        assertEquals(DashboardSqlServerTestConfig.TEST_DATABASE, jdbc.queryForObject("SELECT DB_NAME()", String.class));
        for (String table : List.of("ChiTietKiemTraCapCuu", "KiemTraCapCuu", "DanhMucCapCuu", "YeuCauHuyHoaDon",
                "ThanhToan", "ChiTietHoaDon", "HoaDon", "ChiTietDieuTri", "PhieuKham", "LichHen", "KeHoachDieuTri",
                "HoSoBenhAn", "BenhNhan", "PhanCong", "CaLamViec", "PhieuDieuChinhKho", "LoHang", "Thuoc",
                "VatTu", "NhaCungCap", "DichVu", "TaiKhoan", "NhanVien", "ThamSoHeThong")) {
            jdbc.update("DELETE FROM " + physical(table));
        }
    }

    @Test
    void allOriginalJpqlQueriesExecuteOnEmptySqlServerWithRealAggregates() {
        assertTrue(AopUtils.isAopProxy(repository)); assertTrue(AopUtils.isAopProxy(service));
        assertTrue(jdbc.queryForObject("SELECT COUNT(*) FROM sys.foreign_keys WHERE is_disabled=0 AND is_not_trusted=0",Integer.class)>0);
        LocalDateTime start = D.atStartOfDay(), end = D.plusDays(1).atStartOfDay();
        assertTrue(repository.countAppointmentsByStatus(start,end).isEmpty());
        assertTrue(repository.countExamsByStatus(start,end).isEmpty());
        assertTrue(repository.sumSuccessfulPaymentsByMethod(start,end).isEmpty());
        assertTrue(repository.sumBilledAmountPerActiveInvoice().isEmpty());
        assertTrue(repository.sumSuccessfulPaymentsPerActiveInvoice().isEmpty());
        assertEquals(0, repository.countSuccessfulPaymentsOnCancelledInvoices());
        assertEquals(0, repository.countPendingInvoiceCancellationRequests());
        assertEquals(0, repository.countPendingStockAdjustments());
        assertTrue(repository.findSystemParameter("MISSING").isEmpty());
        assertTrue(repository.findShifts().isEmpty());
        assertTrue(repository.countAssignedStaffByShiftAndRole(D).isEmpty());
        assertTrue(repository.findNonEmptyStockLots().isEmpty());
        assertTrue(repository.findStockItems().isEmpty());
        assertEquals(0, repository.countActiveRealEmergencyCatalogItems());
        assertTrue(repository.findLatestEmergencyCheckAtOrBefore(D.atTime(15,38)).isEmpty());
        assertEquals(0, repository.countFailingEmergencyCheckItems("MISSING", D));
        assertTrue(repository.findEmergencyCheckCoverage("MISSING").isEmpty());
        assertEquals(0, repository.countPlansWithPendingItemsDueBefore(D));
        assertEquals(0, repository.countPlansWithPendingItemsDueOnOrBefore(D));
        var result = service.getAdminDashboard();
        assertEquals(0,result.appointments().totalRecords()); assertEquals(7,result.appointments().byStatus().size());
        assertEquals("0",result.collections().amountVnd()); assertEquals("0",result.receivables().amountVnd());
        assertEquals(16,jdbc.queryForObject("SELECT CONVERT(int,SERVERPROPERTY('ProductMajorVersion'))",Integer.class));
        System.out.println("SQL_SERVER_TEST_VERSION="+jdbc.queryForObject("SELECT CONVERT(varchar(50),SERVERPROPERTY('ProductVersion'))",String.class));
    }

    @Test
    void jpqlDateEnumsAndSeparateInvoiceAggregatesMatchSqlServerFixture() {
        seedCore();
        appointment("LH00",D.atStartOfDay(),"CHO_XAC_NHAN");
        appointment("LH01",D.atTime(23,59,59),"DA_XAC_NHAN");
        appointment("LH02",D.plusDays(1).atStartOfDay(),"DA_DEN");
        appointment("LH03",D.minusDays(1).atTime(23,59,59),"DA_DEN");
        appointment("LH04",D.atTime(9,0),"DA_HUY");
        appointment("LH05",D.atTime(9,30),"DA_DOI_LICH");
        var result=service.getAdminDashboard();
        assertEquals(4,result.appointments().totalRecords()); assertEquals(2,result.appointments().activeCount());
        assertEquals(1L,result.appointments().byStatus().get(TrangThaiLichHen.DA_HUY));
        assertEquals("250000",result.collections().amountVnd());
        assertEquals("250000",result.receivables().amountVnd());
        assertEquals(1,result.receivables().invoiceCount());
        assertEquals(new BigDecimal("500000"), repository.sumBilledAmountPerActiveInvoice().getFirst().amount().stripTrailingZeros().setScale(0));
    }

    @ParameterizedTest
    @ValueSource(strings={"staffing","inventory","emergencyKit","treatmentDue"})
    void optionalSqlFailureMustNotRollbackCoreDashboard(String section) {
        seedCore(); failures.section=section;
        AdminDashboardResponse result=assertDoesNotThrow(service::getAdminDashboard);
        assertTrue(failures.insideTransaction); assertEquals(1,failures.sqlAttempts);
        assertEquals("250000",result.collections().amountVnd()); assertEquals("250000",result.receivables().amountVnd());
        switch(section) {
            case "staffing" -> { assertEquals(SectionStatus.UNAVAILABLE,result.staffing().status()); assertNull(result.staffing().understaffedShiftCount()); }
            case "inventory" -> { assertEquals(SectionStatus.UNAVAILABLE,result.inventory().status()); assertNull(result.inventory().expiredLotCount()); }
            case "emergencyKit" -> { assertEquals(SectionStatus.UNAVAILABLE,result.emergencyKit().status()); assertNull(result.emergencyKit().checkState()); }
            case "treatmentDue" -> { assertEquals(SectionStatus.UNAVAILABLE,result.treatmentDue().status()); assertNull(result.treatmentDue().duePlanCount()); }
            default -> fail(section);
        }
        assertTrue(result.warnings().stream().anyMatch(w -> w.section().equals(section) && w.code().equals("SECTION_KHONG_TAI_DUOC")));
    }

    @Test
    void coreSqlFailureFailsEntireDashboardWithoutFakeZeroPayload() {
        seedCore(); failures.section="appointments";
        var error=assertThrows(DashboardDataUnavailableException.class,service::getAdminDashboard);
        assertEquals("appointments",error.getSection()); assertTrue(failures.insideTransaction);
        assertFalse(error.getMessage().contains("n08_intentionally_missing_column"));
    }

    @ParameterizedTest
    @ValueSource(strings={"staffing","inventory","emergencyKit","treatmentDue"})
    void failedOptionalSectionAlsoDoesNotPoisonAnExistingCallerTransaction(String section) {
        seedCore(); failures.section=section;
        var transaction=new TransactionTemplate(transactionManager);
        assertDoesNotThrow(() -> transaction.execute(status -> {
            var response=service.getAdminDashboard();
            assertEquals("250000",response.receivables().amountVnd());
            assertFalse(status.isRollbackOnly());
            assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM hoa_don",Integer.class));
            return response;
        }));
        assertTrue(failures.insideTransaction); assertEquals(1,failures.sqlAttempts);
    }

    @ParameterizedTest
    @ValueSource(strings={"parameters","stockApprovals","invoiceApprovals"})
    void optionalParameterOrApprovalSqlFailuresAreIsolatedAsWell(String section) {
        seedCore(); failures.section=section;
        var response=assertDoesNotThrow(service::getAdminDashboard);
        assertTrue(failures.insideTransaction); assertTrue(failures.sqlAttempts>0);
        assertEquals("250000",response.collections().amountVnd());
        if(section.equals("parameters")) {
            assertEquals(30,response.inventory().thresholdDays()); assertEquals(14,response.treatmentDue().horizonDays());
            assertTrue(response.warnings().stream().anyMatch(w -> w.code().equals("THAM_SO_KHONG_DOC_DUOC")));
        } else assertTrue(response.warnings().stream().anyMatch(w -> w.section().equals("workQueue")));
    }

    @Test
    void allSevenAppointmentEnumsAndExamEnumsAreReadFromRealRows() {
        seedPeople();
        for(var status:TrangThaiLichHen.values()) appointment("LH"+status.ordinal(),D.atTime(9,status.ordinal()),status.name());
        appointment("OUT",D.plusDays(1).atStartOfDay(),"CHO_XAC_NHAN");
        int i=0;
        for(String status:List.of("CHO_KHAM","DANG_KHAM","DA_KET_LUAN","DA_HUY")) {
            insert("PhieuKham","maPhieuKham,maBenhNhan,maHoSo,maNhanVien,thoiGianTiepNhan,trangThai",
                    "PK"+(i++),"BN01","HS01","NV01",D.atStartOfDay(),status);
        }
        insert("PhieuKham","maPhieuKham,maBenhNhan,maHoSo,maNhanVien,thoiGianTiepNhan,trangThai",
                "PKOUT","BN01","HS01","NV01",D.plusDays(1).atStartOfDay(),"CHO_KHAM");
        var response=service.getAdminDashboard();
        assertEquals(7,response.appointments().totalRecords()); assertEquals(3,response.appointments().activeCount());
        response.appointments().byStatus().values().forEach(count -> assertEquals(1L,count));
        assertEquals(1,response.exams().waiting()); assertEquals(1,response.exams().inProgress()); assertEquals(1,response.exams().concluded());
    }

    @Test
    void collectionDateScopeDiffersFromGlobalCancelledInvoiceWarning() {
        seedCore(); invoice("CANCEL","DA_HUY"); line("CANCEL",1,"100");
        payment("OLD","HD01",D.minusDays(1).atTime(23,59,59),"2","TIEN_MAT","THANH_CONG");
        payment("NEXT","HD01",D.plusDays(1).atStartOfDay(),"3","THE_POS","THANH_CONG");
        payment("CANCELPAY","CANCEL",D.minusDays(30).atStartOfDay(),"100","TIEN_MAT","THANH_CONG");
        var response=service.getAdminDashboard();
        assertEquals("250000",response.collections().amountVnd());
        assertEquals("0",response.collections().byMethodVnd().get(PhuongThucThanhToan.THE_POS));
        assertEquals("249995",response.receivables().amountVnd());
        assertEquals(1,repository.countSuccessfulPaymentsOnCancelledInvoices());
        var warning=response.warnings().stream().filter(w -> w.code().equals("THU_TREN_HOA_DON_DA_HUY")).findFirst().orElseThrow();
        assertTrue(warning.message().contains("toàn hệ thống")); assertTrue(warning.message().contains("không giới hạn ngày"));
    }

    @Test
    void pendingCancellationStillHasDebtOverpaymentDoesNotOffsetOthersAndApprovalCountsAreReal() {
        seedCore(); account();
        invoice("HD02","CHUA_THANH_TOAN"); line("HD02",3,"100000");
        insert("YeuCauHuyHoaDon","maYeuCau,maHoaDon,maTaiKhoanTao,thoiGianTao,lyDo,trangThai",
                "YC01","HD02","TK01",D.atTime(8,0),"Dữ liệu kiểm thử","CHO_DUYET");
        invoice("HD03","DA_HUY"); line("HD03",1,"700000");
        invoice("HD04","DA_THANH_TOAN"); line("HD04",1,"100000");
        payment("OVERPAY","HD04",D.atTime(8,0),"120000","TIEN_MAT","THANH_CONG");
        insert("PhieuDieuChinhKho","maPhieu,loai,maTaiKhoanTao,thoiGianTao,lyDo,trangThai",
                "DC01","KIEM_KE","TK01",D.atStartOfDay(),"Dữ liệu kiểm thử","CHO_DUYET");
        var response=service.getAdminDashboard();
        assertEquals("550000",response.receivables().amountVnd()); assertEquals(2,response.receivables().invoiceCount());
        assertEquals(1,response.receivables().overpaidInvoiceCount());
        assertEquals(1,repository.countPendingStockAdjustments()); assertEquals(1,repository.countPendingInvoiceCancellationRequests());
        assertEquals("DUYET_HUY",response.workQueue().items().stream().filter(w -> w.code().equals("HOA_DON_CHO_DUYET_HUY")).findFirst().orElseThrow().targetScreen());
    }

    @Test
    void decimalMultiplicationAndSumKeepAmountsBeyondEighteenDigits() {
        seedPeople(); invoice("BIG","CHUA_THANH_TOAN"); line("BIG",2,"999999999999999999");
        assertEquals("1999999999999999998",service.getAdminDashboard().receivables().amountVnd());
    }

    @Test
    void staffingUsesDistinctEmployeesRolesStatusesDatesAndOvernightShift() {
        seedPeople();
        employee("NV02","BS","NGHI_VIEC"); employee("NV03","BS","DANG_LAM_VIEC");
        employee("NV04","YT","DANG_LAM_VIEC"); employee("NV05","LT","DANG_LAM_VIEC");
        insert("CaLamViec","maCa,tenCa,gioBatDau,gioKetThuc,soBacSi,soYTa,soLeTan",
                "CA01","Sáng",java.time.LocalTime.of(7,0),java.time.LocalTime.NOON,1,1,1);
        insert("CaLamViec","maCa,tenCa,gioBatDau,gioKetThuc,soBacSi,soYTa,soLeTan",
                "CA02","Qua đêm",java.time.LocalTime.of(22,0),java.time.LocalTime.of(6,0),1,0,0);
        assignment("PC1","NV01",D,"CHO_HIEU_LUC"); assignment("PC2","NV01",D,"HOAN_THANH");
        assignment("PC3","NV02",D,"DANG_THUC_HIEN"); assignment("PC4","NV03",D,"DA_HUY");
        assignment("PC5","NV03",D.minusDays(1),"DANG_THUC_HIEN");
        assignment("PC6","NV04",D,"DANG_THUC_HIEN"); assignment("PC7","NV05",D,"DANG_THUC_HIEN");
        var response=service.getAdminDashboard();
        assertEquals(1,response.staffing().understaffedShiftCount());
        var day=response.staffing().shifts().getFirst(); assertEquals(1,day.assigned().bacSi());
        assertEquals(1,day.assigned().yTa()); assertEquals(1,day.assigned().leTan()); assertEquals(ShiftState.DU_NHAN_SU,day.state());
        var night=response.staffing().shifts().get(1); assertTrue(night.overnight()); assertEquals(1,night.missing().bacSi());
    }

    @Test
    void inventoryJoinsRetainNullExpiryAndNegativeStockForCorrectClassification() {
        insert("NhaCungCap","maNhaCungCap,tenNhaCungCap,soDienThoai,trangThai","NCC","Kiểm thử","0000000000","DANG_HOP_TAC");
        for(String id:List.of("T1","T2")) insert("Thuoc","maThuoc,tenThuoc,donViTinh,kiemSoatDacBiet,dinhMucToiThieu,daDatHang",id,"Mặt hàng kiểm thử","Đơn vị",false,id.equals("T1")?5:1,false);
        insert("VatTu","maVatTu,tenVatTu,donViTinh,dinhMucToiThieu,daDatHang","V1","Vật tư kiểm thử","Đơn vị",3,false);
        lot("L1","T1",null,D.plusDays(31),3); lot("L2","T1",null,D,2); lot("L3","T1",null,D.plusDays(30),1);
        lot("L4",null,"V1",D.plusDays(60),10); lot("UNKNOWN","T1",null,null,8); lot("NEGATIVE","T1",null,D.plusDays(90),-2);
        lot("EMPTY","T1",null,D.minusDays(1),0); lot("BOTH","T1","V1",D.plusDays(90),2); lot("NEITHER",null,null,D.plusDays(90),1);
        var stock=service.getAdminDashboard().inventory();
        assertEquals(SectionStatus.AVAILABLE,stock.status()); assertEquals(2,stock.belowMinimumItemCount());
        assertEquals(1,stock.nearExpiryLotCount()); assertEquals(1,stock.expiredLotCount()); assertEquals(1,stock.unknownExpiryLotCount());
        assertEquals(3,stock.anomalyLotCount()); assertTrue(repository.findNonEmptyStockLots().stream().anyMatch(l -> l.quantity()<0));
    }

    @ParameterizedTest
    @ValueSource(strings={"ZERO_DETAILS","SAMPLE_ONLY","NEW_ITEM","MISSING_EXPIRY"})
    void realEmergencyCoverageCannotBeInferredFromIncompleteChecks(String scenario) {
        seedPeople(); catalog("REAL",true,false); catalog("SAMPLE",true,true); emergencyCheck("KT1",D.atTime(7,45),"DAT");
        switch(scenario) {
            case "SAMPLE_ONLY" -> emergencyDetail("KT1","SAMPLE",true,D.plusDays(90));
            case "NEW_ITEM" -> { emergencyDetail("KT1","REAL",true,D.plusDays(90)); catalog("NEW",true,false); }
            case "MISSING_EXPIRY" -> emergencyDetail("KT1","REAL",true,null);
            case "ZERO_DETAILS" -> { }
            default -> fail(scenario);
        }
        var response=service.getAdminDashboard();
        assertEquals(SectionStatus.AVAILABLE,response.emergencyKit().status());
        assertEquals(EmergencyCheckState.CHUA_DU_THONG_TIN,response.emergencyKit().checkState());
        assertFalse(repository.findEmergencyCheckCoverage("KT1").isEmpty());
        assertTrue(response.warnings().stream().anyMatch(w -> w.code().equals("CAP_CUU_CHUA_DU_THONG_TIN")));
    }

    @Test
    void mixedEmergencyCatalogIgnoresSampleFailuresButRequiresLatestOfficialCheck() {
        seedPeople(); catalog("REAL",true,false); catalog("SAMPLE",true,true); catalog("INACTIVE",false,false);
        emergencyCheck("KT1",D.atTime(7,45),"DAT"); emergencyDetail("KT1","REAL",true,D.plusDays(90));
        emergencyDetail("KT1","SAMPLE",false,null);
        assertEquals(1,repository.countActiveRealEmergencyCatalogItems());
        assertEquals(0,repository.countFailingEmergencyCheckItems("KT1",D.plusDays(30)));
        assertEquals(EmergencyCheckState.TRONG_CHU_KY,service.getAdminDashboard().emergencyKit().checkState());
        emergencyCheck("KT2",D.atTime(8,0),"DAT");
        assertEquals(EmergencyCheckState.CHUA_DU_THONG_TIN,service.getAdminDashboard().emergencyKit().checkState());
        emergencyCheck("FUTURE",D.atTime(20,0),"DAT");
        assertEquals("KT2",repository.findLatestEmergencyCheckAtOrBefore(D.atTime(15,38)).orElseThrow().checkId());
    }

    @Test
    void emergencyCycleBoundariesAndExpiryAreVerifiedOnSqlServer() {
        seedPeople(); catalog("REAL",true,false); emergencyCheck("KT1",D.minusDays(7).atTime(7,45),"DAT");
        emergencyDetail("KT1","REAL",true,D.plusDays(90));
        assertEquals(EmergencyCheckState.TRONG_CHU_KY,service.getAdminDashboard().emergencyKit().checkState());
        jdbc.update("UPDATE kiem_tra_cap_cuu SET thoi_gian=? WHERE ma_kiem_tra='KT1'",D.minusDays(8).atTime(7,45));
        assertEquals(EmergencyCheckState.QUA_CHU_KY,service.getAdminDashboard().emergencyKit().checkState());
        jdbc.update("UPDATE chi_tiet_kiem_tra_cap_cuu SET han_su_dung=?",D.plusDays(30));
        assertEquals(EmergencyCheckState.CAN_XU_LY,service.getAdminDashboard().emergencyKit().checkState());
    }

    @Test
    void inconsistentCompositeKeyColumnsCannotCountAsConfirmedCoverage() {
        seedPeople(); catalog("REAL",true,false); emergencyCheck("KT1",D.atTime(7,45),"DAT");
        emergencyDetail("KT1","REAL",true,D.plusDays(90));
        jdbc.update("UPDATE chi_tiet_kiem_tra_cap_cuu SET maMuc='OTHER'");
        assertEquals(EmergencyCheckState.CHUA_DU_THONG_TIN,service.getAdminDashboard().emergencyKit().checkState());
    }

    @Test
    void distinctTreatmentPlansRespectStatusAndHorizonBoundaries() {
        seedPeople(); insert("DichVu","maDichVu,tenDichVu,donGia,laCanLamSang,dangApDung","DV1","Dịch vụ kiểm thử",1,false,true);
        for(String id:List.of("KH1","KH2","KH3","KH4","KH5")) {
            insert("KeHoachDieuTri","maKeHoach,maHoSo,ngayLap,mucTieu,trangThai,maNhanVien",id,"HS01",D,"Kiểm thử tổng hợp",id.equals("KH4")?"HOAN_THANH":"DANG_THUC_HIEN","NV01");
        }
        planItem("KH1",D.minusDays(1),"CHUA_THUC_HIEN"); planItem("KH1",D.plusDays(5),"CHUA_THUC_HIEN");
        planItem("KH2",D.plusDays(14),"CHUA_THUC_HIEN"); planItem("KH3",D.plusDays(15),"CHUA_THUC_HIEN");
        planItem("KH4",D.minusDays(1),"CHUA_THUC_HIEN"); planItem("KH5",D.minusDays(1),"DA_THUC_HIEN");
        insert("ThamSoHeThong","ten,giaTri,moTa","SO_NGAY_KE_HOACH_DEN_HAN","14","Kiểm thử");
        assertEquals("14",repository.findSystemParameter("SO_NGAY_KE_HOACH_DEN_HAN").orElseThrow());
        var response=service.getAdminDashboard();
        assertEquals(2L,response.treatmentDue().duePlanCount()); assertEquals(1L,response.treatmentDue().overduePlanCount());
        assertTrue(response.workQueue().items().stream().anyMatch(w -> w.code().equals("KE_HOACH_DEN_HAN") && w.title().contains("bao gồm quá hạn")));
    }

    void account() {
        // Không phải credential đăng nhập thật: dữ liệu ràng buộc quan hệ trong container test.
        insert("TaiKhoan","maTaiKhoan,tenDangNhap,matKhau,trangThai,vaiTro,maNhanVien,soLanSai",
                "TK01","test-fixture","khong-dung-de-dang-nhap","HOAT_DONG","AD","NV01",0);
    }
    void employee(String id,String role,String status) {
        insert("NhanVien","maNhanVien,hoTen,soDienThoai,chucDanh,trangThai",id,"Nhân viên kiểm thử","00000000"+id.substring(2),role,status);
    }
    void assignment(String id,String employee,LocalDate date,String status) {
        insert("PhanCong","maPhanCong,maNhanVien,maCa,ngayLam,nhiemVu,trangThai",id,employee,"CA01",date,"Kiểm thử",status);
    }
    void lot(String id,String drug,String supply,LocalDate expiry,int quantity) {
        insert("LoHang","maLo,soLo,maThuoc,maVatTu,maNhaCungCap,hanSuDung,soLuongTon",id,id,drug,supply,"NCC",expiry,quantity);
    }
    void catalog(String id,boolean active,boolean sample) {
        insert("DanhMucCapCuu","maMuc,tenMuc,donViTinh,soLuongToiThieu,soLuongHienCo,dangApDung,laDuLieuMau",id,"Mục kiểm thử","Đơn vị",1,1,active,sample);
    }
    void emergencyCheck(String id,LocalDateTime time,String result) { insert("KiemTraCapCuu","maKiemTra,thoiGian,ketQua,maNhanVien",id,time,result,"NV01"); }
    void emergencyDetail(String check,String item,boolean passed,LocalDate expiry) {
        // Fixture cho phần ĐỌC: điền đầy đủ các cột hiện có do mapping baseline sinh ra, không ALTER/schema bypass.
        // Gate persist MapsId thật được giữ riêng trong DashboardEntityMappingGateTest, hiện FAIL và chặn nghiệm thu.
        jdbc.update("INSERT INTO chi_tiet_kiem_tra_cap_cuu (ma_kiem_tra,ma_muc,maKiemTra,maMuc,dat,han_su_dung,so_luong_thuc_te)"
                +" VALUES (?,?,?,?,?,?,?)",check,item,check,item,passed,expiry,1);
    }
    void planItem(String id,LocalDate date,String status) { insert("ChiTietDieuTri","maKeHoach,maDichVu,viTriRang,ngayDuKien,trangThai",id,"DV1","Kiểm thử",date,status); }

    void seedPeople() {
        insert("NhanVien","maNhanVien,hoTen,soDienThoai,chucDanh,trangThai", "NV01","Nhân viên kiểm thử","0000000001","BS","DANG_LAM_VIEC");
        insert("BenhNhan","maBenhNhan,hoTen,ngaySinh,gioiTinh,soDienThoai,ngayTao", "BN01","Hồ sơ tổng hợp kiểm thử",LocalDate.of(1900,1,1),"NAM","0000000002",D.atStartOfDay());
        insert("HoSoBenhAn","maHoSo,maBenhNhan,ngayLap","HS01","BN01",D);
    }
    void seedCore() {
        seedPeople();
        invoice("HD01","THANH_TOAN_MOT_PHAN");
        line("HD01",2,"150000"); line("HD01",1,"200000");
        payment("TT01","HD01",D.atStartOfDay(),"100000","TIEN_MAT","THANH_CONG");
        payment("TT02","HD01",D.atTime(23,59,59),"150000","CHUYEN_KHOAN","THANH_CONG");
        payment("TT03","HD01",D.atTime(8,0),"50000","THE_POS","DA_HUY");
    }
    void appointment(String id, LocalDateTime time, String status) {
        insert("LichHen","maLichHen,maBenhNhan,maNhanVien,thoiGianHen,trangThai,ngayTao",id,"BN01","NV01",time,status,D.atStartOfDay());
    }
    void invoice(String id,String status) { insert("HoaDon","maHoaDon,maNhanVien,maBenhNhan,ngayLap,trangThai",id,"NV01","BN01",D.atStartOfDay(),status); }
    void line(String id,int quantity,String price) { insert("ChiTietHoaDon","maHoaDon,soLuong,donGiaApDung",id,quantity,new BigDecimal(price)); }
    void payment(String id,String invoice,LocalDateTime time,String amount,String method,String status) {
        insert("ThanhToan","maThanhToan,maHoaDon,thoiGian,soTien,phuongThuc,trangThai,maNhanVien",id,invoice,time,new BigDecimal(amount),method,status,"NV01");
    }
    void insert(String table,String columns,Object... values) {
        String placeholders=String.join(",",Arrays.stream(values).map(v -> "?").toList());
        String physicalColumns=String.join(",",Arrays.stream(columns.split(",")).map(DashboardSqlServerIntegrationTest::physical).toList());
        jdbc.update("INSERT INTO " + physical(table) + " (" + physicalColumns + ") VALUES (" + placeholders + ")",values);
    }
    private static String physical(String logical) {
        return new PhysicalNamingStrategySnakeCaseImpl().toPhysicalColumnName(Identifier.toIdentifier(logical),null).getText();
    }
}
