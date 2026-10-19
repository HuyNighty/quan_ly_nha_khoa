package com.manage.nhakhoa.features.dashboard.service;

import com.manage.nhakhoa.features.capcuu.enums.KetQuaKiemTra;
import com.manage.nhakhoa.features.dashboard.dto.response.AdminDashboardResponse;
import com.manage.nhakhoa.features.dashboard.dto.response.DashboardWarning;
import com.manage.nhakhoa.features.dashboard.dto.response.EmergencyCheckState;
import com.manage.nhakhoa.features.dashboard.dto.response.RoleCount;
import com.manage.nhakhoa.features.dashboard.dto.response.SectionStatus;
import com.manage.nhakhoa.features.dashboard.dto.response.ShiftStaffingDto;
import com.manage.nhakhoa.features.dashboard.dto.response.ShiftState;
import com.manage.nhakhoa.features.dashboard.dto.response.WorkPriority;
import com.manage.nhakhoa.features.dashboard.dto.response.WorkQueueItem;
import com.manage.nhakhoa.features.dashboard.repository.ShiftRow;
import com.manage.nhakhoa.features.dashboard.repository.StockItemRow;
import com.manage.nhakhoa.features.dashboard.repository.StockItemType;
import com.manage.nhakhoa.features.dashboard.service.InMemoryDashboardReadRepository.Appointment;
import com.manage.nhakhoa.features.dashboard.service.InMemoryDashboardReadRepository.Assignment;
import com.manage.nhakhoa.features.dashboard.service.InMemoryDashboardReadRepository.Catalog;
import com.manage.nhakhoa.features.dashboard.service.InMemoryDashboardReadRepository.Check;
import com.manage.nhakhoa.features.dashboard.service.InMemoryDashboardReadRepository.CheckItem;
import com.manage.nhakhoa.features.dashboard.service.InMemoryDashboardReadRepository.Exam;
import com.manage.nhakhoa.features.dashboard.service.InMemoryDashboardReadRepository.Invoice;
import com.manage.nhakhoa.features.dashboard.service.InMemoryDashboardReadRepository.InvoiceLine;
import com.manage.nhakhoa.features.dashboard.service.InMemoryDashboardReadRepository.Lot;
import com.manage.nhakhoa.features.dashboard.service.InMemoryDashboardReadRepository.Payment;
import com.manage.nhakhoa.features.dashboard.service.InMemoryDashboardReadRepository.PlanItem;
import com.manage.nhakhoa.features.hoadon.enums.PhuongThucThanhToan;
import com.manage.nhakhoa.features.hoadon.enums.TrangThaiHoaDon;
import com.manage.nhakhoa.features.hoadon.enums.TrangThaiThanhToan;
import com.manage.nhakhoa.features.khamchuabenh.enums.TrangThaiChiTietDieuTri;
import com.manage.nhakhoa.features.khamchuabenh.enums.TrangThaiKeHoach;
import com.manage.nhakhoa.features.khamchuabenh.enums.TrangThaiPhieuKham;
import com.manage.nhakhoa.features.lichhen.enums.TrangThaiLichHen;
import com.manage.nhakhoa.features.nhansu.enums.TrangThaiNhanVien;
import com.manage.nhakhoa.features.nhansu.enums.TrangThaiPhanCong;
import com.manage.nhakhoa.shared.enums.TrangThaiDuyet;
import com.manage.nhakhoa.shared.enums.VaiTro;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Kiểm thử công thức nghiệp vụ Dashboard theo BA V1/V1.1 với Clock cố định (không dùng giờ máy). */
class DashboardQueryServiceTest {

    /** 2026-10-19 15:38 giờ Việt Nam. */
    private static final Instant NOW = Instant.parse("2026-10-19T08:38:00Z");
    private static final LocalDate D = LocalDate.of(2026, 10, 19);

    private InMemoryDashboardReadRepository repo;

    @BeforeEach
    void setUp() {
        repo = new InMemoryDashboardReadRepository();
    }

    private AdminDashboardResponse run() {
        return run(NOW);
    }

    private AdminDashboardResponse run(Instant instant) {
        return new DashboardQueryService(repo, Clock.fixed(instant, ZoneOffset.UTC)).getAdminDashboard();
    }

    private static LocalDateTime at(LocalDate date, int h, int m, int s) {
        return date.atTime(h, m, s);
    }

    private static boolean hasWarning(AdminDashboardResponse r, String code, String section) {
        return r.warnings().stream().anyMatch(w -> w.code().equals(code) && w.section().equals(section));
    }

    private static WorkQueueItem item(AdminDashboardResponse r, String code) {
        return r.workQueue().items().stream().filter(i -> i.code().equals(code)).findFirst().orElse(null);
    }

    // ------------------------------------------------------------------ ngày nghiệp vụ / Clock

    @Test
    void emptyDatabaseReturnsValidZerosWithAllKeysAndLiveMetadata() {
        AdminDashboardResponse r = run();

        assertEquals("LIVE", r.source());
        assertEquals(D, r.businessDate());
        assertEquals("Asia/Ho_Chi_Minh", r.zoneId());
        assertEquals("2026-10-19T15:38+07:00", r.generatedAt().toString());
        assertEquals("2026-10-19T00:00+07:00", r.period().startInclusive().toString());
        assertEquals("2026-10-20T00:00+07:00", r.period().endExclusive().toString());

        assertEquals(7, r.appointments().byStatus().size());
        assertEquals(0, r.appointments().totalRecords());
        assertEquals(0, r.exams().waiting());
        assertEquals("PHIEU_KHAM_TIEP_NHAN_TRONG_NGAY", r.exams().unit());
        assertEquals("0", r.collections().amountVnd());
        assertEquals(3, r.collections().byMethodVnd().size());
        r.collections().byMethodVnd().values().forEach(v -> assertEquals("0", v));
        assertEquals("0", r.receivables().amountVnd());
        assertEquals(0, r.receivables().invoiceCount());

        assertEquals(SectionStatus.AVAILABLE, r.inventory().status());
        assertEquals(0, r.inventory().belowMinimumItemCount());
        assertEquals(30, r.inventory().thresholdDays());
        assertEquals(SectionStatus.AVAILABLE, r.treatmentDue().status());
        assertEquals(0L, r.treatmentDue().duePlanCount());
        assertEquals(14, r.treatmentDue().horizonDays());
        // Không có ca và không có danh mục cấp cứu thật: không giả "đủ"/"đạt".
        assertEquals(SectionStatus.UNCONFIGURED, r.staffing().status());
        assertEquals(SectionStatus.UNCONFIGURED, r.emergencyKit().status());
        assertNull(r.emergencyKit().checkState());
        // Thiếu tham số → fallback + cảnh báo cấu hình.
        assertTrue(hasWarning(r, "THAM_SO_CHUA_CAU_HINH", "inventory"));
        assertTrue(hasWarning(r, "THAM_SO_CHUA_CAU_HINH", "treatmentDue"));
    }

    @Test
    void businessDateFollowsHoChiMinhZoneNotUtcOrMachineTime() {
        // 23:59:59 giờ VN ngày 19 = 16:59:59Z
        assertEquals(D, run(Instant.parse("2026-10-19T16:59:59Z")).businessDate());
        // 00:00:00 giờ VN ngày 20 = 17:00:00Z (UTC vẫn là ngày 19)
        AdminDashboardResponse next = run(Instant.parse("2026-10-19T17:00:00Z"));
        assertEquals(D.plusDays(1), next.businessDate());
        assertEquals(D.plusDays(1).atStartOfDay(), repo.lastAppointmentStart);
        assertEquals(D.plusDays(2).atStartOfDay(), repo.lastAppointmentEnd);
    }

    // ------------------------------------------------------------------ lịch hẹn (Q05)

    @Test
    void appointmentsCountAllSevenStatusesInHalfOpenDay() {
        repo.appointments.add(new Appointment(at(D, 0, 0, 0), TrangThaiLichHen.CHO_XAC_NHAN));          // biên dưới: tính
        repo.appointments.add(new Appointment(at(D, 23, 59, 59), TrangThaiLichHen.DA_XAC_NHAN));        // cuối ngày: tính
        repo.appointments.add(new Appointment(D.plusDays(1).atStartOfDay(), TrangThaiLichHen.DA_XAC_NHAN)); // biên trên: loại
        repo.appointments.add(new Appointment(at(D.minusDays(1), 23, 59, 59), TrangThaiLichHen.DA_DEN)); // hôm qua: loại
        repo.appointments.add(new Appointment(at(D, 9, 0, 0), TrangThaiLichHen.DA_DEN));
        repo.appointments.add(new Appointment(at(D, 9, 30, 0), TrangThaiLichHen.HOAN_THANH));
        repo.appointments.add(new Appointment(at(D, 10, 0, 0), TrangThaiLichHen.VANG_MAT));
        repo.appointments.add(new Appointment(at(D, 11, 0, 0), TrangThaiLichHen.DA_HUY));
        // Đổi lịch trong cùng ngày: bản cũ DA_DOI_LICH + bản mới CHO_XAC_NHAN = 2 bản ghi.
        repo.appointments.add(new Appointment(at(D, 14, 0, 0), TrangThaiLichHen.DA_DOI_LICH));
        repo.appointments.add(new Appointment(at(D, 16, 0, 0), TrangThaiLichHen.CHO_XAC_NHAN));
        // Đổi lịch sang ngày khác: chỉ bản cũ thuộc D.
        repo.appointments.add(new Appointment(at(D, 15, 0, 0), TrangThaiLichHen.DA_DOI_LICH));
        repo.appointments.add(new Appointment(at(D.plusDays(3), 15, 0, 0), TrangThaiLichHen.CHO_XAC_NHAN));

        AdminDashboardResponse r = run();
        Map<TrangThaiLichHen, Long> s = r.appointments().byStatus();

        assertEquals(2L, s.get(TrangThaiLichHen.CHO_XAC_NHAN));
        assertEquals(1L, s.get(TrangThaiLichHen.DA_XAC_NHAN));
        assertEquals(1L, s.get(TrangThaiLichHen.DA_DEN));
        assertEquals(1L, s.get(TrangThaiLichHen.HOAN_THANH));
        assertEquals(1L, s.get(TrangThaiLichHen.VANG_MAT));
        assertEquals(1L, s.get(TrangThaiLichHen.DA_HUY));
        assertEquals(2L, s.get(TrangThaiLichHen.DA_DOI_LICH));
        assertEquals(9, r.appointments().totalRecords());
        assertEquals(s.values().stream().mapToLong(Long::longValue).sum(), r.appointments().totalRecords());
        assertEquals(4, r.appointments().activeCount());
        assertEquals(2, item(r, "LICH_HEN_CHO_XAC_NHAN").objectCount());
    }

    // ------------------------------------------------------------------ phiếu khám (Q04)

    @Test
    void examsCountTicketsByStatusAndExcludeCancelledAndOtherDays() {
        repo.exams.add(new Exam(at(D, 8, 0, 0), TrangThaiPhieuKham.CHO_KHAM));
        repo.exams.add(new Exam(at(D, 8, 5, 0), TrangThaiPhieuKham.CHO_KHAM)); // có thể cùng bệnh nhân: vẫn 2 phiếu
        repo.exams.add(new Exam(at(D, 9, 0, 0), TrangThaiPhieuKham.DANG_KHAM));
        repo.exams.add(new Exam(at(D, 10, 0, 0), TrangThaiPhieuKham.DA_KET_LUAN));
        repo.exams.add(new Exam(at(D, 10, 30, 0), TrangThaiPhieuKham.DA_HUY));
        repo.exams.add(new Exam(at(D.minusDays(1), 16, 0, 0), TrangThaiPhieuKham.CHO_KHAM));

        AdminDashboardResponse r = run();

        assertEquals(2, r.exams().waiting());
        assertEquals(1, r.exams().inProgress());
        assertEquals(1, r.exams().concluded());
        assertEquals("Phiếu chờ khám", item(r, "PHIEU_CHO_KHAM").title());
    }

    // ------------------------------------------------------------------ thu trong ngày (Q06)

    @Test
    void collectionsSumOnlySuccessfulPaymentsInDayByMethod() {
        repo.invoices.put("HD1", new Invoice("HD1", TrangThaiHoaDon.THANH_TOAN_MOT_PHAN));
        repo.payments.add(new Payment("HD1", at(D, 0, 0, 0), new BigDecimal("100000"), PhuongThucThanhToan.TIEN_MAT, TrangThaiThanhToan.THANH_CONG));
        repo.payments.add(new Payment("HD1", at(D, 23, 59, 59), new BigDecimal("250000"), PhuongThucThanhToan.CHUYEN_KHOAN, TrangThaiThanhToan.THANH_CONG));
        repo.payments.add(new Payment("HD1", at(D, 12, 0, 0), new BigDecimal("70000"), PhuongThucThanhToan.THE_POS, TrangThaiThanhToan.DA_HUY));
        repo.payments.add(new Payment("HD1", at(D.minusDays(1), 23, 59, 59), new BigDecimal("999"), PhuongThucThanhToan.TIEN_MAT, TrangThaiThanhToan.THANH_CONG));
        repo.payments.add(new Payment("HD1", D.plusDays(1).atStartOfDay(), new BigDecimal("888"), PhuongThucThanhToan.TIEN_MAT, TrangThaiThanhToan.THANH_CONG));

        AdminDashboardResponse r = run();

        assertEquals("350000", r.collections().amountVnd());
        assertEquals("100000", r.collections().byMethodVnd().get(PhuongThucThanhToan.TIEN_MAT));
        assertEquals("250000", r.collections().byMethodVnd().get(PhuongThucThanhToan.CHUYEN_KHOAN));
        assertEquals("0", r.collections().byMethodVnd().get(PhuongThucThanhToan.THE_POS));
        BigDecimal sum = r.collections().byMethodVnd().values().stream().map(BigDecimal::new).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(new BigDecimal(r.collections().amountVnd()), sum);
    }

    @Test
    void successfulPaymentOnCancelledInvoiceRaisesAnomalyWarning() {
        repo.invoices.put("HD9", new Invoice("HD9", TrangThaiHoaDon.DA_HUY));
        repo.payments.add(new Payment("HD9", at(D, 9, 0, 0), new BigDecimal("50000"), PhuongThucThanhToan.TIEN_MAT, TrangThaiThanhToan.THANH_CONG));

        AdminDashboardResponse r = run();

        assertTrue(hasWarning(r, "THU_TREN_HOA_DON_DA_HUY", "collections"));
        assertEquals("0", r.receivables().amountVnd()); // hóa đơn hủy không vào công nợ
    }

    // ------------------------------------------------------------------ công nợ (Q07, V1.1 #05)

    @Test
    void receivablesComputedPerInvoiceWithoutCrossJoinAndKeepPendingCancellation() {
        // HD1: 2 dòng (2×150.000 + 1×200.000 = 500.000), thu 100.000 + 150.000, 1 lần thu bị hủy 50.000 → còn 250.000
        repo.invoices.put("HD1", new Invoice("HD1", TrangThaiHoaDon.THANH_TOAN_MOT_PHAN));
        repo.invoiceLines.add(new InvoiceLine("HD1", 2, new BigDecimal("150000")));
        repo.invoiceLines.add(new InvoiceLine("HD1", 1, new BigDecimal("200000")));
        repo.payments.add(new Payment("HD1", at(D.minusDays(2), 9, 0, 0), new BigDecimal("100000"), PhuongThucThanhToan.TIEN_MAT, TrangThaiThanhToan.THANH_CONG));
        repo.payments.add(new Payment("HD1", at(D.minusDays(1), 9, 0, 0), new BigDecimal("150000"), PhuongThucThanhToan.THE_POS, TrangThaiThanhToan.THANH_CONG));
        repo.payments.add(new Payment("HD1", at(D.minusDays(1), 10, 0, 0), new BigDecimal("50000"), PhuongThucThanhToan.TIEN_MAT, TrangThaiThanhToan.DA_HUY));
        // HD2: đang có yêu cầu hủy CHO_DUYET → vẫn tính công nợ 300.000
        repo.invoices.put("HD2", new Invoice("HD2", TrangThaiHoaDon.CHUA_THANH_TOAN));
        repo.invoiceLines.add(new InvoiceLine("HD2", 3, new BigDecimal("100000")));
        repo.cancellationRequests.add(TrangThaiDuyet.CHO_DUYET);
        // HD3: đã hủy → loại
        repo.invoices.put("HD3", new Invoice("HD3", TrangThaiHoaDon.DA_HUY));
        repo.invoiceLines.add(new InvoiceLine("HD3", 1, new BigDecimal("700000")));
        // HD4: đã thanh toán đủ → không nợ
        repo.invoices.put("HD4", new Invoice("HD4", TrangThaiHoaDon.DA_THANH_TOAN));
        repo.invoiceLines.add(new InvoiceLine("HD4", 1, new BigDecimal("80000")));
        repo.payments.add(new Payment("HD4", at(D, 9, 0, 0), new BigDecimal("80000"), PhuongThucThanhToan.TIEN_MAT, TrangThaiThanhToan.THANH_CONG));

        AdminDashboardResponse r = run();

        assertEquals("550000", r.receivables().amountVnd());
        assertEquals(2, r.receivables().invoiceCount());
        assertEquals(0, r.receivables().overpaidInvoiceCount());
        assertEquals("TAT_CA_HOA_DON_CON_HIEU_LUC_HIEN_TAI", r.receivables().scope());
        assertEquals(1, item(r, "HOA_DON_CHO_DUYET_HUY").objectCount());
        assertEquals(WorkPriority.CHO_DUYET, item(r, "HOA_DON_CHO_DUYET_HUY").priority());
    }

    @Test
    void overpaidInvoiceIsAnomalyNotNegativeReceivable() {
        repo.invoices.put("HD1", new Invoice("HD1", TrangThaiHoaDon.DA_THANH_TOAN));
        repo.invoiceLines.add(new InvoiceLine("HD1", 1, new BigDecimal("100000")));
        repo.payments.add(new Payment("HD1", at(D, 9, 0, 0), new BigDecimal("120000"), PhuongThucThanhToan.TIEN_MAT, TrangThaiThanhToan.THANH_CONG));
        repo.invoices.put("HD2", new Invoice("HD2", TrangThaiHoaDon.CHUA_THANH_TOAN));
        repo.invoiceLines.add(new InvoiceLine("HD2", 1, new BigDecimal("40000")));

        AdminDashboardResponse r = run();

        assertEquals("40000", r.receivables().amountVnd()); // không bị trừ 20.000 thừa của HD1
        assertEquals(1, r.receivables().invoiceCount());
        assertEquals(1, r.receivables().overpaidInvoiceCount());
        assertTrue(hasWarning(r, "HOA_DON_THU_VUOT", "receivables"));
        assertEquals(WorkPriority.KHAN, item(r, "HOA_DON_THU_VUOT").priority());
    }

    @Test
    void largeAmountsKeepExactIntegerPrecision() {
        BigDecimal max18 = new BigDecimal("999999999999999999");
        repo.invoices.put("HD1", new Invoice("HD1", TrangThaiHoaDon.CHUA_THANH_TOAN));
        repo.invoices.put("HD2", new Invoice("HD2", TrangThaiHoaDon.CHUA_THANH_TOAN));
        repo.invoiceLines.add(new InvoiceLine("HD1", 1, max18));
        repo.invoiceLines.add(new InvoiceLine("HD2", 1, max18));
        repo.payments.add(new Payment("HD1", at(D, 9, 0, 0), max18, PhuongThucThanhToan.CHUYEN_KHOAN, TrangThaiThanhToan.THANH_CONG));
        repo.payments.add(new Payment("HD2", at(D, 10, 0, 0), new BigDecimal("1"), PhuongThucThanhToan.TIEN_MAT, TrangThaiThanhToan.THANH_CONG));

        AdminDashboardResponse r = run();

        assertEquals("999999999999999998", r.receivables().amountVnd());
        assertEquals("1000000000000000000", r.collections().amountVnd());
    }

    @Test
    void fractionalMoneyIsNotSilentlyRounded() {
        repo.invoices.put("HD1", new Invoice("HD1", TrangThaiHoaDon.CHUA_THANH_TOAN));
        repo.payments.add(new Payment("HD1", at(D, 9, 0, 0), new BigDecimal("100.5"), PhuongThucThanhToan.TIEN_MAT, TrangThaiThanhToan.THANH_CONG));

        DashboardDataUnavailableException ex = assertThrows(DashboardDataUnavailableException.class, this::run);
        assertEquals("collections", ex.getSection());
    }

    // ------------------------------------------------------------------ nhân sự (Q08)

    @Test
    void staffingCountsDistinctActiveEmployeesPerShiftAndRole() {
        repo.shifts.add(new ShiftRow("CA02", "Chiều", LocalTime.of(13, 0), LocalTime.of(17, 0), 1, 1, 1));
        repo.shifts.add(new ShiftRow("CA01", "Sáng", LocalTime.of(7, 0), LocalTime.of(12, 0), 2, 1, 1));
        repo.shifts.add(new ShiftRow("CA03", "Đêm", LocalTime.of(22, 0), LocalTime.of(6, 0), 1, 0, 0));
        repo.shifts.add(new ShiftRow("CA04", "Lỗi", LocalTime.of(8, 0), LocalTime.of(8, 0), 1, 0, 0));
        // CA01: BS NV1 được phân 2 lần → 1 người; NV2 nghỉ việc → loại; NV3 phân công DA_HUY → loại
        repo.assignments.add(new Assignment("NV1", VaiTro.BS, TrangThaiNhanVien.DANG_LAM_VIEC, "CA01", D, TrangThaiPhanCong.DANG_THUC_HIEN));
        repo.assignments.add(new Assignment("NV1", VaiTro.BS, TrangThaiNhanVien.DANG_LAM_VIEC, "CA01", D, TrangThaiPhanCong.CHO_HIEU_LUC));
        repo.assignments.add(new Assignment("NV2", VaiTro.BS, TrangThaiNhanVien.NGHI_VIEC, "CA01", D, TrangThaiPhanCong.DANG_THUC_HIEN));
        repo.assignments.add(new Assignment("NV3", VaiTro.BS, TrangThaiNhanVien.DANG_LAM_VIEC, "CA01", D, TrangThaiPhanCong.DA_HUY));
        repo.assignments.add(new Assignment("NV4", VaiTro.YT, TrangThaiNhanVien.DANG_LAM_VIEC, "CA01", D, TrangThaiPhanCong.HOAN_THANH));
        repo.assignments.add(new Assignment("NV5", VaiTro.LT, TrangThaiNhanVien.DANG_LAM_VIEC, "CA01", D, TrangThaiPhanCong.DANG_THUC_HIEN));
        repo.assignments.add(new Assignment("NV6", VaiTro.AD, TrangThaiNhanVien.DANG_LAM_VIEC, "CA01", D, TrangThaiPhanCong.DANG_THUC_HIEN));
        // CA02: đủ người, NV1 làm thêm ca này → được tính ở ca này
        repo.assignments.add(new Assignment("NV1", VaiTro.BS, TrangThaiNhanVien.DANG_LAM_VIEC, "CA02", D, TrangThaiPhanCong.CHO_HIEU_LUC));
        repo.assignments.add(new Assignment("NV4", VaiTro.YT, TrangThaiNhanVien.DANG_LAM_VIEC, "CA02", D, TrangThaiPhanCong.CHO_HIEU_LUC));
        repo.assignments.add(new Assignment("NV5", VaiTro.LT, TrangThaiNhanVien.DANG_LAM_VIEC, "CA02", D, TrangThaiPhanCong.CHO_HIEU_LUC));
        // CA02 có 2 BS trong khi định mức 1 → thừa không làm "thiếu" âm
        repo.assignments.add(new Assignment("NV8", VaiTro.BS, TrangThaiNhanVien.DANG_LAM_VIEC, "CA02", D, TrangThaiPhanCong.CHO_HIEU_LUC));
        // Ngày khác → loại
        repo.assignments.add(new Assignment("NV7", VaiTro.BS, TrangThaiNhanVien.DANG_LAM_VIEC, "CA03", D.minusDays(1), TrangThaiPhanCong.HOAN_THANH));

        AdminDashboardResponse r = run();
        List<ShiftStaffingDto> shifts = r.staffing().shifts();

        assertEquals(SectionStatus.AVAILABLE, r.staffing().status());
        assertEquals(List.of("CA01", "CA04", "CA02", "CA03"), shifts.stream().map(ShiftStaffingDto::shiftCode).toList());
        ShiftStaffingDto ca01 = shifts.get(0);
        assertEquals(new RoleCount(1, 1, 1), ca01.assigned());
        assertEquals(new RoleCount(1, 0, 0), ca01.missing());
        assertEquals(ShiftState.THIEU_NHAN_SU, ca01.state());
        assertEquals(ShiftState.DU_NHAN_SU, shifts.get(2).state());
        assertEquals(new RoleCount(2, 1, 1), shifts.get(2).assigned());
        assertEquals(RoleCount.ZERO, shifts.get(2).missing());
        ShiftStaffingDto ca03 = shifts.get(3);
        assertTrue(ca03.overnight());
        assertEquals(ShiftState.THIEU_NHAN_SU, ca03.state()); // ca không có phân công vẫn báo thiếu
        assertEquals(ShiftState.LOI_CAU_HINH, shifts.get(1).state());
        assertEquals(2, r.staffing().understaffedShiftCount());
        assertEquals(2, item(r, "CA_THIEU_NHAN_SU").objectCount());
        assertTrue(hasWarning(r, "CA_LAM_LOI_CAU_HINH", "staffing"));
    }

    // ------------------------------------------------------------------ kho (Q09/Q10)

    @Test
    void inventoryClassifiesLotsAndCountsDistinctItemsBelowMinimum() {
        repo.items.add(new StockItemRow(StockItemType.THUOC, "T1", 100));
        repo.items.add(new StockItemRow(StockItemType.THUOC, "T2", 10));
        repo.items.add(new StockItemRow(StockItemType.VAT_TU, "V1", 5));   // không có lô → tồn 0 → dưới định mức
        repo.items.add(new StockItemRow(StockItemType.VAT_TU, "V2", 0));   // định mức 0 → không xét
        repo.lots.add(new Lot("L01", "T1", null, D, 500));                 // HSD hôm nay → hết hạn, không khả dụng
        repo.lots.add(new Lot("L02", "T1", null, D.plusDays(1), 30));      // cận hạn
        repo.lots.add(new Lot("L03", "T1", null, D.plusDays(30), 30));     // cận hạn (biên)
        repo.lots.add(new Lot("L04", "T2", null, D.plusDays(31), 20));     // còn hạn, không cận
        repo.lots.add(new Lot("L05", "T2", null, null, 50));               // không rõ HSD
        repo.lots.add(new Lot("L06", "T2", null, D.minusDays(3), 0));      // lô rỗng → bỏ qua
        repo.lots.add(new Lot("L07", null, "V2", D.plusDays(100), -4));    // tồn âm → bất thường
        repo.lots.add(new Lot("L08", "T2", "V2", D.plusDays(100), 3));     // trỏ cả thuốc và vật tư → bất thường

        AdminDashboardResponse r = run();

        assertEquals(SectionStatus.AVAILABLE, r.inventory().status());
        assertEquals(1, r.inventory().expiredLotCount());
        assertEquals(2, r.inventory().nearExpiryLotCount());
        assertEquals(1, r.inventory().unknownExpiryLotCount());
        assertEquals(2, r.inventory().anomalyLotCount());
        assertEquals(2, r.inventory().belowMinimumItemCount()); // T1 (60 < 100) và V1 (0 < 5); T2 = 20 ≥ 10
        assertTrue(hasWarning(r, "LO_HANG_BAT_THUONG", "inventory"));
        assertEquals(1, item(r, "KHO_HET_HAN").objectCount());
        assertEquals(1, item(r, "KHO_CHUA_RO_HSD").objectCount());
    }

    @Test
    void configuredExpiryThresholdIsUsedAndInvalidValueFallsBackWithWarning() {
        repo.items.add(new StockItemRow(StockItemType.THUOC, "T1", 0));
        repo.lots.add(new Lot("L01", "T1", null, D.plusDays(40), 1));

        repo.parameters.put(DashboardThresholds.KEY_NEAR_EXPIRY_DAYS, " 45 ");
        AdminDashboardResponse configured = run();
        assertEquals(45, configured.inventory().thresholdDays());
        assertEquals(1, configured.inventory().nearExpiryLotCount());
        assertFalse(hasWarning(configured, "THAM_SO_CHUA_CAU_HINH", "inventory"));

        repo.parameters.put(DashboardThresholds.KEY_NEAR_EXPIRY_DAYS, "abc");
        AdminDashboardResponse invalid = run();
        assertEquals(30, invalid.inventory().thresholdDays());
        assertEquals(0, invalid.inventory().nearExpiryLotCount());
        assertTrue(hasWarning(invalid, "THAM_SO_KHONG_HOP_LE", "inventory"));

        repo.parameters.put(DashboardThresholds.KEY_NEAR_EXPIRY_DAYS, "0");
        assertTrue(hasWarning(run(), "THAM_SO_KHONG_HOP_LE", "inventory"));
    }

    // ------------------------------------------------------------------ cấp cứu (Q11)

    @Test
    void emergencyKitWithOnlySampleCatalogIsUnconfiguredNeverPassed() {
        repo.catalog.add(new Catalog(true, true));
        repo.catalog.add(new Catalog(false, false));
        repo.checks.add(new Check("KT1", at(D, 8, 0, 0), KetQuaKiemTra.DAT));

        AdminDashboardResponse r = run();

        assertEquals(SectionStatus.UNCONFIGURED, r.emergencyKit().status());
        assertNull(r.emergencyKit().checkState());
        assertEquals("Chưa có danh mục cấp cứu chính thức", r.emergencyKit().message());
    }

    @Test
    void emergencyKitWithoutAnyCheckNeedsCheck() {
        repo.catalog.add(new Catalog(true, false));
        repo.checks.add(new Check("KT9", at(D, 20, 0, 0), KetQuaKiemTra.DAT)); // sau thời điểm hiện tại → bỏ qua

        AdminDashboardResponse r = run();

        assertEquals(EmergencyCheckState.CHUA_CO_KIEM_TRA, r.emergencyKit().checkState());
        assertEquals(WorkPriority.KHAN, item(r, "CAP_CUU_CAN_KIEM_TRA").priority());
    }

    @Test
    void emergencyKitCycleBoundaryAndNeedsAction() {
        repo.catalog.add(new Catalog(true, false));
        repo.checks.add(new Check("KT1", at(D.minusDays(7), 9, 0, 0), KetQuaKiemTra.DAT));
        repo.checkItems.add(new CheckItem("KT1", true, D.plusDays(90)));
        AdminDashboardResponse within = run();
        assertEquals(EmergencyCheckState.TRONG_CHU_KY, within.emergencyKit().checkState());
        assertEquals(7L, within.emergencyKit().daysSinceLastCheck());
        assertEquals("2026-10-12T09:00+07:00", within.emergencyKit().lastCheckedAt().toString());
        assertNull(item(within, "CAP_CUU_CAN_KIEM_TRA"));

        repo.checks.clear();
        repo.checks.add(new Check("KT1", at(D.minusDays(8), 9, 0, 0), KetQuaKiemTra.DAT));
        assertEquals(EmergencyCheckState.QUA_CHU_KY, run().emergencyKit().checkState());

        repo.checks.add(new Check("KT2", at(D, 7, 0, 0), KetQuaKiemTra.CAN_BO_SUNG));
        repo.checkItems.add(new CheckItem("KT2", true, D.plusDays(90)));
        assertEquals(EmergencyCheckState.CAN_XU_LY, run().emergencyKit().checkState());

        // DAT nhưng có mục hạn dùng không còn hơn 30 ngày → cần xử lý
        repo.checks.add(new Check("KT3", at(D, 8, 0, 0), KetQuaKiemTra.DAT));
        repo.checkItems.add(new CheckItem("KT3", true, D.plusDays(30)));
        AdminDashboardResponse expiring = run();
        assertEquals(EmergencyCheckState.CAN_XU_LY, expiring.emergencyKit().checkState());
        assertEquals(1, item(expiring, "CAP_CUU_CAN_XU_LY").objectCount());
    }

    // ------------------------------------------------------------------ kế hoạch điều trị (Q12)

    @Test
    void treatmentDueCountsDistinctPlansWithinHorizon() {
        // KH1: 3 hạng mục chờ trong hạn → 1 kế hoạch; có 1 hạng mục quá hạn
        repo.planItems.add(new PlanItem("KH1", TrangThaiKeHoach.DANG_THUC_HIEN, TrangThaiChiTietDieuTri.CHUA_THUC_HIEN, D.minusDays(1)));
        repo.planItems.add(new PlanItem("KH1", TrangThaiKeHoach.DANG_THUC_HIEN, TrangThaiChiTietDieuTri.CHUA_THUC_HIEN, D));
        repo.planItems.add(new PlanItem("KH1", TrangThaiKeHoach.DANG_THUC_HIEN, TrangThaiChiTietDieuTri.CHUA_THUC_HIEN, D.plusDays(5)));
        // KH2: hạn đúng D+14 → tính, không quá hạn
        repo.planItems.add(new PlanItem("KH2", TrangThaiKeHoach.DANG_THUC_HIEN, TrangThaiChiTietDieuTri.CHUA_THUC_HIEN, D.plusDays(14)));
        // KH3: D+15 → ngoài cửa sổ
        repo.planItems.add(new PlanItem("KH3", TrangThaiKeHoach.DANG_THUC_HIEN, TrangThaiChiTietDieuTri.CHUA_THUC_HIEN, D.plusDays(15)));
        // KH4: kế hoạch đã hoàn thành → loại; KH5: hạng mục đã thực hiện → loại
        repo.planItems.add(new PlanItem("KH4", TrangThaiKeHoach.HOAN_THANH, TrangThaiChiTietDieuTri.CHUA_THUC_HIEN, D));
        repo.planItems.add(new PlanItem("KH5", TrangThaiKeHoach.DANG_THUC_HIEN, TrangThaiChiTietDieuTri.DA_THUC_HIEN, D.minusDays(2)));

        AdminDashboardResponse r = run();

        assertEquals(2L, r.treatmentDue().duePlanCount());
        assertEquals(1L, r.treatmentDue().overduePlanCount());
        assertEquals("Kế hoạch điều trị đến hạn (bao gồm quá hạn)", item(r, "KE_HOACH_DEN_HAN").title());
        assertEquals(1, item(r, "KE_HOACH_QUA_HAN").objectCount());
    }

    // ------------------------------------------------------------------ lỗi nguồn dữ liệu

    @Test
    void coreSectionFailureThrowsInsteadOfReturningZerosAndHidesSqlDetails() {
        repo.failingMethods.add("sumBilledAmountPerActiveInvoice");

        DashboardDataUnavailableException ex = assertThrows(DashboardDataUnavailableException.class, this::run);

        assertEquals("receivables", ex.getSection());
        assertFalse(ex.getMessage().contains("SELECT"));
        assertFalse(ex.getMessage().contains("0900000000"));
    }

    @Test
    void appointmentFailureThrows() {
        repo.failingMethods.add("countAppointmentsByStatus");
        assertEquals("appointments", assertThrows(DashboardDataUnavailableException.class, this::run).getSection());
    }

    @Test
    void optionalSectionFailuresBecomeUnavailableWithNullCountsAndNoFakeWorkItems() {
        repo.failingMethods.add("findShifts");
        repo.failingMethods.add("findNonEmptyStockLots");
        repo.failingMethods.add("countActiveRealEmergencyCatalogItems");
        repo.failingMethods.add("countPlansWithPendingItemsDueBefore");
        repo.failingMethods.add("countPendingStockAdjustments");
        repo.appointments.add(new Appointment(at(D, 9, 0, 0), TrangThaiLichHen.CHO_XAC_NHAN));

        AdminDashboardResponse r = run();

        assertEquals(SectionStatus.UNAVAILABLE, r.staffing().status());
        assertNull(r.staffing().understaffedShiftCount());
        assertEquals(SectionStatus.UNAVAILABLE, r.inventory().status());
        assertNull(r.inventory().belowMinimumItemCount());
        assertNull(r.inventory().expiredLotCount());
        assertEquals(SectionStatus.UNAVAILABLE, r.emergencyKit().status());
        assertEquals(SectionStatus.UNAVAILABLE, r.treatmentDue().status());
        assertNull(r.treatmentDue().duePlanCount());
        for (String section : List.of("staffing", "inventory", "emergencyKit", "treatmentDue", "workQueue")) {
            assertTrue(hasWarning(r, "SECTION_KHONG_TAI_DUOC", section), section);
        }
        for (DashboardWarning w : r.warnings()) {
            assertFalse(w.message().contains("SELECT"));
        }
        // Chỉ còn mục có nguồn hợp lệ.
        assertEquals(List.of("LICH_HEN_CHO_XAC_NHAN"), r.workQueue().items().stream().map(WorkQueueItem::code).toList());
        // Section lõi vẫn có số liệu.
        assertEquals(1, r.appointments().totalRecords());
    }

    @Test
    void parameterReadFailureFallsBackWithWarning() {
        repo.failingMethods.add("findSystemParameter");
        AdminDashboardResponse r = run();
        assertEquals(30, r.inventory().thresholdDays());
        assertTrue(hasWarning(r, "THAM_SO_KHONG_DOC_DUOC", "inventory"));
    }

    // ------------------------------------------------------------------ hàng việc (Q03)

    @Test
    void workQueueGroupsAreDynamicSortedAndCountedSeparately() {
        repo.appointments.add(new Appointment(at(D, 9, 0, 0), TrangThaiLichHen.CHO_XAC_NHAN));
        repo.appointments.add(new Appointment(at(D, 10, 0, 0), TrangThaiLichHen.CHO_XAC_NHAN));
        repo.exams.add(new Exam(at(D, 9, 0, 0), TrangThaiPhieuKham.CHO_KHAM));
        repo.stockAdjustments.add(TrangThaiDuyet.CHO_DUYET);
        repo.stockAdjustments.add(TrangThaiDuyet.DA_DUYET);
        repo.items.add(new StockItemRow(StockItemType.VAT_TU, "V1", 5));
        repo.catalog.add(new Catalog(true, false));

        AdminDashboardResponse r = run();
        List<String> codes = r.workQueue().items().stream().map(WorkQueueItem::code).toList();

        assertEquals(List.of("CAP_CUU_CAN_KIEM_TRA", "KHO_DUOI_DINH_MUC",
                "KHO_CHO_DUYET_DIEU_CHINH", "LICH_HEN_CHO_XAC_NHAN",
                "PHIEU_CHO_KHAM"), codes);
        assertEquals(5, r.workQueue().workGroupCount());
        assertEquals(1 + 1 + 1 + 2 + 1, r.workQueue().relatedObjectCount());
        r.workQueue().items().forEach(i -> {
            assertTrue(i.objectCount() > 0);
            assertNotNull(i.targetScreen());
            assertNotNull(i.sourceMetric());
        });
    }

    // ------------------------------------------------------------------ bất biến

    @Test
    void responseCollectionsAreImmutable() {
        AdminDashboardResponse r = run();
        assertThrows(UnsupportedOperationException.class, () -> r.appointments().byStatus().put(TrangThaiLichHen.DA_HUY, 9L));
        assertThrows(UnsupportedOperationException.class, () -> r.warnings().add(new DashboardWarning("X", "Y", "Z")));
        assertThrows(UnsupportedOperationException.class, () -> r.workQueue().items().clear());
    }

    @Test
    void serviceIsReadOnlyAndOnlyCallsQueryMethods() {
        run();
        assertFalse(repo.calls.isEmpty());
        repo.calls.forEach(c -> assertTrue(c.startsWith("count") || c.startsWith("sum") || c.startsWith("find"), c));
    }
}
