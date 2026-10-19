package com.manage.nhakhoa.desktop.screens;

import com.manage.nhakhoa.desktop.MenuDefinition;
import com.manage.nhakhoa.desktop.demo.ClinicalSlice;
import com.manage.nhakhoa.desktop.demo.ClinicalSlice.*;
import com.manage.nhakhoa.desktop.demo.DemoClinicStore;
import com.manage.nhakhoa.desktop.demo.DemoClinicStore.ExamTicket;
import com.manage.nhakhoa.desktop.demo.DemoClinicStore.TicketStatus;
import com.manage.nhakhoa.desktop.ui.CardList;
import com.manage.nhakhoa.desktop.ui.DemoTable.Tag;
import com.manage.nhakhoa.desktop.ui.Flat;
import com.manage.nhakhoa.desktop.ui.Theme;
import java.awt.*;
import java.util.Comparator;
import java.util.List;
import javax.swing.*;
import static com.manage.nhakhoa.desktop.ui.DemoWidgets.*;
import static com.manage.nhakhoa.desktop.ui.UiComponents.*;

/**
 * Khám · Phiếu khám (PHIEU_KHAM): tab theo trạng thái, chi tiết bệnh nhân / bác sĩ phụ trách, chỉ định cận lâm sàng và kết quả,
 * chẩn đoán & kết luận mẫu. "Kết luận" chỉ bật khi đủ chẩn đoán và mọi kết quả CLS. Quản trị không ký thay bác sĩ.
 */
public final class ExamPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private static final List<String> CLS = List.of("Chụp X-quang quanh chóp", "Chụp phim toàn cảnh");
    private final transient ScreenContext ctx;
    private final transient DemoClinicStore store;
    private final Flat.SearchBar search = new Flat.SearchBar("exam-search", "Tìm mã phiếu, mã / tên bệnh nhân…");
    private final Flat.Segmented tabs = new Flat.Segmented("exam-tab", List.of("Chờ khám", "Đang khám", "Đã kết luận", "Đã hủy"), null);
    private final CardList<ExamTicket> list;
    private final JPanel stats = Kit.stats();
    private final JPanel detail = Kit.holder("exam-detail");
    private final JLabel feedback = feedback("exam-feedback");
    private final JTextField diagnosis = field("exam-diagnosis", 20, "Chẩn đoán của bác sĩ phụ trách (mẫu)");
    private final JTextField conclusion = field("exam-conclusion", 20, "Kết luận (mẫu)");
    private final JTextField clsResult = field("exam-cls-result", 20, "Kết quả cận lâm sàng (mẫu)");
    private final JComboBox<String> clsService = combo("exam-cls-service", CLS);
    private final JComboBox<String> pendingOrder = combo("exam-cls-order", List.of());
    private transient String selected;

    public ExamPanel(ScreenContext ctx) {
        super(new BorderLayout()); setOpaque(false); setName("screen-PHIEU_KHAM");
        this.ctx = ctx; this.store = ctx.store();
        list = Kit.list("exam-list", ExamTicket::id, t -> Kit.Spec.person(store.patientName(t.patientId()),
                t.id() + "  ·  " + DATE_TIME.format(t.receivedAt()) + "  ·  " + store.doctorName(t.doctorId()), tag(t.status()),
                store.patientName(t.patientId()), t.patientId()), 7);
        list.onSelect(t -> { selected = t.id(); render(); });
        search.field().getDocument().addDocumentListener(SimpleDocumentListener.of(this::refreshList));
        search.onSubmit(() -> list.selectFirst()); search.bindShortcut(this);
        tabs.onChange(i -> { selected = null; refresh(); });
        add(Kit.page(this, "Phiếu khám", "Chờ khám → Đang khám → Đã kết luận · chỉ định cận lâm sàng · bác sĩ phụ trách", ctx, "exam",
                "Chẩn đoán / kết luận là văn bản mẫu; thao tác chuyên môn ghi theo bác sĩ phụ trách, không ký thật.", stats,
                Kit.twoCol(Kit.card("Danh sách phiếu", null, column(10, search, tabs, list)),
                        Kit.card("Chi tiết phiếu khám", null, column(10, detail, feedback)), 0.44)), BorderLayout.NORTH);
        store.addListener(this::refresh);
        tabs.select(1); // mặc định mở tab Đang khám
    }
    static Tag tag(TicketStatus s) { return new Tag(s.label(), tone(s)); }
    private ClinicalSlice clinical() { return store.clinical(); }
    private void refresh() { refreshStats(); refreshList(); render(); }
    private void refreshStats() {
        stats.removeAll();
        List<ExamTicket> today = store.ticketsOn(store.today());
        for (TicketStatus s : TicketStatus.values()) {
            int idx = s.ordinal();
            stats.add(stat("exam-stat-" + s.name(), s.label() + " (hôm nay)", today.stream().filter(t -> t.status() == s).count(), tone(s),
                    () -> tabs.select(idx), tabs.selected() == idx));
        }
        stats.revalidate();
    }
    private void refreshList() {
        String q = MenuDefinition.normalize(search.field().getText());
        TicketStatus st = TicketStatus.values()[tabs.selected()];
        List<ExamTicket> rows = store.tickets().stream().filter(t -> t.status() == st)
                .filter(t -> q.isEmpty() || MenuDefinition.normalize(t.id() + " " + t.patientId() + " " + store.patientName(t.patientId())).contains(q))
                .sorted(Comparator.comparing(ExamTicket::receivedAt).reversed()).toList();
        list.setRows(rows, "Không có phiếu " + st.label().toLowerCase());
        if (selected == null && !rows.isEmpty()) { selected = rows.get(0).id(); }
    }
    private void render() {
        ExamTicket t = store.ticket(selected).orElse(null);
        if (t == null) { Kit.fill(detail, Kit.empty("Chọn một phiếu khám để xem chi tiết")); return; }
        if (list.selected().map(ExamTicket::id).filter(selected::equals).isEmpty()) list.select(selected);
        ExamRecord rec = clinical().record(t.id());
        List<Order> orders = clinical().orders(t.id());
        JPanel ordersBox = new JPanel(); ordersBox.setOpaque(false); ordersBox.setLayout(new BoxLayout(ordersBox, BoxLayout.Y_AXIS));
        if (orders.isEmpty()) ordersBox.add(label("Không có chỉ định cận lâm sàng", 12, false, Theme.MUTED));
        for (Order o : orders) {
            JPanel row = new JPanel(new BorderLayout(8, 0)); row.setOpaque(false);
            row.add(label(o.id() + " · " + o.serviceName() + (o.result() == null ? "" : " → " + o.result()), 12, false, Theme.TEXT), BorderLayout.CENTER);
            row.add(new Flat.Pill(o.status().label(), o.status() == OrderStatus.CO_KET_QUA ? Theme.GREEN : Theme.ORANGE), BorderLayout.EAST);
            row.setAlignmentX(Component.LEFT_ALIGNMENT); ordersBox.add(row);
        }
        JComponent actions;
        String doctor = store.doctorName(t.doctorId());
        switch (t.status()) {
            case CHO_KHAM -> {
                JButton start = icon(primary("Bắt đầu khám", () -> result(feedback, clinical().startExam(t.id()))), "tooth", Color.WHITE); start.setName("exam-start");
                JButton cancel = danger("Hủy phiếu", this::cancelTicket); cancel.setName("exam-cancel");
                actions = column(8, label("Bác sĩ phụ trách: " + doctor, 12, true, Theme.TEXT), buttons(start, cancel));
            }
            case DANG_KHAM -> {
                JButton saveDx = secondary("Ghi chẩn đoán", () -> result(feedback, clinical().saveDiagnosis(t.id(), diagnosis.getText()))); saveDx.setName("exam-save-diagnosis");
                JButton addCls = secondary("Thêm chỉ định", () -> result(feedback, clinical().addOrder(t.id(), (String) clsService.getSelectedItem())));
                addCls.setName("exam-add-order");
                replaceItems(pendingOrder, orders.stream().filter(o -> o.status() == OrderStatus.CHO_KET_QUA).map(o -> o.id() + " · " + o.serviceName()).toList());
                JButton enter = secondary("Nhập kết quả", () -> {
                    String id = Kit.idOf(pendingOrder.getSelectedItem());
                    if (id == null) { fail(feedback, "Không có chỉ định chờ kết quả"); return; }
                    var r = clinical().enterResult(id, clsResult.getText()); result(feedback, r); if (r.ok()) clsResult.setText("");
                });
                enter.setName("exam-enter-result"); enter.setEnabled(pendingOrder.getItemCount() > 0);
                String blocker = clinical().concludeBlocker(t.id());
                JButton conclude = icon(primary("Kết luận (mô phỏng)", this::conclude), "check", Color.WHITE); conclude.setName("exam-conclude");
                conclude.setEnabled(blocker == null); conclude.setToolTipText(blocker == null ? "Đủ chẩn đoán và kết quả CLS" : blocker);
                JLabel why = feedback("exam-conclude-reason");
                if (blocker != null) warn(why, "Chưa thể kết luận: " + blocker); else hint(why, "Đủ điều kiện kết luận");
                if (diagnosis.getText().isBlank()) diagnosis.setText(rec.diagnosis());
                actions = column(10, label("Ghi nhận theo bác sĩ phụ trách: " + doctor + " — Quản trị không ký thay", 12, true, Theme.ORANGE),
                        form("Chẩn đoán", diagnosis), buttons(saveDx),
                        pair(form("Chỉ định cận lâm sàng", clsService), form("Chỉ định chờ kết quả", pendingOrder)),
                        form("Kết quả", clsResult), buttons(addCls, enter),
                        form("Kết luận", conclusion), buttons(conclude), why);
            }
            default -> actions = label(t.status() == TicketStatus.DA_HUY ? "Phiếu đã hủy — chỉ xem" : "Phiếu đã kết luận — chỉ xem, không sửa", 12, false, Theme.MUTED);
        }
        Kit.fill(detail, column(12, Kit.personHead("exam", store.patientName(t.patientId()), t.patientId(),
                        t.patientId() + " · " + t.id(), tag(t.status())),
                Kit.info("exam-info", "Tiếp nhận", DATE_TIME.format(t.receivedAt()) + (t.appointmentId() == null ? " · trực tiếp" : " · theo " + t.appointmentId()),
                        "Bác sĩ phụ trách", doctor, "Lý do đến khám", t.visitReason(), "Chẩn đoán", rec.diagnosis(), "Kết luận", rec.conclusion()),
                Kit.separator(), Kit.section("Chỉ định cận lâm sàng"), ordersBox, Kit.separator(), actions));
    }
    private void conclude() {
        if (!ctx.dialogs().confirm(this, "Kết luận phiếu", "Kết luận " + selected + " theo bác sĩ phụ trách? (mô phỏng, không ký thật)")) {
            info(feedback, "Đã hủy thao tác"); return;
        }
        var r = clinical().conclude(selected, conclusion.getText()); result(feedback, r);
        if (r.ok()) { diagnosis.setText(""); conclusion.setText(""); }
    }
    private void cancelTicket() {
        String r = ctx.dialogs().reason(this, "Hủy phiếu " + selected, "Lý do hủy (5–200 ký tự):");
        if (r == null) { info(feedback, "Đã hủy thao tác"); return; }
        result(feedback, clinical().cancelTicket(selected, r));
    }
    /** Mở phiếu cụ thể (dùng từ màn khác / kiểm thử). */
    public void open(String ticketId) {
        store.ticket(ticketId).ifPresent(t -> { tabs.select(t.status().ordinal()); selected = ticketId; refreshList(); render(); });
    }
}
