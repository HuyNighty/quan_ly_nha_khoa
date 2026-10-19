package com.manage.nhakhoa.features.dashboard.dto.response;

import com.manage.nhakhoa.features.lichhen.enums.TrangThaiLichHen;

import java.util.Map;

/**
 * Lịch hẹn theo thoiGianHen trong ngày D (BA V1 Q05).
 * totalRecords = tổng byStatus (đủ 7 trạng thái, gồm DA_HUY/DA_DOI_LICH) và là số BẢN GHI, không phải số bệnh nhân.
 * activeCount = CHO_XAC_NHAN + DA_XAC_NHAN + DA_DEN, trường riêng.
 */
public record AppointmentSummary(long totalRecords, long activeCount, Map<TrangThaiLichHen, Long> byStatus) {
    public AppointmentSummary {
        java.util.EnumMap<TrangThaiLichHen, Long> copy = new java.util.EnumMap<>(TrangThaiLichHen.class);
        copy.putAll(byStatus);
        byStatus = java.util.Collections.unmodifiableMap(copy);
    }
}
