package com.manage.nhakhoa.features.dashboard.service;

/**
 * Khóa tham số (ThamSoHeThong.ten) và giá trị fallback đã công bố trong BA V1.
 * Service chỉ ĐỌC tham số; thiếu/không hợp lệ thì dùng fallback và phát cảnh báo, không tự ghi DB.
 */
public final class DashboardThresholds {

    /** BA V1 Q10. */
    public static final String KEY_NEAR_EXPIRY_DAYS = "SO_NGAY_CANH_BAO_HSD";
    public static final int DEFAULT_NEAR_EXPIRY_DAYS = 30;

    /** BA V1 Q11. */
    public static final String KEY_EMERGENCY_CHECK_CYCLE_DAYS = "CHU_KY_KIEM_TRA_CAP_CUU_NGAY";
    public static final int DEFAULT_EMERGENCY_CHECK_CYCLE_DAYS = 7;

    /** BA V1 Q11. */
    public static final String KEY_EMERGENCY_SAFE_EXPIRY_DAYS = "SO_NGAY_AN_TOAN_CAP_CUU";
    public static final int DEFAULT_EMERGENCY_SAFE_EXPIRY_DAYS = 30;

    /**
     * BA R1 chấp nhận khóa SO_NGAY_KE_HOACH_DEN_HAN và fallback 14 ngày.
     * Không tự thêm bản ghi cấu hình; thiếu khóa vẫn phát cảnh báo provenance.
     */
    public static final String KEY_TREATMENT_HORIZON_DAYS = "SO_NGAY_KE_HOACH_DEN_HAN";
    public static final int DEFAULT_TREATMENT_HORIZON_DAYS = 14;

    /** Giới hạn trên hợp lý cho mọi ngưỡng ngày (10 năm), chặn cấu hình sai. */
    static final int MAX_DAYS = 3650;

    private DashboardThresholds() {
    }
}
