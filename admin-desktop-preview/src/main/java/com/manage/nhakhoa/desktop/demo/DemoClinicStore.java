package com.manage.nhakhoa.desktop.demo;

import com.manage.nhakhoa.desktop.MenuDefinition;
import com.manage.nhakhoa.desktop.model.DashboardViewModel.AppointmentStatus;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;

/**
 * Kho dữ liệu MÔ PHỎNG trong bộ nhớ của một phiên preview.
 * Không JDBC, không HTTP, không Entity JPA; mọi bản ghi là dữ liệu tổng hợp (synthetic), không phải người thật.
 * Seed được tạo deterministic theo ngày của Clock nên --fixture luôn khớp Tổng quan 12/10/2026 07:45.
 */
public final class DemoClinicStore {
    public static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    public static final String ACTOR = "AD-DEMO";
    public static final String SAVED = "Đã cập nhật dữ liệu mô phỏng — không lưu vào hệ thống thật";
    public static final LocalTime OPEN = LocalTime.of(7, 0), CLOSE = LocalTime.of(20, 0);
    private static final Pattern PHONE = Pattern.compile("0\\d{9}");

    public enum Gender {
        NAM("Nam"), NU("Nữ"), KHAC("Khác");
        private final String label;
        Gender(String label) { this.label = label; }
        public String label() { return label; }
        @Override public String toString() { return label; }
    }
    public enum TicketStatus {
        CHO_KHAM("Chờ khám"), DANG_KHAM("Đang khám"), DA_KET_LUAN("Đã kết luận"), DA_HUY("Đã hủy");
        private final String label;
        TicketStatus(String label) { this.label = label; }
        public String label() { return label; }
    }
    public enum Code { OK, INVALID, DUPLICATE, RULE, NOT_FOUND }

    public record Doctor(String id, String name, boolean onDuty) {
        public String display() { return id + " · " + name + (onDuty ? "" : " (không trực hôm nay)"); }
    }
    public record Patient(String id, String name, LocalDate birthDate, Gender gender, String phone,
            String address, String contact, LocalDateTime createdAt) { }
    public record Appointment(String id, String patientId, String doctorId, LocalDateTime time, String reason,
            AppointmentStatus status, String originId, String replacementId, String cancelReason) {
        Appointment with(AppointmentStatus s, String replacement, String cancel) {
            return new Appointment(id, patientId, doctorId, time, reason, s, originId, replacement, cancel);
        }
    }
    public record ExamTicket(String id, String patientId, String doctorId, String appointmentId,
            LocalDateTime receivedAt, TicketStatus status, String visitReason) { }
    /** Tóm tắt hồ sơ bệnh án giả lập, chỉ đọc; ADMIN không ghi chẩn đoán. */
    public record MedicalSummary(String patientId, String recordId, String allergy, String note, String lastConclusion) { }
    /** Nhật ký kỹ thuật tối thiểu: actor/action/object/outcome, không chứa dữ liệu sức khỏe. */
    public record Activity(LocalDateTime time, String actor, String action, String objectType,
            String objectId, String outcome, String patientId) { }
    public record PatientDraft(String name, LocalDate birthDate, Gender gender, String phone, String address) { }
    public record Result<T>(Code code, String message, T value) {
        public boolean ok() { return code == Code.OK; }
        static <T> Result<T> success(String message, T value) { return new Result<>(Code.OK, SAVED + " · " + message, value); }
        static <T> Result<T> fail(Code code, String message) { return new Result<>(code, message, null); }
    }

    private final Clock clock;
    private final Map<String, Doctor> doctors = new LinkedHashMap<>();
    private final Map<String, Patient> patients = new LinkedHashMap<>();
    private final Map<String, Appointment> appointments = new LinkedHashMap<>();
    private final Map<String, ExamTicket> tickets = new LinkedHashMap<>();
    private final Map<String, MedicalSummary> medical = new LinkedHashMap<>();
    private final List<Activity> activities = new ArrayList<>();
    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();
    private int nextPatient, nextAppointment, nextTicket;
    private StaffSlice staff;
    private ClinicalSlice clinical;
    private BillingSlice billing;
    private SupplySlice supply;
    public StaffSlice staff() { return staff; }
    public ClinicalSlice clinical() { return clinical; }
    public BillingSlice billing() { return billing; }
    public SupplySlice supply() { return supply; }
    /** Cập nhật trạng thái phiếu khám từ slice lâm sàng. */
    void putTicket(ExamTicket t) { tickets.put(t.id(), t); }
    public Optional<ExamTicket> ticket(String id) { return Optional.ofNullable(id == null ? null : tickets.get(id)); }

    public DemoClinicStore(Clock clock) { this.clock = Objects.requireNonNull(clock); seed(); }

    // ---------------------------------------------------------------- seed
    /** Khôi phục bộ mẫu ban đầu; mọi thay đổi mô phỏng của phiên bị bỏ. */
    public void reset() { seed(); fire(); }

    private void seed() {
        doctors.clear(); patients.clear(); appointments.clear(); tickets.clear(); medical.clear(); activities.clear();
        LocalDate d = today();
        doctor("NV002", "BS. Trần Minh Khoa", true);
        doctor("NV003", "BS. Lê Thu Hà", true);
        doctor("NV004", "BS. Phạm Quốc Bảo", false);
        String addr = "Địa chỉ mẫu, TP. Giả Lập";
        patient("BN000001", "Nguyễn Thị Mai", "1988-05-12", Gender.NU, "0900000101", addr, "");
        patient("BN000002", "Trần Văn Hùng", "1975-11-03", Gender.NAM, "0900000102", addr, "");
        patient("BN000003", "Lê Hoàng Nam", "2001-02-20", Gender.NAM, "0900000103", addr, "");
        patient("BN000004", "Phạm Thu Trang", "1995-07-08", Gender.NU, "0900000104", addr, "");
        patient("BN000005", "Võ Minh Tuấn", "1983-09-30", Gender.NAM, "0900000105", addr, "");
        patient("BN000006", "Đặng Ngọc Lan", "1999-12-01", Gender.NU, "0900000106", addr, "");
        patient("BN000007", "Nguyễn Văn An", "1990-03-15", Gender.NAM, "0900000107", addr, "");
        patient("BN000008", "Bùi Thanh Tâm", "1968-04-22", Gender.NU, "0900000108", addr, "");
        patient("BN000009", "Hồ Gia Bảo", "2012-06-18", Gender.NAM, "0900000109", addr, "Người giám hộ mẫu (phụ huynh)");
        patient("BN000010", "Đỗ Khánh Linh", "1992-08-25", Gender.NU, "0900000110", addr, "");
        // Cặp nghi trùng có sẵn (cùng họ tên + ngày sinh), hệ thống KHÔNG tự gộp.
        patient("BN000011", "Nguyễn Văn An", "1990-03-15", Gender.NAM, "0900000111", addr, "");
        patient("BN000012", "Phan Quỳnh Như", "1997-01-09", Gender.NU, "0900000112", addr, "");
        // 10 lịch trong ngày = 2 chờ xác nhận + 2 đã xác nhận + 1 đã đến + 2 hoàn thành + 1 vắng + 1 hủy + 1 đổi lịch.
        appt("LH000001", "BN000001", "NV002", d.atTime(7, 0), "Lấy cao răng", AppointmentStatus.HOAN_THANH, null, null, null);
        appt("LH000002", "BN000002", "NV003", d.atTime(7, 0), "Tái khám nha chu", AppointmentStatus.VANG_MAT, null, null, null);
        appt("LH000003", "BN000003", "NV003", d.atTime(7, 15), "Khám định kỳ", AppointmentStatus.HOAN_THANH, null, null, null);
        appt("LH000004", "BN000004", "NV002", d.atTime(7, 30), "Đau răng hàm", AppointmentStatus.DA_DEN, null, null, null);
        appt("LH000005", "BN000005", "NV002", d.atTime(8, 0), "Trám răng", AppointmentStatus.DA_XAC_NHAN, null, null, null);
        appt("LH000006", "BN000006", "NV003", d.atTime(8, 30), "Tư vấn tẩy trắng", AppointmentStatus.DA_XAC_NHAN, null, null, null);
        appt("LH000007", "BN000007", "NV003", d.atTime(9, 0), "Khám tổng quát", AppointmentStatus.DA_HUY, null, null, "Bệnh nhân báo bận (giả lập)");
        appt("LH000008", "BN000008", "NV002", d.atTime(9, 30), "Tư vấn nhổ răng khôn", AppointmentStatus.CHO_XAC_NHAN, null, null, null);
        appt("LH000009", "BN000009", "NV002", d.atTime(10, 0), "Chỉnh nha định kỳ", AppointmentStatus.DA_DOI_LICH, null, "LH000011", null);
        appt("LH000010", "BN000010", "NV003", d.atTime(14, 0), "Tái khám chỉnh nha", AppointmentStatus.CHO_XAC_NHAN, null, null, null);
        appt("LH000011", "BN000009", "NV002", d.plusDays(2).atTime(9, 0), "Chỉnh nha định kỳ", AppointmentStatus.DA_XAC_NHAN, "LH000009", null, null);
        // Phiếu khám trong ngày: 0 chờ / 1 đang / 2 đã kết luận (khớp Tổng quan).
        ticket("PK000001", "BN000001", "NV002", "LH000001", d.atTime(7, 2), TicketStatus.DA_KET_LUAN, "Lấy cao răng");
        ticket("PK000002", "BN000003", "NV003", "LH000003", d.atTime(7, 16), TicketStatus.DA_KET_LUAN, "Khám định kỳ");
        ticket("PK000003", "BN000004", "NV002", "LH000004", d.atTime(7, 31), TicketStatus.DANG_KHAM, "Đau răng hàm");
        int i = 1;
        for (Patient p : patients.values()) {
            medical.put(p.id(), new MedicalSummary(p.id(), String.format("HSBA%05d", i++),
                    "Chưa ghi nhận (dữ liệu mẫu)", "Thông tin chuyên môn mẫu — chỉ đọc",
                    tickets.values().stream().anyMatch(t -> t.patientId().equals(p.id()) && t.status() == TicketStatus.DA_KET_LUAN)
                            ? "Đã có kết luận mẫu do bác sĩ phụ trách ghi" : "Chưa có kết luận"));
        }
        LocalDateTime t0 = d.atTime(6, 55);
        log(t0, "Xác nhận lịch hẹn", "LichHen", "LH000005", "Thành công", "BN000005");
        log(t0.plusMinutes(5), "Check-in theo lịch", "PhieuKham", "PK000001", "Thành công", "BN000001");
        log(t0.plusMinutes(36), "Check-in theo lịch", "PhieuKham", "PK000003", "Thành công", "BN000004");
        nextPatient = 13; nextAppointment = 12; nextTicket = 4;
        // Các slice nghiệp vụ dùng chung mã BN/LH/PK/NV ở trên; seed theo cùng ngày mô phỏng.
        staff = new StaffSlice(this); staff.seed(d);
        clinical = new ClinicalSlice(this); clinical.seed(d);
        billing = new BillingSlice(this); billing.seed(d);
        supply = new SupplySlice(this); supply.seed(d);
        // Lịch sử thao tác mẫu các ngày trước (không gắn bệnh nhân) để màn Nhật ký có dữ liệu lọc.
        logAs(d.minusDays(7).atTime(7, 45), "NV005 (mô phỏng)", "Ghi nhận kiểm tra hộp cấp cứu", "KiemTraCapCuu", "KT0001", "Thành công", null);
        logAs(d.minusDays(3).atTime(9, 10), "NV011 (mô phỏng)", "Lập phiếu nhập kho", "PhieuNhap", "PN0001", "Thành công", null);
        logAs(d.minusDays(2).atTime(16, 40), ACTOR, "Khóa tài khoản sau 5 lần đăng nhập sai", "TaiKhoan", "TK009", "Thành công", null);
        logAs(d.minusDays(1).atTime(17, 2), "NV007 (mô phỏng)", "Lập biên bản bàn giao", "BanGiao", "BG0002", "Thành công", null);
        logAs(d.minusDays(1).atTime(18, 0), "NV011 (mô phỏng)", "Lập phiếu điều chỉnh kho", "DieuChinhKho", "DC0001", "Thành công", null);
        logAs(d.minusDays(1).atTime(20, 10), "NV008 (mô phỏng)", "Lập biên bản bàn giao", "BanGiao", "BG0003", "Thành công", null);
        logAs(d.minusDays(1).atTime(20, 30), "NV008 (mô phỏng)", "Tạo yêu cầu hủy hóa đơn", "YeuCauHuy", "YC0001", "Thành công", null);
        logAs(d.minusDays(1).atTime(21, 5), "AD-DEMO", "Đăng nhập", "TaiKhoan", "TK001", "Thất bại", null);
    }
    private void doctor(String id, String name, boolean duty) { doctors.put(id, new Doctor(id, name, duty)); }
    private void patient(String id, String name, String dob, Gender g, String phone, String addr, String contact) {
        patients.put(id, new Patient(id, name, LocalDate.parse(dob), g, phone, addr, contact, today().minusDays(30).atTime(8, 0)));
    }
    private void appt(String id, String p, String doc, LocalDateTime time, String reason, AppointmentStatus s,
            String origin, String replacement, String cancel) {
        appointments.put(id, new Appointment(id, p, doc, time, reason, s, origin, replacement, cancel));
    }
    private void ticket(String id, String p, String doc, String appt, LocalDateTime at, TicketStatus s, String reason) {
        tickets.put(id, new ExamTicket(id, p, doc, appt, at, s, reason));
    }
    private void log(LocalDateTime time, String action, String type, String objectId, String outcome, String patientId) {
        activities.add(new Activity(time, ACTOR, action, type, objectId, outcome, patientId));
    }
    /** Ghi nhật ký kỹ thuật cho các slice (actor có thể là nhân viên mô phỏng). Không ghi dữ liệu chuyên môn. */
    void logAs(LocalDateTime time, String actor, String action, String type, String objectId, String outcome, String patientId) {
        activities.add(new Activity(time, actor, action, type, objectId, outcome, patientId));
    }
    void logAs(String actor, String action, String type, String objectId, String outcome, String patientId) {
        logAs(now(), actor, action, type, objectId, outcome, patientId);
    }
    private void log(String action, String type, String objectId, String outcome, String patientId) {
        log(now(), action, type, objectId, outcome, patientId);
    }

    // ---------------------------------------------------------------- listeners & time
    public void addListener(Runnable listener) { listeners.add(listener); }
    public void removeListener(Runnable listener) { listeners.remove(listener); }
    void fire() { listeners.forEach(Runnable::run); }
    public LocalDateTime now() { return LocalDateTime.now(clock.withZone(ZONE)).withSecond(0).withNano(0); }
    public LocalDate today() { return LocalDate.now(clock.withZone(ZONE)); }

    // ---------------------------------------------------------------- queries
    public List<Doctor> doctors() { return List.copyOf(doctors.values()); }
    public List<Doctor> onDutyDoctors() { return doctors.values().stream().filter(Doctor::onDuty).toList(); }
    public Optional<Doctor> doctor(String id) { return Optional.ofNullable(id == null ? null : doctors.get(id)); }
    public String doctorName(String id) { return doctor(id).map(Doctor::name).orElse("—"); }
    public List<Patient> patients() { return List.copyOf(patients.values()); }
    public Optional<Patient> patient(String id) { return Optional.ofNullable(id == null ? null : patients.get(id)); }
    public String patientName(String id) { return patient(id).map(Patient::name).orElse("—"); }
    public Optional<Appointment> appointment(String id) { return Optional.ofNullable(id == null ? null : appointments.get(id)); }
    public List<Appointment> appointments() { return List.copyOf(appointments.values()); }
    public List<Appointment> appointmentsOn(LocalDate date) {
        return appointments.values().stream().filter(a -> a.time().toLocalDate().equals(date))
                .sorted(Comparator.comparing(Appointment::time).thenComparing(Appointment::id)).toList();
    }
    public List<Appointment> appointmentsOf(String patientId) {
        return appointments.values().stream().filter(a -> a.patientId().equals(patientId))
                .sorted(Comparator.comparing(Appointment::time).reversed()).toList();
    }
    public List<ExamTicket> tickets() { return List.copyOf(tickets.values()); }
    public List<ExamTicket> ticketsOf(String patientId) {
        return tickets.values().stream().filter(t -> t.patientId().equals(patientId))
                .sorted(Comparator.comparing(ExamTicket::receivedAt).reversed()).toList();
    }
    public List<ExamTicket> ticketsOn(LocalDate date) {
        return tickets.values().stream().filter(t -> t.receivedAt().toLocalDate().equals(date)).toList();
    }
    public Optional<ExamTicket> ticketForAppointment(String appointmentId) {
        return tickets.values().stream().filter(t -> appointmentId.equals(t.appointmentId())).findFirst();
    }
    public Optional<MedicalSummary> medical(String patientId) { return Optional.ofNullable(medical.get(patientId)); }
    public List<Activity> activities() {
        return activities.stream().sorted(Comparator.comparing(Activity::time).reversed()).toList();
    }
    public List<Activity> activitiesOf(String patientId) {
        return activities().stream().filter(a -> patientId.equals(a.patientId())).toList();
    }
    public Map<AppointmentStatus, Long> statusCounts(LocalDate date) {
        EnumMap<AppointmentStatus, Long> counts = new EnumMap<>(AppointmentStatus.class);
        for (AppointmentStatus s : AppointmentStatus.values()) counts.put(s, 0L);
        appointmentsOn(date).forEach(a -> counts.merge(a.status(), 1L, Long::sum));
        return counts;
    }
    /** Lịch còn hiệu lực = tổng trừ đã hủy và đã đổi lịch (BA Q05); không phải số lượt khách phục vụ. */
    public long activeCount(LocalDate date) {
        return appointmentsOn(date).stream()
                .filter(a -> a.status() != AppointmentStatus.DA_HUY && a.status() != AppointmentStatus.DA_DOI_LICH).count();
    }
    public List<Patient> searchPatients(String query) {
        String q = query == null ? "" : query.trim();
        if (q.isEmpty()) return patients();
        String norm = MenuDefinition.normalize(q), digits = q.replaceAll("\\D", "");
        return patients.values().stream().filter(p -> MenuDefinition.normalize(p.name() + " " + p.id()).contains(norm)
                || (digits.length() >= 3 && p.phone().contains(digits))).toList();
    }
    /** Nghi trùng: cùng SĐT, hoặc cùng họ tên (không dấu) + ngày sinh. Không tự gộp. */
    public List<Patient> duplicatesOf(String excludeId, String name, LocalDate birthDate, String phone) {
        String n = name == null ? "" : MenuDefinition.normalize(name);
        return patients.values().stream().filter(p -> !p.id().equals(excludeId))
                .filter(p -> (phone != null && !phone.isBlank() && p.phone().equals(phone.trim()))
                        || (!n.isEmpty() && MenuDefinition.normalize(p.name()).equals(n) && p.birthDate().equals(birthDate)))
                .toList();
    }
    public List<Patient> suspectedDuplicates(String patientId) {
        return patient(patientId).map(p -> duplicatesOf(p.id(), p.name(), p.birthDate(), p.phone())).orElse(List.of());
    }
    public static String maskPhone(String phone) {
        return phone == null || phone.length() < 6 ? "—" : phone.substring(0, 3) + "****" + phone.substring(phone.length() - 3);
    }

    // ---------------------------------------------------------------- patient mutations
    public Result<Patient> addPatient(PatientDraft draft, boolean acknowledgeDuplicate) {
        List<String> errors = new ArrayList<>();
        String name = draft.name() == null ? "" : draft.name().trim().replaceAll("\\s+", " ");
        if (name.length() < 2 || name.length() > 100) errors.add("Họ tên phải từ 2 đến 100 ký tự");
        else if (!name.matches("[\\p{L} .'-]+")) errors.add("Họ tên chỉ gồm chữ cái và khoảng trắng");
        if (draft.birthDate() == null) errors.add("Ngày sinh không hợp lệ (dd/MM/yyyy)");
        else if (draft.birthDate().isAfter(today())) errors.add("Ngày sinh không được sau ngày hiện tại");
        else if (draft.birthDate().isBefore(today().minusYears(120))) errors.add("Ngày sinh quá xa, kiểm tra lại");
        String phone = draft.phone() == null ? "" : draft.phone().replaceAll("[\\s.]", "");
        if (!PHONE.matcher(phone).matches()) errors.add("Số điện thoại phải gồm 10 chữ số, bắt đầu bằng 0");
        if (draft.gender() == null) errors.add("Chọn giới tính");
        String address = draft.address() == null ? "" : draft.address().trim();
        if (address.length() > 200) errors.add("Địa chỉ tối đa 200 ký tự");
        if (!errors.isEmpty()) return Result.fail(Code.INVALID, String.join(" · ", errors));
        List<Patient> dup = duplicatesOf(null, name, draft.birthDate(), phone);
        if (!dup.isEmpty() && !acknowledgeDuplicate) {
            return new Result<>(Code.DUPLICATE, "Nghi trùng với " + String.join(", ", dup.stream().map(Patient::id).toList())
                    + " · hệ thống không tự gộp hồ sơ", null);
        }
        String id = String.format("BN%06d", nextPatient++);
        Patient p = new Patient(id, name, draft.birthDate(), draft.gender(), phone, address, "", now());
        patients.put(id, p);
        medical.put(id, new MedicalSummary(id, String.format("HSBA%05d", patients.size()), "Chưa ghi nhận (dữ liệu mẫu)",
                "Thông tin chuyên môn mẫu — chỉ đọc", "Chưa có kết luận"));
        log("Thêm bệnh nhân" + (dup.isEmpty() ? "" : " (đã cảnh báo nghi trùng)"), "BenhNhan", id, "Thành công", id);
        fire();
        return Result.success("Đã thêm " + id, p);
    }
    public Result<Patient> updatePatientAdmin(String id, String phone, String address, String contact, boolean acknowledgeDuplicate) {
        Patient p = patients.get(id);
        if (p == null) return Result.fail(Code.NOT_FOUND, "Không tìm thấy bệnh nhân");
        String ph = phone == null ? "" : phone.replaceAll("[\\s.]", "");
        List<String> errors = new ArrayList<>();
        if (!PHONE.matcher(ph).matches()) errors.add("Số điện thoại phải gồm 10 chữ số, bắt đầu bằng 0");
        if (address != null && address.trim().length() > 200) errors.add("Địa chỉ tối đa 200 ký tự");
        if (contact != null && contact.trim().length() > 120) errors.add("Người liên hệ tối đa 120 ký tự");
        if (!errors.isEmpty()) return Result.fail(Code.INVALID, String.join(" · ", errors));
        List<Patient> dup = patients.values().stream().filter(o -> !o.id().equals(id) && o.phone().equals(ph)).toList();
        if (!dup.isEmpty() && !acknowledgeDuplicate) {
            return new Result<>(Code.DUPLICATE, "SĐT đang dùng ở " + String.join(", ", dup.stream().map(Patient::id).toList()), null);
        }
        Patient updated = new Patient(id, p.name(), p.birthDate(), p.gender(), ph,
                address == null ? "" : address.trim(), contact == null ? "" : contact.trim(), p.createdAt());
        if (updated.equals(p)) return Result.fail(Code.RULE, "Không có thay đổi để cập nhật");
        patients.put(id, updated);
        log("Cập nhật thông tin hành chính", "BenhNhan", id, "Thành công", id);
        fire();
        return Result.success("Đã cập nhật hành chính " + id, updated);
    }

    // ---------------------------------------------------------------- appointment mutations
    public Result<Appointment> createAppointment(String patientId, String doctorId, LocalDateTime time, String reason) {
        String err = validateSlot(null, patientId, doctorId, time);
        String r = reason == null ? "" : reason.trim();
        if (err == null && (r.length() < 3 || r.length() > 200)) err = "Lý do hẹn phải từ 3 đến 200 ký tự";
        if (err != null) return Result.fail(Code.INVALID, err);
        String id = String.format("LH%06d", nextAppointment++);
        Appointment a = new Appointment(id, patientId, doctorId, time, r, AppointmentStatus.CHO_XAC_NHAN, null, null, null);
        appointments.put(id, a);
        log("Tạo lịch hẹn", "LichHen", id, "Thành công", patientId);
        fire();
        return Result.success("Đã tạo " + id + " (chờ xác nhận)", a);
    }
    private String validateSlot(String ignoreId, String patientId, String doctorId, LocalDateTime time) {
        if (patient(patientId).isEmpty()) return "Chọn bệnh nhân";
        if (doctor(doctorId).isEmpty()) return "Chọn bác sĩ";
        if (time == null) return "Ngày giờ hẹn không hợp lệ (dd/MM/yyyy và HH:mm)";
        if (!time.isAfter(now())) return "Giờ hẹn phải sau thời điểm hiện tại (" + now().toLocalTime() + ")";
        if (time.toLocalTime().isBefore(OPEN) || !time.toLocalTime().isBefore(CLOSE)) return "Giờ hẹn phải trong khung 07:00–20:00";
        if (time.getMinute() % 15 != 0) return "Giờ hẹn theo bước 15 phút (:00, :15, :30, :45)";
        boolean busy = appointments.values().stream().filter(a -> !a.id().equals(ignoreId)).filter(DemoClinicStore::occupiesSlot)
                .anyMatch(a -> a.time().equals(time) && (a.doctorId().equals(doctorId) || a.patientId().equals(patientId)));
        if (busy) return "Khung giờ đã có lịch của bác sĩ hoặc bệnh nhân này";
        return null;
    }
    private static boolean occupiesSlot(Appointment a) {
        return a.status() == AppointmentStatus.CHO_XAC_NHAN || a.status() == AppointmentStatus.DA_XAC_NHAN
                || a.status() == AppointmentStatus.DA_DEN;
    }
    public static boolean canConfirm(Appointment a) { return a.status() == AppointmentStatus.CHO_XAC_NHAN; }
    public static boolean canChange(Appointment a) {
        return a.status() == AppointmentStatus.CHO_XAC_NHAN || a.status() == AppointmentStatus.DA_XAC_NHAN;
    }
    public Result<Appointment> confirm(String id) {
        Appointment a = appointments.get(id);
        if (a == null) return Result.fail(Code.NOT_FOUND, "Không tìm thấy lịch hẹn");
        if (!canConfirm(a)) return Result.fail(Code.RULE, "Chỉ xác nhận được lịch đang Chờ xác nhận (hiện: " + a.status().label() + ")");
        Appointment u = a.with(AppointmentStatus.DA_XAC_NHAN, a.replacementId(), null);
        appointments.put(id, u);
        log("Xác nhận lịch hẹn", "LichHen", id, "Thành công", a.patientId());
        fire();
        return Result.success("Đã xác nhận " + id, u);
    }
    public Result<Appointment> cancel(String id, String reason) {
        Appointment a = appointments.get(id);
        if (a == null) return Result.fail(Code.NOT_FOUND, "Không tìm thấy lịch hẹn");
        if (!canChange(a)) return Result.fail(Code.RULE, "Không hủy được lịch ở trạng thái " + a.status().label());
        String r = reason == null ? "" : reason.trim();
        if (r.length() < 5 || r.length() > 200) return Result.fail(Code.INVALID, "Lý do hủy phải từ 5 đến 200 ký tự");
        Appointment u = a.with(AppointmentStatus.DA_HUY, null, r);
        appointments.put(id, u);
        log("Hủy lịch hẹn", "LichHen", id, "Thành công", a.patientId());
        fire();
        return Result.success("Đã hủy " + id, u);
    }
    /** Đổi lịch tạo bản MỚI (chờ xác nhận) và giữ bản cũ ở trạng thái Đã đổi lịch, có liên kết hai chiều. */
    public Result<Appointment> reschedule(String id, String doctorId, LocalDateTime newTime) {
        Appointment a = appointments.get(id);
        if (a == null) return Result.fail(Code.NOT_FOUND, "Không tìm thấy lịch hẹn");
        if (!canChange(a)) return Result.fail(Code.RULE, "Không đổi được lịch ở trạng thái " + a.status().label());
        if (a.time().equals(newTime) && a.doctorId().equals(doctorId)) return Result.fail(Code.INVALID, "Giờ và bác sĩ mới trùng lịch cũ");
        String err = validateSlot(id, a.patientId(), doctorId, newTime);
        if (err != null) return Result.fail(Code.INVALID, err);
        String newId = String.format("LH%06d", nextAppointment++);
        Appointment fresh = new Appointment(newId, a.patientId(), doctorId, newTime, a.reason(),
                AppointmentStatus.CHO_XAC_NHAN, id, null, null);
        appointments.put(id, a.with(AppointmentStatus.DA_DOI_LICH, newId, null));
        appointments.put(newId, fresh);
        log("Đổi lịch hẹn → " + newId, "LichHen", id, "Thành công", a.patientId());
        fire();
        return Result.success("Đã đổi " + id + " → " + newId + " (chờ xác nhận)", fresh);
    }

    // ---------------------------------------------------------------- reception
    /** Check-in chỉ sau DA_XAC_NHAN, đúng ngày hẹn, chống tạo trùng phiếu theo mã lịch (BA Q16). */
    public String checkInBlocker(String appointmentId, String doctorId) {
        Appointment a = appointments.get(appointmentId);
        if (a == null) return "Chọn một lịch hẹn";
        Optional<ExamTicket> existing = ticketForAppointment(appointmentId);
        if (existing.isPresent()) return "Lịch đã check-in · " + existing.get().id();
        if (a.status() == AppointmentStatus.CHO_XAC_NHAN) return "Lịch đang Chờ xác nhận — cần xác nhận ở màn Lịch hẹn trước khi check-in";
        if (a.status() != AppointmentStatus.DA_XAC_NHAN) return "Không check-in được lịch ở trạng thái " + a.status().label();
        if (!a.time().toLocalDate().equals(today())) return "Chỉ check-in lịch của ngày hôm nay";
        Optional<Doctor> d = doctor(doctorId);
        if (d.isEmpty()) return "Chọn bác sĩ tiếp nhận";
        if (!d.get().onDuty()) return "Bác sĩ được chọn không trực hôm nay";
        return null;
    }
    public Result<ExamTicket> checkIn(String appointmentId, String doctorId) {
        String blocker = checkInBlocker(appointmentId, doctorId);
        if (blocker != null) return Result.fail(Code.RULE, blocker);
        Appointment a = appointments.get(appointmentId);
        String id = String.format("PK%06d", nextTicket++);
        ExamTicket t = new ExamTicket(id, a.patientId(), doctorId, appointmentId, now(), TicketStatus.CHO_KHAM, a.reason());
        tickets.put(id, t);
        appointments.put(appointmentId, a.with(AppointmentStatus.DA_DEN, a.replacementId(), null));
        log("Check-in theo lịch " + appointmentId, "PhieuKham", id, "Thành công", a.patientId());
        fire();
        return Result.success("Đã tạo phiếu " + id + " cho " + appointmentId, t);
    }
    public String walkInBlocker(String patientId, String doctorId) {
        if (patient(patientId).isEmpty()) return "Chưa chọn bệnh nhân";
        Optional<Doctor> d = doctor(doctorId);
        if (onDutyDoctors().isEmpty()) return "Không có bác sĩ trực ca này";
        if (d.isEmpty()) return "Chọn bác sĩ tiếp nhận";
        if (!d.get().onDuty()) return "Bác sĩ được chọn không trực hôm nay";
        Optional<ExamTicket> open = ticketsOn(today()).stream().filter(t -> t.patientId().equals(patientId))
                .filter(t -> t.status() == TicketStatus.CHO_KHAM || t.status() == TicketStatus.DANG_KHAM).findFirst();
        if (open.isPresent()) return "Bệnh nhân đang có phiếu " + open.get().id() + " chưa kết thúc";
        return null;
    }
    public Result<ExamTicket> walkIn(String patientId, String doctorId, String visitReason) {
        String blocker = walkInBlocker(patientId, doctorId);
        if (blocker != null) return Result.fail(Code.RULE, blocker);
        String r = visitReason == null ? "" : visitReason.trim();
        if (r.length() < 3 || r.length() > 200) return Result.fail(Code.INVALID, "Lý do đến khám phải từ 3 đến 200 ký tự");
        String id = String.format("PK%06d", nextTicket++);
        ExamTicket t = new ExamTicket(id, patientId, doctorId, null, now(), TicketStatus.CHO_KHAM, r);
        tickets.put(id, t);
        log("Tiếp nhận trực tiếp", "PhieuKham", id, "Thành công", patientId);
        fire();
        return Result.success("Đã tạo phiếu " + id + " (tiếp nhận trực tiếp)", t);
    }
    /** Chỉ dùng cho kịch bản kiểm thử/minh họa trạng thái "không có bác sĩ". */
    public void setOnDuty(String doctorId, boolean onDuty) {
        Doctor d = doctors.get(doctorId);
        if (d != null) { doctors.put(doctorId, new Doctor(d.id(), d.name(), onDuty)); fire(); }
    }
}
