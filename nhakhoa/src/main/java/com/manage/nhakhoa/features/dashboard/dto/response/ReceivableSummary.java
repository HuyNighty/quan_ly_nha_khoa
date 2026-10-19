package com.manage.nhakhoa.features.dashboard.dto.response;

/**
 * Công nợ hiện tại trên mọi hóa đơn khác DA_HUY (BA V1 Q07, V1.1 #05: hóa đơn đang chờ duyệt hủy vẫn tính).
 * overpaidInvoiceCount: số hóa đơn đã thu vượt tổng tiền — anomaly, không làm công nợ âm.
 */
public record ReceivableSummary(String amountVnd, long invoiceCount, String scope, long overpaidInvoiceCount) {
}
