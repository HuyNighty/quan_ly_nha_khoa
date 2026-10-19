package com.manage.nhakhoa.features.hoadon.entity;

import com.manage.nhakhoa.features.hoadon.enums.TrangThaiHoaDon;
import com.manage.nhakhoa.features.khamchuabenh.entity.PhieuKham;
import com.manage.nhakhoa.features.benhnhan.entity.BenhNhan;
import com.manage.nhakhoa.features.nhansu.entity.NhanVien;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "HoaDon",
       indexes = {@Index(name = "IX_HoaDon_BenhNhan", columnList = "maBenhNhan")})
public class HoaDon {

    @Id
    @Column(name = "maHoaDon", nullable = false, length = 10)
    private String maHoaDon;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "maNhanVien", nullable = false)
    private NhanVien nhanVien;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "maBenhNhan", nullable = false)
    private BenhNhan benhNhan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "maPhieuKham")
    private PhieuKham phieuKham;

    @Column(name = "ngayLap", nullable = false)
    private LocalDateTime ngayLap;

    @Enumerated(EnumType.STRING)
    @Column(name = "trangThai", nullable = false, length = 20)
    private TrangThaiHoaDon trangThai = TrangThaiHoaDon.CHUA_THANH_TOAN;

    public HoaDon() {}

    public String getMaHoaDon() { return maHoaDon; }
    public void setMaHoaDon(String maHoaDon) { this.maHoaDon = maHoaDon; }

    public NhanVien getNhanVien() { return nhanVien; }
    public void setNhanVien(NhanVien nhanVien) { this.nhanVien = nhanVien; }

    public BenhNhan getBenhNhan() { return benhNhan; }
    public void setBenhNhan(BenhNhan benhNhan) { this.benhNhan = benhNhan; }

    public PhieuKham getPhieuKham() { return phieuKham; }
    public void setPhieuKham(PhieuKham phieuKham) { this.phieuKham = phieuKham; }

    public LocalDateTime getNgayLap() { return ngayLap; }
    public void setNgayLap(LocalDateTime ngayLap) { this.ngayLap = ngayLap; }

    public TrangThaiHoaDon getTrangThai() { return trangThai; }
    public void setTrangThai(TrangThaiHoaDon trangThai) { this.trangThai = trangThai; }
}
