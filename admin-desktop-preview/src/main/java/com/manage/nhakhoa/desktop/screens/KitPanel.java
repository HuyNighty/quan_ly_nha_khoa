package com.manage.nhakhoa.desktop.screens;

import com.manage.nhakhoa.desktop.demo.DemoClinicStore;
import com.manage.nhakhoa.desktop.demo.StaffSlice.Role;
import com.manage.nhakhoa.desktop.demo.SupplySlice;
import com.manage.nhakhoa.desktop.demo.SupplySlice.*;
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
 * Cung ứng · Hộp cấp cứu (CAP_CUU): danh mục MẪU, tình trạng từng mục, chu kỳ kiểm tra 7 ngày, ghi nhận kiểm tra.
 * Không bao giờ hiển thị "đạt an toàn"; mục mẫu không chính thức không tính vào tình trạng chung.
 */
public final class KitPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private final transient DemoClinicStore store;
    private final JPanel stats = Kit.stats();
    private final JLabel summary = feedback("kit-summary");
    private final DemoTable<KitItem> items;
    private final DemoTable<KitCheck> checks;
    private final JLabel feedback = feedback("kit-feedback");
    private final JComboBox<String> actor;
    private final JComboBox<String> item = combo("kit-item", List.of());
    private final JTextField qty = field("kit-qty", 6, "Số lượng thực tế");
    private final JTextField expiry = field("kit-expiry", 10, "HSD dd/MM/yyyy");

    public KitPanel(ScreenContext ctx) {
        super(new BorderLayout()); setOpaque(false); setName("screen-CAP_CUU");
        this.store = ctx.store();
        actor = Kit.actor("kit-actor", store.staff(), s -> s.role() == Role.YT || s.role() == Role.KHO || s.role() == Role.QL || s.role() == Role.BS, "NV006");
        actor.setPreferredSize(new Dimension(300, Flat.FIELD_HEIGHT));
        items = new DemoTable<>("kit-items", List.of(
                new Column<KitItem>("Mục", k -> new DemoTable.Strong(k.name()), 190),
                new Column<KitItem>("Danh mục", k -> k.official() ? "Danh mục mẫu" : "Mục tham khảo", 110),
                new Column<KitItem>("Tối thiểu", KitItem::minimum, 70), new Column<KitItem>("Hiện có", KitItem::quantity, 70),
                new Column<KitItem>("Hạn dùng", k -> k.expiry() == null ? (k.expiryRequired() ? "Chưa nhập" : "Không áp dụng") : DATE.format(k.expiry()), 100),
                new Column<KitItem>("Tình trạng", this::stateTag, 150)), KitItem::id, 6);
        items.onSelect(k -> { item.setSelectedItem(label(k)); qty.setText(String.valueOf(k.quantity())); expiry.setText(k.expiry() == null ? "" : DATE.format(k.expiry())); });
        checks = new DemoTable<>("kit-checks", List.of(new Column<KitCheck>("Mã", KitCheck::id, 80),
                new Column<KitCheck>("Thời gian", c -> DATE_TIME.format(c.time()), 120),
                new Column<KitCheck>("Người kiểm tra", c -> store.staff().name(c.checkerId()), 130), new Column<KitCheck>("Ghi chú", KitCheck::note, 200)), KitCheck::id, 5);
        JButton save = icon(primary("Ghi nhận kiểm tra", this::record), "check", Color.WHITE); save.setName("kit-record");
        add(Kit.page(this, "Hộp cấp cứu", "Danh mục mẫu · chu kỳ kiểm tra " + SupplySlice.KIT_CYCLE_DAYS + " ngày · không phải chứng nhận an toàn lâm sàng", ctx, "kit",
                "Danh mục là MẪU do nhóm dự án đặt; phòng khám thật phải dùng danh mục do người có thẩm quyền ban hành.",
                stats, summary,
                Kit.card("Danh mục & tình trạng", null, items),
                Kit.twoCol(Kit.card("Ghi nhận kiểm tra (mô phỏng)", null, column(10,
                                form("Người kiểm tra", actor), form("Mục *", item), pair(form("Số lượng *", qty), form("Hạn dùng", expiry)),
                                buttons(save), feedback)),
                        Kit.card("Lịch sử kiểm tra", null, checks), 0.45)), BorderLayout.NORTH);
        store.addListener(this::refresh);
        refresh();
    }
    private SupplySlice supply() { return store.supply(); }
    private static String label(KitItem k) { return k.id() + " · " + k.name(); }
    private Tag stateTag(KitItem k) {
        KitState s = supply().kitState(k);
        Color c = switch (s) { case DU -> Theme.GREEN; case CAN_HAN -> Theme.ORANGE; case THIEU, HET_HAN -> Theme.RED; case CHUA_DU_THONG_TIN -> Theme.MUTED; };
        return new Tag(s.label(), c);
    }
    private void refresh() {
        List<KitItem> all = supply().kit();
        stats.removeAll();
        stats.add(stat("kit-stat-items", "Mục trong danh mục", all.size(), Theme.TEXT, null, false));
        stats.add(stat("kit-stat-short", "Thiếu / hết hạn", (int) all.stream().filter(k -> k.official())
                .filter(k -> supply().kitState(k) == KitState.THIEU || supply().kitState(k) == KitState.HET_HAN).count(), Theme.RED, null, false));
        stats.add(stat("kit-stat-near", "Cận hạn", (int) all.stream().filter(k -> supply().kitState(k) == KitState.CAN_HAN).count(), Theme.ORANGE, null, false));
        stats.add(stat("kit-stat-unknown", "Chưa đủ thông tin", (int) all.stream().filter(k -> supply().kitState(k) == KitState.CHUA_DU_THONG_TIN).count(), Theme.MUTED, null, false));
        stats.revalidate();
        String last = supply().lastCheck().map(c -> "Lần kiểm tra gần nhất: " + DATE_TIME.format(c.time()) + " · " + store.staff().name(c.checkerId())).orElse("Chưa có lần kiểm tra");
        String text = "Tình trạng chung: " + supply().kitSummary() + " · " + last + (supply().checkDue() ? " · ĐẾN HẠN KIỂM TRA" : "");
        if (supply().kitSummary().startsWith("ĐỦ") && !supply().checkDue()) info(summary, text); else warn(summary, text);
        items.setRows(all, "Chưa có danh mục");
        checks.setRows(supply().checks(), "Chưa có lần kiểm tra");
        Object keep = item.getSelectedItem();
        replaceItems(item, all.stream().map(KitPanel::label).toList());
        if (keep != null) item.setSelectedItem(keep);
    }
    private void record() {
        if (!expiry.getText().isBlank() && parseDate(expiry.getText()) == null) { fail(feedback, "Hạn dùng không hợp lệ (dd/MM/yyyy)"); return; }
        int q = Kit.parseInt(qty.getText());
        if (q == Integer.MIN_VALUE) { fail(feedback, "Nhập số lượng bằng số"); return; }
        result(feedback, supply().recordKitItem(Kit.idOf(item.getSelectedItem()), q, expiry.getText().isBlank() ? null : parseDate(expiry.getText()),
                Kit.idOf(actor.getSelectedItem())));
    }
}
