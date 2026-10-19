package com.manage.nhakhoa.desktop.ui;

import com.manage.nhakhoa.desktop.model.DashboardViewModel.*;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.Map;
import java.util.function.Consumer;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/** Component trình bày dùng lại, không truy cập nguồn dữ liệu bên ngoài. */
public final class UiComponents {
    private UiComponents() { }
    public static JLabel label(String text, int size, boolean bold, Color color) {
        JLabel l = new FlexibleLabel(text); l.setFont(Theme.font(size, bold)); l.setForeground(color);
        l.setToolTipText(text); return l;
    }
    private static final class FlexibleLabel extends JLabel {
        private static final long serialVersionUID = 1L;
        FlexibleLabel(String text) { super(text); }
        @Override public Dimension getMaximumSize() {
            // Cho BoxLayout dùng hết bề ngang, tránh cắt ký tự cuối do khác biệt font/DPI.
            return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
        }
    }
    public static JPanel row(int gap) { JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, gap, 0)); p.setOpaque(false); return p; }
    public static JPanel column(int gap, Component... children) {
        JPanel p = new JPanel(); p.setOpaque(false); p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        for (int i = 0; i < children.length; i++) {
            if (i > 0) p.add(Box.createVerticalStrut(gap));
            Component c = children[i]; if (c instanceof JComponent jc) jc.setAlignmentX(Component.LEFT_ALIGNMENT); p.add(c);
        }
        return p;
    }
    public static JButton primary(String text, Runnable action) { return button(text, Theme.PRIMARY_TEXT, Color.WHITE, action); }
    public static JButton secondary(String text, Runnable action) { return button(text, Theme.CARD, Theme.TEXT, action); }
    public static JButton danger(String text, Runnable action) { return button(text, Theme.RED, Color.WHITE, action); }
    public static JButton link(String text, Runnable action) {
        JButton b = button(text, Theme.CARD, Theme.PRIMARY_TEXT, action);
        b.putClientProperty("link", true); b.setBorder(new EmptyBorder(3, 3, 3, 3)); return b;
    }
    public static JButton small(String text, Runnable action) {
        JButton b = secondary(text + " ›", action); b.setMargin(new Insets(3, 8, 3, 8)); return b;
    }
    private static JButton button(String text, Color bg, Color fg, Runnable action) {
        JButton b = new UiButton(text); b.setFont(Theme.font(12, true)); b.setForeground(fg); b.setBackground(bg);
        b.setBorder(new EmptyBorder(7, 10, 7, 10));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); b.setToolTipText(text);
        b.addActionListener(e -> action.run());
        b.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) { b.repaint(); }
            @Override public void focusLost(FocusEvent e) { b.repaint(); }
        });
        // Enter và Space đều kích hoạt nút, kể cả nút trên thẻ KPI.
        b.getInputMap().put(KeyStroke.getKeyStroke("ENTER"), "pressed");
        b.getInputMap().put(KeyStroke.getKeyStroke("released ENTER"), "released");
        return b;
    }

    private static final class UiButton extends JButton {
        private static final long serialVersionUID = 1L;
        UiButton(String text) { super(text); setOpaque(false); setContentAreaFilled(false); setFocusPainted(false); }
        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D)graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            boolean link = Boolean.TRUE.equals(getClientProperty("link"));
            if (!link || getModel().isRollover()) {
                g.setColor(getModel().isRollover() ? (link ? Theme.LIGHT : getBackground().darker()) : getBackground());
                g.fillRoundRect(0, 0, getWidth()-1, getHeight()-1, 9, 9);
            }
            g.dispose(); super.paintComponent(graphics);
        }
        @Override protected void paintBorder(Graphics graphics) {
            boolean link = Boolean.TRUE.equals(getClientProperty("link"));
            if (!link || hasFocus()) {
                Graphics2D g = (Graphics2D)graphics.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setStroke(new BasicStroke(hasFocus() ? 2 : 1)); g.setColor(hasFocus() ? Theme.FOCUS : Theme.BORDER);
                g.drawRoundRect(1, 1, getWidth()-3, getHeight()-3, 9, 9); g.dispose();
            }
        }
    }

    public static sealed class RoundedPanel extends JPanel permits StatusBadge, IconTile, UiCard,
            KpiCard, ShiftTile, StatTile, AlertBar {
        private static final long serialVersionUID = 1L;
        private final Color fill;
        private final Color border;
        public RoundedPanel(LayoutManager layout, Color fill, Color border) {
            super(layout); this.fill = fill; this.border = border; setOpaque(false);
        }
        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(fill); g.fillRoundRect(0, 0, getWidth()-1, getHeight()-1, Theme.RADIUS, Theme.RADIUS);
            if (border != null) { g.setColor(border); g.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, Theme.RADIUS, Theme.RADIUS); }
            g.dispose(); super.paintComponent(graphics);
        }
    }
    public static final class StatusBadge extends RoundedPanel {
        private static final long serialVersionUID = 1L;
        public StatusBadge(String text, Color fg, Color bg) {
            super(new FlowLayout(FlowLayout.LEFT, 7, 3), bg, null); add(label(text, 11, true, fg));
            setToolTipText(text);
        }
    }
    public static final class IconTile extends RoundedPanel {
        private static final long serialVersionUID = 1L;
        public IconTile(String icon, Color fg, Color bg, int size) {
            super(new GridBagLayout(), bg, null); add(new JLabel(new UiIcons(icon, fg, size > 36 ? 22 : 18)));
            Dimension d = new Dimension(size, size); setPreferredSize(d); setMinimumSize(d); setMaximumSize(d);
        }
    }
    public static final class UiCard extends RoundedPanel {
        private static final long serialVersionUID = 1L;
        public UiCard(String title, JComponent accessory, JComponent body) {
            super(new BorderLayout(0, 10), Theme.CARD, Theme.BORDER);
            setBorder(new EmptyBorder(14, 16, 14, 16));
            JPanel header = new JPanel(new BorderLayout(8, 0)); header.setOpaque(false);
            header.add(label(title, 15, true, Theme.TEXT), BorderLayout.CENTER);
            if (accessory != null) header.add(accessory, BorderLayout.EAST);
            JPanel bodyTop = new JPanel(new BorderLayout()); bodyTop.setOpaque(false); bodyTop.add(body, BorderLayout.NORTH);
            add(header, BorderLayout.NORTH); add(bodyTop, BorderLayout.CENTER);
        }
    }
    public static final class ScreenHeader extends JPanel {
        private static final long serialVersionUID = 1L;
        public ScreenHeader(String title, String subtitle, JComponent actions) {
            super(new BorderLayout(16, 0)); setOpaque(false);
            add(column(3, label(title, 24, true, Theme.TEXT), label(subtitle, 12, false, Theme.MUTED)), BorderLayout.CENTER);
            if (actions != null) add(actions, BorderLayout.EAST);
        }
    }
    public static final class KpiCard extends RoundedPanel {
        private static final long serialVersionUID = 1L;
        public KpiCard(String name, String title, String value, String detail, String icon, Color color, Runnable action) {
            super(new BorderLayout(10, 0), Theme.CARD, Theme.BORDER); setBorder(new EmptyBorder(13, 14, 13, 14));
            JButton button = link(title, action); button.setName(name); button.setHorizontalAlignment(SwingConstants.LEFT);
            button.setFont(Theme.font(12, false)); button.setForeground(Theme.MUTED);
            button.getAccessibleContext().setAccessibleName(title + ": " + value);
            JPanel iconArea = new JPanel(new BorderLayout()); iconArea.setOpaque(false);
            iconArea.add(new IconTile(icon, color, Theme.SOFT, 40), BorderLayout.NORTH);
            add(iconArea, BorderLayout.WEST);
            add(column(3, button, label(value, 26, true, Theme.TEXT), label(detail, 11, false, Theme.MUTED)), BorderLayout.CENTER);
            setToolTipText(title + ": " + value + " · " + detail);
            // Toàn thẻ nhận nhấp chuột; nút giữ cơ chế focus bàn phím của Swing.
            java.awt.event.MouseAdapter click = new java.awt.event.MouseAdapter() {
                @Override public void mouseClicked(java.awt.event.MouseEvent e) { action.run(); }
            };
            bindClick(this, click); setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }
        private static void bindClick(Container c, java.awt.event.MouseListener listener) {
            c.addMouseListener(listener);
            for (Component child : c.getComponents()) {
                if (child instanceof AbstractButton) continue;
                if (child instanceof Container container) bindClick(container, listener); else child.addMouseListener(listener);
            }
        }
    }
    public static final class TaskRow extends JPanel {
        private static final long serialVersionUID = 1L;
        public TaskRow(TaskSummary task, Consumer<String> navigate) {
            super(new BorderLayout(10, 0)); setOpaque(false); setBorder(new EmptyBorder(5, 0, 5, 0));
            Color color = Theme.priority(task.priority());
            add(new IconTile(task.priority() == Priority.FOLLOW_UP ? "clock" : "document", color, Theme.SOFT, 32), BorderLayout.WEST);
            add(column(2, label(Theme.number(task.affectedCount()) + " · " + task.title(), 12, true, Theme.TEXT),
                    label(task.description(), 11, false, Theme.MUTED)), BorderLayout.CENTER);
            JButton action = small(task.actionLabel(), () -> navigate.accept(task.targetScreenKey()));
            action.setName("task-" + task.id());
            action.setToolTipText("Mở " + com.manage.nhakhoa.desktop.MenuDefinition.entry(task.targetScreenKey()).label() + " · chỉ điều hướng");
            add(action, BorderLayout.EAST);
        }
    }
    public static final class PriorityGroupHeader extends JPanel {
        private static final long serialVersionUID = 1L;
        public PriorityGroupHeader(Priority p) {
            super(new BorderLayout()); setOpaque(false); setBorder(new EmptyBorder(6, 0, 2, 0));
            add(label("●  " + p.label(), 11, true, Theme.priority(p)));
        }
    }
    public static final class StackedStatusBar extends JComponent {
        private static final long serialVersionUID = 1L;
        private final transient Map<AppointmentStatus, Long> counts;
        public StackedStatusBar(Map<AppointmentStatus, Long> counts) {
            this.counts = Map.copyOf(counts); setPreferredSize(new Dimension(100, 12));
            setMinimumSize(new Dimension(0, 12)); setMaximumSize(new Dimension(Integer.MAX_VALUE, 12));
            setToolTipText("Tỷ lệ theo 7 trạng thái lịch hẹn; xem số lượng trong chú giải");
        }
        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            long total = counts.values().stream().reduce(0L, Math::addExact);
            g.setColor(Theme.BACKGROUND); g.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
            if (total > 0) {
                double accumulated = 0;
                for (AppointmentStatus status : AppointmentStatus.values()) {
                    long n = counts.getOrDefault(status, 0L); if (n == 0) continue;
                    int left = (int) Math.round(accumulated / total * getWidth()); accumulated += n;
                    int right = (int) Math.round(accumulated / total * getWidth());
                    g.setColor(Theme.status(status)); g.fillRect(left, 0, Math.max(1, right-left-1), getHeight());
                }
            }
            g.dispose();
        }
    }
    public static final class ShiftTile extends RoundedPanel {
        private static final long serialVersionUID = 1L;
        public ShiftTile(ShiftSummary shift) {
            super(new BorderLayout(), shift.missingNurses() == 0 ? Theme.CARD : Theme.RED_LIGHT, Theme.BORDER);
            setBorder(new EmptyBorder(10, 10, 10, 10));
            JLabel status = label(shift.missingNurses() == 0 ? "Đủ người" : "Y tá thiếu " + Theme.number(shift.missingNurses()),
                    11, true, shift.missingNurses() == 0 ? Theme.GREEN : Theme.RED);
            status.setIcon(new UiIcons(shift.missingNurses() == 0 ? "check" : "alert", shift.missingNurses() == 0 ? Theme.GREEN : Theme.RED, 12));
            add(column(3, label(shift.name(), 12, true, Theme.TEXT), label("So với định mức", 11, false, Theme.MUTED), status));
        }
    }
    public static final class StatTile extends RoundedPanel {
        private static final long serialVersionUID = 1L;
        public StatTile(String title, long value, Color color) {
            super(new BorderLayout(), Theme.SOFT, null); setBorder(new EmptyBorder(10, 10, 10, 10));
            add(column(3, label(title, 11, false, Theme.MUTED), label(Theme.number(value), 20, true, color)));
        }
    }
    public static final class AlertBar extends RoundedPanel {
        private static final long serialVersionUID = 1L;
        public AlertBar(String text, boolean warning) {
            super(new BorderLayout(8, 0), warning ? Theme.RED_LIGHT : Theme.GREEN_LIGHT, null);
            setBorder(new EmptyBorder(9, 10, 9, 10));
            add(new JLabel(new UiIcons("medical", warning ? Theme.RED : Theme.GREEN, 16)), BorderLayout.WEST);
            add(label(text, 11, true, warning ? Theme.RED : Theme.GREEN));
        }
    }
}
