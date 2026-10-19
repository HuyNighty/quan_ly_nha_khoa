package com.manage.nhakhoa;

import com.manage.nhakhoa.features.dashboard.service.DashboardQueryService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.testcontainers.mssqlserver.MSSQLServerContainer;

import java.sql.DriverManager;
import java.time.Duration;
import java.util.UUID;
import javax.sql.DataSource;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Chạy kế thừa chính contextLoads của baseline với Spring Boot thật và DB container riêng.
 * Dùng đầy đủ naming strategy và khóa ngoại của Spring Boot thật; không bỏ qua lỗi DDL.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class DashboardApplicationSqlServerTest extends NhakhoaApplicationTests {
    private static final String DATABASE="N08_Dashboard_App_R1_Test";
    private static final MSSQLServerContainer SQL=new MSSQLServerContainer("mcr.microsoft.com/mssql/server:2022-latest")
            .acceptLicense().withPassword("R1!"+UUID.randomUUID()+"aA1");
    @Autowired ConfigurableApplicationContext context;

    @DynamicPropertySource
    static void disposableDatabase(DynamicPropertyRegistry properties) throws Exception {
        SQL.withLabel("n08.scope","dashboard-r1-boot-context-test");
        SQL.withStartupTimeout(Duration.ofMinutes(3)); SQL.start();
        try(var connection=DriverManager.getConnection(SQL.getJdbcUrl(),SQL.getUsername(),SQL.getPassword());
            var statement=connection.createStatement()) { statement.execute("CREATE DATABASE "+DATABASE); }
        properties.add("spring.datasource.url",() -> SQL.getJdbcUrl()+";databaseName="+DATABASE);
        properties.add("spring.datasource.username",SQL::getUsername);
        properties.add("spring.datasource.password",SQL::getPassword);
        properties.add("spring.jpa.hibernate.ddl-auto",() -> "create-only");
        properties.add("spring.jpa.properties.hibernate.hbm2ddl.halt_on_error",() -> "true");
        properties.add("spring.jpa.properties.hibernate.hbm2ddl.default_constraint_mode",
                () -> "CONSTRAINT");
        properties.add("spring.jpa.show-sql",() -> "false");
        properties.add("spring.jpa.open-in-view",() -> "false");
    }

    @Test
    void actualBootWiringHasExactlyOneReadServiceAndNoDashboardRoute() {
        assertEquals(1,context.getBeansOfType(DashboardQueryService.class).size());
        var mappings=context.getBean("requestMappingHandlerMapping",RequestMappingHandlerMapping.class);
        assertTrue(mappings.getHandlerMethods().keySet().stream().flatMap(info -> info.getPatternValues().stream())
                .noneMatch(path -> path.endsWith("/api/v1/admin/dashboard")));
    }

    @Test
    void actualBootSchemaHasEnabledForeignKeysIncludingCompositeIds() {
        var jdbc=new JdbcTemplate(context.getBean(DataSource.class));
        assertEquals(DATABASE,jdbc.queryForObject("SELECT DB_NAME()",String.class));
        int enabled=jdbc.queryForObject("SELECT COUNT(*) FROM sys.foreign_keys WHERE is_disabled=0 AND is_not_trusted=0",Integer.class);
        assertTrue(enabled>0,"Schema test phải thực sự có FK đang được kiểm tra");
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM sys.foreign_keys WHERE parent_object_id=OBJECT_ID('chi_tiet_danh_gia')"
                +" AND referenced_object_id=OBJECT_ID('danh_gia_nhan_su') AND is_disabled=0",Integer.class));
        assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM sys.foreign_keys WHERE parent_object_id=OBJECT_ID('chi_tiet_kiem_tra_cap_cuu') AND is_disabled=0",Integer.class));
        System.out.println("BOOT_SCHEMA_ENABLED_FOREIGN_KEYS="+enabled);
        System.out.println("MAPS_ID_SCHEMA_COLUMNS="+jdbc.queryForList("SELECT name,max_length,is_nullable FROM sys.columns"
                +" WHERE object_id=OBJECT_ID('chi_tiet_kiem_tra_cap_cuu') AND name IN ('maMuc','ma_muc','maKiemTra','ma_kiem_tra') ORDER BY name"));
    }

    @AfterAll
    static void stopDisposableServer() { SQL.stop(); }
}
