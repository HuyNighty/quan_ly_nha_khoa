package com.manage.nhakhoa.desktop.screens;

import com.manage.nhakhoa.desktop.MenuDefinition;
import com.manage.nhakhoa.desktop.demo.ClinicalSlice;
import com.manage.nhakhoa.desktop.demo.ClinicalSlice.*;
import com.manage.nhakhoa.desktop.demo.DemoClinicStore;
import com.manage.nhakhoa.desktop.demo.DemoClinicStore.Appointment;
import com.manage.nhakhoa.desktop.ui.CardList;
import com.manage.nhakhoa.desktop.ui.DemoTable.Tag;
import com.manage.nhakhoa.desktop.ui.Flat;
import com.manage.nhakhoa.desktop.ui.Theme;
import java.awt.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import javax.swing.*;
import static com.manage.nhakhoa.desktop.ui.DemoWidgets.*;
import static com.manage.nhakhoa.desktop.ui.UiComponents.*;

/**
 * Điều trị · Tái khám (TAI_KHAM): hạng mục điều trị đến hạn trong 14 ngày (gồm quá hạn) — BA Q12. Phân biệt "hạng mục đến hạn"
 * với "lịch tái khám đã đặt"; tạo lịch hẹn ĐỀ XUẤT (chờ xác nhận) cho hạng mục chưa có lịch.
 */
public final class FollowUpPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private final transient ScreenContext ctx;
    private final transient DemoClinicStore store;
    private final Flat.SearchBar search = new Flat.SearchBar("fu-search", "Tìm bệnh nhân, dịch vụ…");
    private final Flat.Segmented filter = new Flat.Segmented("fu-filter", List.of("Tất cả", "Quá hạn", "Đến hạn", "Chưa có lịch", "Đã có lịch"), null);
    private final CardList<PlanItem> list;
    private final JPanel stats = Kit.stats();
    private final JPanel detail = Kit.holder("fu-detail");
    private final JLabel feedback = feedback("fu-feedback");
    private final JTextField date = field("fu-date", 10, "dd/MM/yyyy");
    private final JTextField time = field("fu-time", 6, "HH:mm");
    private transient String selected;

    public FollowUpPanel(ScreenContext ctx) {
        super(new BorderLayout()); setOpaque(false); setName("screen-TAI_KHAM");
        this.ctx = ctx; this.store = ctx.store();
        list = Kit.list("fu-list", PlanItem::id, i -> {
            Plan p = clinical().plan(i.planId()).orElseThrow();
            String booked = clinical().booking(i.id()).map(a -> "  ·  đã có lịch " + a.id()).orElse("  ·  chưa có lịch");
            return Kit.Spec.person(store.patientName(p.patientId()), DATE.format(i.planned()) + "  ·  " + i.serviceName() + booked,
                    dueTag(i), store.patientName(p.patientId()), p.patientId());
        }, 7);
        list.onSelect(i -> { selected = i.id(); render(); });
        search.field().getDocument().addDocumentListener(SimpleDocumentListener.of(this::refreshList));
        search.onSubmit(() -> list.selectFirst()); search.bindShortcut(this);
        filter.onChange(i -> refreshList());
        add(Kit.page(this, "Tái khám", "Hạng mục kế hoạch điều trị đến hạn trong 14 ngày (bao gồm quá hạn) và lịch hẹn tương ứng", ctx, "fu",
                "Số hạng mục đến hạn KHÁC số lịch tái khám đã đặt; lịch đề xuất chờ lễ tân xác nhận, không tự xác nhận.", stats,
                Kit.twoCol(Kit.card("Hạng mục đến hạn", null, column(10, search, filter, list)),
                        Kit.card("Chi tiết & đề xuất lịch", null, column(10, detail, feedback)), 0.5)), BorderLayout.NORTH);
        store.addListener(this::refresh);
        refresh();
    }
    private ClinicalSlice clinical() { return store.clinical(); }
    private Tag dueTag(PlanItem i) {
        DueState d = clinical().dueState(i);
        return new Tag(d.label(), d == DueState.QUA_HAN ? Theme.RED : d == DueState.DEN_HAN ? Theme.ORANGE : Theme.color("0369A1"));
    }
    private void refresh() { refreshStats(); refreshList(); render(); }
    private void refreshStats() {
        stats.removeAll();
        List<PlanItem> due = clinical().dueItems();
        stats.add(stat("fu-stat-items", "Hạng mục đến hạn (gồm quá hạn)", due.size(), Theme.TEXT, () -> filter.select(0), false));
        stats.add(stat("fu-stat-plans", "Kế hoạch có hạng mục đến hạn", clinical().duePlanCount(), Theme.PRIMARY_TEXT, null, false));
        stats.add(stat("fu-stat-overdue", "Quá hạn", due.stream().filter(i -> clinical().dueState(i) == DueState.QUA_HAN).count(), Theme.RED, () -> filter.select(1), false));
        stats.add(stat("fu-stat-booked", "Đã có lịch hẹn", due.stream().filter(i -> clinical().booking(i.id()).isPresent()).count(), Theme.GREEN, () -> filter.select(4), false));
        stats.revalidate();
    }
    private void refreshList() {
        String q = MenuDefinition.normalize(search.field().getText()); int f = filter.selected();
        List<PlanItem> rows = clinical().dueItems().stream().filter(i -> {
            Plan p = clinical().plan(i.planId()).orElseThrow();
            return q.isEmpty() || MenuDefinition.normalize(p.patientId() + " " + store.patientName(p.patientId()) + " " + i.serviceName()).contains(q);
        }).filter(i -> switch (f) {
            case 1 -> clinical().dueState(i) == DueState.QUA_HAN;
            case 2 -> clinical().dueState(i) == DueState.DEN_HAN;
            case 3 -> clinical().booking(i.id()).isEmpty();
            case 4 -> clinical().booking(i.id()).isPresent();
            default -> true;
        }).toList();
        list.setRows(rows, "Không có hạng mục khớp bộ lọc");
    }
    private void render() {
        PlanItem i = clinical().item(selected).orElse(null);
        if (i == null) { Kit.fill(detail, Kit.empty("Chọn một hạng mục để xem và đề xuất lịch hẹn")); return; }
        if (list.selected().map(PlanItem::id).filter(selected::equals).isEmpty()) list.select(selected);
        Plan p = clinical().plan(i.planId()).orElseThrow();
        Optional<Appointment> booked = clinical().booking(i.id());
        JButton openPlan = link("Mở kế hoạch " + p.id(), () -> ctx.navigate().accept("KE_HOACH")); openPlan.setName("fu-open-plan");
        JButton openAppt = link("Mở Lịch hẹn", () -> ctx.navigate().accept("LICH_HEN")); openAppt.setName("fu-open-appointments");
        JComponent action;
        if (booked.isPresent()) {
            action = column(8, label("Đã có lịch " + booked.get().id() + " · " + DATE_TIME.format(booked.get().time()) + " · " + booked.get().status().label(), 13, true, Theme.GREEN),
                    buttons(openAppt, openPlan));
        } else {
            LocalDate suggest = i.planned().isBefore(store.today().plusDays(1)) ? store.today().plusDays(1) : i.planned();
            date.setText(DATE.format(suggest)); time.setText("09:00");
            JButton propose = icon(primary("Tạo lịch hẹn đề xuất", this::propose), "calendar", Color.WHITE); propose.setName("fu-propose");
            action = column(8, Kit.section("Đề xuất lịch hẹn (trạng thái Chờ xác nhận)"), pair(form("Ngày", date), form("Giờ", time)),
                    buttons(propose, openPlan));
        }
        Kit.fill(detail, column(12, Kit.personHead("fu", store.patientName(p.patientId()), p.patientId(), p.patientId() + " · " + p.id(), dueTag(i)),
                Kit.info("fu-info", "Hạng mục", i.serviceName() + " · răng " + i.tooth(), "Ngày dự kiến", DATE.format(i.planned()),
                        "Kế hoạch", p.title(), "Bác sĩ phụ trách", store.doctorName(p.doctorId()), "Chi phí dự kiến", Kit.money(i.price())),
                Kit.separator(), action));
    }
    private void propose() {
        LocalDate d = parseDate(date.getText()); LocalTime t = parseTime(time.getText());
        if (d == null || t == null) { fail(feedback, "Ngày / giờ không hợp lệ (dd/MM/yyyy, HH:mm)"); return; }
        if (!ctx.dialogs().confirm(this, "Đề xuất lịch hẹn", "Tạo lịch hẹn chờ xác nhận lúc " + DATE_TIME.format(d.atTime(t)) + "?")) {
            info(feedback, "Đã hủy thao tác"); return;
        }
        result(feedback, clinical().proposeAppointment(selected, d.atTime(t)));
    }
}
