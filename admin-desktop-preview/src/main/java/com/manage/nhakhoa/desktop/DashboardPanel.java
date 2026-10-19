package com.manage.nhakhoa.desktop;

import com.manage.nhakhoa.desktop.data.DemoDashboardDataProvider;
import com.manage.nhakhoa.desktop.data.DashboardDataProvider;
import com.manage.nhakhoa.desktop.data.DemoDashboardDataProvider.Scenario;
import com.manage.nhakhoa.desktop.model.DashboardViewModel;
import com.manage.nhakhoa.desktop.model.DashboardViewModel.*;
import com.manage.nhakhoa.desktop.ui.Theme;
import com.manage.nhakhoa.desktop.ui.UiIcons;
import java.awt.*;
import java.time.Clock;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import static com.manage.nhakhoa.desktop.ui.UiComponents.*;

/** Ghép các component; nguồn dữ liệu và điều hướng được truyền vào từ khung chính. */
public final class DashboardPanel extends JPanel implements AutoCloseable {
    private static final long serialVersionUID = 1L;
    private final transient Clock clock;
    private final boolean fixture;
    private final transient Consumer<String> navigate;
    private final transient DashboardLoadController loader;
    private final transient Function<Scenario, DashboardDataProvider> providerFactory;
    private final JPanel stage = new JPanel(new BorderLayout());
    private final JLabel greeting = label("", 24, true, Theme.TEXT);
    private final JLabel timestamp = label("", 12, false, Theme.MUTED);
    private final JLabel provenance = label("", 11, false, Theme.MUTED);
    private final JComboBox<Scenario> scenario = new JComboBox<>(Scenario.values());
    private DashboardLoadController.Status status = DashboardLoadController.Status.LOADING;

    public DashboardPanel(Clock clock, boolean fixture, Consumer<String> navigate) {
        this(clock, fixture, navigate, DemoDashboardDataProvider::new);
    }
    public DashboardPanel(Clock clock, boolean fixture, Consumer<String> navigate,
            Function<Scenario, DashboardDataProvider> providerFactory) {
        super(new BorderLayout(0, 16)); this.clock = clock; this.fixture = fixture; this.navigate = navigate;
        this.providerFactory = providerFactory;
        setBackground(Theme.BACKGROUND); setBorder(new EmptyBorder(24, 24, 24, 24));
        greeting.setName("greeting"); timestamp.setName("data-timestamp"); provenance.setName("data-provenance");
        JButton today = secondary("Hôm nay", this::reload); today.setName("today");
        today.setIcon(new UiIcons("calendar", Theme.TEXT, 15));
        today.setToolTipText(fixture ? "Tải lại ngày mô phỏng 12/10/2026 · ảnh mẫu cố định" : "Tải lại dữ liệu mẫu cho ngày hiện tại tại phòng khám");
        JButton refresh = secondary("Tải lại", this::reload); refresh.setName("reload");
        refresh.setIcon(new UiIcons("reload", Theme.TEXT, 15)); refresh.setToolTipText("Tải lại dữ liệu mẫu");
        JPanel buttons = row(8); buttons.add(today); buttons.add(refresh);
        scenario.setName("demo-scenario"); scenario.setToolTipText("Tình huống nguồn dữ liệu mô phỏng · không gọi máy chủ");
        scenario.getAccessibleContext().setAccessibleName("Tình huống dữ liệu mô phỏng");
        JPanel scenarioRow = row(6); scenarioRow.add(label("Tình huống", 11, false, Theme.MUTED)); scenarioRow.add(scenario);
        JPanel header = new JPanel(new BorderLayout(12, 0)); header.setOpaque(false);
        header.add(column(3, greeting, timestamp, provenance), BorderLayout.CENTER);
        header.add(column(5, buttons, scenarioRow), BorderLayout.EAST);
        add(header, BorderLayout.NORTH); stage.setOpaque(false); add(stage, BorderLayout.CENTER);
        loader = new DashboardLoadController(clock, this::showState);
        scenario.addActionListener(e -> reload()); reload();
    }
    public void reload() {
        Theme.requireEdt();
        loader.reload(providerFactory.apply((Scenario) scenario.getSelectedItem()));
    }
    public DashboardLoadController.Status status() { return status; }
    private void showState(DashboardLoadController.LoadState state) {
        Theme.requireEdt(); status = state.status(); stage.removeAll();
        var time = state.data().map(DashboardViewModel::loadedAt).orElseGet(clock::instant);
        greeting.setText(Theme.greeting(time)); greeting.setToolTipText(greeting.getText());
        timestamp.setText(Theme.day(time) + (status == DashboardLoadController.Status.LOADED
                ? " · dữ liệu tải lúc " + DateTimeFormatter.ofPattern("HH:mm:ss").withZone(Theme.ZONE).format(time)
                : " · chưa có dữ liệu được tải"));
        timestamp.setToolTipText(timestamp.getText());
        provenance.setText(fixture ? "Ảnh mẫu cố định · 12/10/2026 07:45 · Asia/Ho_Chi_Minh"
                : "Ngày hiện tại · số liệu mẫu theo ảnh 01 · Asia/Ho_Chi_Minh");
        provenance.setToolTipText(state.data().map(DashboardViewModel::source)
                .orElse("Nguồn: bộ mẫu ảnh 01 · chưa kết nối SQL Server"));
        switch (status) {
            case LOADED -> stage.add(render(state.data().orElseThrow()), BorderLayout.NORTH);
            case LOADING -> {
                JProgressBar progress = new JProgressBar(); progress.setIndeterminate(true);
                progress.setPreferredSize(new Dimension(280, 8));
                stage.add(stateCard("Đang tải dữ liệu…", "Đang đọc nguồn dữ liệu mẫu", progress), BorderLayout.NORTH);
            }
            case EMPTY -> stage.add(stateCard("Không có dữ liệu", "Nguồn mô phỏng không trả về dữ liệu cho ngày đang xem",
                    primary("Thử lại", this::reload)), BorderLayout.NORTH);
            case ERROR -> stage.add(stateCard("Không thể tải dữ liệu", "Nguồn dữ liệu mẫu gặp lỗi. Số liệu cũ đã được bỏ khỏi màn hình.",
                    primary("Thử lại", this::reload)), BorderLayout.NORTH);
        }
        revalidate(); repaint();
    }
    private static JComponent stateCard(String title, String description, JComponent action) {
        JPanel body = column(18, label(description, 13, false, Theme.MUTED), action);
        body.setBorder(new EmptyBorder(20, 0, 20, 0)); return new UiCard(title, null, body);
    }
    private JPanel render(DashboardViewModel data) {
        KpiSummary k = data.kpis();
        JPanel kpis = new JPanel(new GridLayout(1, 4, 14, 0)); kpis.setOpaque(false);
        kpis.add(new KpiCard("kpi-appointments", "Lịch hẹn hôm nay", Theme.number(k.appointmentCount()),
                data.appointments().get(AppointmentStatus.CHO_XAC_NHAN) + " chờ xác nhận", "calendar", Theme.PRIMARY_TEXT, () -> navigate.accept("LICH_HEN")));
        kpis.add(new KpiCard("kpi-waiting", "Phiếu chờ khám", Theme.number(k.waiting()),
                k.examining() + " đang khám · " + k.concluded() + " đã kết luận", "tooth", Theme.PURPLE, () -> navigate.accept("PHIEU_KHAM")));
        kpis.add(new KpiCard("kpi-received", "Đã thu hôm nay", Theme.money(k.received()),
                "Tiền mặt " + Theme.money(k.cash()) + " · chuyển khoản " + Theme.money(k.transfer()) + " · thẻ " + Theme.money(k.card()),
                "wallet", Theme.GREEN, () -> navigate.accept("HOA_DON")));
        kpis.add(new KpiCard("kpi-debt", "Công nợ", Theme.money(k.debt()), k.unpaidInvoices() + " hóa đơn còn nợ",
                "document", Theme.RED, () -> navigate.accept("HOA_DON")));
        JPanel lower = new JPanel(new GridBagLayout()); lower.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints(); c.gridy = 0; c.gridx = 0; c.weightx = .56;
        c.fill = GridBagConstraints.BOTH; c.anchor = GridBagConstraints.NORTHWEST; c.insets = new Insets(0, 0, 0, 16);
        lower.add(taskCard(data), c);
        c.gridx = 1; c.weightx = .44; c.insets = new Insets(0, 0, 0, 0);
        JPanel side = column(16, appointmentCard(data), shiftCard(data), stockCard(data)); lower.add(side, c);
        return column(16, kpis, lower);
    }
    private UiCard taskCard(DashboardViewModel data) {
        List<TaskSummary> visible = data.tasks().stream().filter(t -> t.affectedCount() > 0).toList();
        JPanel body = new JPanel(); body.setOpaque(false); body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        if (visible.isEmpty()) body.add(label("Không có việc đang chờ xử lý", 13, false, Theme.MUTED));
        for (Priority priority : Priority.values()) {
            List<TaskSummary> group = visible.stream().filter(t -> t.priority() == priority).toList();
            if (group.isEmpty()) continue;
            PriorityGroupHeader header = new PriorityGroupHeader(priority); header.setAlignmentX(LEFT_ALIGNMENT); body.add(header);
            for (TaskSummary task : group) {
                TaskRow row = new TaskRow(task, navigate); row.setAlignmentX(LEFT_ALIGNMENT); body.add(row);
            }
        }
        StatusBadge count = new StatusBadge(visible.size() + " nhóm việc", Theme.RED, Theme.RED_LIGHT);
        count.setToolTipText(visible.size() + " nhóm việc · " + Theme.number(data.affectedTotal()) + " đối tượng liên quan; không phải số hồ sơ duy nhất");
        UiCard card = new UiCard("Việc cần xử lý", count, body); card.setName("tasks-card"); return card;
    }
    private UiCard appointmentCard(DashboardViewModel data) {
        JPanel legend = new JPanel(new GridLayout(2, 4, 8, 8)); legend.setOpaque(false);
        for (AppointmentStatus status : AppointmentStatus.values()) {
            JLabel name = label(status.label(), 10, false, Theme.MUTED);
            name.setIcon(new ColorSquare(Theme.status(status)));
            legend.add(column(2, name, label(Theme.number(data.appointments().get(status)), 14, true, Theme.TEXT)));
        }
        JComponent body = column(10, new StackedStatusBar(data.appointments()), legend,
                label("Tổng: " + Theme.number(data.kpis().appointmentCount()) + " lịch hẹn", 11, false, Theme.MUTED));
        JButton open = link("Mở lịch hẹn ›", () -> navigate.accept("LICH_HEN")); open.setName("open-appointments");
        return new UiCard("Lịch hẹn hôm nay", open, body);
    }
    private UiCard shiftCard(DashboardViewModel data) {
        JPanel shifts = new JPanel(new GridLayout(1, Math.max(1, data.shifts().size()), 8, 0)); shifts.setOpaque(false);
        for (ShiftSummary shift : data.shifts()) shifts.add(new ShiftTile(shift));
        if (data.shifts().isEmpty()) shifts.add(label("Chưa có ca làm", 12, false, Theme.MUTED));
        return new UiCard("Ca làm hôm nay", link("Phân công ›", () -> navigate.accept("PHAN_CONG")), shifts);
    }
    private UiCard stockCard(DashboardViewModel data) {
        StockSummary stock = data.stock();
        JPanel stats = new JPanel(new GridLayout(1, 3, 8, 0)); stats.setOpaque(false);
        stats.add(new StatTile("Dưới định mức", stock.belowMinimum(), Theme.ORANGE));
        stats.add(new StatTile("Lô sắp hết hạn", stock.expiringLots(), Theme.ORANGE));
        stats.add(new StatTile("Lô đã hết hạn", stock.expiredLots(), Theme.RED));
        String text = "Hộp cấp cứu: " + (stock.emergencyNeedsSupply() ? "cần bổ sung" : "đạt") + " · "
                + DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(Theme.ZONE).format(stock.emergencyCheckedAt());
        return new UiCard("Thuốc & vật tư", link("Tồn kho ›", () -> navigate.accept("THUOC_VAT_TU")),
                column(10, stats, new AlertBar(text, stock.emergencyNeedsSupply()),
                        link("Xem hộp cấp cứu ›", () -> navigate.accept("CAP_CUU"))));
    }
    @Override public void close() { loader.close(); }

    private record ColorSquare(Color color) implements Icon {
        @Override public int getIconWidth() { return 7; }
        @Override public int getIconHeight() { return 7; }
        @Override public void paintIcon(Component c, Graphics g, int x, int y) { g.setColor(color); g.fillRect(x, y, 7, 7); }
    }
}
