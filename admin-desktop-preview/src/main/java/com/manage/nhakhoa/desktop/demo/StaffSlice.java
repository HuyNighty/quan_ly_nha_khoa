package com.manage.nhakhoa.desktop.demo;

import com.manage.nhakhoa.desktop.demo.DemoClinicStore.Code;
import com.manage.nhakhoa.desktop.demo.DemoClinicStore.Result;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Slice nhân sự MÔ PHỎNG: nhân viên, tài khoản & vai trò (role-only, không Permission/ACL), ca làm việc,
 * phân công, bàn giao ca, đánh giá. Seed khớp Tổng quan: ca chiều và ca tối mỗi ca thiếu 1 y tá,
 * 2 biên bản bàn giao chờ xác nhận, 1 tài khoản bị khóa.
 */
public final class StaffSlice {
    public static final String ADMIN_ID = "NV001";
    private static final Pattern PHONE = Pattern.compile("0\\d{9}");

    public enum Role {
        AD("Quản trị"), LT("Lễ tân"), BS("Bác sĩ"), YT("Y tá"), QL("Quản lý"), KHO("Thủ kho");
        private final String label;
        Role(String label) { this.label = label; }
        public String label() { return label; }
    }
    public enum WorkStatus { DANG_LAM("Đang làm việc"), NGHI_VIEC("Đã nghỉ việc");
        private final String label; WorkStatus(String l) { label = l; } public String label() { return label; } }
    public enum AccountStatus { HOAT_DONG("Hoạt động"), KHOA("Bị khóa"), VO_HIEU("Vô hiệu hóa");
        private final String label; AccountStatus(String l) { label = l; } public String label() { return label; } }
    public enum ShiftKind {
        SANG("Ca sáng", "07:00–12:00"), CHIEU("Ca chiều", "13:00–17:00"), TOI("Ca tối", "17:00–20:00");
        private final String label, hours;
        ShiftKind(String label, String hours) { this.label = label; this.hours = hours; }
        public String label() { return label; }
        public String hours() { return hours; }
        @Override public String toString() { return label + " (" + hours + ")"; }
    }
    public enum HandoverStatus { CHO_XAC_NHAN("Chờ xác nhận"), DA_XAC_NHAN("Đã xác nhận"), DA_HUY("Đã hủy");
        private final String label; HandoverStatus(String l) { label = l; } public String label() { return label; } }
    public enum EvalStatus { NHAP("Bản nháp"), DA_GUI("Đã gửi");
        private final String label; EvalStatus(String l) { label = l; } public String label() { return label; } }

    public record Staff(String id, String name, Role role, String title, String phone, String address,
            LocalDate startDate, WorkStatus status) { }
    public record Account(String id, String username, String staffId, Role role, AccountStatus status, LocalDateTime lastLogin) { }
    public record Assignment(String id, LocalDate date, ShiftKind shift, String staffId, String task) { }
    public record Handover(String id, LocalDate date, ShiftKind fromShift, String giverId, String receiverId, LocalDateTime time,
            String note, boolean discrepancy, HandoverStatus status, String creatorId, String confirmedBy) { }
    public record Evaluation(String id, String staffId, String period, String evaluatorId, List<Integer> scores,
            String comment, EvalStatus status) {
        public double average() { return scores.stream().mapToInt(Integer::intValue).average().orElse(0); }
    }
    /** 5 tiêu chí MẪU, không phải tiêu chuẩn nhân sự chính thức. */
    public static final List<String> CRITERIA = List.of("Chuyên môn", "Thái độ phục vụ", "Tuân thủ quy trình", "Phối hợp nhóm", "Chuyên cần");
    /** Định mức theo ca (giả định demo) cho BS / YT / LT. */
    public static Map<Role, Integer> quota(ShiftKind shift) {
        Map<Role, Integer> q = new EnumMap<>(Role.class);
        q.put(Role.BS, shift == ShiftKind.SANG ? 2 : 1); q.put(Role.YT, shift == ShiftKind.TOI ? 1 : 2); q.put(Role.LT, 1);
        return q;
    }

    private final DemoClinicStore store;
    private final Map<String, Staff> staff = new LinkedHashMap<>();
    private final Map<String, Account> accounts = new LinkedHashMap<>();
    private final List<Assignment> assignments = new ArrayList<>();
    private final Map<String, Handover> handovers = new LinkedHashMap<>();
    private final Map<String, Evaluation> evaluations = new LinkedHashMap<>();
    private int nextAssignment = 1, nextHandover = 1, nextEval = 1;

    StaffSlice(DemoClinicStore store) { this.store = store; }

    void seed(LocalDate d) {
        String addr = "Địa chỉ mẫu, TP. Giả Lập";
        s("NV001", "Ngô Quản Trị", Role.AD, "Quản trị hệ thống", "0911000001", d.minusYears(3), WorkStatus.DANG_LAM, addr);
        s("NV002", "BS. Trần Minh Khoa", Role.BS, "Bác sĩ răng hàm mặt", "0911000002", d.minusYears(5), WorkStatus.DANG_LAM, addr);
        s("NV003", "BS. Lê Thu Hà", Role.BS, "Bác sĩ chỉnh nha", "0911000003", d.minusYears(2), WorkStatus.DANG_LAM, addr);
        s("NV004", "BS. Phạm Quốc Bảo", Role.BS, "Bác sĩ phẫu thuật", "0911000004", d.minusYears(1), WorkStatus.DANG_LAM, addr);
        s("NV005", "Võ Thị Hồng", Role.YT, "Y tá", "0911000005", d.minusYears(4), WorkStatus.DANG_LAM, addr);
        s("NV006", "Đinh Văn Phúc", Role.YT, "Y tá", "0911000006", d.minusYears(2), WorkStatus.DANG_LAM, addr);
        s("NV007", "Lý Ngọc Ánh", Role.YT, "Y tá", "0911000007", d.minusMonths(8), WorkStatus.DANG_LAM, addr);
        s("NV008", "Mai Thu Thảo", Role.LT, "Lễ tân", "0911000008", d.minusYears(2), WorkStatus.DANG_LAM, addr);
        s("NV009", "Cao Minh Đức", Role.LT, "Lễ tân", "0911000009", d.minusMonths(5), WorkStatus.DANG_LAM, addr);
        s("NV010", "Tạ Quang Vinh", Role.QL, "Quản lý phòng khám", "0911000010", d.minusYears(6), WorkStatus.DANG_LAM, addr);
        s("NV011", "Hà Thị Kim", Role.KHO, "Thủ kho", "0911000011", d.minusYears(3), WorkStatus.DANG_LAM, addr);
        s("NV012", "Châu Văn Lộc", Role.YT, "Y tá", "0911000012", d.minusYears(2), WorkStatus.NGHI_VIEC, addr);
        int i = 1;
        for (Staff st : staff.values()) {
            AccountStatus as = st.status() == WorkStatus.NGHI_VIEC ? AccountStatus.VO_HIEU : st.id().equals("NV009") ? AccountStatus.KHOA : AccountStatus.HOAT_DONG;
            String id = String.format("TK%03d", i++);
            accounts.put(id, new Account(id, st.id().toLowerCase() + ".demo", st.id(), st.role(), as,
                    st.status() == WorkStatus.NGHI_VIEC ? null : d.minusDays(i % 4).atTime(7, 30)));
        }
        // Phân công ngày D: ca sáng đủ; ca chiều thiếu 1 YT; ca tối thiếu 1 YT (khớp Tổng quan).
        seedAssign(d, ShiftKind.SANG, "NV002", "Khám phòng 1"); seedAssign(d, ShiftKind.SANG, "NV003", "Khám phòng 2");
        seedAssign(d, ShiftKind.SANG, "NV005", "Phụ tá phòng 1"); seedAssign(d, ShiftKind.SANG, "NV006", "Phụ tá phòng 2");
        seedAssign(d, ShiftKind.SANG, "NV008", "Quầy tiếp nhận");
        seedAssign(d, ShiftKind.CHIEU, "NV002", "Khám phòng 1"); seedAssign(d, ShiftKind.CHIEU, "NV007", "Phụ tá phòng 1");
        seedAssign(d, ShiftKind.CHIEU, "NV009", "Quầy tiếp nhận");
        seedAssign(d, ShiftKind.TOI, "NV003", "Khám phòng 2"); seedAssign(d, ShiftKind.TOI, "NV008", "Quầy tiếp nhận");
        // Ngày hôm sau để thử đổi ngày.
        seedAssign(d.plusDays(1), ShiftKind.SANG, "NV004", "Khám phòng 1"); seedAssign(d.plusDays(1), ShiftKind.SANG, "NV006", "Phụ tá phòng 1");
        // Bàn giao: 2 chờ xác nhận + 1 đã xác nhận.
        handover(d.minusDays(2), ShiftKind.TOI, "NV006", "NV005", d.minusDays(2).atTime(20, 5), "Bàn giao bình thường", false,
                HandoverStatus.DA_XAC_NHAN, "NV005");
        handover(d.minusDays(1), ShiftKind.CHIEU, "NV007", "NV006", d.minusDays(1).atTime(17, 2), "Thiếu 2 hộp găng tay so với sổ (mẫu)", true,
                HandoverStatus.CHO_XAC_NHAN, null);
        handover(d.minusDays(1), ShiftKind.TOI, "NV008", "NV005", d.minusDays(1).atTime(20, 10), "Bàn giao chìa khóa tủ thuốc (mẫu)", false,
                HandoverStatus.CHO_XAC_NHAN, null);
        // Đánh giá: quý 3 đã gửi, quý 4 bản nháp.
        eval("NV005", "Quý 3/2026", "NV010", List.of(5, 4, 5, 4, 5), "Làm việc chủ động (mẫu)", EvalStatus.DA_GUI);
        eval("NV008", "Quý 3/2026", "NV010", List.of(4, 5, 4, 4, 4), "Giao tiếp tốt với bệnh nhân (mẫu)", EvalStatus.DA_GUI);
        eval("NV002", "Quý 3/2026", "NV010", List.of(5, 5, 4, 5, 4), "Chuyên môn vững (mẫu)", EvalStatus.DA_GUI);
        eval("NV006", "Quý 4/2026", "NV010", List.of(4, 4, 3, 4, 4), "", EvalStatus.NHAP);
        eval("NV009", "Quý 4/2026", "NV010", List.of(3, 4, 3, 3, 4), "", EvalStatus.NHAP);
    }
    private void s(String id, String name, Role role, String title, String phone, LocalDate start, WorkStatus st, String addr) {
        staff.put(id, new Staff(id, name, role, title, phone, addr, start, st));
    }
    private void seedAssign(LocalDate d, ShiftKind k, String staffId, String task) {
        String id = String.format("PC%04d", nextAssignment++); assignments.add(new Assignment(id, d, k, staffId, task));
    }
    private void handover(LocalDate d, ShiftKind k, String giver, String receiver, LocalDateTime time, String note, boolean disc,
            HandoverStatus st, String confirmedBy) {
        String id = String.format("BG%04d", nextHandover++);
        handovers.put(id, new Handover(id, d, k, giver, receiver, time, note, disc, st, giver, confirmedBy));
    }
    private void eval(String staffId, String period, String evaluator, List<Integer> scores, String comment, EvalStatus st) {
        String id = String.format("DG%04d", nextEval++);
        evaluations.put(id, new Evaluation(id, staffId, period, evaluator, List.copyOf(scores), comment, st));
    }

    // ---------------------------------------------------------------- queries
    public List<Staff> staff() { return List.copyOf(staff.values()); }
    public Optional<Staff> staff(String id) { return Optional.ofNullable(id == null ? null : staff.get(id)); }
    public String name(String id) { return staff(id).map(Staff::name).orElse(id == null ? "—" : id); }
    public List<Staff> active() { return staff.values().stream().filter(s -> s.status() == WorkStatus.DANG_LAM).toList(); }
    public List<Account> accounts() { return List.copyOf(accounts.values()); }
    public Optional<Account> account(String id) { return Optional.ofNullable(accounts.get(id)); }
    public Optional<Account> accountOf(String staffId) { return accounts.values().stream().filter(a -> a.staffId().equals(staffId)).findFirst(); }
    public List<Assignment> assignments(LocalDate date, ShiftKind shift) {
        return assignments.stream().filter(a -> a.date().equals(date) && a.shift() == shift).toList();
    }
    public List<Assignment> assignmentsOf(String staffId) {
        return assignments.stream().filter(a -> a.staffId().equals(staffId))
                .sorted(Comparator.comparing(Assignment::date).reversed().thenComparing(Assignment::shift)).toList();
    }
    /** Số người hợp lệ theo vai trò trong ca (DISTINCT nhân viên đang làm việc). */
    public long count(LocalDate date, ShiftKind shift, Role role) {
        return assignments(date, shift).stream().map(Assignment::staffId).distinct()
                .filter(id -> staff(id).map(s -> s.role() == role && s.status() == WorkStatus.DANG_LAM).orElse(false)).count();
    }
    public long missing(LocalDate date, ShiftKind shift, Role role) {
        return Math.max(0, quota(shift).getOrDefault(role, 0) - count(date, shift, role));
    }
    public long missingTotal(LocalDate date, ShiftKind shift) {
        return quota(shift).keySet().stream().mapToLong(r -> missing(date, shift, r)).sum();
    }
    public List<Handover> handovers() {
        return handovers.values().stream().sorted(Comparator.comparing(Handover::time).reversed()).toList();
    }
    public Optional<Handover> handover(String id) { return Optional.ofNullable(handovers.get(id)); }
    public List<Evaluation> evaluations() { return List.copyOf(evaluations.values()); }
    public Optional<Evaluation> evaluation(String id) { return Optional.ofNullable(evaluations.get(id)); }
    public long activeAdmins() {
        return accounts.values().stream().filter(a -> a.role() == Role.AD && a.status() == AccountStatus.HOAT_DONG).count();
    }

    // ---------------------------------------------------------------- staff
    public Result<Staff> updateContact(String id, String phone, String address) {
        Staff s = staff.get(id);
        if (s == null) return Result.fail(Code.NOT_FOUND, "Không tìm thấy nhân viên");
        String ph = phone == null ? "" : phone.replaceAll("[\\s.]", "");
        if (!PHONE.matcher(ph).matches()) return Result.fail(Code.INVALID, "Số điện thoại phải gồm 10 chữ số, bắt đầu bằng 0");
        String ad = address == null ? "" : address.trim();
        if (ad.length() > 200) return Result.fail(Code.INVALID, "Địa chỉ tối đa 200 ký tự");
        Staff u = new Staff(s.id(), s.name(), s.role(), s.title(), ph, ad, s.startDate(), s.status());
        if (u.equals(s)) return Result.fail(Code.RULE, "Không có thay đổi để cập nhật");
        staff.put(id, u); store.logAs(DemoClinicStore.ACTOR, "Cập nhật liên hệ nhân viên", "NhanVien", id, "Thành công", null); store.fire();
        return Result.success("Đã cập nhật " + id, u);
    }
    /** Lý do chặn cho nghỉ việc; null nếu được phép. */
    public String resignBlocker(String id) {
        Staff s = staff.get(id);
        if (s == null) return "Không tìm thấy nhân viên";
        if (s.status() == WorkStatus.NGHI_VIEC) return "Nhân viên đã nghỉ việc";
        if (id.equals(ADMIN_ID)) return "Không thể cho tài khoản quản trị đang thao tác nghỉ việc";
        Optional<Account> acc = accountOf(id);
        if (acc.isPresent() && acc.get().role() == Role.AD && acc.get().status() == AccountStatus.HOAT_DONG && activeAdmins() <= 1)
            return "Đây là quản trị viên cuối cùng còn hoạt động";
        if (assignments.stream().anyMatch(a -> a.staffId().equals(id) && !a.date().isBefore(store.today())))
            return "Nhân viên còn phân công từ hôm nay trở đi — gỡ phân công trước";
        return null;
    }
    public Result<Staff> resign(String id, String reason) {
        String b = resignBlocker(id); if (b != null) return Result.fail(Code.RULE, b);
        if (reason == null || reason.trim().length() < 5) return Result.fail(Code.INVALID, "Lý do nghỉ việc phải từ 5 ký tự");
        Staff s = staff.get(id);
        Staff u = new Staff(s.id(), s.name(), s.role(), s.title(), s.phone(), s.address(), s.startDate(), WorkStatus.NGHI_VIEC);
        staff.put(id, u);
        accountOf(id).ifPresent(a -> accounts.put(a.id(), new Account(a.id(), a.username(), a.staffId(), a.role(), AccountStatus.VO_HIEU, a.lastLogin())));
        store.logAs(DemoClinicStore.ACTOR, "Cho nghỉ việc (mô phỏng)", "NhanVien", id, "Thành công", null); store.fire();
        return Result.success("Đã chuyển " + id + " sang Đã nghỉ việc, tài khoản vô hiệu hóa", u);
    }

    // ---------------------------------------------------------------- accounts
    /** Lý do chặn đổi trạng thái tài khoản; null nếu được. */
    public String accountBlocker(String accountId, AccountStatus target) {
        Account a = accounts.get(accountId);
        if (a == null) return "Không tìm thấy tài khoản";
        if (a.status() == target) return "Tài khoản đã ở trạng thái " + target.label();
        if (a.staffId().equals(ADMIN_ID) && target != AccountStatus.HOAT_DONG) return "Không thể tự khóa / vô hiệu tài khoản đang dùng";
        if (a.role() == Role.AD && a.status() == AccountStatus.HOAT_DONG && target != AccountStatus.HOAT_DONG && activeAdmins() <= 1)
            return "Không thể khóa quản trị viên cuối cùng";
        if (target == AccountStatus.HOAT_DONG && staff(a.staffId()).map(s -> s.status() == WorkStatus.NGHI_VIEC).orElse(false))
            return "Nhân viên đã nghỉ việc — không mở lại tài khoản";
        return null;
    }
    public Result<Account> setAccountStatus(String accountId, AccountStatus target, String reason) {
        String b = accountBlocker(accountId, target); if (b != null) return Result.fail(Code.RULE, b);
        if (reason == null || reason.trim().length() < 5) return Result.fail(Code.INVALID, "Lý do phải từ 5 ký tự");
        Account a = accounts.get(accountId);
        Account u = new Account(a.id(), a.username(), a.staffId(), a.role(), target, a.lastLogin());
        accounts.put(a.id(), u);
        store.logAs(DemoClinicStore.ACTOR, "Đổi trạng thái tài khoản → " + target.label(), "TaiKhoan", a.id(), "Thành công", null); store.fire();
        return Result.success(a.id() + " → " + target.label() + " (thao tác giả lập, không có xác thực thật)", u);
    }
    public String roleBlocker(String accountId, Role role) {
        Account a = accounts.get(accountId);
        if (a == null) return "Không tìm thấy tài khoản";
        if (a.role() == role) return "Tài khoản đã có vai trò " + role.label();
        if (a.staffId().equals(ADMIN_ID)) return "Không tự đổi vai trò của tài khoản đang dùng";
        if (a.role() == Role.AD && a.status() == AccountStatus.HOAT_DONG && activeAdmins() <= 1) return "Không thể hạ quyền quản trị viên cuối cùng";
        if (a.status() != AccountStatus.HOAT_DONG) return "Chỉ đổi vai trò cho tài khoản đang hoạt động";
        return null;
    }
    public Result<Account> changeRole(String accountId, Role role) {
        String b = roleBlocker(accountId, role); if (b != null) return Result.fail(Code.RULE, b);
        Account a = accounts.get(accountId);
        Account u = new Account(a.id(), a.username(), a.staffId(), role, a.status(), a.lastLogin());
        accounts.put(a.id(), u);
        store.logAs(DemoClinicStore.ACTOR, "Đổi vai trò → " + role.name(), "TaiKhoan", a.id(), "Thành công", null); store.fire();
        return Result.success(a.id() + " đổi vai trò sang " + role.label() + " (thao tác giả lập)", u);
    }

    // ---------------------------------------------------------------- assignments
    public String assignBlocker(LocalDate date, ShiftKind shift, String staffId) {
        Optional<Staff> s = staff(staffId);
        if (s.isEmpty()) return "Chọn nhân viên";
        if (s.get().status() != WorkStatus.DANG_LAM) return "Nhân viên đã nghỉ việc";
        if (date.isBefore(store.today())) return "Không phân công cho ngày đã qua";
        if (assignments(date, shift).stream().anyMatch(a -> a.staffId().equals(staffId))) return "Nhân viên đã có trong ca này";
        long perDay = assignments.stream().filter(a -> a.date().equals(date) && a.staffId().equals(staffId)).count();
        if (perDay >= 2) return "Mỗi nhân viên tối đa 2 ca/ngày (giả định V1)";
        if (staffId.equals("NV004") && date.equals(store.today())) return "Bác sĩ không trực hôm nay";
        return null;
    }
    public Result<Assignment> assign(LocalDate date, ShiftKind shift, String staffId, String task) {
        String b = assignBlocker(date, shift, staffId); if (b != null) return Result.fail(Code.RULE, b);
        String t = task == null || task.isBlank() ? "Theo phân công" : task.trim();
        String id = String.format("PC%04d", nextAssignment++);
        Assignment a = new Assignment(id, date, shift, staffId, t); assignments.add(a);
        store.logAs(DemoClinicStore.ACTOR, "Phân công " + shift.label(), "PhanCong", id, "Thành công", null); store.fire();
        return Result.success("Đã phân công " + name(staffId) + " vào " + shift.label(), a);
    }
    public Result<Assignment> unassign(String assignmentId, String reason) {
        Assignment a = assignments.stream().filter(x -> x.id().equals(assignmentId)).findFirst().orElse(null);
        if (a == null) return Result.fail(Code.NOT_FOUND, "Không tìm thấy phân công");
        if (a.date().isBefore(store.today())) return Result.fail(Code.RULE, "Không gỡ phân công của ngày đã qua");
        if (reason == null || reason.trim().length() < 5) return Result.fail(Code.INVALID, "Lý do gỡ phải từ 5 ký tự");
        assignments.remove(a);
        store.logAs(DemoClinicStore.ACTOR, "Gỡ phân công", "PhanCong", a.id(), "Thành công", null); store.fire();
        return Result.success("Đã gỡ " + name(a.staffId()) + " khỏi " + a.shift().label(), a);
    }

    // ---------------------------------------------------------------- handover
    public Result<Handover> createHandover(ShiftKind shift, String giverId, String receiverId, String note, boolean discrepancy) {
        if (staff(giverId).isEmpty() || staff(receiverId).isEmpty()) return Result.fail(Code.INVALID, "Chọn người giao và người nhận");
        if (giverId.equals(receiverId)) return Result.fail(Code.INVALID, "Người nhận phải khác người giao");
        String n = note == null ? "" : note.trim();
        if (discrepancy && n.length() < 5) return Result.fail(Code.INVALID, "Có chênh lệch thì ghi chú phải từ 5 ký tự");
        if (n.length() > 300) return Result.fail(Code.INVALID, "Ghi chú tối đa 300 ký tự");
        String id = String.format("BG%04d", nextHandover++);
        Handover h = new Handover(id, store.today(), shift, giverId, receiverId, store.now(), n.isEmpty() ? "Không ghi chú" : n,
                discrepancy, HandoverStatus.CHO_XAC_NHAN, giverId, null);
        handovers.put(id, h);
        store.logAs(giverId + " (mô phỏng)", "Lập biên bản bàn giao", "BanGiao", id, "Thành công", null); store.fire();
        return Result.success("Đã lập " + id + " — chờ " + name(receiverId) + " xác nhận", h);
    }
    public String confirmBlocker(String id, String actorId) {
        Handover h = handovers.get(id);
        if (h == null) return "Không tìm thấy biên bản";
        if (h.status() != HandoverStatus.CHO_XAC_NHAN) return "Biên bản đã " + h.status().label().toLowerCase();
        if (actorId == null) return "Chọn người thao tác";
        if (actorId.equals(h.creatorId())) return "Người lập không tự xác nhận biên bản";
        if (!actorId.equals(h.receiverId())) return "Chỉ người nhận ca (" + name(h.receiverId()) + ") được xác nhận";
        return null;
    }
    public Result<Handover> confirmHandover(String id, String actorId) {
        String b = confirmBlocker(id, actorId); if (b != null) return Result.fail(Code.RULE, b);
        Handover h = handovers.get(id);
        Handover u = new Handover(h.id(), h.date(), h.fromShift(), h.giverId(), h.receiverId(), h.time(), h.note(), h.discrepancy(),
                HandoverStatus.DA_XAC_NHAN, h.creatorId(), actorId);
        handovers.put(id, u);
        store.logAs(actorId + " (mô phỏng)", "Xác nhận bàn giao", "BanGiao", id, "Thành công", null); store.fire();
        return Result.success("Đã xác nhận " + id, u);
    }
    public Result<Handover> cancelHandover(String id, String reason) {
        Handover h = handovers.get(id);
        if (h == null) return Result.fail(Code.NOT_FOUND, "Không tìm thấy biên bản");
        if (h.status() != HandoverStatus.CHO_XAC_NHAN) return Result.fail(Code.RULE, "Chỉ hủy biên bản đang chờ xác nhận");
        if (reason == null || reason.trim().length() < 5) return Result.fail(Code.INVALID, "Lý do hủy phải từ 5 ký tự");
        Handover u = new Handover(h.id(), h.date(), h.fromShift(), h.giverId(), h.receiverId(), h.time(), h.note() + " · Hủy: " + reason.trim(),
                h.discrepancy(), HandoverStatus.DA_HUY, h.creatorId(), null);
        handovers.put(id, u);
        store.logAs(DemoClinicStore.ACTOR, "Hủy biên bản bàn giao", "BanGiao", id, "Thành công", null); store.fire();
        return Result.success("Đã hủy " + id, u);
    }

    // ---------------------------------------------------------------- evaluation
    public String evalBlocker(String id, String actorId) {
        Evaluation e = evaluations.get(id);
        if (e == null) return "Không tìm thấy phiếu đánh giá";
        if (e.status() == EvalStatus.DA_GUI) return "Phiếu đã gửi — không chỉnh sửa";
        if (actorId == null) return "Chọn người đánh giá";
        if (actorId.equals(e.staffId())) return "Không tự đánh giá bản thân";
        return null;
    }
    public Result<Evaluation> saveEvaluation(String id, String actorId, List<Integer> scores, String comment, boolean submit) {
        String b = evalBlocker(id, actorId); if (b != null) return Result.fail(Code.RULE, b);
        if (scores.size() != CRITERIA.size() || scores.stream().anyMatch(x -> x == null || x < 1 || x > 5))
            return Result.fail(Code.INVALID, "Mỗi tiêu chí chấm từ 1 đến 5");
        String c = comment == null ? "" : comment.trim();
        if (submit && c.length() < 5) return Result.fail(Code.INVALID, "Gửi phiếu cần nhận xét từ 5 ký tự");
        Evaluation e = evaluations.get(id);
        Evaluation u = new Evaluation(e.id(), e.staffId(), e.period(), actorId, List.copyOf(scores), c, submit ? EvalStatus.DA_GUI : EvalStatus.NHAP);
        evaluations.put(id, u);
        store.logAs(actorId + " (mô phỏng)", submit ? "Gửi phiếu đánh giá" : "Lưu nháp đánh giá", "DanhGia", id, "Thành công", null); store.fire();
        return Result.success((submit ? "Đã gửi " : "Đã lưu nháp ") + id + String.format(" · điểm TB %.1f", u.average()), u);
    }
}
