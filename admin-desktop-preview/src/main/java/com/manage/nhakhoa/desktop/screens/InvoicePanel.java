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
        list.onSelect(i -> { selected = i.id(); render(); });
        lines = new DemoTable<>("inv-lines", List.of(new Column<Line>("Dịch vụ", Line::name, 200), new Column<Line>("SL", Line::qty, 44),
                new Column<Line>("Đơn giá (đã chốt)", l -> Kit.money(l.unitPrice()), 120), new Column<Line>("Thành tiền", l -> new DemoTable.Strong(Kit.money(l.amount())), 120)),
                Line::serviceId, 4);
        payments = new DemoTable<>("inv-payments", List.of(new Column<Payment>("Mã GD", Payment::id, 90),
                new Column<Payment>("Thời gian", p -> DATE_TIME.format(p.time()), 120), new Column<Payment>("Phương thức", p -> p.method().label(), 100),
                new Column<Payment>("Số tiền", p -> new DemoTable.Strong(Kit.money(p.amount())), 110),
                new Column<Payment>("Trạng thái", p -> new Tag(p.status().label(), p.status() == PayStatus.THANH_CONG ? Theme.GREEN : Theme.MUTED), 105)),
                Payment::id, 4);
        search.field().getDocument().addDocumentListener(SimpleDocumentListener.of(this::refreshList));
        search.onSubmit(() -> list.selectFirst()); search.bindShortcut(this);
        filter.onChange(i -> refreshList());
        add(Kit.page(this, "Hóa đơn & thu tiền", "Công nợ tính trên từng hóa đơn còn hiệu lực · hóa đơn chờ duyệt hủy vẫn tính nợ", ctx, "inv",
                "Thu tiền / hủy giao dịch chỉ đổi bộ nhớ phiên; không gọi cổng thanh toán, không in hóa đơn thật.", stats,
                Kit.twoCol(Kit.card("Hóa đơn", null, column(10, search, filter, list)),
                        Kit.card("Chi tiết hóa đơn", null, column(10, detail, feedback)), 0.42)), BorderLayout.NORTH);
        store.addListener(this::refresh);
        refresh();
    }
    private BillingSlice billing() { return store.billing(); }
    private Tag statusTag(String id) {
        InvoiceStatus s = billing().status(id);
        Color c = switch (s) { case CHUA_THANH_TOAN -> Theme.RED; case MOT_PHAN -> Theme.ORANGE; case DA_THANH_TOAN -> Theme.GREEN; case DA_HUY -> Theme.MUTED; };
        return new Tag(billing().pendingRequest(id).isPresent() ? s.label() + " · chờ hủy" : s.label(), c);
    }
    private void refresh() { refreshStats(); refreshList(); render(); }
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
