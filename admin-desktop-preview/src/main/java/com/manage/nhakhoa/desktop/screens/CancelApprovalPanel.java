package com.manage.nhakhoa.desktop.screens;

import com.manage.nhakhoa.desktop.MenuDefinition;
import com.manage.nhakhoa.desktop.demo.BillingSlice;
import com.manage.nhakhoa.desktop.demo.BillingSlice.*;
import com.manage.nhakhoa.desktop.demo.DemoClinicStore;
import com.manage.nhakhoa.desktop.demo.StaffSlice.Role;
import com.manage.nhakhoa.desktop.ui.CardList;
import com.manage.nhakhoa.desktop.ui.DemoTable.Tag;
import com.manage.nhakhoa.desktop.ui.Flat;
import com.manage.nhakhoa.desktop.ui.Theme;
import java.awt.*;
import java.util.List;
import javax.swing.*;
import static com.manage.nhakhoa.desktop.ui.DemoWidgets.*;
import static com.manage.nhakhoa.desktop.ui.UiComponents.*;

/**
 * Thanh toán · Duyệt hủy hóa đơn (DUYET_HUY): yêu cầu Chờ duyệt / Đã duyệt / Từ chối; người duyệt phải khác người lập;
 * hóa đơn đã thu tiền không duyệt hủy được cho tới khi hủy/hoàn giao dịch. Không tạo nghiệp vụ hủy thật.
 */
public final class CancelApprovalPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private final transient ScreenContext ctx;
    private final transient DemoClinicStore store;
    private final Flat.SearchBar search = new Flat.SearchBar("cancel-search", "Tìm mã yêu cầu, hóa đơn, bệnh nhân…");
    private final Flat.Segmented filter = new Flat.Segmented("cancel-filter", List.of("Chờ duyệt", "Đã duyệt", "Từ chối", "Tất cả"), null);
    private final CardList<CancelRequest> list;
    private final JPanel detail = Kit.holder("cancel-detail");
    private final JLabel feedback = feedback("cancel-feedback");
    private final JComboBox<String> actor;
    private transient String selected;

    public CancelApprovalPanel(ScreenContext ctx) {
        super(new BorderLayout()); setOpaque(false); setName("screen-DUYET_HUY");
        this.ctx = ctx; this.store = ctx.store();
        actor = Kit.actor("cancel-actor", store.staff(), s -> s.role() == Role.QL || s.role() == Role.AD, "NV010");
        actor.addActionListener(e -> render());
        list = Kit.list("cancel-list", CancelRequest::id, r -> {
            Invoice i = billing().invoice(r.invoiceId()).orElseThrow();
            return Kit.Spec.person(store.patientName(i.patientId()), r.id() + "  ·  " + r.invoiceId() + "  ·  lập bởi " + store.staff().name(r.creatorId()),
                    tag(r.status()), store.patientName(i.patientId()), i.patientId());
        }, 6);
        list.onSelect(r -> { selected = r.id(); render(); });
        search.field().getDocument().addDocumentListener(SimpleDocumentListener.of(this::refreshList));
        search.onSubmit(() -> list.selectFirst()); search.bindShortcut(this);
        filter.onChange(i -> refreshList());
        actor.setPreferredSize(new Dimension(300, Flat.FIELD_HEIGHT));
        add(Kit.page(this, "Duyệt hủy hóa đơn", "Người lập ≠ người duyệt · hóa đơn chờ hủy vẫn tính công nợ cho tới khi được duyệt", ctx, "cancel",
                "Duyệt / từ chối là thao tác giả lập theo người thao tác chọn bên dưới, không có đăng nhập thật.",
                buttons(label("Thao tác với tư cách", 12, true, Theme.color("475569")), actor),
                Kit.twoCol(Kit.card("Yêu cầu hủy", null, column(10, search, filter, list)),
                        Kit.card("Chi tiết yêu cầu", null, column(10, detail, feedback)), 0.46)), BorderLayout.NORTH);
        store.addListener(this::refresh);
        refresh();
    }
    static Tag tag(RequestStatus s) {
        return new Tag(s.label(), switch (s) { case CHO_DUYET -> Theme.ORANGE; case DA_DUYET -> Theme.GREEN; case TU_CHOI -> Theme.MUTED; });
    }
    private BillingSlice billing() { return store.billing(); }
    private String actorId() { return Kit.idOf(actor.getSelectedItem()); }
    private void refresh() { refreshList(); render(); }
    private void refreshList() {
        String q = MenuDefinition.normalize(search.field().getText()); int f = filter.selected();
        List<CancelRequest> rows = billing().requests().stream().filter(r -> {
            Invoice i = billing().invoice(r.invoiceId()).orElseThrow();
            return q.isEmpty() || MenuDefinition.normalize(r.id() + " " + r.invoiceId() + " " + i.patientId() + " " + store.patientName(i.patientId())).contains(q);
        }).filter(r -> f == 3 || r.status() == RequestStatus.values()[f]).toList();
        list.setRows(rows, "Không có yêu cầu khớp bộ lọc");
    }
    private void render() {
        CancelRequest r = billing().request(selected).orElse(null);
        if (r == null) { Kit.fill(detail, Kit.empty("Chọn một yêu cầu để xem và quyết định")); return; }
        if (list.selected().map(CancelRequest::id).filter(selected::equals).isEmpty()) list.select(selected);
        Invoice inv = billing().invoice(r.invoiceId()).orElseThrow();
        String ab = billing().approveBlocker(r.id(), actorId()), rb = billing().rejectBlocker(r.id(), actorId());
        JButton approve = icon(primary("Duyệt hủy", this::approve), "check", Color.WHITE); approve.setName("cancel-approve");
        JButton reject = secondary("Từ chối", this::reject); reject.setName("cancel-reject");
        approve.setEnabled(ab == null); reject.setEnabled(rb == null);
        approve.setToolTipText(ab == null ? "Hóa đơn chuyển Đã hủy" : ab); reject.setToolTipText(rb == null ? "Giữ nguyên hóa đơn" : rb);
        JLabel why = feedback("cancel-reason");
        if (ab != null && r.status() == RequestStatus.CHO_DUYET) warn(why, "Không thể duyệt: " + ab); else info(why, " ");
        JButton openInvoice = link("Mở hóa đơn " + inv.id(), () -> ctx.navigate().accept("HOA_DON")); openInvoice.setName("cancel-open-invoice");
        Kit.fill(detail, column(12, Kit.head("cancel", r.id() + " · " + inv.id(), store.patientName(inv.patientId()) + " · " + inv.patientId(), tag(r.status())),
                Kit.info("cancel-info", "Người lập", store.staff().name(r.creatorId()) + " (" + r.creatorId() + ")", "Thời gian", DATE_TIME.format(r.created()),
                        "Lý do", r.reason(), "Tổng hóa đơn", Kit.money(inv.total()), "Đã thu thành công", Kit.money(billing().paid(inv.id())),
                        "Còn nợ hiện tại", Kit.money(billing().debt(inv.id())),
                        "Quyết định", r.deciderId() == null ? "Chưa có" : store.staff().name(r.deciderId()) + " · " + r.decisionNote()),
                openInvoice, Kit.separator(), buttons(approve, reject), why));
    }
    private void approve() {
        if (!ctx.dialogs().confirm(this, "Duyệt hủy", "Duyệt " + selected + " với tư cách " + actor.getSelectedItem() + "? Hóa đơn sẽ chuyển Đã hủy.")) {
            info(feedback, "Đã hủy thao tác"); return;
        }
        result(feedback, billing().approve(selected, actorId()));
    }
    private void reject() {
        String n = ctx.dialogs().reason(this, "Từ chối " + selected, "Lý do từ chối (5–200 ký tự):");
        if (n == null) { info(feedback, "Đã hủy thao tác"); return; }
        result(feedback, billing().reject(selected, actorId(), n));
    }
}
