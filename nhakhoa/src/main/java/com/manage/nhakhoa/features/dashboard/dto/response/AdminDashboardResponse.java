package com.manage.nhakhoa.features.dashboard.dto.response;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/** Read model Dashboard Admin theo contract 03-DASHBOARD-API-CONTRACT-V1. Không chứa Entity JPA, PII hoặc PHI. */
public record AdminDashboardResponse(
        String source,
        LocalDate businessDate,
        String zoneId,
        OffsetDateTime generatedAt,
        PeriodDto period,
        AppointmentSummary appointments,
        ExamSummary exams,
        CollectionSummary collections,
        ReceivableSummary receivables,
        StaffingSummary staffing,
        InventorySummary inventory,
        EmergencyKitSummary emergencyKit,
        TreatmentDueSummary treatmentDue,
        WorkQueueDto workQueue,
        List<DashboardWarning> warnings) {
    public AdminDashboardResponse {
        warnings = List.copyOf(warnings);
    }
}
