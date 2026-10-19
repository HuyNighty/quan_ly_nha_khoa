package com.manage.nhakhoa.features.dashboard.repository;

import java.time.LocalDate;

/** Một lô có soLuongTon khác 0. drugId/supplyId: đúng một trong hai phải khác null, nếu không là bất thường. */
public record StockLotRow(String lotId, String drugId, String supplyId, LocalDate expiryDate, int quantity) {
}
