package com.manage.nhakhoa.features.dashboard.dto.response;

import com.manage.nhakhoa.features.hoadon.enums.PhuongThucThanhToan;

import java.util.Map;

/** Thu hợp lệ trong ngày (ThanhToan THANH_CONG theo thoiGian trong D). Số tiền là chuỗi số nguyên VND. */
public record CollectionSummary(String amountVnd, Map<PhuongThucThanhToan, String> byMethodVnd, String definition) {
    public CollectionSummary {
        java.util.EnumMap<PhuongThucThanhToan, String> copy = new java.util.EnumMap<>(PhuongThucThanhToan.class);
        copy.putAll(byMethodVnd);
        byMethodVnd = java.util.Collections.unmodifiableMap(copy);
    }
}
