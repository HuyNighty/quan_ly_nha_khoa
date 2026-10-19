package com.manage.nhakhoa.features.dashboard.repository;

/** Mặt hàng kho với định mức tối thiểu. */
public record StockItemRow(StockItemType type, String itemId, int minimumQuantity) {
}
