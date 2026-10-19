package com.manage.nhakhoa.features.dashboard.service;

/**
 * Lỗi truy vấn ở section lõi (lịch hẹn, phiếu khám, thu, công nợ). Contract 03: không trả payload toàn 0,
 * tầng HTTP (khi được phép bật) phải map sang 503. Message không chứa SQL, tham số hay dữ liệu cá nhân.
 */
public class DashboardDataUnavailableException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String section;

    public DashboardDataUnavailableException(String section, Throwable cause) {
        super("Không tải được số liệu Dashboard ở section " + section, cause);
        this.section = section;
    }

    public String getSection() {
        return section;
    }
}
