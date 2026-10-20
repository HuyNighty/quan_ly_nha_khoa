package com.manage.nhakhoa.desktop.demo;

import com.manage.nhakhoa.desktop.demo.DemoClinicStore.Code;
import com.manage.nhakhoa.desktop.demo.DemoClinicStore.Result;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Slice thanh toán MÔ PHỎNG: bảng giá dịch vụ, hóa đơn (giá chốt theo dòng — không hồi tố), giao dịch 3 phương thức,
 * yêu cầu hủy hóa đơn (người lập ≠ người duyệt). Không gọi cổng thanh toán.
 * Seed khớp Tổng quan: thu hợp lệ hôm nay 700.000 đ tiền mặt; công nợ 3.600.000 đ trên 5 hóa đơn; 2 yêu cầu hủy chờ duyệt.
 * Hóa đơn chờ duyệt hủy VẪN tính công nợ cho tới khi Đã hủy (BA Q07).
 */
public final class BillingSlice {
    public enum Group { KHAM("Khám"), CAN_LAM_SANG("Cận lâm sàng"), NHA_CHU("Nha chu"), DIEU_TRI("Điều trị"), PHAU_THUAT("Phẫu thuật"),
        PHUC_HINH("Phục hình"), CHINH_NHA("Chỉnh nha"), THAM_MY("Thẩm mỹ");
        private final String label; Group(String l) { label = l; } public String label() { return label; } }
    public enum Method { TIEN_MAT("Tiền mặt"), CHUYEN_KHOAN("Chuyển khoản"), POS("Thẻ POS");
        private final String label; Method(String l) { label = l; } public String label() { return label; }
        @Override public String toString() { return label; } }
    public enum PayStatus { THANH_CONG("Thành công"), DA_HUY("Đã hủy");
        private final String label; PayStatus(String l) { label = l; } public String label() { return label; } }
    public enum InvoiceStatus { CHUA_THANH_TOAN("Chưa thanh toán"), MOT_PHAN("Thanh toán một phần"), DA_THANH_TOAN("Đã thanh toán"), DA_HUY("Đã hủy");
        private final String label; InvoiceStatus(String l) { label = l; } public String label() { return label; } }
    public enum RequestStatus { CHO_DUYET("Chờ duyệt"), DA_DUYET("Đã duyệt"), TU_CHOI("Từ chối");
        private final String label; RequestStatus(String l) { label = l; } public String label() { return label; } }

    public record Service(String id, String name, Group group, long price, boolean active, String condition) { }
    public record Line(String serviceId, String name, int qty, long unitPrice) { public long amount() { return unitPrice * qty; } }
    /** Dòng chọn từ bảng giá; giá được chốt từ Service tại thời điểm lập hóa đơn, không lấy từ client. */
    public record InvoiceLineDraft(String serviceId, int qty) { }
    public record Invoice(String id, String patientId, String ticketId, LocalDateTime created, List<Line> lines, boolean cancelled) {
        public long total() { return lines.stream().mapToLong(Line::amount).sum(); }
    }
    public record Payment(String id, String invoiceId, LocalDateTime time, Method method, long amount, String reference,
            PayStatus status, String note, String actor) { }
    public record CancelRequest(String id, String invoiceId, String creatorId, LocalDateTime created, String reason,
            RequestStatus status, String deciderId, String decisionNote) { }

    private final DemoClinicStore store;
    private final Map<String, Service> services = new LinkedHashMap<>();
    private final Map<String, Invoice> invoices = new LinkedHashMap<>();
    private final Map<String, Payment> payments = new LinkedHashMap<>();
    private final Map<String, CancelRequest> requests = new LinkedHashMap<>();
    private int nextPayment = 1, nextRequest = 1, nextInvoice = 8;

    BillingSlice(DemoClinicStore store) { this.store = store; }

    void seed(LocalDate d) {
        svc("DV001", "Khám tổng quát", Group.KHAM, 100_000, true, "Áp dụng mỗi lượt khám");
        svc("DV002", "Lấy cao răng", Group.NHA_CHU, 300_000, true, "Hai hàm, mức độ thường");
        svc("DV003", "Trám răng composite", Group.DIEU_TRI, 400_000, true, "Mỗi răng");
        svc("DV004", "Nhổ răng khôn", Group.PHAU_THUAT, 1_500_000, true, "Răng mọc lệch, gây tê tại chỗ");
        svc("DV005", "Tẩy trắng tại phòng khám", Group.THAM_MY, 2_500_000, true, "Toàn hàm, 1 lần");
        svc("DV006", "Chụp X-quang quanh chóp", Group.CAN_LAM_SANG, 100_000, true, "Mỗi phim");
        svc("DV007", "Chụp phim toàn cảnh", Group.CAN_LAM_SANG, 250_000, true, "Mỗi phim");
        svc("DV008", "Điều trị tủy", Group.DIEU_TRI, 1_200_000, true, "Răng nhiều chân");
        svc("DV009", "Bọc răng sứ", Group.PHUC_HINH, 3_000_000, true, "Mỗi răng, sứ kim loại (mẫu)");
        svc("DV010", "Chỉnh nha – tái khám", Group.CHINH_NHA, 500_000, true, "Mỗi lần điều chỉnh");
        svc("DV011", "Gắn mắc cài", Group.CHINH_NHA, 8_000_000, true, "Trọn gói mắc cài kim loại (mẫu)");
        svc("DV012", "Tẩy trắng tại nhà", Group.THAM_MY, 1_800_000, false, "Ngừng áp dụng từ 01/09/2026 (mẫu)");
        inv("HD000001", "BN000001", "PK000001", d.atTime(7, 40), line("DV001"), line("DV002"));
        inv("HD000002", "BN000003", "PK000002", d.atTime(7, 42), line("DV001"), line("DV006"), line("DV003"));
        inv("HD000003", "BN000002", null, d.minusDays(10).atTime(10, 0), line("DV008"));
        inv("HD000004", "BN000005", null, d.minusDays(7).atTime(9, 30), line("DV004"));
        inv("HD000005", "BN000010", null, d.minusDays(20).atTime(15, 0), line("DV010"), line("DV005"));
        inv("HD000006", "BN000008", null, d.minusDays(3).atTime(11, 0), line("DV001"), line("DV002"), line("DV006"));
        inv("HD000007", "BN000012", null, d.minusDays(15).atTime(14, 0), line("DV003"));
        invoices.put("HD000007", new Invoice("HD000007", "BN000012", null, d.minusDays(15).atTime(14, 0), invoices.get("HD000007").lines(), true));
        pay("HD000001", d.atTime(7, 41), Method.TIEN_MAT, 400_000, "", PayStatus.THANH_CONG, "");
        pay("HD000002", d.atTime(7, 44), Method.TIEN_MAT, 300_000, "", PayStatus.THANH_CONG, "");
        pay("HD000003", d.minusDays(10).atTime(10, 5), Method.CHUYEN_KHOAN, 400_000, "CK-DEMO-0310", PayStatus.THANH_CONG, "");
        pay("HD000003", d.minusDays(10).atTime(10, 2), Method.POS, 100_000, "POS-DEMO-0001", PayStatus.DA_HUY, "Quẹt nhầm số tiền (mẫu)");
        pay("HD000004", d.minusDays(7).atTime(9, 35), Method.POS, 500_000, "POS-DEMO-0912", PayStatus.THANH_CONG, "");
        pay("HD000005", d.minusDays(20).atTime(15, 10), Method.CHUYEN_KHOAN, 2_000_000, "CK-DEMO-2209", PayStatus.THANH_CONG, "");
        req("HD000006", "NV008", d.minusDays(1).atTime(16, 0), "Bệnh nhân khiếu nại tính trùng dịch vụ (mẫu)", RequestStatus.CHO_DUYET, null, null);
        req("HD000005", StaffSlice.ADMIN_ID, d.atTime(7, 30), "Đề nghị hủy để lập lại hóa đơn đúng gói (mẫu)", RequestStatus.CHO_DUYET, null, null);
        req("HD000007", "NV009", d.minusDays(14).atTime(9, 0), "Lập nhầm hồ sơ bệnh nhân (mẫu)", RequestStatus.DA_DUYET, "NV010", "Đồng ý hủy (mẫu)");
    }
    private void svc(String id, String name, Group g, long price, boolean active, String cond) { services.put(id, new Service(id, name, g, price, active, cond)); }
    private Line line(String svc) { Service s = services.get(svc); return new Line(s.id(), s.name(), 1, s.price()); }
    private void inv(String id, String patient, String ticket, LocalDateTime t, Line... lines) {
        invoices.put(id, new Invoice(id, patient, ticket, t, List.of(lines), false));
    }
    private void pay(String inv, LocalDateTime t, Method m, long amount, String ref, PayStatus st, String note) {
        String id = String.format("GD%05d", nextPayment++); payments.put(id, new Payment(id, inv, t, m, amount, ref, st, note, "NV008"));
    }
    private void req(String inv, String creator, LocalDateTime t, String reason, RequestStatus st, String decider, String note) {
        String id = String.format("YC%04d", nextRequest++); requests.put(id, new CancelRequest(id, inv, creator, t, reason, st, decider, note));
    }

    // ---------------------------------------------------------------- services
    public List<Service> services() { return List.copyOf(services.values()); }
    public Optional<Service> service(String id) { return Optional.ofNullable(services.get(id)); }
    /** Đổi giá chỉ áp dụng cho hóa đơn lập SAU; dòng hóa đơn cũ giữ đơn giá đã chốt. */
    public Result<Service> changePrice(String id, long price, String reason) {
        Service s = services.get(id);
        if (s == null) return Result.fail(Code.NOT_FOUND, "Không tìm thấy dịch vụ");
        if (!s.active()) return Result.fail(Code.RULE, "Dịch vụ đã ngừng áp dụng");
        if (price < 1_000 || price > 100_000_000 || price % 1_000 != 0) return Result.fail(Code.INVALID, "Giá từ 1.000 đến 100.000.000 đ, làm tròn nghìn");
        if (price == s.price()) return Result.fail(Code.INVALID, "Giá mới trùng giá hiện tại");
        if (reason == null || reason.trim().length() < 5) return Result.fail(Code.INVALID, "Lý do điều chỉnh phải từ 5 ký tự");
        Service u = new Service(s.id(), s.name(), s.group(), price, s.active(), s.condition()); services.put(id, u);
        store.logAs(DemoClinicStore.ACTOR, "Điều chỉnh giá dịch vụ", "DichVu", id, "Thành công", null); store.fire();
        return Result.success("Đã đổi giá " + id + "; hóa đơn đã lập giữ nguyên đơn giá cũ", u);
    }

    // ---------------------------------------------------------------- invoices
    /**
     * Lập hóa đơn MÔ PHỎNG: kiểm tra hồ sơ, phiếu khám tùy chọn và dòng dịch vụ;
     * chốt đơn giá hiện tại trên từng dòng, không tự động ghi nhận tiền thu.
     */
    public Result<Invoice> createInvoice(String patientId, String ticketId, List<InvoiceLineDraft> drafts) {
        if (store.patient(patientId).isEmpty()) return Result.fail(Code.NOT_FOUND, "Chọn bệnh nhân hợp lệ");
        String ticket = ticketId == null || ticketId.isBlank() ? null : ticketId.trim();
        if (ticket != null) {
            DemoClinicStore.ExamTicket exam = store.ticket(ticket).orElse(null);
            if (exam == null) return Result.fail(Code.NOT_FOUND, "Phiếu khám không tồn tại");
            if (!exam.patientId().equals(patientId)) return Result.fail(Code.RULE, "Phiếu khám không thuộc bệnh nhân đã chọn");
            if (exam.status() != DemoClinicStore.TicketStatus.DA_KET_LUAN)
                return Result.fail(Code.RULE, "Chỉ được lập hóa đơn cho phiếu khám đã kết luận");
            if (invoices.values().stream().anyMatch(i -> ticket.equals(i.ticketId()) && !i.cancelled()))
                return Result.fail(Code.DUPLICATE, "Phiếu khám đã có hóa đơn còn hiệu lực");
        }
        if (drafts == null || drafts.isEmpty()) return Result.fail(Code.INVALID, "Hóa đơn phải có ít nhất một dịch vụ");
        if (drafts.size() > 20) return Result.fail(Code.INVALID, "Tối đa 20 dòng dịch vụ mỗi hóa đơn");
        List<Line> lines = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        long total = 0;
        for (InvoiceLineDraft d : drafts) {
            if (d == null || d.serviceId() == null) return Result.fail(Code.INVALID, "Dòng dịch vụ không hợp lệ");
            Service s = services.get(d.serviceId());
            if (s == null) return Result.fail(Code.NOT_FOUND, "Không tìm thấy dịch vụ " + d.serviceId());
            if (!s.active()) return Result.fail(Code.RULE, "Dịch vụ " + s.id() + " đã ngừng áp dụng");
            if (!seen.add(s.id())) return Result.fail(Code.DUPLICATE, "Dịch vụ " + s.id() + " bị thêm trùng");
            if (d.qty() < 1 || d.qty() > 99) return Result.fail(Code.INVALID, "Số lượng phải từ 1 đến 99");
            try {
                long lineTotal = Math.multiplyExact(s.price(), d.qty());
                total = Math.addExact(total, lineTotal);
            } catch (ArithmeticException e) {
                return Result.fail(Code.INVALID, "Tổng tiền vượt giới hạn");
            }
            lines.add(new Line(s.id(), s.name(), d.qty(), s.price()));
        }
        if (total <= 0) return Result.fail(Code.INVALID, "Tổng hóa đơn phải lớn hơn 0");
        String id;
        do { id = String.format("HD%06d", nextInvoice++); } while (invoices.containsKey(id));
        Invoice inv = new Invoice(id, patientId, ticket, store.now(), List.copyOf(lines), false);
        invoices.put(id, inv);
        store.logAs(DemoClinicStore.ACTOR, "Lập hóa đơn (mô phỏng)", "HoaDon", id, "Thành công", patientId);
        store.fire();
        return Result.success("Đã lập " + id + ", tổng " + String.format("%,d", total).replace(',', '.') + " đ; chưa thu tiền", inv);
    }
    public List<Invoice> invoices() {
        return invoices.values().stream().sorted(Comparator.comparing(Invoice::created).reversed()).toList();
    }
    public Optional<Invoice> invoice(String id) { return Optional.ofNullable(id == null ? null : invoices.get(id)); }
    public List<Payment> payments(String invoiceId) {
        return payments.values().stream().filter(p -> p.invoiceId().equals(invoiceId)).sorted(Comparator.comparing(Payment::time)).toList();
    }
    public long paid(String invoiceId) {
        return payments(invoiceId).stream().filter(p -> p.status() == PayStatus.THANH_CONG).mapToLong(Payment::amount).sum();
    }
    /** Công nợ từng hóa đơn còn hiệu lực; không âm (trả dư là cảnh báo riêng). */
    public long debt(String invoiceId) {
        Invoice i = invoices.get(invoiceId);
        return i == null || i.cancelled() ? 0 : Math.max(0, i.total() - paid(invoiceId));
    }
    public InvoiceStatus status(String invoiceId) {
        Invoice i = invoices.get(invoiceId);
        if (i.cancelled()) return InvoiceStatus.DA_HUY;
        long p = paid(invoiceId);
        return p == 0 ? InvoiceStatus.CHUA_THANH_TOAN : p < i.total() ? InvoiceStatus.MOT_PHAN : InvoiceStatus.DA_THANH_TOAN;
    }
    public long totalDebt() { return invoices.keySet().stream().mapToLong(this::debt).sum(); }
    public long debtInvoiceCount() { return invoices.keySet().stream().filter(id -> debt(id) > 0).count(); }
    public long receivedOn(LocalDate date, Method method) {
        return payments.values().stream().filter(p -> p.status() == PayStatus.THANH_CONG && p.time().toLocalDate().equals(date))
                .filter(p -> method == null || p.method() == method).mapToLong(Payment::amount).sum();
    }
    public Optional<CancelRequest> pendingRequest(String invoiceId) {
        return requests.values().stream().filter(r -> r.invoiceId().equals(invoiceId) && r.status() == RequestStatus.CHO_DUYET).findFirst();
    }
    public String payBlocker(String invoiceId) {
        Invoice i = invoices.get(invoiceId);
        if (i == null) return "Chọn hóa đơn";
        if (i.cancelled()) return "Hóa đơn đã hủy";
        if (debt(invoiceId) == 0) return "Hóa đơn đã thanh toán đủ";
        if (pendingRequest(invoiceId).isPresent()) return "Hóa đơn đang chờ duyệt hủy — tạm khóa thu tiền (giả định demo)";
        return null;
    }
    public Result<Payment> pay(String invoiceId, Method method, long amount, String reference) {
        String b = payBlocker(invoiceId); if (b != null) return Result.fail(Code.RULE, b);
        if (method == null) return Result.fail(Code.INVALID, "Chọn phương thức");
        if (amount <= 0) return Result.fail(Code.INVALID, "Số tiền phải lớn hơn 0");
        if (amount > debt(invoiceId)) return Result.fail(Code.INVALID, "Không thu quá số còn nợ (" + String.format("%,d", debt(invoiceId)).replace(',', '.') + " đ)");
        String ref = reference == null ? "" : reference.trim();
        if (method != Method.TIEN_MAT && ref.length() < 4) return Result.fail(Code.INVALID, "Chuyển khoản / POS cần mã giao dịch (≥ 4 ký tự)");
        String id = String.format("GD%05d", nextPayment++);
        Payment p = new Payment(id, invoiceId, store.now(), method, amount, method == Method.TIEN_MAT ? "" : ref, PayStatus.THANH_CONG, "", DemoClinicStore.ACTOR);
        payments.put(id, p);
        store.logAs(DemoClinicStore.ACTOR, "Thu tiền " + method.label(), "ThanhToan", id, "Thành công", invoices.get(invoiceId).patientId()); store.fire();
        return Result.success("Đã ghi nhận " + id + " cho " + invoiceId + " (không gọi cổng thanh toán)", p);
    }
    public Result<Payment> voidPayment(String paymentId, String reason) {
        Payment p = payments.get(paymentId);
        if (p == null) return Result.fail(Code.NOT_FOUND, "Chọn giao dịch");
        if (p.status() != PayStatus.THANH_CONG) return Result.fail(Code.RULE, "Giao dịch đã hủy");
        if (!p.time().toLocalDate().equals(store.today())) return Result.fail(Code.RULE, "Chỉ hủy giao dịch trong ngày (giả định demo)");
        if (reason == null || reason.trim().length() < 5) return Result.fail(Code.INVALID, "Lý do hủy giao dịch phải từ 5 ký tự");
        Payment u = new Payment(p.id(), p.invoiceId(), p.time(), p.method(), p.amount(), p.reference(), PayStatus.DA_HUY, reason.trim(), p.actor());
        payments.put(p.id(), u);
        store.logAs(DemoClinicStore.ACTOR, "Hủy giao dịch (giữ dấu vết)", "ThanhToan", p.id(), "Thành công", invoices.get(p.invoiceId()).patientId()); store.fire();
        return Result.success("Đã hủy " + p.id() + " — vẫn lưu dấu vết", u);
    }

    // ---------------------------------------------------------------- cancel requests
    public List<CancelRequest> requests() {
        return requests.values().stream().sorted(Comparator.comparing(CancelRequest::created).reversed()).toList();
    }
    public Optional<CancelRequest> request(String id) { return Optional.ofNullable(requests.get(id)); }
    public Result<CancelRequest> requestCancel(String invoiceId, String creatorId, String reason) {
        Invoice i = invoices.get(invoiceId);
        if (i == null) return Result.fail(Code.NOT_FOUND, "Chọn hóa đơn");
        if (i.cancelled()) return Result.fail(Code.RULE, "Hóa đơn đã hủy");
        if (pendingRequest(invoiceId).isPresent()) return Result.fail(Code.RULE, "Đã có yêu cầu hủy đang chờ duyệt");
        if (reason == null || reason.trim().length() < 5) return Result.fail(Code.INVALID, "Lý do hủy phải từ 5 ký tự");
        String id = String.format("YC%04d", nextRequest++);
        CancelRequest r = new CancelRequest(id, invoiceId, creatorId, store.now(), reason.trim(), RequestStatus.CHO_DUYET, null, null);
        requests.put(id, r);
        store.logAs(creatorId + " (mô phỏng)", "Tạo yêu cầu hủy hóa đơn", "YeuCauHuy", id, "Thành công", i.patientId()); store.fire();
        return Result.success("Đã tạo " + id + " — hóa đơn vẫn tính công nợ cho tới khi được duyệt", r);
    }
    /** Lý do chặn duyệt; null nếu được. Người duyệt phải khác người lập; hóa đơn đã thu tiền phải hủy giao dịch trước. */
    public String approveBlocker(String requestId, String actorId) {
        CancelRequest r = requests.get(requestId);
        if (r == null) return "Chọn yêu cầu";
        if (r.status() != RequestStatus.CHO_DUYET) return "Yêu cầu đã " + r.status().label().toLowerCase();
        if (actorId == null) return "Chọn người duyệt";
        if (actorId.equals(r.creatorId())) return "Người lập yêu cầu không được tự duyệt";
        if (paid(r.invoiceId()) > 0) return "Hóa đơn đã thu " + String.format("%,d", paid(r.invoiceId())).replace(',', '.')
                + " đ — cần hủy/hoàn giao dịch trước (quy trình hoàn tiền chưa có BA)";
        return null;
    }
    public String rejectBlocker(String requestId, String actorId) {
        CancelRequest r = requests.get(requestId);
        if (r == null) return "Chọn yêu cầu";
        if (r.status() != RequestStatus.CHO_DUYET) return "Yêu cầu đã " + r.status().label().toLowerCase();
        if (actorId == null) return "Chọn người duyệt";
        if (actorId.equals(r.creatorId())) return "Người lập yêu cầu không được tự xử lý";
        return null;
    }
    public Result<CancelRequest> approve(String requestId, String actorId) {
        String b = approveBlocker(requestId, actorId); if (b != null) return Result.fail(Code.RULE, b);
        CancelRequest r = requests.get(requestId);
        CancelRequest u = new CancelRequest(r.id(), r.invoiceId(), r.creatorId(), r.created(), r.reason(), RequestStatus.DA_DUYET, actorId, "Đồng ý hủy");
        requests.put(r.id(), u);
        Invoice i = invoices.get(r.invoiceId());
        invoices.put(i.id(), new Invoice(i.id(), i.patientId(), i.ticketId(), i.created(), i.lines(), true));
        store.logAs(actorId + " (mô phỏng)", "Duyệt hủy hóa đơn", "YeuCauHuy", r.id(), "Thành công", i.patientId()); store.fire();
        return Result.success("Đã duyệt " + r.id() + " — " + i.id() + " chuyển Đã hủy, không còn công nợ", u);
    }
    public Result<CancelRequest> reject(String requestId, String actorId, String note) {
        String b = rejectBlocker(requestId, actorId); if (b != null) return Result.fail(Code.RULE, b);
        if (note == null || note.trim().length() < 5) return Result.fail(Code.INVALID, "Lý do từ chối phải từ 5 ký tự");
        CancelRequest r = requests.get(requestId);
        CancelRequest u = new CancelRequest(r.id(), r.invoiceId(), r.creatorId(), r.created(), r.reason(), RequestStatus.TU_CHOI, actorId, note.trim());
        requests.put(r.id(), u);
        store.logAs(actorId + " (mô phỏng)", "Từ chối hủy hóa đơn", "YeuCauHuy", r.id(), "Thành công", invoices.get(r.invoiceId()).patientId()); store.fire();
        return Result.success("Đã từ chối " + r.id() + " — hóa đơn giữ nguyên", u);
    }
}
