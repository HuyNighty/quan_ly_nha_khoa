package com.manage.nhakhoa.features.dashboard.repository;

import com.manage.nhakhoa.features.capcuu.enums.KetQuaKiemTra;
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
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Truy vấn JPQL chỉ đọc trên các Entity hiện hành (không sửa Entity/schema).
 * Mỗi chỉ số là một aggregate riêng để tránh nhân dòng khi JOIN 1-N.
 */
@Repository
@Transactional(readOnly = true)
public class JpaDashboardReadRepository implements DashboardReadRepository {

    private static final List<TrangThaiPhanCong> COUNTED_ASSIGNMENT_STATUSES = List.of(
            TrangThaiPhanCong.CHO_HIEU_LUC, TrangThaiPhanCong.DANG_THUC_HIEN, TrangThaiPhanCong.HOAN_THANH);
    private static final List<VaiTro> STAFFED_ROLES = List.of(VaiTro.BS, VaiTro.YT, VaiTro.LT);

    @PersistenceContext
    private EntityManager entityManager;

    public JpaDashboardReadRepository() {
    }

    JpaDashboardReadRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public Map<TrangThaiLichHen, Long> countAppointmentsByStatus(LocalDateTime startInclusive, LocalDateTime endExclusive) {
        List<Object[]> rows = entityManager.createQuery(
                        "select lh.trangThai, count(lh) from LichHen lh"
                                + " where lh.thoiGianHen >= :start and lh.thoiGianHen < :end"
                                + " group by lh.trangThai", Object[].class)
                .setParameter("start", startInclusive)
                .setParameter("end", endExclusive)
                .getResultList();
        Map<TrangThaiLichHen, Long> result = new EnumMap<>(TrangThaiLichHen.class);
        for (Object[] row : rows) {
            result.put((TrangThaiLichHen) row[0], toLong(row[1]));
        }
        return result;
    }

    @Override
    public Map<TrangThaiPhieuKham, Long> countExamsByStatus(LocalDateTime startInclusive, LocalDateTime endExclusive) {
        List<Object[]> rows = entityManager.createQuery(
                        "select pk.trangThai, count(pk) from PhieuKham pk"
                                + " where pk.thoiGianTiepNhan >= :start and pk.thoiGianTiepNhan < :end"
                                + " group by pk.trangThai", Object[].class)
                .setParameter("start", startInclusive)
                .setParameter("end", endExclusive)
                .getResultList();
        Map<TrangThaiPhieuKham, Long> result = new EnumMap<>(TrangThaiPhieuKham.class);
        for (Object[] row : rows) {
            result.put((TrangThaiPhieuKham) row[0], toLong(row[1]));
        }
        return result;
    }

    @Override
    public Map<PhuongThucThanhToan, BigDecimal> sumSuccessfulPaymentsByMethod(LocalDateTime startInclusive, LocalDateTime endExclusive) {
        List<Object[]> rows = entityManager.createQuery(
                        "select tt.phuongThuc, sum(tt.soTien) from ThanhToan tt"
                                + " where tt.trangThai = :success"
                                + " and tt.thoiGian >= :start and tt.thoiGian < :end"
                                + " group by tt.phuongThuc", Object[].class)
                .setParameter("success", TrangThaiThanhToan.THANH_CONG)
                .setParameter("start", startInclusive)
                .setParameter("end", endExclusive)
                .getResultList();
        Map<PhuongThucThanhToan, BigDecimal> result = new EnumMap<>(PhuongThucThanhToan.class);
        for (Object[] row : rows) {
            result.put((PhuongThucThanhToan) row[0], toBigDecimal(row[1]));
        }
        return result;
    }

    @Override
    public List<InvoiceAmountRow> sumBilledAmountPerActiveInvoice() {
        List<Object[]> rows = entityManager.createQuery(
                        "select hd.maHoaDon, sum(ct.donGiaApDung * ct.soLuong)"
                                + " from ChiTietHoaDon ct join ct.hoaDon hd"
                                + " where hd.trangThai <> :cancelled"
                                + " group by hd.maHoaDon", Object[].class)
                .setParameter("cancelled", TrangThaiHoaDon.DA_HUY)
                .getResultList();
        return toInvoiceRows(rows);
    }

    @Override
    public List<InvoiceAmountRow> sumSuccessfulPaymentsPerActiveInvoice() {
        List<Object[]> rows = entityManager.createQuery(
                        "select hd.maHoaDon, sum(tt.soTien)"
                                + " from ThanhToan tt join tt.hoaDon hd"
                                + " where hd.trangThai <> :cancelled and tt.trangThai = :success"
                                + " group by hd.maHoaDon", Object[].class)
                .setParameter("cancelled", TrangThaiHoaDon.DA_HUY)
                .setParameter("success", TrangThaiThanhToan.THANH_CONG)
                .getResultList();
        return toInvoiceRows(rows);
    }

    @Override
    public long countSuccessfulPaymentsOnCancelledInvoices() {
        return entityManager.createQuery(
                        "select count(tt) from ThanhToan tt join tt.hoaDon hd"
                                + " where hd.trangThai = :cancelled and tt.trangThai = :success", Long.class)
                .setParameter("cancelled", TrangThaiHoaDon.DA_HUY)
                .setParameter("success", TrangThaiThanhToan.THANH_CONG)
                .getSingleResult();
    }

    @Override
    public long countPendingInvoiceCancellationRequests() {
        return entityManager.createQuery(
                        "select count(yc) from YeuCauHuyHoaDon yc where yc.trangThai = :pending", Long.class)
                .setParameter("pending", TrangThaiDuyet.CHO_DUYET)
                .getSingleResult();
    }

    @Override
    public long countPendingStockAdjustments() {
        return entityManager.createQuery(
                        "select count(p) from PhieuDieuChinhKho p where p.trangThai = :pending", Long.class)
                .setParameter("pending", TrangThaiDuyet.CHO_DUYET)
                .getSingleResult();
    }

    @Override
    public Optional<String> findSystemParameter(String key) {
        return entityManager.createQuery(
                        "select ts.giaTri from ThamSoHeThong ts where ts.ten = :key", String.class)
                .setParameter("key", key)
                .getResultList()
                .stream()
                .findFirst();
    }

    @Override
    public List<ShiftRow> findShifts() {
        List<Object[]> rows = entityManager.createQuery(
                        "select ca.maCa, ca.tenCa, ca.gioBatDau, ca.gioKetThuc, ca.soBacSi, ca.soYTa, ca.soLeTan"
                                + " from CaLamViec ca order by ca.gioBatDau, ca.maCa", Object[].class)
                .getResultList();
        List<ShiftRow> result = new ArrayList<>(rows.size());
        for (Object[] row : rows) {
            result.add(new ShiftRow((String) row[0], (String) row[1], (LocalTime) row[2], (LocalTime) row[3],
                    (Integer) row[4], (Integer) row[5], (Integer) row[6]));
        }
        return result;
    }

    @Override
    public List<ShiftAssignmentRow> countAssignedStaffByShiftAndRole(LocalDate workDate) {
        List<Object[]> rows = entityManager.createQuery(
                        "select ca.maCa, nv.chucDanh, count(distinct nv.maNhanVien)"
                                + " from PhanCong pc join pc.caLamViec ca join pc.nhanVien nv"
                                + " where pc.ngayLam = :workDate"
                                + " and pc.trangThai in :assignmentStatuses"
                                + " and nv.trangThai = :working"
                                + " and nv.chucDanh in :roles"
                                + " group by ca.maCa, nv.chucDanh", Object[].class)
                .setParameter("workDate", workDate)
                .setParameter("assignmentStatuses", COUNTED_ASSIGNMENT_STATUSES)
                .setParameter("working", TrangThaiNhanVien.DANG_LAM_VIEC)
                .setParameter("roles", STAFFED_ROLES)
                .getResultList();
        List<ShiftAssignmentRow> result = new ArrayList<>(rows.size());
        for (Object[] row : rows) {
            result.add(new ShiftAssignmentRow((String) row[0], (VaiTro) row[1], toLong(row[2])));
        }
        return result;
    }

    @Override
    public List<StockLotRow> findNonEmptyStockLots() {
        List<Object[]> rows = entityManager.createQuery(
                        "select lo.maLo, t.maThuoc, v.maVatTu, lo.hanSuDung, lo.soLuongTon"
                                + " from LoHang lo left join lo.thuoc t left join lo.vatTu v"
                                + " where lo.soLuongTon <> 0 order by lo.maLo", Object[].class)
                .getResultList();
        List<StockLotRow> result = new ArrayList<>(rows.size());
        for (Object[] row : rows) {
            result.add(new StockLotRow((String) row[0], (String) row[1], (String) row[2],
                    (LocalDate) row[3], ((Number) row[4]).intValue()));
        }
        return result;
    }

    @Override
    public List<StockItemRow> findStockItems() {
        List<StockItemRow> result = new ArrayList<>();
        for (Object[] row : entityManager.createQuery(
                "select t.maThuoc, t.dinhMucToiThieu from Thuoc t order by t.maThuoc", Object[].class).getResultList()) {
            result.add(new StockItemRow(StockItemType.THUOC, (String) row[0], toInt(row[1])));
        }
        for (Object[] row : entityManager.createQuery(
                "select v.maVatTu, v.dinhMucToiThieu from VatTu v order by v.maVatTu", Object[].class).getResultList()) {
            result.add(new StockItemRow(StockItemType.VAT_TU, (String) row[0], toInt(row[1])));
        }
        return result;
    }

    @Override
    public long countActiveRealEmergencyCatalogItems() {
        return entityManager.createQuery(
                        "select count(dm) from DanhMucCapCuu dm"
                                + " where dm.dangApDung = true and dm.laDuLieuMau = false", Long.class)
                .getSingleResult();
    }

    @Override
    public Optional<EmergencyCheckRow> findLatestEmergencyCheckAtOrBefore(LocalDateTime atOrBefore) {
        return entityManager.createQuery(
                        "select kt.maKiemTra, kt.thoiGian, kt.ketQua from KiemTraCapCuu kt"
                                + " where kt.thoiGian <= :at order by kt.thoiGian desc, kt.maKiemTra desc", Object[].class)
                .setParameter("at", atOrBefore)
                .setMaxResults(1)
                .getResultList()
                .stream()
                .findFirst()
                .map(row -> new EmergencyCheckRow((String) row[0], (LocalDateTime) row[1], (KetQuaKiemTra) row[2]));
    }

    @Override
    public long countFailingEmergencyCheckItems(String checkId, LocalDate safeUntilInclusive) {
        return entityManager.createQuery(
                        "select count(ct) from ChiTietKiemTraCapCuu ct"
                                + " where ct.kiemTra.maKiemTra = :checkId"
                                + " and ct.danhMucCapCuu.dangApDung = true and ct.danhMucCapCuu.laDuLieuMau = false"
                                + " and (ct.dat = false or (ct.hanSuDung is not null and ct.hanSuDung <= :safeUntil))",
                        Long.class)
                .setParameter("checkId", checkId)
                .setParameter("safeUntil", safeUntilInclusive)
                .getSingleResult();
    }

    @Override
    public List<EmergencyCheckItemRow> findEmergencyCheckCoverage(String checkId) {
        List<Object[]> rows = entityManager.createQuery(
                        "select dm.maMuc, ct.id.maMuc, ct.dat, ct.hanSuDung, ct.soLuongThucTe, dm.soLuongToiThieu"
                                + " from DanhMucCapCuu dm left join ChiTietKiemTraCapCuu ct"
                                + " on ct.danhMucCapCuu = dm and ct.kiemTra.maKiemTra = :checkId"
                                + " where dm.dangApDung = true and dm.laDuLieuMau = false order by dm.maMuc",
                        Object[].class)
                .setParameter("checkId", checkId)
                .getResultList();
        return rows.stream().map(row -> new EmergencyCheckItemRow((String) row[0], (String) row[1],
                (Boolean) row[2], (LocalDate) row[3], (Integer) row[4], (Integer) row[5])).toList();
    }

    @Override
    public long countPlansWithPendingItemsDueOnOrBefore(LocalDate dueOnOrBefore) {
        return entityManager.createQuery(
                        "select count(distinct kh.maKeHoach) from ChiTietDieuTri ct join ct.keHoach kh"
                                + " where kh.trangThai = :planActive and ct.trangThai = :itemPending"
                                + " and ct.ngayDuKien <= :due", Long.class)
                .setParameter("planActive", TrangThaiKeHoach.DANG_THUC_HIEN)
                .setParameter("itemPending", TrangThaiChiTietDieuTri.CHUA_THUC_HIEN)
                .setParameter("due", dueOnOrBefore)
                .getSingleResult();
    }

    @Override
    public long countPlansWithPendingItemsDueBefore(LocalDate dueBefore) {
        return entityManager.createQuery(
                        "select count(distinct kh.maKeHoach) from ChiTietDieuTri ct join ct.keHoach kh"
                                + " where kh.trangThai = :planActive and ct.trangThai = :itemPending"
                                + " and ct.ngayDuKien < :due", Long.class)
                .setParameter("planActive", TrangThaiKeHoach.DANG_THUC_HIEN)
                .setParameter("itemPending", TrangThaiChiTietDieuTri.CHUA_THUC_HIEN)
                .setParameter("due", dueBefore)
                .getSingleResult();
    }

    private static List<InvoiceAmountRow> toInvoiceRows(List<Object[]> rows) {
        List<InvoiceAmountRow> result = new ArrayList<>(rows.size());
        for (Object[] row : rows) {
            result.add(new InvoiceAmountRow((String) row[0], toBigDecimal(row[1])));
        }
        return result;
    }

    private static long toLong(Object value) {
        return value == null ? 0L : ((Number) value).longValue();
    }

    private static int toInt(Object value) {
        return value == null ? 0 : ((Number) value).intValue();
    }

    private static BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Long || value instanceof Integer) {
            return BigDecimal.valueOf(((Number) value).longValue());
        }
        return new BigDecimal(value.toString());
    }
}
