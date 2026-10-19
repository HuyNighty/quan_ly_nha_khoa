package com.manage.nhakhoa.desktop.demo;

import com.manage.nhakhoa.desktop.demo.DemoClinicStore.Code;
import com.manage.nhakhoa.desktop.demo.DemoClinicStore.Result;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Slice cung ứng MÔ PHỎNG: nhà cung cấp, thuốc & vật tư, lô (HSD), phiếu nhập/xuất/hoàn trả (xuất theo FEFO),
 * phiếu điều chỉnh kho (người lập ≠ người duyệt, đối chiếu snapshot), hộp cấp cứu.
 * Seed khớp Tổng quan: 2 mặt hàng dưới định mức, 2 lô cận hạn, 1 lô hết hạn, hộp cấp cứu cần bổ sung, kiểm tra gần nhất D−7 07:45.
 * HSD đúng ngày D tính là hết hạn; HSD trống là "chưa rõ" — không tự kết luận an toàn.
 */
public final class SupplySlice {
    public static final int NEAR_EXPIRY_DAYS = 30, KIT_CYCLE_DAYS = 7;
    public enum SupplierStatus { DANG_HOP_TAC("Đang hợp tác"), NGUNG_HOP_TAC("Ngừng hợp tác");
        private final String label; SupplierStatus(String l) { label = l; } public String label() { return label; } }
    public enum ItemType { THUOC("Thuốc"), VAT_TU("Vật tư");
        private final String label; ItemType(String l) { label = l; } public String label() { return label; } }
    public enum LotState { CON_HAN("Còn hạn"), CAN_HAN("Cận hạn"), HET_HAN("Hết hạn"), CHUA_RO("Chưa rõ HSD");
        private final String label; LotState(String l) { label = l; } public String label() { return label; } }
    public enum VoucherType { NHAP("Phiếu nhập"), XUAT("Phiếu xuất"), HOAN_TRA("Hoàn trả");
        private final String label; VoucherType(String l) { label = l; } public String label() { return label; } }
    public enum AdjustType { KIEM_KE("Kiểm kê"), HU_HONG("Hư hỏng"), HET_HAN("Hết hạn");
        private final String label; AdjustType(String l) { label = l; } public String label() { return label; }
        @Override public String toString() { return label; } }
    public enum AdjustStatus { NHAP("Bản nháp"), CHO_DUYET("Chờ duyệt"), DA_DUYET("Đã duyệt"), TU_CHOI("Từ chối");
        private final String label; AdjustStatus(String l) { label = l; } public String label() { return label; } }
    public enum KitState { DU("Đủ số lượng"), THIEU("Thiếu / cần bổ sung"), CAN_HAN("Cận hạn"), HET_HAN("Hết hạn"), CHUA_DU_THONG_TIN("Chưa đủ thông tin");
        private final String label; KitState(String l) { label = l; } public String label() { return label; } }

    public record Supplier(String id, String name, String address, String phone, String email, SupplierStatus status) { }
    public record Item(String id, String name, ItemType type, String unit, int minimum, String supplierId, boolean ordered) { }
    public record Lot(String id, String itemId, String lotNo, int quantity, LocalDate expiry, LocalDate received, String supplierId) { }
    public record VoucherLine(String lotId, int quantity) { }
    public record Voucher(String id, VoucherType type, LocalDateTime time, String partner, String staffId, String reason,
            List<VoucherLine> lines, String sourceVoucherId) { }
    public record Adjustment(String id, AdjustType type, String lotId, int systemQty, int actualQty, String reason,
            String creatorId, LocalDateTime created, AdjustStatus status, String approverId) {
        public int diff() { return actualQty - systemQty; }
    }
    public record KitItem(String id, String name, boolean official, int minimum, int quantity, LocalDate expiry, boolean expiryRequired) { }
    public record KitCheck(String id, LocalDateTime time, String checkerId, String note) { }

    private final DemoClinicStore store;
    private final Map<String, Supplier> suppliers = new LinkedHashMap<>();
    private final Map<String, Item> itemsById = new LinkedHashMap<>();
    private final Map<String, Lot> lots = new LinkedHashMap<>();
    private final Map<String, Voucher> vouchers = new LinkedHashMap<>();
    private final Map<String, Adjustment> adjustments = new LinkedHashMap<>();
    private final Map<String, KitItem> kit = new LinkedHashMap<>();
    private final List<KitCheck> checks = new ArrayList<>();
    private int nextLot = 1, nextIn = 1, nextOut = 1, nextReturn = 1, nextAdjust = 1, nextCheck = 1;

    SupplySlice(DemoClinicStore store) { this.store = store; }

    void seed(LocalDate d) {
        supplier("NCC01", "Công ty Dược phẩm Mẫu A", "Số 1 đường Mẫu, TP. Giả Lập", "0283000001", "lienhe@duocmau-a.example", SupplierStatus.DANG_HOP_TAC);
        supplier("NCC02", "Vật tư Nha khoa Mẫu B", "Số 2 đường Mẫu, TP. Giả Lập", "0283000002", "sales@vattu-b.example", SupplierStatus.DANG_HOP_TAC);
        supplier("NCC03", "Thiết bị Y tế Mẫu C", "Số 3 đường Mẫu, TP. Giả Lập", "0283000003", "info@thietbi-c.example", SupplierStatus.NGUNG_HOP_TAC);
        supplier("NCC04", "Dược phẩm Mẫu D", "Số 4 đường Mẫu, TP. Giả Lập", "0283000004", "order@duoc-d.example", SupplierStatus.DANG_HOP_TAC);
        item("T001", "Lidocaine 2% (gây tê)", ItemType.THUOC, "ống", 50, "NCC01");
        item("T002", "Amoxicillin 500mg", ItemType.THUOC, "viên", 100, "NCC04");
        item("T003", "Paracetamol 500mg", ItemType.THUOC, "viên", 100, "NCC04");
        item("T004", "Adrenaline 1mg/ml", ItemType.THUOC, "ống", 5, "NCC01");
        item("T005", "Chlorhexidine 0,12%", ItemType.THUOC, "chai", 10, "NCC01");
        item("V001", "Găng tay y tế", ItemType.VAT_TU, "hộp", 20, "NCC02");
        item("V002", "Composite A2", ItemType.VAT_TU, "tuýp", 5, "NCC02");
        item("V003", "Kim tiêm nha khoa", ItemType.VAT_TU, "hộp", 10, "NCC02");
        String l1 = lot("T001", "LDC-2601", 30, d.plusDays(200), d.minusDays(30), "NCC01");
        String l2 = lot("T002", "AMX-2507", 300, d.plusDays(20), d.minusDays(60), "NCC04");
        lot("T003", "PCM-2610", 200, d.plusDays(400), d.minusDays(12), "NCC04");
        lot("T004", "ADR-2511", 6, d.plusDays(25), d.minusDays(12), "NCC01");
        String l5 = lot("T005", "CHX-2504", 4, d.minusDays(2), d.minusDays(200), "NCC01");
        lot("T005", "CHX-2609", 3, d.plusDays(300), d.minusDays(12), "NCC01");
        String l7 = lot("V001", "GT-2608", 58, null, d.minusDays(12), "NCC02");
        lot("V002", "CPA2-2606", 12, d.plusDays(500), d.minusDays(12), "NCC02");
        lot("V003", "KIM-2605", 25, d.plusDays(700), d.minusDays(30), "NCC02");
        voucher(VoucherType.NHAP, d.minusDays(30).atTime(9, 0), "NCC01", "NV011", "Nhập định kỳ (mẫu)", List.of(new VoucherLine(l1, 30)), null);
        voucher(VoucherType.NHAP, d.minusDays(12).atTime(10, 0), "NCC02", "NV011", "Nhập vật tư (mẫu)", List.of(new VoucherLine(l7, 60)), null);
        String out = voucher(VoucherType.XUAT, d.minusDays(1).atTime(8, 0), "Phòng khám 1", "NV011", "Cấp phát cho ca sáng (mẫu)",
                List.of(new VoucherLine(l7, 3), new VoucherLine(l2, 20)), null);
        voucher(VoucherType.HOAN_TRA, d.minusDays(1).atTime(17, 0), "Phòng khám 1", "NV005", "Trả lại hộp chưa dùng (mẫu)",
                List.of(new VoucherLine(l7, 1)), out);
        adjust(AdjustType.KIEM_KE, l7, 58, 56, "Kiểm kê cuối tuần lệch 2 hộp (mẫu)", "NV011", d.minusDays(1).atTime(18, 0), AdjustStatus.CHO_DUYET, null);
        adjust(AdjustType.HET_HAN, l5, 4, 0, "Lô hết hạn chờ hủy (mẫu)", "NV011", d.atTime(7, 0), AdjustStatus.NHAP, null);
        adjust(AdjustType.HU_HONG, l1, 32, 30, "Vỡ 2 ống khi vận chuyển (mẫu)", "NV011", d.minusDays(20).atTime(9, 0), AdjustStatus.DA_DUYET, "NV010");
        kit("CC01", "Adrenaline 1mg/ml", true, 2, 2, d.plusDays(25), true);
        kit("CC02", "Bình oxy di động", true, 1, 1, null, true);
        kit("CC03", "Bóng Ambu người lớn", true, 1, 1, null, false);
        kit("CC04", "Glucose 30%", true, 2, 1, d.plusDays(180), true);
        kit("CC05", "Atropin 0,25mg (mục mẫu)", false, 2, 2, d.plusDays(100), true);
        checks.add(new KitCheck(String.format("KT%04d", nextCheck++), d.minusDays(7).atTime(7, 45), "NV005", "Kiểm tra định kỳ (mẫu)"));
    }
    private void supplier(String id, String name, String addr, String phone, String email, SupplierStatus st) {
        suppliers.put(id, new Supplier(id, name, addr, phone, email, st));
    }
    private void item(String id, String name, ItemType t, String unit, int min, String sup) { itemsById.put(id, new Item(id, name, t, unit, min, sup, false)); }
    private String lot(String item, String no, int qty, LocalDate exp, LocalDate received, String sup) {
        String id = String.format("LO%04d", nextLot++); lots.put(id, new Lot(id, item, no, qty, exp, received, sup)); return id;
    }
    private String voucher(VoucherType t, LocalDateTime time, String partner, String staff, String reason, List<VoucherLine> lines, String source) {
        String id = switch (t) { case NHAP -> String.format("PN%04d", nextIn++); case XUAT -> String.format("PX%04d", nextOut++);
            case HOAN_TRA -> String.format("HT%04d", nextReturn++); };
        vouchers.put(id, new Voucher(id, t, time, partner, staff, reason, List.copyOf(lines), source)); return id;
    }
    private void adjust(AdjustType t, String lot, int sys, int actual, String reason, String creator, LocalDateTime time, AdjustStatus st, String approver) {
        String id = String.format("DC%04d", nextAdjust++); adjustments.put(id, new Adjustment(id, t, lot, sys, actual, reason, creator, time, st, approver));
    }
    private void kit(String id, String name, boolean official, int min, int qty, LocalDate exp, boolean expiryRequired) {
        kit.put(id, new KitItem(id, name, official, min, qty, exp, expiryRequired));
    }

    // ---------------------------------------------------------------- suppliers
    public List<Supplier> suppliers() { return List.copyOf(suppliers.values()); }
    public Optional<Supplier> supplier(String id) { return Optional.ofNullable(id == null ? null : suppliers.get(id)); }
    public String supplierName(String id) { return supplier(id).map(Supplier::name).orElse(id == null ? "—" : id); }
    public List<Voucher> receiptsOf(String supplierId) {
        return vouchers.values().stream().filter(v -> v.type() == VoucherType.NHAP && supplierId.equals(v.partner())).toList();
    }
    public Result<Supplier> setSupplierStatus(String id, SupplierStatus status, String reason) {
        Supplier s = suppliers.get(id);
        if (s == null) return Result.fail(Code.NOT_FOUND, "Chọn nhà cung cấp");
        if (s.status() == status) return Result.fail(Code.RULE, "Nhà cung cấp đã " + status.label().toLowerCase());
        if (reason == null || reason.trim().length() < 5) return Result.fail(Code.INVALID, "Lý do phải từ 5 ký tự");
        Supplier u = new Supplier(s.id(), s.name(), s.address(), s.phone(), s.email(), status); suppliers.put(id, u);
        store.logAs(DemoClinicStore.ACTOR, "Đổi tình trạng hợp tác → " + status.label(), "NhaCungCap", id, "Thành công", null); store.fire();
        return Result.success(s.name() + " → " + status.label(), u);
    }

    // ---------------------------------------------------------------- items & lots
    public List<Item> items() { return List.copyOf(itemsById.values()); }
    public Optional<Item> item(String id) { return Optional.ofNullable(id == null ? null : itemsById.get(id)); }
    public List<Lot> lots(String itemId) {
        return lots.values().stream().filter(l -> l.itemId().equals(itemId))
                .sorted(Comparator.comparing(Lot::expiry, Comparator.nullsLast(Comparator.naturalOrder()))).toList();
    }
    public Optional<Lot> lot(String id) { return Optional.ofNullable(id == null ? null : lots.get(id)); }
    public List<Lot> allLots() { return List.copyOf(lots.values()); }
    public LotState lotState(Lot l) {
        if (l.expiry() == null) return LotState.CHUA_RO;
        LocalDate d = store.today();
        if (!l.expiry().isAfter(d)) return LotState.HET_HAN;
        if (!l.expiry().isAfter(d.plusDays(NEAR_EXPIRY_DAYS))) return LotState.CAN_HAN;
        return LotState.CON_HAN;
    }
    public int physical(String itemId) { return lots(itemId).stream().mapToInt(Lot::quantity).sum(); }
    /** Tồn khả dụng: lô có HSD sau ngày D (BA Q09); lô chưa rõ HSD tính riêng cho vật tư không bắt buộc HSD. */
    public int available(String itemId) {
        Item it = itemsById.get(itemId);
        return lots(itemId).stream().filter(l -> lotState(l) == LotState.CON_HAN || lotState(l) == LotState.CAN_HAN
                || (lotState(l) == LotState.CHUA_RO && it.type() == ItemType.VAT_TU)).mapToInt(Lot::quantity).sum();
    }
    public boolean belowMinimum(String itemId) { return available(itemId) < itemsById.get(itemId).minimum(); }
    public long belowMinimumCount() { return itemsById.keySet().stream().filter(this::belowMinimum).count(); }
    public long lotCount(LotState state) { return lots.values().stream().filter(l -> l.quantity() > 0 && lotState(l) == state).count(); }
    public Result<Item> markOrdered(String itemId) {
        Item i = itemsById.get(itemId);
        if (i == null) return Result.fail(Code.NOT_FOUND, "Chọn mặt hàng");
        if (i.ordered()) return Result.fail(Code.RULE, "Đã đánh dấu đặt hàng");
        if (!belowMinimum(itemId)) return Result.fail(Code.RULE, "Mặt hàng chưa dưới định mức");
        Item u = new Item(i.id(), i.name(), i.type(), i.unit(), i.minimum(), i.supplierId(), true); itemsById.put(i.id(), u);
        store.logAs(DemoClinicStore.ACTOR, "Đánh dấu đã đặt hàng", "ThuocVatTu", i.id(), "Thành công", null); store.fire();
        return Result.success("Đã đánh dấu " + i.name() + " là đã đặt hàng (không gửi đơn thật)", u);
    }

    // ---------------------------------------------------------------- vouchers
    public List<Voucher> vouchers(VoucherType type) {
        return vouchers.values().stream().filter(v -> v.type() == type).sorted(Comparator.comparing(Voucher::time).reversed()).toList();
    }
    public Optional<Voucher> voucher(String id) { return Optional.ofNullable(id == null ? null : vouchers.get(id)); }
    public Result<Voucher> receive(String itemId, String supplierId, String lotNo, int qty, LocalDate expiry) {
        Item it = itemsById.get(itemId);
        if (it == null) return Result.fail(Code.INVALID, "Chọn mặt hàng");
        Supplier s = suppliers.get(supplierId);
        if (s == null) return Result.fail(Code.INVALID, "Chọn nhà cung cấp");
        if (s.status() != SupplierStatus.DANG_HOP_TAC) return Result.fail(Code.RULE, "Nhà cung cấp đã ngừng hợp tác");
        String no = lotNo == null ? "" : lotNo.trim();
        if (no.length() < 3) return Result.fail(Code.INVALID, "Số lô phải từ 3 ký tự");
        if (lots.values().stream().anyMatch(l -> l.itemId().equals(itemId) && l.lotNo().equalsIgnoreCase(no))) return Result.fail(Code.INVALID, "Số lô đã tồn tại");
        if (qty <= 0 || qty > 100_000) return Result.fail(Code.INVALID, "Số lượng từ 1 đến 100.000");
        if (it.type() == ItemType.THUOC && expiry == null) return Result.fail(Code.INVALID, "Thuốc bắt buộc có hạn dùng (dd/MM/yyyy)");
        if (expiry != null && !expiry.isAfter(store.today())) return Result.fail(Code.INVALID, "Không nhập lô đã hết hạn");
        String lot = lot(itemId, no, qty, expiry, store.today(), supplierId);
        String id = voucher(VoucherType.NHAP, store.now(), supplierId, StaffSlice.ADMIN_ID, "Nhập kho (mô phỏng)", List.of(new VoucherLine(lot, qty)), null);
        store.logAs(DemoClinicStore.ACTOR, "Lập phiếu nhập", "PhieuNhap", id, "Thành công", null); store.fire();
        return Result.success("Đã lập " + id + " · lô " + no, vouchers.get(id));
    }
    /** Xuất theo FEFO: lô hết hạn sớm nhất trước, bỏ qua lô hết hạn; không vượt tồn khả dụng. */
    public Result<Voucher> issue(String itemId, int qty, String receiver, String reason) {
        Item it = itemsById.get(itemId);
        if (it == null) return Result.fail(Code.INVALID, "Chọn mặt hàng");
        if (qty <= 0) return Result.fail(Code.INVALID, "Số lượng phải lớn hơn 0");
        if (qty > available(itemId)) return Result.fail(Code.RULE, "Vượt tồn khả dụng (" + available(itemId) + " " + it.unit() + ")");
        String rc = receiver == null ? "" : receiver.trim(), rs = reason == null ? "" : reason.trim();
        if (rc.length() < 3) return Result.fail(Code.INVALID, "Nhập nơi / người nhận");
        if (rs.length() < 5) return Result.fail(Code.INVALID, "Lý do xuất phải từ 5 ký tự");
        int left = qty; List<VoucherLine> lines = new ArrayList<>();
        for (Lot l : lots(itemId)) {
            LotState st = lotState(l);
            boolean usable = st == LotState.CON_HAN || st == LotState.CAN_HAN || (st == LotState.CHUA_RO && it.type() == ItemType.VAT_TU);
            if (!usable || l.quantity() == 0 || left == 0) continue;
            int take = Math.min(left, l.quantity()); left -= take; lines.add(new VoucherLine(l.id(), take));
            lots.put(l.id(), new Lot(l.id(), l.itemId(), l.lotNo(), l.quantity() - take, l.expiry(), l.received(), l.supplierId()));
        }
        String id = voucher(VoucherType.XUAT, store.now(), rc, StaffSlice.ADMIN_ID, rs, lines, null);
        store.logAs(DemoClinicStore.ACTOR, "Lập phiếu xuất (FEFO)", "PhieuXuat", id, "Thành công", null); store.fire();
        return Result.success("Đã lập " + id + " · lấy từ " + lines.size() + " lô theo FEFO", vouchers.get(id));
    }
    public int returnable(String exportId, String lotId) {
        Voucher v = vouchers.get(exportId);
        if (v == null || v.type() != VoucherType.XUAT) return 0;
        int out = v.lines().stream().filter(l -> l.lotId().equals(lotId)).mapToInt(VoucherLine::quantity).sum();
        int back = vouchers.values().stream().filter(r -> r.type() == VoucherType.HOAN_TRA && exportId.equals(r.sourceVoucherId()))
                .flatMap(r -> r.lines().stream()).filter(l -> l.lotId().equals(lotId)).mapToInt(VoucherLine::quantity).sum();
        return out - back;
    }
    public Result<Voucher> giveBack(String exportId, String lotId, int qty, String reason) {
        Voucher v = vouchers.get(exportId);
        if (v == null || v.type() != VoucherType.XUAT) return Result.fail(Code.INVALID, "Chọn phiếu xuất gốc");
        Lot l = lots.get(lotId);
        if (l == null) return Result.fail(Code.INVALID, "Chọn lô");
        if (qty <= 0) return Result.fail(Code.INVALID, "Số lượng phải lớn hơn 0");
        if (qty > returnable(exportId, lotId)) return Result.fail(Code.RULE, "Không hoàn quá số đã xuất còn lại (" + returnable(exportId, lotId) + ")");
        if (lotState(l) == LotState.HET_HAN) return Result.fail(Code.RULE, "Lô đã hết hạn — lập phiếu điều chỉnh thay vì hoàn trả");
        if (reason == null || reason.trim().length() < 5) return Result.fail(Code.INVALID, "Lý do hoàn trả phải từ 5 ký tự");
        lots.put(l.id(), new Lot(l.id(), l.itemId(), l.lotNo(), l.quantity() + qty, l.expiry(), l.received(), l.supplierId()));
        String id = voucher(VoucherType.HOAN_TRA, store.now(), v.partner(), StaffSlice.ADMIN_ID, reason.trim(), List.of(new VoucherLine(lotId, qty)), exportId);
        store.logAs(DemoClinicStore.ACTOR, "Lập phiếu hoàn trả", "PhieuHoanTra", id, "Thành công", null); store.fire();
        return Result.success("Đã lập " + id + " hoàn " + qty + " về lô " + l.lotNo(), vouchers.get(id));
    }

    // ---------------------------------------------------------------- adjustments
    public List<Adjustment> adjustments() {
        return adjustments.values().stream().sorted(Comparator.comparing(Adjustment::created).reversed()).toList();
    }
    public Optional<Adjustment> adjustment(String id) { return Optional.ofNullable(adjustments.get(id)); }
    public Result<Adjustment> createAdjustment(AdjustType type, String lotId, int actual, String reason, String creatorId) {
        Lot l = lots.get(lotId);
        if (type == null) return Result.fail(Code.INVALID, "Chọn loại điều chỉnh");
        if (l == null) return Result.fail(Code.INVALID, "Chọn lô");
        if (actual < 0) return Result.fail(Code.INVALID, "Số thực tế không âm");
        if (actual == l.quantity()) return Result.fail(Code.INVALID, "Số thực tế bằng số hệ thống — không cần điều chỉnh");
        if (actual > l.quantity() && type != AdjustType.KIEM_KE) return Result.fail(Code.RULE, "Chỉ kiểm kê mới được tăng tồn");
        if (type == AdjustType.HET_HAN && lotState(l) != LotState.HET_HAN) return Result.fail(Code.RULE, "Lô chưa hết hạn");
        if (reason == null || reason.trim().length() < 5) return Result.fail(Code.INVALID, "Lý do phải từ 5 ký tự");
        String id = String.format("DC%04d", nextAdjust++);
        Adjustment a = new Adjustment(id, type, lotId, l.quantity(), actual, reason.trim(), creatorId, store.now(), AdjustStatus.CHO_DUYET, null);
        adjustments.put(id, a);
        store.logAs(creatorId + " (mô phỏng)", "Lập phiếu điều chỉnh " + type.label(), "DieuChinhKho", id, "Thành công", null); store.fire();
        return Result.success("Đã lập " + id + " (chờ duyệt) · snapshot hệ thống " + l.quantity(), a);
    }
    public Result<Adjustment> submit(String id) {
        Adjustment a = adjustments.get(id);
        if (a == null) return Result.fail(Code.NOT_FOUND, "Chọn phiếu");
        if (a.status() != AdjustStatus.NHAP) return Result.fail(Code.RULE, "Chỉ gửi duyệt phiếu nháp");
        Adjustment u = new Adjustment(a.id(), a.type(), a.lotId(), a.systemQty(), a.actualQty(), a.reason(), a.creatorId(), a.created(), AdjustStatus.CHO_DUYET, null);
        adjustments.put(id, u);
        store.logAs(a.creatorId() + " (mô phỏng)", "Gửi duyệt điều chỉnh", "DieuChinhKho", id, "Thành công", null); store.fire();
        return Result.success("Đã gửi duyệt " + id, u);
    }
    public String approveBlocker(String id, String actorId) {
        Adjustment a = adjustments.get(id);
        if (a == null) return "Chọn phiếu";
        if (a.status() != AdjustStatus.CHO_DUYET) return "Phiếu ở trạng thái " + a.status().label();
        if (actorId == null) return "Chọn người duyệt";
        if (actorId.equals(a.creatorId())) return "Người lập không được tự duyệt";
        Lot l = lots.get(a.lotId());
        if (l == null || l.quantity() != a.systemQty()) return "Tồn lô đã thay đổi so với snapshot (" + a.systemQty() + " → "
                + (l == null ? 0 : l.quantity()) + ") — lập phiếu mới";
        return null;
    }
    public Result<Adjustment> approve(String id, String actorId) {
        String b = approveBlocker(id, actorId); if (b != null) return Result.fail(Code.RULE, b);
        Adjustment a = adjustments.get(id); Lot l = lots.get(a.lotId());
        lots.put(l.id(), new Lot(l.id(), l.itemId(), l.lotNo(), a.actualQty(), l.expiry(), l.received(), l.supplierId()));
        Adjustment u = new Adjustment(a.id(), a.type(), a.lotId(), a.systemQty(), a.actualQty(), a.reason(), a.creatorId(), a.created(), AdjustStatus.DA_DUYET, actorId);
        adjustments.put(id, u);
        store.logAs(actorId + " (mô phỏng)", "Duyệt điều chỉnh kho", "DieuChinhKho", id, "Thành công", null); store.fire();
        return Result.success("Đã duyệt " + id + " · lô " + l.lotNo() + " còn " + a.actualQty(), u);
    }
    public Result<Adjustment> reject(String id, String actorId, String note) {
        Adjustment a = adjustments.get(id);
        if (a == null) return Result.fail(Code.NOT_FOUND, "Chọn phiếu");
        if (a.status() != AdjustStatus.CHO_DUYET) return Result.fail(Code.RULE, "Chỉ từ chối phiếu chờ duyệt");
        if (actorId == null || actorId.equals(a.creatorId())) return Result.fail(Code.RULE, "Người lập không được tự xử lý");
        if (note == null || note.trim().length() < 5) return Result.fail(Code.INVALID, "Lý do từ chối phải từ 5 ký tự");
        Adjustment u = new Adjustment(a.id(), a.type(), a.lotId(), a.systemQty(), a.actualQty(), a.reason() + " · Từ chối: " + note.trim(),
                a.creatorId(), a.created(), AdjustStatus.TU_CHOI, actorId);
        adjustments.put(id, u);
        store.logAs(actorId + " (mô phỏng)", "Từ chối điều chỉnh kho", "DieuChinhKho", id, "Thành công", null); store.fire();
        return Result.success("Đã từ chối " + id, u);
    }

    // ---------------------------------------------------------------- emergency kit
    public List<KitItem> kit() { return List.copyOf(kit.values()); }
    public Optional<KitItem> kitItem(String id) { return Optional.ofNullable(kit.get(id)); }
    public KitState kitState(KitItem k) {
        if (k.expiryRequired() && k.expiry() == null) return KitState.CHUA_DU_THONG_TIN;
        if (k.expiry() != null && !k.expiry().isAfter(store.today())) return KitState.HET_HAN;
        if (k.quantity() < k.minimum()) return KitState.THIEU;
        if (k.expiry() != null && !k.expiry().isAfter(store.today().plusDays(NEAR_EXPIRY_DAYS))) return KitState.CAN_HAN;
        return KitState.DU;
    }
    public List<KitCheck> checks() { return checks.stream().sorted(Comparator.comparing(KitCheck::time).reversed()).toList(); }
    public Optional<KitCheck> lastCheck() { return checks().stream().findFirst(); }
    public boolean checkDue() {
        return lastCheck().map(c -> !c.time().toLocalDate().plusDays(KIT_CYCLE_DAYS).isAfter(store.today())).orElse(true);
    }
    /** Tình trạng chung: không bao giờ là "đạt an toàn"; mục mẫu không được tính. */
    public String kitSummary() {
        List<KitItem> official = kit.values().stream().filter(KitItem::official).toList();
        if (official.stream().anyMatch(k -> kitState(k) == KitState.CHUA_DU_THONG_TIN)) return "CHƯA ĐỦ THÔNG TIN / CẦN KIỂM TRA";
        if (official.stream().anyMatch(k -> kitState(k) == KitState.THIEU || kitState(k) == KitState.HET_HAN)) return "CẦN BỔ SUNG";
        return "ĐỦ SỐ LƯỢNG THEO DANH MỤC MẪU — không phải chứng nhận an toàn lâm sàng";
    }
    public Result<KitItem> recordKitItem(String id, int qty, LocalDate expiry, String checkerId) {
        KitItem k = kit.get(id);
        if (k == null) return Result.fail(Code.NOT_FOUND, "Chọn mục");
        if (qty < 0 || qty > 999) return Result.fail(Code.INVALID, "Số lượng từ 0 đến 999");
        if (k.expiryRequired() && expiry == null) return Result.fail(Code.INVALID, "Mục này bắt buộc nhập hạn dùng (dd/MM/yyyy)");
        KitItem u = new KitItem(k.id(), k.name(), k.official(), k.minimum(), qty, expiry, k.expiryRequired()); kit.put(id, u);
        String cid = String.format("KT%04d", nextCheck++);
        checks.add(new KitCheck(cid, store.now(), checkerId, "Cập nhật " + k.name() + " (mô phỏng)"));
        store.logAs(checkerId + " (mô phỏng)", "Ghi nhận kiểm tra hộp cấp cứu", "KiemTraCapCuu", cid, "Thành công", null); store.fire();
        return Result.success("Đã ghi nhận " + cid + " · " + k.name() + " → " + kitState(u).label(), u);
    }
}
