package com.manage.nhakhoa.desktop.screens;

import com.manage.nhakhoa.desktop.demo.DemoClinicStore;
import com.manage.nhakhoa.desktop.demo.StaffSlice;
import com.manage.nhakhoa.desktop.demo.StaffSlice.*;
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
 * Tổng kết · Bàn giao ca (BAN_GIAO): biên bản giao ca, có chênh lệch thì bắt buộc ghi chú; chỉ người nhận ca xác nhận,
 * người lập không tự xác nhận. Hủy chỉ khi đang chờ xác nhận.
 */
public final class HandoverPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private final transient ScreenContext ctx;
    private final transient DemoClinicStore store;
    private final Flat.Segmented filter = new Flat.Segmented("handover-filter", List.of("Chờ xác nhận", "Đã xác nhận", "Đã hủy", "Tất cả"), null);
    private final CardList<Handover> list;
    private final JPanel detail = Kit.holder("handover-detail");
    private final JLabel feedback = feedback("handover-feedback");
    private final JComboBox<String> actor;
    private final JComboBox<ShiftKind> shift = combo("handover-shift", List.of(ShiftKind.values()));
    private final JComboBox<String> giver, receiver;
    private final JCheckBox discrepancy = new JCheckBox("Có chênh lệch (tiền / vật tư / việc tồn)");
    private final JTextField note = field("handover-note", 24, "Ghi chú bàn giao");
    private transient String selected;

    public HandoverPanel(ScreenContext ctx) {
        super(new BorderLayout()); setOpaque(false); setName("screen-BAN_GIAO");
        this.ctx = ctx; this.store = ctx.store();
        actor = Kit.actor("handover-actor", store.staff(), s -> s.status() == WorkStatus.DANG_LAM, "NV005");
        actor.setPreferredSize(new Dimension(300, Flat.FIELD_HEIGHT));
        actor.addActionListener(e -> render());
        giver = Kit.actor("handover-giver", store.staff(), s -> s.status() == WorkStatus.DANG_LAM, "NV005");
        receiver = Kit.actor("handover-receiver", store.staff(), s -> s.status() == WorkStatus.DANG_LAM, "NV006");
        discrepancy.setName("handover-discrepancy"); discrepancy.setOpaque(false); discrepancy.setFont(Theme.font(12, false));
        list = Kit.list("handover-list", Handover::id, h -> Kit.Spec.block(h.id(), staff().name(h.giverId()) + " → " + staff().name(h.receiverId()),
                h.fromShift().label() + " · " + DATE_TIME.format(h.time()) + (h.discrepancy() ? "  ·  có chênh lệch" : ""), tag(h.status())), 6);
        list.onSelect(h -> { selected = h.id(); render(); });
        filter.onChange(i -> { selected = null; refresh(); });
        JButton create = icon(primary("Lập biên bản", this::create), "plus", Color.WHITE); create.setName("handover-create");
        add(Kit.page(this, "Bàn giao ca", "Biên bản giao ca · người nhận xác nhận · người lập không tự xác nhận", ctx, "handover",
                "Xác nhận là thao tác giả lập theo người thao tác chọn bên dưới, không có chữ ký thật.",
                buttons(label("Thao tác với tư cách", 12, true, Theme.color("475569")), actor),
                Kit.twoCol(Kit.card("Biên bản", null, column(10, filter, list)),
                        column(Theme.GAP, Kit.card("Chi tiết biên bản", null, column(10, detail, feedback)),
                                Kit.card("Lập biên bản mới", null, column(10, form("Ca giao", shift), pair(form("Người giao *", giver), form("Người nhận *", receiver)),
                                        discrepancy, form("Ghi chú (bắt buộc nếu có chênh lệch)", note), buttons(create)))), 0.46)), BorderLayout.NORTH);
        store.addListener(this::refresh);
        refresh();
    }
    private StaffSlice staff() { return store.staff(); }
    static Tag tag(HandoverStatus s) {
        return new Tag(s.label(), switch (s) { case CHO_XAC_NHAN -> Theme.ORANGE; case DA_XAC_NHAN -> Theme.GREEN; case DA_HUY -> Theme.MUTED; });
    }
    private String actorId() { return Kit.idOf(actor.getSelectedItem()); }
    private void refresh() {
        int f = filter.selected();
        List<Handover> rows = staff().handovers().stream().filter(h -> f == 3 || h.status() == HandoverStatus.values()[f]).toList();
        list.setRows(rows, "Không có biên bản trong nhóm này");
        if (selected == null && !rows.isEmpty()) selected = rows.get(0).id();
        render();
    }
    private void render() {
        Handover h = staff().handover(selected).orElse(null);
        if (h == null) { Kit.fill(detail, Kit.empty("Chọn một biên bản để xem chi tiết")); return; }
        if (list.selected().map(Handover::id).filter(selected::equals).isEmpty()) list.select(selected);
        String cb = staff().confirmBlocker(h.id(), actorId());
        JButton confirm = icon(primary("Xác nhận nhận ca", this::confirm), "check", Color.WHITE); confirm.setName("handover-confirm");
        confirm.setEnabled(cb == null); confirm.setToolTipText(cb == null ? "Xác nhận đã nhận bàn giao" : cb);
        JButton cancel = secondary("Hủy biên bản", this::cancel); cancel.setName("handover-cancel"); cancel.setEnabled(h.status() == HandoverStatus.CHO_XAC_NHAN);
        JLabel why = feedback("handover-reason");
        if (cb != null && h.status() == HandoverStatus.CHO_XAC_NHAN) hint(why, cb); else info(why, " ");
        Kit.fill(detail, column(10, Kit.head("handover", h.id() + " · " + h.fromShift().label(), DATE.format(h.date()) + " · " + h.fromShift().hours(), tag(h.status())),
                Kit.info("handover-info", "Người giao", staff().name(h.giverId()), "Người nhận", staff().name(h.receiverId()),
                        "Thời điểm lập", DATE_TIME.format(h.time()), "Chênh lệch", h.discrepancy() ? "Có" : "Không", "Ghi chú", h.note(),
                        "Xác nhận bởi", h.confirmedBy() == null ? "—" : staff().name(h.confirmedBy())),
                buttons(confirm, cancel), why));
    }
    private void create() {
        var r = staff().createHandover((ShiftKind) shift.getSelectedItem(), Kit.idOf(giver.getSelectedItem()), Kit.idOf(receiver.getSelectedItem()),
                note.getText(), discrepancy.isSelected());
        result(feedback, r);
        if (r.ok()) { filter.select(0); selected = r.value().id(); note.setText(""); discrepancy.setSelected(false); refresh(); }
    }
    private void confirm() {
        if (!ctx.dialogs().confirm(this, "Xác nhận bàn giao", "Xác nhận " + selected + " với tư cách " + actor.getSelectedItem() + "?")) { info(feedback, "Đã hủy thao tác"); return; }
        result(feedback, staff().confirmHandover(selected, actorId()));
    }
    private void cancel() {
        String n = ctx.dialogs().reason(this, "Hủy " + selected, "Lý do hủy (5–200 ký tự):");
        if (n == null) { info(feedback, "Đã hủy thao tác"); return; }
        result(feedback, staff().cancelHandover(selected, n));
    }
}
