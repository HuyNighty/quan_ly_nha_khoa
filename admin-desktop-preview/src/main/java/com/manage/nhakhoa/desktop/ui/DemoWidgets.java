package com.manage.nhakhoa.desktop.ui;

import com.manage.nhakhoa.desktop.demo.DemoClinicStore;
import com.manage.nhakhoa.desktop.model.DashboardViewModel.AppointmentStatus;
import java.awt.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import static com.manage.nhakhoa.desktop.ui.UiComponents.*;

/** Thành phần chung cho các màn mô phỏng UI-DEMO: banner, thông báo, form, timeline, bố cục. */
public final class DemoWidgets {
    public static final String NOTICE = "DỮ LIỆU MÔ PHỎNG · Không kết nối hệ thống thật";
    public static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);
    public static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    public static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private DemoWidgets() { }

    /** Nền bo góc tự vẽ (RoundedPanel trong UiComponents là sealed). */
    public static final class Tile extends JPanel {
        private static final long serialVersionUID = 1L;
        private final Color fill;
        private final Color line;
        public Tile(LayoutManager layout, Color fill, Color line) { super(layout); this.fill = fill; this.line = line; setOpaque(false); }
        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(fill); g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
            if (line != null) { g.setColor(line); g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12); }
            g.dispose(); super.paintComponent(graphics);
        }
    }

    /**
     * CardLayout co theo thẻ đang hiện: chiều cao = nội dung thẻ hiện tại, neo trên cùng.
     * Tránh khoảng trống lớn khi thẻ ngắn nằm cùng thẻ dài (form trực tiếp vs bảng lịch hẹn).
     */
    public static final class FitCards extends JPanel {
        private static final long serialVersionUID = 1L;
        private String current;
        public FitCards() { super(new CardLayout()); setOpaque(false); }
        public void addCard(JComponent content, String key) {
            JPanel holder = new JPanel(new BorderLayout()); holder.setOpaque(false); holder.add(content, BorderLayout.NORTH);
            add(holder, key); if (current == null) current = key;
        }
        public void show(String key) { current = key; ((CardLayout) getLayout()).show(this, key); revalidate(); repaint(); }
        public String current() { return current; }
        @Override public Dimension getPreferredSize() {
            for (Component c : getComponents()) if (c.isVisible()) { Dimension d = c.getPreferredSize(); return new Dimension(d.width, d.height); }
            return super.getPreferredSize();
        }
        @Override public Dimension getMaximumSize() { return new Dimension(Integer.MAX_VALUE, getPreferredSize().height); }
    }

    /** Băng nhãn mô phỏng mảnh, luôn hiện trên đầu mỗi màn. */
    public static JComponent banner(String name, String detail) {
        Tile t = new Tile(new BorderLayout(10, 0), Theme.ORANGE_LIGHT, Theme.color("FED7AA")); t.setName(name);
        t.setBorder(new EmptyBorder(7, 12, 7, 12));
        t.add(new JLabel(new UiIcons("info", Theme.ORANGE, 16)), BorderLayout.WEST);
        JLabel text = label("<html><b>" + NOTICE + "</b> &nbsp;·&nbsp; " + escape(detail) + "</html>", 12, false, Theme.ORANGE);
        text.setToolTipText(NOTICE + " · " + detail);
        t.add(text, BorderLayout.CENTER);
        return t;
    }
    /** Thẻ số liệu nhỏ; có thể bấm để lọc nhanh. */
    public static JComponent stat(String name, String title, long value, Color color, Runnable onClick, boolean active) {
        Tile t = new Tile(new BorderLayout(0, 2), active ? Flat.tint(color, 0.88) : Theme.CARD, active ? color : Theme.BORDER);
        t.setBorder(new EmptyBorder(7, 9, 7, 9));
        JLabel v = label(Theme.number(value), 20, true, color); v.setName(name);
        JLabel caption = label(title, 11, active, active ? color : Theme.MUTED);
        t.add(caption, BorderLayout.NORTH); t.add(v, BorderLayout.CENTER);
        t.setToolTipText(title + ": " + value + (onClick == null ? "" : " · bấm để lọc"));
        if (onClick != null) {
            t.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            java.awt.event.MouseAdapter m = new java.awt.event.MouseAdapter() {
                @Override public void mouseClicked(java.awt.event.MouseEvent e) { onClick.run(); }
            };
            t.addMouseListener(m); v.addMouseListener(m); caption.addMouseListener(m);
        }
        return t;
    }

    // ------------------------------------------------------------------ feedback (Notice)
    public static Flat.Notice feedback(String name) { return new Flat.Notice(name); }
    public static void result(JLabel l, DemoClinicStore.Result<?> r) { if (r.ok()) ok(l, r.message()); else fail(l, r.message()); }
    public static void ok(JLabel l, String text) {
        String prefix = DemoClinicStore.SAVED + " · ";
        String html = text.startsWith(prefix) ? "<html>" + escape(text.substring(prefix.length())) + "<br><font color='#647084'>"
                + DemoClinicStore.SAVED + "</font></html>" : "<html>" + escape(text) + "</html>";
        set(l, Flat.Kind.SUCCESS, html, text);
    }
    public static void fail(JLabel l, String text) { set(l, Flat.Kind.ERROR, "<html>" + escape(text) + "</html>", text); }
    public static void warn(JLabel l, String text) { set(l, Flat.Kind.WARNING, "<html>" + escape(text) + "</html>", text); }
    public static void info(JLabel l, String text) { set(l, Flat.Kind.NONE, text, text); }
    public static void hint(JLabel l, String text) { set(l, Flat.Kind.INFO, "<html>" + escape(text) + "</html>", text); }
    private static void set(JLabel l, Flat.Kind kind, String html, String plain) {
        if (l instanceof Flat.Notice n) n.show(kind, html, plain);
        else { l.setText(plain); l.setToolTipText(plain); l.setForeground(Flat.Notice.color(kind)); }
    }
    public static String escape(String s) { return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;"); }

    // ------------------------------------------------------------------ inputs
    public static JTextField field(String name, int columns, String placeholder) { return Flat.field(name, placeholder, placeholder, false); }
    public static JTextField search(String name, String placeholder) { return Flat.field(name, placeholder, placeholder, true); }
    public static <T> JComboBox<T> combo(String name, List<T> items) { return Flat.combo(name, items); }
    public static <T> void replaceItems(JComboBox<T> combo, List<T> items) {
        Object keep = combo.getSelectedItem(); combo.removeAllItems(); items.forEach(combo::addItem);
        if (keep != null && items.contains(keep)) combo.setSelectedItem(keep);
    }
    /** Form dọc: nhãn nhỏ phía trên ô nhập; truyền cặp (String, Component). */
    public static JPanel form(Object... pairs) {
        JPanel p = new JPanel(new GridBagLayout()); p.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints(); c.gridx = 0; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL; c.anchor = GridBagConstraints.WEST;
        for (int i = 0; i < pairs.length; i += 2) {
            c.gridy = i; c.insets = new Insets(i == 0 ? 0 : 8, 0, 4, 0);
            p.add(label((String) pairs[i], 12, true, Theme.color("475569")), c);
            c.gridy = i + 1; c.insets = new Insets(0, 0, 0, 0);
            p.add((Component) pairs[i + 1], c);
        }
        return p;
    }
    /** Hai ô cạnh nhau trong form. */
    public static JPanel pair(Component a, Component b) {
        JPanel p = new JPanel(new GridLayout(1, 2, 10, 0)); p.setOpaque(false); p.add(a); p.add(b); return p;
    }
    public static JLabel value(String name) { JLabel l = label("—", 13, true, Theme.TEXT); l.setName(name); return l; }
    public static JPanel buttons(Component... items) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0)); p.setOpaque(false);
        for (Component c : items) p.add(c); return p;
    }
    public static JPanel buttonsRight(Component... items) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0)); p.setOpaque(false);
        for (Component c : items) p.add(c); return p;
    }
    public static <B extends AbstractButton> B icon(B b, String icon, Color color) {
        b.setIcon(new UiIcons(icon, color, 14)); b.setIconTextGap(6); return b;
    }

    // ------------------------------------------------------------------ layout
    public static JPanel twoColumns(JComponent left, JComponent right, double leftWeight) {
        JPanel p = new JPanel(new GridBagLayout()); p.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints(); c.fill = GridBagConstraints.BOTH; c.anchor = GridBagConstraints.NORTH;
        c.gridy = 0; c.weighty = 1;
        c.gridx = 0; c.weightx = leftWeight; c.insets = new Insets(0, 0, 0, Theme.GAP); p.add(top(left), c);
        c.gridx = 1; c.weightx = 1 - leftWeight; c.insets = new Insets(0, 0, 0, 0); p.add(top(right), c);
        return p;
    }
    private static JPanel top(JComponent c) {
        JPanel holder = new TopHolder(c); holder.setOpaque(false); holder.add(c, BorderLayout.NORTH); return holder;
    }
    /** Giữ cột theo trọng số GridBag: bề ngang tối thiểu nhỏ, chiều cao theo nội dung. */
    private static final class TopHolder extends JPanel {
        private static final long serialVersionUID = 1L;
        private final JComponent content;
        TopHolder(JComponent content) { super(new BorderLayout()); this.content = content; }
        @Override public Dimension getPreferredSize() { return new Dimension(10, content.getPreferredSize().height); }
    }
    public static JPanel screen(JComponent... children) {
        JPanel p = column(14, children); p.setBorder(new EmptyBorder(20, Theme.PADDING, Theme.PADDING, Theme.PADDING));
        return p;
    }
    /** Thẻ trắng có tiêu đề kèm số bước (UX theo luồng). */
    public static UiCard step(int number, String title, JComponent accessory, JComponent body) {
        return new UiCard((number > 0 ? "Bước " + number + "  ·  " : "") + title, accessory, body);
    }

    // ------------------------------------------------------------------ parse & tags
    public static LocalDate parseDate(String text) {
        try { return LocalDate.parse(text.trim(), DATE); } catch (DateTimeParseException | NullPointerException e) { return null; }
    }
    public static LocalTime parseTime(String text) {
        try { return LocalTime.parse(text.trim(), TIME); } catch (DateTimeParseException | NullPointerException e) { return null; }
    }
    public static Color tone(AppointmentStatus s) {
        return switch (s) {
            case CHO_XAC_NHAN -> Theme.color("B45309"); case DA_XAC_NHAN -> Theme.color("0E7490"); case DA_DEN -> Theme.PURPLE;
            case HOAN_THANH -> Theme.GREEN; case VANG_MAT -> Theme.color("475569"); case DA_HUY -> Theme.RED; case DA_DOI_LICH -> Theme.color("64748B");
        };
    }
    public static DemoTable.Tag status(AppointmentStatus s) { return new DemoTable.Tag(s.label(), tone(s)); }
    public static Color tone(DemoClinicStore.TicketStatus s) {
        return switch (s) { case CHO_KHAM -> Theme.color("B45309"); case DANG_KHAM -> Theme.PURPLE; case DA_KET_LUAN -> Theme.GREEN; case DA_HUY -> Theme.RED; };
    }
    public static DemoTable.Tag ticket(DemoClinicStore.TicketStatus s) { return new DemoTable.Tag(s.label(), tone(s)); }

    // ------------------------------------------------------------------ timeline
    /** Timeline dọc: chấm màu + thời gian + mô tả, không dùng ký tự biểu tượng. */
    public static void fillTimeline(JPanel box, List<DemoClinicStore.Activity> items, int limit, String emptyText) {
        box.removeAll();
        if (items.isEmpty()) box.add(label(emptyText, 12, false, Theme.MUTED));
        items.stream().limit(limit).forEach(a -> {
            JPanel row = new JPanel(new BorderLayout(10, 0)); row.setOpaque(false); row.setBorder(new EmptyBorder(4, 0, 4, 0));
            JLabel dot = new JLabel(new UiIcons("dot", Theme.PRIMARY, 12)); dot.setVerticalAlignment(SwingConstants.TOP);
            row.add(dot, BorderLayout.WEST);
            row.add(column(1, label(a.action() + " · " + a.objectId(), 12, true, Theme.TEXT),
                    label(DATE_TIME.format(a.time()) + " · " + a.actor() + " · " + a.outcome(), 11, false, Theme.MUTED)), BorderLayout.CENTER);
            row.setAlignmentX(Component.LEFT_ALIGNMENT); box.add(row);
        });
        box.revalidate(); box.repaint();
    }
    public static JPanel timelineBox(String name) {
        JPanel p = new JPanel(); p.setName(name); p.setOpaque(false); p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS)); return p;
    }
}
