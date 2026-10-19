package com.manage.nhakhoa.desktop.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** DTO bất biến của bản xem thử, không phụ thuộc Entity hay quy tắc tính dữ liệu thật. */
public record DashboardViewModel(LocalDate date, Instant loadedAt, String source,
        KpiSummary kpis, Map<AppointmentStatus, Long> appointments,
        List<TaskSummary> tasks, List<ShiftSummary> shifts, StockSummary stock) {
    public DashboardViewModel {
        Objects.requireNonNull(date);
        Objects.requireNonNull(loadedAt);
        Objects.requireNonNull(source);
        Objects.requireNonNull(kpis);
        Objects.requireNonNull(stock);
        EnumMap<AppointmentStatus, Long> normalized = new EnumMap<>(AppointmentStatus.class);
        for (AppointmentStatus status : AppointmentStatus.values()) {
            long count = appointments.getOrDefault(status, 0L);
            nonNegative(count);
            normalized.put(status, count);
        }
        appointments = Map.copyOf(normalized);
        tasks = List.copyOf(tasks);
        shifts = List.copyOf(shifts);
        if (tasks.stream().map(TaskSummary::id).distinct().count() != tasks.size()) {
            throw new IllegalArgumentException("Mã nhóm việc phải duy nhất");
        }
        long total = appointments.values().stream().reduce(0L, Math::addExact);
        if (total != kpis.appointmentCount()) {
            throw new IllegalArgumentException("Tổng lịch hẹn không khớp các trạng thái");
        }
    }

    public long affectedTotal() {
        return tasks.stream().map(TaskSummary::affectedCount).reduce(0L, Math::addExact);
    }

    public enum Priority {
        IMMEDIATE("CẦN XỬ LÝ NGAY"), PENDING("CHỜ DUYỆT / XÁC NHẬN"), FOLLOW_UP("THEO DÕI");
        private final String label;
        Priority(String label) { this.label = label; }
        public String label() { return label; }
    }

    public enum AppointmentStatus {
        CHO_XAC_NHAN("Chờ xác nhận"), DA_XAC_NHAN("Đã xác nhận"), DA_DEN("Đã đến"),
        HOAN_THANH("Hoàn thành"), VANG_MAT("Vắng mặt"), DA_HUY("Đã hủy"), DA_DOI_LICH("Đã đổi lịch");
        private final String label;
        AppointmentStatus(String label) { this.label = label; }
        public String label() { return label; }
    }

    public record KpiSummary(long appointmentCount, long waiting, long examining, long concluded,
            long cash, long transfer, long card, long debt, long unpaidInvoices) {
        public KpiSummary {
            for (long n : new long[]{appointmentCount, waiting, examining, concluded,
                    cash, transfer, card, debt, unpaidInvoices}) { nonNegative(n); }
            Math.addExact(Math.addExact(cash, transfer), card);
        }
        public long received() { return Math.addExact(Math.addExact(cash, transfer), card); }
    }

    public record TaskSummary(String id, Priority priority, String title, String description,
            long affectedCount, String targetScreenKey, String actionLabel) {
        public TaskSummary {
            Objects.requireNonNull(id);
            Objects.requireNonNull(priority);
            Objects.requireNonNull(title);
            Objects.requireNonNull(description);
            Objects.requireNonNull(targetScreenKey);
            Objects.requireNonNull(actionLabel);
            nonNegative(affectedCount);
        }
    }

    public record ShiftSummary(String name, long missingNurses) {
        public ShiftSummary { Objects.requireNonNull(name); nonNegative(missingNurses); }
    }

    public record StockSummary(long belowMinimum, long expiringLots, long expiredLots,
            boolean emergencyNeedsSupply, Instant emergencyCheckedAt) {
        public StockSummary {
            nonNegative(belowMinimum); nonNegative(expiringLots); nonNegative(expiredLots);
            Objects.requireNonNull(emergencyCheckedAt);
        }
    }

    private static void nonNegative(long value) {
        if (value < 0) { throw new IllegalArgumentException("Số lượng hoặc số tiền không được âm"); }
    }
}
