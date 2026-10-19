package com.manage.nhakhoa.features.dashboard.dto.response;

/**
 * Tồn kho (BA V1 Q09/Q10). Khi status != AVAILABLE các số là null (không thay bằng 0).
 * anomalyLotCount: lô tồn âm hoặc không trỏ đúng một trong Thuoc/VatTu — loại khỏi phép tính, báo bất thường.
 */
public record InventorySummary(
        SectionStatus status,
        Integer belowMinimumItemCount,
        Integer nearExpiryLotCount,
        Integer expiredLotCount,
        Integer unknownExpiryLotCount,
        Integer thresholdDays,
        Integer anomalyLotCount) {
}
