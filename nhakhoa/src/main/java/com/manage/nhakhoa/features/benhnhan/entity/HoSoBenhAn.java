package com.manage.nhakhoa.features.benhnhan.entity;

import org.hibernate.annotations.Nationalized;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "HoSoBenhAn")
public class HoSoBenhAn {

    @Id
    @Column(name = "maHoSo", nullable = false, length = 10)
    private String maHoSo;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "maBenhNhan", nullable = false, unique = true)
    private BenhNhan benhNhan;

    @Column(name = "ngayLap", nullable = false)
    private LocalDate ngayLap;

    @Nationalized
    @Column(name = "tienSuBenh", length = 500)
    private String tienSuBenh;

    @Nationalized
    @Column(name = "diUng", length = 300)
    private String diUng;

    public HoSoBenhAn() {}

    public String getMaHoSo() { return maHoSo; }
    public void setMaHoSo(String maHoSo) { this.maHoSo = maHoSo; }

    public BenhNhan getBenhNhan() { return benhNhan; }
    public void setBenhNhan(BenhNhan benhNhan) { this.benhNhan = benhNhan; }

    public LocalDate getNgayLap() { return ngayLap; }
    public void setNgayLap(LocalDate ngayLap) { this.ngayLap = ngayLap; }

    public String getTienSuBenh() { return tienSuBenh; }
    public void setTienSuBenh(String tienSuBenh) { this.tienSuBenh = tienSuBenh; }

    public String getDiUng() { return diUng; }
    public void setDiUng(String diUng) { this.diUng = diUng; }
}
