package com.manage.nhakhoa.features.lichhen.entity;

import com.manage.nhakhoa.features.lichhen.enums.TrangThaiLichHen;
import com.manage.nhakhoa.features.khamchuabenh.entity.KeHoachDieuTri;
import com.manage.nhakhoa.features.taikhoan.entity.TaiKhoan;
import com.manage.nhakhoa.features.benhnhan.entity.BenhNhan;
import com.manage.nhakhoa.features.nhansu.entity.NhanVien;
import org.hibernate.annotations.Nationalized;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "LichHen",
       indexes = {@Index(name = "IX_LichHen_BacSi_ThoiGian", columnList = "maNhanVien,thoiGianHen"), @Index(name = "IX_LichHen_ThoiGian", columnList = "thoiGianHen")})
public class LichHen {

    @Id
    @Column(name = "maLichHen", nullable = false, length = 10)
    private String maLichHen;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "maBenhNhan", nullable = false)
    private BenhNhan benhNhan;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "maNhanVien", nullable = false)
    private NhanVien nhanVien;

    @Column(name = "thoiGianHen", nullable = false)
    private LocalDateTime thoiGianHen;

    @Nationalized
    @Column(name = "lyDoHen", length = 300)
    private String lyDoHen;

    @Enumerated(EnumType.STRING)
    @Column(name = "trangThai", nullable = false, length = 20)
    private TrangThaiLichHen trangThai = TrangThaiLichHen.CHO_XAC_NHAN;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "maLichHenGoc")
    private LichHen lichHenGoc;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "maKeHoach")
    private KeHoachDieuTri keHoach;

    @Nationalized
    @Column(name = "lyDoHuy", length = 300)
    private String lyDoHuy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "maTaiKhoanTao")
    private TaiKhoan taiKhoanTao;

    @Column(name = "ngayTao", nullable = false)
    private LocalDateTime ngayTao;

    public LichHen() {}

    @PrePersist
    protected void prePersist() {
        if (ngayTao == null) ngayTao = LocalDateTime.now();
    }

    public String getMaLichHen() { return maLichHen; }
    public void setMaLichHen(String maLichHen) { this.maLichHen = maLichHen; }

    public BenhNhan getBenhNhan() { return benhNhan; }
    public void setBenhNhan(BenhNhan benhNhan) { this.benhNhan = benhNhan; }

    public NhanVien getNhanVien() { return nhanVien; }
    public void setNhanVien(NhanVien nhanVien) { this.nhanVien = nhanVien; }

    public LocalDateTime getThoiGianHen() { return thoiGianHen; }
    public void setThoiGianHen(LocalDateTime thoiGianHen) { this.thoiGianHen = thoiGianHen; }

    public String getLyDoHen() { return lyDoHen; }
    public void setLyDoHen(String lyDoHen) { this.lyDoHen = lyDoHen; }

    public TrangThaiLichHen getTrangThai() { return trangThai; }
    public void setTrangThai(TrangThaiLichHen trangThai) { this.trangThai = trangThai; }

    public LichHen getLichHenGoc() { return lichHenGoc; }
    public void setLichHenGoc(LichHen lichHenGoc) { this.lichHenGoc = lichHenGoc; }

    public KeHoachDieuTri getKeHoach() { return keHoach; }
    public void setKeHoach(KeHoachDieuTri keHoach) { this.keHoach = keHoach; }

    public String getLyDoHuy() { return lyDoHuy; }
    public void setLyDoHuy(String lyDoHuy) { this.lyDoHuy = lyDoHuy; }

    public TaiKhoan getTaiKhoanTao() { return taiKhoanTao; }
    public void setTaiKhoanTao(TaiKhoan taiKhoanTao) { this.taiKhoanTao = taiKhoanTao; }

    public LocalDateTime getNgayTao() { return ngayTao; }
    public void setNgayTao(LocalDateTime ngayTao) { this.ngayTao = ngayTao; }
}
