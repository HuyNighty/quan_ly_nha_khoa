package com.manage.nhakhoa.features.dashboard.dto.response;

/**
 * Kết quả đánh giá lịch kiểm tra hộp cấp cứu (BA V1 Q11).
 * Không có giá trị "ĐẠT/AN TOÀN": {@link #TRONG_CHU_KY} chỉ nghĩa là đã kiểm tra trong chu kỳ
 * và không phát hiện mục cần bổ sung, không phải chứng nhận an toàn lâm sàng.
 */
public enum EmergencyCheckState {
    CHUA_CO_KIEM_TRA,
    /** Thiếu bao phủ mục chính thức hoặc thiếu thông tin để xác nhận lần kiểm tra. */
    CHUA_DU_THONG_TIN,
    QUA_CHU_KY,
    CAN_XU_LY,
    TRONG_CHU_KY
}
