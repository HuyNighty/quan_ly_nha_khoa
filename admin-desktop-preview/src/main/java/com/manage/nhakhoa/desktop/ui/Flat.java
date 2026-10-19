package com.manage.nhakhoa.desktop.ui;

import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;
import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicScrollBarUI;

/**
 * Bộ component phẳng tự vẽ cho các màn mô phỏng: ô nhập có placeholder, combo, segmented control,
 * thanh cuộn mảnh, avatar, nhãn trạng thái dạng viên, thông báo có icon.
 * Không dùng ký tự biểu tượng Unicode (✓ ⚠ ●) vì font Segoe UI trên Windows không có đủ glyph.
 */
public final class Flat {
    private Flat() { }
    public static final int FIELD_HEIGHT = 34;

    /** Pha màu với trắng để làm nền nhạt cho viên trạng thái. */
    public static Color tint(Color c, double amount) {
        return new Color((int) (c.getRed() + (255 - c.getRed()) * amount), (int) (c.getGreen() + (255 - c.getGreen()) * amount),
                (int) (c.getBlue() + (255 - c.getBlue()) * amount));
    }
    public static void smooth(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    }

    // ------------------------------------------------------------------ input border
    /** Viền bo góc; đổi màu xanh khi component có focus. */
    public static final class RoundBorder extends AbstractBorder {
        private static final long serialVersionUID = 1L;
        private final int left;
        public RoundBorder(int left) { this.left = left; }
        @Override public void paintBorder(Component c, Graphics graphics, int x, int y, int w, int h) {
            Graphics2D g = (Graphics2D) graphics.create(); smooth(g);
            boolean focus = c.hasFocus() || (c instanceof JComboBox<?> combo && combo.isPopupVisible());
            g.setColor(!c.isEnabled() ? Theme.BORDER : focus ? Theme.PRIMARY : Theme.color("C9D7DA"));
            g.setStroke(new BasicStroke(focus ? 1.6f : 1f));
            g.drawRoundRect(x + 1, y + 1, w - 3, h - 3, 10, 10); g.dispose();
        }
        @Override public Insets getBorderInsets(Component c) { return new Insets(6, left, 6, 10); }
        @Override public Insets getBorderInsets(Component c, Insets i) { Insets n = getBorderInsets(c); i.set(n.top, n.left, n.bottom, n.right); return i; }
    }

    // ------------------------------------------------------------------ text field
    public static JTextField field(String name, String placeholder, String accessible, boolean searchIcon) {
        Field f = new Field(placeholder, searchIcon); f.setName(name); f.setToolTipText(accessible);
        f.getAccessibleContext().setAccessibleName(accessible); return f;
    }
    private static final class Field extends JTextField {
        private static final long serialVersionUID = 1L;
        private final String placeholder;
        private final boolean icon;
        private final boolean bare;
        Field(String placeholder, boolean icon) { this(placeholder, icon, false); }
        Field(String placeholder, boolean icon, boolean bare) {
            this.placeholder = placeholder; this.icon = icon; this.bare = bare;
            setFont(Theme.font(bare ? 14 : 13, false)); setForeground(Theme.TEXT); setCaretColor(Theme.PRIMARY_TEXT);
            setBackground(Theme.CARD); setOpaque(false); setBorder(bare ? new EmptyBorder(0, 0, 0, 0) : new RoundBorder(icon ? 32 : 10));
            setSelectionColor(Theme.color("CCEFEA")); setSelectedTextColor(Theme.TEXT);
            addFocusListener(new FocusAdapter() {
                @Override public void focusGained(FocusEvent e) { repaint(); }
                @Override public void focusLost(FocusEvent e) { repaint(); }
            });
        }
        @Override public Dimension getPreferredSize() { Dimension d = super.getPreferredSize(); return new Dimension(d.width, bare ? d.height : FIELD_HEIGHT); }
        @Override public Dimension getMaximumSize() { return new Dimension(Integer.MAX_VALUE, bare ? super.getMaximumSize().height : FIELD_HEIGHT); }
        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create(); smooth(g);
            if (!bare) { g.setColor(isEnabled() ? Theme.CARD : Theme.SOFT); g.fillRoundRect(1, 1, getWidth() - 2, getHeight() - 2, 10, 10); }
            if (icon) new UiIcons("search", Theme.MUTED, 16).paintIcon(this, g, 11, (getHeight() - 16) / 2);
            g.dispose();
            super.paintComponent(graphics);
            if (getText().isEmpty() && placeholder != null) {
                Graphics2D p = (Graphics2D) graphics.create(); smooth(p);
                p.setColor(Theme.color("94A3B8")); p.setFont(getFont());
                Insets in = getInsets(); FontMetrics fm = p.getFontMetrics();
                p.drawString(placeholder, in.left, (getHeight() - fm.getHeight()) / 2 + fm.getAscent()); p.dispose();
            }
        }
    }

    // ------------------------------------------------------------------ search bar
    /**
     * Thanh tìm kiếm lớn: icon, placeholder, nút xóa nhanh khi có chữ, gợi ý phím tắt Ctrl+F khi trống.
     * Enter / mũi tên xuống gọi hành động để chuyển xuống danh sách kết quả.
     */
    public static final class SearchBar extends JPanel {
        private static final long serialVersionUID = 1L;
        private final JTextField field;
        private final JButton clear;
        private final JLabel shortcut;
        public SearchBar(String fieldName, String placeholder) {
            super(new BorderLayout(10, 0)); setOpaque(false); setBorder(new EmptyBorder(0, 14, 0, 8));
            field = new Field(placeholder, false, true); field.setName(fieldName);
            field.setToolTipText(placeholder); field.getAccessibleContext().setAccessibleName(placeholder);
            clear = new JButton(new UiIcons("close", Theme.MUTED, 14)); clear.setName(fieldName + "-clear");
            clear.setBorder(new EmptyBorder(6, 6, 6, 6)); clear.setContentAreaFilled(false); clear.setFocusable(false);
            clear.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); clear.setToolTipText("Xóa từ khóa (Esc)");
            clear.addActionListener(e -> { field.setText(""); field.requestFocusInWindow(); });
            shortcut = UiComponents.label("Ctrl+F", 11, true, Theme.MUTED);
            shortcut.setBorder(new EmptyBorder(3, 7, 3, 7));
            JLabel icon = new JLabel(new UiIcons("search", Theme.PRIMARY_TEXT, 18));
            JPanel east = new JPanel(new GridBagLayout()); east.setOpaque(false); east.add(shortcut); east.add(clear);
            add(icon, BorderLayout.WEST); add(field, BorderLayout.CENTER); add(east, BorderLayout.EAST);
            field.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
                @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { sync(); }
                @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { sync(); }
                @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { sync(); }
            });
            field.addFocusListener(new FocusAdapter() {
                @Override public void focusGained(FocusEvent e) { repaint(); }
                @Override public void focusLost(FocusEvent e) { repaint(); }
            });
            field.getInputMap().put(KeyStroke.getKeyStroke("ESCAPE"), "clear-search");
            field.getActionMap().put("clear-search", new AbstractAction() {
                private static final long serialVersionUID = 1L;
                @Override public void actionPerformed(java.awt.event.ActionEvent e) { field.setText(""); }
            });
            sync();
        }
        private void sync() { boolean has = !field.getText().isEmpty(); clear.setVisible(has); shortcut.setVisible(!has); }
        public JTextField field() { return field; }
        /** Enter hoặc mũi tên xuống trong ô tìm kiếm. */
        public void onSubmit(Runnable action) {
            for (String k : new String[]{"ENTER", "DOWN"}) {
                field.getInputMap().put(KeyStroke.getKeyStroke(k), "submit-search");
            }
            field.getActionMap().put("submit-search", new AbstractAction() {
                private static final long serialVersionUID = 1L;
                @Override public void actionPerformed(java.awt.event.ActionEvent e) { action.run(); }
            });
        }
        /** Ctrl+F trong phạm vi màn hình chứa thanh tìm kiếm sẽ đưa con trỏ vào ô tìm. */
        public void bindShortcut(JComponent scope) {
            scope.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("control F"), "focus-search-" + field.getName());
            scope.getActionMap().put("focus-search-" + field.getName(), new AbstractAction() {
                private static final long serialVersionUID = 1L;
                @Override public void actionPerformed(java.awt.event.ActionEvent e) { if (scope.isShowing()) { field.requestFocusInWindow(); field.selectAll(); } }
            });
        }
        @Override public Dimension getPreferredSize() { return new Dimension(super.getPreferredSize().width, 46); }
        @Override public Dimension getMaximumSize() { return new Dimension(Integer.MAX_VALUE, 46); }
        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create(); smooth(g);
            boolean focus = field.hasFocus();
            if (focus) { g.setColor(tint(Theme.PRIMARY, 0.82)); g.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16); }
            g.setColor(Theme.CARD); g.fillRoundRect(2, 2, getWidth() - 4, getHeight() - 4, 13, 13);
            g.setColor(focus ? Theme.PRIMARY : Theme.color("C9D7DA")); g.setStroke(new BasicStroke(focus ? 1.6f : 1f));
            g.drawRoundRect(2, 2, getWidth() - 5, getHeight() - 5, 13, 13);
            if (shortcut.isVisible()) {
                Rectangle r = SwingUtilities.convertRectangle(shortcut.getParent(), shortcut.getBounds(), this);
                g.setColor(Theme.SOFT); g.fillRoundRect(r.x, r.y, r.width, r.height, 8, 8);
                g.setColor(Theme.BORDER); g.setStroke(new BasicStroke(1f)); g.drawRoundRect(r.x, r.y, r.width - 1, r.height - 1, 8, 8);
            }
            g.dispose();
        }
    }

    // ------------------------------------------------------------------ combo
    public static <T> JComboBox<T> combo(String name, List<T> items) {
        JComboBox<T> c = new JComboBox<>(); c.setName(name); items.forEach(c::addItem); style(c); return c;
    }
    public static void style(JComboBox<?> c) {
        c.setUI(new ComboUI()); c.setFont(Theme.font(13, false)); c.setBackground(Theme.CARD); c.setForeground(Theme.TEXT);
        c.setBorder(new RoundBorder(10)); c.setOpaque(false); c.setFocusable(true);
        c.setRenderer(new ComboRenderer());
        c.setPreferredSize(new Dimension(Math.max(140, c.getPreferredSize().width), FIELD_HEIGHT));
        c.setMaximumSize(new Dimension(Integer.MAX_VALUE, FIELD_HEIGHT));
    }
    private static final class ComboRenderer extends DefaultListCellRenderer {
        private static final long serialVersionUID = 1L;
        @Override public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected, boolean focus) {
            JLabel l = (JLabel) super.getListCellRendererComponent(list, value, index, selected, focus);
            l.setBorder(new EmptyBorder(index < 0 ? 0 : 7, index < 0 ? 0 : 10, index < 0 ? 0 : 7, 10));
            l.setFont(Theme.font(13, false)); l.setOpaque(index >= 0);
            l.setBackground(selected && index >= 0 ? Theme.LIGHT : Theme.CARD); l.setForeground(Theme.TEXT);
            return l;
        }
    }
    private static final class ComboUI extends BasicComboBoxUI {
        @Override protected JButton createArrowButton() {
            JButton b = new JButton(new UiIcons("down", Theme.MUTED, 14));
            b.setBorder(new EmptyBorder(0, 4, 0, 10)); b.setContentAreaFilled(false); b.setFocusable(false); b.setOpaque(false);
            b.setName("ComboBox.arrowButton"); return b;
        }
        @Override public void paint(Graphics graphics, JComponent c) {
            Graphics2D g = (Graphics2D) graphics.create(); smooth(g);
            g.setColor(c.isEnabled() ? Theme.CARD : Theme.SOFT); g.fillRoundRect(1, 1, c.getWidth() - 2, c.getHeight() - 2, 10, 10); g.dispose();
            super.paint(graphics, c);
        }
        @Override public void paintCurrentValueBackground(Graphics g, Rectangle bounds, boolean hasFocus) { }
        @Override protected Insets getInsets() { return new Insets(6, 10, 6, 4); }
    }

    // ------------------------------------------------------------------ segmented control
    /** Nhóm nút chọn một (thay JRadioButton / JTabbedPane). Nút thứ i có tên name-i hoặc tên truyền vào. */
    public static final class Segmented extends JPanel {
        private static final long serialVersionUID = 1L;
        private final transient List<JToggleButton> buttons = new ArrayList<>();
        private transient IntConsumer onChange = i -> { };
        private int selected;
        public Segmented(String name, List<String> labels, List<String> names) {
            super(new GridLayout(1, labels.size(), 4, 0)); setName(name); setOpaque(false);
            setBorder(new EmptyBorder(3, 3, 3, 3));
            ButtonGroup group = new ButtonGroup();
            for (int i = 0; i < labels.size(); i++) {
                int index = i;
                JToggleButton b = new SegmentButton(labels.get(i));
                b.setName(names != null ? names.get(i) : name + "-" + i);
                b.addActionListener(e -> select(index)); group.add(b); buttons.add(b); add(b);
            }
            buttons.get(0).setSelected(true);
        }
        public void onChange(IntConsumer listener) { this.onChange = listener; }
        public int selected() { return selected; }
        public void select(int index) {
            selected = index; buttons.get(index).setSelected(true);
            buttons.forEach(Component::repaint); onChange.accept(index);
        }
        public JToggleButton button(int i) { return buttons.get(i); }
        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create(); smooth(g);
            g.setColor(Theme.color("E2ECEE")); g.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12); g.dispose();
        }
        @Override public Dimension getMaximumSize() { return new Dimension(Integer.MAX_VALUE, getPreferredSize().height); }
    }
    private static final class SegmentButton extends JToggleButton {
        private static final long serialVersionUID = 1L;
        SegmentButton(String text) {
            super(text); setFont(Theme.font(12, true)); setFocusPainted(false); setContentAreaFilled(false);
            setBorder(new EmptyBorder(7, 6, 7, 6)); setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); setToolTipText(text);
        }
        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create(); smooth(g);
            if (isSelected()) {
                g.setColor(new Color(15, 59, 69, 28)); g.fillRoundRect(1, 2, getWidth() - 2, getHeight() - 2, 10, 10);
                g.setColor(Theme.CARD); g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 2, 10, 10);
            } else if (getModel().isRollover()) {
                g.setColor(Theme.color("EEF5F5")); g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 2, 10, 10);
            }
            if (hasFocus()) { g.setColor(Theme.FOCUS); g.setStroke(new BasicStroke(1.5f)); g.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 4, 10, 10); }
            g.dispose();
            setForeground(isSelected() ? Theme.PRIMARY_TEXT : Theme.MUTED);
            super.paintComponent(graphics);
        }
    }

    // ------------------------------------------------------------------ scrollbar
    public static void thin(JScrollPane pane) {
        pane.getVerticalScrollBar().setUI(new ThinBar()); pane.getHorizontalScrollBar().setUI(new ThinBar());
        pane.getVerticalScrollBar().setPreferredSize(new Dimension(10, 10)); pane.getHorizontalScrollBar().setPreferredSize(new Dimension(10, 10));
        pane.getVerticalScrollBar().setOpaque(false); pane.getHorizontalScrollBar().setOpaque(false);
    }
    private static final class ThinBar extends BasicScrollBarUI {
        @Override protected JButton createDecreaseButton(int o) { return zero(); }
        @Override protected JButton createIncreaseButton(int o) { return zero(); }
        private static JButton zero() { JButton b = new JButton(); b.setPreferredSize(new Dimension(0, 0)); b.setMinimumSize(new Dimension(0, 0)); b.setMaximumSize(new Dimension(0, 0)); return b; }
        @Override protected void paintTrack(Graphics g, JComponent c, Rectangle r) { }
        @Override protected void paintThumb(Graphics graphics, JComponent c, Rectangle r) {
            if (r.isEmpty()) return;
            Graphics2D g = (Graphics2D) graphics.create(); smooth(g);
            g.setColor(isThumbRollover() ? Theme.color("94A3B8") : Theme.color("C3D0D3"));
            g.fillRoundRect(r.x + 2, r.y + 2, r.width - 4, r.height - 4, 8, 8); g.dispose();
        }
    }

    // ------------------------------------------------------------------ avatar & pill
    /** Bảng màu avatar: chọn ổn định theo mã (cùng bệnh nhân luôn cùng màu). */
    private static final Color[] AVATAR_TONES = {
        Theme.color("0D9488"), Theme.color("2563EB"), Theme.color("7C3AED"), Theme.color("C2410C"),
        Theme.color("DB2777"), Theme.color("15803D"), Theme.color("0369A1"), Theme.color("B45309")};
    public static Color avatarTone(String key) {
        return key == null ? Theme.MUTED : AVATAR_TONES[Math.floorMod(key.hashCode(), AVATAR_TONES.length)];
    }
    public static String initials(String fullName) {
        String[] parts = fullName == null ? new String[0] : fullName.trim().split("\\s+");
        if (parts.length == 0 || parts[0].isEmpty()) return "";
        return (parts.length == 1 ? parts[0].substring(0, 1) : parts[parts.length - 2].substring(0, 1) + parts[parts.length - 1].substring(0, 1))
                .toUpperCase(Theme.VI);
    }
    /** Vẽ avatar tròn: nền nhạt cùng tông, chữ cái đầu đậm. */
    public static void paintAvatar(Graphics2D g, int x, int y, int size, String name, Color tone) {
        String t = initials(name);
        g.setColor(t.isEmpty() ? Theme.BORDER : tint(tone, 0.84)); g.fillOval(x, y, size, size);
        if (t.isEmpty()) { new UiIcons("person", Theme.MUTED, size / 2).paintIcon(null, g, x + size / 4, y + size / 4); return; }
        g.setColor(tone); g.setFont(Theme.font(Math.max(11, size * 3 / 8), true));
        FontMetrics fm = g.getFontMetrics();
        g.drawString(t, x + (size - fm.stringWidth(t)) / 2, y + (size - fm.getHeight()) / 2 + fm.getAscent());
    }
    /** Vòng tròn chữ cái đầu tên (không dùng ảnh người thật). */
    public static final class Avatar extends JComponent {
        private static final long serialVersionUID = 1L;
        private String person;
        private transient Color tone = Theme.MUTED;
        private final int size;
        public Avatar(int size) { this.size = size; setPreferredSize(new Dimension(size, size)); setMinimumSize(getPreferredSize()); setMaximumSize(getPreferredSize()); }
        public void setPerson(String fullName) { setPerson(fullName, fullName); }
        public void setPerson(String fullName, String key) { person = fullName; tone = avatarTone(key); repaint(); }
        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create(); smooth(g); paintAvatar(g, 0, 0, size - 1, person, tone); g.dispose();
        }
    }
    /** Nhãn dạng viên bo tròn, nền nhạt cùng tông màu chữ. */
    public static final class Pill extends JLabel {
        private static final long serialVersionUID = 1L;
        private Color tone;
        public Pill(String text, Color tone) { super(text); setTone(tone); setFont(Theme.font(11, true)); setBorder(new EmptyBorder(3, 9, 3, 9)); setToolTipText(text); }
        public void setTone(Color tone) { this.tone = tone; setForeground(tone); repaint(); }
        public void set(String text, Color tone) { setText(text); setToolTipText(text); setTone(tone); }
        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create(); smooth(g);
            g.setColor(tint(tone, 0.87)); g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, getHeight(), getHeight()); g.dispose();
            super.paintComponent(graphics);
        }
        @Override public Dimension getMaximumSize() { return getPreferredSize(); }
    }

    // ------------------------------------------------------------------ notice
    public enum Kind { NONE, INFO, SUCCESS, ERROR, WARNING }
    /** Khối thông báo có icon Java2D; ẩn nền khi Kind.NONE. Văn bản vẫn đọc được qua getText(). */
    public static final class Notice extends JLabel {
        private static final long serialVersionUID = 1L;
        private Kind kind = Kind.NONE;
        public Notice(String name) {
            super(" "); setName(name); setFont(Theme.font(12, true)); setIconTextGap(8);
            setVerticalAlignment(SwingConstants.TOP); setVerticalTextPosition(SwingConstants.TOP);
        }
        public Kind kind() { return kind; }
        public void show(Kind k, String html, String plain) {
            kind = k; setText(html); setToolTipText(plain);
            Color c = color(k);
            setForeground(k == Kind.NONE ? Theme.MUTED : c);
            setIcon(k == Kind.NONE ? null : new UiIcons(k == Kind.SUCCESS ? "check" : k == Kind.INFO ? "info" : "alert", c, 16));
            setBorder(k == Kind.NONE ? new EmptyBorder(2, 0, 2, 0) : new EmptyBorder(8, 10, 8, 10));
            revalidate(); repaint();
        }
        static Color color(Kind k) {
            return switch (k) { case SUCCESS -> Theme.GREEN; case ERROR -> Theme.RED; case WARNING -> Theme.ORANGE; case INFO -> Theme.PRIMARY_TEXT; case NONE -> Theme.MUTED; };
        }
        @Override protected void paintComponent(Graphics graphics) {
            if (kind != Kind.NONE) {
                Graphics2D g = (Graphics2D) graphics.create(); smooth(g);
                g.setColor(tint(color(kind), 0.9)); g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10); g.dispose();
            }
            super.paintComponent(graphics);
        }
        @Override public Dimension getMaximumSize() { return new Dimension(Integer.MAX_VALUE, getPreferredSize().height); }
        /** Văn bản HTML tự xuống dòng theo bề ngang thực tế, tránh bị cắt mất dòng thứ hai. */
        @Override public Dimension getPreferredSize() {
            Dimension d = super.getPreferredSize();
            Object view = getClientProperty(javax.swing.plaf.basic.BasicHTML.propertyKey);
            int width = getWidth() > 0 ? getWidth() : getParent() != null ? getParent().getWidth() : 0;
            if (view instanceof javax.swing.text.View v && width > 40) {
                Insets in = getInsets(); int iconW = getIcon() == null ? 0 : getIcon().getIconWidth() + getIconTextGap();
                int textW = width - in.left - in.right - iconW;
                if (textW > 20) {
                    v.setSize(textW, 0);
                    int h = (int) Math.ceil(v.getPreferredSpan(javax.swing.text.View.Y_AXIS)) + in.top + in.bottom;
                    return new Dimension(Math.min(d.width, width), Math.max(h, getIcon() == null ? 0 : getIcon().getIconHeight() + in.top + in.bottom));
                }
            }
            return d;
        }
    }

    /** Lưới "nhãn: giá trị" hai cột. */
    public static JPanel infoGrid(Object... pairs) {
        JPanel p = new JPanel(new GridBagLayout()); p.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints(); c.anchor = GridBagConstraints.NORTHWEST; c.insets = new Insets(4, 0, 4, 14);
        for (int i = 0; i < pairs.length; i += 2) {
            c.gridy = i / 2; c.gridx = 0; c.weightx = 0; c.fill = GridBagConstraints.NONE;
            p.add(UiComponents.label((String) pairs[i], 12, false, Theme.MUTED), c);
            c.gridx = 1; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL; p.add((Component) pairs[i + 1], c);
        }
        return p;
    }
}
