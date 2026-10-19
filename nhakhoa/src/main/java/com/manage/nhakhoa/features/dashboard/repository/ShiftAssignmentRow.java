package com.manage.nhakhoa.features.dashboard.repository;

import com.manage.nhakhoa.shared.enums.VaiTro;

/** COUNT(DISTINCT maNhanVien) theo (maCa, chucDanh) trong một ngayLam. */
public record ShiftAssignmentRow(String shiftCode, VaiTro role, long distinctEmployeeCount) {
}
