package com.manage.nhakhoa.desktop.ui;

import com.manage.nhakhoa.desktop.model.DashboardViewModel.AppointmentStatus;
import com.manage.nhakhoa.desktop.model.DashboardViewModel.Priority;
import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Locale;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/** Token giao diện và định dạng hiển thị, không chứa quy tắc nghiệp vụ. */
public final class Theme {
    public static final Color BACKGROUND = color("EEF5F5"), CARD = Color.WHITE, BORDER = color("D5E3E4"),
            TEXT = color("111827"), MUTED = color("647084"), PRIMARY = color("0D9488"),
            PRIMARY_TEXT = color("08766D"), LIGHT = color("E6F4F2"), SIDEBAR = color("0F3B45"),
            SELECTED = color("18606B"), SEARCH = color("0A2D35"), SIDE_TEXT = color("E2E8F0"),
            SIDE_MUTED = color("A7C7CC"), ACCENT = color("5EEAD4"), RED = color("DC2626"),
            RED_LIGHT = color("FEF2F2"), ORANGE = color("C2410C"), ORANGE_LIGHT = color("FFF7ED"),
            GREEN = color("15803D"), GREEN_LIGHT = color("F0FDF4"), PURPLE = color("7C3AED"),
            PURPLE_LIGHT = color("F5F3FF"), SOFT = color("F8FAFC"), FOCUS = color("2563EB");
    public static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    public static final Locale VI = Locale.forLanguageTag("vi-VN");
    public static final int GAP = 16, PADDING = 24, RADIUS = 14;
    private static final String FONT = Arrays.asList(GraphicsEnvironment.getLocalGraphicsEnvironment()
            .getAvailableFontFamilyNames()).contains("Segoe UI") ? "Segoe UI" : Font.DIALOG;
    private Theme() { }
    public static Color color(String hex) { return new Color(Integer.parseInt(hex, 16)); }
    public static Font font(int size, boolean bold) { return new Font(FONT, bold ? Font.BOLD : Font.PLAIN, size); }
    public static String money(long amount) { return NumberFormat.getIntegerInstance(VI).format(amount) + " đ"; }
    public static String number(long count) { return NumberFormat.getIntegerInstance(VI).format(count); }
    public static String date(Instant time) { return DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss", VI).withZone(ZONE).format(time); }
    public static String day(Instant time) {
        String[] days = {"Thứ Hai", "Thứ Ba", "Thứ Tư", "Thứ Năm", "Thứ Sáu", "Thứ Bảy", "Chủ Nhật"};
        var local = time.atZone(ZONE);
        return days[local.getDayOfWeek().getValue() - 1] + ", "
                + DateTimeFormatter.ofPattern("dd/MM/yyyy").format(local);
    }
    public static String greeting(Instant time) {
        int hour = time.atZone(ZONE).getHour();
        return "Chào buổi " + (hour < 12 ? "sáng" : hour < 18 ? "chiều" : "tối") + ", Quản trị viên";
    }
    public static Color priority(Priority p) {
        return switch (p) { case IMMEDIATE -> RED; case PENDING -> ORANGE; case FOLLOW_UP -> PRIMARY_TEXT; };
    }
    public static Color status(AppointmentStatus s) {
        return switch (s) {
            case CHO_XAC_NHAN -> color("F59E0B"); case DA_XAC_NHAN -> color("0E7490");
            case DA_DEN -> color("8B5CF6"); case HOAN_THANH -> color("16A34A");
            case VANG_MAT -> color("475569"); case DA_HUY -> RED; case DA_DOI_LICH -> color("CBD5E1");
        };
    }
    public static void requireEdt() {
        if (!SwingUtilities.isEventDispatchThread()) { throw new IllegalStateException("Giao diện phải cập nhật trên EDT"); }
    }
    public static void install() {
        var keys = UIManager.getDefaults().keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            if (UIManager.get(key) instanceof Font) { UIManager.put(key, font(13, false)); }
        }
        UIManager.put("Button.focus", FOCUS);
        UIManager.put("ComboBox.background", CARD);
        UIManager.put("ScrollBar.width", 10);
        UIManager.put("Tree.leftChildIndent", 8);
        UIManager.put("Tree.rightChildIndent", 8);
    }
}
