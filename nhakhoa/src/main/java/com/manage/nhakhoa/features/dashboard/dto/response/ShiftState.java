package com.manage.nhakhoa.features.dashboard.dto.response;

/** Trạng thái định mức của một ca trong ngày nghiệp vụ (theo lịch phân công, không phải chấm công). */
public enum ShiftState {
    DU_NHAN_SU,
    THIEU_NHAN_SU,
    /** Giờ ca hoặc định mức không hợp lệ (ví dụ giờ bắt đầu = giờ kết thúc, định mức âm). */
    LOI_CAU_HINH
}
