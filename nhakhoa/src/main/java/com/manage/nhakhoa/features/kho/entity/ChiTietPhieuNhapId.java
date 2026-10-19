package com.manage.nhakhoa.features.kho.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

/** Khóa chính phức hợp của bảng ChiTietPhieuNhap. */
@Embeddable
public class ChiTietPhieuNhapId implements Serializable {

    @Column(name = "maPhieuNhap", nullable = false, length = 10)
    private String maPhieuNhap;

    @Column(name = "maLo", nullable = false, length = 10)
    private String maLo;

    public ChiTietPhieuNhapId() {}

    public ChiTietPhieuNhapId(String maPhieuNhap, String maLo) {
        this.maPhieuNhap = maPhieuNhap;
        this.maLo = maLo;
    }

    public String getMaPhieuNhap() { return maPhieuNhap; }
    public void setMaPhieuNhap(String maPhieuNhap) { this.maPhieuNhap = maPhieuNhap; }

    public String getMaLo() { return maLo; }
    public void setMaLo(String maLo) { this.maLo = maLo; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ChiTietPhieuNhapId that)) return false;
        return Objects.equals(maPhieuNhap, that.maPhieuNhap) && Objects.equals(maLo, that.maLo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(maPhieuNhap, maLo);
    }
}
