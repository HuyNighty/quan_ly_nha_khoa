package com.manage.nhakhoa.features.hethong.entity;

import com.manage.nhakhoa.features.taikhoan.entity.TaiKhoan;
import com.manage.nhakhoa.features.nhansu.entity.NhanVien;
import org.hibernate.annotations.Nationalized;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "NhatKyThaoTac",
       indexes = {@Index(name = "IX_NhatKy_DoiTuong", columnList = "doiTuong"), @Index(name = "IX_NhatKy_ThoiGian", columnList = "thoiGian")})
public class NhatKyThaoTac {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "maNhatKy", nullable = false)
    private Long maNhatKy;

    @Column(name = "thoiGian", nullable = false)
    private LocalDateTime thoiGian;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "maTaiKhoan")
    private TaiKhoan taiKhoan;

    @Nationalized
    @Column(name = "chucNang", nullable = false, length = 50)
    private String chucNang;

    @Nationalized
    @Column(name = "hanhDong", nullable = false, length = 200)
    private String hanhDong;

    @Column(name = "doiTuong", length = 50)
    private String doiTuong;

    @Nationalized
    @Column(name = "noiDung", length = 1000)
    private String noiDung;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "maNguoiChiuTrachNhiem")
    private NhanVien nguoiChiuTrachNhiem;

    public NhatKyThaoTac() {}

    @PrePersist
    protected void prePersist() {
        if (thoiGian == null) thoiGian = LocalDateTime.now();
    }

    public Long getMaNhatKy() { return maNhatKy; }
    public void setMaNhatKy(Long maNhatKy) { this.maNhatKy = maNhatKy; }

    public LocalDateTime getThoiGian() { return thoiGian; }
    public void setThoiGian(LocalDateTime thoiGian) { this.thoiGian = thoiGian; }

    public TaiKhoan getTaiKhoan() { return taiKhoan; }
    public void setTaiKhoan(TaiKhoan taiKhoan) { this.taiKhoan = taiKhoan; }

    public String getChucNang() { return chucNang; }
    public void setChucNang(String chucNang) { this.chucNang = chucNang; }

    public String getHanhDong() { return hanhDong; }
    public void setHanhDong(String hanhDong) { this.hanhDong = hanhDong; }

    public String getDoiTuong() { return doiTuong; }
    public void setDoiTuong(String doiTuong) { this.doiTuong = doiTuong; }

    public String getNoiDung() { return noiDung; }
    public void setNoiDung(String noiDung) { this.noiDung = noiDung; }

    public NhanVien getNguoiChiuTrachNhiem() { return nguoiChiuTrachNhiem; }
    public void setNguoiChiuTrachNhiem(NhanVien nguoiChiuTrachNhiem) { this.nguoiChiuTrachNhiem = nguoiChiuTrachNhiem; }
}
