package com.manage.nhakhoa.features.capcuu.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

/** Khóa chính phức hợp của bảng ChiTietKiemTraCapCuu. */
@Embeddable
public class ChiTietKiemTraCapCuuId implements Serializable {

    @Column(name = "maKiemTra", nullable = false, length = 10)
    private String maKiemTra;

    @Column(name = "maMuc", nullable = false, length = 10)
    private String maMuc;

    public ChiTietKiemTraCapCuuId() {}

    public ChiTietKiemTraCapCuuId(String maKiemTra, String maMuc) {
        this.maKiemTra = maKiemTra;
        this.maMuc = maMuc;
    }

    public String getMaKiemTra() { return maKiemTra; }
    public void setMaKiemTra(String maKiemTra) { this.maKiemTra = maKiemTra; }

    public String getMaMuc() { return maMuc; }
    public void setMaMuc(String maMuc) { this.maMuc = maMuc; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ChiTietKiemTraCapCuuId that)) return false;
        return Objects.equals(maKiemTra, that.maKiemTra) && Objects.equals(maMuc, that.maMuc);
    }

    @Override
    public int hashCode() {
        return Objects.hash(maKiemTra, maMuc);
    }
}
