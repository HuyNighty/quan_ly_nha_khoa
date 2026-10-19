package com.manage.nhakhoa.features.benhnhan.entity.gui;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;

/**
 * UI skeleton for the patient list. It intentionally contains no persistence,
 * repository, service, or entity-mutation logic yet.
 */
public final class BenhNhanPanel extends JPanel {

    private static final Color PAGE_BACKGROUND = new Color(245, 248, 250);
    private static final Color ACCENT = new Color(18, 142, 137);
    private static final Color TEXT = new Color(34, 49, 54);
    private static final Color MUTED = new Color(105, 122, 124);
    private static final Color BORDER = new Color(220, 228, 232);

    private final JTextField keywordField = new JTextField(24);
    private final JLabel statusLabel = new JLabel("UI khung — chưa kết nối BenhNhanService hoặc cơ sở dữ liệu.");
    private final JButton searchButton = new JButton("Tìm kiếm");
    private final JButton clearButton = new JButton("Xóa lọc");
    private final JButton addButton = new JButton("Thêm bệnh nhân");
    private final JButton editButton = new JButton("Sửa hồ sơ");
    private final JButton viewButton = new JButton("Xem hồ sơ");

    private final DefaultTableModel tableModel = new DefaultTableModel(
            new Object[]{"Mã bệnh nhân", "Họ tên", "Ngày sinh", "Giới tính", "Số điện thoại", "Địa chỉ"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable patientTable = new JTable(tableModel);

    public BenhNhanPanel() {
        setLayout(new BorderLayout(0, 16));
        setBorder(BorderFactory.createEmptyBorder(22, 24, 18, 24));
        setBackground(PAGE_BACKGROUND);

        add(buildHeader(), BorderLayout.NORTH);
        add(buildTable(), BorderLayout.CENTER);
        statusLabel.setForeground(MUTED);
        add(statusLabel, BorderLayout.SOUTH);

        registerEvents();
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout(0, 14));
        header.setOpaque(false);

        JLabel title = new JLabel("Quản lý bệnh nhân");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 24f));
        title.setForeground(TEXT);
        JLabel subtitle = new JLabel("Tìm kiếm và quản lý thông tin hồ sơ bệnh nhân");
        subtitle.setForeground(MUTED);
        JPanel titleBlock = new JPanel(new BorderLayout(0, 4));
        titleBlock.setOpaque(false);
        titleBlock.add(title, BorderLayout.NORTH);
        titleBlock.add(subtitle, BorderLayout.SOUTH);

        stylePrimary(addButton);
        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);
        titleRow.add(titleBlock, BorderLayout.WEST);
        titleRow.add(addButton, BorderLayout.EAST);

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        toolbar.setOpaque(false);
        toolbar.add(new JLabel("Từ khóa:"));
        toolbar.add(keywordField);
        styleSecondary(searchButton);
        styleSecondary(clearButton);
        styleSecondary(editButton);
        styleSecondary(viewButton);
        toolbar.add(searchButton);
        toolbar.add(clearButton);
        toolbar.add(editButton);
        toolbar.add(viewButton);

        header.add(titleRow, BorderLayout.NORTH);
        header.add(toolbar, BorderLayout.SOUTH);
        return header;
    }

    private JScrollPane buildTable() {
        patientTable.setRowHeight(32);
        patientTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        patientTable.setAutoCreateRowSorter(true);
        patientTable.getTableHeader().setReorderingAllowed(false);
        patientTable.setFillsViewportHeight(true);

        JScrollPane scrollPane = new JScrollPane(patientTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(BORDER));
        scrollPane.getViewport().setBackground(Color.WHITE);
        return scrollPane;
    }

    private void registerEvents() {
        searchButton.addActionListener(event -> showNotConnected());
        keywordField.addActionListener(event -> showNotConnected());
        clearButton.addActionListener(event -> {
            keywordField.setText("");
            statusLabel.setText("Đã xóa từ khóa. Danh sách sẽ được tải khi có BenhNhanService.");
        });

        addButton.setEnabled(false);
        editButton.setEnabled(false);
        viewButton.setEnabled(false);
        addButton.setToolTipText("Chờ kết nối BenhNhanService và form bệnh nhân.");
        editButton.setToolTipText("Chờ kết nối BenhNhanService.");
        viewButton.setToolTipText("Chờ kết nối truy vấn chi tiết hồ sơ.");
    }

    private void showNotConnected() {
        statusLabel.setText("Tìm kiếm chưa hoạt động: panel hiện chưa được nối BenhNhanService.");
    }

    private void stylePrimary(JButton button) {
        button.setBackground(ACCENT);
        button.setForeground(Color.WHITE);
        button.setOpaque(true);
        button.setFocusPainted(false);
    }

    private void styleSecondary(JButton button) {
        button.setForeground(ACCENT);
        button.setFocusPainted(false);
    }
}
