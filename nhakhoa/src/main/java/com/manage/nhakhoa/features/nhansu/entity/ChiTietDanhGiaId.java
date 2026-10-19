package com.manage.nhakhoa.features.nhansu.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

/** Khóa chính phức hợp của bảng ChiTietDanhGia. */
@Embeddable
public class ChiTietDanhGiaId implements Serializable {

    @Column(name = "maDanhGia", nullable = false, length = 10)
    private String maDanhGia;

    @Column(name = "thuTu", nullable = false)
    private Integer thuTu;

    public ChiTietDanhGiaId() {}

    public ChiTietDanhGiaId(String maDanhGia, Integer thuTu) {
        this.maDanhGia = maDanhGia;
        this.thuTu = thuTu;
    }

    public String getMaDanhGia() { return maDanhGia; }
    public void setMaDanhGia(String maDanhGia) { this.maDanhGia = maDanhGia; }

    public Integer getThuTu() { return thuTu; }
    public void setThuTu(Integer thuTu) { this.thuTu = thuTu; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ChiTietDanhGiaId that)) return false;
        return Objects.equals(maDanhGia, that.maDanhGia) && Objects.equals(thuTu, that.thuTu);
    }

    @Override
    public int hashCode() {
        return Objects.hash(maDanhGia, thuTu);
    }
}
