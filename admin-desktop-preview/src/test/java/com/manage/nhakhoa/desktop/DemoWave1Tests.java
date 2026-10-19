package com.manage.nhakhoa.desktop;

import com.manage.nhakhoa.desktop.data.DemoDashboardDataProvider;
import com.manage.nhakhoa.desktop.data.DemoDashboardDataProvider.Scenario;
import com.manage.nhakhoa.desktop.demo.DemoClinicStore;
import com.manage.nhakhoa.desktop.demo.DemoClinicStore.*;
import com.manage.nhakhoa.desktop.model.DashboardViewModel.AppointmentStatus;
import com.manage.nhakhoa.desktop.screens.AppointmentsPanel;
import com.manage.nhakhoa.desktop.ui.DemoDialogs;
import com.manage.nhakhoa.desktop.ui.RowView;
import com.manage.nhakhoa.desktop.ui.DemoWidgets;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;
import javax.swing.*;

/** UI-DEMO-1: kho mô phỏng + 3 màn Tiếp nhận / Lịch hẹn / Hồ sơ bệnh nhân. Chạy từ PreviewTests. */
final class DemoWave1Tests {
    private static int checks;
    private static final Clock FIXED = AdminPreview.fixtureClock();
    private DemoWave1Tests() { }

    static int run() throws Exception {
        checks = 0;
        storeSeedAndRules(); menuByBusinessPhase();
        edt(() -> { routing(); reception(); appointments(); patients(); crossScreen(); return null; });
        return checks;
    }
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError("UI-DEMO-1: " + message);
        checks++; System.out.println("ĐẠT · [UI-DEMO-1] " + message);
    }

    /** Hộp thoại kịch bản: trả lời theo hàng đợi, mặc định đồng ý. */
    static final class Scripted implements DemoDialogs {
        final Deque<Object> answers = new ArrayDeque<>();
        final List<String> asked = new ArrayList<>();
        @Override public boolean confirm(Component parent, String title, String message) {
            asked.add(title); Object a = answers.poll(); return a == null || Boolean.TRUE.equals(a);
        }
        @Override public String reason(Component parent, String title, String prompt) {
            asked.add(title); Object a = answers.poll(); return a instanceof String s ? s : null;
        }
    }

    // ------------------------------------------------------------------ store
    private static void storeSeedAndRules() {
        DemoClinicStore s = new DemoClinicStore(FIXED);
        LocalDate d = s.today();
        var dash = DemoDashboardDataProvider.sample(FIXED, Scenario.FULL);
        check(d.equals(LocalDate.of(2026, 10, 12)) && s.now().toString().equals("2026-10-12T07:45"), "Store dùng cùng mốc 12/10/2026 07:45 với Tổng quan");
        check(s.statusCounts(d).equals(dash.appointments()), "Đối soát 7 trạng thái lịch hẹn với fixture Tổng quan (2/2/1/2/1/1/1)");
        check(s.appointmentsOn(d).stream().map(Appointment::id).toList().equals(
                List.of("LH000001", "LH000002", "LH000003", "LH000004", "LH000005", "LH000006", "LH000007", "LH000008", "LH000009", "LH000010")),
                "Lịch trong ngày là LH000001–LH000010");
        check(s.activeCount(d) == 8, "Còn hiệu lực = 10 − 1 hủy − 1 đổi lịch = 8");
        var todayTickets = s.ticketsOn(d);
        check(count(todayTickets, TicketStatus.CHO_KHAM) == dash.kpis().waiting() && count(todayTickets, TicketStatus.DANG_KHAM) == dash.kpis().examining()
                && count(todayTickets, TicketStatus.DA_KET_LUAN) == dash.kpis().concluded(), "Phiếu khám 0/1/2 khớp Tổng quan");
        check(s.appointments().stream().allMatch(a -> s.patient(a.patientId()).isPresent() && s.doctor(a.doctorId()).isPresent()),
                "Mọi lịch tham chiếu BN và BS tồn tại");
        check(s.tickets().stream().allMatch(t -> t.appointmentId() == null || s.appointment(t.appointmentId())
                .map(a -> a.patientId().equals(t.patientId())).orElse(false)), "Phiếu theo lịch cùng bệnh nhân với lịch gốc");
        Appointment old = s.appointment("LH000009").orElseThrow(), fresh = s.appointment("LH000011").orElseThrow();
        check(old.status() == AppointmentStatus.DA_DOI_LICH && "LH000011".equals(old.replacementId()) && "LH000009".equals(fresh.originId()),
                "Liên kết hai chiều lịch gốc LH000009 ↔ lịch mới LH000011");
        check(s.searchPatients("nguyen van an").stream().map(Patient::id).toList().equals(List.of("BN000007", "BN000011")), "Tìm bệnh nhân không dấu");
        check(s.searchPatients("0900000105").stream().map(Patient::id).toList().equals(List.of("BN000005")) && s.searchPatients("bn000012").size() == 1,
                "Tìm theo SĐT và mã không phân biệt hoa thường");
        check(s.searchPatients("khong ton tai").isEmpty(), "Không tìm thấy trả danh sách rỗng");
        check(s.suspectedDuplicates("BN000007").stream().map(Patient::id).toList().equals(List.of("BN000011")), "Nghi trùng họ tên + ngày sinh, không tự gộp");
        check(DemoClinicStore.maskPhone("0900000109").equals("090****109"), "Che SĐT mặc định");

        Result<Patient> invalid = s.addPatient(new PatientDraft(" ", LocalDate.of(2030, 1, 1), Gender.NAM, "12345", ""), false);
        check(invalid.code() == Code.INVALID && invalid.message().contains("Họ tên") && invalid.message().contains("Ngày sinh")
                && invalid.message().contains("Số điện thoại"), "Validate form bệnh nhân bằng tiếng Việt");
        int before = s.patients().size();
        Result<Patient> dup = s.addPatient(new PatientDraft("Khách Mới", LocalDate.of(2000, 1, 1), Gender.NU, "0900000101", ""), false);
        check(dup.code() == Code.DUPLICATE && dup.message().contains("BN000001") && s.patients().size() == before, "Nghi trùng SĐT chặn tạo cho tới khi xác nhận");
        Result<Patient> added = s.addPatient(new PatientDraft("Khách Mới", LocalDate.of(2000, 1, 1), Gender.NU, "0900000101", ""), true);
        check(added.ok() && added.value().id().equals("BN000013") && added.message().startsWith(DemoClinicStore.SAVED),
                "Xác nhận nghi trùng tạo hồ sơ riêng BN000013, thông điệp mô phỏng");

        String pending = s.checkInBlocker("LH000008", "NV002");
        check(pending != null && pending.contains("Chờ xác nhận") && !s.checkIn("LH000008", "NV002").ok(), "Không check-in lịch Chờ xác nhận");
        check(s.confirm("LH000008").ok() && s.appointment("LH000008").orElseThrow().status() == AppointmentStatus.DA_XAC_NHAN, "Xác nhận Chờ → Đã xác nhận");
        check(!s.confirm("LH000008").ok(), "Không xác nhận lại lịch đã xác nhận");
        Result<ExamTicket> in = s.checkIn("LH000008", "NV002");
        check(in.ok() && in.value().id().equals("PK000004") && in.value().status() == TicketStatus.CHO_KHAM
                && s.appointment("LH000008").orElseThrow().status() == AppointmentStatus.DA_DEN, "Check-in tạo PK000004 Chờ khám và lịch thành Đã đến");
        check(!s.checkIn("LH000008", "NV002").ok() && s.ticketsOn(d).size() == 4, "Chống tạo trùng phiếu theo mã lịch");
        check(s.checkInBlocker("LH000005", "NV004").contains("không trực"), "Không check-in với bác sĩ không trực");
        check(s.checkInBlocker("LH000011", "NV002").contains("hôm nay"), "Chỉ check-in lịch của ngày hôm nay");
        check(s.walkIn("BN000012", "NV003", " ").code() == Code.INVALID, "Tiếp nhận trực tiếp bắt buộc lý do");
        check(s.walkIn("BN000012", "NV003", "Ê buốt răng").ok() && s.walkInBlocker("BN000012", "NV003").contains("chưa kết thúc"),
                "Tiếp nhận trực tiếp tạo phiếu, chặn phiếu thứ hai đang mở");
        check(s.walkInBlocker("BN000004", "NV002").contains("PK000003"), "BN đang khám không tạo thêm phiếu trực tiếp");
        s.setOnDuty("NV002", false); s.setOnDuty("NV003", false);
        check(s.walkInBlocker("BN000006", "NV002").equals("Không có bác sĩ trực ca này"), "Trạng thái không có bác sĩ trực");
        s.reset();
        check(s.patients().size() == 12 && s.tickets().size() == 3 && s.statusCounts(d).equals(dash.appointments())
                && s.onDutyDoctors().size() == 2, "Reset khôi phục seed");

        check(s.reschedule("LH000005", "NV002", d.atTime(7, 30)).code() == Code.INVALID, "Không đổi lịch về quá khứ");
        check(s.reschedule("LH000005", "NV002", d.plusDays(2).atTime(9, 0)).message().contains("Khung giờ"), "Chặn trùng khung giờ của bác sĩ");
        check(s.createAppointment("BN000012", "NV002", d.atTime(21, 0), "Khám").message().contains("07:00–20:00"), "Khung giờ làm việc 07:00–20:00");
        check(s.createAppointment("BN000012", "NV002", d.atTime(10, 10), "Khám").message().contains("15 phút"), "Giờ hẹn theo bước 15 phút");
        Result<Appointment> moved = s.reschedule("LH000005", "NV003", d.plusDays(1).atTime(9, 0));
        check(moved.ok() && moved.value().id().equals("LH000012") && moved.value().status() == AppointmentStatus.CHO_XAC_NHAN
                && "LH000005".equals(moved.value().originId()) && s.appointment("LH000005").orElseThrow().status() == AppointmentStatus.DA_DOI_LICH,
                "Đổi lịch tạo bản mới LH000012 Chờ xác nhận, bản cũ Đã đổi lịch");
        check(s.appointmentsOn(d).size() == 10 && s.activeCount(d) == 7, "Tổng trong ngày giữ 10, còn hiệu lực giảm còn 7");
        check(s.cancel("LH000006", "abc").code() == Code.INVALID, "Hủy bắt buộc lý do ≥ 5 ký tự");
        check(s.cancel("LH000001", "Bệnh nhân báo bận").code() == Code.RULE, "Không hủy lịch đã hoàn thành");
        check(s.cancel("LH000006", "Bệnh nhân báo bận").ok() && s.appointment("LH000006").orElseThrow().cancelReason().equals("Bệnh nhân báo bận"),
                "Hủy có lý do được lưu");
        Result<Appointment> created = s.createAppointment("BN000012", "NV002", d.plusDays(1).atTime(8, 15), "Khám định kỳ");
        check(created.ok() && created.value().status() == AppointmentStatus.CHO_XAC_NHAN, "Tạo lịch mới ở trạng thái Chờ xác nhận");
        check(s.updatePatientAdmin("BN000002", "0900000101", "", "", false).code() == Code.DUPLICATE, "Sửa SĐT trùng cần xác nhận");
        check(s.updatePatientAdmin("BN000002", "0911", "", "", false).code() == Code.INVALID, "Sửa SĐT sai định dạng bị chặn");
        check(s.activities().stream().allMatch(a -> (a.actor().equals(DemoClinicStore.ACTOR) || a.actor().endsWith("(mô phỏng)")) && !a.action().contains("chẩn đoán")),
                "Nhật ký demo chỉ chứa actor/hành động/mã, không dữ liệu chuyên môn");
    }
    private static long count(List<ExamTicket> tickets, TicketStatus status) { return tickets.stream().filter(t -> t.status() == status).count(); }

    // ------------------------------------------------------------------ UI
    private static AdminShell shell(Scripted dialogs) {
        return new AdminShell(FIXED, true, new DemoClinicStore(FIXED), dialogs);
    }
    private static void menuByBusinessPhase() {
        check(MenuDefinition.GROUPS.stream().map(MenuDefinition.Group::label).toList().equals(List.of(
                "Tổ chức", "Lập kế hoạch", "Tiếp nhận", "Khám", "Điều trị", "Thanh toán", "Cung ứng", "Tổng kết")),
                "Menu nhóm theo giai đoạn: Tổ chức → Kế hoạch → Tiếp nhận → Khám → Điều trị → Thanh toán → Kho → Tổng kết");
        check(!groupOf("TIEP_NHAN").equals(groupOf("LICH_HEN")) && !groupOf("PHIEU_KHAM").equals(groupOf("KE_HOACH")),
                "Tiếp nhận tách khỏi Lịch hẹn; Khám tách khỏi Điều trị");
        var tasks = DemoDashboardDataProvider.sample(FIXED, Scenario.FULL).tasks();
        for (MenuDefinition.Group g : MenuDefinition.GROUPS) {
            long pointing = tasks.stream().filter(t -> g.entries().stream().anyMatch(e -> e.key().equals(t.targetScreenKey()))).count();
            check(g.demoBadge() == pointing, "Badge nhóm " + g.label() + " = số nhóm việc Tổng quan trỏ tới (" + pointing + ")");
        }
        check(MenuDefinition.GROUPS.stream().mapToInt(MenuDefinition.Group::demoBadge).sum() == tasks.size(), "Tổng badge menu = 9 nhóm việc");
    }
    private static String groupOf(String key) {
        return MenuDefinition.GROUPS.stream().filter(g -> g.entries().stream().anyMatch(e -> e.key().equals(key))).findFirst().orElseThrow().key();
    }
    private static void routing() throws Exception {
        AdminShell shell = shell(new Scripted());
        for (String key : List.of("TIEP_NHAN", "LICH_HEN", "BENH_NHAN")) {
            shell.navigate(key);
            check(shell.implemented(key) && shell.currentScreen().equals(key) && ((JLabel) find(shell, "breadcrumb")).getText().endsWith(MenuDefinition.entry(key).label())
                    && visible(shell, "screen-" + key), "Router mở màn mô phỏng thật: " + key);
            check(texts(shell.screen(key)).stream().anyMatch(t -> t.contains(DemoWidgets.NOTICE)), "Nhãn DỮ LIỆU MÔ PHỎNG trên " + key);
        }
        long remaining = MenuDefinition.entries().stream().filter(e -> !shell.implemented(e.key())).count();
        check(remaining == 0, "Không còn màn placeholder: 21/21 mục menu có giao diện mô phỏng");
        shell.close();
    }
    private static void reception() throws Exception {
        Scripted dialogs = new Scripted(); AdminShell shell = shell(dialogs); shell.navigate("TIEP_NHAN");
        JComponent screen = shell.screen("TIEP_NHAN");
        check(text(screen, "reception-summary-name").contains("Chưa chọn bệnh nhân") && !((JButton) find(screen, "reception-checkin")).isEnabled(),
                "State: chưa chọn bệnh nhân, nút check-in khóa");
        field(screen, "reception-search").setText("khong co ai");
        check(table(screen, "reception-results").isEmptyState() && text(screen, "reception-state").contains("Không tìm thấy"), "State: không tìm thấy");
        field(screen, "reception-search").setText("nguyen van an");
        RowView<?> results = table(screen, "reception-results");
        check(results.rows().size() == 2, "Tìm không dấu ra 2 hồ sơ cùng tên");
        field(screen, "reception-search").getActionMap().get("submit-search").actionPerformed(null);
        check(results.selected().map(o -> ((Patient) o).id()).orElse("").equals("BN000007"), "Enter trong ô tìm kiếm chọn kết quả đầu tiên");
        check(find(screen, "reception-search-clear").isVisible(), "Có chữ thì hiện nút xóa nhanh");
        click(screen, "reception-search-clear");
        check(field(screen, "reception-search").getText().isEmpty() && results.rows().size() == 12, "Nút xóa nhanh trả lại toàn bộ danh sách");
        click(screen, "reception-filter-2");
        check(results.rows().size() == 2, "Lọc nhanh Nghi trùng trong màn Tiếp nhận");
        click(screen, "reception-filter-0");
        field(screen, "reception-search").setText("nguyen van an");
        results.select("BN000007");
        check(text(screen, "reception-summary-id").equals("BN000007") && text(screen, "reception-duplicate-warning").contains("BN000011"),
                "State: nghi trùng hiển thị cảnh báo");
        field(screen, "reception-search").setText("BN000008"); results.select("BN000008");
        check(text(screen, "reception-reason").contains("Chờ xác nhận") && !((JButton) find(screen, "reception-checkin")).isEnabled()
                && find(screen, "reception-open-schedule").isVisible(), "State: lịch chờ xác nhận → yêu cầu xác nhận trước");
        click(screen, "reception-open-schedule");
        check(shell.currentScreen().equals("LICH_HEN"), "Nút mở Lịch hẹn điều hướng đúng khóa");
        shell.navigate("TIEP_NHAN");
        field(screen, "reception-search").setText("BN000005"); results.select("BN000005");
        check(((JButton) find(screen, "reception-checkin")).isEnabled(), "State: lịch đã xác nhận cho phép check-in");
        dialogs.answers.add(false); click(screen, "reception-checkin");
        check(shell.store().ticketForAppointment("LH000005").isEmpty() && text(screen, "reception-feedback").contains("hủy"), "Hủy hộp thoại không tạo phiếu");
        click(screen, "reception-checkin");
        check(shell.store().ticketForAppointment("LH000005").isPresent() && text(screen, "reception-feedback").contains(DemoClinicStore.SAVED)
                && text(screen, "reception-feedback").contains("PK000004"), "Check-in mô phỏng tạo PK000004 và báo không lưu hệ thống thật");
        check(text(screen, "reception-reason").contains("đã check-in") && !((JButton) find(screen, "reception-checkin")).isEnabled(), "State: đã check-in");
        check(texts(find(screen, "reception-timeline")).stream().anyMatch(t -> t.contains("PK000004")), "Timeline hiển thị kết quả thao tác");
        field(screen, "reception-search").setText("BN000012"); results.select("BN000012");
        check(text(screen, "reception-reason").contains("Không có lịch hẹn hôm nay"), "BN không có lịch được gợi ý tiếp nhận trực tiếp");
        ((AbstractButton) find(screen, "reception-mode-walkin")).doClick(0);
        click(screen, "reception-walkin");
        check(text(screen, "reception-feedback").contains("lý do"), "State: form tiếp nhận thiếu dữ liệu");
        field(screen, "reception-visit-reason").setText("Ê buốt răng"); click(screen, "reception-walkin");
        check(shell.store().ticketsOf("BN000012").size() == 1 && shell.store().ticketsOf("BN000012").get(0).appointmentId() == null,
                "Tiếp nhận trực tiếp tạo phiếu không gắn lịch");
        shell.store().setOnDuty("NV002", false); shell.store().setOnDuty("NV003", false);
        check(text(screen, "reception-reason").contains("Không có bác sĩ trực") && !find(screen, "reception-doctor").isEnabled(), "State: không có bác sĩ");
        shell.store().reset();
        click(screen, "reception-add-toggle");
        field(screen, "new-name").setText("A"); click(screen, "new-save");
        check(text(screen, "new-feedback").contains("Họ tên") && shell.store().patients().size() == 12, "Form thêm BN báo lỗi validate");
        field(screen, "new-name").setText("Khách Thử"); field(screen, "new-dob").setText("01/01/2000"); field(screen, "new-phone").setText("0900000101");
        dialogs.answers.add(false); click(screen, "new-save");
        check(text(screen, "new-feedback").contains("Nghi trùng") && shell.store().patients().size() == 12, "Nghi trùng: từ chối thì không tạo");
        dialogs.answers.add(true); click(screen, "new-save");
        check(shell.store().patient("BN000013").isPresent() && text(screen, "reception-summary-id").equals("BN000013") && text(screen, "reception-summary-name").equals("Khách Thử"),
                "Nghi trùng: xác nhận tạo hồ sơ riêng và chọn ngay");
        shell.store().reset(); field(screen, "reception-search").setText(""); ((AbstractButton) find(screen, "reception-mode-schedule")).doClick(0);
        results.select("BN000005");
        snapshot(shell, 1382, 840, "demo1-tiep-nhan-1382x840.png");
        snapshot(shell, 1280, 760, "demo1-tiep-nhan-1280x760.png");
        shell.close();
    }
    private static void appointments() throws Exception {
        Scripted dialogs = new Scripted(); AdminShell shell = shell(dialogs); shell.navigate("LICH_HEN");
        JComponent screen = shell.screen("LICH_HEN");
        check(text(screen, "appt-total").equals("10") && text(screen, "appt-active").equals("8") && text(screen, "appt-stat-CHO_XAC_NHAN").equals("2")
                && text(screen, "appt-stat-DA_DOI_LICH").equals("1"), "Thống kê 7 trạng thái + tổng + còn hiệu lực");
        RowView<?> t = table(screen, "appt-table");
        check(t.rows().size() == 10, "Bảng ngày có 10 lịch");
        ((JComboBox<?>) find(screen, "appt-status-filter")).setSelectedItem("Đã hủy");
        check(t.rows().size() == 1, "Lọc trạng thái Đã hủy");
        ((JComboBox<?>) find(screen, "appt-status-filter")).setSelectedIndex(0);
        ((JComboBox<?>) find(screen, "appt-doctor-filter")).setSelectedIndex(2);
        check(t.rows().size() == 5, "Lọc theo bác sĩ NV003");
        field(screen, "appt-search").setText("xyz");
        check(t.isEmptyState() && t.emptyText().contains("Xóa bộ lọc"), "State rỗng do bộ lọc có hướng dẫn");
        click(screen, "appt-clear-filters");
        check(t.rows().size() == 10, "Xóa bộ lọc");
        t.select("LH000001");
        check(!((JButton) find(screen, "appt-confirm")).isEnabled() && !((JButton) find(screen, "appt-cancel")).isEnabled(), "Lịch hoàn thành không có thao tác");
        t.select("LH000008"); click(screen, "appt-confirm");
        check(shell.store().appointment("LH000008").orElseThrow().status() == AppointmentStatus.DA_XAC_NHAN && text(screen, "appt-feedback").contains(DemoClinicStore.SAVED)
                && text(screen, "appt-stat-CHO_XAC_NHAN").equals("1"), "Xác nhận cập nhật bảng và thống kê");
        t.select("LH000010"); dialogs.answers.add("abc"); click(screen, "appt-cancel");
        check(text(screen, "appt-feedback").contains("Lý do hủy") && shell.store().appointment("LH000010").orElseThrow().status() == AppointmentStatus.CHO_XAC_NHAN,
                "Hủy với lý do ngắn bị chặn");
        dialogs.answers.add(Boolean.FALSE); click(screen, "appt-cancel");
        check(text(screen, "appt-feedback").contains("Đã hủy thao tác"), "Đóng hộp thoại lý do không đổi dữ liệu");
        t.select("LH000006"); click(screen, "appt-reschedule");
        check(find(screen, "appt-form-save").isShowing() || find(screen, "appt-form-save").getParent() != null, "Mở form đổi lịch");
        field(screen, "appt-form-date").setText("12/10/2026"); field(screen, "appt-form-time").setText("07:30"); click(screen, "appt-form-save");
        check(text(screen, "appt-form-feedback").contains("sau thời điểm"), "Form đổi lịch chặn giờ quá khứ");
        field(screen, "appt-form-date").setText("13/10/2026"); field(screen, "appt-form-time").setText("10:00"); click(screen, "appt-form-save");
        AppointmentsPanel panel = (AppointmentsPanel) screen;
        check(shell.store().appointment("LH000006").orElseThrow().status() == AppointmentStatus.DA_DOI_LICH && panel.viewDate().equals(LocalDate.of(2026, 10, 13))
                && t.selected().map(o -> ((Appointment) o).originId()).orElse("").equals("LH000006"), "Đổi lịch qua form: chuyển tới lịch mới và giữ liên kết");
        click(screen, "appt-open-linked");
        check(panel.viewDate().equals(LocalDate.of(2026, 10, 12)) && t.selected().map(o -> ((Appointment) o).id()).orElse("").equals("LH000006"),
                "Mở lịch liên kết quay về lịch gốc");
        click(screen, "appt-new"); field(screen, "appt-form-reason").setText(""); click(screen, "appt-form-save");
        check(text(screen, "appt-form-feedback").contains("Lý do hẹn"), "Form tạo lịch báo thiếu lý do");
        field(screen, "appt-form-reason").setText("Khám định kỳ"); click(screen, "appt-form-save");
        check(text(screen, "appt-feedback").contains("chờ xác nhận"), "Tạo lịch mới qua form");
        click(screen, "appt-next"); click(screen, "appt-next"); click(screen, "appt-next"); click(screen, "appt-next");
        check(table(screen, "appt-table").isEmptyState() && text(screen, "appt-total").equals("0"), "Ngày không có lịch hiển thị trạng thái rỗng");
        click(screen, "appt-today");
        check(panel.viewDate().equals(LocalDate.of(2026, 10, 12)), "Nút Hôm nay theo Clock mô phỏng");
        dialogs.answers.add(false); click(screen, "appt-reset");
        check(shell.store().appointmentsOn(LocalDate.of(2026, 10, 12)).stream().anyMatch(a -> a.id().equals("LH000006") && a.status() == AppointmentStatus.DA_DOI_LICH),
                "Từ chối khôi phục giữ thay đổi phiên");
        click(screen, "appt-reset");
        check(text(screen, "appt-total").equals("10") && shell.store().appointment("LH000012").isEmpty(), "Nút Khôi phục dữ liệu mẫu nạp lại seed");
        closeForms(screen); t.select("LH000009");
        snapshot(shell, 1382, 840, "demo1-lich-hen-1382x840.png");
        snapshot(shell, 1280, 760, "demo1-lich-hen-1280x760.png");
        shell.close();
    }
    private static void patients() throws Exception {
        Scripted dialogs = new Scripted(); AdminShell shell = shell(dialogs); shell.navigate("BENH_NHAN");
        JComponent screen = shell.screen("BENH_NHAN");
        RowView<?> t = table(screen, "patients-table");
        check(t.rows().size() == 12 && text(screen, "patient-title").contains("Chưa chọn"), "Danh sách 12 hồ sơ, chưa chọn");
        click(screen, "patients-filter-2");
        check(t.rows().size() == 2, "Lọc hồ sơ nghi trùng");
        click(screen, "patients-filter-1");
        check(t.rows().size() == 10, "Lọc bệnh nhân có lịch hôm nay");
        click(screen, "patients-clear"); t.select("BN000009");
        check(text(screen, "patient-phone").contains("090****109") && texts(screen).stream().anyMatch(s -> s.contains("Người giám hộ mẫu")), "SĐT che mặc định, có người liên hệ");
        ((JToggleButton) find(screen, "patient-reveal")).doClick(0);
        check(text(screen, "patient-phone").contains("0900000109"), "Hiện SĐT theo thao tác chủ động");
        ((JToggleButton) find(screen, "patient-reveal")).doClick(0);
        check(table(screen, "patient-history").rows().size() == 2, "Lịch sử hẹn gồm lịch gốc và lịch mới đổi");
        check(texts(screen).stream().anyMatch(s -> s.contains("chỉ đọc") && s.contains("không ghi chẩn đoán")), "Hồ sơ chuyên môn chỉ đọc");
        click(screen, "patient-edit"); field(screen, "edit-phone").setText("12"); click(screen, "edit-save");
        check(text(screen, "patients-feedback").contains("10 chữ số"), "Sửa hành chính validate SĐT");
        field(screen, "edit-phone").setText("0900000999"); field(screen, "edit-address").setText("Địa chỉ mẫu mới"); click(screen, "edit-save");
        check(shell.store().patient("BN000009").orElseThrow().phone().equals("0900000999") && text(screen, "patients-feedback").contains(DemoClinicStore.SAVED),
                "Sửa hành chính mô phỏng có xác nhận");
        check(texts(find(screen, "patient-activity")).stream().anyMatch(s -> s.contains("Cập nhật thông tin hành chính")), "Tab Hoạt động ghi thao tác");
        field(screen, "patients-search").setText("khong co");
        check(t.isEmptyState(), "State rỗng khi tìm không thấy");
        click(screen, "patients-clear"); t.select("BN000007");
        snapshot(shell, 1382, 840, "demo1-ho-so-1382x840.png");
        snapshot(shell, 1280, 760, "demo1-ho-so-1280x760.png");
        shell.close();
    }
    private static void crossScreen() throws Exception {
        AdminShell shell = shell(new Scripted());
        shell.navigate("TIEP_NHAN"); JComponent r = shell.screen("TIEP_NHAN");
        field(r, "reception-search").setText("BN000006"); table(r, "reception-results").select("BN000006"); click(r, "reception-checkin");
        shell.navigate("LICH_HEN"); JComponent a = shell.screen("LICH_HEN");
        check(text(a, "appt-stat-DA_DEN").equals("2") && text(a, "appt-stat-DA_XAC_NHAN").equals("1"), "Check-in ở Tiếp nhận phản ánh sang Lịch hẹn");
        shell.navigate("BENH_NHAN"); JComponent p = shell.screen("BENH_NHAN"); table(p, "patients-table").select("BN000006");
        check(table(p, "patient-visits").rows().size() == 1, "Phiếu mới hiện trong Hồ sơ bệnh nhân");
        check(DemoDashboardDataProvider.sample(FIXED, Scenario.FULL).kpis().appointmentCount() == 10, "Tổng quan vẫn là fixture độc lập, không bị thao tác demo thay đổi");
        shell.close();
    }

    // ------------------------------------------------------------------ helpers
    private static void closeForms(Component root) {
        all(root).stream().filter(c -> c instanceof JButton b && "Đóng".equals(b.getText())).forEach(c -> ((JButton) c).doClick(0));
    }
    private static <T> T edt(Callable<T> task) throws Exception {
        FutureTask<T> f = new FutureTask<>(task); SwingUtilities.invokeLater(f); return f.get(60, TimeUnit.SECONDS);
    }
    private static List<Component> all(Component root) {
        List<Component> out = new ArrayList<>(); out.add(root);
        if (root instanceof Container c) for (Component child : c.getComponents()) out.addAll(all(child));
        return out;
    }
    private static Component find(Component root, String name) {
        return all(root).stream().filter(c -> name.equals(c.getName())).findFirst().orElseThrow(() -> new AssertionError("Thiếu component " + name));
    }
    private static boolean visible(Component root, String name) {
        Component c = find(root, name);
        for (Component p = c; p != null && p != root; p = p.getParent()) if (!p.isVisible()) return false;
        return true;
    }
    private static String text(Component root, String name) { return ((JLabel) find(root, name)).getText(); }
    private static JTextField field(Component root, String name) { return (JTextField) find(root, name); }
    private static RowView<?> table(Component root, String name) { return (RowView<?>) find(root, name); }
    private static void click(Component root, String name) { ((AbstractButton) find(root, name)).doClick(0); }
    private static List<String> texts(Component root) {
        return all(root).stream().filter(JLabel.class::isInstance).map(c -> ((JLabel) c).getText()).filter(java.util.Objects::nonNull).toList();
    }
    private static void layout(Component component) {
        if (component instanceof Container c) { c.doLayout(); for (Component child : c.getComponents()) layout(child); }
    }
    private static void snapshot(JComponent component, int width, int height, String name) throws Exception {
        component.setSize(width, height);
        for (int pass = 0; pass < 4; pass++) { for (Component child : all(component)) child.invalidate(); layout(component); }
        BufferedImage output = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = output.createGraphics(); component.printAll(g); g.dispose();
        Path dir = Path.of("build/screenshots"); Files.createDirectories(dir); ImageIO.write(output, "png", dir.resolve(name).toFile());
        check(Files.size(dir.resolve(name)) > 10_000, "Ảnh Swing thật: " + name);
    }
}
