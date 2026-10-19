package com.manage.nhakhoa.features.dashboard.service;

import com.manage.nhakhoa.features.capcuu.enums.KetQuaKiemTra;
import com.manage.nhakhoa.features.dashboard.repository.DashboardReadRepository;
import com.manage.nhakhoa.features.dashboard.repository.EmergencyCheckRow;
import com.manage.nhakhoa.features.dashboard.repository.EmergencyCheckItemRow;
import com.manage.nhakhoa.features.dashboard.repository.InvoiceAmountRow;
import com.manage.nhakhoa.features.dashboard.repository.ShiftAssignmentRow;
import com.manage.nhakhoa.features.dashboard.repository.ShiftRow;
import com.manage.nhakhoa.features.dashboard.repository.StockItemRow;
import com.manage.nhakhoa.features.dashboard.repository.StockLotRow;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Bản giả lập trong bộ nhớ, mô phỏng đúng ngữ nghĩa các truy vấn JPQL của JpaDashboardReadRepository
 * (lọc trạng thái, khoảng nửa mở, aggregate riêng, COUNT DISTINCT). Có thể ép lỗi từng phương thức.
 * Không chứa dữ liệu cá nhân thật.
 */
class InMemoryDashboardReadRepository implements DashboardReadRepository {

    record Appointment(LocalDateTime time, TrangThaiLichHen status) { }
    record Exam(LocalDateTime receivedAt, TrangThaiPhieuKham status) { }
    record Invoice(String id, TrangThaiHoaDon status) { }
    record InvoiceLine(String invoiceId, int quantity, BigDecimal unitPrice) { }
    record Payment(String invoiceId, LocalDateTime time, BigDecimal amount, PhuongThucThanhToan method,
                   TrangThaiThanhToan status) { }
    record Assignment(String employeeId, VaiTro role, TrangThaiNhanVien employeeStatus, String shiftCode,
                      LocalDate workDate, TrangThaiPhanCong status) { }
    record Lot(String lotId, String drugId, String supplyId, LocalDate expiry, int quantity) { }
    record Catalog(String id, boolean active, boolean sample, int minimum) {
        Catalog(boolean active, boolean sample) { this(sample ? "SAMPLE" : "REAL", active, sample, 1); }
    }
    record Check(String checkId, LocalDateTime time, KetQuaKiemTra result) { }
    record CheckItem(String checkId, String itemId, Boolean ok, LocalDate expiry, Integer actual) {
        CheckItem(String checkId, boolean ok, LocalDate expiry) { this(checkId, "REAL", ok, expiry, 1); }
    }
    record PlanItem(String planId, TrangThaiKeHoach planStatus, TrangThaiChiTietDieuTri itemStatus, LocalDate dueDate) { }

    final List<Appointment> appointments = new ArrayList<>();
    final List<Exam> exams = new ArrayList<>();
    final Map<String, Invoice> invoices = new LinkedHashMap<>();
    final List<InvoiceLine> invoiceLines = new ArrayList<>();
    final List<Payment> payments = new ArrayList<>();
    final List<TrangThaiDuyet> cancellationRequests = new ArrayList<>();
    final List<TrangThaiDuyet> stockAdjustments = new ArrayList<>();
    final Map<String, String> parameters = new HashMap<>();
    final List<ShiftRow> shifts = new ArrayList<>();
    final List<Assignment> assignments = new ArrayList<>();
    final List<Lot> lots = new ArrayList<>();
    final List<StockItemRow> items = new ArrayList<>();
    final List<Catalog> catalog = new ArrayList<>();
    final List<Check> checks = new ArrayList<>();
    final List<CheckItem> checkItems = new ArrayList<>();
    final List<PlanItem> planItems = new ArrayList<>();

    final Set<String> failingMethods = new HashSet<>();
    final List<String> calls = new ArrayList<>();
    LocalDateTime lastAppointmentStart;
    LocalDateTime lastAppointmentEnd;

    private void enter(String method) {
        calls.add(method);
        if (failingMethods.contains(method)) {
            throw new IllegalStateException("SELECT secret_column FROM BenhNhan WHERE soDienThoai='0900000000'");
        }
    }

    private static boolean inRange(LocalDateTime t, LocalDateTime start, LocalDateTime end) {
        return !t.isBefore(start) && t.isBefore(end);
    }

    @Override
    public Map<TrangThaiLichHen, Long> countAppointmentsByStatus(LocalDateTime start, LocalDateTime end) {
        enter("countAppointmentsByStatus");
        lastAppointmentStart = start;
        lastAppointmentEnd = end;
        Map<TrangThaiLichHen, Long> result = new EnumMap<>(TrangThaiLichHen.class);
        appointments.stream().filter(a -> inRange(a.time(), start, end))
                .forEach(a -> result.merge(a.status(), 1L, Long::sum));
        return result;
    }

    @Override
    public Map<TrangThaiPhieuKham, Long> countExamsByStatus(LocalDateTime start, LocalDateTime end) {
        enter("countExamsByStatus");
        Map<TrangThaiPhieuKham, Long> result = new EnumMap<>(TrangThaiPhieuKham.class);
        exams.stream().filter(e -> inRange(e.receivedAt(), start, end))
                .forEach(e -> result.merge(e.status(), 1L, Long::sum));
        return result;
    }

    @Override
    public Map<PhuongThucThanhToan, BigDecimal> sumSuccessfulPaymentsByMethod(LocalDateTime start, LocalDateTime end) {
        enter("sumSuccessfulPaymentsByMethod");
        Map<PhuongThucThanhToan, BigDecimal> result = new EnumMap<>(PhuongThucThanhToan.class);
        payments.stream()
                .filter(p -> p.status() == TrangThaiThanhToan.THANH_CONG && inRange(p.time(), start, end))
                .forEach(p -> result.merge(p.method(), p.amount(), BigDecimal::add));
        return result;
    }

    private boolean active(String invoiceId) {
        Invoice invoice = invoices.get(invoiceId);
        return invoice != null && invoice.status() != TrangThaiHoaDon.DA_HUY;
    }

    @Override
    public List<InvoiceAmountRow> sumBilledAmountPerActiveInvoice() {
        enter("sumBilledAmountPerActiveInvoice");
        Map<String, BigDecimal> sums = new LinkedHashMap<>();
        invoiceLines.stream().filter(l -> active(l.invoiceId()))
                .forEach(l -> sums.merge(l.invoiceId(), l.unitPrice().multiply(BigDecimal.valueOf(l.quantity())),
                        BigDecimal::add));
        return sums.entrySet().stream().map(e -> new InvoiceAmountRow(e.getKey(), e.getValue())).toList();
    }

    @Override
    public List<InvoiceAmountRow> sumSuccessfulPaymentsPerActiveInvoice() {
        enter("sumSuccessfulPaymentsPerActiveInvoice");
        Map<String, BigDecimal> sums = new LinkedHashMap<>();
        payments.stream().filter(p -> p.status() == TrangThaiThanhToan.THANH_CONG && active(p.invoiceId()))
                .forEach(p -> sums.merge(p.invoiceId(), p.amount(), BigDecimal::add));
        return sums.entrySet().stream().map(e -> new InvoiceAmountRow(e.getKey(), e.getValue())).toList();
    }

    @Override
    public long countSuccessfulPaymentsOnCancelledInvoices() {
        enter("countSuccessfulPaymentsOnCancelledInvoices");
        return payments.stream().filter(p -> p.status() == TrangThaiThanhToan.THANH_CONG
                && invoices.containsKey(p.invoiceId())
                && invoices.get(p.invoiceId()).status() == TrangThaiHoaDon.DA_HUY).count();
    }

    @Override
    public long countPendingInvoiceCancellationRequests() {
        enter("countPendingInvoiceCancellationRequests");
        return cancellationRequests.stream().filter(s -> s == TrangThaiDuyet.CHO_DUYET).count();
    }

    @Override
    public long countPendingStockAdjustments() {
        enter("countPendingStockAdjustments");
        return stockAdjustments.stream().filter(s -> s == TrangThaiDuyet.CHO_DUYET).count();
    }

    @Override
    public Optional<String> findSystemParameter(String key) {
        enter("findSystemParameter");
        return Optional.ofNullable(parameters.get(key));
    }

    @Override
    public List<ShiftRow> findShifts() {
        enter("findShifts");
        return List.copyOf(shifts);
    }

    @Override
    public List<ShiftAssignmentRow> countAssignedStaffByShiftAndRole(LocalDate workDate) {
        enter("countAssignedStaffByShiftAndRole");
        Set<TrangThaiPhanCong> counted = Set.of(TrangThaiPhanCong.CHO_HIEU_LUC, TrangThaiPhanCong.DANG_THUC_HIEN,
                TrangThaiPhanCong.HOAN_THANH);
        Set<VaiTro> roles = Set.of(VaiTro.BS, VaiTro.YT, VaiTro.LT);
        Map<String, Map<VaiTro, Set<String>>> distinct = new LinkedHashMap<>();
        assignments.stream()
                .filter(a -> a.workDate().equals(workDate) && counted.contains(a.status())
                        && a.employeeStatus() == TrangThaiNhanVien.DANG_LAM_VIEC && roles.contains(a.role()))
                .forEach(a -> distinct.computeIfAbsent(a.shiftCode(), k -> new EnumMap<>(VaiTro.class))
                        .computeIfAbsent(a.role(), k -> new HashSet<>()).add(a.employeeId()));
        List<ShiftAssignmentRow> result = new ArrayList<>();
        distinct.forEach((shift, byRole) -> byRole.forEach((role, ids) ->
                result.add(new ShiftAssignmentRow(shift, role, ids.size()))));
        return result;
    }

    @Override
    public List<StockLotRow> findNonEmptyStockLots() {
        enter("findNonEmptyStockLots");
        return lots.stream().filter(l -> l.quantity() != 0)
                .sorted(Comparator.comparing(Lot::lotId))
                .map(l -> new StockLotRow(l.lotId(), l.drugId(), l.supplyId(), l.expiry(), l.quantity()))
                .toList();
    }

    @Override
    public List<StockItemRow> findStockItems() {
        enter("findStockItems");
        return List.copyOf(items);
    }

    @Override
    public long countActiveRealEmergencyCatalogItems() {
        enter("countActiveRealEmergencyCatalogItems");
        return catalog.stream().filter(c -> c.active() && !c.sample()).count();
    }

    @Override
    public Optional<EmergencyCheckRow> findLatestEmergencyCheckAtOrBefore(LocalDateTime atOrBefore) {
        enter("findLatestEmergencyCheckAtOrBefore");
        return checks.stream().filter(c -> !c.time().isAfter(atOrBefore))
                .max(Comparator.comparing(Check::time).thenComparing(Check::checkId))
                .map(c -> new EmergencyCheckRow(c.checkId(), c.time(), c.result()));
    }

    @Override
    public long countFailingEmergencyCheckItems(String checkId, LocalDate safeUntilInclusive) {
        enter("countFailingEmergencyCheckItems");
        return checkItems.stream().filter(i -> i.checkId().equals(checkId))
                .filter(i -> catalog.stream().anyMatch(c -> c.active() && !c.sample() && c.id().equals(i.itemId())))
                .filter(i -> Boolean.FALSE.equals(i.ok()) || (i.expiry() != null && !i.expiry().isAfter(safeUntilInclusive)))
                .count();
    }

    @Override
    public List<EmergencyCheckItemRow> findEmergencyCheckCoverage(String checkId) {
        enter("findEmergencyCheckCoverage");
        return catalog.stream().filter(c -> c.active() && !c.sample()).map(c -> {
            CheckItem detail = checkItems.stream().filter(i -> i.checkId().equals(checkId) && i.itemId().equals(c.id())).findFirst().orElse(null);
            return new EmergencyCheckItemRow(c.id(), detail == null ? null : detail.itemId(), detail == null ? null : detail.ok(),
                    detail == null ? null : detail.expiry(), detail == null ? null : detail.actual(), c.minimum());
        }).toList();
    }

    @Override
    public long countPlansWithPendingItemsDueOnOrBefore(LocalDate dueOnOrBefore) {
        enter("countPlansWithPendingItemsDueOnOrBefore");
        return pendingPlans().stream().filter(p -> !p.dueDate().isAfter(dueOnOrBefore))
                .map(PlanItem::planId).collect(Collectors.toSet()).size();
    }

    @Override
    public long countPlansWithPendingItemsDueBefore(LocalDate dueBefore) {
        enter("countPlansWithPendingItemsDueBefore");
        return pendingPlans().stream().filter(p -> p.dueDate().isBefore(dueBefore))
                .map(PlanItem::planId).collect(Collectors.toSet()).size();
    }

    private List<PlanItem> pendingPlans() {
        return planItems.stream().filter(p -> p.planStatus() == TrangThaiKeHoach.DANG_THUC_HIEN
                && p.itemStatus() == TrangThaiChiTietDieuTri.CHUA_THUC_HIEN).toList();
    }
}
