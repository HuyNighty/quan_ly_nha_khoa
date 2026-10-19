package com.manage.nhakhoa.features.dashboard.repository;

import java.time.LocalDate;

/** Một mục chính thức hiện hành và chi tiết tương ứng (có thể thiếu) trong lần kiểm tra đã chọn. */
public record EmergencyCheckItemRow(String catalogItemId, String checkedItemId, Boolean passed,
                                    LocalDate expiryDate, Integer actualQuantity, Integer minimumQuantity) {
}
