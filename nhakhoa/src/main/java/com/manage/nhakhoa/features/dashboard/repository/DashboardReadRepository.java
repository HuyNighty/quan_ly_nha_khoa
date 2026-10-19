package com.manage.nhakhoa.features.dashboard.repository;

import com.manage.nhakhoa.features.hoadon.enums.PhuongThucThanhToan;
import com.manage.nhakhoa.features.khamchuabenh.enums.TrangThaiPhieuKham;
import com.manage.nhakhoa.features.lichhen.enums.TrangThaiLichHen;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Cổng đọc dữ liệu cho Dashboard Admin. CHỈ ĐỌC: không có phương thức ghi.
 * Mọi khoảng thời gian là nửa mở [startInclusive, endExclusive) theo giờ địa phương phòng khám.
 * Implementation gốc: {@link JpaDashboardReadRepository}; unit test dùng bản giả lập trong bộ nhớ.
 */
public interface DashboardReadRepository {

    /** Số bản ghi LichHen theo trạng thái với thoiGianHen trong khoảng. Trạng thái không có bản ghi có thể vắng mặt. */
    Map<TrangThaiLichHen, Long> countAppointmentsByStatus(LocalDateTime startInclusive, LocalDateTime endExclusive);

    /** Số PhieuKham theo trạng thái với thoiGianTiepNhan trong khoảng. */
    Map<TrangThaiPhieuKham, Long> countExamsByStatus(LocalDateTime startInclusive, LocalDateTime endExclusive);

    /** Tổng ThanhToan.soTien THANH_CONG theo phương thức với thoiGian trong khoảng. */
    Map<PhuongThucThanhToan, BigDecimal> sumSuccessfulPaymentsByMethod(LocalDateTime startInclusive, LocalDateTime endExclusive);

    /** SUM(soLuong * donGiaApDung) theo từng hóa đơn khác DA_HUY (aggregate riêng, không join thanh toán). */
    List<InvoiceAmountRow> sumBilledAmountPerActiveInvoice();

    /** SUM(soTien) THANH_CONG theo từng hóa đơn khác DA_HUY (aggregate riêng, không join chi tiết). */
    List<InvoiceAmountRow> sumSuccessfulPaymentsPerActiveInvoice();

    /** Số giao dịch THANH_CONG thuộc hóa đơn DA_HUY — anomaly theo BA V1 Q06. */
    long countSuccessfulPaymentsOnCancelledInvoices();

    /** Số YeuCauHuyHoaDon đang CHO_DUYET. */
    long countPendingInvoiceCancellationRequests();

    /** Số PhieuDieuChinhKho đang CHO_DUYET. */
    long countPendingStockAdjustments();

    /** Giá trị thô của ThamSoHeThong theo khóa; rỗng nếu chưa cấu hình. */
    Optional<String> findSystemParameter(String key);

    /** Toàn bộ CaLamViec. */
    List<ShiftRow> findShifts();

    /**
     * COUNT(DISTINCT NhanVien) theo (maCa, chucDanh) cho ngayLam, chỉ PhanCong CHO_HIEU_LUC/DANG_THUC_HIEN/HOAN_THANH,
     * NhanVien DANG_LAM_VIEC và chucDanh BS/YT/LT.
     */
    List<ShiftAssignmentRow> countAssignedStaffByShiftAndRole(LocalDate workDate);

    /** Các LoHang có soLuongTon khác 0 (kể cả âm để phát hiện bất thường). */
    List<StockLotRow> findNonEmptyStockLots();

    /** Toàn bộ Thuoc và VatTu với định mức tối thiểu. */
    List<StockItemRow> findStockItems();

    /** Số DanhMucCapCuu dangApDung = true và laDuLieuMau = false. */
    long countActiveRealEmergencyCatalogItems();

    /** KiemTraCapCuu có thoiGian lớn nhất và <= thời điểm cho trước. */
    Optional<EmergencyCheckRow> findLatestEmergencyCheckAtOrBefore(LocalDateTime atOrBefore);

    /** Số chi tiết của mục chính thức đang áp dụng có dat = false hoặc hanSuDung <= safeUntilInclusive; không chứng minh coverage. */
    long countFailingEmergencyCheckItems(String checkId, LocalDate safeUntilInclusive);

    /** LEFT JOIN từng mục chính thức đang áp dụng với chi tiết của lần kiểm tra; mục thiếu chi tiết vẫn có dòng. */
    List<EmergencyCheckItemRow> findEmergencyCheckCoverage(String checkId);

    /**
     * COUNT(DISTINCT maKeHoach) của KeHoachDieuTri DANG_THUC_HIEN có ít nhất một ChiTietDieuTri CHUA_THUC_HIEN
     * với ngayDuKien <= dueOnOrBefore.
     */
    long countPlansWithPendingItemsDueOnOrBefore(LocalDate dueOnOrBefore);

    /** Như trên nhưng ngayDuKien < dueBefore (quá hạn). */
    long countPlansWithPendingItemsDueBefore(LocalDate dueBefore);
}
