package com.manage.nhakhoa.features.dashboard.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Tiền VND: BigDecimal scale 0; không làm tròn âm thầm (ArithmeticException nếu dữ liệu có phần lẻ). */
final class Money {

    private Money() {
    }

    static BigDecimal vnd(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(0, RoundingMode.UNNECESSARY);
    }

    static String format(BigDecimal value) {
        return vnd(value).toPlainString();
    }
}
