package com.manage.nhakhoa.features.kho.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

/** Khóa chính phức hợp của bảng ChiTietPhieuXuat. */
@Embeddable
public class ChiTietPhieuXuatId implements Serializable {

    @Column(name = "maPhieuXuat", nullable = false, length = 10)
    private String maPhieuXuat;

    @Column(name = "maLo", nullable = false, length = 10)
    private String maLo;

    public ChiTietPhieuXuatId() {}

    public ChiTietPhieuXuatId(String maPhieuXuat, String maLo) {
        this.maPhieuXuat = maPhieuXuat;
        this.maLo = maLo;
    }

    public String getMaPhieuXuat() { return maPhieuXuat; }
    public void setMaPhieuXuat(String maPhieuXuat) { this.maPhieuXuat = maPhieuXuat; }

    public String getMaLo() { return maLo; }
    public void setMaLo(String maLo) { this.maLo = maLo; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ChiTietPhieuXuatId that)) return false;
        return Objects.equals(maPhieuXuat, that.maPhieuXuat) && Objects.equals(maLo, that.maLo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(maPhieuXuat, maLo);
    }
}
