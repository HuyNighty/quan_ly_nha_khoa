package com.manage.nhakhoa.desktop.screens;

import com.manage.nhakhoa.desktop.demo.DemoClinicStore;
import com.manage.nhakhoa.desktop.demo.StaffSlice;
import com.manage.nhakhoa.desktop.demo.StaffSlice.*;
import com.manage.nhakhoa.desktop.ui.CardList;
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
 * Tổng kết · Đánh giá nhân viên (DANH_GIA): 5 tiêu chí MẪU chấm 1–5, lưu nháp / gửi; không tự đánh giá bản thân;
 * phiếu đã gửi chỉ đọc. Không phải quy chế nhân sự chính thức.
 */
public final class EvaluationPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private final transient DemoClinicStore store;
    private final Flat.Segmented filter = new Flat.Segmented("eval-filter", List.of("Bản nháp", "Đã gửi", "Tất cả"), null);
    private final CardList<Evaluation> list;
    private final JPanel detail = Kit.holder("eval-detail");
    private final JLabel feedback = feedback("eval-feedback");
    private final JComboBox<String> actor;
    private final transient List<Flat.Segmented> scores = new ArrayList<>();
    private final JTextField comment = field("eval-comment", 14, "Nhận xét (bắt buộc khi gửi, từ 5 ký tự)");
    private transient String selected;

    public EvaluationPanel(ScreenContext ctx) {
        super(new BorderLayout()); setOpaque(false); setName("screen-DANH_GIA");
        this.store = ctx.store();
        actor = Kit.actor("eval-actor", store.staff(), s -> s.role() == Role.QL || s.role() == Role.AD || s.role() == Role.BS, "NV010");
        actor.setPreferredSize(new Dimension(300, Flat.FIELD_HEIGHT));
        actor.addActionListener(e -> render());
        for (int i = 0; i < StaffSlice.CRITERIA.size(); i++) scores.add(new Flat.Segmented("eval-score-" + i, List.of("1", "2", "3", "4", "5"), null));
        list = Kit.list("eval-list", Evaluation::id, e -> Kit.Spec.person(staff().name(e.staffId()),
                e.id() + "  ·  " + e.period() + "  ·  " + (e.status() == EvalStatus.DA_GUI ? String.format("điểm TB %.1f", e.average()) : "chưa gửi"),
                tag(e.status()), staff().name(e.staffId()), e.staffId()), 6);
        list.onSelect(e -> { selected = e.id(); load(); render(); });
        filter.onChange(i -> { selected = null; refresh(); });
        add(Kit.page(this, "Đánh giá nhân viên", "5 tiêu chí mẫu · thang 1–5 · không tự đánh giá bản thân · phiếu đã gửi chỉ đọc", ctx, "eval",
                "Tiêu chí là MẪU để minh họa luồng; không dùng làm căn cứ khen thưởng / kỷ luật thật.",
                buttons(label("Người đánh giá", 12, true, Theme.color("475569")), actor),
                Kit.twoCol(Kit.card("Phiếu đánh giá", null, column(10, filter, list)),
                        Kit.card("Chấm điểm", null, column(10, detail, feedback)), 0.42)), BorderLayout.NORTH);
        store.addListener(this::refresh);
        refresh();
    }
    private StaffSlice staff() { return store.staff(); }
    static Tag tag(EvalStatus s) { return new Tag(s.label(), s == EvalStatus.DA_GUI ? Theme.GREEN : Theme.ORANGE); }
    private String actorId() { return Kit.idOf(actor.getSelectedItem()); }
    private void refresh() {
        int f = filter.selected();
        List<Evaluation> rows = staff().evaluations().stream().filter(e -> f == 2 || e.status() == EvalStatus.values()[f]).toList();
        list.setRows(rows, "Không có phiếu trong nhóm này");
        if (selected == null && !rows.isEmpty()) { selected = rows.get(0).id(); load(); }
        render();
    }
    private void load() {
        staff().evaluation(selected).ifPresent(e -> {
            for (int i = 0; i < scores.size(); i++) scores.get(i).select(i < e.scores().size() ? Math.max(0, e.scores().get(i) - 1) : 2);
            comment.setText(e.comment() == null ? "" : e.comment());
        });
    }
    private void render() {
        Evaluation e = staff().evaluation(selected).orElse(null);
        if (e == null) { Kit.fill(detail, Kit.empty("Chọn một phiếu để chấm điểm")); return; }
        if (list.selected().map(Evaluation::id).filter(selected::equals).isEmpty()) list.select(selected);
        String b = staff().evalBlocker(e.id(), actorId());
        JPanel grid = new JPanel(new GridBagLayout()); grid.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints(); c.anchor = GridBagConstraints.WEST; c.insets = new Insets(4, 0, 4, 12);
        for (int i = 0; i < scores.size(); i++) {
            c.gridy = i; c.gridx = 0; c.weightx = 0; grid.add(label(StaffSlice.CRITERIA.get(i), 13, false, Theme.TEXT), c);
            c.gridx = 1; c.weightx = 1; grid.add(scores.get(i), c);
            scores.get(i).setEnabled(b == null);
        }
        comment.setEnabled(b == null);
        JButton draft = secondary("Lưu nháp", () -> save(false)); draft.setName("eval-draft"); draft.setEnabled(b == null);
        JButton submit = icon(primary("Gửi phiếu", () -> save(true)), "check", Color.WHITE); submit.setName("eval-submit"); submit.setEnabled(b == null);
        JLabel why = feedback("eval-reason");
        if (b != null) hint(why, b); else info(why, " ");
        Kit.fill(detail, column(10, Kit.personHead("eval", staff().name(e.staffId()), e.staffId(),
                        e.id() + " · kỳ " + e.period() + " · " + staff().staff(e.staffId()).map(s -> s.role().label()).orElse(""), tag(e.status())),
                Kit.info("eval-info", "Người đánh giá", e.evaluatorId() == null ? "Chưa có" : staff().name(e.evaluatorId()),
                        "Điểm trung bình", e.scores().isEmpty() ? "—" : String.format("%.1f / 5", e.average())),
                Kit.section("Tiêu chí (mẫu)"), grid, form("Nhận xét", comment), buttons(draft, submit), why));
    }
    private void save(boolean submit) {
        List<Integer> s = scores.stream().map(x -> x.selected() + 1).toList();
        result(feedback, staff().saveEvaluation(selected, actorId(), s, comment.getText(), submit));
    }
}
