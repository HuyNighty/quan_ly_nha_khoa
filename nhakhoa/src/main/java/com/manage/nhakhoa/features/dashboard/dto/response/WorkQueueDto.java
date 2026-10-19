package com.manage.nhakhoa.features.dashboard.dto.response;

import java.util.List;

/**
 * workGroupCount = số nhóm có objectCount > 0; relatedObjectCount = tổng LƯỢT liên quan (có thể trùng giữa nhóm), không unique.
 * Badge sidebar KHÔNG được tính từ hai số này (BA V1 Q03).
 */
public record WorkQueueDto(int workGroupCount, long relatedObjectCount, List<WorkQueueItem> items) {
    public WorkQueueDto {
        items = List.copyOf(items);
    }
}
