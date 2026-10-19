package com.manage.nhakhoa.features.nhansu.entity;

import com.manage.nhakhoa.features.taikhoan.entity.TaiKhoan;
import com.manage.nhakhoa.features.nhansu.enums.TrangThaiBanGiao;
import org.hibernate.annotations.Nationalized;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "BanGiaoCa")
public class BanGiaoCa {

    @Id
    @Column(name = "maBanGiao", nullable = false, length = 10)
    private String maBanGiao;

    @Column(name = "thoiGian", nullable = false)
    private LocalDateTime thoiGian;

    @Nationalized
    @Column(name = "noiDung", nullable = false, length = 1000)
    private String noiDung;

    @Enumerated(EnumType.STRING)
    @Column(name = "trangThai", nullable = false, length = 20)
    private TrangThaiBanGiao trangThai = TrangThaiBanGiao.CHO_BAN_GIAO;

    @Column(name = "ngay", nullable = false)
    private LocalDate ngay;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "maNguoiGiao", nullable = false)
    private NhanVien nguoiGiao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "maNguoiNhan", nullable = false)
    private NhanVien nguoiNhan;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "maCaGiao", nullable = false)
    private CaLamViec caGiao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "maCaNhan", nullable = false)
    private CaLamViec caNhan;

    @Column(name = "coChenhLech", nullable = false)
    private Boolean coChenhLech = false;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "maTaiKhoanTao", nullable = false)
    private TaiKhoan taiKhoanTao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "maTaiKhoanXacNhan")
    private TaiKhoan taiKhoanXacNhan;

    @Column(name = "thoiGianXacNhan")
    private LocalDateTime thoiGianXacNhan;

    @Nationalized
    @Column(name = "ghiChuXacNhan", length = 500)
    private String ghiChuXacNhan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "maTaiKhoanHuy")
    private TaiKhoan taiKhoanHuy;

    @Column(name = "thoiGianHuy")
    private LocalDateTime thoiGianHuy;

    @Nationalized
    @Column(name = "lyDoHuy", length = 300)
    private String lyDoHuy;

    public BanGiaoCa() {}

    public String getMaBanGiao() { return maBanGiao; }
    public void setMaBanGiao(String maBanGiao) { this.maBanGiao = maBanGiao; }

    public LocalDateTime getThoiGian() { return thoiGian; }
    public void setThoiGian(LocalDateTime thoiGian) { this.thoiGian = thoiGian; }

    public String getNoiDung() { return noiDung; }
    public void setNoiDung(String noiDung) { this.noiDung = noiDung; }

    public TrangThaiBanGiao getTrangThai() { return trangThai; }
    public void setTrangThai(TrangThaiBanGiao trangThai) { this.trangThai = trangThai; }

    public LocalDate getNgay() { return ngay; }
    public void setNgay(LocalDate ngay) { this.ngay = ngay; }

    public NhanVien getNguoiGiao() { return nguoiGiao; }
    public void setNguoiGiao(NhanVien nguoiGiao) { this.nguoiGiao = nguoiGiao; }

    public NhanVien getNguoiNhan() { return nguoiNhan; }
    public void setNguoiNhan(NhanVien nguoiNhan) { this.nguoiNhan = nguoiNhan; }

    public CaLamViec getCaGiao() { return caGiao; }
    public void setCaGiao(CaLamViec caGiao) { this.caGiao = caGiao; }

    public CaLamViec getCaNhan() { return caNhan; }
    public void setCaNhan(CaLamViec caNhan) { this.caNhan = caNhan; }

    public Boolean getCoChenhLech() { return coChenhLech; }
    public void setCoChenhLech(Boolean coChenhLech) { this.coChenhLech = coChenhLech; }

    public TaiKhoan getTaiKhoanTao() { return taiKhoanTao; }
    public void setTaiKhoanTao(TaiKhoan taiKhoanTao) { this.taiKhoanTao = taiKhoanTao; }

    public TaiKhoan getTaiKhoanXacNhan() { return taiKhoanXacNhan; }
    public void setTaiKhoanXacNhan(TaiKhoan taiKhoanXacNhan) { this.taiKhoanXacNhan = taiKhoanXacNhan; }

    public LocalDateTime getThoiGianXacNhan() { return thoiGianXacNhan; }
    public void setThoiGianXacNhan(LocalDateTime thoiGianXacNhan) { this.thoiGianXacNhan = thoiGianXacNhan; }

    public String getGhiChuXacNhan() { return ghiChuXacNhan; }
    public void setGhiChuXacNhan(String ghiChuXacNhan) { this.ghiChuXacNhan = ghiChuXacNhan; }

    public TaiKhoan getTaiKhoanHuy() { return taiKhoanHuy; }
    public void setTaiKhoanHuy(TaiKhoan taiKhoanHuy) { this.taiKhoanHuy = taiKhoanHuy; }

    public LocalDateTime getThoiGianHuy() { return thoiGianHuy; }
    public void setThoiGianHuy(LocalDateTime thoiGianHuy) { this.thoiGianHuy = thoiGianHuy; }

    public String getLyDoHuy() { return lyDoHuy; }
    public void setLyDoHuy(String lyDoHuy) { this.lyDoHuy = lyDoHuy; }
}
