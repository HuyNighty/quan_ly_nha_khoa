package com.manage.nhakhoa.desktop;

import com.manage.nhakhoa.desktop.demo.DemoClinicStore;
import com.manage.nhakhoa.desktop.screens.*;
import com.manage.nhakhoa.desktop.ui.DemoDialogs;
import com.manage.nhakhoa.desktop.ui.SidebarMenu;
import com.manage.nhakhoa.desktop.ui.Theme;
import com.manage.nhakhoa.desktop.ui.TopBar;
import java.awt.*;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import static com.manage.nhakhoa.desktop.ui.UiComponents.*;

/** Một router CardLayout trong cùng cửa sổ, không tạo CRUD hay JFrame khi điều hướng. */
public final class AdminShell extends JPanel implements AutoCloseable {
    private static final long serialVersionUID = 1L;
    private final SidebarMenu sidebar;
    private final TopBar topbar = new TopBar();
    private final DashboardPanel dashboard;
    private final JPanel cards = new JPanel(new CardLayout());
    private final JLabel placeholderTitle = label("", 24, true, Theme.TEXT);
    private final transient DemoClinicStore store;
    private final transient Map<String, JComponent> screens = new LinkedHashMap<>();
    private String currentScreen = MenuDefinition.HOME;
    public AdminShell(Clock clock, boolean fixture) { this(clock, fixture, new DemoClinicStore(clock), DemoDialogs.swing()); }
    public AdminShell(Clock clock, boolean fixture, DemoClinicStore store, DemoDialogs dialogs) {
        super(new BorderLayout()); Theme.requireEdt(); this.store = store;
        sidebar = new SidebarMenu(this::navigate); add(sidebar, BorderLayout.WEST);
        dashboard = new DashboardPanel(clock, fixture, this::navigate);
        cards.add(scroll(dashboard), MenuDefinition.HOME);
        JPanel placeholder = new JPanel(new BorderLayout()); placeholder.setBackground(Theme.BACKGROUND);
        placeholder.setBorder(new EmptyBorder(24, 24, 24, 24));
        placeholderTitle.setName("placeholder-title");
        placeholder.add(column(18, placeholderTitle,
                new UiCard("Chưa triển khai", null, column(18,
                        label("Màn hình này chưa được triển khai ở giai đoạn UI-M1", 15, true, Theme.TEXT),
                        label("Bản xem thử chỉ hỗ trợ điều hướng. Chức năng nghiệp vụ cần đặc tả được duyệt.", 13, false, Theme.MUTED),
                        primary("Về Tổng quan", () -> navigate(MenuDefinition.HOME))))), BorderLayout.NORTH);
        cards.add(scroll(placeholder), "UNIMPLEMENTED");
        // UI-DEMO-1: các màn mô phỏng dùng chung một DemoClinicStore trong bộ nhớ.
        ScreenContext context = new ScreenContext(store, this::navigate, dialogs);
        screens.put("TIEP_NHAN", new ReceptionPanel(context));
        screens.put("LICH_HEN", new AppointmentsPanel(context));
        screens.put("BENH_NHAN", new PatientsPanel(context));
        // Tổ chức & Lập kế hoạch
        screens.put("NHAN_VIEN", new StaffPanel(context));
        screens.put("TAI_KHOAN", new AccountsPanel(context));
        screens.put("BANG_GIA", new PricePanel(context));
        screens.put("NHA_CUNG_CAP", new SupplierPanel(context));
        screens.put("PHAN_CONG", new ShiftPanel(context));
        // Khám, Điều trị, Thanh toán
        screens.put("PHIEU_KHAM", new ExamPanel(context));
        screens.put("KE_HOACH", new PlanPanel(context));
        screens.put("TAI_KHAM", new FollowUpPanel(context));
        screens.put("HOA_DON", new InvoicePanel(context));
        screens.put("DUYET_HUY", new CancelApprovalPanel(context));
        screens.put("THUOC_VAT_TU", new StockPanel(context));
        screens.put("NHAP_XUAT", new VoucherPanel(context));
        screens.put("KIEM_KE", new AdjustPanel(context));
        screens.put("CAP_CUU", new KitPanel(context));
        screens.put("BAN_GIAO", new HandoverPanel(context));
        screens.put("DANH_GIA", new EvaluationPanel(context));
        screens.put("NHAT_KY", new AuditPanel(context));
        screens.forEach((key, panel) -> cards.add(scroll(panel), key));
        JPanel workspace = new JPanel(new BorderLayout()); workspace.add(topbar, BorderLayout.NORTH); workspace.add(cards, BorderLayout.CENTER);
        add(workspace, BorderLayout.CENTER); sidebar.select(MenuDefinition.HOME);
    }
    private static JScrollPane scroll(JComponent content) {
        JPanel sizing = new WidthTrackingPanel(); sizing.add(content, BorderLayout.NORTH);
        JScrollPane pane = new JScrollPane(sizing); pane.setBorder(null); pane.setName("content-scroll");
        pane.getViewport().setBackground(Theme.BACKGROUND);
        pane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        pane.getVerticalScrollBar().setUnitIncrement(24); return pane;
    }
    public void navigate(String key) {
        Theme.requireEdt(); var entry = MenuDefinition.entry(key); currentScreen = key;
        topbar.setScreen(entry.label()); sidebar.select(key);
        placeholderTitle.setText(entry.label()); placeholderTitle.setToolTipText(entry.label());
        ((CardLayout) cards.getLayout()).show(cards, key.equals(MenuDefinition.HOME) || screens.containsKey(key) ? key : "UNIMPLEMENTED");
    }
    public String currentScreen() { return currentScreen; }
    @Override public void paint(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        super.paint(g); g.dispose();
    }
    public DashboardPanel dashboard() { return dashboard; }
    public DemoClinicStore store() { return store; }
    /** Màn đã có giao diện mô phỏng (không còn placeholder). */
    public boolean implemented(String key) { return key.equals(MenuDefinition.HOME) || screens.containsKey(key); }
    public JComponent screen(String key) { return screens.get(key); }
    @Override public void close() { dashboard.close(); }
    private static final class WidthTrackingPanel extends JPanel implements Scrollable {
        private static final long serialVersionUID = 1L;
        WidthTrackingPanel() { super(new BorderLayout()); setBackground(Theme.BACKGROUND); }
        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override public int getScrollableUnitIncrement(Rectangle r, int orientation, int direction) { return 24; }
        @Override public int getScrollableBlockIncrement(Rectangle r, int orientation, int direction) { return Math.max(24, r.height - 24); }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override public boolean getScrollableTracksViewportHeight() { return false; }
    }
}
