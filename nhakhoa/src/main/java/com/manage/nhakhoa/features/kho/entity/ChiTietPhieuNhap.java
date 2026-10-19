package com.manage.nhakhoa.features.kho.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "ChiTietPhieuNhap")
public class ChiTietPhieuNhap {

    @EmbeddedId
    private ChiTietPhieuNhapId id = new ChiTietPhieuNhapId();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("maPhieuNhap")
    @JoinColumn(name = "maPhieuNhap", nullable = false)
    private PhieuNhapKho phieuNhap;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("maLo")
    @JoinColumn(name = "maLo", nullable = false)
    private LoHang loHang;

    @Column(name = "soLuong", nullable = false)
    private Integer soLuong;

    @Column(name = "donGiaNhap", nullable = false, precision = 18, scale = 0)
    private BigDecimal donGiaNhap;

    public ChiTietPhieuNhap() {}

    public ChiTietPhieuNhapId getId() { return id; }
    public void setId(ChiTietPhieuNhapId id) { this.id = id; }

    public PhieuNhapKho getPhieuNhap() { return phieuNhap; }
    public void setPhieuNhap(PhieuNhapKho phieuNhap) { this.phieuNhap = phieuNhap; }

    public LoHang getLoHang() { return loHang; }
    public void setLoHang(LoHang loHang) { this.loHang = loHang; }

    public Integer getSoLuong() { return soLuong; }
    public void setSoLuong(Integer soLuong) { this.soLuong = soLuong; }

    public BigDecimal getDonGiaNhap() { return donGiaNhap; }
    public void setDonGiaNhap(BigDecimal donGiaNhap) { this.donGiaNhap = donGiaNhap; }
}
