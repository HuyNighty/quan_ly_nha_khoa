package com.manage.nhakhoa.desktop;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/** Khóa điều hướng ổn định, nhãn và từ khóa tìm kiếm độc lập với khóa. */
public final class MenuDefinition {
    public static final String HOME = "TONG_QUAN";
    public record Entry(String key, String label, String keywords, String icon) { }
    public record Group(String key, String label, int demoBadge, List<Entry> entries) {
        public Group { entries = List.copyOf(entries); }
    }
    public static final Entry HOME_ENTRY = new Entry(HOME, "Tổng quan", "QL01", "grid");
    public static final List<Group> GROUPS = List.of(
        new Group("reception", "Tiếp nhận & lịch hẹn", 0, List.of(
            e("TIEP_NHAN", "Tiếp nhận bệnh nhân", "UC01 UC02 UC08", "person"),
            e("LICH_HEN", "Lịch hẹn", "UC03 UC06 UC07", "calendar"),
            e("BENH_NHAN", "Hồ sơ bệnh nhân", "UC09 UC23", "person"))),
        new Group("clinical", "Khám & điều trị", 6, List.of(
            e("PHIEU_KHAM", "Phiếu khám", "UC10 UC11 UC16", "tooth"),
            e("KE_HOACH", "Kế hoạch điều trị", "UC12 UC13 UC15", "document"),
            e("TAI_KHAM", "Tái khám", "UC14", "clock"))),
        new Group("billing", "Thanh toán & công nợ", 2, List.of(
            e("HOA_DON", "Hóa đơn & thanh toán", "UC04 UC05 công nợ", "wallet"),
            e("DUYET_HUY", "Duyệt hủy hóa đơn", "yêu cầu hủy", "document"),
            e("BANG_GIA", "Bảng giá dịch vụ", "giá", "document"))),
        new Group("staff", "Quản lý nhân viên", 2, List.of(
            e("NHAN_VIEN", "Hồ sơ nhân viên", "UC22 UC26", "person"),
            e("PHAN_CONG", "Phân công & lịch làm việc", "UC17 UC18 UC20 UC24", "calendar"),
            e("BAN_GIAO", "Bàn giao ca", "UC19", "exchange"),
            e("DANH_GIA", "Đánh giá nhân viên", "UC25", "document"))),
        new Group("stock", "Quản lý thuốc & vật tư", 1, List.of(
            e("THUOC_VAT_TU", "Thuốc & vật tư", "UC27 UC29 UC33 UC36 UC37 tồn kho", "box"),
            e("NHAP_XUAT", "Nhập / xuất / hoàn trả", "UC30 UC31 UC32", "exchange"),
            e("KIEM_KE", "Kiểm kê & hao hụt", "UC28 UC35", "document"),
            e("CAP_CUU", "Hộp cấp cứu", "UC34", "medical"),
            e("NHA_CUNG_CAP", "Nhà cung cấp", "nhập kho", "box"))),
        new Group("access", "Kiểm soát truy cập", 1, List.of(
            e("TAI_KHOAN", "Tài khoản & vai trò", "UC21 AD LT BS YT QL KHO", "lock"),
            e("NHAT_KY", "Nhật ký thao tác", "nhật ký", "document")))
    );

    private MenuDefinition() { }
    private static Entry e(String key, String label, String keywords, String icon) {
        return new Entry(key, label, keywords, icon);
    }
    public static List<Entry> entries() {
        return Stream.concat(Stream.of(HOME_ENTRY), GROUPS.stream().flatMap(g -> g.entries().stream())).toList();
    }
    public static Entry entry(String key) {
        return entries().stream().filter(e -> e.key().equals(key)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy màn hình: " + key));
    }
    public static String normalize(String text) {
        return Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .replace('đ', 'd').replace('Đ', 'D').toLowerCase(Locale.ROOT).trim().replaceAll("\\s+", " ");
    }
    public static boolean matches(Entry entry, String groupLabel, String query) {
        return normalize(groupLabel + " " + entry.label() + " " + entry.key() + " " + entry.keywords())
                .contains(normalize(query));
    }
}
