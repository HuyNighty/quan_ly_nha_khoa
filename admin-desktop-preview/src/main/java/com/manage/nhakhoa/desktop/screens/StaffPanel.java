package com.manage.nhakhoa.desktop.screens;

import com.manage.nhakhoa.desktop.MenuDefinition;
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

/** Tổ chức · Nhân viên (NHAN_VIEN): danh sách theo vai trò/trạng thái, liên hệ, lịch sử phân công, cho nghỉ việc có ràng buộc. */
public final class StaffPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private static final List<String> FILTERS = List.of("Tất cả", "Bác sĩ", "Y tá", "Lễ tân", "Khác", "Đã nghỉ");
    private final transient ScreenContext ctx;
    private final transient DemoClinicStore store;
    private final Flat.SearchBar search = new Flat.SearchBar("staff-search", "Tìm mã NV, họ tên, chức danh…");
    private final Flat.Segmented filter = new Flat.Segmented("staff-filter", FILTERS, null);
    private final CardList<Staff> list;
    private final JLabel count = label(" ", 12, false, Theme.MUTED);
    private final JPanel stats = Kit.stats();
    private final JPanel detail = Kit.holder("staff-detail");
    private final JLabel feedback = feedback("staff-feedback");
    private final JTextField editPhone = field("staff-edit-phone", 12, "10 số, bắt đầu bằng 0");
    private final JTextField editAddress = field("staff-edit-address", 20, "Địa chỉ");
    private transient String selected;
    private boolean editing;

    public StaffPanel(ScreenContext ctx) {
        super(new BorderLayout()); setOpaque(false); setName("screen-NHAN_VIEN");
        this.ctx = ctx; this.store = ctx.store();
        list = Kit.list("staff-list", Staff::id, s -> Kit.Spec.person(s.name(), s.id() + "  ·  " + s.title() + "  ·  từ " + DATE.format(s.startDate()),
                roleTag(s), s.name(), s.id()), 8);
        list.onSelect(s -> { selected = s.id(); editing = false; render(); });
        search.field().getDocument().addDocumentListener(SimpleDocumentListener.of(this::refreshList));
        search.onSubmit(() -> list.selectFirst()); search.bindShortcut(this);
        filter.onChange(i -> refreshList());
        add(Kit.page(this, "Nhân viên", "Hồ sơ nhân sự, vai trò AD/LT/BS/YT/QL/KHO và lịch sử phân công", ctx, "staff",
                "Tên, SĐT nhân viên là dữ liệu tổng hợp; cho nghỉ việc chỉ đổi bộ nhớ phiên.", stats,
                Kit.twoCol(Kit.card("Danh sách nhân viên", count, column(10, search, filter, list)),
                        Kit.card("Hồ sơ nhân viên", null, column(10, detail, feedback)), 0.46)), BorderLayout.NORTH);
        store.addListener(this::refresh);
        refresh();
    }
    private static Tag roleTag(Staff s) {
        if (s.status() == WorkStatus.NGHI_VIEC) return new Tag("Đã nghỉ việc", Theme.MUTED);
        Color c = switch (s.role()) { case BS -> Theme.PRIMARY_TEXT; case YT -> Theme.color("0369A1"); case LT -> Theme.PURPLE;
            case AD -> Theme.RED; case QL -> Theme.ORANGE; case KHO -> Theme.color("B45309"); };
        return new Tag(s.role().label(), c);
    }
    private StaffSlice staff() { return store.staff(); }
    private void refresh() { refreshStats(); refreshList(); render(); }
    private void refreshStats() {
        stats.removeAll();
        List<Staff> active = staff().active();
        stats.add(stat("staff-stat-active", "Đang làm việc", active.size(), Theme.PRIMARY_TEXT, null, false));
        for (Role r : new Role[]{Role.BS, Role.YT, Role.LT}) {
            stats.add(stat("staff-stat-" + r.name(), r.label(), active.stream().filter(s -> s.role() == r).count(), Theme.TEXT, null, false));
        }
        stats.add(stat("staff-stat-left", "Đã nghỉ việc", staff().staff().size() - active.size(), Theme.MUTED, null, false));
        stats.revalidate();
    }
    private void refreshList() {
        String q = MenuDefinition.normalize(search.field().getText()); int f = filter.selected();
        List<Staff> rows = staff().staff().stream().filter(s -> q.isEmpty() || MenuDefinition.normalize(s.id() + " " + s.name() + " " + s.title()).contains(q))
                .filter(s -> switch (f) {
                    case 1 -> s.role() == Role.BS && s.status() == WorkStatus.DANG_LAM;
                    case 2 -> s.role() == Role.YT && s.status() == WorkStatus.DANG_LAM;
                    case 3 -> s.role() == Role.LT && s.status() == WorkStatus.DANG_LAM;
                    case 4 -> (s.role() == Role.AD || s.role() == Role.QL || s.role() == Role.KHO) && s.status() == WorkStatus.DANG_LAM;
                    case 5 -> s.status() == WorkStatus.NGHI_VIEC;
                    default -> true;
                }).toList();
        list.setRows(rows, "Không có nhân viên khớp bộ lọc");
        count.setText(rows.size() + " / " + staff().staff().size() + " nhân viên");
    }
    private void render() {
        Staff s = staff().staff(selected).orElse(null);
        if (s == null) { Kit.fill(detail, Kit.empty("Chọn một nhân viên để xem hồ sơ")); return; }
        if (list.selected().map(Staff::id).filter(selected::equals).isEmpty()) list.select(selected);
        String account = staff().accountOf(s.id()).map(a -> a.id() + " · " + a.username() + " · " + a.status().label()).orElse("Chưa có tài khoản");
        JComponent body;
        JButton edit = icon(secondary("Sửa liên hệ", () -> { editing = true; editPhone.setText(s.phone()); editAddress.setText(s.address()); render(); }), "document", Theme.PRIMARY_TEXT);
        edit.setName("staff-edit");
        JButton resign = icon(danger("Cho nghỉ việc", this::resign), "close", Color.WHITE); resign.setName("staff-resign");
        String blocker = staff().resignBlocker(s.id());
        resign.setEnabled(blocker == null); resign.setToolTipText(blocker == null ? "Chuyển Đã nghỉ việc, vô hiệu tài khoản (mô phỏng)" : blocker);
        JLabel why = feedback("staff-resign-reason");
        if (blocker != null && s.status() == WorkStatus.DANG_LAM) hint(why, "Không thể cho nghỉ: " + blocker); else info(why, " ");
        JPanel history = new JPanel(); history.setOpaque(false); history.setLayout(new BoxLayout(history, BoxLayout.Y_AXIS));
        List<Assignment> as = staff().assignmentsOf(s.id());
        if (as.isEmpty()) history.add(label("Chưa có phân công", 12, false, Theme.MUTED));
        as.stream().limit(5).forEach(a -> history.add(label("•  " + DATE.format(a.date()) + " · " + a.shift().label() + " (" + a.shift().hours() + ") · " + a.task(), 12, false, Theme.TEXT)));
        if (editing) {
            JButton save = primary("Lưu", this::saveContact); save.setName("staff-edit-save");
            body = column(12, Kit.personHead("staff", s.name(), s.id(), s.id() + " · " + s.title(), roleTag(s)), Kit.separator(),
                    form("Số điện thoại *", editPhone, "Địa chỉ", editAddress), buttons(save, secondary("Đóng", () -> { editing = false; render(); })));
        } else {
            body = column(12, Kit.personHead("staff", s.name(), s.id(), s.id() + " · " + s.title(), roleTag(s)),
                    Kit.info("staff-info", "Vai trò", s.role().label() + " (" + s.role().name() + ")", "Trạng thái", s.status().label(),
                            "Ngày vào làm", DATE.format(s.startDate()), "Số điện thoại", DemoClinicStore.maskPhone(s.phone()),
                            "Địa chỉ", s.address(), "Tài khoản", account),
                    Kit.separator(), Kit.section("Phân công gần đây"), history, Kit.separator(), buttons(edit, resign), why);
        }
        Kit.fill(detail, body);
    }
    private void saveContact() {
        if (!ctx.dialogs().confirm(this, "Cập nhật liên hệ", "Lưu SĐT / địa chỉ mới cho " + selected + "?")) { info(feedback, "Đã hủy thao tác"); return; }
        var r = staff().updateContact(selected, editPhone.getText(), editAddress.getText());
        result(feedback, r); if (r.ok()) { editing = false; render(); }
    }
    private void resign() {
        String reason = ctx.dialogs().reason(this, "Cho nghỉ việc " + selected, "Lý do (5–200 ký tự). Tài khoản sẽ bị vô hiệu hóa:");
        if (reason == null) { info(feedback, "Đã hủy thao tác"); return; }
        result(feedback, staff().resign(selected, reason));
    }
}
