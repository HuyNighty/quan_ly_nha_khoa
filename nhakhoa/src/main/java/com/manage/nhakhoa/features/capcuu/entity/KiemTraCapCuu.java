package com.manage.nhakhoa.features.capcuu.entity;

import com.manage.nhakhoa.features.capcuu.enums.KetQuaKiemTra;
import com.manage.nhakhoa.features.nhansu.entity.NhanVien;
import org.hibernate.annotations.Nationalized;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "KiemTraCapCuu")
public class KiemTraCapCuu {

    @Id
    @Column(name = "maKiemTra", nullable = false, length = 10)
    private String maKiemTra;

    @Column(name = "thoiGian", nullable = false)
    private LocalDateTime thoiGian;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "maNhanVien", nullable = false)
    private NhanVien nhanVien;

    @Enumerated(EnumType.STRING)
    @Column(name = "ketQua", nullable = false, length = 15)
    private KetQuaKiemTra ketQua;

    @Nationalized
    @Column(name = "ghiChu", length = 500)
    private String ghiChu;

    public KiemTraCapCuu() {}

    public String getMaKiemTra() { return maKiemTra; }
    public void setMaKiemTra(String maKiemTra) { this.maKiemTra = maKiemTra; }

    public LocalDateTime getThoiGian() { return thoiGian; }
    public void setThoiGian(LocalDateTime thoiGian) { this.thoiGian = thoiGian; }

    public NhanVien getNhanVien() { return nhanVien; }
    public void setNhanVien(NhanVien nhanVien) { this.nhanVien = nhanVien; }

    public KetQuaKiemTra getKetQua() { return ketQua; }
    public void setKetQua(KetQuaKiemTra ketQua) { this.ketQua = ketQua; }

    public String getGhiChu() { return ghiChu; }
    public void setGhiChu(String ghiChu) { this.ghiChu = ghiChu; }
}
