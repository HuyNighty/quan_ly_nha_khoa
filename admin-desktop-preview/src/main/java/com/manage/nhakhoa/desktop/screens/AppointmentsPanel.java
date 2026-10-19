package com.manage.nhakhoa.desktop.screens;

import com.manage.nhakhoa.desktop.MenuDefinition;
import com.manage.nhakhoa.desktop.demo.DemoClinicStore;
import com.manage.nhakhoa.desktop.demo.DemoClinicStore.*;
import com.manage.nhakhoa.desktop.model.DashboardViewModel.AppointmentStatus;
import com.manage.nhakhoa.desktop.ui.DemoTable;
import com.manage.nhakhoa.desktop.ui.DemoTable.Column;
import com.manage.nhakhoa.desktop.ui.DemoTable.Strong;
import com.manage.nhakhoa.desktop.ui.Flat;
import com.manage.nhakhoa.desktop.ui.Theme;
import java.awt.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.swing.*;
import static com.manage.nhakhoa.desktop.ui.DemoWidgets.*;
import static com.manage.nhakhoa.desktop.ui.UiComponents.*;

/**
 * #03 Lịch hẹn (LICH_HEN) — master/detail theo ngày: thẻ thống kê bấm để lọc nhanh, bảng lịch, panel chi tiết
 * với thao tác xác nhận / đổi lịch (tạo bản mới) / hủy có lý do; form tạo/đổi lịch thay chỗ panel chi tiết.
 */
public final class AppointmentsPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private static final String ALL_DOCTORS = "Tất cả bác sĩ", ALL_STATUS = "Tất cả trạng thái";
    private final transient ScreenContext ctx;
    private final transient DemoClinicStore store;
    private transient LocalDate viewDate;
    private final JLabel dateLabel = label("", 15, true, Theme.TEXT);
    private final JTextField search = search("appt-search", "Tìm mã lịch, mã hoặc tên bệnh nhân…");
    private final JComboBox<String> doctorFilter = combo("appt-doctor-filter", List.of(ALL_DOCTORS));
    private final JComboBox<String> statusFilter = combo("appt-status-filter", List.of(ALL_STATUS));
    private final JPanel stats = new JPanel(new GridLayout(1, 9, 8, 0));
    private final DemoTable<Appointment> table;
    private final JLabel tableCaption = label(" ", 12, false, Theme.MUTED);
    // Panel phải: chi tiết hoặc form
    private final FitCards side = new FitCards();
    private final JLabel detailId = value("appt-detail-id");
    private final Flat.Pill detailStatus = new Flat.Pill(" ", Theme.MUTED);
    private final JLabel dPatient = value("appt-d-patient"), dDoctor = value("appt-d-doctor"), dTime = value("appt-d-time"),
            dReason = value("appt-d-reason"), dTicket = value("appt-d-ticket"), dCancel = value("appt-d-cancel");
    private final JLabel detailLink = label(" ", 12, true, Theme.PRIMARY_TEXT);
    private final JPanel detailBody;
    private final JLabel detailEmpty = label("Chọn một lịch hẹn trong bảng để xem chi tiết và thao tác", 13, false, Theme.MUTED);
    private final JButton confirm, cancel, reschedule, openLinked;
    private final JLabel feedback = feedback("appt-feedback");
    private final JLabel formTitle = label("Tạo lịch hẹn", 16, true, Theme.TEXT);
    private final JComboBox<String> formPatient = combo("appt-form-patient", List.of());
    private final JComboBox<String> formDoctor = combo("appt-form-doctor", List.of());
    private final JTextField formDate = field("appt-form-date", 10, "dd/MM/yyyy");
    private final JTextField formTime = field("appt-form-time", 6, "HH:mm");
    private final JTextField formReason = field("appt-form-reason", 20, "Ví dụ: Khám định kỳ");
    private final JLabel formFeedback = feedback("appt-form-feedback");
    private transient String rescheduleId;

    public AppointmentsPanel(ScreenContext ctx) {
        super(new BorderLayout()); setOpaque(false); setName("screen-LICH_HEN");
        this.ctx = ctx; this.store = ctx.store(); this.viewDate = store.today();
        for (AppointmentStatus s : AppointmentStatus.values()) statusFilter.addItem(s.label());
        store.doctors().forEach(d -> doctorFilter.addItem(d.display()));
        table = new DemoTable<>("appt-table", List.of(
                new Column<Appointment>("Giờ", a -> new Strong(TIME.format(a.time())), 62),
                new Column<Appointment>("Mã LH", Appointment::id, 100),
                new Column<Appointment>("Bệnh nhân", a -> store.patientName(a.patientId()), 150),
                new Column<Appointment>("Bác sĩ", a -> store.doctorName(a.doctorId()), 150),
                new Column<Appointment>("Trạng thái", a -> status(a.status()), 130)), Appointment::id, 10);
        table.onSelect(a -> showDetail());
        confirm = icon(primary("Xác nhận lịch", this::doConfirm), "check", Color.WHITE); confirm.setName("appt-confirm");
        reschedule = icon(secondary("Đổi lịch", this::openReschedule), "calendar", Theme.PRIMARY_TEXT); reschedule.setName("appt-reschedule");
        cancel = icon(danger("Hủy lịch", this::doCancel), "close", Color.WHITE); cancel.setName("appt-cancel");
        openLinked = link("Mở lịch liên kết", this::openLinked); openLinked.setName("appt-open-linked");
        JButton prev = icon(secondary("", () -> setDate(viewDate.minusDays(1))), "left", Theme.TEXT); prev.setName("appt-prev"); prev.setToolTipText("Ngày trước");
        JButton next = icon(secondary("", () -> setDate(viewDate.plusDays(1))), "right", Theme.TEXT); next.setName("appt-next"); next.setToolTipText("Ngày sau");
        JButton today = secondary("Hôm nay", () -> setDate(store.today())); today.setName("appt-today");
        JButton create = icon(primary("Tạo lịch hẹn", this::openCreate), "plus", Color.WHITE); create.setName("appt-new");
        JButton clear = link("Xóa bộ lọc", this::clearFilters); clear.setName("appt-clear-filters");
        search.getDocument().addDocumentListener(SimpleDocumentListener.of(this::refreshAll));
        doctorFilter.addActionListener(e -> refreshAll()); statusFilter.addActionListener(e -> refreshAll());

        detailId.setFont(Theme.font(18, true));
        JPanel head = new JPanel(new BorderLayout(8, 0)); head.setOpaque(false);
        head.add(detailId, BorderLayout.CENTER); head.add(buttons(detailStatus), BorderLayout.EAST);
        detailBody = column(12, head,
                Flat.infoGrid("Bệnh nhân", dPatient, "Bác sĩ", dDoctor, "Thời gian", dTime, "Lý do hẹn", dReason,
                        "Phiếu khám", dTicket, "Lý do hủy", dCancel),
                column(2, detailLink, openLinked), separator(), buttons(confirm, reschedule, cancel), feedback);
        JPanel detailCard = new UiCard("Chi tiết lịch hẹn", null, column(10, detailEmpty, detailBody));
        JButton save = primary("Lưu lịch hẹn", this::save); save.setName("appt-form-save");
        JButton close = secondary("Đóng", this::closeForm);
        JPanel formCard = new UiCard("Biểu mẫu lịch hẹn", null, column(10, formTitle,
                form("Bệnh nhân *", formPatient, "Bác sĩ *", formDoctor),
                pair(form("Ngày *", formDate), form("Giờ *", formTime)),
                form("Lý do hẹn *", formReason),
                label("<html>Giờ hẹn phải sau hiện tại, trong 07:00–20:00, bước 15 phút, không trùng khung giờ của bác sĩ hoặc bệnh nhân.</html>", 11, false, Theme.MUTED),
                buttons(save, close), formFeedback));
        side.addCard(detailCard, "detail"); side.addCard(formCard, "form");

        stats.setOpaque(false);
        JPanel dateNav = buttons(prev, dateLabel, next, today);
        JPanel toolbar = new JPanel(new BorderLayout()); toolbar.setOpaque(false);
        toolbar.add(dateNav, BorderLayout.WEST); toolbar.add(buttonsRight(create), BorderLayout.EAST);
        JPanel filters = new JPanel(new GridBagLayout()); filters.setOpaque(false);
        // Kích thước cơ sở nhỏ để GridBag không rơi về minimum khi cửa sổ hẹp; phần dư chia theo trọng số.
        for (var pair : List.of(new Object[]{search, 150}, new Object[]{doctorFilter, 140}, new Object[]{statusFilter, 110})) {
            JComponent comp = (JComponent) pair[0]; int w = (Integer) pair[1];
            comp.setPreferredSize(new Dimension(w, Flat.FIELD_HEIGHT)); comp.setMinimumSize(new Dimension(w, Flat.FIELD_HEIGHT));
        }
        GridBagConstraints c = new GridBagConstraints(); c.fill = GridBagConstraints.HORIZONTAL; c.insets = new Insets(0, 0, 0, 8);
        c.weightx = 0.5; filters.add(search, c); c.weightx = 0.3; filters.add(doctorFilter, c); c.weightx = 0.2; filters.add(statusFilter, c);
        c.weightx = 0; filters.add(clear, c);
        JComponent list = new UiCard("Danh sách lịch hẹn", tableCaption, column(10, filters, table));
        add(screen(new ScreenHeader("Lịch hẹn", "Xác nhận, đổi lịch (tạo bản mới, giữ bản cũ) và hủy có lý do — 7 trạng thái",
                        buttons(ctx.resetButton(this, "appt-reset"))),
                banner("appt-banner", "Tổng số gồm cả Đã hủy và Đã đổi lịch, không phải số khách được phục vụ."),
                toolbar, stats, twoColumns(list, side, 0.66)), BorderLayout.NORTH);
        store.addListener(this::refresh);
        refresh();
    }
    private static JComponent separator() {
        JSeparator s = new JSeparator(); s.setForeground(Theme.BORDER); s.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1)); return s;
    }
    public LocalDate viewDate() { return viewDate; }
    public void setDate(LocalDate date) { Theme.requireEdt(); viewDate = date; refresh(); }
    /** Chuyển tới ngày của lịch và chọn dòng; trả về false nếu không thấy. */
    public boolean focusAppointment(String id) {
        var a = store.appointment(id); if (a.isEmpty()) return false;
        viewDate = a.get().time().toLocalDate(); search.setText(""); statusFilter.setSelectedIndex(0); doctorFilter.setSelectedIndex(0);
        refresh(); return table.select(id);
    }
    private void clearFilters() { search.setText(""); doctorFilter.setSelectedIndex(0); statusFilter.setSelectedIndex(0); }
    private void refresh() {
        dateLabel.setText("  " + Theme.day(viewDate.atStartOfDay(Theme.ZONE).toInstant())
                + (viewDate.equals(store.today()) ? "  ·  Hôm nay" : "") + "  ");
        refreshAll();
        replaceItems(formPatient, store.patients().stream().map(p -> p.id() + " · " + p.name()).toList());
        replaceItems(formDoctor, store.doctors().stream().map(Doctor::display).toList());
    }
    private void refreshAll() { refreshStats(); refreshTable(); }
    private void refreshStats() {
        stats.removeAll();
        Map<AppointmentStatus, Long> counts = store.statusCounts(viewDate);
        long total = counts.values().stream().mapToLong(Long::longValue).sum();
        String st = (String) statusFilter.getSelectedItem();
        stats.add(stat("appt-total", "Tổng lịch", total, Theme.TEXT, () -> statusFilter.setSelectedIndex(0), ALL_STATUS.equals(st)));
        stats.add(stat("appt-active", "Còn hiệu lực", store.activeCount(viewDate), Theme.PRIMARY_TEXT, null, false));
        for (AppointmentStatus s : AppointmentStatus.values()) {
            boolean active = s.label().equals(st);
            stats.add(stat("appt-stat-" + s.name(), s.label(), counts.get(s), tone(s),
                    () -> statusFilter.setSelectedItem(active ? ALL_STATUS : s.label()), active));
        }
        stats.revalidate(); stats.repaint();
    }
    private void refreshTable() {
        String q = MenuDefinition.normalize(search.getText());
        String doc = (String) doctorFilter.getSelectedItem(), st = (String) statusFilter.getSelectedItem();
        List<Appointment> all = store.appointmentsOn(viewDate), rows = new ArrayList<>();
        for (Appointment a : all) {
            if (doc != null && !ALL_DOCTORS.equals(doc) && !doc.startsWith(a.doctorId() + " ")) continue;
            if (st != null && !ALL_STATUS.equals(st) && !a.status().label().equals(st)) continue;
            if (!q.isEmpty() && !MenuDefinition.normalize(a.id() + " " + a.patientId() + " " + store.patientName(a.patientId())).contains(q)) continue;
            rows.add(a);
        }
        boolean filtered = !q.isEmpty() || !ALL_DOCTORS.equals(doc) || !ALL_STATUS.equals(st);
        table.setRows(rows, filtered ? "Không có lịch khớp bộ lọc · bấm “Xóa bộ lọc”" : "Không có lịch hẹn trong ngày này");
        tableCaption.setText(Theme.number(rows.size()) + " / " + Theme.number(all.size()) + " lịch");
        showDetail();
    }
    private void showDetail() {
        Appointment a = table.selected().orElse(null);
        detailEmpty.setVisible(a == null); detailBody.setVisible(a != null);
        if (a == null) {
            detailId.setText("Chưa chọn lịch hẹn");
            for (JButton b : new JButton[]{confirm, cancel, reschedule}) { b.setEnabled(false); b.setToolTipText("Chọn lịch hẹn trước"); }
            openLinked.setVisible(false); revalidate(); repaint(); return;
        }
        detailId.setText(a.id()); detailStatus.set(a.status().label(), tone(a.status()));
        dPatient.setText(store.patientName(a.patientId()) + " · " + a.patientId());
        dDoctor.setText(store.doctorName(a.doctorId()));
        dTime.setText(DATE_TIME.format(a.time())); dReason.setText(a.reason());
        dTicket.setText(store.ticketForAppointment(a.id()).map(t -> t.id() + " · " + t.status().label()).orElse("Chưa có"));
        dCancel.setText(a.cancelReason() == null ? "—" : a.cancelReason());
        String linked = a.replacementId() != null ? a.replacementId() : a.originId();
        detailLink.setText(a.replacementId() != null ? "Đã đổi sang lịch mới " + a.replacementId()
                : a.originId() != null ? "Lịch mới đổi từ lịch gốc " + a.originId() : " ");
        openLinked.setVisible(linked != null);
        boolean canConfirm = DemoClinicStore.canConfirm(a), canChange = DemoClinicStore.canChange(a);
        confirm.setEnabled(canConfirm); cancel.setEnabled(canChange); reschedule.setEnabled(canChange);
        String why = "Không áp dụng cho trạng thái " + a.status().label();
        confirm.setToolTipText(canConfirm ? "Chờ xác nhận → Đã xác nhận" : "Chỉ xác nhận lịch Chờ xác nhận");
        cancel.setToolTipText(canChange ? "Hủy có lý do" : why);
        reschedule.setToolTipText(canChange ? "Tạo bản mới, bản cũ thành Đã đổi lịch" : why);
        revalidate(); repaint();
    }
    private void doConfirm() {
        table.selected().ifPresent(a -> {
            if (ctx.dialogs().confirm(this, "Xác nhận lịch hẹn", "Xác nhận " + a.id() + " (" + store.patientName(a.patientId()) + ")?"))
                result(feedback, store.confirm(a.id()));
            else info(feedback, "Đã hủy thao tác");
        });
    }
    private void doCancel() {
        table.selected().ifPresent(a -> {
            String r = ctx.dialogs().reason(this, "Hủy lịch hẹn " + a.id(), "Nhập lý do hủy (5–200 ký tự):");
            if (r == null) { info(feedback, "Đã hủy thao tác"); return; }
            result(feedback, store.cancel(a.id(), r));
        });
    }
    private void showForm() { side.show("form"); revalidate(); repaint(); }
    private void closeForm() { side.show("detail"); revalidate(); repaint(); }
    private void openCreate() {
        rescheduleId = null; formTitle.setText("Tạo lịch hẹn mới"); formPatient.setEnabled(true);
        LocalDateTime next = store.now().plusHours(1).withMinute(0);
        formDate.setText(DATE.format(next.toLocalDate())); formTime.setText(TIME.format(next.toLocalTime()));
        formReason.setText(""); formReason.setEnabled(true); info(formFeedback, "Lịch mới ở trạng thái Chờ xác nhận");
        showForm();
    }
    private void openReschedule() {
        Appointment a = table.selected().orElse(null); if (a == null) return;
        rescheduleId = a.id(); formTitle.setText("Đổi lịch " + a.id() + " → tạo lịch mới");
        formPatient.setSelectedItem(a.patientId() + " · " + store.patientName(a.patientId())); formPatient.setEnabled(false);
        formDoctor.setSelectedItem(store.doctor(a.doctorId()).map(Doctor::display).orElse(null));
        formDate.setText(DATE.format(a.time().toLocalDate().plusDays(1))); formTime.setText(TIME.format(a.time().toLocalTime()));
        formReason.setText(a.reason()); formReason.setEnabled(false);
        info(formFeedback, "Lịch cũ sẽ chuyển Đã đổi lịch, lịch mới ở trạng thái Chờ xác nhận");
        showForm();
    }
    private void save() {
        LocalDate d = parseDate(formDate.getText()); LocalTime t = parseTime(formTime.getText());
        LocalDateTime when = d == null || t == null ? null : d.atTime(t);
        String patient = idOf(formPatient.getSelectedItem()), doctor = idOf(formDoctor.getSelectedItem());
        Result<Appointment> r;
        if (rescheduleId == null) {
            r = store.createAppointment(patient, doctor, when, formReason.getText());
        } else {
            if (when != null && !ctx.dialogs().confirm(this, "Đổi lịch", "Đổi " + rescheduleId + " sang " + DATE_TIME.format(when)
                    + "? Lịch cũ chuyển Đã đổi lịch và tạo lịch mới chờ xác nhận.")) { info(formFeedback, "Đã hủy thao tác"); return; }
            r = store.reschedule(rescheduleId, doctor, when);
        }
        result(formFeedback, r);
        if (r.ok()) { closeForm(); focusAppointment(r.value().id()); result(feedback, r); }
    }
    private void openLinked() {
        table.selected().ifPresent(a -> { String id = a.replacementId() != null ? a.replacementId() : a.originId(); if (id != null) focusAppointment(id); });
    }
    private static String idOf(Object item) { if (item == null) return null; String s = item.toString(); int i = s.indexOf(' '); return i < 0 ? s : s.substring(0, i); }
}
