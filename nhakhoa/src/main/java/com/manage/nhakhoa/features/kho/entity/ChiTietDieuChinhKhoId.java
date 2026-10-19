package com.manage.nhakhoa.features.kho.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

/** Khóa chính phức hợp của bảng ChiTietDieuChinhKho. */
@Embeddable
public class ChiTietDieuChinhKhoId implements Serializable {

    @Column(name = "maPhieu", nullable = false, length = 10)
    private String maPhieu;

    @Column(name = "maLo", nullable = false, length = 10)
    private String maLo;

    public ChiTietDieuChinhKhoId() {}

    public ChiTietDieuChinhKhoId(String maPhieu, String maLo) {
        this.maPhieu = maPhieu;
        this.maLo = maLo;
    }

    public String getMaPhieu() { return maPhieu; }
    public void setMaPhieu(String maPhieu) { this.maPhieu = maPhieu; }

    public String getMaLo() { return maLo; }
    public void setMaLo(String maLo) { this.maLo = maLo; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ChiTietDieuChinhKhoId that)) return false;
        return Objects.equals(maPhieu, that.maPhieu) && Objects.equals(maLo, that.maLo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(maPhieu, maLo);
    }
}
