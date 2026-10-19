package com.manage.nhakhoa.features.dashboard.repository;

import java.math.BigDecimal;

/** Tổng tiền đã gộp theo từng hóa đơn (chỉ mã nội bộ, không có thông tin bệnh nhân). */
public record InvoiceAmountRow(String invoiceId, BigDecimal amount) {
}
