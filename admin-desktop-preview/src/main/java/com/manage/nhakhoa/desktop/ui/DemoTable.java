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
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableRowSorter;

/**
 * Bảng dùng chung: header phẳng, dòng xen kẽ, hover, trạng thái dạng viên màu, sort theo cột,
 * giữ lựa chọn theo mã khi dữ liệu đổi, chiều cao co theo số dòng, empty state có icon và hướng dẫn.
 */
public final class DemoTable<T> extends JPanel implements RowView<T> {
    private static final long serialVersionUID = 1L;
    public static final int ROW = 36, HEADER = 34;
    /** Giá trị ô có màu (vẽ dạng viên trạng thái). */
    public record Tag(String text, Color color) implements Comparable<Tag> {
        @Override public String toString() { return text; }
        @Override public int compareTo(Tag o) { return text.compareTo(o.text); }
    }
    /** Ô chữ đậm (mã, giờ) để mắt bắt nhanh cột khóa. */
    public record Strong(String text) implements Comparable<Strong> {
        @Override public String toString() { return text; }
        @Override public int compareTo(Strong o) { return text.compareTo(o.text); }
    }
    public record Column<T>(String title, Function<T, Object> value, int width) { }

    private final transient List<Column<T>> columns;
    private final transient Function<T, String> idOf;
    private transient List<T> rows = List.of();
    private transient Consumer<T> onSelect = row -> { };
    private final Model model;
    private final JTable table;
    private final JLabel empty;
    private final JPanel cards = new JPanel(new CardLayout());
    private final int maxRows;
    private boolean adjusting;
    private int hover = -1;

    public DemoTable(String name, List<Column<T>> columns, Function<T, String> idOf, int maxRows) {
        super(new BorderLayout());
        this.columns = List.copyOf(columns); this.idOf = idOf; this.maxRows = maxRows;
        setOpaque(false); setName(name);
        model = new Model(); table = new FitTable(model);
        table.setName(name + "-table");
        table.setRowHeight(ROW); table.setFont(Theme.font(13, false)); table.setShowGrid(false); table.setShowHorizontalLines(true);
        table.setIntercellSpacing(new Dimension(0, 1)); table.setGridColor(Theme.color("EDF2F4"));
        table.setSelectionBackground(Theme.color("D7F0EC")); table.setSelectionForeground(Theme.TEXT);
        table.setFillsViewportHeight(true); table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setBackground(Theme.CARD); table.setFocusable(true);
        table.getTableHeader().setDefaultRenderer(new HeaderRenderer()); table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setPreferredSize(new Dimension(10, HEADER));
        table.getTableHeader().setBackground(Theme.SOFT);
        table.setRowSorter(new TableRowSorter<>(model));
        table.setDefaultRenderer(Object.class, new CellRenderer());
        table.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        for (int i = 0; i < this.columns.size(); i++) {
            var col = table.getColumnModel().getColumn(i); int w = this.columns.get(i).width();
            col.setPreferredWidth(w); col.setMinWidth(w <= 100 ? w : (int) (w * 0.75)); // cột mã/giờ không bị cắt
        }
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && !adjusting) selected().ifPresent(r -> onSelect.accept(r));
        });
        MouseAdapter hoverTracker = new MouseAdapter() {
            @Override public void mouseMoved(MouseEvent e) { setHover(table.rowAtPoint(e.getPoint())); }
            @Override public void mouseExited(MouseEvent e) { setHover(-1); }
        };
        table.addMouseMotionListener(hoverTracker); table.addMouseListener(hoverTracker);
        JScrollPane scroll = new JScrollPane(table); scroll.setColumnHeaderView(table.getTableHeader()); scroll.setBorder(BorderFactory.createLineBorder(Theme.color("E2E8F0")));
        scroll.getViewport().setBackground(Theme.CARD); Flat.thin(scroll);
        empty = UiComponents.label("", 13, false, Theme.MUTED); empty.setName(name + "-empty");
        empty.setIcon(new UiIcons("search", Theme.color("94A3B8"), 22)); empty.setIconTextGap(10);
        empty.setHorizontalAlignment(SwingConstants.CENTER); empty.setBorder(new EmptyBorder(18, 12, 18, 12));
        JPanel emptyBox = new DemoWidgets.Tile(new BorderLayout(), Theme.SOFT, Theme.color("E2E8F0")); emptyBox.add(empty);
        cards.setOpaque(false); cards.add(scroll, "table"); cards.add(emptyBox, "empty");
        add(cards);
        resize();
    }
    private void setHover(int row) { if (row != hover) { hover = row; table.repaint(); } }
    private void resize() {
        int visible = rows.isEmpty() ? 3 : Math.max(3, Math.min(maxRows, rows.size()));
        cards.setPreferredSize(new Dimension(200, HEADER + ROW * visible + 3));
        revalidate();
    }
    public void onSelect(Consumer<T> listener) { this.onSelect = Objects.requireNonNull(listener); }
    public JTable table() { return table; }
    public List<T> rows() { return rows; }
    public boolean isEmptyState() { return rows.isEmpty(); }
    public String emptyText() { return empty.getText(); }

    /** Thay dữ liệu; giữ dòng đang chọn nếu mã vẫn còn, không phát sự kiện chọn giả. */
    public void setRows(List<T> newRows, String emptyText) {
        Theme.requireEdt();
        String keep = selected().map(idOf).orElse(null);
        adjusting = true;
        try {
            table.clearSelection();
            rows = List.copyOf(newRows); model.fireTableDataChanged(); table.clearSelection();
            empty.setText(emptyText); empty.setToolTipText(emptyText);
            ((CardLayout) cards.getLayout()).show(cards, rows.isEmpty() ? "empty" : "table");
            if (keep != null) selectQuietly(keep);
        } finally { adjusting = false; }
        resize();
    }
    public Optional<T> selected() {
        int view = table.getSelectedRow();
        if (view < 0 || view >= table.getRowCount()) return Optional.empty();
        int index = table.convertRowIndexToModel(view);
        return index < rows.size() ? Optional.of(rows.get(index)) : Optional.empty();
    }
    /** Chọn theo mã và phát sự kiện chọn; trả về false nếu không có trong bảng. */
    public boolean select(String id) {
        if (!selectQuietly(id)) return false;
        selected().ifPresent(r -> onSelect.accept(r)); return true;
    }
    private boolean selectQuietly(String id) {
        boolean old = adjusting; adjusting = true;
        try {
            for (int i = 0; i < rows.size(); i++) {
                if (idOf.apply(rows.get(i)).equals(id)) {
                    int view = table.convertRowIndexToView(i);
                    if (view < 0) return false;
                    table.setRowSelectionInterval(view, view);
                    table.scrollRectToVisible(table.getCellRect(view, 0, true));
                    return true;
                }
            }
            table.clearSelection(); return false;
        } finally { adjusting = old; }
    }
    public void clearSelection() { boolean old = adjusting; adjusting = true; table.clearSelection(); adjusting = old; }

    private final class Model extends AbstractTableModel {
        private static final long serialVersionUID = 1L;
        @Override public int getRowCount() { return rows.size(); }
        @Override public int getColumnCount() { return columns.size(); }
        @Override public String getColumnName(int c) { return columns.get(c).title(); }
        @Override public Object getValueAt(int r, int c) { Object v = columns.get(c).value().apply(rows.get(r)); return v == null ? "" : v; }
    }
    /** Giãn theo khung khi đủ rộng; khi hẹp giữ bề rộng tối thiểu và hiện thanh cuộn ngang thay vì cắt chữ. */
    private static final class FitTable extends JTable {
        private static final long serialVersionUID = 1L;
        FitTable(AbstractTableModel model) { super(model); setAutoResizeMode(AUTO_RESIZE_SUBSEQUENT_COLUMNS); }
        @Override public boolean getScrollableTracksViewportWidth() {
            Container parent = getParent();
            int min = 0; for (int i = 0; i < getColumnModel().getColumnCount(); i++) min += getColumnModel().getColumn(i).getMinWidth();
            return parent == null || parent.getWidth() >= min;
        }
    }
    private static final class HeaderRenderer extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1L;
        @Override public Component getTableCellRendererComponent(JTable t, Object value, boolean s, boolean f, int row, int col) {
            JLabel l = (JLabel) super.getTableCellRendererComponent(t, value, false, false, row, col);
            l.setText(String.valueOf(value).toUpperCase(Theme.VI)); l.setFont(Theme.font(11, true)); l.setForeground(Theme.MUTED);
            l.setBackground(Theme.SOFT); l.setOpaque(true);
            l.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.color("E2E8F0")), new EmptyBorder(0, 12, 0, 8)));
            return l;
        }
    }
    private final class CellRenderer implements TableCellRenderer {
        private final PillCell cell = new PillCell();
        @Override public Component getTableCellRendererComponent(JTable t, Object value, boolean selected, boolean focus, int row, int col) {
            Color bg = selected ? t.getSelectionBackground() : row == hover ? Theme.color("F1F8F7") : row % 2 == 1 ? Theme.color("FAFCFC") : Theme.CARD;
            cell.setBackground(bg); cell.setText(String.valueOf(value));
            // Chỉ hiện tooltip khi chữ bị cắt, tránh khung nổi đè lên dòng khác khi rê chuột.
            int colWidth = t.getColumnModel().getColumn(col).getWidth();
            int textWidth = cell.getFontMetrics(value instanceof Strong ? Theme.font(13, true) : value instanceof Tag ? Theme.font(11, true) : Theme.font(13, false))
                    .stringWidth(String.valueOf(value)) + (value instanceof Tag ? 40 : 24);
            cell.setToolTipText(textWidth > colWidth ? String.valueOf(value) : null);
            if (value instanceof Tag tag) { cell.tone = tag.color(); cell.setFont(Theme.font(11, true)); cell.setForeground(tag.color()); }
            else {
                cell.tone = null; boolean strong = value instanceof Strong;
                cell.setFont(Theme.font(13, strong)); cell.setForeground(strong ? Theme.TEXT : Theme.color("334155"));
            }
            return cell;
        }
    }
    /** Ô bảng tự vẽ: chữ thường, hoặc viên trạng thái bo tròn khi có tone (không cần layout con). */
    private static final class PillCell extends JLabel {
        private static final long serialVersionUID = 1L;
        private transient Color tone;
        PillCell() { setOpaque(true); setBorder(new EmptyBorder(0, 12, 0, 8)); }
        @Override protected void paintComponent(Graphics graphics) {
            if (tone == null) { super.paintComponent(graphics); return; }
            Graphics2D g = (Graphics2D) graphics.create(); Flat.smooth(g);
            g.setColor(getBackground()); g.fillRect(0, 0, getWidth(), getHeight());
            g.setFont(getFont()); FontMetrics fm = g.getFontMetrics();
            String text = getText(); int w = Math.min(fm.stringWidth(text) + 20, getWidth() - 16), h = fm.getHeight() + 8;
            int x = 10, y = (getHeight() - h) / 2;
            g.setColor(Flat.tint(tone, 0.87)); g.fillRoundRect(x, y, w, h, h, h);
            g.setColor(tone);
            String shown = text;
            while (fm.stringWidth(shown) > w - 20 && shown.length() > 1) shown = shown.substring(0, shown.length() - 1);
            if (!shown.equals(text)) shown = shown.substring(0, Math.max(1, shown.length() - 1)) + "…";
            g.drawString(shown, x + 10, y + 4 + fm.getAscent()); g.dispose();
        }
    }
}
