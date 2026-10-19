package com.manage.nhakhoa.features.dashboard.service;

import com.manage.nhakhoa.features.dashboard.repository.StockItemRow;
import com.manage.nhakhoa.features.dashboard.repository.StockItemType;
import com.manage.nhakhoa.features.dashboard.repository.StockLotRow;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * BA V1 Q09/Q10 (bảo thủ):
 * - expired: hanSuDung <= D; near: D < hanSuDung <= D + threshold; null HSD: unknown, không tính khả dụng.
 * - Tồn khả dụng theo mã mặt hàng = tổng soLuongTon của lô còn hạn (hanSuDung > D).
 * - Dưới định mức: dinhMucToiThieu > 0 và tồn khả dụng < định mức, đếm mã duy nhất.
 * - Lô tồn âm hoặc không trỏ đúng một trong Thuoc/VatTu: bất thường, loại khỏi phép tính.
 */
final class InventoryCalculator {

    record Result(int belowMinimumItemCount, int nearExpiryLotCount, int expiredLotCount,
                  int unknownExpiryLotCount, int anomalyLotCount) {
    }

    private record ItemKey(StockItemType type, String id) {
    }

    private InventoryCalculator() {
    }

    static Result calculate(List<StockLotRow> lots, List<StockItemRow> items, LocalDate businessDate, int thresholdDays) {
        LocalDate nearLimit = businessDate.plusDays(thresholdDays);
        Set<String> seenLots = new HashSet<>();
        Map<ItemKey, Long> available = new HashMap<>();
        int near = 0;
        int expired = 0;
        int unknown = 0;
        int anomaly = 0;

        for (StockLotRow lot : lots) {
            if (!seenLots.add(lot.lotId()) || lot.quantity() == 0) {
                continue;
            }
            boolean hasDrug = lot.drugId() != null;
            boolean hasSupply = lot.supplyId() != null;
            if (lot.quantity() < 0 || hasDrug == hasSupply) {
                anomaly++;
                continue;
            }
            LocalDate expiry = lot.expiryDate();
            if (expiry == null) {
                unknown++;
                continue;
            }
            if (!expiry.isAfter(businessDate)) {
                expired++;
                continue;
            }
            if (!expiry.isAfter(nearLimit)) {
                near++;
            }
            ItemKey key = hasDrug
                    ? new ItemKey(StockItemType.THUOC, lot.drugId())
                    : new ItemKey(StockItemType.VAT_TU, lot.supplyId());
            available.merge(key, (long) lot.quantity(), Long::sum);
        }

        Set<ItemKey> below = new HashSet<>();
        for (StockItemRow item : items) {
            if (item.minimumQuantity() <= 0) {
                continue;
            }
            ItemKey key = new ItemKey(item.type(), item.itemId());
            if (available.getOrDefault(key, 0L) < item.minimumQuantity()) {
                below.add(key);
            }
        }
        return new Result(below.size(), near, expired, unknown, anomaly);
    }
}
