package com.manage.nhakhoa.features.lichhen.gui;

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
 * UI skeleton for appointment management. It does not create, update, or query
 * LichHen records until the feature service is connected.
 */
public final class LichHenPanel extends JPanel {

    private static final Color PAGE_BACKGROUND = new Color(245, 248, 250);
    private static final Color ACCENT = new Color(18, 142, 137);
    private static final Color TEXT = new Color(34, 49, 54);
    private static final Color MUTED = new Color(105, 122, 124);
    private static final Color BORDER = new Color(220, 228, 232);

    private final JTextField dateField = new JTextField(LocalDate.now().toString(), 12);
    private final JComboBox<String> statusFilter = new JComboBox<>(new String[]{
            "Tất cả trạng thái", "Chờ xác nhận", "Đã xác nhận", "Đã đến",
            "Hoàn thành", "Vắng mặt", "Đã hủy", "Đã đổi lịch"
    });
    private final JLabel statusLabel = new JLabel("UI khung — chưa kết nối LichHenService hoặc cơ sở dữ liệu.");
    private final JButton refreshButton = new JButton("Tải lịch");
    private final JButton addButton = new JButton("Tạo lịch hẹn");
    private final JButton rescheduleButton = new JButton("Đổi lịch");
    private final JButton cancelButton = new JButton("Hủy lịch");

    private final DefaultTableModel tableModel = new DefaultTableModel(
            new Object[]{"Giờ hẹn", "Mã lịch", "Mã bệnh nhân", "Bệnh nhân",
                    "Mã nhân viên", "Nhân viên phụ trách", "Lý do", "Trạng thái"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable appointmentTable = new JTable(tableModel);

    public LichHenPanel() {
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

        JLabel title = new JLabel("Quản lý lịch hẹn");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 24f));
        title.setForeground(TEXT);
        JLabel subtitle = new JLabel("Tra cứu lịch hẹn theo ngày và trạng thái");
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

        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filters.setOpaque(false);
        filters.add(new JLabel("Ngày (yyyy-MM-dd):"));
        filters.add(dateField);
        filters.add(new JLabel("Trạng thái:"));
        filters.add(statusFilter);
        styleSecondary(refreshButton);
        styleSecondary(rescheduleButton);
        styleSecondary(cancelButton);
        filters.add(refreshButton);
        filters.add(rescheduleButton);
        filters.add(cancelButton);

        header.add(titleRow, BorderLayout.NORTH);
        header.add(filters, BorderLayout.SOUTH);
        return header;
    }

    private JScrollPane buildTable() {
        appointmentTable.setRowHeight(32);
        appointmentTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        appointmentTable.setAutoCreateRowSorter(true);
        appointmentTable.getTableHeader().setReorderingAllowed(false);
        appointmentTable.setFillsViewportHeight(true);

        JScrollPane scrollPane = new JScrollPane(appointmentTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(BORDER));
        scrollPane.getViewport().setBackground(Color.WHITE);
        return scrollPane;
    }

    private void registerEvents() {
        refreshButton.addActionListener(event -> validateFilters());
        dateField.addActionListener(event -> validateFilters());
        addButton.setEnabled(false);
        rescheduleButton.setEnabled(false);
        cancelButton.setEnabled(false);
        addButton.setToolTipText("Chờ LichHenService và form lịch hẹn.");
        rescheduleButton.setToolTipText("Chờ LichHenService.");
        cancelButton.setToolTipText("Chờ LichHenService.");
    }

    private void validateFilters() {
        try {
            LocalDate.parse(dateField.getText().trim());
        } catch (DateTimeParseException exception) {
            JOptionPane.showMessageDialog(this,
                    "Ngày không hợp lệ. Vui lòng nhập theo yyyy-MM-dd.",
                    "Kiểm tra ngày", JOptionPane.WARNING_MESSAGE);
            dateField.requestFocusInWindow();
            return;
        }
        statusLabel.setText("Bộ lọc hợp lệ; danh sách sẽ tải khi có LichHenService.");
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
