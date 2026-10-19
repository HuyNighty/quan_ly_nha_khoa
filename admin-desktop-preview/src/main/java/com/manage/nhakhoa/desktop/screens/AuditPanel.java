package com.manage.nhakhoa.desktop.screens;

import com.manage.nhakhoa.desktop.MenuDefinition;
import com.manage.nhakhoa.desktop.demo.DemoClinicStore;
import com.manage.nhakhoa.desktop.demo.DemoClinicStore.Activity;
import com.manage.nhakhoa.desktop.ui.DemoTable;
import com.manage.nhakhoa.desktop.ui.DemoTable.Column;
import com.manage.nhakhoa.desktop.ui.DemoTable.Tag;
import com.manage.nhakhoa.desktop.ui.Flat;
import com.manage.nhakhoa.desktop.ui.Theme;
import java.awt.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import javax.swing.*;
import static com.manage.nhakhoa.desktop.ui.DemoWidgets.*;
import static com.manage.nhakhoa.desktop.ui.UiComponents.*;

/**
 * Tổng kết · Nhật ký hệ thống (NHAT_KY): chỉ đọc; lọc theo người thao tác, loại đối tượng, ngày; không chứa dữ liệu sức khỏe.
 * Mọi thao tác mô phỏng trong phiên đều ghi vào đây.
 */
public final class AuditPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private static final String ALL = "Tất cả";
    private final transient DemoClinicStore store;
    private final Flat.SearchBar search = new Flat.SearchBar("audit-search", "Tìm thao tác, mã đối tượng, mã bệnh nhân…");
    private final JComboBox<String> actor = combo("audit-actor", List.of(ALL));
    private final JComboBox<String> type = combo("audit-type", List.of(ALL));
    private final Flat.Segmented range = new Flat.Segmented("audit-range", List.of("Hôm nay", "7 ngày", "Tất cả"), null);
    private final DemoTable<Activity> table;
    private final JLabel count = label(" ", 12, false, Theme.MUTED);
    private final JPanel detail = Kit.holder("audit-detail");

    public AuditPanel(ScreenContext ctx) {
        super(new BorderLayout()); setOpaque(false); setName("screen-NHAT_KY");
        this.store = ctx.store();
        count.setName("audit-count");
        table = new DemoTable<>("audit-table", List.of(
                new Column<Activity>("Thời gian", a -> DATE_TIME.format(a.time()), 120),
                new Column<Activity>("Người thao tác", Activity::actor, 130),
                new Column<Activity>("Thao tác", a -> new DemoTable.Strong(a.action()), 220),
                new Column<Activity>("Đối tượng", a -> a.objectType() + " · " + a.objectId(), 170),
                new Column<Activity>("Kết quả", a -> new Tag(a.outcome(), "Thành công".equals(a.outcome()) ? Theme.GREEN : Theme.RED), 100)),
                AuditPanel::key, 12);
        table.onSelect(this::show);
        range.select(2);
        search.field().getDocument().addDocumentListener(SimpleDocumentListener.of(this::refreshTable));
        search.bindShortcut(this);
        actor.addActionListener(e -> refreshTable()); type.addActionListener(e -> refreshTable()); range.onChange(i -> refreshTable());
        actor.setPreferredSize(new Dimension(190, Flat.FIELD_HEIGHT)); type.setPreferredSize(new Dimension(170, Flat.FIELD_HEIGHT));
        add(Kit.page(this, "Nhật ký thao tác", "Chỉ đọc · ghi người thao tác / hành động / đối tượng / kết quả · không chứa dữ liệu sức khỏe", ctx, "audit",
                "Nhật ký chỉ tồn tại trong phiên; đặt lại dữ liệu mô phỏng sẽ xóa các dòng phát sinh.",
                Kit.card("Bộ lọc", null, column(10, search, buttons(form("Người thao tác", actor), form("Loại đối tượng", type), form("Khoảng thời gian", range)))),
                Kit.twoCol(Kit.card("Sự kiện", count, table), Kit.card("Chi tiết sự kiện", null, detail), 0.68)), BorderLayout.NORTH);
        store.addListener(this::refresh);
        refresh();
    }
    private static String key(Activity a) { return a.time() + "|" + a.objectId() + "|" + a.action(); }
    private void refresh() {
        List<Activity> all = store.activities();
        keep(actor, new TreeSet<>(all.stream().map(Activity::actor).toList()));
        keep(type, new TreeSet<>(all.stream().map(Activity::objectType).toList()));
        refreshTable();
    }
    private static void keep(JComboBox<String> box, TreeSet<String> values) {
        Object cur = box.getSelectedItem();
        List<String> items = new ArrayList<>(); items.add(ALL); items.addAll(values);
        replaceItems(box, items);
        box.setSelectedItem(cur != null && items.contains(cur.toString()) ? cur : ALL);
    }
    private void refreshTable() {
        String q = MenuDefinition.normalize(search.field().getText());
        Object a = actor.getSelectedItem(), t = type.getSelectedItem();
        LocalDate d = store.today(); int r = range.selected();
        List<Activity> rows = store.activities().stream()
                .filter(x -> a == null || ALL.equals(a) || a.equals(x.actor()))
                .filter(x -> t == null || ALL.equals(t) || t.equals(x.objectType()))
                .filter(x -> r == 2 || (r == 0 ? x.time().toLocalDate().equals(d) : !x.time().toLocalDate().isBefore(d.minusDays(6))))
                .filter(x -> q.isEmpty() || MenuDefinition.normalize(x.action() + " " + x.objectId() + " " + x.objectType() + " " + x.actor()
                        + " " + (x.patientId() == null ? "" : x.patientId())).contains(q))
                .toList();
        table.setRows(rows, "Không có sự kiện khớp bộ lọc");
        count.setText(rows.size() + " / " + store.activities().size() + " sự kiện");
        if (rows.isEmpty()) Kit.fill(detail, Kit.empty("Chọn một sự kiện")); else { table.select(key(rows.get(0))); show(rows.get(0)); }
    }
    private void show(Activity x) {
        if (x == null) return;
        Kit.fill(detail, column(10, Kit.head("audit", x.action(), DATE_TIME.format(x.time()),
                        new Tag(x.outcome(), "Thành công".equals(x.outcome()) ? Theme.GREEN : Theme.RED)),
                Kit.info("audit-info", "Người thao tác", x.actor(), "Loại đối tượng", x.objectType(), "Mã đối tượng", x.objectId(),
                        "Bệnh nhân liên quan", x.patientId() == null ? "—" : x.patientId()),
                label("<html>Nhật ký kỹ thuật tối thiểu: không lưu chẩn đoán, kết quả hay nội dung bệnh án.</html>", 11, false, Theme.MUTED)));
    }
}
