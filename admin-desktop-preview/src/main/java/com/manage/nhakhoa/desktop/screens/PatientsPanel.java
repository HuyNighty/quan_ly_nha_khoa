package com.manage.nhakhoa.desktop.screens;

import com.manage.nhakhoa.desktop.demo.DemoClinicStore;
import com.manage.nhakhoa.desktop.demo.DemoClinicStore.*;
import com.manage.nhakhoa.desktop.ui.DemoTable;
import com.manage.nhakhoa.desktop.ui.DemoTable.Column;
import com.manage.nhakhoa.desktop.ui.DemoTable.Strong;
import com.manage.nhakhoa.desktop.ui.CardList;
import com.manage.nhakhoa.desktop.ui.DemoWidgets;
import com.manage.nhakhoa.desktop.ui.Flat;
import com.manage.nhakhoa.desktop.ui.Theme;
import com.manage.nhakhoa.desktop.ui.UiIcons;
import java.awt.*;
import java.time.Period;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import static com.manage.nhakhoa.desktop.ui.DemoWidgets.*;
import static com.manage.nhakhoa.desktop.ui.UiComponents.*;

/**
 * #04 Hồ sơ bệnh nhân (BENH_NHAN) — danh sách có lọc nhanh, hồ sơ chi tiết 4 tab: hành chính (sửa mô phỏng),
 * lịch hẹn & phiếu khám, bệnh án tóm tắt (chỉ đọc), hoạt động.
 */
public final class PatientsPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private final transient ScreenContext ctx;
    private final transient DemoClinicStore store;
    private final Flat.SearchBar searchBar = new Flat.SearchBar("patients-search", "Tìm mã BN, họ tên hoặc SĐT…");
    private final JTextField search = searchBar.field();
    private final Flat.Segmented filter = new Flat.Segmented("patients-filter", List.of("Tất cả", "Lịch hôm nay", "Nghi trùng"), null);
    private final JLabel count = label(" ", 12, false, Theme.MUTED);
    private final CardList<Patient> table;
    // Chi tiết
    private final Flat.Avatar avatar = new Flat.Avatar(68);
    private final JLabel title = value("patient-title");
    private final JLabel subtitle = label(" ", 12, false, Theme.MUTED);
    private final Flat.Pill chipId = new Flat.Pill(" ", Theme.PRIMARY_TEXT), chipAge = new Flat.Pill(" ", Theme.color("475569")),
            chipRecord = new Flat.Pill(" ", Theme.color("475569"));
    private final JLabel duplicate = feedback("patient-duplicate");
    private final Flat.Segmented tabs = new Flat.Segmented("patient-tabs", List.of("Hành chính", "Lịch & phiếu", "Bệnh án", "Hoạt động"), null);
    private final FitCards tabCards = new FitCards();
    private final JLabel gender = value("patient-gender"), dob = value("patient-dob"), address = value("patient-address"),
            contact = value("patient-contact"), created = value("patient-created");
    private final JLabel phone = value("patient-phone");
    private final JToggleButton reveal = new JToggleButton("Hiện");
    private final JButton edit;
    private final JPanel editForm;
    private final JTextField editPhone = field("edit-phone", 12, "10 số, bắt đầu bằng 0");
    private final JTextField editAddress = field("edit-address", 20, "Địa chỉ");
    private final JTextField editContact = field("edit-contact", 20, "Người liên hệ / giám hộ");
    private final JLabel feedback = feedback("patients-feedback");
    private final DemoTable<Appointment> history;
    private final DemoTable<ExamTicket> visits;
    private final JLabel recordId = value("patient-record"), allergy = value("patient-allergy"), note = value("patient-note"), conclusion = value("patient-conclusion");
    private final JPanel activity = timelineBox("patient-activity");
    private final JPanel detailBody;
    private final JLabel detailEmpty = label("Chọn một bệnh nhân trong danh sách để xem hồ sơ", 13, false, Theme.MUTED);
    private transient String patientId;

    public PatientsPanel(ScreenContext ctx) {
        super(new BorderLayout()); setOpaque(false); setName("screen-BENH_NHAN");
        this.ctx = ctx; this.store = ctx.store();
        table = Rows.patients("patients-table", store, p -> store.appointmentsOf(p.id()).stream().findFirst()
                .map(a -> new DemoTable.Tag(DemoWidgets.DATE.format(a.time().toLocalDate()).substring(0, 5) + " · " + a.status().label(), tone(a.status())))
                .orElse(store.suspectedDuplicates(p.id()).isEmpty() ? null : new DemoTable.Tag("Nghi trùng", Theme.ORANGE)), 9);
        table.onSelect(p -> select(p.id()));
        history = new DemoTable<>("patient-history", List.of(
                new Column<Appointment>("Ngày giờ", a -> new Strong(DATE_TIME.format(a.time())), 120), new Column<Appointment>("Mã LH", Appointment::id, 100),
                new Column<Appointment>("Bác sĩ", a -> store.doctorName(a.doctorId()), 140),
                new Column<Appointment>("Trạng thái", a -> status(a.status()), 125)), Appointment::id, 4);
        visits = new DemoTable<>("patient-visits", List.of(
                new Column<ExamTicket>("Tiếp nhận", t -> new Strong(DATE_TIME.format(t.receivedAt())), 120), new Column<ExamTicket>("Mã PK", ExamTicket::id, 100),
                new Column<ExamTicket>("Hình thức", t -> t.appointmentId() == null ? "Trực tiếp" : "Theo " + t.appointmentId(), 120),
                new Column<ExamTicket>("Trạng thái", t -> ticket(t.status()), 125)), ExamTicket::id, 3);
        reveal.setName("patient-reveal"); reveal.setFont(Theme.font(12, true)); reveal.setFocusPainted(false);
        reveal.setContentAreaFilled(false); reveal.setBorder(new EmptyBorder(2, 6, 2, 6)); reveal.setForeground(Theme.PRIMARY_TEXT);
        reveal.setIcon(new UiIcons("eye", Theme.PRIMARY_TEXT, 14)); reveal.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        reveal.setToolTipText("SĐT che mặc định; chỉ hiện khi cần liên hệ (mô phỏng)");
        reveal.addActionListener(e -> showPhone());
        edit = icon(secondary("Sửa thông tin hành chính", this::openEdit), "document", Theme.PRIMARY_TEXT); edit.setName("patient-edit");
        JButton save = primary("Lưu thay đổi", this::saveEdit); save.setName("edit-save");
        JButton close = secondary("Đóng", () -> editFormVisible(false));
        editForm = new EditBox(column(10, label("Sửa thông tin hành chính", 13, true, Theme.TEXT),
                form("Số điện thoại *", editPhone, "Địa chỉ", editAddress, "Người liên hệ", editContact),
                label("<html>Họ tên / ngày sinh không sửa ở bản demo để tránh lẫn hồ sơ.</html>", 11, false, Theme.MUTED),
                buttons(save, close)));
        editForm.setVisible(false);
        JPanel phoneRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0)); phoneRow.setOpaque(false); phoneRow.add(phone); phoneRow.add(reveal);
        addTab(column(12, Flat.infoGrid("Giới tính", gender, "Ngày sinh", dob, "Số điện thoại", phoneRow, "Địa chỉ", address,
                "Người liên hệ", contact, "Ngày tạo hồ sơ", created), buttons(edit), editForm), "0");
        addTab(column(8, label("Lịch hẹn", 12, true, Theme.color("475569")), history,
                label("Phiếu khám", 12, true, Theme.color("475569")), visits,
                buttons(link("Mở Lịch hẹn", () -> ctx.navigate().accept("LICH_HEN")), link("Mở Tiếp nhận", () -> ctx.navigate().accept("TIEP_NHAN")))), "1");
        JLabel readOnly = feedback("patient-readonly");
        hint(readOnly, "Thông tin chuyên môn chỉ đọc · Quản trị viên không ghi chẩn đoán · dữ liệu mẫu, không phải bệnh án thật");
        addTab(column(12, readOnly, Flat.infoGrid("Mã hồ sơ bệnh án", recordId, "Dị ứng", allergy, "Ghi chú", note, "Kết luận gần nhất", conclusion)), "2");
        addTab(activity, "3");
        tabs.onChange(i -> tabCards.show(String.valueOf(i)));
        filter.onChange(i -> refreshTable());
        search.getDocument().addDocumentListener(SimpleDocumentListener.of(this::refreshTable));
        JButton clear = link("Xóa bộ lọc", () -> { search.setText(""); filter.select(0); }); clear.setName("patients-clear");

        title.setFont(Theme.font(20, true));
        JPanel chips = buttons(chipId, chipAge, chipRecord); chips.setBorder(new EmptyBorder(2, -8, 0, 0));
        JPanel identity = new JPanel(new BorderLayout(16, 0)); identity.setOpaque(false);
        JPanel avatarBox = new JPanel(new BorderLayout()); avatarBox.setOpaque(false); avatarBox.add(avatar, BorderLayout.NORTH);
        identity.add(avatarBox, BorderLayout.WEST); identity.add(column(6, title, subtitle, chips), BorderLayout.CENTER);
        detailBody = column(12, identity, duplicate, tabs, tabCards, feedback);
        searchBar.onSubmit(() -> { if (table.selectFirst()) table.listView().requestFocusInWindow(); });
        searchBar.bindShortcut(this);
        JPanel filterRow = new JPanel(new BorderLayout(10, 0)); filterRow.setOpaque(false);
        filterRow.add(filter, BorderLayout.CENTER); filterRow.add(clear, BorderLayout.EAST);
        JComponent left = new UiCard("Danh sách bệnh nhân", count, column(10, searchBar, filterRow, table));
        JComponent right = new UiCard("Hồ sơ", null, column(10, detailEmpty, detailBody));
        add(screen(new ScreenHeader("Hồ sơ bệnh nhân", "Thông tin hành chính tách khỏi thông tin chuyên môn · SĐT che mặc định",
                        buttons(ctx.resetButton(this, "patients-reset"))),
                banner("patients-banner", "Tên, SĐT, địa chỉ đều là dữ liệu tổng hợp; sửa hành chính chỉ đổi bộ nhớ phiên."),
                twoColumns(left, right, 0.44)), BorderLayout.NORTH);
        store.addListener(this::refresh);
        refresh();
    }
    /** Neo nội dung tab lên trên để không bị căn giữa theo tab cao nhất. */
    private void addTab(JComponent content, String key) {
        tabCards.addCard(content, key);
    }
    /** Khung nền nhạt cho form sửa. */
    private static final class EditBox extends JPanel {
        private static final long serialVersionUID = 1L;
        EditBox(JComponent body) {
            super(new BorderLayout()); setBackground(Theme.SOFT);
            setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Theme.BORDER), new EmptyBorder(12, 12, 12, 12)));
            add(body);
        }
    }
    private void refresh() { refreshTable(); select(patientId); }
    private void refreshTable() {
        String q = search.getText().trim(); int f = filter.selected();
        List<Patient> rows = store.searchPatients(q).stream().filter(p -> switch (f) {
            case 1 -> store.appointmentsOf(p.id()).stream().anyMatch(a -> a.time().toLocalDate().equals(store.today()));
            case 2 -> !store.suspectedDuplicates(p.id()).isEmpty();
            default -> true;
        }).toList();
        table.setRows(rows, q.isEmpty() && f == 0 ? "Chưa có bệnh nhân" : "Không có bệnh nhân khớp bộ lọc · bấm “Xóa bộ lọc”");
        count.setText(Theme.number(rows.size()) + " / " + Theme.number(store.patients().size()) + " hồ sơ");
    }
    public void select(String id) {
        Theme.requireEdt();
        patientId = id == null || store.patient(id).isEmpty() ? null : id;
        boolean has = patientId != null;
        detailEmpty.setVisible(!has); detailBody.setVisible(has);
        if (!has) {
            title.setText("Chưa chọn bệnh nhân"); avatar.setPerson(null, null); editFormVisible(false);
            history.setRows(List.of(), "Chưa chọn bệnh nhân"); visits.setRows(List.of(), "Chưa chọn bệnh nhân");
            fillTimeline(activity, List.of(), 8, "Chưa chọn bệnh nhân");
            revalidate(); repaint(); return;
        }
        Patient p = store.patient(patientId).orElseThrow();
        if (table.selected().map(Patient::id).filter(patientId::equals).isEmpty()) table.select(patientId);
        avatar.setPerson(p.name(), p.id()); title.setText(p.name());
        subtitle.setText("Sinh ngày " + DATE.format(p.birthDate()) + " · hồ sơ tạo " + DATE.format(p.createdAt().toLocalDate()));
        chipId.set(p.id(), Theme.PRIMARY_TEXT);
        chipAge.set(p.gender().label() + " · " + Period.between(p.birthDate(), store.today()).getYears() + " tuổi", Theme.color("475569"));
        chipRecord.set(store.medical(p.id()).map(m -> m.recordId()).orElse("Chưa có HSBA"), Theme.color("475569"));
        List<Patient> dup = store.suspectedDuplicates(p.id());
        if (dup.isEmpty()) info(duplicate, "Không phát hiện hồ sơ nghi trùng");
        else warn(duplicate, "Nghi trùng với " + String.join(", ", dup.stream().map(Patient::id).toList()) + " · không tự gộp hồ sơ");
        gender.setText(p.gender().label()); dob.setText(DATE.format(p.birthDate()));
        address.setText(p.address().isBlank() ? "—" : p.address()); contact.setText(p.contact().isBlank() ? "—" : p.contact());
        created.setText(DATE.format(p.createdAt().toLocalDate()) + " (mẫu)");
        showPhone();
        history.setRows(store.appointmentsOf(p.id()), "Chưa có lịch hẹn");
        visits.setRows(store.ticketsOf(p.id()), "Chưa có phiếu khám");
        store.medical(p.id()).ifPresent(m -> {
            recordId.setText(m.recordId()); allergy.setText(m.allergy()); note.setText(m.note()); conclusion.setText(m.lastConclusion());
        });
        fillTimeline(activity, store.activitiesOf(p.id()), 8, "Chưa có thao tác với hồ sơ này");
        revalidate(); repaint();
    }
    private void showPhone() {
        store.patient(patientId).ifPresent(p -> phone.setText(reveal.isSelected() ? p.phone() : DemoClinicStore.maskPhone(p.phone())));
        reveal.setText(reveal.isSelected() ? "Che" : "Hiện");
    }
    private void openEdit() {
        store.patient(patientId).ifPresent(p -> {
            editPhone.setText(p.phone()); editAddress.setText(p.address()); editContact.setText(p.contact());
            tabs.select(0); editFormVisible(true);
        });
    }
    private void editFormVisible(boolean visible) { editForm.setVisible(visible); edit.setVisible(!visible); revalidate(); repaint(); }
    private void saveEdit() { saveEdit(false); }
    private void saveEdit(boolean acknowledged) {
        if (patientId == null) return;
        if (!acknowledged && !ctx.dialogs().confirm(this, "Cập nhật hành chính", "Lưu thay đổi hành chính cho " + patientId + "?")) {
            info(feedback, "Đã hủy thao tác"); return;
        }
        Result<Patient> r = store.updatePatientAdmin(patientId, editPhone.getText(), editAddress.getText(), editContact.getText(), acknowledged);
        if (r.code() == Code.DUPLICATE) {
            warn(feedback, r.message());
            if (ctx.dialogs().confirm(this, "SĐT trùng", r.message() + "\nVẫn lưu (ví dụ người thân dùng chung số)?")) saveEdit(true);
            return;
        }
        result(feedback, r);
        if (r.ok()) editFormVisible(false);
    }
}
