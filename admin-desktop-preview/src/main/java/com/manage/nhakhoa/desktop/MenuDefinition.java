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
    /**
     * Menu hướng nghiệp vụ: mỗi nhóm là một giai đoạn hoạt động thực tế của phòng khám, xếp theo thứ tự
     * Tổ chức → Lập kế hoạch → Tiếp nhận → Khám → Điều trị → Thanh toán → Kho → Tổng kết.
     * Khóa màn hình (TIEP_NHAN, LICH_HEN…) giữ nguyên; chỉ đổi nhóm và nhãn.
     * Badge nhóm = số nhóm việc trên Tổng quan trỏ tới màn hình trong nhóm (tổng 9, khớp "9 nhóm việc").
     */
    public static final List<Group> GROUPS = List.of(
        new Group("organize", "Tổ chức", 1, List.of(
            e("NHAN_VIEN", "Nhân viên", "UC22 UC26 hồ sơ nhân viên quản lý nhân sự", "person"),
            e("TAI_KHOAN", "Tài khoản & vai trò", "UC21 phân quyền vai trò AD LT BS YT QL KHO", "lock"),
            e("BANG_GIA", "Bảng giá dịch vụ", "giá dịch vụ danh mục", "document"),
            e("NHA_CUNG_CAP", "Nhà cung cấp", "nhập kho đối tác", "box"))),
        new Group("plan", "Lập kế hoạch", 1, List.of(
            e("LICH_HEN", "Lịch hẹn", "UC03 UC06 UC07 đặt lịch", "calendar"),
            e("PHAN_CONG", "Phân công & lịch làm việc", "UC17 UC18 UC20 UC24 ca trực", "calendar"))),
        new Group("reception", "Tiếp nhận", 0, List.of(
            e("TIEP_NHAN", "Tiếp nhận bệnh nhân", "UC01 UC02 UC08 check-in", "person"),
            e("BENH_NHAN", "Hồ sơ bệnh nhân", "UC09 UC23", "person"))),
        new Group("exam", "Khám", 0, List.of(
            e("PHIEU_KHAM", "Phiếu khám", "UC10 UC11 UC16 chẩn đoán", "tooth"))),
        new Group("treatment", "Điều trị", 1, List.of(
            e("KE_HOACH", "Kế hoạch điều trị", "UC12 UC13 UC15", "document"),
            e("TAI_KHAM", "Tái khám", "UC14 hạng mục đến hạn", "clock"))),
        new Group("billing", "Thanh toán", 1, List.of(
            e("HOA_DON", "Hóa đơn & thu tiền", "UC04 UC05 công nợ", "wallet"),
            e("DUYET_HUY", "Duyệt hủy hóa đơn", "yêu cầu hủy", "document"))),
        new Group("stock", "Cung ứng", 4, List.of(
            e("THUOC_VAT_TU", "Thuốc & vật tư", "UC27 UC29 UC33 UC36 UC37 tồn kho", "box"),
            e("NHAP_XUAT", "Nhập / xuất / hoàn trả", "UC30 UC31 UC32", "exchange"),
            e("KIEM_KE", "Kiểm kê & hao hụt", "UC28 UC35", "document"),
            e("CAP_CUU", "Hộp cấp cứu", "UC34", "medical"))),
        new Group("summary", "Tổng kết", 1, List.of(
            e("BAN_GIAO", "Bàn giao ca", "UC19 tổng kết ca", "exchange"),
            e("DANH_GIA", "Đánh giá nhân viên", "UC25 đánh giá", "document"),
            e("NHAT_KY", "Nhật ký thao tác", "nhật ký audit", "document")))
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
