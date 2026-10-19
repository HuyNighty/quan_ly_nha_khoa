package com.manage.nhakhoa.desktop.screens;

import com.manage.nhakhoa.desktop.demo.DemoClinicStore;
import com.manage.nhakhoa.desktop.demo.DemoClinicStore.*;
import com.manage.nhakhoa.desktop.model.DashboardViewModel.AppointmentStatus;
import com.manage.nhakhoa.desktop.ui.CardList;
import com.manage.nhakhoa.desktop.ui.DemoTable.Tag;
import com.manage.nhakhoa.desktop.ui.Flat;
import com.manage.nhakhoa.desktop.ui.Theme;
import java.awt.*;
import java.util.List;
import java.util.Optional;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import static com.manage.nhakhoa.desktop.ui.DemoWidgets.*;
import static com.manage.nhakhoa.desktop.ui.UiComponents.*;

/**
 * #02 Tiếp nhận bệnh nhân (TIEP_NHAN). Trái: thanh tìm kiếm lớn + danh sách thẻ bệnh nhân có avatar (hoặc form thêm mới).
 * Phải: hồ sơ tóm tắt avatar lớn → tiếp nhận theo lịch đã xác nhận / trực tiếp → hoạt động gần đây.
 * Mọi thao tác chỉ đổi DemoClinicStore trong bộ nhớ.
 */
public final class ReceptionPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private final transient ScreenContext ctx;
    private final transient DemoClinicStore store;
    // Cột trái
    private final Flat.SearchBar searchBar = new Flat.SearchBar("reception-search", "Tìm mã BN, họ tên hoặc SĐT…");
    private final JTextField search = searchBar.field();
    private final Flat.Segmented filter = new Flat.Segmented("reception-filter", List.of("Tất cả", "Lịch hôm nay", "Nghi trùng"), null);
    private final CardList<Patient> results;
    private final JLabel state = feedback("reception-state");
    private final FitCards leftCards = new FitCards();
    private final JLabel leftTitle = label("Tìm bệnh nhân", 15, true, Theme.TEXT);
    private final JButton addToggle;
    private final JTextField newName = field("new-name", 18, "Ví dụ: Nguyễn Văn B");
    private final JTextField newDob = field("new-dob", 10, "dd/MM/yyyy");
    private final JTextField newPhone = field("new-phone", 12, "10 số, bắt đầu bằng 0");
    private final JComboBox<Gender> newGender = combo("new-gender", List.of(Gender.values()));
    private final JTextField newAddress = field("new-address", 18, "Địa chỉ (giả lập)");
    private final JLabel addFeedback = feedback("new-feedback");
    // Hồ sơ tóm tắt
    private final Flat.Avatar avatar = new Flat.Avatar(68);
    private final JLabel summaryName = label("Chưa chọn bệnh nhân", 20, true, Theme.TEXT);
    private final JLabel summaryInfo = label("Tìm và chọn một bệnh nhân ở danh sách bên trái", 12, false, Theme.MUTED);
    private final Flat.Pill chipId = new Flat.Pill(" ", Theme.PRIMARY_TEXT), chipAge = new Flat.Pill(" ", Theme.color("475569")),
            chipPhone = new Flat.Pill(" ", Theme.color("475569")), chipToday = new Flat.Pill(" ", Theme.PRIMARY_TEXT);
    private final JPanel chips = buttons(chipId, chipAge, chipPhone, chipToday);
    private final JLabel duplicateWarning = feedback("reception-duplicate-warning");
    // Tiếp nhận
    private final Flat.Segmented mode = new Flat.Segmented("reception-mode", List.of("Theo lịch hẹn", "Trực tiếp (không hẹn)"),
            List.of("reception-mode-schedule", "reception-mode-walkin"));
    private final FitCards modeCards = new FitCards();
    private final CardList<Appointment> todays;
    private final JComboBox<String> doctor = combo("reception-doctor", List.of());
    private final JTextField visitReason = field("reception-visit-reason", 20, "Ví dụ: Ê buốt răng, đau khi nhai");
    private final JButton checkIn, createWalkIn, openSchedule;
    private final JLabel reason = feedback("reception-reason");
    private final JLabel result = feedback("reception-feedback");
    private final JPanel timeline = timelineBox("reception-timeline");
    private transient String patientId;

    public ReceptionPanel(ScreenContext ctx) {
        super(new BorderLayout()); setOpaque(false); setName("screen-TIEP_NHAN");
        this.ctx = ctx; this.store = ctx.store();
        summaryName.setName("reception-summary-name"); chipId.setName("reception-summary-id");
        results = Rows.patients("reception-results", store, this::badge, 7);
        todays = Rows.appointments("reception-appointments", store, 3);
        checkIn = big(icon(primary("Check-in & tạo phiếu khám", this::doCheckIn), "check", Color.WHITE)); checkIn.setName("reception-checkin");
        createWalkIn = big(icon(primary("Tạo phiếu tiếp nhận trực tiếp", this::doWalkIn), "plus", Color.WHITE)); createWalkIn.setName("reception-walkin");
        openSchedule = big(icon(secondary("Mở Lịch hẹn để xác nhận", () -> ctx.navigate().accept("LICH_HEN")), "calendar", Theme.PRIMARY_TEXT));
        openSchedule.setName("reception-open-schedule");
        addToggle = icon(secondary("Thêm mới", () -> toggleAdd(true)), "plus", Theme.PRIMARY_TEXT); addToggle.setName("reception-add-toggle");
        addToggle.setToolTipText("Thêm hồ sơ bệnh nhân mới (mô phỏng)");

        // --- Cột trái: danh sách / form thêm mới
        JButton save = primary("Lưu bệnh nhân", () -> addPatient(false)); save.setName("new-save");
        JButton cancelAdd = secondary("Đóng", () -> toggleAdd(false));
        leftCards.addCard(column(10, searchBar, filter, state, results), "list");
        leftCards.addCard(column(10,
                label("<html>Nhập thông tin hành chính cơ bản. Hệ thống cảnh báo nghi trùng theo SĐT hoặc họ tên + ngày sinh, không tự gộp hồ sơ.</html>", 12, false, Theme.MUTED),
                form("Họ tên *", newName), pair(form("Ngày sinh *", newDob), form("Giới tính *", newGender)),
                form("Số điện thoại *", newPhone, "Địa chỉ", newAddress), buttons(save, cancelAdd), addFeedback), "add");
        JPanel leftHead = new JPanel(new BorderLayout()); leftHead.setOpaque(false);
        leftHead.add(leftTitle, BorderLayout.CENTER); leftHead.add(addToggle, BorderLayout.EAST);
        JPanel left = card(column(14, leftHead, leftCards));

        // --- Cột phải: hồ sơ tóm tắt
        chips.setBorder(new EmptyBorder(2, -8, 0, 0));
        JPanel identity = new JPanel(new BorderLayout(16, 0)); identity.setOpaque(false);
        JPanel avatarBox = new JPanel(new BorderLayout()); avatarBox.setOpaque(false); avatarBox.add(avatar, BorderLayout.NORTH);
        identity.add(avatarBox, BorderLayout.WEST); identity.add(column(6, summaryName, summaryInfo, chips), BorderLayout.CENTER);
        JPanel profile = card(column(12, identity, duplicateWarning));

        // --- Tiếp nhận
        modeCards.addCard(column(8, label("Lịch hẹn hôm nay", 12, true, Theme.color("475569")), todays), "schedule");
        modeCards.addCard(form("Lý do đến khám *", visitReason), "walkin");
        mode.onChange(i -> { modeCards.show(i == 0 ? "schedule" : "walkin"); updateActions(); });
        JPanel actionRow = new JPanel(new BorderLayout(12, 0)); actionRow.setOpaque(false);
        actionRow.add(form("Bác sĩ tiếp nhận (đang trực)", doctor), BorderLayout.CENTER);
        JPanel cta = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0)); cta.setOpaque(false); cta.add(openSchedule); cta.add(checkIn); cta.add(createWalkIn);
        JPanel ctaBox = new JPanel(new BorderLayout()); ctaBox.setOpaque(false); ctaBox.add(cta, BorderLayout.SOUTH);
        actionRow.add(ctaBox, BorderLayout.EAST);
        JPanel intake = new UiCard("Tiếp nhận", null, column(12, mode, modeCards, actionRow, reason, result));
        JPanel recent = new UiCard("Hoạt động gần đây", null, timeline);

        search.getDocument().addDocumentListener(SimpleDocumentListener.of(this::refreshResults));
        searchBar.onSubmit(() -> { if (results.selectFirst()) results.listView().requestFocusInWindow(); });
        searchBar.bindShortcut(this);
        filter.onChange(i -> refreshResults());
        results.onSelect(p -> selectPatient(p.id()));
        todays.onSelect(a -> updateActions());
        doctor.addActionListener(e -> updateActions());

        add(screen(new ScreenHeader("Tiếp nhận bệnh nhân", "Tìm hồ sơ, kiểm tra lịch hẹn hôm nay, rồi check-in hoặc tiếp nhận trực tiếp",
                        buttons(ctx.resetButton(this, "reception-reset"))),
                banner("reception-banner", "Tạo phiếu ở đây chỉ đổi bộ nhớ phiên xem thử; Tổng quan là fixture độc lập, không tự cập nhật."),
                twoColumns(left, column(Theme.GAP, profile, intake, recent), 0.41)), BorderLayout.NORTH);
        store.addListener(this::refresh);
        refresh();
    }
    private static JPanel card(JComponent body) {
        Tile t = new Tile(new BorderLayout(), Theme.CARD, Theme.BORDER); t.setBorder(new EmptyBorder(18, 18, 18, 18)); t.add(body); return t;
    }
    private static JButton big(JButton b) { b.setBorder(new EmptyBorder(10, 16, 10, 16)); b.setFont(Theme.font(13, true)); return b; }

    /** Viên trạng thái trên thẻ bệnh nhân: lịch hôm nay, hoặc cảnh báo nghi trùng. */
    private Tag badge(Patient p) {
        Optional<Appointment> today = store.appointmentsOf(p.id()).stream().filter(a -> a.time().toLocalDate().equals(store.today()))
                .filter(a -> a.status() != AppointmentStatus.DA_DOI_LICH).findFirst();
        if (today.isPresent()) return new Tag(TIME.format(today.get().time()) + " · " + today.get().status().label(), tone(today.get().status()));
        if (!store.suspectedDuplicates(p.id()).isEmpty()) return new Tag("Nghi trùng", Theme.ORANGE);
        return null;
    }
    private void toggleAdd(boolean visible) {
        leftCards.show(visible ? "add" : "list"); leftTitle.setText(visible ? "Thêm bệnh nhân mới" : "Tìm bệnh nhân");
        addToggle.setVisible(!visible);
        if (visible) { info(addFeedback, " "); newName.requestFocusInWindow(); } else search.requestFocusInWindow();
        revalidate(); repaint();
    }
    private void refresh() { refreshResults(); refreshDoctors(); selectPatient(patientId); }
    private void refreshResults() {
        String q = search.getText().trim(); int f = filter.selected();
        List<Patient> rows = store.searchPatients(q).stream().filter(p -> switch (f) {
            case 1 -> store.appointmentsOf(p.id()).stream().anyMatch(a -> a.time().toLocalDate().equals(store.today()));
            case 2 -> !store.suspectedDuplicates(p.id()).isEmpty();
            default -> true;
        }).toList();
        results.setRows(rows, q.isEmpty() ? "Không có bệnh nhân trong bộ lọc này" : "Không tìm thấy “" + q + "” · bấm Thêm mới để tạo hồ sơ");
        if (results.isEmptyState()) warn(state, "Không tìm thấy bệnh nhân khớp từ khóa");
        else info(state, Theme.number(rows.size()) + " bệnh nhân" + (patientId == null ? " · chọn một người để tiếp nhận" : " · đang chọn " + patientId));
    }
    private void refreshDoctors() { replaceItems(doctor, store.onDutyDoctors().stream().map(Doctor::display).toList()); }
    private String doctorId() {
        Object v = doctor.getSelectedItem(); return v == null ? null : v.toString().substring(0, v.toString().indexOf(' '));
    }
    public void selectPatient(String id) {
        Theme.requireEdt();
        patientId = id == null || store.patient(id).isEmpty() ? null : id;
        chips.setVisible(patientId != null);
        if (patientId == null) {
            avatar.setPerson(null); summaryName.setText("Chưa chọn bệnh nhân");
            summaryInfo.setText("Tìm và chọn một bệnh nhân ở danh sách bên trái");
            info(duplicateWarning, " "); duplicateWarning.setVisible(false); todays.setRows(List.of(), "Chưa chọn bệnh nhân");
            fillTimeline(timeline, List.of(), 5, "Chưa có thao tác");
        } else {
            Patient p = store.patient(patientId).orElseThrow();
            if (results.selected().map(Patient::id).filter(patientId::equals).isEmpty()) results.select(patientId);
            avatar.setPerson(p.name(), p.id());
            summaryName.setText(p.name());
            summaryInfo.setText("Sinh ngày " + DATE.format(p.birthDate()) + (p.address().isBlank() ? "" : " · " + p.address()));
            chipId.set(p.id(), Theme.PRIMARY_TEXT);
            chipAge.set(p.gender().label() + " · " + Rows.age(store, p), Theme.color("475569"));
            chipPhone.set("SĐT " + DemoClinicStore.maskPhone(p.phone()), Theme.color("475569"));
            List<Patient> dup = store.suspectedDuplicates(patientId);
            duplicateWarning.setVisible(!dup.isEmpty());
            if (dup.isEmpty()) info(duplicateWarning, " ");
            else warn(duplicateWarning, "Nghi trùng với " + String.join(", ", dup.stream().map(Patient::id).toList()) + " · hệ thống không tự gộp hồ sơ");
            List<Appointment> today = store.appointmentsOf(patientId).stream().filter(a -> a.time().toLocalDate().equals(store.today())).toList();
            todays.setRows(today, "Không có lịch hẹn hôm nay · chọn Trực tiếp (không hẹn)");
            chipToday.setVisible(!today.isEmpty());
            if (!today.isEmpty()) chipToday.set(today.size() + " lịch hôm nay", Theme.PRIMARY_TEXT);
            String keep = todays.selected().map(Appointment::id).orElse(null);
            String target = keep != null ? keep : today.stream()
                    .filter(a -> a.status() == AppointmentStatus.DA_XAC_NHAN || a.status() == AppointmentStatus.CHO_XAC_NHAN)
                    .map(Appointment::id).findFirst().orElse(null);
            if (target != null) todays.select(target);
            refreshResults();
            fillTimeline(timeline, store.activitiesOf(patientId), 5, "Chưa có thao tác với bệnh nhân này");
        }
        updateActions();
    }
    private void updateActions() {
        boolean schedule = mode.selected() == 0;
        boolean noDoctor = store.onDutyDoctors().isEmpty();
        doctor.setEnabled(!noDoctor);
        String blocker;
        if (patientId == null) blocker = "Chưa chọn bệnh nhân";
        else if (noDoctor) blocker = "Không có bác sĩ trực ca này · không thể tạo phiếu";
        else if (schedule) {
            Appointment sel = todays.selected().orElse(null);
            blocker = sel != null ? store.checkInBlocker(sel.id(), doctorId())
                    : todays.rows().isEmpty() ? "Không có lịch hẹn hôm nay · chọn Trực tiếp (không hẹn)" : "Chọn một lịch hẹn hôm nay";
        } else blocker = store.walkInBlocker(patientId, doctorId());
        checkIn.setVisible(schedule); createWalkIn.setVisible(!schedule);
        boolean pending = schedule && todays.selected().map(a -> a.status() == AppointmentStatus.CHO_XAC_NHAN).orElse(false);
        openSchedule.setVisible(pending);
        checkIn.setEnabled(schedule && blocker == null); createWalkIn.setEnabled(!schedule && blocker == null);
        if (blocker == null) hint(reason, schedule ? "Sẵn sàng check-in lịch đã xác nhận" : "Sẵn sàng tiếp nhận trực tiếp");
        else if (patientId == null) info(reason, blocker);
        else warn(reason, blocker);
        (schedule ? checkIn : createWalkIn).setToolTipText(blocker == null ? "Tạo phiếu khám mô phỏng" : blocker);
        revalidate(); repaint();
    }
    private void doCheckIn() {
        Appointment a = todays.selected().orElse(null);
        if (a == null || patientId == null) return;
        if (!ctx.dialogs().confirm(this, "Check-in", "Check-in " + a.id() + " cho " + patientId + " và tạo phiếu khám chờ khám?")) {
            info(result, "Đã hủy thao tác"); return;
        }
        result(result, store.checkIn(a.id(), doctorId()));
    }
    private void doWalkIn() {
        if (patientId == null) return;
        if (visitReason.getText().trim().length() < 3) { fail(result, "Nhập lý do đến khám (ít nhất 3 ký tự)"); visitReason.requestFocusInWindow(); return; }
        if (!ctx.dialogs().confirm(this, "Tiếp nhận trực tiếp", "Tạo phiếu khám trực tiếp (không theo lịch) cho " + patientId + "?")) {
            info(result, "Đã hủy thao tác"); return;
        }
        Result<ExamTicket> r = store.walkIn(patientId, doctorId(), visitReason.getText());
        result(result, r); if (r.ok()) visitReason.setText("");
    }
    private void addPatient(boolean acknowledged) {
        PatientDraft draft = new PatientDraft(newName.getText(), parseDate(newDob.getText()),
                (Gender) newGender.getSelectedItem(), newPhone.getText(), newAddress.getText());
        Result<Patient> r = store.addPatient(draft, acknowledged);
        if (r.code() == Code.DUPLICATE) {
            warn(addFeedback, r.message());
            if (ctx.dialogs().confirm(this, "Nghi trùng hồ sơ", r.message() + "\nVẫn tạo hồ sơ mới riêng biệt?")) addPatient(true);
            return;
        }
        result(addFeedback, r);
        if (r.ok()) {
            for (JTextField f : new JTextField[]{newName, newDob, newPhone, newAddress}) f.setText("");
            toggleAdd(false); filter.select(0); search.setText(r.value().id()); selectPatient(r.value().id());
            result(result, r);
        }
    }
}
