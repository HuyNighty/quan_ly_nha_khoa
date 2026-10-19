package com.manage.nhakhoa.features.kho.entity;

import com.manage.nhakhoa.features.kho.enums.LoaiNhap;
import com.manage.nhakhoa.features.kho.enums.TrangThaiPhieuKho;
import com.manage.nhakhoa.features.nhansu.entity.NhanVien;
import org.hibernate.annotations.Nationalized;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "PhieuNhapKho")
public class PhieuNhapKho {

    @Id
    @Column(name = "maPhieuNhap", nullable = false, length = 10)
    private String maPhieuNhap;

    @Column(name = "ngayNhap", nullable = false)
    private LocalDateTime ngayNhap;

    @Enumerated(EnumType.STRING)
    @Column(name = "trangThai", nullable = false, length = 20)
    private TrangThaiPhieuKho trangThai = TrangThaiPhieuKho.HOAN_TAT;

    @Enumerated(EnumType.STRING)
    @Column(name = "loaiNhap", nullable = false, length = 10)
    private LoaiNhap loaiNhap = LoaiNhap.NHAP_NCC;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "maNhaCungCap")
    private NhaCungCap nhaCungCap;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "maPhieuXuatGoc")
    private PhieuXuatKho phieuXuatGoc;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "maNhanVien", nullable = false)
    private NhanVien nhanVien;

    @Nationalized
    @Column(name = "ghiChu", length = 300)
    private String ghiChu;

    public PhieuNhapKho() {}

    public String getMaPhieuNhap() { return maPhieuNhap; }
    public void setMaPhieuNhap(String maPhieuNhap) { this.maPhieuNhap = maPhieuNhap; }

    public LocalDateTime getNgayNhap() { return ngayNhap; }
    public void setNgayNhap(LocalDateTime ngayNhap) { this.ngayNhap = ngayNhap; }

    public TrangThaiPhieuKho getTrangThai() { return trangThai; }
    public void setTrangThai(TrangThaiPhieuKho trangThai) { this.trangThai = trangThai; }

    public LoaiNhap getLoaiNhap() { return loaiNhap; }
    public void setLoaiNhap(LoaiNhap loaiNhap) { this.loaiNhap = loaiNhap; }

    public NhaCungCap getNhaCungCap() { return nhaCungCap; }
    public void setNhaCungCap(NhaCungCap nhaCungCap) { this.nhaCungCap = nhaCungCap; }

    public PhieuXuatKho getPhieuXuatGoc() { return phieuXuatGoc; }
    public void setPhieuXuatGoc(PhieuXuatKho phieuXuatGoc) { this.phieuXuatGoc = phieuXuatGoc; }

    public NhanVien getNhanVien() { return nhanVien; }
    public void setNhanVien(NhanVien nhanVien) { this.nhanVien = nhanVien; }

    public String getGhiChu() { return ghiChu; }
    public void setGhiChu(String ghiChu) { this.ghiChu = ghiChu; }
}
