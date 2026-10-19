package com.manage.nhakhoa.features.dashboard.dto.response;

import java.time.LocalTime;

/** Định mức và số phân công DISTINCT nhân viên của một ca trong ngày D (BA V1 Q08). Không có tên nhân viên. */
public record ShiftStaffingDto(
        String shiftCode,
        String shiftName,
        LocalTime startTime,
        LocalTime endTime,
        boolean overnight,
        ShiftState state,
        RoleCount required,
        RoleCount assigned,
        RoleCount missing) {
}
