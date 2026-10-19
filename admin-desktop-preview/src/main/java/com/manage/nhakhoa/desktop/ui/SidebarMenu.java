package com.manage.nhakhoa.desktop.ui;

import com.manage.nhakhoa.desktop.MenuDefinition;
import com.manage.nhakhoa.desktop.MenuDefinition.Entry;
import com.manage.nhakhoa.desktop.MenuDefinition.Group;
import java.awt.*;
import java.awt.event.*;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.tree.*;
import static com.manage.nhakhoa.desktop.ui.UiComponents.*;

/** Menu dữ liệu 21 mục; JTree cung cấp focus, mũi tên và công cụ trợ năng. */
public final class SidebarMenu extends JPanel {
    private static final long serialVersionUID = 1L;
    private final JTextField search = new JTextField();
    private final JTree tree = new JTree();
    private final transient Set<String> expanded = new HashSet<>();
    private final transient Consumer<String> navigate;
    private String selected = MenuDefinition.HOME;
    private boolean rebuilding;

    public SidebarMenu(Consumer<String> navigate) {
        super(new BorderLayout(0, 16)); this.navigate = navigate;
        setBackground(Theme.SIDEBAR); setPreferredSize(new Dimension(272, 0));
        setBorder(new EmptyBorder(20, 10, 16, 10));
        JPanel brand = new JPanel(new BorderLayout(12, 0)); brand.setOpaque(false);
        brand.add(new IconTile("tooth", Color.WHITE, Theme.PRIMARY, 42), BorderLayout.WEST);
        brand.add(column(4, label("N08 Nha Khoa", 18, true, Color.WHITE),
                label("Quản lý phòng khám", 12, false, Theme.SIDE_MUTED)), BorderLayout.CENTER);
        search.setName("menu-search"); search.setToolTipText("Tìm chức năng không dấu, tên nhóm hoặc mã nghiệp vụ · Esc để xóa");
        search.getAccessibleContext().setAccessibleName("Tìm chức năng");
        search.setBackground(Theme.SEARCH); search.setForeground(Theme.SIDE_TEXT); search.setCaretColor(Theme.ACCENT);
        search.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Theme.SELECTED), new EmptyBorder(8, 10, 8, 10)));
        JPanel searchBox = column(5, label("Tìm chức năng", 11, false, Theme.SIDE_MUTED), search);
        add(column(22, brand, searchBox), BorderLayout.NORTH);
        tree.setName("navigation-tree"); tree.getAccessibleContext().setAccessibleName("Điều hướng quản trị");
        tree.setRootVisible(false); tree.setShowsRootHandles(false); tree.setRowHeight(36);
        tree.setBackground(Theme.SIDEBAR); tree.setForeground(Theme.SIDE_TEXT); tree.setToggleClickCount(0);
        tree.putClientProperty("JTree.lineStyle", "None");
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        tree.setCellRenderer(new MenuRenderer()); ToolTipManager.sharedInstance().registerComponent(tree);
        tree.addTreeSelectionListener(e -> {
            if (rebuilding) return;
            Object value = value(tree.getSelectionPath());
            if (value instanceof Entry entry) { selected = entry.key(); navigate.accept(selected); }
        });
        tree.addTreeExpansionListener(new TreeExpansionListener() {
            @Override public void treeExpanded(TreeExpansionEvent e) { remember(e, true); }
            @Override public void treeCollapsed(TreeExpansionEvent e) { remember(e, false); }
            private void remember(TreeExpansionEvent e, boolean open) {
                if (!rebuilding && search.getText().isBlank() && value(e.getPath()) instanceof Group group) {
                    if (open) expanded.add(group.key()); else expanded.remove(group.key());
                }
            }
        });
        tree.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                TreePath path = tree.getPathForLocation(e.getX(), e.getY());
                if (path != null && value(path) instanceof Group) toggle(path);
            }
        });
        Action activate = new AbstractAction() {
            private static final long serialVersionUID = 1L;
            @Override public void actionPerformed(ActionEvent e) {
                TreePath path = tree.getSelectionPath();
                if (value(path) instanceof Group) toggle(path);
                else if (value(path) instanceof Entry entry) navigate.accept(entry.key());
            }
        };
        tree.getInputMap().put(KeyStroke.getKeyStroke("ENTER"), "activate-screen");
        tree.getInputMap().put(KeyStroke.getKeyStroke("SPACE"), "activate-screen");
        tree.getActionMap().put("activate-screen", activate);
        search.getInputMap().put(KeyStroke.getKeyStroke("ESCAPE"), "clear-search");
        search.getActionMap().put("clear-search", new AbstractAction() {
            private static final long serialVersionUID = 1L;
            @Override public void actionPerformed(ActionEvent e) { search.setText(""); }
        });
        search.addActionListener(e -> {
            for (int i = 0; i < tree.getRowCount(); i++) {
                TreePath path = tree.getPathForRow(i);
                if (value(path) instanceof Entry) { tree.setSelectionPath(path); tree.requestFocusInWindow(); break; }
            }
        });
        search.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { rebuild(); }
            @Override public void removeUpdate(DocumentEvent e) { rebuild(); }
            @Override public void changedUpdate(DocumentEvent e) { rebuild(); }
        });
        JScrollPane scroll = new JScrollPane(tree); scroll.setBorder(null);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(36); scroll.getViewport().setBackground(Theme.SIDEBAR);
        add(scroll, BorderLayout.CENTER);
        JLabel footer = label("Bản xem thử giao diện · UI-DEMO-1", 11, false, Theme.SIDE_MUTED);
        add(footer, BorderLayout.SOUTH); rebuild();
    }
    private static Object value(TreePath path) {
        return path == null ? null : ((DefaultMutableTreeNode) path.getLastPathComponent()).getUserObject();
    }
    private void toggle(TreePath path) { if (tree.isExpanded(path)) tree.collapsePath(path); else tree.expandPath(path); }
    private void rebuild() {
        rebuilding = true;
        try {
            DefaultMutableTreeNode root = new DefaultMutableTreeNode("Quản trị");
            String query = search.getText();
            if (MenuDefinition.matches(MenuDefinition.HOME_ENTRY, "", query)) root.add(new DefaultMutableTreeNode(MenuDefinition.HOME_ENTRY));
            for (Group group : MenuDefinition.GROUPS) {
                DefaultMutableTreeNode node = new DefaultMutableTreeNode(group);
                group.entries().stream().filter(e -> MenuDefinition.matches(e, group.label(), query))
                        .forEach(e -> node.add(new DefaultMutableTreeNode(e)));
                if (node.getChildCount() > 0) root.add(node);
            }
            if (root.getChildCount() == 0) root.add(new DefaultMutableTreeNode("Không tìm thấy chức năng"));
            tree.setModel(new DefaultTreeModel(root));
            for (int i = 0; i < root.getChildCount(); i++) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) root.getChildAt(i);
                if (node.getUserObject() instanceof Group group && (!query.isBlank() || expanded.contains(group.key()))) {
                    tree.expandPath(new TreePath(node.getPath()));
                }
            }
            findSelection(root);
        } finally { rebuilding = false; }
    }
    private void findSelection(DefaultMutableTreeNode root) {
        var nodes = root.depthFirstEnumeration();
        while (nodes.hasMoreElements()) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) nodes.nextElement();
            if (node.getUserObject() instanceof Entry entry && entry.key().equals(selected)) {
                tree.setSelectionPath(new TreePath(node.getPath())); return;
            }
        }
        tree.clearSelection();
    }
    public void select(String key) {
        Theme.requireEdt(); MenuDefinition.entry(key); selected = key;
        if (!search.getText().isEmpty()) search.setText("");
        for (Group group : MenuDefinition.GROUPS) {
            if (group.entries().stream().anyMatch(e -> e.key().equals(key))) expanded.add(group.key());
        }
        rebuild(); tree.scrollPathToVisible(tree.getSelectionPath());
    }
    private final class MenuRenderer extends JPanel implements TreeCellRenderer {
        private static final long serialVersionUID = 1L;
        @Override public Component getTreeCellRendererComponent(JTree t, Object node, boolean isSelected,
                boolean open, boolean leaf, int row, boolean hasFocus) {
            removeAll(); setLayout(new BorderLayout(7, 0));
            setBackground(isSelected ? Theme.SELECTED : Theme.SIDEBAR);
            setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(hasFocus ? Theme.ACCENT : getBackground()), new EmptyBorder(7, 5, 7, 5)));
            Object object = ((DefaultMutableTreeNode) node).getUserObject();
            String title = object.toString(), icon = "grid";
            if (object instanceof Entry entry) { title = entry.label(); icon = entry.icon(); }
            if (object instanceof Group group) { title = group.label(); icon = group.entries().getFirst().icon(); }
            add(new JLabel(new UiIcons(icon, isSelected ? Theme.ACCENT : Theme.SIDE_MUTED, 17)), BorderLayout.WEST);
            add(label(title, 12, object instanceof Group || isSelected, Theme.SIDE_TEXT), BorderLayout.CENTER);
            if (object instanceof Group group) {
                JPanel end = row(5);
                if (group.demoBadge() > 0) end.add(new StatusBadge("" + group.demoBadge(), Color.WHITE, Theme.RED));
                end.add(label(open ? "−" : "›", 13, true, Theme.SIDE_MUTED)); add(end, BorderLayout.EAST);
                setToolTipText(group.label() + " · " + group.demoBadge()
                        + " nhóm việc trên Tổng quan cần xử lý ở giai đoạn này (dữ liệu mô phỏng)");
            } else setToolTipText(title);
            setPreferredSize(new Dimension(object instanceof Group ? 247 : 231, 36));
            return this;
        }
        @Override public void paint(Graphics graphics) {
            // CellRendererPane không đảm bảo validate cây JPanel lồng nhau trước khi vẽ.
            layoutRenderer(this);
            super.paint(graphics);
        }
        private static void layoutRenderer(Container container) {
            container.doLayout();
            for (Component child : container.getComponents()) {
                if (child instanceof Container nested) layoutRenderer(nested);
            }
        }
    }
}
