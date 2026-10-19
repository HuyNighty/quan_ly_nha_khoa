package com.manage.nhakhoa.features.khamchuabenh.entity;

import com.manage.nhakhoa.features.lichhen.entity.LichHen;
import com.manage.nhakhoa.features.khamchuabenh.enums.TrangThaiPhieuKham;
import com.manage.nhakhoa.features.benhnhan.entity.BenhNhan;
import com.manage.nhakhoa.features.benhnhan.entity.HoSoBenhAn;
import com.manage.nhakhoa.features.nhansu.entity.NhanVien;
import org.hibernate.annotations.Nationalized;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "PhieuKham",
       indexes = {@Index(name = "IX_PhieuKham_ThoiGian", columnList = "thoiGianTiepNhan")})
public class PhieuKham {

    @Id
    @Column(name = "maPhieuKham", nullable = false, length = 10)
    private String maPhieuKham;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "maBenhNhan", nullable = false)
    private BenhNhan benhNhan;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "maHoSo", nullable = false)
    private HoSoBenhAn hoSoBenhAn;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "maNhanVien", nullable = false)
    private NhanVien nhanVien;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "maLichHen")
    private LichHen lichHen;

    @Column(name = "thoiGianTiepNhan", nullable = false)
    private LocalDateTime thoiGianTiepNhan;

    @Nationalized
    @Column(name = "trieuChung", length = 500)
    private String trieuChung;

    @Nationalized
    @Column(name = "chanDoan", length = 1000)
    private String chanDoan;

    @Nationalized
    @Column(name = "ketLuan", length = 1000)
    private String ketLuan;

    @Enumerated(EnumType.STRING)
    @Column(name = "trangThai", nullable = false, length = 20)
    private TrangThaiPhieuKham trangThai = TrangThaiPhieuKham.CHO_KHAM;

    @Nationalized
    @Column(name = "lyDoHuy", length = 300)
    private String lyDoHuy;

    public PhieuKham() {}

    public String getMaPhieuKham() { return maPhieuKham; }
    public void setMaPhieuKham(String maPhieuKham) { this.maPhieuKham = maPhieuKham; }

    public BenhNhan getBenhNhan() { return benhNhan; }
    public void setBenhNhan(BenhNhan benhNhan) { this.benhNhan = benhNhan; }

    public HoSoBenhAn getHoSoBenhAn() { return hoSoBenhAn; }
    public void setHoSoBenhAn(HoSoBenhAn hoSoBenhAn) { this.hoSoBenhAn = hoSoBenhAn; }

    public NhanVien getNhanVien() { return nhanVien; }
    public void setNhanVien(NhanVien nhanVien) { this.nhanVien = nhanVien; }

    public LichHen getLichHen() { return lichHen; }
    public void setLichHen(LichHen lichHen) { this.lichHen = lichHen; }

    public LocalDateTime getThoiGianTiepNhan() { return thoiGianTiepNhan; }
    public void setThoiGianTiepNhan(LocalDateTime thoiGianTiepNhan) { this.thoiGianTiepNhan = thoiGianTiepNhan; }

    public String getTrieuChung() { return trieuChung; }
    public void setTrieuChung(String trieuChung) { this.trieuChung = trieuChung; }

    public String getChanDoan() { return chanDoan; }
    public void setChanDoan(String chanDoan) { this.chanDoan = chanDoan; }

    public String getKetLuan() { return ketLuan; }
    public void setKetLuan(String ketLuan) { this.ketLuan = ketLuan; }

    public TrangThaiPhieuKham getTrangThai() { return trangThai; }
    public void setTrangThai(TrangThaiPhieuKham trangThai) { this.trangThai = trangThai; }

    public String getLyDoHuy() { return lyDoHuy; }
    public void setLyDoHuy(String lyDoHuy) { this.lyDoHuy = lyDoHuy; }
}
