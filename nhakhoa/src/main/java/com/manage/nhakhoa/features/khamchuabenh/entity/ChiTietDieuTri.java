package com.manage.nhakhoa.features.khamchuabenh.entity;

import com.manage.nhakhoa.features.khamchuabenh.enums.TrangThaiChiTietDieuTri;
import com.manage.nhakhoa.features.nhansu.entity.NhanVien;
import org.hibernate.annotations.Nationalized;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "ChiTietDieuTri")
public class ChiTietDieuTri {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "maChiTiet", nullable = false)
    private Integer maChiTiet;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "maKeHoach", nullable = false)
    private KeHoachDieuTri keHoach;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "maDichVu", nullable = false)
    private DichVu dichVu;

    @Nationalized
    @Column(name = "viTriRang", nullable = false, length = 50)
    private String viTriRang;

    @Column(name = "ngayDuKien", nullable = false)
    private LocalDate ngayDuKien;

    @Column(name = "ngayThucHien")
    private LocalDate ngayThucHien;

    @Nationalized
    @Column(name = "ketQua", length = 500)
    private String ketQua;

    @Enumerated(EnumType.STRING)
    @Column(name = "trangThai", nullable = false, length = 20)
    private TrangThaiChiTietDieuTri trangThai = TrangThaiChiTietDieuTri.CHUA_THUC_HIEN;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "maNhanVienThucHien")
    private NhanVien nhanVienThucHien;

    @Nationalized
    @Column(name = "ghiChu", length = 300)
    private String ghiChu;

    public ChiTietDieuTri() {}

    public Integer getMaChiTiet() { return maChiTiet; }
    public void setMaChiTiet(Integer maChiTiet) { this.maChiTiet = maChiTiet; }

    public KeHoachDieuTri getKeHoach() { return keHoach; }
    public void setKeHoach(KeHoachDieuTri keHoach) { this.keHoach = keHoach; }

    public DichVu getDichVu() { return dichVu; }
    public void setDichVu(DichVu dichVu) { this.dichVu = dichVu; }

    public String getViTriRang() { return viTriRang; }
    public void setViTriRang(String viTriRang) { this.viTriRang = viTriRang; }

    public LocalDate getNgayDuKien() { return ngayDuKien; }
    public void setNgayDuKien(LocalDate ngayDuKien) { this.ngayDuKien = ngayDuKien; }

    public LocalDate getNgayThucHien() { return ngayThucHien; }
    public void setNgayThucHien(LocalDate ngayThucHien) { this.ngayThucHien = ngayThucHien; }

    public String getKetQua() { return ketQua; }
    public void setKetQua(String ketQua) { this.ketQua = ketQua; }

    public TrangThaiChiTietDieuTri getTrangThai() { return trangThai; }
    public void setTrangThai(TrangThaiChiTietDieuTri trangThai) { this.trangThai = trangThai; }

    public NhanVien getNhanVienThucHien() { return nhanVienThucHien; }
    public void setNhanVienThucHien(NhanVien nhanVienThucHien) { this.nhanVienThucHien = nhanVienThucHien; }

    public String getGhiChu() { return ghiChu; }
    public void setGhiChu(String ghiChu) { this.ghiChu = ghiChu; }
}
