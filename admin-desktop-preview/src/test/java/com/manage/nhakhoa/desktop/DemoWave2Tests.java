package com.manage.nhakhoa.desktop;

import com.manage.nhakhoa.desktop.DemoWave1Tests.Scripted;
import com.manage.nhakhoa.desktop.demo.DemoClinicStore;
import com.manage.nhakhoa.desktop.demo.StaffSlice.ShiftKind;
import com.manage.nhakhoa.desktop.demo.SupplySlice.AdjustStatus;
import com.manage.nhakhoa.desktop.demo.SupplySlice.LotState;
import com.manage.nhakhoa.desktop.demo.BillingSlice.InvoiceStatus;
import com.manage.nhakhoa.desktop.demo.BillingSlice.InvoiceLineDraft;
import com.manage.nhakhoa.desktop.demo.BillingSlice.Method;
import com.manage.nhakhoa.desktop.ui.DemoWidgets;
import com.manage.nhakhoa.desktop.ui.RowView;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;
import javax.swing.*;

/** Wave 2: 18 màn còn lại theo menu nghiệp vụ — quy tắc store, router 21/21, luồng Cung ứng / Tổng kết, ảnh Swing. */
final class DemoWave2Tests {
    private static int checks;
    private static final Clock FIXED = AdminPreview.fixtureClock();
    private DemoWave2Tests() { }

    static int run() throws Exception {
        checks = 0;
        storeRules();
        edt(() -> { allScreens(); invoiceCreationFlow(); supplyFlows(); summaryFlows(); return null; });
        return checks;
    }
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError("UI-DEMO-2: " + message);
        checks++; System.out.println("ĐẠT · [UI-DEMO-2] " + message);
    }

    private static void storeRules() {
        DemoClinicStore s = new DemoClinicStore(FIXED);
        LocalDate d = s.today();
        // Thanh toán (Q07)
        check(s.billing().totalDebt() == 3_600_000 && s.billing().debtInvoiceCount() == 5, "Công nợ 3.600.000 đ trên 5 hóa đơn (gồm hóa đơn chờ hủy)");
        check(s.billing().receivedOn(d, null) == 700_000, "Thu hợp lệ hôm nay 700.000 đ khớp Tổng quan");
        check(s.billing().approveBlocker("YC0001", "NV008") != null, "Người lập không tự duyệt hủy hóa đơn");
        check(s.billing().approveBlocker("YC0002", "NV010") != null, "Hóa đơn đã thu tiền không duyệt hủy được");
        check(s.billing().approve("YC0001", "NV010").ok() && s.billing().status("HD000006") == InvoiceStatus.DA_HUY, "Duyệt hủy: hóa đơn chuyển Đã hủy");
        // Lập hóa đơn mới — validate bệnh nhân / phiếu khám / dịch vụ; chốt giá, chưa thu tiền
        check(s.billing().createInvoice("BN999999", null, List.of(new InvoiceLineDraft("DV003", 1))).code() == DemoClinicStore.Code.NOT_FOUND,
                "Lập hóa đơn: chặn bệnh nhân không tồn tại");
        check(s.billing().createInvoice("BN000004", "PK000001", List.of(new InvoiceLineDraft("DV003", 1))).code() == DemoClinicStore.Code.RULE,
                "Lập hóa đơn: chặn phiếu khám không thuộc bệnh nhân");
        check(s.billing().createInvoice("BN000004", "PK000003", List.of(new InvoiceLineDraft("DV003", 1))).code() == DemoClinicStore.Code.RULE,
                "Lập hóa đơn: chặn phiếu khám chưa kết luận");
        check(s.billing().createInvoice("BN000001", "PK000001", List.of(new InvoiceLineDraft("DV003", 1))).code() == DemoClinicStore.Code.DUPLICATE,
                "Lập hóa đơn: chặn phiếu khám có hóa đơn còn hiệu lực");
        check(s.billing().createInvoice("BN000004", null, List.of()).code() == DemoClinicStore.Code.INVALID,
                "Lập hóa đơn: phải có dòng dịch vụ");
        check(s.billing().createInvoice("BN000004", null, List.of(new InvoiceLineDraft("DV003", 0))).code() == DemoClinicStore.Code.INVALID,
                "Lập hóa đơn: số lượng phải dương");
        check(s.billing().createInvoice("BN000004", null, List.of(new InvoiceLineDraft("DV012", 1))).code() == DemoClinicStore.Code.RULE,
                "Lập hóa đơn: chặn dịch vụ đã ngừng áp dụng");
        check(s.billing().createInvoice("BN000004", null, List.of(new InvoiceLineDraft("DV003", 1), new InvoiceLineDraft("DV003", 1))).code()
                == DemoClinicStore.Code.DUPLICATE, "Lập hóa đơn: chặn dịch vụ trùng");
        var created = s.billing().createInvoice("BN000004", null, List.of(new InvoiceLineDraft("DV003", 2)));
        check(created.ok() && "HD000008".equals(created.value().id()) && created.value().total() == 800_000,
                "Lập hóa đơn: sinh mã và tính tiền đúng");
        check(s.billing().paid(created.value().id()) == 0 && s.billing().debt(created.value().id()) == 800_000
                && s.billing().status(created.value().id()) == InvoiceStatus.CHUA_THANH_TOAN,
                "Hóa đơn mới chưa ghi nhận thu tiền, tăng công nợ");
        check(s.billing().changePrice("DV003", 450_000, "Thay đổi giá kiểm thử").ok()
                && created.value().lines().getFirst().unitPrice() == 400_000,
                "Chốt đơn giá trên hóa đơn; bảng giá đổi không hồi tố");
        check(s.billing().pay(created.value().id(), Method.TIEN_MAT, 900_000, "").code() == DemoClinicStore.Code.INVALID,
                "Chặn thu tiền vượt công nợ hóa đơn mới");
        check(s.billing().pay(created.value().id(), Method.TIEN_MAT, 200_000, "").ok()
                && s.billing().status(created.value().id()) == InvoiceStatus.MOT_PHAN
                && s.billing().debt(created.value().id()) == 600_000,
                "Hóa đơn mới hỗ trợ thanh toán một phần");
        // Điều trị (Q12)
        check(s.clinical().dueItems().size() == 6 && s.clinical().duePlanCount() == 5, "Hạng mục đến hạn 14 ngày (gồm quá hạn): 6 mục / 5 kế hoạch");
        // Tổ chức / Lập kế hoạch
        check(s.staff().missingTotal(d, ShiftKind.CHIEU) == 1 && s.staff().missingTotal(d, ShiftKind.TOI) == 1, "Ca chiều và ca tối mỗi ca thiếu 1 người");
        // Cung ứng (Q09)
        check(s.supply().belowMinimumCount() == 2 && s.supply().lotCount(LotState.CAN_HAN) == 2 && s.supply().lotCount(LotState.HET_HAN) == 1,
                "Kho: dưới định mức 2, lô cận hạn 2, lô hết hạn còn tồn 1");
        check(s.supply().issue("T005", 999, "Phòng khám 1", "Xuất thử vượt tồn").code() != DemoClinicStore.Code.OK, "Xuất không vượt tồn khả dụng");
        check(s.supply().approveBlocker("DC0001", "NV011") != null, "Người lập phiếu điều chỉnh không tự duyệt");
        String lot = s.supply().adjustment("DC0001").orElseThrow().lotId();
        check(s.supply().approve("DC0001", "NV010").ok() && s.supply().lot(lot).orElseThrow().quantity() == 56
                && s.supply().adjustment("DC0001").orElseThrow().status() == AdjustStatus.DA_DUYET, "Duyệt điều chỉnh cập nhật tồn lô theo số thực tế");
        check(s.supply().checkDue() && !s.supply().kitSummary().contains("ĐẠT"), "Hộp cấp cứu đến hạn kiểm tra (D−7) và không bao giờ báo 'đạt an toàn'");
        // Tổng kết
        check(s.staff().confirmBlocker("BG0003", "NV008") != null && s.staff().confirmBlocker("BG0003", "NV006") != null,
                "Bàn giao: người lập / người khác không xác nhận thay người nhận");
        check(s.staff().confirmHandover("BG0003", "NV005").ok(), "Người nhận ca xác nhận bàn giao");
        check(s.staff().evalBlocker("DG0004", "NV006") != null, "Không tự đánh giá bản thân");
        check(s.activities().stream().anyMatch(a -> a.action().equals("Xác nhận bàn giao")), "Thao tác demo ghi vào Nhật ký");
    }

    private static void allScreens() throws Exception {
        AdminShell shell = new AdminShell(FIXED, true, new DemoClinicStore(FIXED), new Scripted());
        for (MenuDefinition.Entry e : MenuDefinition.entries()) {
            if (e.key().equals(MenuDefinition.HOME)) continue; // Tổng quan là màn fixture riêng
            shell.navigate(e.key());
            JComponent screen = shell.screen(e.key());
            check(shell.currentScreen().equals(e.key()) && screen != null && ("screen-" + e.key()).equals(screen.getName()), "Router mở màn " + e.label());
            check(texts(screen).stream().anyMatch(t -> t.contains(DemoWidgets.NOTICE)), "Nhãn DỮ LIỆU MÔ PHỎNG trên " + e.key());
            if (!List.of("TIEP_NHAN", "LICH_HEN", "BENH_NHAN").contains(e.key()))
                snapshot(screen, 1110, 840, "demo2-" + e.key().toLowerCase().replace('_', '-') + ".png");
        }
        shell.close();
    }

    private static void invoiceCreationFlow() {
        Scripted dialogs = new Scripted();
        DemoClinicStore store = new DemoClinicStore(FIXED);
        AdminShell shell = new AdminShell(FIXED, true, store, dialogs);
        shell.navigate("HOA_DON");
        JComponent page = shell.screen("HOA_DON");
        click(page, "inv-new");
        ((JComboBox<?>) find(page, "inv-create-patient")).setSelectedItem(item(find(page, "inv-create-patient"), "BN000004"));
        check(((JComboBox<?>) find(page, "inv-create-ticket")).getItemCount() == 1,
                "Form chỉ cho chọn phiếu khám đủ điều kiện (bệnh nhân chưa kết luận)");
        ((JComboBox<?>) find(page, "inv-create-service")).setSelectedItem(item(find(page, "inv-create-service"), "DV003"));
        field(page, "inv-create-qty").setText("2");
        click(page, "inv-create-add");
        check(text(page, "inv-create-feedback").contains("Đã thêm"), "Form thêm dòng dịch vụ");
        check(text(page, "inv-create-total").contains("800.000"), "Form tính đúng tổng hóa đơn dự kiến");
        dialogs.answers.add(Boolean.TRUE);
        click(page, "inv-create-save");
        check(store.billing().invoice("HD000008").isPresent()
                && table(page, "inv-list").selected().map(x -> ((com.manage.nhakhoa.desktop.demo.BillingSlice.Invoice)x).id()).orElse("").equals("HD000008"),
                "Lưu từ giao diện tạo hóa đơn và chọn hóa đơn mới");
        check(store.billing().debt("HD000008") == 800_000, "Hóa đơn mới có thể thu tiền");
        dialogs.answers.add(Boolean.TRUE);
        click(page, "inv-pay");
        check(store.billing().status("HD000008") == InvoiceStatus.DA_THANH_TOAN,
                "Thu tiền trên hóa đơn vừa lập kết thúc công nợ");
        shell.close();
    }

    private static void supplyFlows() {
        Scripted dialogs = new Scripted();
        AdminShell shell = new AdminShell(FIXED, true, new DemoClinicStore(FIXED), dialogs);
        shell.navigate("THUOC_VAT_TU"); JComponent st = shell.screen("THUOC_VAT_TU");
        check(text(st, "stock-stat-below").equals("2") && text(st, "stock-stat-expired").equals("1"), "Thuốc & vật tư: thẻ thống kê khớp Tổng quan");
        table(st, "stock-list").select("T001");
        check(((AbstractButton) find(st, "stock-mark-ordered")).isEnabled(), "Mặt hàng dưới định mức cho phép đánh dấu đặt hàng");
        click(st, "stock-mark-ordered");
        check(!((AbstractButton) find(st, "stock-mark-ordered")).isEnabled(), "Đánh dấu đặt hàng chỉ một lần (mô phỏng)");

        shell.navigate("KIEM_KE"); JComponent adj = shell.screen("KIEM_KE");
        ((JComboBox<?>) find(adj, "adj-actor")).setSelectedItem(item(find(adj, "adj-actor"), "NV011"));
        check(!((AbstractButton) find(adj, "adj-approve")).isEnabled(), "Kiểm kê: nút Duyệt khóa khi người thao tác là người lập");
        ((JComboBox<?>) find(adj, "adj-actor")).setSelectedItem(item(find(adj, "adj-actor"), "NV010"));
        dialogs.answers.add(Boolean.TRUE); click(adj, "adj-approve");
        check(text(adj, "adj-feedback").contains("DC0001") || shell.store().supply().adjustment("DC0001").orElseThrow().status() == AdjustStatus.DA_DUYET,
                "Kiểm kê: quản lý duyệt phiếu chờ duyệt");

        shell.navigate("CAP_CUU"); JComponent kit = shell.screen("CAP_CUU");
        check(text(kit, "kit-summary").contains("ĐẾN HẠN KIỂM TRA"), "Hộp cấp cứu hiển thị đến hạn kiểm tra");
        field(kit, "kit-qty").setText("2"); field(kit, "kit-expiry").setText("");
        ((JComboBox<?>) find(kit, "kit-item")).setSelectedItem(item(find(kit, "kit-item"), "CC02"));
        click(kit, "kit-record");
        check(text(kit, "kit-feedback").contains("hạn dùng"), "Mục bắt buộc HSD không ghi nhận khi thiếu hạn dùng");
        shell.close();
    }

    private static void summaryFlows() {
        Scripted dialogs = new Scripted();
        AdminShell shell = new AdminShell(FIXED, true, new DemoClinicStore(FIXED), dialogs);
        shell.navigate("BAN_GIAO"); JComponent h = shell.screen("BAN_GIAO");
        table(h, "handover-list").select("BG0003");
        ((JComboBox<?>) find(h, "handover-actor")).setSelectedItem(item(find(h, "handover-actor"), "NV008"));
        check(!((AbstractButton) find(h, "handover-confirm")).isEnabled(), "Bàn giao: người lập không tự xác nhận");
        ((JComboBox<?>) find(h, "handover-actor")).setSelectedItem(item(find(h, "handover-actor"), "NV005"));
        dialogs.answers.add(Boolean.TRUE); click(h, "handover-confirm");
        check(shell.store().staff().handover("BG0003").orElseThrow().confirmedBy().equals("NV005"), "Bàn giao: người nhận xác nhận qua giao diện");
        ((JCheckBox) find(h, "handover-discrepancy")).setSelected(true); field(h, "handover-note").setText("");
        click(h, "handover-create");
        check(text(h, "handover-feedback").contains("chênh lệch"), "Có chênh lệch thì bắt buộc ghi chú");

        shell.navigate("DANH_GIA"); JComponent ev = shell.screen("DANH_GIA");
        table(ev, "eval-list").select("DG0004");
        ((JComboBox<?>) find(ev, "eval-actor")).setSelectedItem(item(find(ev, "eval-actor"), "NV010"));
        field(ev, "eval-comment").setText("Phối hợp tốt trong ca");
        click(ev, "eval-submit");
        check(text(ev, "eval-feedback").contains("Đã gửi DG0004"), "Đánh giá: gửi phiếu với nhận xét");

        shell.navigate("NHAT_KY"); JComponent au = shell.screen("NHAT_KY");
        check(table(au, "audit-table").rows().stream().anyMatch(r -> r.toString().contains("Gửi phiếu đánh giá")), "Nhật ký ghi thao tác vừa làm ở màn khác");
        field(au, "audit-search").setText("BG0003");
        check(!table(au, "audit-table").rows().isEmpty() && table(au, "audit-table").rows().stream().allMatch(r -> r.toString().contains("BG0003")),
                "Nhật ký lọc theo mã đối tượng");
        click(au, "audit-range-0");
        check(table(au, "audit-table").rows().stream().allMatch(r -> r.toString().contains(FIXED.instant().atZone(FIXED.getZone()).toLocalDate().toString())),
                "Nhật ký lọc theo ngày hôm nay");
        shell.close();
    }

    // ------------------------------------------------------------------ helpers
    private static Object item(Component combo, String id) {
        JComboBox<?> c = (JComboBox<?>) combo;
        for (int i = 0; i < c.getItemCount(); i++) if (c.getItemAt(i).toString().startsWith(id)) return c.getItemAt(i);
        throw new AssertionError("Không có lựa chọn " + id);
    }
    private static <T> T edt(Callable<T> task) throws Exception {
        FutureTask<T> f = new FutureTask<>(task); SwingUtilities.invokeLater(f); return f.get(120, TimeUnit.SECONDS);
    }
    private static List<Component> all(Component root) {
        List<Component> out = new ArrayList<>(); out.add(root);
        if (root instanceof Container c) for (Component child : c.getComponents()) out.addAll(all(child));
        return out;
    }
    private static Component find(Component root, String name) {
        return all(root).stream().filter(c -> name.equals(c.getName())).findFirst().orElseThrow(() -> new AssertionError("Thiếu component " + name));
    }
    private static String text(Component root, String name) { return ((JLabel) find(root, name)).getText(); }
    private static JTextField field(Component root, String name) { return (JTextField) find(root, name); }
    private static RowView<?> table(Component root, String name) { return (RowView<?>) find(root, name); }
    private static void click(Component root, String name) { ((AbstractButton) find(root, name)).doClick(0); }
    private static List<String> texts(Component root) {
        return all(root).stream().filter(JLabel.class::isInstance).map(c -> ((JLabel) c).getText()).filter(java.util.Objects::nonNull).toList();
    }
    private static void layout(Component component) {
        if (component instanceof Container c) { c.doLayout(); for (Component child : c.getComponents()) layout(child); }
    }
    private static void snapshot(JComponent component, int width, int height, String name) throws Exception {
        component.setSize(width, height);
        for (int pass = 0; pass < 4; pass++) { for (Component child : all(component)) child.invalidate(); layout(component); }
        BufferedImage output = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = output.createGraphics(); component.printAll(g); g.dispose();
        Path dir = Path.of("build/screenshots"); Files.createDirectories(dir); ImageIO.write(output, "png", dir.resolve(name).toFile());
        check(Files.size(dir.resolve(name)) > 10_000, "Ảnh Swing thật: " + name);
    }
}
