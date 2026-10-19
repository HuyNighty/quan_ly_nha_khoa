package com.manage.nhakhoa.features.dashboard.dto.response;

import java.util.List;

/** Nhân sự theo lịch phân công. Khi status != AVAILABLE: shifts rỗng và understaffedShiftCount null. */
public record StaffingSummary(SectionStatus status, List<ShiftStaffingDto> shifts, Integer understaffedShiftCount) {
    public StaffingSummary {
        shifts = List.copyOf(shifts);
    }
}
