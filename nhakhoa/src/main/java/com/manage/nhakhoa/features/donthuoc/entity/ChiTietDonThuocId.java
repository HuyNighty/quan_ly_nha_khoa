package com.manage.nhakhoa.features.donthuoc.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

/** Khóa chính phức hợp của bảng ChiTietDonThuoc. */
@Embeddable
public class ChiTietDonThuocId implements Serializable {

    @Column(name = "maDonThuoc", nullable = false, length = 10)
    private String maDonThuoc;

    @Column(name = "maThuoc", nullable = false, length = 10)
    private String maThuoc;

    public ChiTietDonThuocId() {}

    public ChiTietDonThuocId(String maDonThuoc, String maThuoc) {
        this.maDonThuoc = maDonThuoc;
        this.maThuoc = maThuoc;
    }

    public String getMaDonThuoc() { return maDonThuoc; }
    public void setMaDonThuoc(String maDonThuoc) { this.maDonThuoc = maDonThuoc; }

    public String getMaThuoc() { return maThuoc; }
    public void setMaThuoc(String maThuoc) { this.maThuoc = maThuoc; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ChiTietDonThuocId that)) return false;
        return Objects.equals(maDonThuoc, that.maDonThuoc) && Objects.equals(maThuoc, that.maThuoc);
    }

    @Override
    public int hashCode() {
        return Objects.hash(maDonThuoc, maThuoc);
    }
}
