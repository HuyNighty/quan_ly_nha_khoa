package com.manage.nhakhoa.features.dashboard.dto.response;

import java.time.OffsetDateTime;

/** Hộp cấp cứu (BA V1 Q11). Không bao giờ kết luận "đạt/an toàn". */
public record EmergencyKitSummary(
        SectionStatus status,
        EmergencyCheckState checkState,
        OffsetDateTime lastCheckedAt,
        Long daysSinceLastCheck,
        Integer checkCycleDays,
        String message) {
}
