package com.manage.nhakhoa.features.kho.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "ChiTietPhieuXuat")
public class ChiTietPhieuXuat {

    @EmbeddedId
    private ChiTietPhieuXuatId id = new ChiTietPhieuXuatId();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("maPhieuXuat")
    @JoinColumn(name = "maPhieuXuat", nullable = false)
    private PhieuXuatKho phieuXuat;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("maLo")
    @JoinColumn(name = "maLo", nullable = false)
    private LoHang loHang;

    @Column(name = "soLuong", nullable = false)
    private Integer soLuong;

    public ChiTietPhieuXuat() {}

    public ChiTietPhieuXuatId getId() { return id; }
    public void setId(ChiTietPhieuXuatId id) { this.id = id; }

    public PhieuXuatKho getPhieuXuat() { return phieuXuat; }
    public void setPhieuXuat(PhieuXuatKho phieuXuat) { this.phieuXuat = phieuXuat; }

    public LoHang getLoHang() { return loHang; }
    public void setLoHang(LoHang loHang) { this.loHang = loHang; }

    public Integer getSoLuong() { return soLuong; }
    public void setSoLuong(Integer soLuong) { this.soLuong = soLuong; }
}
