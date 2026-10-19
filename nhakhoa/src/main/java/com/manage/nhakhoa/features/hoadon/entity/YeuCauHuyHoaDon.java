package com.manage.nhakhoa.features.hoadon.entity;

import com.manage.nhakhoa.features.taikhoan.entity.TaiKhoan;
import com.manage.nhakhoa.shared.enums.TrangThaiDuyet;
import org.hibernate.annotations.Nationalized;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "YeuCauHuyHoaDon")
public class YeuCauHuyHoaDon {

    @Id
    @Column(name = "maYeuCau", nullable = false, length = 10)
    private String maYeuCau;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "maHoaDon", nullable = false)
    private HoaDon hoaDon;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "maTaiKhoanTao", nullable = false)
    private TaiKhoan taiKhoanTao;

    @Column(name = "thoiGianTao", nullable = false)
    private LocalDateTime thoiGianTao;

    @Nationalized
    @Column(name = "lyDo", nullable = false, length = 500)
    private String lyDo;

    @Enumerated(EnumType.STRING)
    @Column(name = "trangThai", nullable = false, length = 10)
    private TrangThaiDuyet trangThai = TrangThaiDuyet.CHO_DUYET;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "maTaiKhoanDuyet")
    private TaiKhoan taiKhoanDuyet;

    @Column(name = "thoiGianDuyet")
    private LocalDateTime thoiGianDuyet;

    @Nationalized
    @Column(name = "lyDoDuyet", length = 500)
    private String lyDoDuyet;

    public YeuCauHuyHoaDon() {}

    public String getMaYeuCau() { return maYeuCau; }
    public void setMaYeuCau(String maYeuCau) { this.maYeuCau = maYeuCau; }

    public HoaDon getHoaDon() { return hoaDon; }
    public void setHoaDon(HoaDon hoaDon) { this.hoaDon = hoaDon; }

    public TaiKhoan getTaiKhoanTao() { return taiKhoanTao; }
    public void setTaiKhoanTao(TaiKhoan taiKhoanTao) { this.taiKhoanTao = taiKhoanTao; }

    public LocalDateTime getThoiGianTao() { return thoiGianTao; }
    public void setThoiGianTao(LocalDateTime thoiGianTao) { this.thoiGianTao = thoiGianTao; }

    public String getLyDo() { return lyDo; }
    public void setLyDo(String lyDo) { this.lyDo = lyDo; }

    public TrangThaiDuyet getTrangThai() { return trangThai; }
    public void setTrangThai(TrangThaiDuyet trangThai) { this.trangThai = trangThai; }

    public TaiKhoan getTaiKhoanDuyet() { return taiKhoanDuyet; }
    public void setTaiKhoanDuyet(TaiKhoan taiKhoanDuyet) { this.taiKhoanDuyet = taiKhoanDuyet; }

    public LocalDateTime getThoiGianDuyet() { return thoiGianDuyet; }
    public void setThoiGianDuyet(LocalDateTime thoiGianDuyet) { this.thoiGianDuyet = thoiGianDuyet; }

    public String getLyDoDuyet() { return lyDoDuyet; }
    public void setLyDoDuyet(String lyDoDuyet) { this.lyDoDuyet = lyDoDuyet; }
}
