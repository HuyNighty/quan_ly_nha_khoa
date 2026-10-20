package com.manage.nhakhoa.desktop.screens;

import com.manage.nhakhoa.desktop.MenuDefinition;
import com.manage.nhakhoa.desktop.demo.BillingSlice;
import com.manage.nhakhoa.desktop.demo.BillingSlice.*;
import com.manage.nhakhoa.desktop.demo.DemoClinicStore;
import com.manage.nhakhoa.desktop.demo.StaffSlice;
import com.manage.nhakhoa.desktop.ui.CardList;
import com.manage.nhakhoa.desktop.ui.DemoTable;
import com.manage.nhakhoa.desktop.ui.DemoTable.Column;
import com.manage.nhakhoa.desktop.ui.DemoTable.Tag;
import com.manage.nhakhoa.desktop.ui.Flat;
import com.manage.nhakhoa.desktop.ui.Theme;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import static com.manage.nhakhoa.desktop.ui.DemoWidgets.*;
import static com.manage.nhakhoa.desktop.ui.UiComponents.*;

/**
 * Thanh toán · Hóa đơn & thu tiền (HOA_DON): công nợ từng hóa đơn, giao dịch tiền mặt / chuyển khoản / POS, hủy giao dịch giữ dấu vết,
 * yêu cầu hủy hóa đơn. "Trạng thái thanh toán" và "Yêu cầu hủy" là hai trạng thái riêng. Không gọi cổng thanh toán.
 */
public final class InvoicePanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private final transient ScreenContext ctx;
    private final transient DemoClinicStore store;
    private final Flat.SearchBar search = new Flat.SearchBar("inv-search", "Tìm mã hóa đơn, mã / tên bệnh nhân…");
    private final Flat.Segmented filter = new Flat.Segmented("inv-filter", List.of("Tất cả", "Còn nợ", "Đã trả đủ", "Chờ hủy", "Đã hủy"), null);
    private final CardList<Invoice> list;
    private final JPanel stats = Kit.stats();
    private final JPanel detail = Kit.holder("inv-detail");
    private final JPanel side = new JPanel(new CardLayout());
    private final JComboBox<String> createPatient = combo("inv-create-patient", List.of());
    private final JComboBox<String> createTicket = combo("inv-create-ticket", List.of());
    private final JComboBox<String> createService = combo("inv-create-service", List.of());
    private final JTextField createQty = field("inv-create-qty", 5, "SL (1–99)");
    private final JLabel createTotal = label("Tổng tạm tính: 0 đ", 14, true, Theme.TEXT);
    private final JLabel createFeedback = feedback("inv-create-feedback");
    private final List<InvoiceLineDraft> draft = new ArrayList<>();
    private final DemoTable<Line> draftTable;
    private boolean creating;
    private final JLabel feedback = feedback("inv-feedback");
    private final DemoTable<Line> lines;
    private final DemoTable<Payment> payments;
    private final JComboBox<Method> method = combo("inv-method", List.of(Method.values()));
    private final JTextField amount = field("inv-amount", 10, "Số tiền, ví dụ 300000");
    private final JTextField reference = field("inv-ref", 12, "Mã giao dịch CK / POS");
    private transient String selected;

    public InvoicePanel(ScreenContext ctx) {
        super(new BorderLayout()); setOpaque(false); setName("screen-HOA_DON");
        this.ctx = ctx; this.store = ctx.store();
        list = Kit.list("inv-list", Invoice::id, i -> Kit.Spec.person(store.patientName(i.patientId()),
                i.id() + "  ·  " + Kit.money(i.total()) + (billing().debt(i.id()) > 0 ? "  ·  còn nợ " + Kit.money(billing().debt(i.id())) : ""),
                statusTag(i.id()), store.patientName(i.patientId()), i.patientId()), 7);
        list.onSelect(i -> { selected = i.id(); closeCreate(); render(); });
        lines = new DemoTable<>("inv-lines", List.of(new Column<Line>("Dịch vụ", Line::name, 200), new Column<Line>("SL", Line::qty, 44),
                new Column<Line>("Đơn giá (đã chốt)", l -> Kit.money(l.unitPrice()), 120), new Column<Line>("Thành tiền", l -> new DemoTable.Strong(Kit.money(l.amount())), 120)),
                Line::serviceId, 4);
        payments = new DemoTable<>("inv-payments", List.of(new Column<Payment>("Mã GD", Payment::id, 90),
                new Column<Payment>("Thời gian", p -> DATE_TIME.format(p.time()), 120), new Column<Payment>("Phương thức", p -> p.method().label(), 100),
                new Column<Payment>("Số tiền", p -> new DemoTable.Strong(Kit.money(p.amount())), 110),
                new Column<Payment>("Trạng thái", p -> new Tag(p.status().label(), p.status() == PayStatus.THANH_CONG ? Theme.GREEN : Theme.MUTED), 105)),
                Payment::id, 4);
        draftTable = new DemoTable<>("inv-create-lines", List.of(
                new Column<Line>("Dịch vụ", Line::name, 180),
                new Column<Line>("SL", Line::qty, 40),
                new Column<Line>("Đơn giá", l -> Kit.money(l.unitPrice()), 110),
                new Column<Line>("Thành tiền", l -> new DemoTable.Strong(Kit.money(l.amount())), 110)),
                Line::serviceId, 4);
        createTotal.setName("inv-create-total");
        createPatient.addActionListener(e -> refreshEligibleTickets());
        createQty.setText("1");
        JButton addLine = secondary("Thêm dịch vụ", this::addDraftLine); addLine.setName("inv-create-add");
        JButton removeLine = secondary("Bỏ dòng đã chọn", this::removeDraftLine); removeLine.setName("inv-create-remove");
        JButton saveInvoice = primary("Lưu hóa đơn", this::saveCreatedInvoice); saveInvoice.setName("inv-create-save");
        JButton cancelInvoice = secondary("Đóng", this::closeCreate); cancelInvoice.setName("inv-create-close");
        JButton newInvoice = icon(primary("Lập hóa đơn", this::openCreate), "plus", Color.WHITE); newInvoice.setName("inv-new");
        side.setOpaque(false);
        side.add(Kit.card("Chi tiết hóa đơn", null, column(10, detail, feedback)), "detail");
        side.add(Kit.card("Lập hóa đơn mới", null, column(10,
                form("Bệnh nhân *", createPatient),
                form("Phiếu khám (không bắt buộc, phải đã kết luận)", createTicket),
                pair(form("Dịch vụ *", createService), form("Số lượng *", createQty)),
                buttons(addLine, removeLine),
                Kit.section("Chi tiết hóa đơn dự kiến"), draftTable, createTotal,
                label("<html>Giá được chốt khi lưu; hóa đơn mới ở trạng thái Chưa thanh toán. Mọi dữ liệu chỉ là mô phỏng.</html>",
                        11, false, Theme.MUTED),
                buttons(saveInvoice, cancelInvoice), createFeedback)), "form");
        search.field().getDocument().addDocumentListener(SimpleDocumentListener.of(this::refreshList));
        search.onSubmit(() -> list.selectFirst()); search.bindShortcut(this);
        filter.onChange(i -> refreshList());
        add(Kit.page(this, "Hóa đơn & thu tiền", "Công nợ tính trên từng hóa đơn còn hiệu lực · hóa đơn chờ duyệt hủy vẫn tính nợ", ctx, "inv",
                "Thu tiền / hủy giao dịch chỉ đổi bộ nhớ phiên; không gọi cổng thanh toán, không in hóa đơn thật.", stats,
                Kit.twoCol(Kit.card("Hóa đơn", newInvoice, column(10, search, filter, list)),
                        side, 0.42)), BorderLayout.NORTH);
        store.addListener(this::refresh);
        refresh();
    }
    private BillingSlice billing() { return store.billing(); }
    private Tag statusTag(String id) {
        InvoiceStatus s = billing().status(id);
        Color c = switch (s) { case CHUA_THANH_TOAN -> Theme.RED; case MOT_PHAN -> Theme.ORANGE; case DA_THANH_TOAN -> Theme.GREEN; case DA_HUY -> Theme.MUTED; };
        return new Tag(billing().pendingRequest(id).isPresent() ? s.label() + " · chờ hủy" : s.label(), c);
    }
    private void refresh() { refreshStats(); refreshList(); if (!creating) render(); }
    private void refreshStats() {
        stats.removeAll();
        var d = store.today();
        stats.add(stat("inv-stat-received", "Thu hợp lệ hôm nay", billing().receivedOn(d, null), Theme.GREEN, null, false));
        for (Method m : Method.values()) stats.add(stat("inv-stat-" + m.name(), m.label(), billing().receivedOn(d, m), Theme.TEXT, null, false));
        stats.add(stat("inv-stat-debt", "Công nợ (" + billing().debtInvoiceCount() + " hóa đơn)", billing().totalDebt(), Theme.RED, () -> filter.select(1), false));
        // Số tiền hiển thị dạng tiền tệ thay cho số đếm.
        for (Component c : stats.getComponents()) relabelMoney((Container) c);
        stats.revalidate();
    }
    private static void relabelMoney(Container c) {
        for (Component ch : c.getComponents()) {
            if (ch instanceof JLabel l && l.getName() != null && l.getName().startsWith("inv-stat-")) l.setText(Theme.money(Kit.parseMoney(l.getText())));
            if (ch instanceof Container k) relabelMoney(k);
        }
    }
    private void refreshList() {
        String q = MenuDefinition.normalize(search.field().getText()); int f = filter.selected();
        List<Invoice> rows = billing().invoices().stream()
                .filter(i -> q.isEmpty() || MenuDefinition.normalize(i.id() + " " + i.patientId() + " " + store.patientName(i.patientId())).contains(q))
                .filter(i -> switch (f) {
                    case 1 -> billing().debt(i.id()) > 0;
                    case 2 -> billing().status(i.id()) == InvoiceStatus.DA_THANH_TOAN;
                    case 3 -> billing().pendingRequest(i.id()).isPresent();
                    case 4 -> billing().status(i.id()) == InvoiceStatus.DA_HUY;
                    default -> true;
                }).toList();
        list.setRows(rows, "Không có hóa đơn khớp bộ lọc");
    }
    private void render() {
        Invoice inv = billing().invoice(selected).orElse(null);
        if (inv == null) { Kit.fill(detail, Kit.empty("Chọn một hóa đơn để xem chi tiết và thu tiền")); return; }
        if (list.selected().map(Invoice::id).filter(selected::equals).isEmpty()) list.select(selected);
        lines.setRows(inv.lines(), "—");
        payments.setRows(billing().payments(inv.id()), "Chưa có giao dịch");
        String req = billing().pendingRequest(inv.id()).map(r -> r.id() + " · Chờ duyệt (" + store.staff().name(r.creatorId()) + ")")
                .orElse(billing().requests().stream().filter(r -> r.invoiceId().equals(inv.id())).findFirst().map(r -> r.id() + " · " + r.status().label()).orElse("Không có"));
        String pb = billing().payBlocker(inv.id());
        JButton pay = icon(primary("Ghi nhận thu tiền", this::pay), "wallet", Color.WHITE); pay.setName("inv-pay");
        pay.setEnabled(pb == null); pay.setToolTipText(pb == null ? "Không thu quá số còn nợ" : pb);
        JButton voidPay = secondary("Hủy giao dịch đã chọn", this::voidPayment); voidPay.setName("inv-void");
        JButton request = danger("Yêu cầu hủy hóa đơn", this::requestCancel); request.setName("inv-request-cancel");
        request.setEnabled(!inv.cancelled() && billing().pendingRequest(inv.id()).isEmpty());
        JLabel why = feedback("inv-pay-reason");
        if (pb != null) hint(why, pb); else info(why, " ");
        if (pb == null) amount.setText(String.valueOf(billing().debt(inv.id())));
        Kit.fill(detail, column(12, Kit.head("inv", inv.id() + " · " + store.patientName(inv.patientId()),
                        inv.patientId() + (inv.ticketId() == null ? "" : " · " + inv.ticketId()) + " · lập " + DATE_TIME.format(inv.created()), statusTag(inv.id())),
                Kit.info("inv-info", "Tổng tiền", Kit.money(inv.total()), "Đã thu (thành công)", Kit.money(billing().paid(inv.id())),
                        "Còn nợ", Kit.money(billing().debt(inv.id())), "Trạng thái thanh toán", billing().status(inv.id()).label(), "Yêu cầu hủy", req),
                Kit.section("Dòng hóa đơn"), lines, Kit.section("Lịch sử giao dịch"), payments, Kit.separator(),
                Kit.section("Thu tiền (mô phỏng)"), pair(form("Phương thức", method), form("Số tiền (đ)", amount)), form("Mã giao dịch (CK / POS)", reference),
                buttons(pay, voidPay, request), why));
    }
    /** Mở form lập hóa đơn, không đụng tới dữ liệu thật hoặc giao dịch thu tiền. */
    private void openCreate() {
        search.field().setText("");
        filter.select(0);
        draft.clear();
        replaceItems(createPatient, store.patients().stream()
                .map(p -> p.id() + " · " + p.name()).toList());
        replaceItems(createService, billing().services().stream().filter(Service::active)
                .map(s -> s.id() + " · " + s.name() + " (" + Kit.money(s.price()) + ")").toList());
        createQty.setText("1");
        refreshEligibleTickets();
        refreshDraft();
        info(createFeedback, "Chọn bệnh nhân và thêm ít nhất một dịch vụ; chưa ghi nhận thanh toán.");
        creating = true;
        ((CardLayout) side.getLayout()).show(side, "form");
        revalidate(); repaint();
    }
    private void closeCreate() {
        creating = false;
        ((CardLayout) side.getLayout()).show(side, "detail");
        revalidate(); repaint();
    }
    private void refreshEligibleTickets() {
        String patientId = Kit.idOf(createPatient.getSelectedItem());
        List<String> options = new ArrayList<>();
        options.add("(Không gắn phiếu khám)");
        if (patientId != null) store.ticketsOf(patientId).stream()
                .filter(t -> t.status() == DemoClinicStore.TicketStatus.DA_KET_LUAN)
                .filter(t -> billing().invoices().stream().noneMatch(i -> !i.cancelled() && t.id().equals(i.ticketId())))
                .forEach(t -> options.add(t.id() + " · Đã kết luận"));
        replaceItems(createTicket, options);
    }
    private void addDraftLine() {
        String serviceId = Kit.idOf(createService.getSelectedItem());
        int qty = Kit.parseInt(createQty.getText());
        Service service = billing().service(serviceId).orElse(null);
        if (service == null || !service.active()) { fail(createFeedback, "Chọn dịch vụ đang áp dụng"); return; }
        if (qty < 1 || qty > 99) { fail(createFeedback, "Số lượng phải từ 1 đến 99"); return; }
        if (draft.size() >= 20) { fail(createFeedback, "Tối đa 20 dòng dịch vụ"); return; }
        if (draft.stream().anyMatch(d -> d.serviceId().equals(serviceId))) {
            fail(createFeedback, "Dịch vụ đã có trong hóa đơn; hãy bỏ dòng trước khi thêm lại"); return;
        }
        draft.add(new InvoiceLineDraft(serviceId, qty));
        refreshDraft();
        info(createFeedback, "Đã thêm " + service.name() + " × " + qty);
    }
    private void removeDraftLine() {
        Line selectedLine = draftTable.selected().orElse(null);
        if (selectedLine == null) { fail(createFeedback, "Chọn dòng dịch vụ cần bỏ"); return; }
        draft.removeIf(d -> d.serviceId().equals(selectedLine.serviceId()));
        refreshDraft();
        info(createFeedback, "Đã bỏ dòng " + selectedLine.serviceId());
    }
    private void refreshDraft() {
        List<Line> lines = draft.stream().map(d -> billing().service(d.serviceId())
                .map(s -> new Line(s.id(), s.name(), d.qty(), s.price())).orElseThrow()).toList();
        draftTable.setRows(lines, "Chưa có dịch vụ · nhấn Thêm dịch vụ");
        createTotal.setText("Tổng tạm tính: " + Kit.money(lines.stream().mapToLong(Line::amount).sum()));
    }
    private void saveCreatedInvoice() {
        // Luôn tính lại tổng theo bảng giá mới nhất ngay trước xác nhận.
        refreshDraft();
        String patientId = Kit.idOf(createPatient.getSelectedItem());
        String ticketId = createTicket.getSelectedIndex() <= 0 ? null : Kit.idOf(createTicket.getSelectedItem());
        if (draft.isEmpty()) { fail(createFeedback, "Hãy thêm ít nhất một dịch vụ"); return; }
        if (!ctx.dialogs().confirm(this, "Lập hóa đơn", "Lập hóa đơn cho " + store.patientName(patientId)
                + " với tổng dự kiến " + createTotal.getText() + "? Chưa thu tiền; không ghi SQL Server.")) {
            info(createFeedback, "Đã hủy thao tác"); return;
        }
        DemoClinicStore.Result<Invoice> r = billing().createInvoice(patientId, ticketId, List.copyOf(draft));
        result(createFeedback, r);
        if (r.ok()) {
            closeCreate();
            refreshList();
            list.select(r.value().id());
            result(feedback, r);
        }
    }
    private void pay() {
        long a = Kit.parseMoney(amount.getText());
        Method m = (Method) method.getSelectedItem();
        if (!ctx.dialogs().confirm(this, "Thu tiền", "Ghi nhận " + Kit.money(Math.max(a, 0)) + " (" + m + ") cho " + selected + "?")) { info(feedback, "Đã hủy thao tác"); return; }
        var r = billing().pay(selected, m, a, reference.getText()); result(feedback, r); if (r.ok()) reference.setText("");
    }
    private void voidPayment() {
        Payment p = payments.selected().orElse(null);
        if (p == null) { fail(feedback, "Chọn một giao dịch trong lịch sử"); return; }
        String r = ctx.dialogs().reason(this, "Hủy giao dịch " + p.id(), "Lý do hủy (5–200 ký tự). Giao dịch vẫn được lưu dấu vết:");
        if (r == null) { info(feedback, "Đã hủy thao tác"); return; }
        result(feedback, billing().voidPayment(p.id(), r));
    }
    private void requestCancel() {
        String r = ctx.dialogs().reason(this, "Yêu cầu hủy " + selected, "Lý do (5–200 ký tự). Hóa đơn vẫn tính công nợ tới khi được duyệt:");
        if (r == null) { info(feedback, "Đã hủy thao tác"); return; }
        result(feedback, billing().requestCancel(selected, StaffSlice.ADMIN_ID, r));
    }
}
