package com.manage.nhakhoa.features.dashboard.dto.response;

/** Một nhóm việc có nguồn truy vấn thật; chỉ xuất khi objectCount > 0. */
public record WorkQueueItem(
        String code,
        WorkPriority priority,
        String title,
        long objectCount,
        String targetScreen,
        String sourceMetric) {
}
