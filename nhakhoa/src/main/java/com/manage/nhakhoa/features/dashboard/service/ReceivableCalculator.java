package com.manage.nhakhoa.features.dashboard.service;

import com.manage.nhakhoa.features.dashboard.repository.InvoiceAmountRow;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * BA V1 Q07: receivable(hóa đơn) = max(tổng chi tiết − tổng thanh toán THANH_CONG, 0); cộng theo từng hóa đơn.
 * Hai nguồn đã aggregate riêng nên không có nhân chéo dòng chi tiết × dòng thanh toán.
 */
final class ReceivableCalculator {

    record Result(BigDecimal amount, long invoiceCount, long overpaidInvoiceCount) {
    }

    private ReceivableCalculator() {
    }

    static Result calculate(List<InvoiceAmountRow> billedPerInvoice, List<InvoiceAmountRow> paidPerInvoice) {
        Map<String, BigDecimal> billed = sumById(billedPerInvoice);
        Map<String, BigDecimal> paid = sumById(paidPerInvoice);
        Set<String> invoiceIds = new HashSet<>(billed.keySet());
        invoiceIds.addAll(paid.keySet());

        BigDecimal total = BigDecimal.ZERO;
        long withDebt = 0;
        long overpaid = 0;
        for (String id : invoiceIds) {
            BigDecimal remaining = billed.getOrDefault(id, BigDecimal.ZERO)
                    .subtract(paid.getOrDefault(id, BigDecimal.ZERO));
            int sign = remaining.signum();
            if (sign > 0) {
                total = total.add(remaining);
                withDebt++;
            } else if (sign < 0) {
                overpaid++;
            }
        }
        return new Result(Money.vnd(total), withDebt, overpaid);
    }

    private static Map<String, BigDecimal> sumById(List<InvoiceAmountRow> rows) {
        Map<String, BigDecimal> result = new HashMap<>();
        for (InvoiceAmountRow row : rows) {
            if (row.amount() != null) {
                result.merge(row.invoiceId(), row.amount(), BigDecimal::add);
            }
        }
        return result;
    }
}
