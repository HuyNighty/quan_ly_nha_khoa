package com.manage.nhakhoa.desktop.screens;

import com.manage.nhakhoa.desktop.MenuDefinition;
import com.manage.nhakhoa.desktop.demo.DemoClinicStore;
import com.manage.nhakhoa.desktop.demo.SupplySlice;
import com.manage.nhakhoa.desktop.demo.SupplySlice.*;
import com.manage.nhakhoa.desktop.ui.CardList;
import com.manage.nhakhoa.desktop.ui.DemoTable.Tag;
import com.manage.nhakhoa.desktop.ui.Flat;
import com.manage.nhakhoa.desktop.ui.Theme;
import java.awt.*;
import java.util.List;
import javax.swing.*;
import static com.manage.nhakhoa.desktop.ui.DemoWidgets.*;
import static com.manage.nhakhoa.desktop.ui.UiComponents.*;

/** Tổ chức · Nhà cung cấp (NHA_CUNG_CAP): liên hệ giả, tình trạng hợp tác, phiếu nhập liên quan. Không có cột mã số thuế (Entity chưa có). */
public final class SupplierPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private final transient ScreenContext ctx;
    private final transient DemoClinicStore store;
    private final Flat.SearchBar search = new Flat.SearchBar("sup-search", "Tìm mã, tên, email nhà cung cấp…");
    private final Flat.Segmented filter = new Flat.Segmented("sup-filter", List.of("Tất cả", "Đang hợp tác", "Ngừng hợp tác"), null);
    private final CardList<Supplier> list;
    private final JLabel count = label(" ", 12, false, Theme.MUTED);
    private final JPanel detail = Kit.holder("sup-detail");
    private final JLabel feedback = feedback("sup-feedback");
    private transient String selected;

    public SupplierPanel(ScreenContext ctx) {
        super(new BorderLayout()); setOpaque(false); setName("screen-NHA_CUNG_CAP");
        this.ctx = ctx; this.store = ctx.store();
        list = Kit.list("sup-list", Supplier::id, s -> Kit.Spec.person(s.name(), s.id() + "  ·  " + s.email(), tag(s.status()), s.name(), s.id()), 6);
        list.onSelect(s -> { selected = s.id(); render(); });
        search.field().getDocument().addDocumentListener(SimpleDocumentListener.of(this::refreshList));
        search.onSubmit(() -> list.selectFirst()); search.bindShortcut(this);
        filter.onChange(i -> refreshList());
        add(Kit.page(this, "Nhà cung cấp", "Đối tác cung ứng thuốc & vật tư, tình trạng hợp tác và lịch sử phiếu nhập", ctx, "sup",
                "Địa chỉ, SĐT, email là dữ liệu giả; đổi tình trạng chỉ đổi bộ nhớ phiên.",
                Kit.twoCol(Kit.card("Danh sách nhà cung cấp", count, column(10, search, filter, list)),
                        Kit.card("Chi tiết", null, column(10, detail, feedback)), 0.48)), BorderLayout.NORTH);
        store.addListener(this::refresh);
        refresh();
    }
    static Tag tag(SupplierStatus s) { return new Tag(s.label(), s == SupplierStatus.DANG_HOP_TAC ? Theme.GREEN : Theme.MUTED); }
    private SupplySlice supply() { return store.supply(); }
    private void refresh() { refreshList(); render(); }
    private void refreshList() {
        String q = MenuDefinition.normalize(search.field().getText()); int f = filter.selected();
        List<Supplier> rows = supply().suppliers().stream().filter(s -> q.isEmpty() || MenuDefinition.normalize(s.id() + " " + s.name() + " " + s.email()).contains(q))
                .filter(s -> f == 0 || s.status() == SupplierStatus.values()[f - 1]).toList();
        list.setRows(rows, "Không có nhà cung cấp khớp bộ lọc");
        count.setText(rows.size() + " / " + supply().suppliers().size() + " nhà cung cấp");
    }
    private void render() {
        Supplier s = supply().supplier(selected).orElse(null);
        if (s == null) { Kit.fill(detail, Kit.empty("Chọn một nhà cung cấp để xem chi tiết")); return; }
        if (list.selected().map(Supplier::id).filter(selected::equals).isEmpty()) list.select(selected);
        boolean active = s.status() == SupplierStatus.DANG_HOP_TAC;
        JButton toggle = active ? danger("Ngừng hợp tác", this::toggle) : primary("Hợp tác lại", this::toggle); toggle.setName("sup-toggle");
        JPanel receipts = new JPanel(); receipts.setOpaque(false); receipts.setLayout(new BoxLayout(receipts, BoxLayout.Y_AXIS));
        var rs = supply().receiptsOf(s.id());
        if (rs.isEmpty()) receipts.add(label("Chưa có phiếu nhập", 12, false, Theme.MUTED));
        rs.forEach(v -> receipts.add(label("•  " + v.id() + " · " + DATE.format(v.time().toLocalDate()) + " · " + v.lines().size() + " dòng · " + v.reason(), 12, false, Theme.TEXT)));
        long items = supply().items().stream().filter(i -> s.id().equals(i.supplierId())).count();
        Kit.fill(detail, column(12, Kit.personHead("sup", s.name(), s.id(), s.id(), tag(s.status())),
                Kit.info("sup-info", "Địa chỉ", s.address(), "Điện thoại", s.phone(), "Email", s.email(), "Mặt hàng cung ứng", items + " mặt hàng"),
                Kit.separator(), Kit.section("Phiếu nhập liên quan"), receipts, Kit.separator(), buttons(toggle),
                label(active ? "Ngừng hợp tác sẽ chặn lập phiếu nhập mới từ nhà cung cấp này." : "Hợp tác lại để có thể lập phiếu nhập.", 11, false, Theme.MUTED)));
    }
    private void toggle() {
        Supplier s = supply().supplier(selected).orElse(null); if (s == null) return;
        SupplierStatus target = s.status() == SupplierStatus.DANG_HOP_TAC ? SupplierStatus.NGUNG_HOP_TAC : SupplierStatus.DANG_HOP_TAC;
        String r = ctx.dialogs().reason(this, target.label() + " · " + s.name(), "Lý do (5–200 ký tự):");
        if (r == null) { info(feedback, "Đã hủy thao tác"); return; }
        result(feedback, supply().setSupplierStatus(s.id(), target, r));
    }
}
