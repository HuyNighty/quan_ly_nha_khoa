package com.manage.nhakhoa.features.kho.entity;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "LoHang",
       indexes = {@Index(name = "IX_LoHang_Thuoc", columnList = "maThuoc"), @Index(name = "IX_LoHang_VatTu", columnList = "maVatTu")})
public class LoHang {

    @Id
    @Column(name = "maLo", nullable = false, length = 10)
    private String maLo;

    @Column(name = "soLo", nullable = false, length = 30)
    private String soLo;

    @Column(name = "ngaySanXuat")
    private LocalDate ngaySanXuat;

    @Column(name = "hanSuDung")
    private LocalDate hanSuDung;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "maThuoc")
    private Thuoc thuoc;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "maVatTu")
    private VatTu vatTu;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "maNhaCungCap", nullable = false)
    private NhaCungCap nhaCungCap;

    @Column(name = "soLuongTon", nullable = false)
    private Integer soLuongTon = 0;

    public LoHang() {}

    public String getMaLo() { return maLo; }
    public void setMaLo(String maLo) { this.maLo = maLo; }

    public String getSoLo() { return soLo; }
    public void setSoLo(String soLo) { this.soLo = soLo; }

    public LocalDate getNgaySanXuat() { return ngaySanXuat; }
    public void setNgaySanXuat(LocalDate ngaySanXuat) { this.ngaySanXuat = ngaySanXuat; }

    public LocalDate getHanSuDung() { return hanSuDung; }
    public void setHanSuDung(LocalDate hanSuDung) { this.hanSuDung = hanSuDung; }

    public Thuoc getThuoc() { return thuoc; }
    public void setThuoc(Thuoc thuoc) { this.thuoc = thuoc; }

    public VatTu getVatTu() { return vatTu; }
    public void setVatTu(VatTu vatTu) { this.vatTu = vatTu; }

    public NhaCungCap getNhaCungCap() { return nhaCungCap; }
    public void setNhaCungCap(NhaCungCap nhaCungCap) { this.nhaCungCap = nhaCungCap; }

    public Integer getSoLuongTon() { return soLuongTon; }
    public void setSoLuongTon(Integer soLuongTon) { this.soLuongTon = soLuongTon; }
}
