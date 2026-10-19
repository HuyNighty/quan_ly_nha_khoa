package com.manage.nhakhoa.features.dashboard.dto.response;

/** Phiếu khám tiếp nhận trong ngày D theo trạng thái — đếm PHIẾU, nhãn UI "Phiếu chờ khám" (BA V1 Q04). */
public record ExamSummary(long waiting, long inProgress, long concluded, String unit) {
}
