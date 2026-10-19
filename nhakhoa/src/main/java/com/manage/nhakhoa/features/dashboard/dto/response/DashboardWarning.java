package com.manage.nhakhoa.features.dashboard.dto.response;

/** Cảnh báo không nhạy cảm (không chứa PII/PHI, không chứa nội dung lỗi SQL). */
public record DashboardWarning(String code, String section, String message) {
}
