package com.manage.nhakhoa.features.nhansu.entity;

import com.manage.nhakhoa.features.nhansu.enums.TrangThaiPhanCong;
import org.hibernate.annotations.Nationalized;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "PhanCong")
public class PhanCong {

    @Id
    @Column(name = "maPhanCong", nullable = false, length = 10)
    private String maPhanCong;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "maNhanVien", nullable = false)
    private NhanVien nhanVien;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "maCa", nullable = false)
    private CaLamViec caLamViec;

    @Column(name = "ngayLam", nullable = false)
    private LocalDate ngayLam;

    @Nationalized
    @Column(name = "nhiemVu", nullable = false, length = 200)
    private String nhiemVu;

    @Enumerated(EnumType.STRING)
    @Column(name = "trangThai", nullable = false, length = 20)
    private TrangThaiPhanCong trangThai = TrangThaiPhanCong.CHO_HIEU_LUC;

    @Nationalized
    @Column(name = "lyDoHuy", length = 300)
    private String lyDoHuy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "maPhanCongGoc")
    private PhanCong phanCongGoc;

    public PhanCong() {}

    public String getMaPhanCong() { return maPhanCong; }
    public void setMaPhanCong(String maPhanCong) { this.maPhanCong = maPhanCong; }

    public NhanVien getNhanVien() { return nhanVien; }
    public void setNhanVien(NhanVien nhanVien) { this.nhanVien = nhanVien; }

    public CaLamViec getCaLamViec() { return caLamViec; }
    public void setCaLamViec(CaLamViec caLamViec) { this.caLamViec = caLamViec; }

    public LocalDate getNgayLam() { return ngayLam; }
    public void setNgayLam(LocalDate ngayLam) { this.ngayLam = ngayLam; }

    public String getNhiemVu() { return nhiemVu; }
    public void setNhiemVu(String nhiemVu) { this.nhiemVu = nhiemVu; }

    public TrangThaiPhanCong getTrangThai() { return trangThai; }
    public void setTrangThai(TrangThaiPhanCong trangThai) { this.trangThai = trangThai; }

    public String getLyDoHuy() { return lyDoHuy; }
    public void setLyDoHuy(String lyDoHuy) { this.lyDoHuy = lyDoHuy; }

    public PhanCong getPhanCongGoc() { return phanCongGoc; }
    public void setPhanCongGoc(PhanCong phanCongGoc) { this.phanCongGoc = phanCongGoc; }
}
