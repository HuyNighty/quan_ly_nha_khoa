package com.manage.nhakhoa.features.hethong.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "BoDemMa")
public class BoDemMa {

    @Id
    @Column(name = "loai", nullable = false, length = 10)
    private String loai;

    @Column(name = "tienTo", nullable = false, length = 10)
    private String tienTo;

    @Column(name = "doDai", nullable = false)
    private Integer doDai;

    @Column(name = "giaTri", nullable = false)
    private Integer giaTri = 0;

    public BoDemMa() {}

    public String getLoai() { return loai; }
    public void setLoai(String loai) { this.loai = loai; }

    public String getTienTo() { return tienTo; }
    public void setTienTo(String tienTo) { this.tienTo = tienTo; }

    public Integer getDoDai() { return doDai; }
    public void setDoDai(Integer doDai) { this.doDai = doDai; }

    public Integer getGiaTri() { return giaTri; }
    public void setGiaTri(Integer giaTri) { this.giaTri = giaTri; }
}
