package com.manage.nhakhoa.features.khamchuabenh.entity;

import com.manage.nhakhoa.features.khamchuabenh.enums.TrangThaiChiDinh;
import com.manage.nhakhoa.features.nhansu.entity.NhanVien;
import org.hibernate.annotations.Nationalized;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "ChiDinhCanLamSang")
public class ChiDinhCanLamSang {

    @Id
    @Column(name = "maChiDinh", nullable = false, length = 10)
    private String maChiDinh;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "maPhieuKham", nullable = false)
    private PhieuKham phieuKham;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "maDichVu", nullable = false)
    private DichVu dichVu;

    @Nationalized
    @Column(name = "yeuCau", nullable = false, length = 300)
    private String yeuCau;

    @Nationalized
    @Column(name = "ketQua", length = 1000)
    private String ketQua;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "maNhanVien", nullable = false)
    private NhanVien nhanVien;

    @Column(name = "thoiGianChiDinh", nullable = false)
    private LocalDateTime thoiGianChiDinh;

    @Column(name = "thoiGianKetQua")
    private LocalDateTime thoiGianKetQua;

    @Enumerated(EnumType.STRING)
    @Column(name = "trangThai", nullable = false, length = 20)
    private TrangThaiChiDinh trangThai = TrangThaiChiDinh.CHO_THUC_HIEN;

    public ChiDinhCanLamSang() {}

    public String getMaChiDinh() { return maChiDinh; }
    public void setMaChiDinh(String maChiDinh) { this.maChiDinh = maChiDinh; }

    public PhieuKham getPhieuKham() { return phieuKham; }
    public void setPhieuKham(PhieuKham phieuKham) { this.phieuKham = phieuKham; }

    public DichVu getDichVu() { return dichVu; }
    public void setDichVu(DichVu dichVu) { this.dichVu = dichVu; }

    public String getYeuCau() { return yeuCau; }
    public void setYeuCau(String yeuCau) { this.yeuCau = yeuCau; }

    public String getKetQua() { return ketQua; }
    public void setKetQua(String ketQua) { this.ketQua = ketQua; }

    public NhanVien getNhanVien() { return nhanVien; }
    public void setNhanVien(NhanVien nhanVien) { this.nhanVien = nhanVien; }

    public LocalDateTime getThoiGianChiDinh() { return thoiGianChiDinh; }
    public void setThoiGianChiDinh(LocalDateTime thoiGianChiDinh) { this.thoiGianChiDinh = thoiGianChiDinh; }

    public LocalDateTime getThoiGianKetQua() { return thoiGianKetQua; }
    public void setThoiGianKetQua(LocalDateTime thoiGianKetQua) { this.thoiGianKetQua = thoiGianKetQua; }

    public TrangThaiChiDinh getTrangThai() { return trangThai; }
    public void setTrangThai(TrangThaiChiDinh trangThai) { this.trangThai = trangThai; }
}
