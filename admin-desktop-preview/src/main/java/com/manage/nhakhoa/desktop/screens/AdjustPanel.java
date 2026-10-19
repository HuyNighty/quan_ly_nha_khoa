package com.manage.nhakhoa.desktop.screens;

import com.manage.nhakhoa.desktop.demo.DemoClinicStore;
import com.manage.nhakhoa.desktop.demo.StaffSlice.Role;
import com.manage.nhakhoa.desktop.demo.SupplySlice;
import com.manage.nhakhoa.desktop.demo.SupplySlice.*;
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
 * Cung ứng · Kiểm kê & hao hụt (KIEM_KE): phiếu điều chỉnh loại kiểm kê / hư hỏng / hết hạn, số hệ thống (snapshot) vs thực tế,
 * gửi duyệt, duyệt với người lập ≠ người duyệt và chặn khi tồn lô đã đổi so với snapshot.
 */
public final class AdjustPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private final transient ScreenContext ctx;
    private final transient DemoClinicStore store;
    private final Flat.Segmented filter = new Flat.Segmented("adj-filter", List.of("Chờ duyệt", "Bản nháp", "Đã duyệt", "Từ chối", "Tất cả"), null);
    private final CardList<Adjustment> list;
    private final JPanel detail = Kit.holder("adj-detail");
    private final JLabel feedback = feedback("adj-feedback");
    private final JComboBox<String> actor;
    private final JComboBox<AdjustType> type = combo("adj-type", List.of(AdjustType.values()));
    private final JComboBox<String> lot = combo("adj-lot", List.of());
    private final JTextField actual = field("adj-actual", 6, "Số thực tế");
    private final JTextField reason = field("adj-reason", 18, "Lý do");
    private transient String selected;

    public AdjustPanel(ScreenContext ctx) {
        super(new BorderLayout()); setOpaque(false); setName("screen-KIEM_KE");
        this.ctx = ctx; this.store = ctx.store();
        actor = Kit.actor("adj-actor", store.staff(), s -> s.role() == Role.QL || s.role() == Role.KHO || s.role() == Role.AD, "NV010");
        actor.addActionListener(e -> render());
        actor.setPreferredSize(new Dimension(300, Flat.FIELD_HEIGHT));
        list = Kit.list("adj-list", Adjustment::id, a -> Kit.Spec.block(a.id(), lotName(a.lotId()),
                a.type().label() + "  ·  hệ thống " + a.systemQty() + " → thực tế " + a.actualQty() + " (" + (a.diff() > 0 ? "+" : "") + a.diff() + ")",
                tag(a.status())), 6);
        list.onSelect(a -> { selected = a.id(); render(); });
        filter.onChange(i -> { selected = null; refresh(); });
        JButton create = icon(primary("Lập phiếu (chờ duyệt)", this::create), "plus", Color.WHITE); create.setName("adj-create");
        add(Kit.page(this, "Kiểm kê & hao hụt", "Phiếu điều chỉnh kho · đối chiếu snapshot · người lập ≠ người duyệt", ctx, "adj",
                "Duyệt chỉ đổi tồn lô trong bộ nhớ phiên; không giảm lô bằng quy trình chưa duyệt.",
                buttons(label("Thao tác với tư cách", 12, true, Theme.color("475569")), actor),
                Kit.twoCol(Kit.card("Phiếu điều chỉnh", null, column(10, filter, list)),
                        column(Theme.GAP, Kit.card("Chi tiết phiếu", null, column(10, detail, feedback)),
                                Kit.card("Lập phiếu mới", null, column(10, pair(form("Loại *", type), form("Số thực tế *", actual)), form("Lô *", lot, "Lý do *", reason),
                                        buttons(create), label("Người lập = người thao tác đang chọn ở trên.", 11, false, Theme.MUTED)))), 0.46)),
                BorderLayout.NORTH);
        store.addListener(this::refresh);
        refresh();
    }
    private SupplySlice supply() { return store.supply(); }
    private String lotName(String lotId) {
        return supply().lot(lotId).map(l -> supply().item(l.itemId()).map(Item::name).orElse("") + " · lô " + l.lotNo()).orElse(lotId);
    }
    static Tag tag(AdjustStatus s) {
        return new Tag(s.label(), switch (s) { case NHAP -> Theme.MUTED; case CHO_DUYET -> Theme.ORANGE; case DA_DUYET -> Theme.GREEN; case TU_CHOI -> Theme.RED; });
    }
    private String actorId() { return Kit.idOf(actor.getSelectedItem()); }
    private void refresh() {
        int f = filter.selected();
        AdjustStatus[] map = {AdjustStatus.CHO_DUYET, AdjustStatus.NHAP, AdjustStatus.DA_DUYET, AdjustStatus.TU_CHOI};
        List<Adjustment> rows = supply().adjustments().stream().filter(a -> f == 4 || a.status() == map[f]).toList();
        list.setRows(rows, "Không có phiếu trong nhóm này");
        if (selected == null && !rows.isEmpty()) selected = rows.get(0).id();
        replaceItems(lot, supply().allLots().stream().map(l -> l.id() + " · " + lotName(l.id()) + " (tồn " + l.quantity() + ")").toList());
        render();
    }
    private void render() {
        Adjustment a = supply().adjustment(selected).orElse(null);
        if (a == null) { Kit.fill(detail, Kit.empty("Chọn một phiếu để xem chi tiết")); return; }
        if (list.selected().map(Adjustment::id).filter(selected::equals).isEmpty()) list.select(selected);
        int current = supply().lot(a.lotId()).map(Lot::quantity).orElse(0);
        String ab = supply().approveBlocker(a.id(), actorId());
        JButton submit = secondary("Gửi duyệt", () -> result(feedback, supply().submit(a.id()))); submit.setName("adj-submit");
        submit.setEnabled(a.status() == AdjustStatus.NHAP);
        JButton approve = icon(primary("Duyệt", this::approve), "check", Color.WHITE); approve.setName("adj-approve");
        approve.setEnabled(ab == null); approve.setToolTipText(ab == null ? "Cập nhật tồn lô theo số thực tế" : ab);
        JButton reject = secondary("Từ chối", this::reject); reject.setName("adj-reject"); reject.setEnabled(a.status() == AdjustStatus.CHO_DUYET);
        JLabel why = feedback("adj-reason-label");
        if (ab != null && a.status() == AdjustStatus.CHO_DUYET) warn(why, "Không thể duyệt: " + ab); else info(why, " ");
        Kit.fill(detail, column(10, Kit.head("adj", a.id() + " · " + a.type().label(), lotName(a.lotId()), tag(a.status())),
                Kit.info("adj-info", "Số hệ thống (snapshot)", String.valueOf(a.systemQty()), "Số thực tế", String.valueOf(a.actualQty()),
                        "Chênh lệch", (a.diff() > 0 ? "+" : "") + a.diff(), "Tồn lô hiện tại", String.valueOf(current),
                        "Người lập", store.staff().name(a.creatorId()) + " · " + DATE_TIME.format(a.created()), "Lý do", a.reason(),
                        "Người duyệt", a.approverId() == null ? "—" : store.staff().name(a.approverId())),
                buttons(submit, approve, reject), why));
    }
    private void create() {
        int qty = Kit.parseInt(actual.getText());
        if (qty == Integer.MIN_VALUE) { fail(feedback, "Nhập số thực tế bằng số"); return; }
        var r = supply().createAdjustment((AdjustType) type.getSelectedItem(), Kit.idOf(lot.getSelectedItem()), qty, reason.getText(), actorId());
        result(feedback, r); if (r.ok()) { filter.select(0); selected = r.value().id(); actual.setText(""); reason.setText(""); refresh(); }
    }
    private void approve() {
        if (!ctx.dialogs().confirm(this, "Duyệt điều chỉnh", "Duyệt " + selected + " với tư cách " + actor.getSelectedItem() + "?")) { info(feedback, "Đã hủy thao tác"); return; }
        result(feedback, supply().approve(selected, actorId()));
    }
    private void reject() {
        String n = ctx.dialogs().reason(this, "Từ chối " + selected, "Lý do từ chối (5–200 ký tự):");
        if (n == null) { info(feedback, "Đã hủy thao tác"); return; }
        result(feedback, supply().reject(selected, actorId(), n));
    }
}
