package com.manage.nhakhoa.features.dashboard.service;

import com.manage.nhakhoa.features.dashboard.dto.response.DashboardWarning;
import com.manage.nhakhoa.features.dashboard.repository.DashboardReadRepository;

import java.util.List;
import java.util.Optional;

/** Đọc ngưỡng ngày từ ThamSoHeThong; fallback + cảnh báo khi thiếu, sai định dạng hoặc lỗi đọc. */
final class ThresholdResolver {

    private final DashboardReadRepository repository;

    ThresholdResolver(DashboardReadRepository repository) {
        this.repository = repository;
    }

    int resolveDays(String key, int fallback, String section, List<DashboardWarning> warnings) {
        Optional<String> raw;
        try {
            raw = repository.findSystemParameter(key);
        } catch (RuntimeException ex) {
            warnings.add(new DashboardWarning("THAM_SO_KHONG_DOC_DUOC", section,
                    "Không đọc được tham số " + key + "; dùng mặc định " + fallback + " ngày."));
            return fallback;
        }
        if (raw.isEmpty() || raw.get().isBlank()) {
            warnings.add(new DashboardWarning("THAM_SO_CHUA_CAU_HINH", section,
                    "Chưa cấu hình " + key + "; dùng mặc định " + fallback + " ngày."));
            return fallback;
        }
        try {
            int value = Integer.parseInt(raw.get().trim());
            if (value <= 0 || value > DashboardThresholds.MAX_DAYS) {
                throw new NumberFormatException("out of range");
            }
            return value;
        } catch (NumberFormatException ex) {
            warnings.add(new DashboardWarning("THAM_SO_KHONG_HOP_LE", section,
                    "Giá trị " + key + " không hợp lệ; dùng mặc định " + fallback + " ngày."));
            return fallback;
        }
    }
}
