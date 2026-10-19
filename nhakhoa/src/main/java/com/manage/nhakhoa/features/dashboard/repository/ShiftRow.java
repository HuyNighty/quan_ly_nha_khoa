package com.manage.nhakhoa.features.dashboard.repository;

import java.time.LocalTime;

/** Định mức nhân sự của CaLamViec. Các định mức có thể null nếu dữ liệu bất thường. */
public record ShiftRow(String shiftCode, String shiftName, LocalTime startTime, LocalTime endTime,
                       Integer requiredBacSi, Integer requiredYTa, Integer requiredLeTan) {
}
