package com.manage.nhakhoa.features.dashboard.service;

import com.manage.nhakhoa.features.dashboard.dto.response.RoleCount;
import com.manage.nhakhoa.features.dashboard.dto.response.ShiftStaffingDto;
import com.manage.nhakhoa.features.dashboard.dto.response.ShiftState;
import com.manage.nhakhoa.features.dashboard.repository.ShiftAssignmentRow;
import com.manage.nhakhoa.features.dashboard.repository.ShiftRow;
import com.manage.nhakhoa.shared.enums.VaiTro;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * BA V1 Q08: missing = max(định mức − số nhân viên DISTINCT đã phân công, 0) theo từng chức danh BS/YT/LT.
 * Đây là lịch phân công, không phải chấm công/hiện diện. Ca không có phân công vẫn báo thiếu nếu định mức dương.
 * Ca qua đêm (gioKetThuc < gioBatDau) hợp lệ, quy thuộc ngày bắt đầu; giờ bằng nhau hoặc định mức âm/null = lỗi cấu hình.
 */
final class StaffingCalculator {

    private StaffingCalculator() {
    }

    static List<ShiftStaffingDto> calculate(List<ShiftRow> shifts, List<ShiftAssignmentRow> assignments) {
        Map<String, Map<VaiTro, Long>> assignedByShift = new HashMap<>();
        for (ShiftAssignmentRow row : assignments) {
            assignedByShift.computeIfAbsent(row.shiftCode(), k -> new EnumMap<>(VaiTro.class))
                    .merge(row.role(), row.distinctEmployeeCount(), Long::sum);
        }

        List<ShiftStaffingDto> result = new ArrayList<>(shifts.size());
        for (ShiftRow shift : shifts) {
            Map<VaiTro, Long> assignedMap = assignedByShift.getOrDefault(shift.shiftCode(), Map.of());
            RoleCount assigned = new RoleCount(
                    toInt(assignedMap.get(VaiTro.BS)), toInt(assignedMap.get(VaiTro.YT)), toInt(assignedMap.get(VaiTro.LT)));

            boolean validHours = shift.startTime() != null && shift.endTime() != null
                    && !shift.startTime().equals(shift.endTime());
            boolean validRequirement = isValidRequirement(shift.requiredBacSi())
                    && isValidRequirement(shift.requiredYTa())
                    && isValidRequirement(shift.requiredLeTan());
            boolean overnight = validHours && shift.endTime().isBefore(shift.startTime());

            if (!validHours || !validRequirement) {
                RoleCount required = new RoleCount(nonNull(shift.requiredBacSi()), nonNull(shift.requiredYTa()),
                        nonNull(shift.requiredLeTan()));
                result.add(new ShiftStaffingDto(shift.shiftCode(), shift.shiftName(), shift.startTime(), shift.endTime(),
                        overnight, ShiftState.LOI_CAU_HINH, required, assigned, RoleCount.ZERO));
                continue;
            }

            RoleCount required = new RoleCount(shift.requiredBacSi(), shift.requiredYTa(), shift.requiredLeTan());
            RoleCount missing = new RoleCount(
                    Math.max(required.bacSi() - assigned.bacSi(), 0),
                    Math.max(required.yTa() - assigned.yTa(), 0),
                    Math.max(required.leTan() - assigned.leTan(), 0));
            ShiftState state = missing.bacSi() + missing.yTa() + missing.leTan() > 0
                    ? ShiftState.THIEU_NHAN_SU : ShiftState.DU_NHAN_SU;
            result.add(new ShiftStaffingDto(shift.shiftCode(), shift.shiftName(), shift.startTime(), shift.endTime(),
                    overnight, state, required, assigned, missing));
        }
        result.sort(Comparator.comparing(ShiftStaffingDto::startTime, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(ShiftStaffingDto::shiftCode, Comparator.nullsLast(Comparator.naturalOrder())));
        return result;
    }

    private static boolean isValidRequirement(Integer value) {
        return value != null && value >= 0;
    }

    private static int nonNull(Integer value) {
        return value == null ? 0 : value;
    }

    private static int toInt(Long value) {
        return value == null ? 0 : Math.toIntExact(value);
    }
}
