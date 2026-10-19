package com.manage.nhakhoa.features.donthuoc.entity;

import com.manage.nhakhoa.features.khamchuabenh.entity.PhieuKham;
import com.manage.nhakhoa.features.benhnhan.entity.BenhNhan;
import com.manage.nhakhoa.features.nhansu.entity.NhanVien;
import org.hibernate.annotations.Nationalized;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "DonThuoc")
public class DonThuoc {

    @Id
    @Column(name = "maDonThuoc", nullable = false, length = 10)
    private String maDonThuoc;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "maPhieuKham", nullable = false, unique = true)
    private PhieuKham phieuKham;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "maBenhNhan", nullable = false)
    private BenhNhan benhNhan;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "maNhanVien", nullable = false)
    private NhanVien nhanVien;

    @Column(name = "ngayKe", nullable = false)
    private LocalDate ngayKe;

    @Nationalized
    @Column(name = "loiDan", length = 500)
    private String loiDan;

    public DonThuoc() {}

    public String getMaDonThuoc() { return maDonThuoc; }
    public void setMaDonThuoc(String maDonThuoc) { this.maDonThuoc = maDonThuoc; }

    public PhieuKham getPhieuKham() { return phieuKham; }
    public void setPhieuKham(PhieuKham phieuKham) { this.phieuKham = phieuKham; }

    public BenhNhan getBenhNhan() { return benhNhan; }
    public void setBenhNhan(BenhNhan benhNhan) { this.benhNhan = benhNhan; }

    public NhanVien getNhanVien() { return nhanVien; }
    public void setNhanVien(NhanVien nhanVien) { this.nhanVien = nhanVien; }

    public LocalDate getNgayKe() { return ngayKe; }
    public void setNgayKe(LocalDate ngayKe) { this.ngayKe = ngayKe; }

    public String getLoiDan() { return loiDan; }
    public void setLoiDan(String loiDan) { this.loiDan = loiDan; }
}
