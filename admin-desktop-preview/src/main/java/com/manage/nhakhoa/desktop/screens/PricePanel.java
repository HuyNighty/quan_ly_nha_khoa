package com.manage.nhakhoa.desktop.screens;

import com.manage.nhakhoa.desktop.MenuDefinition;
import com.manage.nhakhoa.desktop.demo.BillingSlice;
import com.manage.nhakhoa.desktop.demo.BillingSlice.*;
import com.manage.nhakhoa.desktop.demo.DemoClinicStore;
import com.manage.nhakhoa.desktop.ui.CardList;
import com.manage.nhakhoa.desktop.ui.DemoTable.Tag;
import com.manage.nhakhoa.desktop.ui.Flat;
import com.manage.nhakhoa.desktop.ui.Theme;
import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.swing.*;
import static com.manage.nhakhoa.desktop.ui.DemoWidgets.*;
import static com.manage.nhakhoa.desktop.ui.UiComponents.*;

/** Tổ chức · Bảng giá dịch vụ (BANG_GIA): lọc nhóm, sắp xếp, điều chỉnh giá mô phỏng không hồi tố hóa đơn đã lập. */
public final class PricePanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private static final String ALL = "Tất cả nhóm";
    private final transient ScreenContext ctx;
    private final transient DemoClinicStore store;
    private final Flat.SearchBar search = new Flat.SearchBar("price-search", "Tìm mã hoặc tên dịch vụ…");
    private final JComboBox<String> group = combo("price-group", List.of(ALL));
    private final Flat.Segmented sort = new Flat.Segmented("price-sort", List.of("Theo mã", "Giá tăng", "Giá giảm"), null);
    private final CardList<Service> list;
    private final JLabel count = label(" ", 12, false, Theme.MUTED);
    private final JPanel detail = Kit.holder("price-detail");
    private final JLabel feedback = feedback("price-feedback");
    private final JTextField newPrice = field("price-new", 12, "Ví dụ: 450000");
    private final JTextField reason = field("price-reason", 20, "Lý do điều chỉnh");
    private transient String selected;

    public PricePanel(ScreenContext ctx) {
        super(new BorderLayout()); setOpaque(false); setName("screen-BANG_GIA");
        this.ctx = ctx; this.store = ctx.store();
        for (BillingSlice.Group g : BillingSlice.Group.values()) group.addItem(g.label());
        list = Kit.list("price-list", Service::id, s -> Kit.Spec.block(s.id(), s.name(), s.group().label() + "  ·  " + Kit.money(s.price()),
                s.active() ? new Tag("Đang áp dụng", Theme.GREEN) : new Tag("Ngừng áp dụng", Theme.MUTED)), 8);
        list.onSelect(s -> { selected = s.id(); render(); });
        search.field().getDocument().addDocumentListener(SimpleDocumentListener.of(this::refreshList));
        search.onSubmit(() -> list.selectFirst()); search.bindShortcut(this);
        group.addActionListener(e -> refreshList()); sort.onChange(i -> refreshList());
        group.setPreferredSize(new Dimension(160, Flat.FIELD_HEIGHT));
        JPanel filters = new JPanel(new BorderLayout(8, 0)); filters.setOpaque(false); filters.add(sort, BorderLayout.CENTER); filters.add(group, BorderLayout.EAST);
        add(Kit.page(this, "Bảng giá dịch vụ", "Danh mục dịch vụ, nhóm, đơn giá và điều kiện áp dụng", ctx, "price",
                "Đổi giá chỉ áp dụng cho hóa đơn lập sau; hóa đơn mẫu đã lập giữ nguyên đơn giá.",
                Kit.twoCol(Kit.card("Dịch vụ", count, column(10, search, filters, list)),
                        Kit.card("Chi tiết dịch vụ", null, column(10, detail, feedback)), 0.52)), BorderLayout.NORTH);
        store.addListener(this::refresh);
        refresh();
    }
    private BillingSlice billing() { return store.billing(); }
    private void refresh() { refreshList(); render(); }
    private void refreshList() {
        String q = MenuDefinition.normalize(search.field().getText()); String g = (String) group.getSelectedItem();
        List<Service> rows = new ArrayList<>(billing().services().stream()
                .filter(s -> q.isEmpty() || MenuDefinition.normalize(s.id() + " " + s.name()).contains(q))
                .filter(s -> g == null || ALL.equals(g) || s.group().label().equals(g)).toList());
        if (sort.selected() == 1) rows.sort(Comparator.comparingLong(Service::price));
        if (sort.selected() == 2) rows.sort(Comparator.comparingLong(Service::price).reversed());
        list.setRows(rows, "Không có dịch vụ khớp bộ lọc");
        count.setText(rows.size() + " dịch vụ · " + billing().services().stream().filter(Service::active).count() + " đang áp dụng");
    }
    private void render() {
        Service s = billing().service(selected).orElse(null);
        if (s == null) { Kit.fill(detail, Kit.empty("Chọn một dịch vụ để xem chi tiết và điều chỉnh giá")); return; }
        if (list.selected().map(Service::id).filter(selected::equals).isEmpty()) list.select(selected);
        long used = billing().invoices().stream().flatMap(i -> i.lines().stream()).filter(l -> l.serviceId().equals(s.id())).count();
        JButton save = primary("Lưu giá mới (mô phỏng)", this::save); save.setName("price-save"); save.setEnabled(s.active());
        save.setToolTipText(s.active() ? "Áp dụng cho hóa đơn lập sau" : "Dịch vụ đã ngừng áp dụng");
        newPrice.setText(""); reason.setText("");
        Kit.fill(detail, column(12, Kit.head("price", s.name(), s.id() + " · " + s.group().label(),
                        s.active() ? new Tag("Đang áp dụng", Theme.GREEN) : new Tag("Ngừng áp dụng", Theme.MUTED)),
                Kit.info("price-info", "Đơn giá hiện tại", Kit.money(s.price()), "Nhóm", s.group().label(), "Điều kiện áp dụng", s.condition(),
                        "Dòng hóa đơn đang dùng", used + " dòng (giữ đơn giá đã chốt)"),
                Kit.separator(), Kit.section("Điều chỉnh giá"), pair(form("Giá mới (đ) *", newPrice), form("Lý do *", reason)), buttons(save)));
    }
    private void save() {
        long p = Kit.parseMoney(newPrice.getText());
        if (p <= 0) { fail(feedback, "Nhập giá mới bằng số, ví dụ 450000"); return; }
        if (!ctx.dialogs().confirm(this, "Điều chỉnh giá", "Đổi giá " + selected + " thành " + Kit.money(p) + "? Hóa đơn đã lập giữ đơn giá cũ.")) {
            info(feedback, "Đã hủy thao tác"); return;
        }
        result(feedback, billing().changePrice(selected, p, reason.getText()));
    }
}
