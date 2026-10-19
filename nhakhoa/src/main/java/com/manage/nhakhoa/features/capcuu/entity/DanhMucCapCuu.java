package com.manage.nhakhoa.features.capcuu.entity;

import com.manage.nhakhoa.features.kho.entity.Thuoc;
import com.manage.nhakhoa.features.kho.entity.VatTu;
import org.hibernate.annotations.Nationalized;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "DanhMucCapCuu")
public class DanhMucCapCuu {

    @Id
    @Column(name = "maMuc", nullable = false, length = 10)
    private String maMuc;

    @Nationalized
    @Column(name = "tenMuc", nullable = false, length = 150)
    private String tenMuc;

    @Nationalized
    @Column(name = "donViTinh", nullable = false, length = 30)
    private String donViTinh;

    @Column(name = "soLuongToiThieu", nullable = false)
    private Integer soLuongToiThieu;

    @Column(name = "soLuongHienCo", nullable = false)
    private Integer soLuongHienCo = 0;

    @Column(name = "hanSuDungGanNhat")
    private LocalDate hanSuDungGanNhat;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "maThuoc")
    private Thuoc thuoc;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "maVatTu")
    private VatTu vatTu;

    @Column(name = "dangApDung", nullable = false)
    private Boolean dangApDung = true;

    @Column(name = "laDuLieuMau", nullable = false)
    private Boolean laDuLieuMau = false;

    @Nationalized
    @Column(name = "ghiChu", length = 300)
    private String ghiChu;

    public DanhMucCapCuu() {}

    public String getMaMuc() { return maMuc; }
    public void setMaMuc(String maMuc) { this.maMuc = maMuc; }

    public String getTenMuc() { return tenMuc; }
    public void setTenMuc(String tenMuc) { this.tenMuc = tenMuc; }

    public String getDonViTinh() { return donViTinh; }
    public void setDonViTinh(String donViTinh) { this.donViTinh = donViTinh; }

    public Integer getSoLuongToiThieu() { return soLuongToiThieu; }
    public void setSoLuongToiThieu(Integer soLuongToiThieu) { this.soLuongToiThieu = soLuongToiThieu; }

    public Integer getSoLuongHienCo() { return soLuongHienCo; }
    public void setSoLuongHienCo(Integer soLuongHienCo) { this.soLuongHienCo = soLuongHienCo; }

    public LocalDate getHanSuDungGanNhat() { return hanSuDungGanNhat; }
    public void setHanSuDungGanNhat(LocalDate hanSuDungGanNhat) { this.hanSuDungGanNhat = hanSuDungGanNhat; }

    public Thuoc getThuoc() { return thuoc; }
    public void setThuoc(Thuoc thuoc) { this.thuoc = thuoc; }

    public VatTu getVatTu() { return vatTu; }
    public void setVatTu(VatTu vatTu) { this.vatTu = vatTu; }

    public Boolean getDangApDung() { return dangApDung; }
    public void setDangApDung(Boolean dangApDung) { this.dangApDung = dangApDung; }

    public Boolean getLaDuLieuMau() { return laDuLieuMau; }
    public void setLaDuLieuMau(Boolean laDuLieuMau) { this.laDuLieuMau = laDuLieuMau; }

    public String getGhiChu() { return ghiChu; }
    public void setGhiChu(String ghiChu) { this.ghiChu = ghiChu; }
}
