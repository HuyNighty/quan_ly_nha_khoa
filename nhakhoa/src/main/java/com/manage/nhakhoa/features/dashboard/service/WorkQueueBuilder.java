package com.manage.nhakhoa.features.dashboard.service;

import com.manage.nhakhoa.features.dashboard.dto.response.WorkPriority;
import com.manage.nhakhoa.features.dashboard.dto.response.WorkQueueDto;
import com.manage.nhakhoa.features.dashboard.dto.response.WorkQueueItem;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Gom nhóm việc có nguồn truy vấn thật; bỏ nhóm count = 0; sắp xếp khẩn → chờ duyệt → theo dõi, rồi theo code. */
final class WorkQueueBuilder {

    private final List<WorkQueueItem> items = new ArrayList<>();

    WorkQueueBuilder add(String code, WorkPriority priority, String title, long objectCount,
                         String targetScreen, String sourceMetric) {
        if (objectCount > 0) {
            items.add(new WorkQueueItem(code, priority, title, objectCount, targetScreen, sourceMetric));
        }
        return this;
    }

    WorkQueueDto build() {
        List<WorkQueueItem> sorted = new ArrayList<>(items);
        sorted.sort(Comparator.comparing(WorkQueueItem::priority).thenComparing(WorkQueueItem::code));
        long related = sorted.stream().mapToLong(WorkQueueItem::objectCount).sum();
        return new WorkQueueDto(sorted.size(), related, sorted);
    }
}
