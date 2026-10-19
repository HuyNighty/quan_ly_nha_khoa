package com.manage.nhakhoa.desktop;

import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.Clock;
import javax.swing.JFrame;

/** Vòng đời riêng của bản xem thử, không khởi động Spring. */
public final class AdminPreviewFrame extends JFrame {
    private static final long serialVersionUID = 1L;
    public AdminPreviewFrame(Clock clock, boolean fixture) {
        super("N08 Nha Khoa · Quản trị · Bản xem thử UI-M1");
        AdminShell shell = new AdminShell(clock, fixture); setContentPane(shell);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE); setSize(1382, 840); setMinimumSize(new Dimension(1280, 760));
        setLocationRelativeTo(null);
        addWindowListener(new WindowAdapter() { @Override public void windowClosed(WindowEvent e) { shell.close(); } });
    }
}
