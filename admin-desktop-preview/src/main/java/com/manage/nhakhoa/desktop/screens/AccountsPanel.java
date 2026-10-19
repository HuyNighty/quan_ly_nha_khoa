package com.manage.nhakhoa.desktop.screens;

import com.manage.nhakhoa.desktop.MenuDefinition;
import com.manage.nhakhoa.desktop.demo.DemoClinicStore;
import com.manage.nhakhoa.desktop.demo.StaffSlice;
import com.manage.nhakhoa.desktop.demo.StaffSlice.*;
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
 * Tổ chức · Tài khoản & vai trò (TAI_KHOAN): 6 vai trò role-only (không Permission/ACL), khóa / mở khóa / đổi vai trò giả lập
 * với ràng buộc không tự khóa và không bỏ quản trị viên cuối cùng. Bảng vai trò – chức năng chỉ để tham khảo.
 */
public final class AccountsPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    /** Vai trò → nhóm chức năng chính (tham khảo, theo ma trận BA V1, chỉ đọc). */
    record RoleRow(Role role, String functions) { }
    private static final List<RoleRow> MATRIX = List.of(
            new RoleRow(Role.AD, "Toàn bộ quản trị: tài khoản, danh mục, xem báo cáo; không ký chuyên môn thay bác sĩ"),
            new RoleRow(Role.LT, "Lịch hẹn, tiếp nhận, hồ sơ hành chính bệnh nhân, thu tiền, tạo yêu cầu hủy hóa đơn"),
            new RoleRow(Role.BS, "Phiếu khám, chẩn đoán, chỉ định, kế hoạch điều trị, tái khám"),
            new RoleRow(Role.YT, "Hỗ trợ khám, nhập kết quả cận lâm sàng, bàn giao ca, kiểm tra hộp cấp cứu"),
            new RoleRow(Role.QL, "Phân công ca, duyệt hủy hóa đơn, duyệt điều chỉnh kho, đánh giá nhân viên"),
            new RoleRow(Role.KHO, "Thuốc & vật tư, nhập / xuất / hoàn trả, kiểm kê"));
    private final transient ScreenContext ctx;
    private final transient DemoClinicStore store;
    private final Flat.SearchBar search = new Flat.SearchBar("acc-search", "Tìm mã TK, tên đăng nhập, nhân viên…");
    private final Flat.Segmented filter = new Flat.Segmented("acc-filter", List.of("Tất cả", "Hoạt động", "Bị khóa", "Vô hiệu"), null);
    private final CardList<Account> list;
    private final JLabel count = label(" ", 12, false, Theme.MUTED);
    private final JPanel detail = Kit.holder("acc-detail");
    private final JLabel feedback = feedback("acc-feedback");
    private final JComboBox<Role> roleBox = combo("acc-role", List.of(Role.values()));
    private transient String selected;

    public AccountsPanel(ScreenContext ctx) {
        super(new BorderLayout()); setOpaque(false); setName("screen-TAI_KHOAN");
        this.ctx = ctx; this.store = ctx.store();
        list = Kit.list("acc-list", Account::id, a -> {
            String n = store.staff().name(a.staffId());
            return Kit.Spec.person(n, a.id() + "  ·  " + a.username() + "  ·  " + a.role().label(), statusTag(a.status()), n, a.staffId());
        }, 7);
        list.onSelect(a -> { selected = a.id(); render(); });
        search.field().getDocument().addDocumentListener(SimpleDocumentListener.of(this::refreshList));
        search.onSubmit(() -> list.selectFirst()); search.bindShortcut(this);
        filter.onChange(i -> refreshList());
        DemoTable<RoleRow> matrix = new DemoTable<>("acc-matrix", List.of(
                new Column<RoleRow>("Vai trò", r -> new DemoTable.Strong(r.role().name() + " · " + r.role().label()), 140),
                new Column<RoleRow>("Chức năng chính (tham khảo, chỉ đọc)", RoleRow::functions, 520)), r -> r.role().name(), 6);
        matrix.setRows(MATRIX, "—");
        roleBox.setRenderer(new DefaultListCellRenderer() {
            private static final long serialVersionUID = 1L;
            @Override public Component getListCellRendererComponent(JList<?> l, Object v, int i, boolean s, boolean f) {
                JLabel c = (JLabel) super.getListCellRendererComponent(l, v, i, s, f);
                if (v instanceof Role r) c.setText(r.name() + " · " + r.label());
                c.setBorder(BorderFactory.createEmptyBorder(i < 0 ? 0 : 6, i < 0 ? 0 : 10, i < 0 ? 0 : 6, 10)); return c;
            }
        });
        add(Kit.page(this, "Tài khoản & vai trò", "6 vai trò AD / LT / BS / YT / QL / KHO · không có phân quyền chi tiết (Permission/ACL)", ctx, "acc",
                "Không kết nối xác thực thật; khóa / đổi vai trò là thao tác giả lập, không đổi mật khẩu.",
                Kit.twoCol(Kit.card("Tài khoản", count, column(10, search, filter, list)),
                        Kit.card("Chi tiết tài khoản", null, column(10, detail, feedback)), 0.46),
                Kit.card("Vai trò và chức năng (tham khảo)", null, matrix)), BorderLayout.NORTH);
        store.addListener(this::refresh);
        refresh();
    }
    static Tag statusTag(AccountStatus s) {
        return new Tag(s.label(), switch (s) { case HOAT_DONG -> Theme.GREEN; case KHOA -> Theme.RED; case VO_HIEU -> Theme.MUTED; });
    }
    private StaffSlice staff() { return store.staff(); }
    private void refresh() { refreshList(); render(); }
    private void refreshList() {
        String q = MenuDefinition.normalize(search.field().getText()); int f = filter.selected();
        List<Account> rows = staff().accounts().stream()
                .filter(a -> q.isEmpty() || MenuDefinition.normalize(a.id() + " " + a.username() + " " + staff().name(a.staffId())).contains(q))
                .filter(a -> f == 0 || a.status() == AccountStatus.values()[f - 1]).toList();
        list.setRows(rows, "Không có tài khoản khớp bộ lọc");
        count.setText(rows.size() + " tài khoản · " + staff().activeAdmins() + " quản trị đang hoạt động");
    }
    private void render() {
        Account a = staff().account(selected).orElse(null);
        if (a == null) { Kit.fill(detail, Kit.empty("Chọn một tài khoản để xem chi tiết")); return; }
        if (list.selected().map(Account::id).filter(selected::equals).isEmpty()) list.select(selected);
        String n = staff().name(a.staffId());
        JButton lock = icon(danger("Khóa", () -> status(AccountStatus.KHOA)), "lock", Color.WHITE); lock.setName("acc-lock");
        JButton unlock = icon(primary("Mở khóa", () -> status(AccountStatus.HOAT_DONG)), "check", Color.WHITE); unlock.setName("acc-unlock");
        JButton disable = secondary("Vô hiệu hóa", () -> status(AccountStatus.VO_HIEU)); disable.setName("acc-disable");
        JButton role = secondary("Đổi vai trò (giả lập)", this::changeRole); role.setName("acc-change-role");
        String lb = staff().accountBlocker(a.id(), AccountStatus.KHOA), ub = staff().accountBlocker(a.id(), AccountStatus.HOAT_DONG),
                db = staff().accountBlocker(a.id(), AccountStatus.VO_HIEU);
        lock.setEnabled(lb == null); unlock.setEnabled(ub == null); disable.setEnabled(db == null);
        lock.setToolTipText(lb == null ? "Khóa tài khoản (giả lập)" : lb); unlock.setToolTipText(ub == null ? "Mở khóa (giả lập)" : ub);
        disable.setToolTipText(db == null ? "Vô hiệu hóa (giả lập)" : db);
        roleBox.setSelectedItem(a.role());
        JLabel policy = feedback("acc-policy");
        String reason = lb != null && a.status() == AccountStatus.HOAT_DONG ? lb : null;
        if (reason != null) hint(policy, "Chính sách: " + reason); else info(policy, " ");
        Kit.fill(detail, column(12, Kit.personHead("acc", n, a.staffId(), a.id() + " · " + a.username(), statusTag(a.status())),
                Kit.info("acc-info", "Nhân viên", a.staffId() + " · " + n, "Vai trò", a.role().name() + " · " + a.role().label(),
                        "Trạng thái", a.status().label(), "Đăng nhập gần nhất (mẫu)", a.lastLogin() == null ? "—" : DATE_TIME.format(a.lastLogin())),
                Kit.separator(), buttons(lock, unlock, disable), policy, Kit.separator(),
                Kit.section("Đổi vai trò (thao tác giả lập — không tạo Permission/ACL)"), buttons(roleBox, role)));
    }
    private void status(AccountStatus target) {
        String r = ctx.dialogs().reason(this, target.label() + " " + selected, "Lý do (5–200 ký tự) — thao tác giả lập, không có xác thực thật:");
        if (r == null) { info(feedback, "Đã hủy thao tác"); return; }
        result(feedback, staff().setAccountStatus(selected, target, r));
    }
    private void changeRole() {
        Role r = (Role) roleBox.getSelectedItem();
        if (r == null) return;
        String b = staff().roleBlocker(selected, r);
        if (b != null) { fail(feedback, b); return; }
        if (!ctx.dialogs().confirm(this, "Đổi vai trò", "Đổi vai trò " + selected + " sang " + r.name() + " · " + r.label() + "? (thao tác giả lập)")) {
            info(feedback, "Đã hủy thao tác"); return;
        }
        result(feedback, staff().changeRole(selected, r));
    }
}
