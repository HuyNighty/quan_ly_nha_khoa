package com.manage.nhakhoa.features.dashboard.repository;

import com.manage.nhakhoa.features.hoadon.enums.PhuongThucThanhToan;
import com.manage.nhakhoa.features.hoadon.enums.TrangThaiHoaDon;
import com.manage.nhakhoa.features.hoadon.enums.TrangThaiThanhToan;
import com.manage.nhakhoa.features.khamchuabenh.enums.TrangThaiChiTietDieuTri;
import com.manage.nhakhoa.features.khamchuabenh.enums.TrangThaiKeHoach;
import com.manage.nhakhoa.features.lichhen.enums.TrangThaiLichHen;
import com.manage.nhakhoa.features.nhansu.enums.TrangThaiNhanVien;
import com.manage.nhakhoa.features.nhansu.enums.TrangThaiPhanCong;
import com.manage.nhakhoa.shared.enums.VaiTro;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Kiểm tra cấu trúc JPQL (bộ lọc, toán tử biên, tham số enum) và ánh xạ kết quả, bằng EntityManager giả qua Proxy.
 * KHÔNG thay thế integration test trên SQL Server/Hibernate thật (chưa có môi trường test DB).
 */
class JpaDashboardReadRepositoryTest {

    private final List<String> queries = new ArrayList<>();
    private final List<Map<String, Object>> parameters = new ArrayList<>();
    private final Deque<List<?>> cannedResults = new ArrayDeque<>();
    private JpaDashboardReadRepository repository;

    @BeforeEach
    void setUp() {
        EntityManager em = (EntityManager) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{EntityManager.class}, (proxy, method, args) -> {
                    if (!method.getName().equals("createQuery")) {
                        throw new UnsupportedOperationException("Repository chỉ được tạo truy vấn đọc: " + method.getName());
                    }
                    queries.add((String) args[0]);
                    Map<String, Object> params = new HashMap<>();
                    parameters.add(params);
                    List<?> result = cannedResults.isEmpty() ? List.of() : cannedResults.poll();
                    return typedQuery(params, result);
                });
        repository = new JpaDashboardReadRepository(em);
    }

    private TypedQuery<?> typedQuery(Map<String, Object> params, List<?> result) {
        return (TypedQuery<?>) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{TypedQuery.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "setParameter" -> {
                        params.put((String) args[0], args[1]);
                        yield proxy;
                    }
                    case "setMaxResults" -> proxy;
                    case "getResultList" -> result;
                    case "getSingleResult" -> result.isEmpty() ? 0L : result.get(0);
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

    @Test
    void appointmentQueryUsesHalfOpenRangeOnThoiGianHen() {
        cannedResults.add(List.<Object[]>of(new Object[]{TrangThaiLichHen.DA_HUY, 3L}));
        LocalDateTime start = LocalDate.of(2026, 10, 19).atStartOfDay();
        Map<TrangThaiLichHen, Long> result = repository.countAppointmentsByStatus(start, start.plusDays(1));

        assertEquals(3L, result.get(TrangThaiLichHen.DA_HUY));
        String q = queries.get(0);
        assertTrue(q.contains("lh.thoiGianHen >= :start"));
        assertTrue(q.contains("lh.thoiGianHen < :end"));
        assertFalse(q.contains("<= :end"));
        assertFalse(q.contains("trangThai <>"), "không được loại trạng thái nào khỏi tổng lịch hẹn");
        assertEquals(start, parameters.get(0).get("start"));
    }

    @Test
    void paymentQueriesFilterSuccessfulAndAggregateSeparately() {
        cannedResults.add(List.<Object[]>of(new Object[]{PhuongThucThanhToan.TIEN_MAT, new BigDecimal("1000")}));
        LocalDateTime start = LocalDate.of(2026, 10, 19).atStartOfDay();
        assertEquals(new BigDecimal("1000"),
                repository.sumSuccessfulPaymentsByMethod(start, start.plusDays(1)).get(PhuongThucThanhToan.TIEN_MAT));
        assertEquals(TrangThaiThanhToan.THANH_CONG, parameters.get(0).get("success"));
        assertTrue(queries.get(0).contains("tt.thoiGian >= :start and tt.thoiGian < :end"));

        cannedResults.add(List.<Object[]>of(new Object[]{"HD1", new BigDecimal("500000")}));
        cannedResults.add(List.<Object[]>of(new Object[]{"HD1", 250000L}));
        List<InvoiceAmountRow> billed = repository.sumBilledAmountPerActiveInvoice();
        List<InvoiceAmountRow> paid = repository.sumSuccessfulPaymentsPerActiveInvoice();

        assertEquals(new InvoiceAmountRow("HD1", new BigDecimal("500000")), billed.get(0));
        assertEquals(new InvoiceAmountRow("HD1", new BigDecimal("250000")), paid.get(0));
        String billedQuery = queries.get(1);
        String paidQuery = queries.get(2);
        assertTrue(billedQuery.contains("from ChiTietHoaDon") && !billedQuery.contains("ThanhToan"),
                "tổng chi tiết không được join thanh toán (tránh nhân dòng)");
        assertTrue(paidQuery.contains("from ThanhToan") && !paidQuery.contains("ChiTietHoaDon"));
        assertEquals(TrangThaiHoaDon.DA_HUY, parameters.get(1).get("cancelled"));
        assertEquals(TrangThaiHoaDon.DA_HUY, parameters.get(2).get("cancelled"));
        assertEquals(TrangThaiThanhToan.THANH_CONG, parameters.get(2).get("success"));
    }

    @Test
    void staffingQueryCountsDistinctEmployeesWithBaStatuses() {
        cannedResults.add(List.<Object[]>of(new Object[]{"CA01", VaiTro.BS, 2L}));
        List<ShiftAssignmentRow> rows = repository.countAssignedStaffByShiftAndRole(LocalDate.of(2026, 10, 19));

        assertEquals(new ShiftAssignmentRow("CA01", VaiTro.BS, 2L), rows.get(0));
        assertTrue(queries.get(0).contains("count(distinct nv.maNhanVien)"));
        Map<String, Object> p = parameters.get(0);
        assertEquals(List.of(TrangThaiPhanCong.CHO_HIEU_LUC, TrangThaiPhanCong.DANG_THUC_HIEN, TrangThaiPhanCong.HOAN_THANH),
                p.get("assignmentStatuses"));
        assertEquals(TrangThaiNhanVien.DANG_LAM_VIEC, p.get("working"));
        assertEquals(List.of(VaiTro.BS, VaiTro.YT, VaiTro.LT), p.get("roles"));
    }

    @Test
    void treatmentQueriesCountDistinctPlans() {
        cannedResults.add(List.of(4L));
        cannedResults.add(List.of(1L));
        LocalDate d = LocalDate.of(2026, 10, 19);
        assertEquals(4L, repository.countPlansWithPendingItemsDueOnOrBefore(d.plusDays(14)));
        assertEquals(1L, repository.countPlansWithPendingItemsDueBefore(d));

        assertTrue(queries.get(0).contains("count(distinct kh.maKeHoach)"));
        assertTrue(queries.get(0).contains("ct.ngayDuKien <= :due"));
        assertTrue(queries.get(1).contains("ct.ngayDuKien < :due"));
        assertEquals(TrangThaiKeHoach.DANG_THUC_HIEN, parameters.get(0).get("planActive"));
        assertEquals(TrangThaiChiTietDieuTri.CHUA_THUC_HIEN, parameters.get(0).get("itemPending"));
    }

    @Test
    void stockLotQueryKeepsNegativeQuantitiesForAnomalyDetection() {
        cannedResults.add(List.<Object[]>of(new Object[]{"L1", "T1", null, null, -2}));
        List<StockLotRow> rows = repository.findNonEmptyStockLots();
        assertEquals(new StockLotRow("L1", "T1", null, null, -2), rows.get(0));
        assertTrue(queries.get(0).contains("lo.soLuongTon <> 0"));
    }

    @Test
    void allQueriesAreSelects() {
        LocalDateTime t = LocalDateTime.of(2026, 10, 19, 0, 0);
        LocalDate d = t.toLocalDate();
        repository.countAppointmentsByStatus(t, t.plusDays(1));
        repository.countExamsByStatus(t, t.plusDays(1));
        repository.sumSuccessfulPaymentsByMethod(t, t.plusDays(1));
        repository.sumBilledAmountPerActiveInvoice();
        repository.sumSuccessfulPaymentsPerActiveInvoice();
        repository.countSuccessfulPaymentsOnCancelledInvoices();
        repository.countPendingInvoiceCancellationRequests();
        repository.countPendingStockAdjustments();
        repository.findSystemParameter("SO_NGAY_CANH_BAO_HSD");
        repository.findShifts();
        repository.countAssignedStaffByShiftAndRole(d);
        repository.findNonEmptyStockLots();
        repository.findStockItems();
        repository.countActiveRealEmergencyCatalogItems();
        repository.findLatestEmergencyCheckAtOrBefore(t);
        repository.countFailingEmergencyCheckItems("KT1", d);
        repository.findEmergencyCheckCoverage("KT1");
        repository.countPlansWithPendingItemsDueOnOrBefore(d);
        repository.countPlansWithPendingItemsDueBefore(d);

        assertEquals(20, queries.size());
        for (String q : queries) {
            String lower = q.toLowerCase();
            assertTrue(lower.startsWith("select "), q);
            assertFalse(lower.contains("update ") || lower.contains("delete ") || lower.contains("insert "), q);
        }
    }

    @Test
    void emergencyCoverageRetainsMissingOfficialItemsAndExcludesSampleItems() {
        cannedResults.add(List.<Object[]>of(new Object[]{"REAL", null, null, null, null, 2}));
        List<EmergencyCheckItemRow> rows = repository.findEmergencyCheckCoverage("KT1");
        assertEquals(new EmergencyCheckItemRow("REAL", null, null, null, null, 2), rows.getFirst());
        assertTrue(queries.getFirst().contains("left join ChiTietKiemTraCapCuu"));
        assertTrue(queries.getFirst().contains("dm.dangApDung = true and dm.laDuLieuMau = false"));
        assertTrue(queries.getFirst().contains("ct.kiemTra.maKiemTra = :checkId"));
        assertEquals("KT1", parameters.getFirst().get("checkId"));
    }
}
