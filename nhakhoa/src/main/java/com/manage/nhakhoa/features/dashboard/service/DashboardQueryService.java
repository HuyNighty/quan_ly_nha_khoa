package com.manage.nhakhoa.features.dashboard.service;

import com.manage.nhakhoa.features.capcuu.enums.KetQuaKiemTra;
import com.manage.nhakhoa.features.dashboard.dto.response.AdminDashboardResponse;
import com.manage.nhakhoa.features.dashboard.dto.response.AppointmentSummary;
import com.manage.nhakhoa.features.dashboard.dto.response.CollectionSummary;
import com.manage.nhakhoa.features.dashboard.dto.response.DashboardWarning;
import com.manage.nhakhoa.features.dashboard.dto.response.EmergencyCheckState;
import com.manage.nhakhoa.features.dashboard.dto.response.EmergencyKitSummary;
import com.manage.nhakhoa.features.dashboard.dto.response.ExamSummary;
import com.manage.nhakhoa.features.dashboard.dto.response.InventorySummary;
import com.manage.nhakhoa.features.dashboard.dto.response.PeriodDto;
import com.manage.nhakhoa.features.dashboard.dto.response.ReceivableSummary;
import com.manage.nhakhoa.features.dashboard.dto.response.SectionStatus;
import com.manage.nhakhoa.features.dashboard.dto.response.ShiftStaffingDto;
import com.manage.nhakhoa.features.dashboard.dto.response.ShiftState;
import com.manage.nhakhoa.features.dashboard.dto.response.StaffingSummary;
import com.manage.nhakhoa.features.dashboard.dto.response.TreatmentDueSummary;
import com.manage.nhakhoa.features.dashboard.dto.response.WorkPriority;
import com.manage.nhakhoa.features.dashboard.repository.DashboardReadRepository;
import com.manage.nhakhoa.features.dashboard.repository.EmergencyCheckRow;
import com.manage.nhakhoa.features.hoadon.enums.PhuongThucThanhToan;
import com.manage.nhakhoa.features.khamchuabenh.enums.TrangThaiPhieuKham;
import com.manage.nhakhoa.features.lichhen.enums.TrangThaiLichHen;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Tổng hợp Dashboard Admin chỉ đọc theo BA V1/V1.1 và contract 03.
 *
 * <p>Không phụ thuộc HTTP: endpoint REST CHƯA được tạo vì AUTH-R1 chưa có trong checkout (GATE_AUTH=BLOCKED).
 * Ngày nghiệp vụ lấy từ {@link Clock} inject theo {@code Asia/Ho_Chi_Minh}, khoảng nửa mở [D 00:00, D+1 00:00).</p>
 *
 * <p>Section lõi (lịch hẹn, phiếu khám, thu, công nợ) lỗi → {@link DashboardDataUnavailableException}
 * (không trả payload toàn 0). Section phụ (ca làm, kho, cấp cứu, kế hoạch điều trị) lỗi → UNAVAILABLE + cảnh báo,
 * số liệu null.</p>
 */
@Service
// Không chia sẻ transaction giữa section. Repository mở transaction read-only cho mỗi lời gọi;
// lỗi SQL đã rollback xong trước khi service bắt để trả UNAVAILABLE. Tạm ngưng cả transaction của caller.
// Đổi lại, các section READ_COMMITTED không phải một snapshot nguyên tử của toàn Dashboard.
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class DashboardQueryService {

    public static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    public static final String SOURCE_LIVE = "LIVE";
    public static final String EXAM_UNIT = "PHIEU_KHAM_TIEP_NHAN_TRONG_NGAY";
    public static final String COLLECTION_DEFINITION = "THANH_TOAN_THANH_CONG_TRONG_NGAY_THEO_TRANG_THAI_HIEN_TAI";
    public static final String RECEIVABLE_SCOPE = "TAT_CA_HOA_DON_CON_HIEU_LUC_HIEN_TAI";

    static final String SECTION_STAFFING = "staffing";
    static final String SECTION_INVENTORY = "inventory";
    static final String SECTION_EMERGENCY = "emergencyKit";
    static final String SECTION_TREATMENT = "treatmentDue";
    static final String SECTION_COLLECTIONS = "collections";
    static final String SECTION_RECEIVABLES = "receivables";
    static final String SECTION_WORK_QUEUE = "workQueue";

    private final DashboardReadRepository repository;
    private final Clock clock;
    private final ThresholdResolver thresholds;

    public DashboardQueryService(DashboardReadRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
        this.thresholds = new ThresholdResolver(repository);
    }

    public AdminDashboardResponse getAdminDashboard() {
        ZonedDateTime now = ZonedDateTime.now(clock.withZone(BUSINESS_ZONE));
        LocalDate businessDate = now.toLocalDate();
        ZonedDateTime start = businessDate.atStartOfDay(BUSINESS_ZONE);
        ZonedDateTime end = businessDate.plusDays(1).atStartOfDay(BUSINESS_ZONE);
        LocalDateTime startLocal = start.toLocalDateTime();
        LocalDateTime endLocal = end.toLocalDateTime();

        List<DashboardWarning> warnings = new ArrayList<>();
        WorkQueueBuilder workQueue = new WorkQueueBuilder();

        AppointmentSummary appointments = loadAppointments(startLocal, endLocal);
        ExamSummary exams = loadExams(startLocal, endLocal);
        CollectionSummary collections = loadCollections(startLocal, endLocal, warnings);
        ReceivableSummary receivables = loadReceivables(warnings, workQueue);

        StaffingSummary staffing = loadStaffing(businessDate, warnings, workQueue);
        InventorySummary inventory = loadInventory(businessDate, warnings, workQueue);
        EmergencyKitSummary emergencyKit = loadEmergencyKit(now.toLocalDateTime(), businessDate, warnings, workQueue);
        TreatmentDueSummary treatmentDue = loadTreatmentDue(businessDate, warnings, workQueue);

        workQueue.add("LICH_HEN_CHO_XAC_NHAN", WorkPriority.CHO_DUYET, "Lịch hẹn chờ xác nhận",
                appointments.byStatus().get(TrangThaiLichHen.CHO_XAC_NHAN),
                DashboardTargetScreens.LICH_HEN, "appointments.byStatus.CHO_XAC_NHAN");
        workQueue.add("PHIEU_CHO_KHAM", WorkPriority.THEO_DOI, "Phiếu chờ khám",
                exams.waiting(), DashboardTargetScreens.PHIEU_KHAM, "exams.waiting");
        addPendingApprovals(warnings, workQueue);

        return new AdminDashboardResponse(
                SOURCE_LIVE,
                businessDate,
                BUSINESS_ZONE.getId(),
                now.toOffsetDateTime(),
                new PeriodDto(start.toOffsetDateTime(), end.toOffsetDateTime()),
                appointments,
                exams,
                collections,
                receivables,
                staffing,
                inventory,
                emergencyKit,
                treatmentDue,
                workQueue.build(),
                warnings);
    }

    // ---------------------------------------------------------------- core sections

    private AppointmentSummary loadAppointments(LocalDateTime start, LocalDateTime end) {
        try {
            Map<TrangThaiLichHen, Long> raw = repository.countAppointmentsByStatus(start, end);
            Map<TrangThaiLichHen, Long> byStatus = new EnumMap<>(TrangThaiLichHen.class);
            long total = 0;
            for (TrangThaiLichHen status : TrangThaiLichHen.values()) {
                long count = nonNegative(raw.get(status));
                byStatus.put(status, count);
                total += count;
            }
            long active = byStatus.get(TrangThaiLichHen.CHO_XAC_NHAN)
                    + byStatus.get(TrangThaiLichHen.DA_XAC_NHAN)
                    + byStatus.get(TrangThaiLichHen.DA_DEN);
            return new AppointmentSummary(total, active, byStatus);
        } catch (RuntimeException ex) {
            throw new DashboardDataUnavailableException("appointments", ex);
        }
    }

    private ExamSummary loadExams(LocalDateTime start, LocalDateTime end) {
        try {
            Map<TrangThaiPhieuKham, Long> raw = repository.countExamsByStatus(start, end);
            return new ExamSummary(
                    nonNegative(raw.get(TrangThaiPhieuKham.CHO_KHAM)),
                    nonNegative(raw.get(TrangThaiPhieuKham.DANG_KHAM)),
                    nonNegative(raw.get(TrangThaiPhieuKham.DA_KET_LUAN)),
                    EXAM_UNIT);
        } catch (RuntimeException ex) {
            throw new DashboardDataUnavailableException("exams", ex);
        }
    }

    private CollectionSummary loadCollections(LocalDateTime start, LocalDateTime end, List<DashboardWarning> warnings) {
        Map<PhuongThucThanhToan, String> byMethod = new EnumMap<>(PhuongThucThanhToan.class);
        BigDecimal total = BigDecimal.ZERO;
        long paymentsOnCancelled;
        try {
            Map<PhuongThucThanhToan, BigDecimal> raw = repository.sumSuccessfulPaymentsByMethod(start, end);
            for (PhuongThucThanhToan method : PhuongThucThanhToan.values()) {
                BigDecimal amount = Money.vnd(raw.get(method));
                byMethod.put(method, amount.toPlainString());
                total = total.add(amount);
            }
            paymentsOnCancelled = repository.countSuccessfulPaymentsOnCancelledInvoices();
        } catch (RuntimeException ex) {
            throw new DashboardDataUnavailableException(SECTION_COLLECTIONS, ex);
        }
        if (paymentsOnCancelled > 0) {
            warnings.add(new DashboardWarning("THU_TREN_HOA_DON_DA_HUY", SECTION_COLLECTIONS,
                    "Đối soát toàn hệ thống (không giới hạn ngày): có " + paymentsOnCancelled
                            + " giao dịch thành công thuộc hóa đơn đã hủy; khác với số thu trong ngày."));
        }
        return new CollectionSummary(Money.format(total), byMethod, COLLECTION_DEFINITION);
    }

    private ReceivableSummary loadReceivables(List<DashboardWarning> warnings, WorkQueueBuilder workQueue) {
        ReceivableCalculator.Result result;
        try {
            result = ReceivableCalculator.calculate(
                    repository.sumBilledAmountPerActiveInvoice(),
                    repository.sumSuccessfulPaymentsPerActiveInvoice());
        } catch (RuntimeException ex) {
            throw new DashboardDataUnavailableException(SECTION_RECEIVABLES, ex);
        }
        if (result.overpaidInvoiceCount() > 0) {
            warnings.add(new DashboardWarning("HOA_DON_THU_VUOT", SECTION_RECEIVABLES,
                    "Có " + result.overpaidInvoiceCount() + " hóa đơn đã thu vượt tổng tiền; không tính công nợ âm."));
        }
        workQueue.add("HOA_DON_THU_VUOT", WorkPriority.KHAN, "Hóa đơn thu vượt cần đối soát",
                result.overpaidInvoiceCount(), DashboardTargetScreens.HOA_DON, "receivables.overpaidInvoiceCount");
        return new ReceivableSummary(Money.format(result.amount()), result.invoiceCount(), RECEIVABLE_SCOPE,
                result.overpaidInvoiceCount());
    }

    // ---------------------------------------------------------------- optional sections

    private StaffingSummary loadStaffing(LocalDate businessDate, List<DashboardWarning> warnings, WorkQueueBuilder workQueue) {
        List<ShiftStaffingDto> shifts;
        try {
            shifts = StaffingCalculator.calculate(repository.findShifts(),
                    repository.countAssignedStaffByShiftAndRole(businessDate));
        } catch (RuntimeException ex) {
            warnings.add(unavailable(SECTION_STAFFING, "Không tải được dữ liệu ca làm/phân công."));
            return new StaffingSummary(SectionStatus.UNAVAILABLE, List.of(), null);
        }
        if (shifts.isEmpty()) {
            warnings.add(new DashboardWarning("CHUA_CAU_HINH_CA_LAM", SECTION_STAFFING, "Chưa có ca làm việc nào được cấu hình."));
            return new StaffingSummary(SectionStatus.UNCONFIGURED, List.of(), null);
        }
        int understaffed = (int) shifts.stream().filter(s -> s.state() == ShiftState.THIEU_NHAN_SU).count();
        long misconfigured = shifts.stream().filter(s -> s.state() == ShiftState.LOI_CAU_HINH).count();
        if (misconfigured > 0) {
            warnings.add(new DashboardWarning("CA_LAM_LOI_CAU_HINH", SECTION_STAFFING,
                    "Có " + misconfigured + " ca có giờ hoặc định mức không hợp lệ."));
        }
        workQueue.add("CA_THIEU_NHAN_SU", WorkPriority.KHAN, "Ca thiếu nhân sự theo phân công",
                understaffed, DashboardTargetScreens.PHAN_CONG_CA, "staffing.understaffedShiftCount");
        workQueue.add("CA_LOI_CAU_HINH", WorkPriority.THEO_DOI, "Ca làm cấu hình chưa hợp lệ",
                misconfigured, DashboardTargetScreens.PHAN_CONG_CA, "staffing.shifts[state=LOI_CAU_HINH]");
        return new StaffingSummary(SectionStatus.AVAILABLE, shifts, understaffed);
    }

    private InventorySummary loadInventory(LocalDate businessDate, List<DashboardWarning> warnings, WorkQueueBuilder workQueue) {
        int thresholdDays = thresholds.resolveDays(DashboardThresholds.KEY_NEAR_EXPIRY_DAYS,
                DashboardThresholds.DEFAULT_NEAR_EXPIRY_DAYS, SECTION_INVENTORY, warnings);
        InventoryCalculator.Result result;
        try {
            result = InventoryCalculator.calculate(repository.findNonEmptyStockLots(), repository.findStockItems(),
                    businessDate, thresholdDays);
        } catch (RuntimeException ex) {
            warnings.add(unavailable(SECTION_INVENTORY, "Không tải được dữ liệu tồn kho."));
            return new InventorySummary(SectionStatus.UNAVAILABLE, null, null, null, null, thresholdDays, null);
        }
        if (result.anomalyLotCount() > 0) {
            warnings.add(new DashboardWarning("LO_HANG_BAT_THUONG", SECTION_INVENTORY,
                    "Có " + result.anomalyLotCount() + " lô tồn âm hoặc không gắn đúng một thuốc/vật tư; đã loại khỏi phép tính."));
        }
        workQueue.add("KHO_HET_HAN", WorkPriority.KHAN, "Lô còn tồn đã hết hạn",
                result.expiredLotCount(), DashboardTargetScreens.TON_KHO, "inventory.expiredLotCount");
        workQueue.add("KHO_DUOI_DINH_MUC", WorkPriority.KHAN, "Mặt hàng dưới định mức",
                result.belowMinimumItemCount(), DashboardTargetScreens.TON_KHO, "inventory.belowMinimumItemCount");
        workQueue.add("KHO_CAN_HAN", WorkPriority.THEO_DOI, "Lô cận hạn sử dụng",
                result.nearExpiryLotCount(), DashboardTargetScreens.TON_KHO, "inventory.nearExpiryLotCount");
        workQueue.add("KHO_CHUA_RO_HSD", WorkPriority.THEO_DOI, "Lô chưa xác định hạn dùng",
                result.unknownExpiryLotCount(), DashboardTargetScreens.TON_KHO, "inventory.unknownExpiryLotCount");
        return new InventorySummary(SectionStatus.AVAILABLE, result.belowMinimumItemCount(), result.nearExpiryLotCount(),
                result.expiredLotCount(), result.unknownExpiryLotCount(), thresholdDays, result.anomalyLotCount());
    }

    private EmergencyKitSummary loadEmergencyKit(LocalDateTime nowLocal, LocalDate businessDate,
                                                 List<DashboardWarning> warnings, WorkQueueBuilder workQueue) {
        int cycleDays = thresholds.resolveDays(DashboardThresholds.KEY_EMERGENCY_CHECK_CYCLE_DAYS,
                DashboardThresholds.DEFAULT_EMERGENCY_CHECK_CYCLE_DAYS, SECTION_EMERGENCY, warnings);
        int safeDays = thresholds.resolveDays(DashboardThresholds.KEY_EMERGENCY_SAFE_EXPIRY_DAYS,
                DashboardThresholds.DEFAULT_EMERGENCY_SAFE_EXPIRY_DAYS, SECTION_EMERGENCY, warnings);
        try {
            long officialCount = repository.countActiveRealEmergencyCatalogItems();
            if (officialCount == 0) {
                workQueue.add("CAP_CUU_CHUA_CAU_HINH", WorkPriority.THEO_DOI, "Chưa có danh mục cấp cứu chính thức",
                        1, DashboardTargetScreens.CAP_CUU, "emergencyKit.status");
                return new EmergencyKitSummary(SectionStatus.UNCONFIGURED, null, null, null, cycleDays,
                        "Chưa có danh mục cấp cứu chính thức");
            }
            Optional<EmergencyCheckRow> latest = repository.findLatestEmergencyCheckAtOrBefore(nowLocal);
            if (latest.isEmpty()) {
                workQueue.add("CAP_CUU_CAN_KIEM_TRA", WorkPriority.KHAN, "Hộp cấp cứu cần kiểm tra",
                        1, DashboardTargetScreens.CAP_CUU, "emergencyKit.checkState");
                return new EmergencyKitSummary(SectionStatus.AVAILABLE, EmergencyCheckState.CHUA_CO_KIEM_TRA, null, null,
                        cycleDays, "Chưa có lần kiểm tra nào — cần kiểm tra");
            }
            EmergencyCheckRow check = latest.get();
            long days = ChronoUnit.DAYS.between(check.checkedAt().toLocalDate(), businessDate);
            EmergencyCoverageCalculator.Result coverage = EmergencyCoverageCalculator.calculate(officialCount,
                    repository.findEmergencyCheckCoverage(check.checkId()), businessDate.plusDays(safeDays));
            var lastCheckedAt = check.checkedAt().atZone(BUSINESS_ZONE).toOffsetDateTime();

            if (!coverage.complete()) {
                warnings.add(new DashboardWarning("CAP_CUU_CHUA_DU_THONG_TIN", SECTION_EMERGENCY,
                        "Lần kiểm tra chưa bao phủ đầy đủ danh mục chính thức hoặc thiếu thông tin hạn dùng/số lượng; cần kiểm tra."));
                workQueue.add("CAP_CUU_CAN_KIEM_TRA", WorkPriority.KHAN, "Hộp cấp cứu cần kiểm tra đầy đủ",
                        1, DashboardTargetScreens.CAP_CUU, "emergencyKit.checkState");
                return new EmergencyKitSummary(SectionStatus.AVAILABLE, EmergencyCheckState.CHUA_DU_THONG_TIN,
                        lastCheckedAt, days, cycleDays, "Chưa đủ thông tin — cần kiểm tra đầy đủ danh mục chính thức");
            }
            if (check.result() == KetQuaKiemTra.CAN_BO_SUNG || coverage.failingItemCount() > 0) {
                workQueue.add("CAP_CUU_CAN_XU_LY", WorkPriority.KHAN, "Hộp cấp cứu cần bổ sung/xử lý",
                        1, DashboardTargetScreens.CAP_CUU, "emergencyKit.checkState");
                return new EmergencyKitSummary(SectionStatus.AVAILABLE, EmergencyCheckState.CAN_XU_LY, lastCheckedAt, days,
                        cycleDays, "Lần kiểm tra gần nhất có mục cần bổ sung hoặc sắp hết hạn");
            }
            if (days > cycleDays) {
                workQueue.add("CAP_CUU_CAN_KIEM_TRA", WorkPriority.KHAN, "Hộp cấp cứu cần kiểm tra",
                        1, DashboardTargetScreens.CAP_CUU, "emergencyKit.checkState");
                return new EmergencyKitSummary(SectionStatus.AVAILABLE, EmergencyCheckState.QUA_CHU_KY, lastCheckedAt, days,
                        cycleDays, "Đã quá chu kỳ kiểm tra — cần kiểm tra");
            }
            return new EmergencyKitSummary(SectionStatus.AVAILABLE, EmergencyCheckState.TRONG_CHU_KY, lastCheckedAt, days,
                    cycleDays, "Đã kiểm tra trong chu kỳ; không phải chứng nhận an toàn lâm sàng");
        } catch (RuntimeException ex) {
            warnings.add(unavailable(SECTION_EMERGENCY, "Không tải được dữ liệu kiểm tra cấp cứu."));
            return new EmergencyKitSummary(SectionStatus.UNAVAILABLE, null, null, null, cycleDays,
                    "Không tải được dữ liệu kiểm tra cấp cứu");
        }
    }

    private TreatmentDueSummary loadTreatmentDue(LocalDate businessDate, List<DashboardWarning> warnings,
                                                 WorkQueueBuilder workQueue) {
        int horizonDays = thresholds.resolveDays(DashboardThresholds.KEY_TREATMENT_HORIZON_DAYS,
                DashboardThresholds.DEFAULT_TREATMENT_HORIZON_DAYS, SECTION_TREATMENT, warnings);
        long due;
        long overdue;
        try {
            due = repository.countPlansWithPendingItemsDueOnOrBefore(businessDate.plusDays(horizonDays));
            overdue = repository.countPlansWithPendingItemsDueBefore(businessDate);
        } catch (RuntimeException ex) {
            warnings.add(unavailable(SECTION_TREATMENT, "Không tải được dữ liệu kế hoạch điều trị."));
            return new TreatmentDueSummary(SectionStatus.UNAVAILABLE, null, null, horizonDays);
        }
        workQueue.add("KE_HOACH_QUA_HAN", WorkPriority.KHAN, "Kế hoạch điều trị có hạng mục quá hạn",
                overdue, DashboardTargetScreens.KE_HOACH_DIEU_TRI, "treatmentDue.overduePlanCount");
        workQueue.add("KE_HOACH_DEN_HAN", WorkPriority.THEO_DOI, "Kế hoạch điều trị đến hạn (bao gồm quá hạn)",
                due, DashboardTargetScreens.KE_HOACH_DIEU_TRI, "treatmentDue.duePlanCount");
        return new TreatmentDueSummary(SectionStatus.AVAILABLE, due, overdue, horizonDays);
    }

    private void addPendingApprovals(List<DashboardWarning> warnings, WorkQueueBuilder workQueue) {
        try {
            workQueue.add("HOA_DON_CHO_DUYET_HUY", WorkPriority.CHO_DUYET, "Yêu cầu hủy hóa đơn chờ duyệt",
                    repository.countPendingInvoiceCancellationRequests(), DashboardTargetScreens.DUYET_HUY,
                    "YeuCauHuyHoaDon[trangThai=CHO_DUYET]");
        } catch (RuntimeException ex) {
            warnings.add(unavailable(SECTION_WORK_QUEUE, "Không tải được yêu cầu hủy hóa đơn chờ duyệt."));
        }
        try {
            workQueue.add("KHO_CHO_DUYET_DIEU_CHINH", WorkPriority.CHO_DUYET, "Phiếu điều chỉnh kho chờ duyệt",
                    repository.countPendingStockAdjustments(), DashboardTargetScreens.DIEU_CHINH_KHO,
                    "PhieuDieuChinhKho[trangThai=CHO_DUYET]");
        } catch (RuntimeException ex) {
            warnings.add(unavailable(SECTION_WORK_QUEUE, "Không tải được phiếu điều chỉnh kho chờ duyệt."));
        }
    }

    private static DashboardWarning unavailable(String section, String message) {
        return new DashboardWarning("SECTION_KHONG_TAI_DUOC", section, message);
    }

    private static long nonNegative(Long value) {
        if (value == null) {
            return 0L;
        }
        if (value < 0) {
            throw new IllegalStateException("Số đếm âm từ nguồn dữ liệu");
        }
        return value;
    }
}
