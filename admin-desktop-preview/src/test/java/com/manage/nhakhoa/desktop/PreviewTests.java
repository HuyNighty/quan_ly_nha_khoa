package com.manage.nhakhoa.desktop;

import com.manage.nhakhoa.desktop.DashboardLoadController.LoadState;
import com.manage.nhakhoa.desktop.DashboardLoadController.Status;
import com.manage.nhakhoa.desktop.data.DashboardDataProvider;
import com.manage.nhakhoa.desktop.data.DemoDashboardDataProvider;
import com.manage.nhakhoa.desktop.data.DemoDashboardDataProvider.Scenario;
import com.manage.nhakhoa.desktop.model.DashboardViewModel;
import com.manage.nhakhoa.desktop.model.DashboardViewModel.*;
import com.manage.nhakhoa.desktop.ui.Theme;
import com.manage.nhakhoa.desktop.ui.UiComponents.StackedStatusBar;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

/** Kiểm thử độc lập bằng JDK, có assertion và ảnh render Swing thật, không cần DB. */
public final class PreviewTests {
    private static int checks;
    private static final Clock FIXED = AdminPreview.fixtureClock();
    private PreviewTests() { }
    public static void main(String[] args) throws Exception {
        edt(() -> { Theme.install(); return null; });
        modelsAndMenu(); asyncLoading(); shellAndStates(); longLabelsAndLargeCounts();
        if (List.of(args).contains("--desktop")) desktopLifecycle();
        System.out.println("ĐẠT: " + checks + " kiểm tra; không có lỗi. Ảnh Swing tại build/screenshots.");
    }
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        checks++; System.out.println("ĐẠT · " + message);
    }
    private static void modelsAndMenu() throws Exception {
        var data = DemoDashboardDataProvider.sample(FIXED, Scenario.FULL);
        check(data.date().equals(LocalDate.of(2026, 10, 12)) && Theme.date(data.loadedAt()).equals("12/10/2026 07:45:00"), "Ảnh mẫu đúng ngày, giờ và múi giờ");
        check(data.kpis().appointmentCount() == 10 && data.appointments().values().stream().mapToLong(Long::longValue).sum() == 10, "10 lịch = 2+2+1+2+1+1+1");
        check(data.kpis().waiting() == 0 && data.kpis().examining() == 1 && data.kpis().concluded() == 2, "Phiếu khám 0 / 1 / 2");
        check(Theme.money(data.kpis().received()).equals("700.000 đ") && Theme.money(data.kpis().debt()).equals("3.600.000 đ") && data.kpis().unpaidInvoices() == 5, "Tiền thu và công nợ đúng định dạng tiếng Việt");
        check(data.tasks().size() == 9 && data.affectedTotal() == 18 && data.tasks().stream().map(TaskSummary::id).distinct().count() == 9, "9 nhóm việc khác 18 đối tượng, mã việc duy nhất");
        check(data.tasks().stream().map(TaskSummary::priority).toList().equals(List.of(Priority.IMMEDIATE, Priority.IMMEDIATE, Priority.IMMEDIATE,
                Priority.PENDING, Priority.PENDING, Priority.PENDING, Priority.FOLLOW_UP, Priority.FOLLOW_UP, Priority.FOLLOW_UP)), "Thứ tự ưu tiên đỏ → cam → xanh");
        check(data.shifts().stream().filter(s -> s.missingNurses() > 0).count() == 2 && data.stock().belowMinimum() == 2
                && data.stock().expiringLots() == 2 && data.stock().expiredLots() == 1 && data.stock().emergencyNeedsSupply(), "Ca làm và tồn kho đúng mẫu");
        check(Theme.date(data.stock().emergencyCheckedAt()).equals("05/10/2026 07:45:00"), "Mốc kiểm tra hộp cấp cứu đúng ảnh 01");
        check(MenuDefinition.entries().size() == 21 && MenuDefinition.GROUPS.size() == 6
                && MenuDefinition.entries().stream().map(MenuDefinition.Entry::key).distinct().count() == 21, "Menu 21 khóa duy nhất / 6 nhóm");
        check(MenuDefinition.entry("TAI_KHOAN").label().equals("Tài khoản & vai trò"), "Nhãn tài khoản theo role-only");
        for (TaskSummary task : data.tasks()) check(MenuDefinition.entry(task.targetScreenKey()) != null, "Đích điều hướng hợp lệ: " + task.id());
        for (String q : List.of("lich hen", "cong no", "thuoc", "quan ly", "UC14", "đánh giá")) {
            check(MenuDefinition.GROUPS.stream().flatMap(g -> g.entries().stream().filter(e -> MenuDefinition.matches(e, g.label(), q))).findAny().isPresent(), "Tìm không dấu / tên nhóm / mã: " + q);
        }
        Clock runtime = Clock.fixed(Instant.parse("2026-10-19T08:15:00Z"), Theme.ZONE);
        check(DemoDashboardDataProvider.sample(runtime, Scenario.FULL).date().equals(LocalDate.of(2026,10,19)), "Ngày hiện tại qua Clock, không giữ nhầm ngày ảnh mẫu");
        check(Theme.greeting(FIXED.instant()).contains("sáng") && Theme.greeting(runtime.instant()).contains("chiều")
                && Theme.greeting(Instant.parse("2026-10-19T12:00:00Z")).contains("tối"), "Lời chào sáng / chiều / tối theo giờ phòng khám");
        check(Theme.day(Instant.parse("2026-10-18T17:01:00Z")).equals("Thứ Hai, 19/10/2026"), "Ranh giới ngày theo Asia/Ho_Chi_Minh");
        var zero = DemoDashboardDataProvider.sample(FIXED, Scenario.ZERO_APPOINTMENTS);
        check(zero.appointments().size() == 7 && zero.appointments().values().stream().allMatch(n -> n == 0), "Thiếu trạng thái được chuẩn hóa thành 0");
        boolean immutable = false;
        try { data.tasks().clear(); } catch (UnsupportedOperationException expected) { immutable = true; }
        check(immutable, "DTO không cho màn hình sửa bộ mẫu");
        boolean mismatch = false;
        try { new DashboardViewModel(data.date(), data.loadedAt(), data.source(), data.kpis(), Map.of(), data.tasks(), data.shifts(), data.stock()); }
        catch (IllegalArgumentException expected) { mismatch = true; }
        check(mismatch, "Chặn tổng trạng thái không khớp KPI");
        boolean negative = false;
        try { new ShiftSummary("Ca", -1); } catch (IllegalArgumentException expected) { negative = true; }
        check(negative, "Chặn số lượng âm");
        edt(() -> {
            StackedStatusBar bar = new StackedStatusBar(Map.of()); bar.setSize(400, 12);
            BufferedImage image = new BufferedImage(400, 12, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = image.createGraphics(); bar.paint(g); g.dispose();
            check(true, "Thanh tỷ lệ rỗng không chia cho 0"); return null;
        });
    }
    private static void asyncLoading() throws Exception {
        AtomicReference<LoadState> current = new AtomicReference<>();
        List<LoadState> history = new ArrayList<>();
        AtomicReference<Boolean> background = new AtomicReference<>(false);
        DashboardLoadController controller = edt(() -> new DashboardLoadController(FIXED, state -> {
            if (!SwingUtilities.isEventDispatchThread()) throw new AssertionError("Listener ngoài EDT");
            current.set(state); history.add(state);
        }));
        try {
            edt(() -> { controller.reload(c -> { background.set(!SwingUtilities.isEventDispatchThread()); return Optional.of(DemoDashboardDataProvider.sample(c, Scenario.FULL)); }); return null; });
            await(() -> current.get().status() == Status.LOADED);
            check(background.get(), "Provider chạy nền, listener cập nhật trên EDT");
            edt(() -> { controller.reload(c -> { throw new IllegalStateException("chi tiết kỹ thuật không được hiện ra UI"); });
                check(current.get().status() == Status.LOADING && current.get().data().isEmpty(), "Tải lại bỏ số liệu cũ ngay khi bắt đầu"); return null; });
            await(() -> current.get().status() == Status.ERROR);
            check(current.get().data().isEmpty(), "Provider lỗi không giữ dữ liệu cũ");
            edt(() -> { controller.reload(c -> Optional.empty()); return null; });
            await(() -> current.get().status() == Status.EMPTY);
            check(current.get().data().isEmpty(), "Nguồn rỗng khác nguồn lỗi");
            CountDownLatch oldStarted = new CountDownLatch(1), releaseOld = new CountDownLatch(1);
            DashboardDataProvider old = c -> {
                oldStarted.countDown(); boolean released = false;
                while (!released) { try { released = releaseOld.await(5, TimeUnit.SECONDS); } catch (InterruptedException ignored) { /* Cố ý mô phỏng nguồn không hợp tác hủy. */ } }
                return Optional.of(DemoDashboardDataProvider.sample(c, Scenario.FULL));
            };
            edt(() -> { controller.reload(old); return null; });
            if (!oldStarted.await(3, TimeUnit.SECONDS)) throw new AssertionError("Nguồn cũ chưa bắt đầu");
            edt(() -> { controller.reload(c -> Optional.of(DemoDashboardDataProvider.sample(c, Scenario.ZERO_APPOINTMENTS))); return null; });
            await(() -> current.get().status() == Status.LOADED);
            check(current.get().data().orElseThrow().kpis().appointmentCount() == 0, "Tải nhanh liên tiếp: lần mới nhất thắng");
            releaseOld.countDown(); Thread.sleep(100);
            edt(() -> { check(current.get().data().orElseThrow().kpis().appointmentCount() == 0, "Nguồn cũ bỏ qua hủy vẫn không ghi đè lần mới"); return null; });
            int beforeClose = edt(() -> history.size());
            edt(() -> { controller.close(); controller.reload(c -> Optional.empty()); return null; });
            Thread.sleep(60);
            check(edt(() -> history.size()) == beforeClose, "Đóng controller không phát sinh cập nhật muộn");
        } finally { edt(() -> { controller.close(); return null; }); }
    }
    private static void shellAndStates() throws Exception {
        AdminShell shell = edt(() -> new AdminShell(FIXED, true));
        try {
            await(() -> edtUnchecked(() -> shell.dashboard().status() == Status.LOADED));
            edt(() -> {
                check(texts(shell).contains("Dữ liệu mô phỏng") && texts(shell).contains("9 nhóm việc"), "Nhãn mô phỏng và badge 9 nhóm việc hiển thị");
                check(!texts(shell).contains("Bệnh nhân chờ khám") && texts(shell).contains("Phiếu chờ khám"), "KPI dùng đúng đơn vị phiếu khám");
                JPopupMenu accountMenu = ((JButton)find(shell,"account-menu")).getComponentPopupMenu();
                check(!accountMenu.getComponent(0).isEnabled() && !accountMenu.getComponent(1).isEnabled(), "Đổi mật khẩu và Đăng xuất bị vô hiệu hóa khi chưa tích hợp xác thực");
                snapshot(shell, 1382, 840, "dashboard-1382x840.png");
                snapshot(shell, 1280, 760, "dashboard-1280x760.png");
                JScrollPane contentScroll = all(shell).stream().filter(c -> c instanceof JScrollPane p
                        && "content-scroll".equals(p.getName()) && p.isVisible()).map(c -> (JScrollPane)c).findFirst().orElseThrow();
                check(contentScroll.getViewport().getView().getHeight() > contentScroll.getViewport().getHeight(), "Nội dung dài có vùng cuộn riêng ở 1280×760");
                contentScroll.getVerticalScrollBar().setValue(contentScroll.getVerticalScrollBar().getMaximum());
                snapshot(shell,1280,760,"dashboard-1280x760-bottom.png");
                contentScroll.getVerticalScrollBar().setValue(0);
                Component topbar = find(shell, "breadcrumb").getParent();
                check(topbar.getHeight() == 52 && topbar.getWidth() > 900, "Topbar giữ cao 52 và không bị cắt ở 1280×760");
                JTextField search = (JTextField) find(shell, "menu-search");
                JTree tree = (JTree) find(shell, "navigation-tree");
                for (String query : List.of("lich hen", "cong no", "thuoc", "quan ly")) {
                    search.setText(query); check(entryKeys(tree).size() > 0, "Sidebar lọc thực tế: " + query);
                }
                search.setText("không tìm thấy 123"); check(entryKeys(tree).isEmpty(), "Tìm không khớp không mở nhầm màn hình");
                key(search,"ESCAPE");
                check(search.getText().isEmpty() && entryKeys(tree).size() == 21, "Esc xóa tìm kiếm và phục hồi đủ 21 mục");
                TreePath group = tree.getPathForRow(1); tree.setSelectionPath(group);
                boolean expanded = tree.isExpanded(group);
                key(tree,"ENTER");
                check(tree.isExpanded(group) != expanded, "Enter / Space mở và thu nhóm");
                key(tree,"SPACE");
                check(tree.isExpanded(group) == expanded, "Thu nhóm bằng bàn phím lần hai");
                tree.setSelectionRow(0); key(tree,"DOWN");
                check(tree.getLeadSelectionRow() == 1, "Phím xuống di chuyển focus chọn trong cây menu");
                key(tree,"UP"); check(tree.getLeadSelectionRow() == 0, "Phím lên quay về mục trước");
                for (MenuDefinition.Entry entry : MenuDefinition.entries()) {
                    shell.navigate(entry.key());
                    check(shell.currentScreen().equals(entry.key()) && ((JLabel) find(shell, "breadcrumb")).getText().endsWith(entry.label()), "Router và breadcrumb: " + entry.key());
                }
                check(texts(shell).contains("Màn hình này chưa được triển khai ở giai đoạn UI-M1"), "Thông báo màn hình chưa triển khai chính xác");
                snapshot(shell, 1280, 760, "placeholder-1280x760.png");
                shell.navigate(MenuDefinition.HOME);
                click(shell, "kpi-appointments"); check(shell.currentScreen().equals("LICH_HEN"), "KPI lịch hẹn mở LICH_HEN");
                shell.navigate(MenuDefinition.HOME);
                click(shell, "open-appointments"); check(shell.currentScreen().equals("LICH_HEN"), "Link mở lịch hẹn cùng khóa KPI");
                shell.navigate(MenuDefinition.HOME);
                for (TaskSummary task : DemoDashboardDataProvider.sample(FIXED, Scenario.FULL).tasks()) {
                    click(shell, "task-" + task.id()); check(shell.currentScreen().equals(task.targetScreenKey()), "CTA chỉ điều hướng: " + task.id()); shell.navigate(MenuDefinition.HOME);
                }
                AbstractButton kpi = (AbstractButton) find(shell, "kpi-appointments");
                check(kpi.getInputMap().get(KeyStroke.getKeyStroke("ENTER")).equals("pressed")
                        && kpi.getInputMap().get(KeyStroke.getKeyStroke("SPACE")) != null, "Nút hỗ trợ Enter và Space qua key binding Swing");
                key(kpi,"ENTER"); key(kpi,"released ENTER");
                check(shell.currentScreen().equals("LICH_HEN"), "Chuỗi nhấn / thả Enter kích hoạt nút KPI");
                shell.navigate(MenuDefinition.HOME); key(kpi,"SPACE"); key(kpi,"released SPACE");
                check(shell.currentScreen().equals("LICH_HEN"), "Chuỗi nhấn / thả Space kích hoạt nút KPI");
                shell.navigate(MenuDefinition.HOME);
                click(shell,"reload"); snapshot(shell,1280,760,"loading-1280x760.png");
                return null;
            });
            choose(shell, Scenario.ERROR); awaitStatus(shell, Status.ERROR);
            edt(() -> {
                check(texts(shell).contains("Không thể tải dữ liệu") && findOptional(shell, "kpi-appointments") == null,
                        "Lỗi hiển thị trung thực, gỡ KPI cũ");
                snapshot(shell, 1280, 760, "error-1280x760.png");
                JButton retry = all(shell).stream().filter(c -> c instanceof JButton b && b.getText().equals("Thử lại")).map(c -> (JButton)c).findFirst().orElseThrow();
                retry.doClick(0); check(shell.dashboard().status() == Status.LOADING, "Thử lại thực sự bắt đầu yêu cầu tải"); return null;
            });
            awaitStatus(shell, Status.ERROR);
            choose(shell, Scenario.EMPTY); awaitStatus(shell, Status.EMPTY);
            edt(() -> { check(texts(shell).contains("Không có dữ liệu"), "Trạng thái không có dữ liệu hiển thị"); snapshot(shell, 1280, 760, "empty-1280x760.png"); return null; });
            choose(shell, Scenario.NO_TASKS); awaitStatus(shell, Status.LOADED);
            edt(() -> { check(texts(shell).contains("Không có việc đang chờ xử lý") && !texts(shell).contains("CẦN XỬ LÝ NGAY"), "0 việc: ẩn nhóm rỗng, hiển thị hướng dẫn đúng"); return null; });
            choose(shell, Scenario.ZERO_APPOINTMENTS); awaitStatus(shell, Status.LOADED);
            edt(() -> {
                check(texts(shell).contains("Tổng: 0 lịch hẹn"), "0 lịch hẹn render đầy đủ chú giải");
                snapshot(shell, 1280, 760, "zero-appointments-1280x760.png");
                click(shell, "reload"); shell.navigate("LICH_HEN");
                check(shell.dashboard().status() == Status.LOADING, "Có thể điều hướng trong khi đang tải"); return null;
            });
            awaitStatus(shell, Status.LOADED);
            check(edt(shell::currentScreen).equals("LICH_HEN"), "Tải xong không giành lại màn hình đang xem");
        } finally { edt(() -> { shell.close(); return null; }); }
    }
    private static void longLabelsAndLargeCounts() throws Exception {
        var base = DemoDashboardDataProvider.sample(FIXED, Scenario.FULL);
        String longTitle = "Hạng mục có tên rất dài cần được giữ trong tooltip để người dùng có thể đọc đầy đủ nội dung";
        var data = new DashboardViewModel(base.date(), base.loadedAt(), base.source(),
                new KpiSummary(14_000_000_000L,0,1,2,999_999_999_999L,0,0,999_999_999_999L,9_999_999),
                Map.of(AppointmentStatus.DA_XAC_NHAN,14_000_000_000L),
                List.of(new TaskSummary("long",Priority.IMMEDIATE,longTitle,longTitle,9_999_999,"LICH_HEN","Xem chi tiết")),base.shifts(),base.stock());
        DashboardPanel panel = edt(() -> new DashboardPanel(FIXED,true,k -> { },s -> c -> Optional.of(data)));
        try {
            await(() -> edtUnchecked(() -> panel.status() == Status.LOADED));
            edt(() -> { snapshot(panel, 1008, 760, "long-labels.png");
                check(all(panel).stream().anyMatch(c -> c instanceof JLabel l && longTitle.equals(l.getToolTipText())), "Nhãn dài giữ đầy đủ nội dung trong tooltip");
                check(texts(panel).contains("Tổng: 14.000.000.000 lịch hẹn"), "Số lượng lớn không tràn số nguyên 32 bit"); return null; });
        } finally { edt(() -> { panel.close(); return null; }); }
        Clock runtime = Clock.fixed(Instant.parse("2026-10-19T08:15:00Z"), Theme.ZONE);
        AdminShell runtimeShell = edt(() -> new AdminShell(runtime,false));
        try { awaitStatus(runtimeShell,Status.LOADED); edt(() -> {
            check(((JLabel)find(runtimeShell,"data-timestamp")).getText().contains("19/10/2026")
                    && ((JLabel)find(runtimeShell,"data-provenance")).getText().startsWith("Ngày hiện tại"), "Runtime hiển thị ngày mới và nguồn mẫu riêng biệt"); return null; });
        } finally { edt(() -> { runtimeShell.close(); return null; }); }
    }
    private static void desktopLifecycle() throws Exception {
        AdminPreviewFrame frame = edt(() -> { var f = new AdminPreviewFrame(FIXED,true); f.setVisible(true); return f; });
        try { Thread.sleep(600); edt(() -> {
            check(frame.isShowing() && frame.getWidth() == 1382 && frame.getHeight() == 840, "JFrame thật mở đúng kích thước trên desktop");
            AdminShell shell = (AdminShell) frame.getContentPane();
            snapshot(shell,shell.getWidth(),shell.getHeight(),"desktop-1382x840-content.png");
            frame.setSize(1280,760); frame.validate();
            snapshot(shell,shell.getWidth(),shell.getHeight(),"desktop-1280x760-content.png");
            check(find(shell,"account-menu").isShowing() && find(shell,"reload").getWidth() > 60,
                    "Cửa sổ thật 1280×760 giữ nguyên hàng điều khiển và vùng cuộn");
            shell.navigate("TAI_KHOAN");
            check(java.util.Arrays.stream(Window.getWindows()).filter(Window::isShowing).count() == 1, "Điều hướng không tạo cửa sổ mới"); return null; });
        } finally { edt(() -> { frame.dispose(); return null; }); }
        edt(() -> { check(!frame.isDisplayable(), "Đóng JFrame giải phóng bản xem thử"); return null; });
    }
    private static void choose(AdminShell shell, Scenario scenario) throws Exception {
        edt(() -> { ((JComboBox<?>) find(shell,"demo-scenario")).setSelectedItem(scenario); return null; });
    }
    private static void awaitStatus(AdminShell shell, Status status) throws Exception {
        await(() -> edtUnchecked(() -> shell.dashboard().status() == status));
    }
    private static void await(BooleanSupplier condition) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(8);
        while (!condition.getAsBoolean()) { if (System.nanoTime() > deadline) throw new AssertionError("Quá thời gian chờ dữ liệu"); Thread.sleep(10); }
    }
    private static <T> T edt(Callable<T> task) throws Exception {
        if (SwingUtilities.isEventDispatchThread()) return task.call();
        FutureTask<T> future = new FutureTask<>(task); SwingUtilities.invokeAndWait(future); return future.get();
    }
    private static boolean edtUnchecked(Callable<Boolean> task) {
        try { return edt(task); } catch (Exception e) { throw new AssertionError(e); }
    }
    private static List<Component> all(Component root) {
        List<Component> result = new ArrayList<>(); result.add(root);
        if (root instanceof Container c) for (Component child : c.getComponents()) result.addAll(all(child));
        return result;
    }
    private static Component findOptional(Component root, String name) {
        return all(root).stream().filter(c -> name.equals(c.getName())).findFirst().orElse(null);
    }
    private static Component find(Component root, String name) {
        Component found = findOptional(root,name); if (found == null) throw new AssertionError("Thiếu component " + name); return found;
    }
    private static List<String> texts(Component root) {
        return all(root).stream().filter(c -> c instanceof JLabel || c instanceof AbstractButton)
                .map(c -> c instanceof JLabel l ? l.getText() : ((AbstractButton)c).getText()).toList();
    }
    private static void click(Component root, String name) { ((AbstractButton)find(root,name)).doClick(0); }
    private static void key(JComponent component, String stroke) {
        Object binding = component.getInputMap().get(KeyStroke.getKeyStroke(stroke));
        if (binding == null) throw new AssertionError("Thiếu phím " + stroke);
        Action action = component.getActionMap().get(binding);
        if (action == null) throw new AssertionError("Thiếu xử lý phím " + stroke);
        action.actionPerformed(new ActionEvent(component,0,stroke));
    }
    private static List<String> entryKeys(JTree tree) {
        List<String> keys = new ArrayList<>();
        var nodes = ((DefaultMutableTreeNode)tree.getModel().getRoot()).depthFirstEnumeration();
        while (nodes.hasMoreElements()) {
            var node = (DefaultMutableTreeNode) nodes.nextElement();
            if (node.getUserObject() instanceof MenuDefinition.Entry e) keys.add(e.key());
        }
        return keys;
    }
    private static void layout(Component component) {
        if (component instanceof Container c) { c.doLayout(); for (Component child : c.getComponents()) layout(child); }
    }
    private static void snapshot(JComponent component, int width, int height, String name) throws Exception {
        component.setSize(width,height);
        for (int pass = 0; pass < 4; pass++) {
            for (Component child : all(component)) child.invalidate();
            layout(component);
        }
        BufferedImage output = new BufferedImage(width,height,BufferedImage.TYPE_INT_RGB);
        Graphics2D g = output.createGraphics(); component.printAll(g); g.dispose();
        Path dir = Path.of("build/screenshots"); Files.createDirectories(dir); ImageIO.write(output,"png",dir.resolve(name).toFile());
    }
}
