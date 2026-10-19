package com.manage.nhakhoa.features.hoadon.entity;

import com.manage.nhakhoa.features.hoadon.enums.PhuongThucThanhToan;
import com.manage.nhakhoa.features.hoadon.enums.TrangThaiThanhToan;
import com.manage.nhakhoa.features.taikhoan.entity.TaiKhoan;
import com.manage.nhakhoa.features.nhansu.entity.NhanVien;
import org.hibernate.annotations.Nationalized;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ThanhToan",
       indexes = {@Index(name = "IX_ThanhToan_HoaDon", columnList = "maHoaDon")})
public class ThanhToan {

    @Id
    @Column(name = "maThanhToan", nullable = false, length = 10)
    private String maThanhToan;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "maHoaDon", nullable = false)
    private HoaDon hoaDon;

    @Column(name = "thoiGian", nullable = false)
    private LocalDateTime thoiGian;

    @Column(name = "soTien", nullable = false, precision = 18, scale = 0)
    private BigDecimal soTien;

    @Enumerated(EnumType.STRING)
    @Column(name = "phuongThuc", nullable = false, length = 15)
    private PhuongThucThanhToan phuongThuc;

    @Column(name = "maGiaoDich", length = 50)
    private String maGiaoDich;

    @Column(name = "tienKhachDua", precision = 18, scale = 0)
    private BigDecimal tienKhachDua;

    @Enumerated(EnumType.STRING)
    @Column(name = "trangThai", nullable = false, length = 15)
    private TrangThaiThanhToan trangThai = TrangThaiThanhToan.THANH_CONG;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "maNhanVien", nullable = false)
    private NhanVien nhanVien;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "maTaiKhoanHuy")
    private TaiKhoan taiKhoanHuy;

    @Column(name = "thoiGianHuy")
    private LocalDateTime thoiGianHuy;

    @Nationalized
    @Column(name = "lyDoHuy", length = 300)
    private String lyDoHuy;

    public ThanhToan() {}

    public String getMaThanhToan() { return maThanhToan; }
    public void setMaThanhToan(String maThanhToan) { this.maThanhToan = maThanhToan; }

    public HoaDon getHoaDon() { return hoaDon; }
    public void setHoaDon(HoaDon hoaDon) { this.hoaDon = hoaDon; }

    public LocalDateTime getThoiGian() { return thoiGian; }
    public void setThoiGian(LocalDateTime thoiGian) { this.thoiGian = thoiGian; }

    public BigDecimal getSoTien() { return soTien; }
    public void setSoTien(BigDecimal soTien) { this.soTien = soTien; }

    public PhuongThucThanhToan getPhuongThuc() { return phuongThuc; }
    public void setPhuongThuc(PhuongThucThanhToan phuongThuc) { this.phuongThuc = phuongThuc; }

    public String getMaGiaoDich() { return maGiaoDich; }
    public void setMaGiaoDich(String maGiaoDich) { this.maGiaoDich = maGiaoDich; }

    public BigDecimal getTienKhachDua() { return tienKhachDua; }
    public void setTienKhachDua(BigDecimal tienKhachDua) { this.tienKhachDua = tienKhachDua; }

    public TrangThaiThanhToan getTrangThai() { return trangThai; }
    public void setTrangThai(TrangThaiThanhToan trangThai) { this.trangThai = trangThai; }

    public NhanVien getNhanVien() { return nhanVien; }
    public void setNhanVien(NhanVien nhanVien) { this.nhanVien = nhanVien; }

    public TaiKhoan getTaiKhoanHuy() { return taiKhoanHuy; }
    public void setTaiKhoanHuy(TaiKhoan taiKhoanHuy) { this.taiKhoanHuy = taiKhoanHuy; }

    public LocalDateTime getThoiGianHuy() { return thoiGianHuy; }
    public void setThoiGianHuy(LocalDateTime thoiGianHuy) { this.thoiGianHuy = thoiGianHuy; }

    public String getLyDoHuy() { return lyDoHuy; }
    public void setLyDoHuy(String lyDoHuy) { this.lyDoHuy = lyDoHuy; }
}
