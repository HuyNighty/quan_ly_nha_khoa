package com.manage.nhakhoa.features.nhansu.entity;

import org.hibernate.annotations.Nationalized;

import jakarta.persistence.*;

import java.time.LocalTime;

@Entity
@Table(name = "CaLamViec")
public class CaLamViec {

    @Id
    @Column(name = "maCa", nullable = false, length = 10)
    private String maCa;

    @Nationalized
    @Column(name = "tenCa", nullable = false, length = 50)
    private String tenCa;

    @Column(name = "gioBatDau", nullable = false)
    private LocalTime gioBatDau;

    @Column(name = "gioKetThuc", nullable = false)
    private LocalTime gioKetThuc;

    @Column(name = "soBacSi", nullable = false)
    private Integer soBacSi = 0;

    @Column(name = "soYTa", nullable = false)
    private Integer soYTa = 0;

    @Column(name = "soLeTan", nullable = false)
    private Integer soLeTan = 0;

    public CaLamViec() {}

    public String getMaCa() { return maCa; }
    public void setMaCa(String maCa) { this.maCa = maCa; }

    public String getTenCa() { return tenCa; }
    public void setTenCa(String tenCa) { this.tenCa = tenCa; }

    public LocalTime getGioBatDau() { return gioBatDau; }
    public void setGioBatDau(LocalTime gioBatDau) { this.gioBatDau = gioBatDau; }

    public LocalTime getGioKetThuc() { return gioKetThuc; }
    public void setGioKetThuc(LocalTime gioKetThuc) { this.gioKetThuc = gioKetThuc; }

    public Integer getSoBacSi() { return soBacSi; }
    public void setSoBacSi(Integer soBacSi) { this.soBacSi = soBacSi; }

    public Integer getSoYTa() { return soYTa; }
    public void setSoYTa(Integer soYTa) { this.soYTa = soYTa; }

    public Integer getSoLeTan() { return soLeTan; }
    public void setSoLeTan(Integer soLeTan) { this.soLeTan = soLeTan; }
}
