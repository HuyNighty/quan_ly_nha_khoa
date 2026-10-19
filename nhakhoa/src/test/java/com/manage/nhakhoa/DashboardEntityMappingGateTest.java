package com.manage.nhakhoa;

import com.manage.nhakhoa.features.capcuu.entity.ChiTietKiemTraCapCuu;
import com.manage.nhakhoa.features.capcuu.entity.ChiTietKiemTraCapCuuId;
import com.manage.nhakhoa.features.capcuu.entity.DanhMucCapCuu;
import com.manage.nhakhoa.features.capcuu.entity.KiemTraCapCuu;
import com.manage.nhakhoa.features.capcuu.enums.KetQuaKiemTra;
import com.manage.nhakhoa.features.nhansu.entity.NhanVien;
import com.manage.nhakhoa.features.nhansu.enums.TrangThaiNhanVien;
import com.manage.nhakhoa.shared.enums.VaiTro;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Gate độc lập, hiện FAIL ở mapping Entity baseline; R1 không được phép sửa Entity để làm xanh.
 * Chạy riêng: mvnw.cmd -Dtest=DashboardEntityMappingGateTest test.
 * Không được dùng kết quả truy vấn đọc hoặc contextLoads để thay thế gate này.
 */
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Tag("entity-mapping-gate")
class DashboardEntityMappingGateTest {
    @Autowired EntityManagerFactory factory;
    @DynamicPropertySource
    static void database(DynamicPropertyRegistry properties) throws Exception {
        DashboardApplicationSqlServerTest.disposableDatabase(properties);
    }
    @AfterAll static void cleanup() { DashboardApplicationSqlServerTest.stopDisposableServer(); }

    @Test
    void currentCompositeIdMappingMustSupportRealEntityManagerPersistence() {
        assertDoesNotThrow(() -> {
            try(var em=factory.createEntityManager()) {
                var transaction=em.getTransaction(); transaction.begin();
                try {
                    var employee=new NhanVien(); employee.setMaNhanVien("NV_GATE"); employee.setHoTen("Nhân viên kiểm thử");
                    employee.setSoDienThoai("0000000099"); employee.setChucDanh(VaiTro.YT);
                    employee.setTrangThai(TrangThaiNhanVien.DANG_LAM_VIEC); em.persist(employee);
                    var catalog=new DanhMucCapCuu(); catalog.setMaMuc("REAL_GATE"); catalog.setTenMuc("Mục kiểm thử");
                    catalog.setDonViTinh("Đơn vị"); catalog.setSoLuongToiThieu(1); catalog.setSoLuongHienCo(1);
                    catalog.setDangApDung(true); catalog.setLaDuLieuMau(false); em.persist(catalog);
                    var check=new KiemTraCapCuu(); check.setMaKiemTra("KT_GATE");
                    check.setThoiGian(LocalDate.of(2026,10,19).atTime(7,45));
                    check.setNhanVien(employee); check.setKetQua(KetQuaKiemTra.DAT); em.persist(check);
                    var detail=new ChiTietKiemTraCapCuu(); detail.setId(new ChiTietKiemTraCapCuuId("KT_GATE","REAL_GATE"));
                    detail.setKiemTra(check); detail.setDanhMucCapCuu(catalog); detail.setSoLuongThucTe(1);
                    detail.setDat(true); detail.setHanSuDung(LocalDate.of(2027,1,1)); em.persist(detail);
                    transaction.commit();
                } catch(RuntimeException failure) {
                    if(transaction.isActive()) transaction.rollback();
                    throw failure;
                }
            }
        },"Gate mapping chỉ PASS khi EntityManager ghi được chi tiết với MapsId hiện hành");
    }
}
