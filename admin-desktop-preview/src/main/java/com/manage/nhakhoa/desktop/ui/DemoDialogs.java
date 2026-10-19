package com.manage.nhakhoa.desktop.ui;

import java.awt.Component;
import javax.swing.JOptionPane;

/** Hộp thoại xác nhận cho thao tác mô phỏng; tách interface để kiểm thử không cần modal thật. */
public interface DemoDialogs {
    String TAG = "\n\n[MÔ PHỎNG] Chỉ thay đổi dữ liệu trong bộ nhớ của phiên xem thử.";
    boolean confirm(Component parent, String title, String message);
    /** Trả về lý do người dùng nhập hoặc null nếu hủy. */
    String reason(Component parent, String title, String prompt);

    static DemoDialogs swing() {
        return new DemoDialogs() {
            @Override public boolean confirm(Component parent, String title, String message) {
                return JOptionPane.showConfirmDialog(parent, message + TAG, title + " · Mô phỏng",
                        JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.OK_OPTION;
            }
            @Override public String reason(Component parent, String title, String prompt) {
                return JOptionPane.showInputDialog(parent, prompt + TAG, title + " · Mô phỏng", JOptionPane.QUESTION_MESSAGE);
            }
        };
    }
}
