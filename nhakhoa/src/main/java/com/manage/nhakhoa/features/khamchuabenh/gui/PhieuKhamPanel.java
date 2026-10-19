package com.manage.nhakhoa.features.khamchuabenh.gui;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
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
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * UI skeleton for examination tickets and reception. Actual check-in and
 * persistence must be implemented in TiepNhanService, not in this panel.
 */
public final class PhieuKhamPanel extends JPanel {

    private static final Color PAGE_BACKGROUND = new Color(245, 248, 250);
    private static final Color ACCENT = new Color(18, 142, 137);
    private static final Color TEXT = new Color(34, 49, 54);
    private static final Color MUTED = new Color(105, 122, 124);
    private static final Color BORDER = new Color(220, 228, 232);

    private final JTextField dateField = new JTextField(LocalDate.now().toString(), 12);
    private final JTextField keywordField = new JTextField(18);
    private final JComboBox<String> statusFilter = new JComboBox<>(new String[]{
            "Tất cả trạng thái", "Chờ khám", "Đang khám", "Đã kết luận", "Đã hủy"
    });
    private final JLabel statusLabel = new JLabel("UI khung — chưa kết nối TiepNhanService hoặc cơ sở dữ liệu.");
    private final JButton refreshButton = new JButton("Tải hàng đợi");
    private final JButton checkInButton = new JButton("Check-in lịch hẹn");
    private final JButton directIntakeButton = new JButton("Tiếp nhận trực tiếp");
    private final JButton viewButton = new JButton("Xem phiếu khám");

    private final DefaultTableModel tableModel = new DefaultTableModel(
            new Object[]{"Mã phiếu", "Giờ tiếp nhận", "Mã bệnh nhân", "Bệnh nhân",
                    "Mã lịch hẹn", "Nhân viên tiếp nhận", "Trạng thái"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable examinationTable = new JTable(tableModel);

    public PhieuKhamPanel() {
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

        JLabel title = new JLabel("Tiếp nhận / Phiếu khám");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 24f));
        title.setForeground(TEXT);
        JLabel subtitle = new JLabel("Hàng đợi khám trong ngày và thao tác tiếp nhận bệnh nhân");
        subtitle.setForeground(MUTED);
        JPanel titleBlock = new JPanel(new BorderLayout(0, 4));
        titleBlock.setOpaque(false);
        titleBlock.add(title, BorderLayout.NORTH);
        titleBlock.add(subtitle, BorderLayout.SOUTH);

        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);
        titleRow.add(titleBlock, BorderLayout.WEST);

        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filters.setOpaque(false);
        filters.add(new JLabel("Ngày (yyyy-MM-dd):"));
        filters.add(dateField);
        filters.add(new JLabel("Mã/tên/SĐT bệnh nhân:"));
        filters.add(keywordField);
        filters.add(new JLabel("Trạng thái:"));
        filters.add(statusFilter);
        styleSecondary(refreshButton);
        filters.add(refreshButton);

        stylePrimary(checkInButton);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actions.setOpaque(false);
        actions.add(checkInButton);
        actions.add(directIntakeButton);
        actions.add(viewButton);

        JPanel controls = new JPanel(new BorderLayout(0, 10));
        controls.setOpaque(false);
        controls.add(filters, BorderLayout.NORTH);
        controls.add(actions, BorderLayout.SOUTH);
        header.add(titleRow, BorderLayout.NORTH);
        header.add(controls, BorderLayout.SOUTH);
        return header;
    }

    private JScrollPane buildTable() {
        examinationTable.setRowHeight(32);
        examinationTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        examinationTable.setAutoCreateRowSorter(true);
        examinationTable.getTableHeader().setReorderingAllowed(false);
        examinationTable.setFillsViewportHeight(true);

        JScrollPane scrollPane = new JScrollPane(examinationTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(BORDER));
        scrollPane.getViewport().setBackground(Color.WHITE);
        return scrollPane;
    }

    private void registerEvents() {
        refreshButton.addActionListener(event -> validateDate());
        dateField.addActionListener(event -> validateDate());
        checkInButton.setEnabled(false);
        directIntakeButton.setEnabled(false);
        viewButton.setEnabled(false);
        checkInButton.setToolTipText("Chờ TiepNhanService và AuthSession.");
        directIntakeButton.setToolTipText("Chờ TiepNhanService và BenhNhanService.");
        viewButton.setToolTipText("Chờ truy vấn chi tiết phiếu khám.");
    }

    private void validateDate() {
        try {
            LocalDate.parse(dateField.getText().trim());
        } catch (DateTimeParseException exception) {
            JOptionPane.showMessageDialog(this,
                    "Ngày không hợp lệ. Vui lòng nhập theo yyyy-MM-dd.",
                    "Kiểm tra ngày", JOptionPane.WARNING_MESSAGE);
            dateField.requestFocusInWindow();
            return;
        }
        statusLabel.setText("Bộ lọc hợp lệ; hàng đợi sẽ tải khi có TiepNhanService.");
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
