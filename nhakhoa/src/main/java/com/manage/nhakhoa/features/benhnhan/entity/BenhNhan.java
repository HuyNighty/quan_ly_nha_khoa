package com.manage.nhakhoa.features.benhnhan.entity;

import com.manage.nhakhoa.features.taikhoan.entity.TaiKhoan;
import com.manage.nhakhoa.shared.enums.GioiTinh;
import org.hibernate.annotations.Nationalized;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "BenhNhan",
       indexes = {@Index(name = "IX_BenhNhan_SDT", columnList = "soDienThoai")})
public class BenhNhan {

    @Id
    @Column(name = "maBenhNhan", nullable = false, length = 10)
    private String maBenhNhan;

    @Nationalized
    @Column(name = "hoTen", nullable = false, length = 100)
    private String hoTen;

    @Column(name = "ngaySinh", nullable = false)
    private LocalDate ngaySinh;

    @Enumerated(EnumType.STRING)
    @Column(name = "gioiTinh", nullable = false, length = 5)
    private GioiTinh gioiTinh;

    @Column(name = "soDienThoai", nullable = false, length = 15)
    private String soDienThoai;

    @Nationalized
    @Column(name = "diaChi", length = 255)
    private String diaChi;

    @Nationalized
    @Column(name = "nguoiLienHe", length = 200)
    private String nguoiLienHe;

    @Column(name = "ngayTao", nullable = false)
    private LocalDateTime ngayTao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "maTaiKhoanTao")
    private TaiKhoan taiKhoanTao;

    public BenhNhan() {}

    @PrePersist
    protected void prePersist() {
        if (ngayTao == null) ngayTao = LocalDateTime.now();
    }

    public String getMaBenhNhan() { return maBenhNhan; }
    public void setMaBenhNhan(String maBenhNhan) { this.maBenhNhan = maBenhNhan; }

    public String getHoTen() { return hoTen; }
    public void setHoTen(String hoTen) { this.hoTen = hoTen; }

    public LocalDate getNgaySinh() { return ngaySinh; }
    public void setNgaySinh(LocalDate ngaySinh) { this.ngaySinh = ngaySinh; }

    public GioiTinh getGioiTinh() { return gioiTinh; }
    public void setGioiTinh(GioiTinh gioiTinh) { this.gioiTinh = gioiTinh; }

    public String getSoDienThoai() { return soDienThoai; }
    public void setSoDienThoai(String soDienThoai) { this.soDienThoai = soDienThoai; }

    public String getDiaChi() { return diaChi; }
    public void setDiaChi(String diaChi) { this.diaChi = diaChi; }

    public String getNguoiLienHe() { return nguoiLienHe; }
    public void setNguoiLienHe(String nguoiLienHe) { this.nguoiLienHe = nguoiLienHe; }

    public LocalDateTime getNgayTao() { return ngayTao; }
    public void setNgayTao(LocalDateTime ngayTao) { this.ngayTao = ngayTao; }

    public TaiKhoan getTaiKhoanTao() { return taiKhoanTao; }
    public void setTaiKhoanTao(TaiKhoan taiKhoanTao) { this.taiKhoanTao = taiKhoanTao; }
}
