package com.manage.nhakhoa.features.dashboard.dto.response;

/** Trạng thái của từng section Dashboard (contract 03). Không dùng zero để che lỗi. */
public enum SectionStatus {
    /** Truy vấn thành công; các số đếm (kể cả 0) là hợp lệ. */
    AVAILABLE,
    /** Không tính được chính xác (lỗi truy vấn hoặc thiếu dữ liệu nguồn); các trường số là null. */
    UNAVAILABLE,
    /** Nghiệp vụ chưa có danh mục/cấu hình thật để kết luận. */
    UNCONFIGURED
}
