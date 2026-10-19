package com.manage.nhakhoa.desktop.screens;

import com.manage.nhakhoa.desktop.MenuDefinition;
import com.manage.nhakhoa.desktop.demo.ClinicalSlice;
import com.manage.nhakhoa.desktop.demo.ClinicalSlice.*;
import com.manage.nhakhoa.desktop.demo.DemoClinicStore;
import com.manage.nhakhoa.desktop.ui.CardList;
import com.manage.nhakhoa.desktop.ui.DemoTable.Tag;
import com.manage.nhakhoa.desktop.ui.Flat;
import com.manage.nhakhoa.desktop.ui.Theme;
import java.awt.*;
import java.time.LocalDate;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import static com.manage.nhakhoa.desktop.ui.DemoWidgets.*;
import static com.manage.nhakhoa.desktop.ui.UiComponents.*;

/**
 * Điều trị · Kế hoạch điều trị (KE_HOACH): danh sách kế hoạch, bác sĩ phụ trách, hạng mục / răng / ngày dự kiến, tiến độ,
 * đến hạn / quá hạn, tổng chi phí dự kiến. Hoàn thành hạng mục ghi theo bác sĩ phụ trách; chỉ hoàn tất kế hoạch khi hết hạng mục chưa làm.
 */
public final class PlanPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private final transient ScreenContext ctx;
    private final transient DemoClinicStore store;
    private final Flat.SearchBar search = new Flat.SearchBar("plan-search", "Tìm mã kế hoạch, bệnh nhân…");
    private final Flat.Segmented filter = new Flat.Segmented("plan-filter", List.of("Đang thực hiện", "Hoàn thành", "Đã hủy", "Tất cả"), null);
    private final CardList<Plan> list;
    private final CardList<PlanItem> itemList;
    private final JPanel detail = Kit.holder("plan-detail");
    private final JLabel feedback = feedback("plan-feedback");
    private final JTextField newDate = field("plan-new-date", 10, "dd/MM/yyyy");
    private transient String selected;

    public PlanPanel(ScreenContext ctx) {
        super(new BorderLayout()); setOpaque(false); setName("screen-KE_HOACH");
        this.ctx = ctx; this.store = ctx.store();
        list = Kit.list("plan-list", Plan::id, p -> {
            long done = clinical().doneCount(p.id()), all = clinical().activeCount(p.id());
            return Kit.Spec.person(store.patientName(p.patientId()), p.id() + "  ·  " + p.title() + "  ·  " + done + "/" + all + " hạng mục",
                    tag(p.status()), store.patientName(p.patientId()), p.patientId());
        }, 7);
        itemList = Kit.list("plan-items", PlanItem::id, i -> Kit.Spec.block(DATE.format(i.planned()).substring(0, 5), i.serviceName() + " · răng " + i.tooth(),
                i.id() + "  ·  " + Kit.money(i.price()) + (i.doneBy() == null ? "" : "  ·  " + store.doctorName(i.doneBy())), itemTag(i)), 5);
        list.onSelect(p -> { selected = p.id(); render(); });
        itemList.onSelect(i -> { newDate.setText(DATE.format(i.planned())); renderActions(); });
        search.field().getDocument().addDocumentListener(SimpleDocumentListener.of(this::refreshList));
        search.onSubmit(() -> list.selectFirst()); search.bindShortcut(this);
        filter.onChange(i -> refreshList());
        add(Kit.page(this, "Kế hoạch điều trị", "Hạng mục theo răng, ngày dự kiến, tiến độ và chi phí dự kiến · bác sĩ phụ trách", ctx, "plan",
                "Đánh dấu hoàn thành chỉ đổi bộ nhớ phiên, ghi theo bác sĩ phụ trách; không phải hồ sơ bệnh án thật.",
                Kit.twoCol(Kit.card("Kế hoạch", null, column(10, search, filter, list)),
                        Kit.card("Chi tiết kế hoạch", null, column(10, detail, feedback)), 0.44)), BorderLayout.NORTH);
        store.addListener(this::refresh);
        refresh();
    }
    static Tag tag(PlanStatus s) {
        return new Tag(s.label(), switch (s) { case DANG_THUC_HIEN -> Theme.PRIMARY_TEXT; case HOAN_THANH -> Theme.GREEN; case DA_HUY -> Theme.MUTED; });
    }
    private Tag itemTag(PlanItem i) {
        if (i.status() == ItemStatus.DA_LAM) return new Tag("Đã làm", Theme.GREEN);
        if (i.status() == ItemStatus.DA_HUY) return new Tag("Đã hủy", Theme.MUTED);
        DueState d = clinical().dueState(i);
        return new Tag(d.label(), switch (d) { case QUA_HAN -> Theme.RED; case DEN_HAN -> Theme.ORANGE; case SAP_TOI -> Theme.color("0369A1"); case CHUA_DEN -> Theme.MUTED; });
    }
    private ClinicalSlice clinical() { return store.clinical(); }
    private void refresh() { refreshList(); render(); }
    private void refreshList() {
        String q = MenuDefinition.normalize(search.field().getText()); int f = filter.selected();
        List<Plan> rows = clinical().plans().stream()
                .filter(p -> q.isEmpty() || MenuDefinition.normalize(p.id() + " " + p.patientId() + " " + store.patientName(p.patientId()) + " " + p.title()).contains(q))
                .filter(p -> f == 3 || p.status() == PlanStatus.values()[f]).toList();
        list.setRows(rows, "Không có kế hoạch khớp bộ lọc");
    }
    private void render() {
        Plan p = clinical().plan(selected).orElse(null);
        if (p == null) { Kit.fill(detail, Kit.empty("Chọn một kế hoạch để xem hạng mục")); return; }
        if (list.selected().map(Plan::id).filter(selected::equals).isEmpty()) list.select(selected);
        List<PlanItem> items = clinical().items(p.id());
        itemList.setRows(items, "Kế hoạch chưa có hạng mục");
        long done = clinical().doneCount(p.id()), all = clinical().activeCount(p.id());
        JProgressBar bar = new JProgressBar(0, (int) Math.max(1, all)); bar.setValue((int) done); bar.setStringPainted(true);
        bar.setString(done + "/" + all + " hạng mục"); bar.setForeground(Theme.PRIMARY); bar.setBackground(Theme.SOFT);
        bar.setBorder(new EmptyBorder(0, 0, 0, 0)); bar.setName("plan-progress");
        long due = items.stream().filter(i -> i.status() == ItemStatus.CHUA_LAM && clinical().dueState(i) != DueState.CHUA_DEN && clinical().dueState(i) != DueState.SAP_TOI).count();
        Kit.fill(detail, column(12, Kit.personHead("plan", store.patientName(p.patientId()), p.patientId(), p.id() + " · " + p.title(), tag(p.status())),
                Kit.info("plan-info", "Bác sĩ phụ trách", store.doctorName(p.doctorId()), "Ngày lập", DATE.format(p.created()),
                        "Chi phí dự kiến", Kit.money(clinical().planTotal(p.id())) + " (không gồm hạng mục đã hủy)",
                        "Đến hạn (gồm quá hạn)", due + " hạng mục trong 3 ngày tới hoặc đã quá hạn"),
                bar, Kit.section("Hạng mục"), itemList, actionsBox));
        renderActions();
    }
    private final JPanel actionsBox = Kit.holder("plan-actions");
    private void renderActions() {
        Plan p = clinical().plan(selected).orElse(null);
        if (p == null) return;
        PlanItem i = itemList.selected().orElse(null);
        JButton done = icon(primary("Đánh dấu đã làm", this::completeItem), "check", Color.WHITE); done.setName("plan-item-done");
        JButton move = secondary("Dời ngày", this::moveItem); move.setName("plan-item-move");
        JButton finish = secondary("Hoàn tất kế hoạch", this::finishPlan); finish.setName("plan-finish");
        String ib = i == null ? "Chọn một hạng mục" : clinical().completeItemBlocker(i.id());
        done.setEnabled(ib == null); done.setToolTipText(ib == null ? "Ghi theo bác sĩ phụ trách " + store.doctorName(p.doctorId()) : ib);
        move.setEnabled(i != null && i.status() == ItemStatus.CHUA_LAM);
        String pb = clinical().completePlanBlocker(p.id());
        finish.setEnabled(pb == null); finish.setToolTipText(pb == null ? "Chuyển kế hoạch sang Hoàn thành" : pb);
        JLabel why = feedback("plan-finish-reason");
        if (pb != null && p.status() == PlanStatus.DANG_THUC_HIEN) hint(why, "Chưa hoàn tất được: " + pb); else info(why, " ");
        Kit.fill(actionsBox, column(8, Kit.separator(), pair(form("Ngày dự kiến mới", newDate), new JLabel()), buttons(done, move, finish), why));
    }
    private void completeItem() {
        itemList.selected().ifPresent(i -> {
            Plan p = clinical().plan(i.planId()).orElseThrow();
            if (!ctx.dialogs().confirm(this, "Hoàn thành hạng mục", "Ghi nhận " + i.serviceName() + " đã làm, theo bác sĩ phụ trách " + store.doctorName(p.doctorId()) + "?")) {
                info(feedback, "Đã hủy thao tác"); return;
            }
            result(feedback, clinical().completeItem(i.id()));
        });
    }
    private void moveItem() {
        itemList.selected().ifPresent(i -> {
            LocalDate d = parseDate(newDate.getText());
            result(feedback, clinical().reschedule(i.id(), d));
        });
    }
    private void finishPlan() {
        if (!ctx.dialogs().confirm(this, "Hoàn tất kế hoạch", "Chuyển " + selected + " sang Hoàn thành?")) { info(feedback, "Đã hủy thao tác"); return; }
        result(feedback, clinical().completePlan(selected));
    }
    public void open(String planId, String itemId) {
        filter.select(3); selected = planId; refreshList(); render(); if (itemId != null) itemList.select(itemId);
    }
}
