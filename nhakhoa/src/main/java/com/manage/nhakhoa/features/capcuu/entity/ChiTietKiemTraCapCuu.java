package com.manage.nhakhoa.features.capcuu.entity;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "ChiTietKiemTraCapCuu")
public class ChiTietKiemTraCapCuu {

    @EmbeddedId
    private ChiTietKiemTraCapCuuId id = new ChiTietKiemTraCapCuuId();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("maKiemTra")
    @JoinColumn(name = "maKiemTra", nullable = false)
    private KiemTraCapCuu kiemTra;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("maMuc")
    @JoinColumn(name = "maMuc", nullable = false)
    private DanhMucCapCuu danhMucCapCuu;

    @Column(name = "soLuongThucTe", nullable = false)
    private Integer soLuongThucTe;

    @Column(name = "hanSuDung")
    private LocalDate hanSuDung;

    @Column(name = "dat", nullable = false)
    private Boolean dat;

    public ChiTietKiemTraCapCuu() {}

    public ChiTietKiemTraCapCuuId getId() { return id; }
    public void setId(ChiTietKiemTraCapCuuId id) { this.id = id; }

    public KiemTraCapCuu getKiemTra() { return kiemTra; }
    public void setKiemTra(KiemTraCapCuu kiemTra) { this.kiemTra = kiemTra; }

    public DanhMucCapCuu getDanhMucCapCuu() { return danhMucCapCuu; }
    public void setDanhMucCapCuu(DanhMucCapCuu danhMucCapCuu) { this.danhMucCapCuu = danhMucCapCuu; }

    public Integer getSoLuongThucTe() { return soLuongThucTe; }
    public void setSoLuongThucTe(Integer soLuongThucTe) { this.soLuongThucTe = soLuongThucTe; }

    public LocalDate getHanSuDung() { return hanSuDung; }
    public void setHanSuDung(LocalDate hanSuDung) { this.hanSuDung = hanSuDung; }

    public Boolean getDat() { return dat; }
    public void setDat(Boolean dat) { this.dat = dat; }
}
