package com.manage.nhakhoa.features.kho.entity;

import org.hibernate.annotations.Nationalized;

import jakarta.persistence.*;

@Entity
@Table(name = "Thuoc")
public class Thuoc {

    @Id
    @Column(name = "maThuoc", nullable = false, length = 10)
    private String maThuoc;

    @Nationalized
    @Column(name = "tenThuoc", nullable = false, length = 150)
    private String tenThuoc;

    @Nationalized
    @Column(name = "donViTinh", nullable = false, length = 30)
    private String donViTinh;

    @Column(name = "kiemSoatDacBiet", nullable = false)
    private Boolean kiemSoatDacBiet = false;

    @Nationalized
    @Column(name = "hamLuong", length = 50)
    private String hamLuong;

    @Nationalized
    @Column(name = "hoatChat", length = 150)
    private String hoatChat;

    @Column(name = "dinhMucToiThieu", nullable = false)
    private Integer dinhMucToiThieu = 0;

    @Column(name = "daDatHang", nullable = false)
    private Boolean daDatHang = false;

    public Thuoc() {}

    public String getMaThuoc() { return maThuoc; }
    public void setMaThuoc(String maThuoc) { this.maThuoc = maThuoc; }

    public String getTenThuoc() { return tenThuoc; }
    public void setTenThuoc(String tenThuoc) { this.tenThuoc = tenThuoc; }

    public String getDonViTinh() { return donViTinh; }
    public void setDonViTinh(String donViTinh) { this.donViTinh = donViTinh; }

    public Boolean getKiemSoatDacBiet() { return kiemSoatDacBiet; }
    public void setKiemSoatDacBiet(Boolean kiemSoatDacBiet) { this.kiemSoatDacBiet = kiemSoatDacBiet; }

    public String getHamLuong() { return hamLuong; }
    public void setHamLuong(String hamLuong) { this.hamLuong = hamLuong; }

    public String getHoatChat() { return hoatChat; }
    public void setHoatChat(String hoatChat) { this.hoatChat = hoatChat; }

    public Integer getDinhMucToiThieu() { return dinhMucToiThieu; }
    public void setDinhMucToiThieu(Integer dinhMucToiThieu) { this.dinhMucToiThieu = dinhMucToiThieu; }

    public Boolean getDaDatHang() { return daDatHang; }
    public void setDaDatHang(Boolean daDatHang) { this.daDatHang = daDatHang; }
}
