package com.manage.nhakhoa.features.nhansu.entity;

import com.manage.nhakhoa.features.nhansu.enums.TrangThaiNhanVien;
import com.manage.nhakhoa.shared.enums.GioiTinh;
import com.manage.nhakhoa.shared.enums.VaiTro;
import org.hibernate.annotations.Nationalized;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "NhanVien")
public class NhanVien {

    @Id
    @Column(name = "maNhanVien", nullable = false, length = 10)
    private String maNhanVien;

    @Nationalized
    @Column(name = "hoTen", nullable = false, length = 100)
    private String hoTen;

    @Column(name = "soDienThoai", nullable = false, length = 15, unique = true)
    private String soDienThoai;

    @Enumerated(EnumType.STRING)
    @Column(name = "chucDanh", nullable = false, length = 10)
    private VaiTro chucDanh;

    @Enumerated(EnumType.STRING)
    @Column(name = "trangThai", nullable = false, length = 20)
    private TrangThaiNhanVien trangThai = TrangThaiNhanVien.DANG_LAM_VIEC;

    @Column(name = "ngaySinh")
    private LocalDate ngaySinh;

    @Enumerated(EnumType.STRING)
    @Column(name = "gioiTinh", length = 5)
    private GioiTinh gioiTinh;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "ngayVaoLam")
    private LocalDate ngayVaoLam;

    @Nationalized
    @Column(name = "chuyenMon", length = 200)
    private String chuyenMon;

    @Column(name = "ngayNghiViec")
    private LocalDate ngayNghiViec;

    public NhanVien() {}

    public String getMaNhanVien() { return maNhanVien; }
    public void setMaNhanVien(String maNhanVien) { this.maNhanVien = maNhanVien; }

    public String getHoTen() { return hoTen; }
    public void setHoTen(String hoTen) { this.hoTen = hoTen; }

    public String getSoDienThoai() { return soDienThoai; }
    public void setSoDienThoai(String soDienThoai) { this.soDienThoai = soDienThoai; }

    public VaiTro getChucDanh() { return chucDanh; }
    public void setChucDanh(VaiTro chucDanh) { this.chucDanh = chucDanh; }

    public TrangThaiNhanVien getTrangThai() { return trangThai; }
    public void setTrangThai(TrangThaiNhanVien trangThai) { this.trangThai = trangThai; }

    public LocalDate getNgaySinh() { return ngaySinh; }
    public void setNgaySinh(LocalDate ngaySinh) { this.ngaySinh = ngaySinh; }

    public GioiTinh getGioiTinh() { return gioiTinh; }
    public void setGioiTinh(GioiTinh gioiTinh) { this.gioiTinh = gioiTinh; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public LocalDate getNgayVaoLam() { return ngayVaoLam; }
    public void setNgayVaoLam(LocalDate ngayVaoLam) { this.ngayVaoLam = ngayVaoLam; }

    public String getChuyenMon() { return chuyenMon; }
    public void setChuyenMon(String chuyenMon) { this.chuyenMon = chuyenMon; }

    public LocalDate getNgayNghiViec() { return ngayNghiViec; }
    public void setNgayNghiViec(LocalDate ngayNghiViec) { this.ngayNghiViec = ngayNghiViec; }
}
