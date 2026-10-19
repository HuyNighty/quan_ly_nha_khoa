package com.manage.nhakhoa.features.hethong.entity;

import org.hibernate.annotations.Nationalized;

import jakarta.persistence.*;

@Entity
@Table(name = "ThamSoHeThong")
public class ThamSoHeThong {

    @Id
    @Column(name = "ten", nullable = false, length = 50)
    private String ten;

    @Column(name = "giaTri", nullable = false, length = 100)
    private String giaTri;

    @Nationalized
    @Column(name = "moTa", length = 300)
    private String moTa;

    public ThamSoHeThong() {}

    public String getTen() { return ten; }
    public void setTen(String ten) { this.ten = ten; }

    public String getGiaTri() { return giaTri; }
    public void setGiaTri(String giaTri) { this.giaTri = giaTri; }

    public String getMoTa() { return moTa; }
    public void setMoTa(String moTa) { this.moTa = moTa; }
}
