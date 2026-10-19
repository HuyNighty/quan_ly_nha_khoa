package com.manage.nhakhoa.features.dashboard.service;

import com.manage.nhakhoa.features.dashboard.repository.EmergencyCheckItemRow;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Không suy an toàn từ chi tiết mẫu, chi tiết thiếu hoặc HSD chưa được xác nhận. */
final class EmergencyCoverageCalculator {
    record Result(boolean complete, long failingItemCount) { }
    private EmergencyCoverageCalculator() { }

    static Result calculate(long officialCount, List<EmergencyCheckItemRow> rows, LocalDate safeUntil) {
        if (officialCount <= 0 || rows.size() != officialCount) return new Result(false, 0);
        Set<String> seen = new HashSet<>();
        long failing = 0;
        for (EmergencyCheckItemRow row : rows) {
            if (row.catalogItemId() == null || !seen.add(row.catalogItemId())
                    || !row.catalogItemId().equals(row.checkedItemId()) || row.passed() == null
                    || row.expiryDate() == null || row.actualQuantity() == null || row.actualQuantity() < 0
                    || row.minimumQuantity() == null || row.minimumQuantity() < 0) {
                return new Result(false, 0);
            }
            if (!row.passed() || !row.expiryDate().isAfter(safeUntil)
                    || row.actualQuantity() < row.minimumQuantity()) failing++;
        }
        return new Result(true, failing);
    }
}
