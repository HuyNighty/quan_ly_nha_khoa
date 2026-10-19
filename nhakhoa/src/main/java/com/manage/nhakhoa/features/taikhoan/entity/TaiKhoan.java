package com.manage.nhakhoa.features.taikhoan.entity;

import com.manage.nhakhoa.features.taikhoan.enums.TrangThaiTaiKhoan;
import com.manage.nhakhoa.features.nhansu.entity.NhanVien;
import com.manage.nhakhoa.shared.enums.VaiTro;
import org.hibernate.annotations.Nationalized;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "TaiKhoan")
public class TaiKhoan {

    @Id
    @Column(name = "maTaiKhoan", nullable = false, length = 10)
    private String maTaiKhoan;

    @Column(name = "tenDangNhap", nullable = false, length = 50, unique = true)
    private String tenDangNhap;

    @Column(name = "matKhau", nullable = false, length = 200)
    private String matKhau;

    @Enumerated(EnumType.STRING)
    @Column(name = "trangThai", nullable = false, length = 20)
    private TrangThaiTaiKhoan trangThai = TrangThaiTaiKhoan.HOAT_DONG;

    @Enumerated(EnumType.STRING)
    @Column(name = "vaiTro", nullable = false, length = 10)
    private VaiTro vaiTro;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "maNhanVien", nullable = false, unique = true)
    private NhanVien nhanVien;

    @Column(name = "soLanSai", nullable = false)
    private Integer soLanSai = 0;

    @Column(name = "lanDangNhapCuoi")
    private LocalDateTime lanDangNhapCuoi;

    @Nationalized
    @Column(name = "lyDoKhoa", length = 300)
    private String lyDoKhoa;

    public TaiKhoan() {}

    public String getMaTaiKhoan() { return maTaiKhoan; }
    public void setMaTaiKhoan(String maTaiKhoan) { this.maTaiKhoan = maTaiKhoan; }

    public String getTenDangNhap() { return tenDangNhap; }
    public void setTenDangNhap(String tenDangNhap) { this.tenDangNhap = tenDangNhap; }

    public String getMatKhau() { return matKhau; }
    public void setMatKhau(String matKhau) { this.matKhau = matKhau; }

    public TrangThaiTaiKhoan getTrangThai() { return trangThai; }
    public void setTrangThai(TrangThaiTaiKhoan trangThai) { this.trangThai = trangThai; }

    public VaiTro getVaiTro() { return vaiTro; }
    public void setVaiTro(VaiTro vaiTro) { this.vaiTro = vaiTro; }

    public NhanVien getNhanVien() { return nhanVien; }
    public void setNhanVien(NhanVien nhanVien) { this.nhanVien = nhanVien; }

    public Integer getSoLanSai() { return soLanSai; }
    public void setSoLanSai(Integer soLanSai) { this.soLanSai = soLanSai; }

    public LocalDateTime getLanDangNhapCuoi() { return lanDangNhapCuoi; }
    public void setLanDangNhapCuoi(LocalDateTime lanDangNhapCuoi) { this.lanDangNhapCuoi = lanDangNhapCuoi; }

    public String getLyDoKhoa() { return lyDoKhoa; }
    public void setLyDoKhoa(String lyDoKhoa) { this.lyDoKhoa = lyDoKhoa; }
}
