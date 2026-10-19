package com.manage.nhakhoa.desktop.data;

import com.manage.nhakhoa.desktop.model.DashboardViewModel;
import com.manage.nhakhoa.desktop.model.DashboardViewModel.*;
import java.io.IOException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import static com.manage.nhakhoa.desktop.model.DashboardViewModel.Priority.*;

/** Bộ mẫu được chép từ ảnh 01; các số không phải kết quả truy vấn SQL Server. */
public final class DemoDashboardDataProvider implements DashboardDataProvider {
    public static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    public enum Scenario {
        FULL("Đầy đủ dữ liệu"), EMPTY("Không có dữ liệu"), ERROR("Lỗi tải dữ liệu"),
        NO_TASKS("Không có việc chờ"), ZERO_APPOINTMENTS("Không có lịch hẹn");
        private final String label;
        Scenario(String label) { this.label = label; }
        @Override public String toString() { return label; }
    }
    private final Scenario scenario;
    public DemoDashboardDataProvider(Scenario scenario) { this.scenario = scenario; }

    @Override public Optional<DashboardViewModel> load(Clock clock) throws Exception {
        Thread.sleep(280); // Độ trễ minh họa, chỉ chạy trong SwingWorker.
        if (scenario == Scenario.ERROR) { throw new IOException("Lỗi nguồn dữ liệu mẫu"); }
        if (scenario == Scenario.EMPTY) { return Optional.empty(); }
        return Optional.of(sample(clock, scenario));
    }

    public static DashboardViewModel sample(Clock clock, Scenario scenario) {
        LocalDate date = LocalDate.now(clock.withZone(ZONE));
        Map<AppointmentStatus, Long> statuses = scenario == Scenario.ZERO_APPOINTMENTS ? Map.of() : Map.of(
                AppointmentStatus.CHO_XAC_NHAN, 2L, AppointmentStatus.DA_XAC_NHAN, 2L,
                AppointmentStatus.DA_DEN, 1L, AppointmentStatus.HOAN_THANH, 2L,
                AppointmentStatus.VANG_MAT, 1L, AppointmentStatus.DA_HUY, 1L,
                AppointmentStatus.DA_DOI_LICH, 1L);
        List<TaskSummary> tasks = scenario == Scenario.NO_TASKS ? List.of() : List.of(
                new TaskSummary("expired-stock", IMMEDIATE, "Lô hết hạn còn tồn", "Quản lý thuốc & vật tư · xem phiếu liên quan", 1, "KIEM_KE", "Xem phiếu"),
                new TaskSummary("emergency", IMMEDIATE, "Hộp cấp cứu cần bổ sung", "Kiểm tra hộp cấp cứu · dữ liệu mẫu", 1, "CAP_CUU", "Kiểm tra"),
                new TaskSummary("understaffed", IMMEDIATE, "Ca thiếu nhân sự", "Ca chiều, ca tối thiếu y tá", 2, "PHAN_CONG", "Phân công"),
                new TaskSummary("invoice-cancel", PENDING, "Yêu cầu hủy hóa đơn", "Thanh toán & công nợ · chờ xem xét", 2, "DUYET_HUY", "Xem yêu cầu"),
                new TaskSummary("inventory-count", PENDING, "Phiếu kiểm kê / hao hụt", "Quản lý thuốc & vật tư", 1, "KIEM_KE", "Xem phiếu"),
                new TaskSummary("handover", PENDING, "Biên bản bàn giao ca", "Quản lý nhân viên · chờ xác nhận", 2, "BAN_GIAO", "Xem biên bản"),
                new TaskSummary("followup", FOLLOW_UP, "Hạng mục đến / quá hạn tái khám", "Khám & điều trị · theo dõi hạng mục", 6, "TAI_KHAM", "Xem"),
                new TaskSummary("low-stock", FOLLOW_UP, "Mặt hàng dưới định mức", "Quản lý thuốc & vật tư", 2, "THUOC_VAT_TU", "Xem"),
                new TaskSummary("locked-account", FOLLOW_UP, "Tài khoản bị khóa", "Kiểm soát truy cập · chưa hỗ trợ mở khóa", 1, "TAI_KHOAN", "Xem tài khoản"));
        return new DashboardViewModel(date, clock.instant(),
                "Nguồn: bộ mẫu ảnh 01 · áp dụng cho ngày mô phỏng · không kết nối SQL Server",
                new KpiSummary(scenario == Scenario.ZERO_APPOINTMENTS ? 0 : 10,
                        0, 1, 2, 700_000, 0, 0, 3_600_000, 5), statuses, tasks,
                List.of(new ShiftSummary("Ca sáng", 0), new ShiftSummary("Ca chiều", 1), new ShiftSummary("Ca tối", 1)),
                new StockSummary(2, 2, 1, true, date.minusDays(7).atTime(7, 45).atZone(ZONE).toInstant()));
    }
}
