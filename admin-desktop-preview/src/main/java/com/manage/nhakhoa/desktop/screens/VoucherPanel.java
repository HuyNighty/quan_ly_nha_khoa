package com.manage.nhakhoa.desktop.screens;

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
 * Cung ứng · Nhập / xuất / hoàn trả (NHAP_XUAT): phiếu theo loại, dòng chi tiết theo lô; lập phiếu nhập (NCC đang hợp tác, có HSD với thuốc),
 * xuất theo FEFO không vượt tồn khả dụng và bỏ lô hết hạn, hoàn trả không vượt số đã xuất. Không động đến kho thật.
 */
public final class VoucherPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private final transient ScreenContext ctx;
    private final transient DemoClinicStore store;
    private final Flat.Segmented tabs = new Flat.Segmented("voucher-tab", List.of("Phiếu nhập", "Phiếu xuất", "Hoàn trả"), null);
    private final CardList<Voucher> list;
    private final DemoTable<VoucherLine> lines;
    private final JPanel detail = Kit.holder("voucher-detail");
    private final FitCards forms = new FitCards();
    private final JLabel feedback = feedback("voucher-feedback");
    // Nhập
    private final JComboBox<String> inItem = combo("in-item", List.of());
    private final JComboBox<String> inSupplier = combo("in-supplier", List.of());
    private final JTextField inLot = field("in-lot", 10, "Số lô, ví dụ AMX-2611");
    private final JTextField inQty = field("in-qty", 6, "Số lượng");
    private final JTextField inExpiry = field("in-expiry", 10, "HSD dd/MM/yyyy");
    // Xuất
    private final JComboBox<String> outItem = combo("out-item", List.of());
    private final JTextField outQty = field("out-qty", 6, "Số lượng");
    private final JTextField outReceiver = field("out-receiver", 14, "Nơi / người nhận");
    private final JTextField outReason = field("out-reason", 18, "Lý do xuất");
    private final JLabel outAvail = label(" ", 12, false, Theme.MUTED);
    // Hoàn trả
    private final JComboBox<String> rtSource = combo("rt-source", List.of());
    private final JComboBox<String> rtLot = combo("rt-lot", List.of());
    private final JTextField rtQty = field("rt-qty", 6, "Số lượng");
    private final JTextField rtReason = field("rt-reason", 18, "Lý do hoàn trả");
    private transient String selected;

    public VoucherPanel(ScreenContext ctx) {
        super(new BorderLayout()); setOpaque(false); setName("screen-NHAP_XUAT");
        this.ctx = ctx; this.store = ctx.store();
        list = Kit.list("voucher-list", Voucher::id, v -> Kit.Spec.block(v.id(), partnerName(v),
                DATE_TIME.format(v.time()) + "  ·  " + v.lines().size() + " dòng  ·  " + store.staff().name(v.staffId()), typeTag(v.type())), 7);
        list.onSelect(v -> { selected = v.id(); render(); });
        lines = new DemoTable<>("voucher-lines", List.of(
                new Column<VoucherLine>("Mặt hàng", l -> supply().lot(l.lotId()).flatMap(x -> supply().item(x.itemId())).map(Item::name).orElse("—"), 170),
                new Column<VoucherLine>("Lô", l -> supply().lot(l.lotId()).map(Lot::lotNo).orElse("—"), 100),
                new Column<VoucherLine>("HSD", l -> supply().lot(l.lotId()).map(x -> x.expiry() == null ? "Chưa rõ" : DATE.format(x.expiry())).orElse("—"), 90),
                new Column<VoucherLine>("Số lượng", l -> new DemoTable.Strong(String.valueOf(l.quantity())), 80)), VoucherLine::lotId, 4);
        tabs.onChange(i -> { selected = null; forms.show(String.valueOf(i)); refresh(); });
        outItem.addActionListener(e -> updateAvail()); rtSource.addActionListener(e -> updateReturnLots());
        JButton in = icon(primary("Lập phiếu nhập", this::receive), "plus", Color.WHITE); in.setName("in-save");
        JButton out = icon(primary("Lập phiếu xuất (FEFO)", this::issue), "exchange", Color.WHITE); out.setName("out-save");
        JButton rt = icon(primary("Lập phiếu hoàn trả", this::giveBack), "exchange", Color.WHITE); rt.setName("rt-save");
        forms.addCard(column(10, form("Mặt hàng *", inItem, "Nhà cung cấp *", inSupplier), pair(form("Số lô *", inLot), form("Số lượng *", inQty)),
                form("Hạn dùng (bắt buộc với thuốc)", inExpiry), buttons(in)), "0");
        forms.addCard(column(10, form("Mặt hàng *", outItem), outAvail, pair(form("Số lượng *", outQty), form("Nơi / người nhận *", outReceiver)),
                form("Lý do *", outReason), buttons(out)), "1");
        forms.addCard(column(10, form("Phiếu xuất gốc *", rtSource, "Lô *", rtLot), pair(form("Số lượng *", rtQty), form("Lý do *", rtReason)), buttons(rt)), "2");
        add(Kit.page(this, "Nhập / xuất / hoàn trả", "Phiếu nhập từ nhà cung cấp, xuất theo FEFO (hết hạn sớm xuất trước), hoàn trả theo phiếu xuất", ctx, "voucher",
                "Phiếu chỉ đổi tồn trong bộ nhớ phiên; không xuất lô hết hạn, không vượt tồn khả dụng.",
                Kit.twoCol(Kit.card("Phiếu", null, column(10, tabs, list)),
                        column(Theme.GAP, Kit.card("Chi tiết phiếu", null, detail), Kit.card("Lập phiếu mới (mô phỏng)", null, column(10, forms, feedback)))
                        , 0.44)), BorderLayout.NORTH);
        store.addListener(this::refresh);
        refresh();
    }
    private SupplySlice supply() { return store.supply(); }
    private String partnerName(Voucher v) { return v.type() == VoucherType.NHAP ? supply().supplierName(v.partner()) : v.partner(); }
    static Tag typeTag(VoucherType t) { return new Tag(t.label(), t == VoucherType.NHAP ? Theme.GREEN : t == VoucherType.XUAT ? Theme.color("0369A1") : Theme.PURPLE); }
    private void refresh() {
        VoucherType t = VoucherType.values()[tabs.selected()];
        List<Voucher> rows = supply().vouchers(t);
        list.setRows(rows, "Chưa có " + t.label().toLowerCase());
        if (selected == null && !rows.isEmpty()) selected = rows.get(0).id();
        replaceItems(inItem, supply().items().stream().map(i -> i.id() + " · " + i.name()).toList());
        replaceItems(outItem, supply().items().stream().map(i -> i.id() + " · " + i.name()).toList());
        replaceItems(inSupplier, supply().suppliers().stream().filter(s -> s.status() == SupplierStatus.DANG_HOP_TAC).map(s -> s.id() + " · " + s.name()).toList());
        replaceItems(rtSource, supply().vouchers(VoucherType.XUAT).stream().map(v -> v.id() + " · " + v.partner()).toList());
        updateAvail(); updateReturnLots(); render();
    }
    private void updateAvail() {
        String id = Kit.idOf(outItem.getSelectedItem());
        supply().item(id).ifPresent(i -> outAvail.setText("Tồn khả dụng: " + supply().available(id) + " " + i.unit() + " · lô hết hạn không được xuất"));
    }
    private void updateReturnLots() {
        String src = Kit.idOf(rtSource.getSelectedItem());
        replaceItems(rtLot, supply().voucher(src).map(v -> v.lines().stream().map(l -> l.lotId() + " · " + supply().lot(l.lotId()).map(Lot::lotNo).orElse("")
                + " (còn hoàn được " + supply().returnable(src, l.lotId()) + ")").toList()).orElse(List.of()));
    }
    private void render() {
        Voucher v = supply().voucher(selected).orElse(null);
        if (v == null) { Kit.fill(detail, Kit.empty("Chọn một phiếu để xem dòng chi tiết")); return; }
        if (list.selected().map(Voucher::id).filter(selected::equals).isEmpty()) list.select(selected);
        lines.setRows(v.lines(), "—");
        Kit.fill(detail, column(10, Kit.head("voucher", v.id() + " · " + v.type().label(), DATE_TIME.format(v.time()), typeTag(v.type())),
                Kit.info("voucher-info", v.type() == VoucherType.NHAP ? "Nhà cung cấp" : "Nơi / người nhận", partnerName(v),
                        "Người lập", store.staff().name(v.staffId()), "Lý do", v.reason(), "Phiếu gốc", v.sourceVoucherId()),
                lines));
    }
    private void receive() {
        if (!inExpiry.getText().isBlank() && parseDate(inExpiry.getText()) == null) { fail(feedback, "Hạn dùng không hợp lệ (dd/MM/yyyy)"); return; }
        var r = supply().receive(Kit.idOf(inItem.getSelectedItem()), Kit.idOf(inSupplier.getSelectedItem()), inLot.getText(),
                Kit.parseInt(inQty.getText()), inExpiry.getText().isBlank() ? null : parseDate(inExpiry.getText()));
        result(feedback, r); if (r.ok()) { selected = r.value().id(); inLot.setText(""); inQty.setText(""); inExpiry.setText(""); refresh(); }
    }
    private void issue() {
        if (!ctx.dialogs().confirm(this, "Lập phiếu xuất", "Xuất " + outQty.getText() + " từ " + outItem.getSelectedItem() + " theo FEFO?")) { info(feedback, "Đã hủy thao tác"); return; }
        var r = supply().issue(Kit.idOf(outItem.getSelectedItem()), Kit.parseInt(outQty.getText()), outReceiver.getText(), outReason.getText());
        result(feedback, r); if (r.ok()) { selected = r.value().id(); outQty.setText(""); refresh(); }
    }
    private void giveBack() {
        var r = supply().giveBack(Kit.idOf(rtSource.getSelectedItem()), Kit.idOf(rtLot.getSelectedItem()), Kit.parseInt(rtQty.getText()), rtReason.getText());
        result(feedback, r); if (r.ok()) { selected = r.value().id(); rtQty.setText(""); refresh(); }
    }
}
