package com.manage.nhakhoa.desktop;

import com.manage.nhakhoa.desktop.ui.Theme;
import java.awt.GraphicsEnvironment;
import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import javax.swing.SwingUtilities;

/** Điểm chạy độc lập: --fixture hoặc --runtime (mặc định). */
public final class AdminPreview {
    private AdminPreview() { }
    public static Clock fixtureClock() { return Clock.fixed(Instant.parse("2026-10-12T00:45:00Z"), Theme.ZONE); }
    public static void main(String[] args) {
        if (args.length > 1 || Arrays.stream(args).anyMatch(a -> !a.equals("--fixture") && !a.equals("--runtime"))) {
            System.err.println("Tham số hợp lệ: --fixture hoặc --runtime"); return;
        }
        if (GraphicsEnvironment.isHeadless()) {
            System.err.println("Cần môi trường có màn hình để mở bản xem thử Swing"); return;
        }
        boolean fixture = Arrays.asList(args).contains("--fixture");
        Clock clock = fixture ? fixtureClock() : Clock.system(Theme.ZONE);
        SwingUtilities.invokeLater(() -> { Theme.install(); new AdminPreviewFrame(clock, fixture).setVisible(true); });
    }
}
