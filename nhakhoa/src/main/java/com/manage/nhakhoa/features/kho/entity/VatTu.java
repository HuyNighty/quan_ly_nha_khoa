package com.manage.nhakhoa.features.kho.entity;

import org.hibernate.annotations.Nationalized;

import jakarta.persistence.*;

@Entity
@Table(name = "VatTu")
public class VatTu {

    @Id
    @Column(name = "maVatTu", nullable = false, length = 10)
    private String maVatTu;

    @Nationalized
    @Column(name = "tenVatTu", nullable = false, length = 150)
    private String tenVatTu;

    @Nationalized
    @Column(name = "donViTinh", nullable = false, length = 30)
    private String donViTinh;

    @Nationalized
    @Column(name = "moTa", length = 300)
    private String moTa;

    @Column(name = "dinhMucToiThieu", nullable = false)
    private Integer dinhMucToiThieu = 0;

    @Column(name = "daDatHang", nullable = false)
    private Boolean daDatHang = false;

    public VatTu() {}

    public String getMaVatTu() { return maVatTu; }
    public void setMaVatTu(String maVatTu) { this.maVatTu = maVatTu; }

    public String getTenVatTu() { return tenVatTu; }
    public void setTenVatTu(String tenVatTu) { this.tenVatTu = tenVatTu; }

    public String getDonViTinh() { return donViTinh; }
    public void setDonViTinh(String donViTinh) { this.donViTinh = donViTinh; }

    public String getMoTa() { return moTa; }
    public void setMoTa(String moTa) { this.moTa = moTa; }

    public Integer getDinhMucToiThieu() { return dinhMucToiThieu; }
    public void setDinhMucToiThieu(Integer dinhMucToiThieu) { this.dinhMucToiThieu = dinhMucToiThieu; }

    public Boolean getDaDatHang() { return daDatHang; }
    public void setDaDatHang(Boolean daDatHang) { this.daDatHang = daDatHang; }
}
