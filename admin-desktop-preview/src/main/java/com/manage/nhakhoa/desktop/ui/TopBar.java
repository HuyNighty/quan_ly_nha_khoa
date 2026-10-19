package com.manage.nhakhoa.desktop.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import static com.manage.nhakhoa.desktop.ui.UiComponents.*;

/** Danh tính minh họa; không tạo phiên đăng nhập hoặc thao tác xác thực giả. */
public final class TopBar extends JPanel {
    private static final long serialVersionUID = 1L;
    private final JLabel breadcrumb = label("Quản trị / Tổng quan", 12, true, Theme.TEXT);
    public TopBar() {
        super(new BorderLayout(12, 0)); setBackground(Theme.CARD);
        setPreferredSize(new Dimension(0, 52)); setBorder(new EmptyBorder(8, 24, 8, 24));
        breadcrumb.setName("breadcrumb"); add(breadcrumb, BorderLayout.CENTER);
        JPanel actions = row(12);
        actions.add(new StatusBadge("Dữ liệu mô phỏng", Theme.PRIMARY_TEXT, Theme.LIGHT));
        JLabel bell = new JLabel(new UiIcons("bell", Theme.MUTED, 20));
        bell.setToolTipText("Thông báo chỉ hiển thị trong bản xem thử"); bell.getAccessibleContext().setAccessibleName("Thông báo · chỉ hiển thị"); actions.add(bell);
        JButton account = secondary("Quản trị viên · Quản trị", () -> { }); account.setName("account-menu");
        JPopupMenu menu = new JPopupMenu();
        for (String text : new String[]{"Đổi mật khẩu", "Đăng xuất"}) {
            JMenuItem item = new JMenuItem(text); item.setEnabled(false);
            item.setToolTipText("Chỉ khả dụng khi tích hợp đăng nhập"); menu.add(item);
        }
        menu.addSeparator(); JMenuItem explanation = new JMenuItem("Chỉ khả dụng khi tích hợp đăng nhập"); explanation.setEnabled(false); menu.add(explanation);
        account.setToolTipText("Người dùng mô phỏng · chưa tích hợp đăng nhập");
        account.setComponentPopupMenu(menu);
        account.addActionListener(e -> menu.show(account, 0, account.getHeight())); actions.add(account);
        add(actions, BorderLayout.EAST);
    }
    public void setScreen(String label) { Theme.requireEdt(); breadcrumb.setText("Quản trị / " + label); breadcrumb.setToolTipText(breadcrumb.getText()); }
}
