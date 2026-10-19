package com.manage.nhakhoa.features.dashboard.dto.response;

/** Đến hạn (bao gồm quá hạn): DISTINCT maKeHoach; duePlanCount bao gồm overduePlanCount, không cộng như hai tập rời. */
public record TreatmentDueSummary(SectionStatus status, Long duePlanCount, Long overduePlanCount, Integer horizonDays) {
}
