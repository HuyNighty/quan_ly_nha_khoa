package com.manage.nhakhoa.features.kho.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "ChiTietDieuChinhKho")
public class ChiTietDieuChinhKho {

    @EmbeddedId
    private ChiTietDieuChinhKhoId id = new ChiTietDieuChinhKhoId();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("maPhieu")
    @JoinColumn(name = "maPhieu", nullable = false)
    private PhieuDieuChinhKho phieu;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("maLo")
    @JoinColumn(name = "maLo", nullable = false)
    private LoHang loHang;

    @Column(name = "soLuongHeThong", nullable = false)
    private Integer soLuongHeThong;

    @Column(name = "soLuongThucTe", nullable = false)
    private Integer soLuongThucTe;

    public ChiTietDieuChinhKho() {}

    public ChiTietDieuChinhKhoId getId() { return id; }
    public void setId(ChiTietDieuChinhKhoId id) { this.id = id; }

    public PhieuDieuChinhKho getPhieu() { return phieu; }
    public void setPhieu(PhieuDieuChinhKho phieu) { this.phieu = phieu; }

    public LoHang getLoHang() { return loHang; }
    public void setLoHang(LoHang loHang) { this.loHang = loHang; }

    public Integer getSoLuongHeThong() { return soLuongHeThong; }
    public void setSoLuongHeThong(Integer soLuongHeThong) { this.soLuongHeThong = soLuongHeThong; }

    public Integer getSoLuongThucTe() { return soLuongThucTe; }
    public void setSoLuongThucTe(Integer soLuongThucTe) { this.soLuongThucTe = soLuongThucTe; }
}
