package com.manage.nhakhoa.features.khamchuabenh.entity;

import com.manage.nhakhoa.features.khamchuabenh.enums.TrangThaiKeHoach;
import com.manage.nhakhoa.features.benhnhan.entity.HoSoBenhAn;
import com.manage.nhakhoa.features.nhansu.entity.NhanVien;
import org.hibernate.annotations.Nationalized;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "KeHoachDieuTri")
public class KeHoachDieuTri {

    @Id
    @Column(name = "maKeHoach", nullable = false, length = 10)
    private String maKeHoach;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "maHoSo", nullable = false)
    private HoSoBenhAn hoSoBenhAn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "maPhieuKham")
    private PhieuKham phieuKham;

    @Column(name = "ngayLap", nullable = false)
    private LocalDate ngayLap;

    @Nationalized
    @Column(name = "mucTieu", nullable = false, length = 300)
    private String mucTieu;

    @Enumerated(EnumType.STRING)
    @Column(name = "trangThai", nullable = false, length = 20)
    private TrangThaiKeHoach trangThai = TrangThaiKeHoach.DANG_THUC_HIEN;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "maNhanVien", nullable = false)
    private NhanVien nhanVien;

    @Nationalized
    @Column(name = "lyDoHuy", length = 300)
    private String lyDoHuy;

    public KeHoachDieuTri() {}

    public String getMaKeHoach() { return maKeHoach; }
    public void setMaKeHoach(String maKeHoach) { this.maKeHoach = maKeHoach; }

    public HoSoBenhAn getHoSoBenhAn() { return hoSoBenhAn; }
    public void setHoSoBenhAn(HoSoBenhAn hoSoBenhAn) { this.hoSoBenhAn = hoSoBenhAn; }

    public PhieuKham getPhieuKham() { return phieuKham; }
    public void setPhieuKham(PhieuKham phieuKham) { this.phieuKham = phieuKham; }

    public LocalDate getNgayLap() { return ngayLap; }
    public void setNgayLap(LocalDate ngayLap) { this.ngayLap = ngayLap; }

    public String getMucTieu() { return mucTieu; }
    public void setMucTieu(String mucTieu) { this.mucTieu = mucTieu; }

    public TrangThaiKeHoach getTrangThai() { return trangThai; }
    public void setTrangThai(TrangThaiKeHoach trangThai) { this.trangThai = trangThai; }

    public NhanVien getNhanVien() { return nhanVien; }
    public void setNhanVien(NhanVien nhanVien) { this.nhanVien = nhanVien; }

    public String getLyDoHuy() { return lyDoHuy; }
    public void setLyDoHuy(String lyDoHuy) { this.lyDoHuy = lyDoHuy; }
}
