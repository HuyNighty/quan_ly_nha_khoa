package com.manage.nhakhoa.features.dashboard.repository;

import com.manage.nhakhoa.features.capcuu.enums.KetQuaKiemTra;

import java.time.LocalDateTime;

/** Bản KiemTraCapCuu gần nhất (không gồm người kiểm tra/ghi chú). */
public record EmergencyCheckRow(String checkId, LocalDateTime checkedAt, KetQuaKiemTra result) {
}
