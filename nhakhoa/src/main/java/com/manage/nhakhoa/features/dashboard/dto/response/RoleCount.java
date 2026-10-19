package com.manage.nhakhoa.features.dashboard.dto.response;

/** Số người theo chức danh BS / YT / LT. */
public record RoleCount(int bacSi, int yTa, int leTan) {
    public static final RoleCount ZERO = new RoleCount(0, 0, 0);
}
