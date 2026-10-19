package com.manage.nhakhoa.desktop.screens;

import com.manage.nhakhoa.desktop.MenuDefinition;
import com.manage.nhakhoa.desktop.demo.DemoClinicStore;
import com.manage.nhakhoa.desktop.demo.SupplySlice;
import com.manage.nhakhoa.desktop.demo.SupplySlice.*;
import com.manage.nhakhoa.desktop.ui.CardList;
import com.manage.nhakhoa.desktop.ui.DemoTable;
import com.manage.nhakhoa.desktop.ui.DemoTable.Column;
import com.manage.nhakhoa.desktop.ui.DemoTable.Tag;
import com.manage.nhakhoa.desktop.ui.Flat;
import com.manage.nhakhoa.desktop.ui.Theme;
import java.awt.*;
import java.util.List;
import javax.swing.*;
import static com.manage.nhakhoa.desktop.ui.DemoWidgets.*;
import static com.manage.nhakhoa.desktop.ui.UiComponents.*;

/**
 * Cung ứng · Thuốc & vật tư (THUOC_VAT_TU): tồn vật lý / khả dụng / định mức, lô và HSD (cận hạn 30 ngày, HSD = ngày D là hết hạn,
 * HSD trống là chưa rõ), nhà cung cấp, đánh dấu đã đặt hàng mô phỏng.
 */
public final class StockPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private final transient ScreenContext ctx;
    private final transient DemoClinicStore store;
    private final Flat.SearchBar search = new Flat.SearchBar("stock-search", "Tìm mã, tên thuốc / vật tư…");
    private final Flat.Segmented filter = new Flat.Segmented("stock-filter", List.of("Tất cả", "Thuốc", "Vật tư", "Dưới định mức", "Có lô cận / hết hạn"), null);
    private final CardList<Item> list;
    private final JPanel stats = Kit.stats();
    private final JPanel detail = Kit.holder("stock-detail");
    private final JLabel feedback = feedback("stock-feedback");
    private final DemoTable<Lot> lots;
    private transient String selected;

    public StockPanel(ScreenContext ctx) {
        super(new BorderLayout()); setOpaque(false); setName("screen-THUOC_VAT_TU");
        this.ctx = ctx; this.store = ctx.store();
        list = Kit.list("stock-list", Item::id, i -> Kit.Spec.block(i.id(), i.name(),
                i.type().label() + "  ·  khả dụng " + supply().available(i.id()) + "/" + i.minimum() + " " + i.unit() + "  ·  vật lý " + supply().physical(i.id()),
                itemTag(i)), 8);
        list.onSelect(i -> { selected = i.id(); render(); });
        lots = new DemoTable<>("stock-lots", List.of(new Column<Lot>("Số lô", l -> new DemoTable.Strong(l.lotNo()), 110),
                new Column<Lot>("Số lượng", Lot::quantity, 70), new Column<Lot>("Hạn dùng", l -> l.expiry() == null ? "Chưa rõ" : DATE.format(l.expiry()), 95),
                new Column<Lot>("Tình trạng", this::lotTag, 110), new Column<Lot>("Nhà cung cấp", l -> supply().supplierName(l.supplierId()), 160)), Lot::id, 4);
        search.field().getDocument().addDocumentListener(SimpleDocumentListener.of(this::refreshList));
        search.onSubmit(() -> list.selectFirst()); search.bindShortcut(this);
        filter.onChange(i -> refreshList());
        add(Kit.page(this, "Thuốc & vật tư", "Tồn khả dụng chỉ tính lô còn hạn sau ngày D · cận hạn ≤ 30 ngày · HSD trống là chưa rõ", ctx, "stock",
                "Không khẳng định an toàn sử dụng từ dữ liệu mẫu; đánh dấu đặt hàng không gửi đơn thật.", stats,
                Kit.twoCol(Kit.card("Danh mục", null, column(10, search, filter, list)),
                        Kit.card("Chi tiết & lô", null, column(10, detail, feedback)), 0.47)), BorderLayout.NORTH);
        store.addListener(this::refresh);
        refresh();
    }
    private SupplySlice supply() { return store.supply(); }
    private Tag itemTag(Item i) {
        if (supply().belowMinimum(i.id())) return new Tag(i.ordered() ? "Dưới định mức · đã đặt" : "Dưới định mức", Theme.RED);
        if (supply().lots(i.id()).stream().anyMatch(l -> l.quantity() > 0 && supply().lotState(l) == LotState.HET_HAN)) return new Tag("Có lô hết hạn", Theme.RED);
        if (supply().lots(i.id()).stream().anyMatch(l -> l.quantity() > 0 && supply().lotState(l) == LotState.CAN_HAN)) return new Tag("Có lô cận hạn", Theme.ORANGE);
        return new Tag("Đủ", Theme.GREEN);
    }
    private Tag lotTag(Lot l) {
        LotState s = supply().lotState(l);
        return new Tag(s.label(), switch (s) { case CON_HAN -> Theme.GREEN; case CAN_HAN -> Theme.ORANGE; case HET_HAN -> Theme.RED; case CHUA_RO -> Theme.MUTED; });
    }
    private void refresh() { refreshStats(); refreshList(); render(); }
    private void refreshStats() {
        stats.removeAll();
        stats.add(stat("stock-stat-items", "Mặt hàng", supply().items().size(), Theme.TEXT, () -> filter.select(0), false));
        stats.add(stat("stock-stat-below", "Dưới định mức", supply().belowMinimumCount(), Theme.RED, () -> filter.select(3), false));
        stats.add(stat("stock-stat-near", "Lô cận hạn (≤ 30 ngày)", supply().lotCount(LotState.CAN_HAN), Theme.ORANGE, () -> filter.select(4), false));
        stats.add(stat("stock-stat-expired", "Lô hết hạn còn tồn", supply().lotCount(LotState.HET_HAN), Theme.RED, () -> filter.select(4), false));
        stats.add(stat("stock-stat-unknown", "Lô chưa rõ HSD", supply().lotCount(LotState.CHUA_RO), Theme.MUTED, null, false));
        stats.revalidate();
    }
    private void refreshList() {
        String q = MenuDefinition.normalize(search.field().getText()); int f = filter.selected();
        List<Item> rows = supply().items().stream().filter(i -> q.isEmpty() || MenuDefinition.normalize(i.id() + " " + i.name()).contains(q))
                .filter(i -> switch (f) {
                    case 1 -> i.type() == ItemType.THUOC;
                    case 2 -> i.type() == ItemType.VAT_TU;
                    case 3 -> supply().belowMinimum(i.id());
                    case 4 -> supply().lots(i.id()).stream().anyMatch(l -> l.quantity() > 0
                            && (supply().lotState(l) == LotState.CAN_HAN || supply().lotState(l) == LotState.HET_HAN));
                    default -> true;
                }).toList();
        list.setRows(rows, "Không có mặt hàng khớp bộ lọc");
        if (selected == null && !rows.isEmpty()) selected = rows.get(0).id();
    }
    private void render() {
        Item i = supply().item(selected).orElse(null);
        if (i == null) { Kit.fill(detail, Kit.empty("Chọn một mặt hàng để xem lô và hạn dùng")); return; }
        if (list.selected().map(Item::id).filter(selected::equals).isEmpty()) list.select(selected);
        lots.setRows(supply().lots(i.id()), "Chưa có lô");
        JButton order = icon(primary("Đánh dấu đã đặt hàng", () -> result(feedback, supply().markOrdered(i.id()))), "box", Color.WHITE);
        order.setName("stock-mark-ordered"); order.setEnabled(supply().belowMinimum(i.id()) && !i.ordered());
        order.setToolTipText(i.ordered() ? "Đã đánh dấu" : supply().belowMinimum(i.id()) ? "Mô phỏng, không gửi đơn thật" : "Chưa dưới định mức");
        JButton toVoucher = link("Lập phiếu nhập / xuất", () -> ctx.navigate().accept("NHAP_XUAT")); toVoucher.setName("stock-open-vouchers");
        Kit.fill(detail, column(12, Kit.head("stock", i.name(), i.id() + " · " + i.type().label() + " · đơn vị " + i.unit(), itemTag(i)),
                Kit.info("stock-info", "Tồn vật lý", supply().physical(i.id()) + " " + i.unit(), "Tồn khả dụng", supply().available(i.id()) + " " + i.unit(),
                        "Định mức tối thiểu", i.minimum() + " " + i.unit(), "Nhà cung cấp chính", supply().supplierName(i.supplierId()),
                        "Đặt hàng", i.ordered() ? "Đã đánh dấu đặt hàng (mô phỏng)" : "Chưa"),
                Kit.section("Lô hàng (sắp xếp theo hạn dùng — FEFO)"), lots, Kit.separator(), buttons(order, toVoucher)));
    }
}
