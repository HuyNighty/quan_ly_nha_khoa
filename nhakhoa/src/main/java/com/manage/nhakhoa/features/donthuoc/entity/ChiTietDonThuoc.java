package com.manage.nhakhoa.features.donthuoc.entity;

import com.manage.nhakhoa.features.kho.entity.Thuoc;
import org.hibernate.annotations.Nationalized;

import jakarta.persistence.*;

@Entity
@Table(name = "ChiTietDonThuoc")
public class ChiTietDonThuoc {

    @EmbeddedId
    private ChiTietDonThuocId id = new ChiTietDonThuocId();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("maDonThuoc")
    @JoinColumn(name = "maDonThuoc", nullable = false)
    private DonThuoc donThuoc;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("maThuoc")
    @JoinColumn(name = "maThuoc", nullable = false)
    private Thuoc thuoc;

    @Column(name = "soLuong", nullable = false)
    private Integer soLuong;

    @Nationalized
    @Column(name = "lieuDung", nullable = false, length = 100)
    private String lieuDung;

    @Nationalized
    @Column(name = "cachDung", nullable = false, length = 200)
    private String cachDung;

    @Column(name = "soNgayDung", nullable = false)
    private Integer soNgayDung;

    public ChiTietDonThuoc() {}

    public ChiTietDonThuocId getId() { return id; }
    public void setId(ChiTietDonThuocId id) { this.id = id; }

    public DonThuoc getDonThuoc() { return donThuoc; }
    public void setDonThuoc(DonThuoc donThuoc) { this.donThuoc = donThuoc; }

    public Thuoc getThuoc() { return thuoc; }
    public void setThuoc(Thuoc thuoc) { this.thuoc = thuoc; }

    public Integer getSoLuong() { return soLuong; }
    public void setSoLuong(Integer soLuong) { this.soLuong = soLuong; }

    public String getLieuDung() { return lieuDung; }
    public void setLieuDung(String lieuDung) { this.lieuDung = lieuDung; }

    public String getCachDung() { return cachDung; }
    public void setCachDung(String cachDung) { this.cachDung = cachDung; }

    public Integer getSoNgayDung() { return soNgayDung; }
    public void setSoNgayDung(Integer soNgayDung) { this.soNgayDung = soNgayDung; }
}
