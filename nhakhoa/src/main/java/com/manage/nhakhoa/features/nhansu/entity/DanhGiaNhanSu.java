package com.manage.nhakhoa.features.nhansu.entity;

import com.manage.nhakhoa.features.nhansu.enums.TrangThaiDanhGia;
import org.hibernate.annotations.Nationalized;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "DanhGiaNhanSu",
       uniqueConstraints = {@UniqueConstraint(name = "UQ_DanhGia_Ky", columnNames = {"maNhanVien", "kyDanhGia"})})
public class DanhGiaNhanSu {

    @Id
    @Column(name = "maDanhGia", nullable = false, length = 10)
    private String maDanhGia;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "maNhanVien", nullable = false)
    private NhanVien nhanVien;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "maNguoiDanhGia", nullable = false)
    private NhanVien nguoiDanhGia;

    @Column(name = "kyDanhGia", nullable = false, length = 10)
    private String kyDanhGia;

    @Column(name = "ngayDanhGia", nullable = false)
    private LocalDate ngayDanhGia;

    @Column(name = "diem", precision = 3, scale = 2)
    private BigDecimal diem;

    @Nationalized
    @Column(name = "nhanXet", length = 1000)
    private String nhanXet;

    @Enumerated(EnumType.STRING)
    @Column(name = "trangThai", nullable = false, length = 10)
    private TrangThaiDanhGia trangThai = TrangThaiDanhGia.NHAP;

    public DanhGiaNhanSu() {}

    public String getMaDanhGia() { return maDanhGia; }
    public void setMaDanhGia(String maDanhGia) { this.maDanhGia = maDanhGia; }

    public NhanVien getNhanVien() { return nhanVien; }
    public void setNhanVien(NhanVien nhanVien) { this.nhanVien = nhanVien; }

    public NhanVien getNguoiDanhGia() { return nguoiDanhGia; }
    public void setNguoiDanhGia(NhanVien nguoiDanhGia) { this.nguoiDanhGia = nguoiDanhGia; }

    public String getKyDanhGia() { return kyDanhGia; }
    public void setKyDanhGia(String kyDanhGia) { this.kyDanhGia = kyDanhGia; }

    public LocalDate getNgayDanhGia() { return ngayDanhGia; }
    public void setNgayDanhGia(LocalDate ngayDanhGia) { this.ngayDanhGia = ngayDanhGia; }

    public BigDecimal getDiem() { return diem; }
    public void setDiem(BigDecimal diem) { this.diem = diem; }

    public String getNhanXet() { return nhanXet; }
    public void setNhanXet(String nhanXet) { this.nhanXet = nhanXet; }

    public TrangThaiDanhGia getTrangThai() { return trangThai; }
    public void setTrangThai(TrangThaiDanhGia trangThai) { this.trangThai = trangThai; }
}
