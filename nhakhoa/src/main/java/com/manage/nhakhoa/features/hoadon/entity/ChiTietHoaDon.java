package com.manage.nhakhoa.features.hoadon.entity;

import com.manage.nhakhoa.features.khamchuabenh.entity.DichVu;
import com.manage.nhakhoa.features.kho.entity.Thuoc;
import com.manage.nhakhoa.features.kho.entity.VatTu;
import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "ChiTietHoaDon")
public class ChiTietHoaDon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "maChiTiet", nullable = false)
    private Integer maChiTiet;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "maHoaDon", nullable = false)
    private HoaDon hoaDon;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "maDichVu")
    private DichVu dichVu;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "maThuoc")
    private Thuoc thuoc;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "maVatTu")
    private VatTu vatTu;

    @Column(name = "soLuong", nullable = false)
    private Integer soLuong;

    @Column(name = "donGiaApDung", nullable = false, precision = 18, scale = 0)
    private BigDecimal donGiaApDung;

    public ChiTietHoaDon() {}

    public Integer getMaChiTiet() { return maChiTiet; }
    public void setMaChiTiet(Integer maChiTiet) { this.maChiTiet = maChiTiet; }

    public HoaDon getHoaDon() { return hoaDon; }
    public void setHoaDon(HoaDon hoaDon) { this.hoaDon = hoaDon; }

    public DichVu getDichVu() { return dichVu; }
    public void setDichVu(DichVu dichVu) { this.dichVu = dichVu; }

    public Thuoc getThuoc() { return thuoc; }
    public void setThuoc(Thuoc thuoc) { this.thuoc = thuoc; }

    public VatTu getVatTu() { return vatTu; }
    public void setVatTu(VatTu vatTu) { this.vatTu = vatTu; }

    public Integer getSoLuong() { return soLuong; }
    public void setSoLuong(Integer soLuong) { this.soLuong = soLuong; }

    public BigDecimal getDonGiaApDung() { return donGiaApDung; }
    public void setDonGiaApDung(BigDecimal donGiaApDung) { this.donGiaApDung = donGiaApDung; }
}
