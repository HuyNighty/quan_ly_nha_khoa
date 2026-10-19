package com.manage.nhakhoa.features.dashboard.dto.response;

import java.time.OffsetDateTime;

/** Khoảng ngày nghiệp vụ nửa mở [startInclusive, endExclusive). */
public record PeriodDto(OffsetDateTime startInclusive, OffsetDateTime endExclusive) {
}
