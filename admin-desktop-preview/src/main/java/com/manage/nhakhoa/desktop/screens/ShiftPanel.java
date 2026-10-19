package com.manage.nhakhoa.desktop.screens;

import com.manage.nhakhoa.desktop.demo.DemoClinicStore;
import com.manage.nhakhoa.desktop.demo.StaffSlice;
import com.manage.nhakhoa.desktop.demo.StaffSlice.*;
import com.manage.nhakhoa.desktop.ui.CardList;
import com.manage.nhakhoa.desktop.ui.DemoTable.Tag;
import com.manage.nhakhoa.desktop.ui.Flat;
import com.manage.nhakhoa.desktop.ui.Theme;
import java.awt.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import static com.manage.nhakhoa.desktop.ui.DemoWidgets.*;
import static com.manage.nhakhoa.desktop.ui.UiComponents.*;

/**
 * Lập kế hoạch · Phân công & lịch làm việc (PHAN_CONG): 3 ca theo ngày, định mức BS/YT/LT, thiếu nhân sự tính từ phân công hợp lệ
 * (DISTINCT, đang làm việc). Thiếu người ≠ vắng mặt thực tế; không phải chấm công.
 */
public final class ShiftPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private final transient ScreenContext ctx;
    private final transient DemoClinicStore store;
    private transient LocalDate date;
    private final JLabel dateLabel = label("", 15, true, Theme.TEXT);
    private final JPanel shiftTiles = new JPanel(new GridLayout(1, 3, 12, 0));
    private final Flat.Segmented shiftPick = new Flat.Segmented("shift-pick", List.of("Ca sáng", "Ca chiều", "Ca tối"), null);
    private final CardList<Assignment> list;
    private final JLabel quotaLine = feedback("shift-quota");
    private final JComboBox<String> staffBox = combo("shift-staff", List.of());
    private final JTextField task = field("shift-task", 16, "Nhiệm vụ, ví dụ: Phụ tá phòng 2");
    private final JLabel feedback = feedback("shift-feedback");

    public ShiftPanel(ScreenContext ctx) {
        super(new BorderLayout()); setOpaque(false); setName("screen-PHAN_CONG");
        this.ctx = ctx; this.store = ctx.store(); this.date = store.today();
        list = Kit.list("shift-list", Assignment::id, a -> {
            Staff s = store.staff().staff(a.staffId()).orElseThrow();
            return Kit.Spec.person(s.name(), s.id() + "  ·  " + a.task(), new Tag(s.role().label(), s.role() == Role.BS ? Theme.PRIMARY_TEXT
                    : s.role() == Role.YT ? Theme.color("0369A1") : Theme.PURPLE), s.name(), s.id());
        }, 6);
        JButton prev = icon(secondary("", () -> setDate(date.minusDays(1))), "left", Theme.TEXT); prev.setName("shift-prev");
        JButton next = icon(secondary("", () -> setDate(date.plusDays(1))), "right", Theme.TEXT); next.setName("shift-next");
        JButton today = secondary("Hôm nay", () -> setDate(store.today())); today.setName("shift-today");
        JButton add = icon(primary("Phân công vào ca", this::assign), "plus", Color.WHITE); add.setName("shift-assign");
        JButton remove = icon(danger("Gỡ khỏi ca", this::unassign), "close", Color.WHITE); remove.setName("shift-unassign");
        shiftPick.onChange(i -> refresh());
        shiftTiles.setOpaque(false);
        staffBox.setPreferredSize(new Dimension(260, Flat.FIELD_HEIGHT));
        add(Kit.page(this, "Phân công & lịch làm việc", "3 ca/ngày · định mức BS / YT / LT · tối đa 2 ca/người/ngày (giả định V1)", ctx, "shift",
                "Thiếu nhân sự tính từ phân công, không phải chấm công hay vắng mặt thực tế.",
                buttons(prev, dateLabel, next, today), shiftTiles,
                Kit.twoCol(Kit.card("Nhân sự trong ca", null, column(10, shiftPick, quotaLine, list, buttons(remove))),
                        Kit.card("Phân công thêm", null, column(10, form("Nhân viên *", staffBox, "Nhiệm vụ", task), buttons(add), feedback,
                                label("<html>Chặn khi: đã có trong ca · quá 2 ca/ngày · đã nghỉ việc<br>· ngày đã qua · bác sĩ không trực hôm nay</html>", 11, false, Theme.MUTED))), 0.58)),
                BorderLayout.NORTH);
        store.addListener(this::refresh);
        refresh();
    }
    private StaffSlice staff() { return store.staff(); }
    private ShiftKind shift() { return ShiftKind.values()[shiftPick.selected()]; }
    public void setDate(LocalDate d) { date = d; refresh(); }
    private void refresh() {
        dateLabel.setText("  " + Theme.day(date.atStartOfDay(Theme.ZONE).toInstant()) + (date.equals(store.today()) ? "  ·  Hôm nay" : "") + "  ");
        shiftTiles.removeAll();
        for (ShiftKind k : ShiftKind.values()) shiftTiles.add(tile(k));
        shiftTiles.revalidate(); shiftTiles.repaint();
        list.setRows(staff().assignments(date, shift()), "Chưa có phân công trong " + shift().label().toLowerCase());
        Map<Role, Integer> q = StaffSlice.quota(shift());
        StringBuilder sb = new StringBuilder(shift().label() + " " + shift().hours() + " · ");
        for (Role r : q.keySet()) sb.append(r.name()).append(' ').append(staff().count(date, shift(), r)).append('/').append(q.get(r)).append("   ");
        long miss = staff().missingTotal(date, shift());
        if (miss > 0) warn(quotaLine, sb + "· thiếu " + miss + " theo định mức"); else hint(quotaLine, sb + "· đủ định mức");
        replaceItems(staffBox, staff().active().stream().filter(s -> s.role() == Role.BS || s.role() == Role.YT || s.role() == Role.LT)
                .map(s -> s.id() + " · " + s.name() + " (" + s.role().name() + ")").toList());
    }
    private JComponent tile(ShiftKind k) {
        long miss = staff().missingTotal(date, k);
        boolean active = k == shift();
        Tile t = new Tile(new BorderLayout(), miss > 0 ? Theme.RED_LIGHT : Theme.CARD, active ? Theme.PRIMARY : Theme.BORDER);
        t.setBorder(new EmptyBorder(10, 12, 10, 12)); t.setName("shift-tile-" + k.name());
        Map<Role, Integer> q = StaffSlice.quota(k);
        StringBuilder detail = new StringBuilder();
        for (Role r : q.keySet()) detail.append(r.name()).append(' ').append(staff().count(date, k, r)).append('/').append(q.get(r)).append("  ");
        JLabel status = label(miss == 0 ? "Đủ định mức" : "Thiếu " + miss + " người", 12, true, miss == 0 ? Theme.GREEN : Theme.RED);
        status.setName("shift-tile-status-" + k.name());
        status.setIcon(new com.manage.nhakhoa.desktop.ui.UiIcons(miss == 0 ? "check" : "alert", miss == 0 ? Theme.GREEN : Theme.RED, 14));
        t.add(column(4, label(k.label() + " · " + k.hours(), 13, true, Theme.TEXT), label(detail.toString(), 12, false, Theme.MUTED), status));
        t.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        t.addMouseListener(new java.awt.event.MouseAdapter() { @Override public void mouseClicked(java.awt.event.MouseEvent e) { shiftPick.select(k.ordinal()); } });
        return t;
    }
    private void assign() {
        String id = Kit.idOf(staffBox.getSelectedItem());
        String b = staff().assignBlocker(date, shift(), id);
        if (b != null) { fail(feedback, b); return; }
        if (!ctx.dialogs().confirm(this, "Phân công", "Phân công " + staff().name(id) + " vào " + shift().label() + " ngày " + DATE.format(date) + "?")) {
            info(feedback, "Đã hủy thao tác"); return;
        }
        var r = staff().assign(date, shift(), id, task.getText()); result(feedback, r); if (r.ok()) task.setText("");
    }
    private void unassign() {
        Assignment a = list.selected().orElse(null);
        if (a == null) { fail(feedback, "Chọn một người trong ca để gỡ"); return; }
        String r = ctx.dialogs().reason(this, "Gỡ " + staff().name(a.staffId()), "Lý do gỡ khỏi ca (5–200 ký tự):");
        if (r == null) { info(feedback, "Đã hủy thao tác"); return; }
        result(feedback, staff().unassign(a.id(), r));
    }
}
