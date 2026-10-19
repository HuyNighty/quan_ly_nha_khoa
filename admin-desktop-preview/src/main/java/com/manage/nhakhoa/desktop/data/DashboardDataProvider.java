package com.manage.nhakhoa.desktop.data;

import com.manage.nhakhoa.desktop.model.DashboardViewModel;
import java.time.Clock;
import java.util.Optional;

/** Chạy ngoài EDT. Optional.empty biểu diễn nguồn không có dữ liệu. */
@FunctionalInterface
public interface DashboardDataProvider {
    Optional<DashboardViewModel> load(Clock clock) throws Exception;
}
