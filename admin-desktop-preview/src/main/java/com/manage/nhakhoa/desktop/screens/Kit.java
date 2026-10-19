package com.manage.nhakhoa.desktop.screens;

import com.manage.nhakhoa.desktop.demo.StaffSlice;
import com.manage.nhakhoa.desktop.demo.StaffSlice.Staff;
import com.manage.nhakhoa.desktop.ui.CardList;
import com.manage.nhakhoa.desktop.ui.DemoTable.Tag;
import com.manage.nhakhoa.desktop.ui.Flat;
import com.manage.nhakhoa.desktop.ui.Theme;
import java.awt.*;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import static com.manage.nhakhoa.desktop.ui.DemoWidgets.*;
import static com.manage.nhakhoa.desktop.ui.UiComponents.*;

/** Bộ khung dùng chung cho các màn mô phỏng master–detail: trang, thẻ danh sách, thẻ chi tiết, dòng thẻ, người thao tác. */
final class Kit {
    private Kit() { }

    /** Mô tả một dòng thẻ: avatar (nếu có tên người) hoặc khối chữ đầu dòng, tiêu đề, mô tả, viên trạng thái. */
    record Spec(String title, String subtitle, Tag tag, String avatarName, String avatarKey, String lead) {
        static Spec person(String title, String subtitle, Tag tag, String name, String key) { return new Spec(title, subtitle, tag, name, key, null); }
        static Spec block(String lead, String title, String subtitle, Tag tag) { return new Spec(title, subtitle, tag, null, null, lead); }
    }
    static <T> CardList<T> list(String name, Function<T, String> idOf, Function<T, Spec> spec, int maxRows) {
        return new CardList<>(name, idOf, (g, item, r, selected, hover) -> {
            Spec s = spec.apply(item); int cy = r.height / 2, x = 18, textX;
            if (s.avatarName() != null) {
                Flat.paintAvatar(g, x, cy - 21, 42, s.avatarName(), Flat.avatarTone(s.avatarKey())); textX = x + 56;
            } else if (s.lead() != null) {
                g.setFont(Theme.font(13, true)); FontMetrics fm = g.getFontMetrics();
                int w = Math.max(64, fm.stringWidth(s.lead()) + 20);
                g.setColor(selected ? Flat.tint(Theme.PRIMARY, 0.8) : Theme.SOFT); g.fillRoundRect(x, 10, w, r.height - 20, 10, 10);
                g.setColor(Theme.TEXT); g.drawString(s.lead(), x + (w - fm.stringWidth(s.lead())) / 2, cy + fm.getAscent() / 2 - 1);
                textX = x + w + 14;
            } else textX = x + 4;
            int right = r.width - 16, nameRight = right;
            if (s.tag() != null) nameRight = CardList.pillRight(g, s.tag().text(), right, cy - 9, s.tag().color()) - 10;
            CardList.text(g, s.title(), textX, cy - 4, nameRight - textX, Theme.font(14, true), Theme.TEXT);
            CardList.text(g, s.subtitle(), textX, cy + 16, right - textX, Theme.font(12, false), Theme.MUTED);
        }, 66, maxRows);
    }

    static JPanel page(JComponent self, String title, String subtitle, ScreenContext ctx, String key, String bannerText, JComponent... body) {
        JComponent[] all = new JComponent[body.length + 2];
        all[0] = new ScreenHeader(title, subtitle, buttons(ctx.resetButton(self, key + "-reset")));
        all[1] = banner(key + "-banner", bannerText);
        System.arraycopy(body, 0, all, 2, body.length);
        return screen(all);
    }
    /** Thẻ trắng có tiêu đề + phụ chú (đếm) bên phải. */
    static JPanel card(String title, JComponent accessory, JComponent body) { return new UiCard(title, accessory, body); }
    static JPanel stats(JComponent... tiles) {
        JPanel p = new JPanel(new GridLayout(1, tiles.length, 10, 0)); p.setOpaque(false);
        for (JComponent t : tiles) p.add(t);
        return p;
    }
    /** Lưới nhãn–giá trị từ cặp chuỗi; giá trị được đặt tên prefix-i để kiểm thử. */
    static JPanel info(String prefix, String... pairs) {
        Object[] objs = new Object[pairs.length];
        for (int i = 0; i < pairs.length; i += 2) {
            objs[i] = pairs[i];
            JLabel v = label("<html>" + escape(pairs[i + 1] == null || pairs[i + 1].isBlank() ? "—" : pairs[i + 1]) + "</html>", 13, true, Theme.TEXT);
            v.setName(prefix + "-" + (i / 2)); objs[i + 1] = v;
        }
        return Flat.infoGrid(objs);
    }
    /** Header chi tiết: tiêu đề lớn + viên trạng thái + phụ đề. */
    static JPanel head(String name, String title, String subtitle, Tag tag) {
        JLabel t = label(title, 18, true, Theme.TEXT); t.setName(name + "-detail-title");
        JPanel row = new JPanel(new BorderLayout(10, 0)); row.setOpaque(false); row.add(t, BorderLayout.CENTER);
        if (tag != null) { Flat.Pill p = new Flat.Pill(tag.text(), tag.color()); p.setName(name + "-detail-status"); row.add(buttons(p), BorderLayout.EAST); }
        return column(4, row, label(subtitle, 12, false, Theme.MUTED));
    }
    static JPanel personHead(String name, String person, String key, String subtitle, Tag tag) {
        Flat.Avatar a = new Flat.Avatar(56); a.setPerson(person, key);
        JPanel box = new JPanel(new BorderLayout(14, 0)); box.setOpaque(false);
        JPanel ab = new JPanel(new BorderLayout()); ab.setOpaque(false); ab.add(a, BorderLayout.NORTH);
        box.add(ab, BorderLayout.WEST); box.add(head(name, person, subtitle, tag), BorderLayout.CENTER);
        return box;
    }
    static JComponent separator() {
        JSeparator s = new JSeparator(); s.setForeground(Theme.BORDER); s.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1)); return s;
    }
    static JLabel section(String text) { return label(text, 12, true, Theme.color("475569")); }
    static JLabel empty(String text) { JLabel l = label(text, 13, false, Theme.MUTED); l.setBorder(new EmptyBorder(20, 0, 20, 0)); return l; }
    /** Thay nội dung panel chi tiết. */
    static void fill(JPanel holder, JComponent content) {
        holder.removeAll(); holder.add(content, BorderLayout.NORTH); holder.revalidate(); holder.repaint();
    }
    static JPanel holder(String name) { JPanel p = new JPanel(new BorderLayout()); p.setOpaque(false); p.setName(name); return p; }
    /** Combo "thao tác với tư cách" (mô phỏng người dùng khác để thử quy tắc người lập ≠ người duyệt). */
    static JComboBox<String> actor(String name, StaffSlice staff, Predicate<Staff> filter, String preferred) {
        List<String> items = staff.active().stream().filter(filter).map(s -> s.id() + " · " + s.name() + " (" + s.role().label() + ")").toList();
        JComboBox<String> c = combo(name, items);
        items.stream().filter(i -> i.startsWith(preferred + " ")).findFirst().ifPresent(c::setSelectedItem);
        c.setToolTipText("Thao tác với tư cách nhân viên này — mô phỏng, không có đăng nhập thật");
        return c;
    }
    static String idOf(Object item) { if (item == null) return null; String s = item.toString(); int i = s.indexOf(' '); return i < 0 ? s : s.substring(0, i); }
    static JPanel twoCol(JComponent left, JComponent right, double w) { return twoColumns(left, right, w); }
    static long parseMoney(String text) {
        try { return Long.parseLong(text == null ? "" : text.replaceAll("[^0-9]", "")); } catch (NumberFormatException e) { return -1; }
    }
    static int parseInt(String text) {
        try { return Integer.parseInt(text == null ? "" : text.trim()); } catch (NumberFormatException e) { return Integer.MIN_VALUE; }
    }
    static String money(long v) { return Theme.money(v); }
}
