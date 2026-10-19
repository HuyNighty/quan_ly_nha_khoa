package com.manage.nhakhoa.features.nhansu.entity;

import org.hibernate.annotations.Nationalized;

import jakarta.persistence.*;

@Entity
@Table(name = "ChiTietDanhGia")
public class ChiTietDanhGia {

    @EmbeddedId
    private ChiTietDanhGiaId id = new ChiTietDanhGiaId();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("maDanhGia")
    @JoinColumn(name = "maDanhGia", nullable = false)
    private DanhGiaNhanSu danhGia;

    @Nationalized
    @Column(name = "tieuChi", nullable = false, length = 100)
    private String tieuChi;

    @Column(name = "diem", nullable = false)
    private Integer diem;

    public ChiTietDanhGia() {}

    public ChiTietDanhGiaId getId() { return id; }
    public void setId(ChiTietDanhGiaId id) { this.id = id; }

    public DanhGiaNhanSu getDanhGia() { return danhGia; }
    public void setDanhGia(DanhGiaNhanSu danhGia) { this.danhGia = danhGia; }

    public String getTieuChi() { return tieuChi; }
    public void setTieuChi(String tieuChi) { this.tieuChi = tieuChi; }

    public Integer getDiem() { return diem; }
    public void setDiem(Integer diem) { this.diem = diem; }
}
