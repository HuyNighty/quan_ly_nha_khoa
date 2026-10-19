package com.manage.nhakhoa.features.kho.entity;

import com.manage.nhakhoa.features.kho.enums.TrangThaiPhieuKho;
import com.manage.nhakhoa.features.nhansu.entity.NhanVien;
import org.hibernate.annotations.Nationalized;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "PhieuXuatKho")
public class PhieuXuatKho {

    @Id
    @Column(name = "maPhieuXuat", nullable = false, length = 10)
    private String maPhieuXuat;

    @Column(name = "ngayXuat", nullable = false)
    private LocalDateTime ngayXuat;

    @Nationalized
    @Column(name = "boPhanNhan", nullable = false, length = 100)
    private String boPhanNhan;

    @Nationalized
    @Column(name = "nguoiNhan", length = 100)
    private String nguoiNhan;

    @Nationalized
    @Column(name = "lyDo", length = 300)
    private String lyDo;

    @Enumerated(EnumType.STRING)
    @Column(name = "trangThai", nullable = false, length = 20)
    private TrangThaiPhieuKho trangThai = TrangThaiPhieuKho.HOAN_TAT;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "maNhanVien", nullable = false)
    private NhanVien nhanVien;

    public PhieuXuatKho() {}

    public String getMaPhieuXuat() { return maPhieuXuat; }
    public void setMaPhieuXuat(String maPhieuXuat) { this.maPhieuXuat = maPhieuXuat; }

    public LocalDateTime getNgayXuat() { return ngayXuat; }
    public void setNgayXuat(LocalDateTime ngayXuat) { this.ngayXuat = ngayXuat; }

    public String getBoPhanNhan() { return boPhanNhan; }
    public void setBoPhanNhan(String boPhanNhan) { this.boPhanNhan = boPhanNhan; }

    public String getNguoiNhan() { return nguoiNhan; }
    public void setNguoiNhan(String nguoiNhan) { this.nguoiNhan = nguoiNhan; }

    public String getLyDo() { return lyDo; }
    public void setLyDo(String lyDo) { this.lyDo = lyDo; }

    public TrangThaiPhieuKho getTrangThai() { return trangThai; }
    public void setTrangThai(TrangThaiPhieuKho trangThai) { this.trangThai = trangThai; }

    public NhanVien getNhanVien() { return nhanVien; }
    public void setNhanVien(NhanVien nhanVien) { this.nhanVien = nhanVien; }
}
