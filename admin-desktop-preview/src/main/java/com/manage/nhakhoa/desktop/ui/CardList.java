package com.manage.nhakhoa.desktop.ui;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/**
 * Danh sách dạng thẻ (mỗi dòng tự vẽ: avatar, tên, mô tả, viên trạng thái). Bàn phím lên/xuống của JList,
 * hover, dòng đang chọn có vạch nhấn bên trái, empty state có icon. Giữ lựa chọn theo mã khi dữ liệu đổi.
 */
public final class CardList<T> extends JPanel implements RowView<T> {
    private static final long serialVersionUID = 1L;
    /** Vẽ một dòng trong khung r; selected/hover để đổi nền. */
    public interface Painter<T> { void paint(Graphics2D g, T item, Rectangle r, boolean selected, boolean hover); }

    private final transient Function<T, String> idOf;
    private final transient Painter<T> painter;
    private transient List<T> rows = List.of();
    private transient Consumer<T> onSelect = r -> { };
    private final DefaultListModel<Integer> model = new DefaultListModel<>();
    private final JList<Integer> list = new JList<>(model);
    private final JLabel empty;
    private final JPanel cards = new JPanel(new CardLayout());
    private final int rowHeight, maxRows;
    private boolean adjusting;
    private int hover = -1;

    public CardList(String name, Function<T, String> idOf, Painter<T> painter, int rowHeight, int maxRows) {
        super(new BorderLayout()); setName(name); setOpaque(false);
        this.idOf = idOf; this.painter = painter; this.rowHeight = rowHeight; this.maxRows = maxRows;
        list.setName(name + "-list"); list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setFixedCellHeight(rowHeight); list.setBackground(Theme.CARD); list.setCellRenderer(new Canvas());
        list.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        list.addListSelectionListener(e -> { if (!e.getValueIsAdjusting() && !adjusting) selected().ifPresent(r -> onSelect.accept(r)); });
        MouseAdapter m = new MouseAdapter() {
            @Override public void mouseMoved(MouseEvent e) { int i = list.locationToIndex(e.getPoint()); setHover(i >= 0 && list.getCellBounds(i, i).contains(e.getPoint()) ? i : -1); }
            @Override public void mouseExited(MouseEvent e) { setHover(-1); }
        };
        list.addMouseMotionListener(m); list.addMouseListener(m);
        JScrollPane scroll = new JScrollPane(list); scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(Theme.CARD); Flat.thin(scroll); scroll.getVerticalScrollBar().setUnitIncrement(rowHeight / 2);
        empty = UiComponents.label("", 13, false, Theme.MUTED); empty.setName(name + "-empty");
        empty.setIcon(new UiIcons("search", Theme.color("94A3B8"), 22)); empty.setIconTextGap(10);
        empty.setHorizontalAlignment(SwingConstants.CENTER); empty.setBorder(new EmptyBorder(24, 12, 24, 12));
        JPanel emptyBox = new DemoWidgets.Tile(new BorderLayout(), Theme.SOFT, Theme.color("E2E8F0")); emptyBox.add(empty);
        cards.setOpaque(false); cards.add(scroll, "list"); cards.add(emptyBox, "empty"); add(cards);
        resize();
    }
    public JList<Integer> listView() { return list; }
    private void setHover(int i) { if (i != hover) { hover = i; list.repaint(); } }
    private void resize() {
        int visible = rows.isEmpty() ? 2 : Math.max(1, Math.min(maxRows, rows.size()));
        cards.setPreferredSize(new Dimension(200, rowHeight * visible + 2)); revalidate();
    }
    @Override public void onSelect(Consumer<T> listener) { this.onSelect = Objects.requireNonNull(listener); }
    @Override public List<T> rows() { return rows; }
    @Override public boolean isEmptyState() { return rows.isEmpty(); }
    @Override public String emptyText() { return empty.getText(); }
    @Override public void setRows(List<T> newRows, String emptyText) {
        Theme.requireEdt();
        String keep = selected().map(idOf).orElse(null);
        adjusting = true;
        try {
            list.clearSelection(); rows = List.copyOf(newRows); model.clear();
            for (int i = 0; i < rows.size(); i++) model.addElement(i);
            empty.setText(emptyText); empty.setToolTipText(emptyText);
            ((CardLayout) cards.getLayout()).show(cards, rows.isEmpty() ? "empty" : "list");
            if (keep != null) selectQuietly(keep);
        } finally { adjusting = false; }
        resize();
    }
    @Override public Optional<T> selected() {
        int i = list.getSelectedIndex();
        return i >= 0 && i < rows.size() ? Optional.of(rows.get(i)) : Optional.empty();
    }
    @Override public boolean select(String id) {
        if (!selectQuietly(id)) return false;
        selected().ifPresent(r -> onSelect.accept(r)); return true;
    }
    /** Chọn dòng đầu tiên (dùng cho Enter / mũi tên xuống từ ô tìm kiếm). */
    public boolean selectFirst() { return !rows.isEmpty() && select(idOf.apply(rows.get(0))); }
    private boolean selectQuietly(String id) {
        boolean old = adjusting; adjusting = true;
        try {
            for (int i = 0; i < rows.size(); i++) {
                if (idOf.apply(rows.get(i)).equals(id)) { list.setSelectedIndex(i); list.ensureIndexIsVisible(i); return true; }
            }
            list.clearSelection(); return false;
        } finally { adjusting = old; }
    }
    @Override public void clearSelection() { boolean old = adjusting; adjusting = true; list.clearSelection(); adjusting = old; }

    /** Renderer: một canvas vẽ lại theo dòng hiện tại. */
    private final class Canvas extends JComponent implements ListCellRenderer<Integer> {
        private static final long serialVersionUID = 1L;
        private int index;
        private boolean selected;
        @Override public Component getListCellRendererComponent(JList<? extends Integer> l, Integer value, int i, boolean sel, boolean focus) {
            index = value == null ? -1 : value; selected = sel; return this;
        }
        @Override protected void paintComponent(Graphics graphics) {
            if (index < 0 || index >= rows.size()) return;
            Graphics2D g = (Graphics2D) graphics.create(); Flat.smooth(g);
            boolean hov = index == hover;
            g.setColor(selected ? Theme.color("E7F6F3") : hov ? Theme.color("F4FAF9") : Theme.CARD);
            g.fillRect(0, 0, getWidth(), getHeight());
            if (selected) { g.setColor(Theme.PRIMARY); g.fillRoundRect(0, 8, 4, getHeight() - 16, 4, 4); }
            g.setColor(Theme.color("EEF2F4")); g.fillRect(12, getHeight() - 1, getWidth() - 24, 1);
            painter.paint(g, rows.get(index), new Rectangle(0, 0, getWidth(), getHeight()), selected, hov);
            g.dispose();
        }
    }

    // ------------------------------------------------------------------ helpers vẽ dùng chung
    /** Vẽ chuỗi, cắt bằng "…" nếu vượt bề rộng; trả về bề rộng đã vẽ. */
    public static int text(Graphics2D g, String s, int x, int baseline, int maxWidth, Font font, Color color) {
        g.setFont(font); g.setColor(color); FontMetrics fm = g.getFontMetrics();
        String shown = s;
        if (fm.stringWidth(shown) > maxWidth) {
            while (shown.length() > 1 && fm.stringWidth(shown + "…") > maxWidth) shown = shown.substring(0, shown.length() - 1);
            shown += "…";
        }
        g.drawString(shown, x, baseline); return fm.stringWidth(shown);
    }
    /** Vẽ viên trạng thái căn phải tại xRight; trả về x bắt đầu của viên. */
    public static int pillRight(Graphics2D g, String s, int xRight, int centerY, Color tone) {
        g.setFont(Theme.font(11, true)); FontMetrics fm = g.getFontMetrics();
        int w = fm.stringWidth(s) + 18, h = fm.getHeight() + 6, x = xRight - w, y = centerY - h / 2;
        g.setColor(Flat.tint(tone, 0.87)); g.fillRoundRect(x, y, w, h, h, h);
        g.setColor(tone); g.drawString(s, x + 9, y + 3 + fm.getAscent()); return x;
    }
}
