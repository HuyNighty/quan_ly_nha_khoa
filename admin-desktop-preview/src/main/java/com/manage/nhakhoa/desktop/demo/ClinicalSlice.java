package com.manage.nhakhoa.desktop.demo;

import com.manage.nhakhoa.desktop.demo.DemoClinicStore.Appointment;
import com.manage.nhakhoa.desktop.demo.DemoClinicStore.Code;
import com.manage.nhakhoa.desktop.demo.DemoClinicStore.ExamTicket;
import com.manage.nhakhoa.desktop.demo.DemoClinicStore.Result;
import com.manage.nhakhoa.desktop.demo.DemoClinicStore.TicketStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Slice lâm sàng MÔ PHỎNG: chi tiết phiếu khám (chẩn đoán/kết luận mẫu), chỉ định cận lâm sàng, kế hoạch điều trị,
 * hạng mục đến hạn (tái khám). Mọi thao tác chuyên môn gắn bác sĩ phụ trách; Quản trị không ký thay.
 * Seed khớp Tổng quan: 6 hạng mục đến / quá hạn trong 14 ngày.
 */
public final class ClinicalSlice {
    public static final int DUE_HORIZON_DAYS = 14;
    public enum OrderStatus { CHO_KET_QUA("Chờ kết quả"), CO_KET_QUA("Đã có kết quả");
        private final String label; OrderStatus(String l) { label = l; } public String label() { return label; } }
    public enum PlanStatus { DANG_THUC_HIEN("Đang thực hiện"), HOAN_THANH("Hoàn thành"), DA_HUY("Đã hủy");
        private final String label; PlanStatus(String l) { label = l; } public String label() { return label; } }
    public enum ItemStatus { CHUA_LAM("Chưa làm"), DA_LAM("Đã làm"), DA_HUY("Đã hủy");
        private final String label; ItemStatus(String l) { label = l; } public String label() { return label; } }
    public enum DueState { QUA_HAN("Quá hạn"), DEN_HAN("Đến hạn"), SAP_TOI("Sắp tới"), CHUA_DEN("Chưa đến");
        private final String label; DueState(String l) { label = l; } public String label() { return label; } }

    public record ExamRecord(String ticketId, String diagnosis, String conclusion) { }
    public record Order(String id, String ticketId, String serviceName, OrderStatus status, String result) { }
    public record Plan(String id, String patientId, String doctorId, String title, LocalDate created, PlanStatus status) { }
    public record PlanItem(String id, String planId, String serviceName, String tooth, LocalDate planned, long price,
            ItemStatus status, String doneBy, String note) { }

    private final DemoClinicStore store;
    private final Map<String, ExamRecord> records = new HashMap<>();
    private final Map<String, Order> orders = new LinkedHashMap<>();
    private final Map<String, Plan> plans = new LinkedHashMap<>();
    private final Map<String, PlanItem> items = new LinkedHashMap<>();
    /** Hạng mục → lịch hẹn đã đặt cho hạng mục đó (đề xuất từ màn Tái khám). */
    private final Map<String, String> bookings = new HashMap<>();
    private int nextOrder = 1, nextPlan = 1, nextItem = 1;

    ClinicalSlice(DemoClinicStore store) { this.store = store; }

    void seed(LocalDate d) {
        records.put("PK000001", new ExamRecord("PK000001", "Viêm nướu nhẹ (mẫu)", "Đã lấy cao răng; hẹn kiểm tra sau 6 tháng (mẫu)"));
        records.put("PK000002", new ExamRecord("PK000002", "Sâu răng 26 mặt nhai (mẫu)", "Đã trám composite (mẫu)"));
        records.put("PK000003", new ExamRecord("PK000003", "", ""));
        order("PK000002", "Chụp X-quang quanh chóp", OrderStatus.CO_KET_QUA, "Không thấy tổn thương quanh chóp (mẫu)");
        order("PK000003", "Chụp X-quang quanh chóp", OrderStatus.CHO_KET_QUA, null);
        // Kế hoạch điều trị: 6 hạng mục chưa làm trong 14 ngày tới (gồm quá hạn).
        String p1 = plan("BN000004", "NV002", "Điều trị tủy & phục hình răng 36", d.minusDays(1), PlanStatus.DANG_THUC_HIEN);
        item(p1, "Điều trị tủy", "36", d.plusDays(3), 1_200_000, ItemStatus.CHUA_LAM);
        item(p1, "Bọc răng sứ", "36", d.plusDays(20), 3_000_000, ItemStatus.CHUA_LAM);
        String p2 = plan("BN000009", "NV002", "Chỉnh nha mắc cài", d.minusMonths(4), PlanStatus.DANG_THUC_HIEN);
        item(p2, "Gắn mắc cài", "—", d.minusMonths(4), 8_000_000, ItemStatus.DA_LAM);
        String overdueBooked = item(p2, "Chỉnh nha – tái khám", "—", d.minusDays(5), 500_000, ItemStatus.CHUA_LAM);
        item(p2, "Chỉnh nha – tái khám", "—", d.plusDays(25), 500_000, ItemStatus.CHUA_LAM);
        String p3 = plan("BN000002", "NV003", "Điều trị nha chu", d.minusDays(30), PlanStatus.DANG_THUC_HIEN);
        item(p3, "Lấy cao răng sâu", "Hàm trên", d.minusDays(3), 300_000, ItemStatus.CHUA_LAM);
        item(p3, "Lấy cao răng sâu", "Hàm dưới", d.plusDays(10), 300_000, ItemStatus.CHUA_LAM);
        String p4 = plan("BN000005", "NV002", "Nhổ răng khôn 48", d.minusDays(10), PlanStatus.HOAN_THANH);
        item(p4, "Nhổ răng khôn", "48", d.minusDays(7), 1_500_000, ItemStatus.DA_LAM);
        String p5 = plan("BN000010", "NV003", "Tẩy trắng răng", d.minusDays(20), PlanStatus.DANG_THUC_HIEN);
        item(p5, "Tẩy trắng tại phòng khám", "Toàn hàm", d.plusDays(7), 2_500_000, ItemStatus.CHUA_LAM);
        String p6 = plan("BN000006", "NV003", "Trám thẩm mỹ răng cửa", d.minusDays(2), PlanStatus.DANG_THUC_HIEN);
        item(p6, "Trám răng composite", "11", d.plusDays(12), 400_000, ItemStatus.CHUA_LAM);
        String p7 = plan("BN000001", "NV002", "Tẩy trắng (bệnh nhân không tiếp tục)", d.minusDays(40), PlanStatus.DA_HUY);
        item(p7, "Tẩy trắng tại phòng khám", "Toàn hàm", d.minusDays(30), 2_500_000, ItemStatus.DA_HUY);
        // Hạng mục quá hạn của BN000009 đã có lịch LH000011 (đổi từ LH000009).
        bookings.put(overdueBooked, "LH000011");
    }
    private void order(String ticket, String service, OrderStatus st, String result) {
        String id = String.format("CD%04d", nextOrder++); orders.put(id, new Order(id, ticket, service, st, result));
    }
    private String plan(String patient, String doctor, String title, LocalDate created, PlanStatus st) {
        String id = String.format("KH%04d", nextPlan++); plans.put(id, new Plan(id, patient, doctor, title, created, st)); return id;
    }
    private String item(String plan, String service, String tooth, LocalDate date, long price, ItemStatus st) {
        String id = String.format("HM%04d", nextItem++);
        items.put(id, new PlanItem(id, plan, service, tooth, date, price, st, st == ItemStatus.DA_LAM ? plans.get(plan).doctorId() : null, ""));
        return id;
    }

    // ---------------------------------------------------------------- exam
    public ExamRecord record(String ticketId) { return records.getOrDefault(ticketId, new ExamRecord(ticketId, "", "")); }
    public List<Order> orders(String ticketId) { return orders.values().stream().filter(o -> o.ticketId().equals(ticketId)).toList(); }
    /** Lý do chưa được kết luận; null nếu đủ điều kiện (có chẩn đoán và mọi chỉ định đã có kết quả). */
    public String concludeBlocker(String ticketId) {
        Optional<ExamTicket> t = store.ticket(ticketId);
        if (t.isEmpty()) return "Không tìm thấy phiếu";
        if (t.get().status() != TicketStatus.DANG_KHAM) return "Chỉ kết luận phiếu đang khám";
        if (record(ticketId).diagnosis().isBlank()) return "Chưa có chẩn đoán của bác sĩ phụ trách";
        long pending = orders(ticketId).stream().filter(o -> o.status() == OrderStatus.CHO_KET_QUA).count();
        if (pending > 0) return "Còn " + pending + " chỉ định cận lâm sàng chưa có kết quả";
        return null;
    }
    public Result<ExamTicket> startExam(String ticketId) {
        ExamTicket t = store.ticket(ticketId).orElse(null);
        if (t == null) return Result.fail(Code.NOT_FOUND, "Không tìm thấy phiếu");
        if (t.status() != TicketStatus.CHO_KHAM) return Result.fail(Code.RULE, "Chỉ bắt đầu khám phiếu đang chờ khám");
        if (store.doctor(t.doctorId()).map(d -> !d.onDuty()).orElse(true)) return Result.fail(Code.RULE, "Bác sĩ phụ trách không trực hôm nay");
        ExamTicket u = new ExamTicket(t.id(), t.patientId(), t.doctorId(), t.appointmentId(), t.receivedAt(), TicketStatus.DANG_KHAM, t.visitReason());
        store.putTicket(u);
        store.logAs(t.doctorId() + " (mô phỏng)", "Bắt đầu khám", "PhieuKham", t.id(), "Thành công", t.patientId()); store.fire();
        return Result.success(t.id() + " chuyển Đang khám · bác sĩ phụ trách " + store.doctorName(t.doctorId()), u);
    }
    public Result<Order> addOrder(String ticketId, String serviceName) {
        ExamTicket t = store.ticket(ticketId).orElse(null);
        if (t == null || t.status() != TicketStatus.DANG_KHAM) return Result.fail(Code.RULE, "Chỉ chỉ định khi phiếu đang khám");
        if (serviceName == null || serviceName.isBlank()) return Result.fail(Code.INVALID, "Chọn dịch vụ cận lâm sàng");
        String id = String.format("CD%04d", nextOrder++);
        Order o = new Order(id, ticketId, serviceName, OrderStatus.CHO_KET_QUA, null); orders.put(id, o);
        store.logAs(t.doctorId() + " (mô phỏng)", "Chỉ định cận lâm sàng", "ChiDinh", id, "Thành công", t.patientId()); store.fire();
        return Result.success("Đã chỉ định " + serviceName + " (" + id + ")", o);
    }
    public Result<Order> enterResult(String orderId, String result) {
        Order o = orders.get(orderId);
        if (o == null) return Result.fail(Code.NOT_FOUND, "Không tìm thấy chỉ định");
        if (o.status() == OrderStatus.CO_KET_QUA) return Result.fail(Code.RULE, "Chỉ định đã có kết quả");
        String r = result == null ? "" : result.trim();
        if (r.length() < 3 || r.length() > 300) return Result.fail(Code.INVALID, "Kết quả phải từ 3 đến 300 ký tự");
        Order u = new Order(o.id(), o.ticketId(), o.serviceName(), OrderStatus.CO_KET_QUA, r); orders.put(o.id(), u);
        String patient = store.ticket(o.ticketId()).map(ExamTicket::patientId).orElse(null);
        store.logAs(DemoClinicStore.ACTOR, "Nhập kết quả cận lâm sàng (mô phỏng)", "ChiDinh", o.id(), "Thành công", patient); store.fire();
        return Result.success("Đã nhập kết quả " + o.id(), u);
    }
    public Result<ExamRecord> saveDiagnosis(String ticketId, String diagnosis) {
        ExamTicket t = store.ticket(ticketId).orElse(null);
        if (t == null || t.status() != TicketStatus.DANG_KHAM) return Result.fail(Code.RULE, "Chỉ ghi chẩn đoán khi phiếu đang khám");
        String d = diagnosis == null ? "" : diagnosis.trim();
        if (d.length() < 3 || d.length() > 300) return Result.fail(Code.INVALID, "Chẩn đoán phải từ 3 đến 300 ký tự");
        ExamRecord u = new ExamRecord(ticketId, d, record(ticketId).conclusion()); records.put(ticketId, u);
        store.logAs(t.doctorId() + " (mô phỏng)", "Ghi chẩn đoán", "PhieuKham", ticketId, "Thành công", t.patientId()); store.fire();
        return Result.success("Đã ghi chẩn đoán theo bác sĩ phụ trách " + store.doctorName(t.doctorId()), u);
    }
    public Result<ExamTicket> conclude(String ticketId, String conclusion) {
        String b = concludeBlocker(ticketId); if (b != null) return Result.fail(Code.RULE, b);
        String c = conclusion == null ? "" : conclusion.trim();
        if (c.length() < 3 || c.length() > 300) return Result.fail(Code.INVALID, "Kết luận phải từ 3 đến 300 ký tự");
        ExamTicket t = store.ticket(ticketId).orElseThrow();
        records.put(ticketId, new ExamRecord(ticketId, record(ticketId).diagnosis(), c));
        ExamTicket u = new ExamTicket(t.id(), t.patientId(), t.doctorId(), t.appointmentId(), t.receivedAt(), TicketStatus.DA_KET_LUAN, t.visitReason());
        store.putTicket(u);
        store.logAs(t.doctorId() + " (mô phỏng)", "Kết luận phiếu khám", "PhieuKham", t.id(), "Thành công", t.patientId()); store.fire();
        return Result.success(t.id() + " đã kết luận (mô phỏng, không ký thật)", u);
    }
    public Result<ExamTicket> cancelTicket(String ticketId, String reason) {
        ExamTicket t = store.ticket(ticketId).orElse(null);
        if (t == null) return Result.fail(Code.NOT_FOUND, "Không tìm thấy phiếu");
        if (t.status() != TicketStatus.CHO_KHAM) return Result.fail(Code.RULE, "Chỉ hủy phiếu đang chờ khám");
        if (reason == null || reason.trim().length() < 5) return Result.fail(Code.INVALID, "Lý do hủy phải từ 5 ký tự");
        ExamTicket u = new ExamTicket(t.id(), t.patientId(), t.doctorId(), t.appointmentId(), t.receivedAt(), TicketStatus.DA_HUY, t.visitReason());
        store.putTicket(u);
        store.logAs(DemoClinicStore.ACTOR, "Hủy phiếu khám", "PhieuKham", t.id(), "Thành công", t.patientId()); store.fire();
        return Result.success("Đã hủy " + t.id(), u);
    }

    // ---------------------------------------------------------------- plans
    public List<Plan> plans() { return List.copyOf(plans.values()); }
    public Optional<Plan> plan(String id) { return Optional.ofNullable(plans.get(id)); }
    public List<PlanItem> items(String planId) {
        return items.values().stream().filter(i -> i.planId().equals(planId)).sorted(Comparator.comparing(PlanItem::planned)).toList();
    }
    public Optional<PlanItem> item(String id) { return Optional.ofNullable(items.get(id)); }
    public long planTotal(String planId) { return items(planId).stream().filter(i -> i.status() != ItemStatus.DA_HUY).mapToLong(PlanItem::price).sum(); }
    public long doneCount(String planId) { return items(planId).stream().filter(i -> i.status() == ItemStatus.DA_LAM).count(); }
    public long activeCount(String planId) { return items(planId).stream().filter(i -> i.status() != ItemStatus.DA_HUY).count(); }
    public DueState dueState(PlanItem i) {
        LocalDate d = store.today();
        if (i.planned().isBefore(d)) return DueState.QUA_HAN;
        if (!i.planned().isAfter(d.plusDays(3))) return DueState.DEN_HAN;
        if (!i.planned().isAfter(d.plusDays(DUE_HORIZON_DAYS))) return DueState.SAP_TOI;
        return DueState.CHUA_DEN;
    }
    /** Hạng mục chưa làm của kế hoạch đang thực hiện, đến hạn trong 14 ngày (bao gồm quá hạn) — BA Q12. */
    public List<PlanItem> dueItems() {
        return items.values().stream().filter(i -> i.status() == ItemStatus.CHUA_LAM)
                .filter(i -> plans.get(i.planId()).status() == PlanStatus.DANG_THUC_HIEN)
                .filter(i -> !i.planned().isAfter(store.today().plusDays(DUE_HORIZON_DAYS)))
                .sorted(Comparator.comparing(PlanItem::planned)).toList();
    }
    public long duePlanCount() { return dueItems().stream().map(PlanItem::planId).distinct().count(); }
    public Optional<Appointment> booking(String itemId) {
        String appt = bookings.get(itemId);
        return store.appointment(appt).filter(a -> a.status() != com.manage.nhakhoa.desktop.model.DashboardViewModel.AppointmentStatus.DA_HUY);
    }
    public String completeItemBlocker(String itemId) {
        PlanItem i = items.get(itemId);
        if (i == null) return "Chọn hạng mục";
        if (plans.get(i.planId()).status() != PlanStatus.DANG_THUC_HIEN) return "Kế hoạch không ở trạng thái đang thực hiện";
        if (i.status() != ItemStatus.CHUA_LAM) return "Hạng mục đã " + i.status().label().toLowerCase();
        return null;
    }
    public Result<PlanItem> completeItem(String itemId) {
        String b = completeItemBlocker(itemId); if (b != null) return Result.fail(Code.RULE, b);
        PlanItem i = items.get(itemId); Plan p = plans.get(i.planId());
        PlanItem u = new PlanItem(i.id(), i.planId(), i.serviceName(), i.tooth(), i.planned(), i.price(), ItemStatus.DA_LAM, p.doctorId(),
                "Ghi nhận theo bác sĩ phụ trách " + store.doctorName(p.doctorId()) + " (mô phỏng)");
        items.put(i.id(), u);
        store.logAs(p.doctorId() + " (mô phỏng)", "Hoàn thành hạng mục điều trị", "ChiTietDieuTri", i.id(), "Thành công", p.patientId()); store.fire();
        return Result.success(i.id() + " đã làm · bác sĩ phụ trách " + store.doctorName(p.doctorId()), u);
    }
    public Result<PlanItem> reschedule(String itemId, LocalDate date) {
        PlanItem i = items.get(itemId);
        if (i == null) return Result.fail(Code.NOT_FOUND, "Chọn hạng mục");
        if (i.status() != ItemStatus.CHUA_LAM) return Result.fail(Code.RULE, "Chỉ dời ngày hạng mục chưa làm");
        if (date == null) return Result.fail(Code.INVALID, "Ngày không hợp lệ (dd/MM/yyyy)");
        if (date.isBefore(store.today())) return Result.fail(Code.INVALID, "Ngày dự kiến không được ở quá khứ");
        PlanItem u = new PlanItem(i.id(), i.planId(), i.serviceName(), i.tooth(), date, i.price(), i.status(), i.doneBy(), i.note());
        items.put(i.id(), u);
        store.logAs(DemoClinicStore.ACTOR, "Dời ngày dự kiến", "ChiTietDieuTri", i.id(), "Thành công", plans.get(i.planId()).patientId()); store.fire();
        return Result.success("Đã dời " + i.id() + " sang " + date.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")), u);
    }
    public String completePlanBlocker(String planId) {
        Plan p = plans.get(planId);
        if (p == null) return "Chọn kế hoạch";
        if (p.status() != PlanStatus.DANG_THUC_HIEN) return "Kế hoạch đã " + p.status().label().toLowerCase();
        long left = items(planId).stream().filter(i -> i.status() == ItemStatus.CHUA_LAM).count();
        if (left > 0) return "Còn " + left + " hạng mục chưa làm";
        if (doneCount(planId) == 0) return "Chưa có hạng mục nào đã làm";
        return null;
    }
    public Result<Plan> completePlan(String planId) {
        String b = completePlanBlocker(planId); if (b != null) return Result.fail(Code.RULE, b);
        Plan p = plans.get(planId);
        Plan u = new Plan(p.id(), p.patientId(), p.doctorId(), p.title(), p.created(), PlanStatus.HOAN_THANH); plans.put(p.id(), u);
        store.logAs(p.doctorId() + " (mô phỏng)", "Hoàn tất kế hoạch điều trị", "KeHoachDieuTri", p.id(), "Thành công", p.patientId()); store.fire();
        return Result.success(p.id() + " hoàn thành", u);
    }
    /** Tạo lịch hẹn ĐỀ XUẤT (chờ xác nhận) cho hạng mục đến hạn; không tự xác nhận. */
    public Result<Appointment> proposeAppointment(String itemId, LocalDateTime time) {
        PlanItem i = items.get(itemId);
        if (i == null) return Result.fail(Code.NOT_FOUND, "Chọn hạng mục");
        if (i.status() != ItemStatus.CHUA_LAM) return Result.fail(Code.RULE, "Hạng mục không còn chờ thực hiện");
        if (booking(itemId).isPresent()) return Result.fail(Code.RULE, "Hạng mục đã có lịch " + bookings.get(itemId));
        Plan p = plans.get(i.planId());
        Result<Appointment> r = store.createAppointment(p.patientId(), p.doctorId(), time, "Tái khám: " + i.serviceName());
        if (r.ok()) bookings.put(itemId, r.value().id());
        return r;
    }
    List<PlanItem> allItems() { return new ArrayList<>(items.values()); }
}
