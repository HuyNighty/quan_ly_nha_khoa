package com.manage.nhakhoa.features.kho.entity;

import com.manage.nhakhoa.features.kho.enums.TrangThaiNhaCungCap;
import org.hibernate.annotations.Nationalized;

import jakarta.persistence.*;

@Entity
@Table(name = "NhaCungCap")
public class NhaCungCap {

    @Id
    @Column(name = "maNhaCungCap", nullable = false, length = 10)
    private String maNhaCungCap;

    @Nationalized
    @Column(name = "tenNhaCungCap", nullable = false, length = 150)
    private String tenNhaCungCap;

    @Nationalized
    @Column(name = "diaChi", length = 255)
    private String diaChi;

    @Column(name = "soDienThoai", nullable = false, length = 20)
    private String soDienThoai;

    @Column(name = "email", length = 100)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "trangThai", nullable = false, length = 20)
    private TrangThaiNhaCungCap trangThai = TrangThaiNhaCungCap.DANG_HOP_TAC;

    public NhaCungCap() {}

    public String getMaNhaCungCap() { return maNhaCungCap; }
    public void setMaNhaCungCap(String maNhaCungCap) { this.maNhaCungCap = maNhaCungCap; }

    public String getTenNhaCungCap() { return tenNhaCungCap; }
    public void setTenNhaCungCap(String tenNhaCungCap) { this.tenNhaCungCap = tenNhaCungCap; }

    public String getDiaChi() { return diaChi; }
    public void setDiaChi(String diaChi) { this.diaChi = diaChi; }

    public String getSoDienThoai() { return soDienThoai; }
    public void setSoDienThoai(String soDienThoai) { this.soDienThoai = soDienThoai; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public TrangThaiNhaCungCap getTrangThai() { return trangThai; }
    public void setTrangThai(TrangThaiNhaCungCap trangThai) { this.trangThai = trangThai; }
}
