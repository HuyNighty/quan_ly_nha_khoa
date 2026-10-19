package com.manage.nhakhoa.features.khamchuabenh.entity;

import org.hibernate.annotations.Nationalized;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "DichVu")
public class DichVu {

    @Id
    @Column(name = "maDichVu", nullable = false, length = 10)
    private String maDichVu;

    @Nationalized
    @Column(name = "tenDichVu", nullable = false, length = 100)
    private String tenDichVu;

    @Column(name = "donGia", nullable = false, precision = 18, scale = 0)
    private BigDecimal donGia;

    @Nationalized
    @Column(name = "moTa", length = 300)
    private String moTa;

    @Column(name = "laCanLamSang", nullable = false)
    private Boolean laCanLamSang = false;

    @Column(name = "dangApDung", nullable = false)
    private Boolean dangApDung = true;

    public DichVu() {}

    public String getMaDichVu() { return maDichVu; }
    public void setMaDichVu(String maDichVu) { this.maDichVu = maDichVu; }

    public String getTenDichVu() { return tenDichVu; }
    public void setTenDichVu(String tenDichVu) { this.tenDichVu = tenDichVu; }

    public BigDecimal getDonGia() { return donGia; }
    public void setDonGia(BigDecimal donGia) { this.donGia = donGia; }

    public String getMoTa() { return moTa; }
    public void setMoTa(String moTa) { this.moTa = moTa; }

    public Boolean getLaCanLamSang() { return laCanLamSang; }
    public void setLaCanLamSang(Boolean laCanLamSang) { this.laCanLamSang = laCanLamSang; }

    public Boolean getDangApDung() { return dangApDung; }
    public void setDangApDung(Boolean dangApDung) { this.dangApDung = dangApDung; }
}
