package com.manage.nhakhoa.features.dashboard.integration;

import com.manage.nhakhoa.features.dashboard.repository.*;
import com.manage.nhakhoa.features.dashboard.service.DashboardQueryService;
import com.manage.nhakhoa.features.lichhen.enums.TrangThaiLichHen;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceContext;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.hibernate.SpringImplicitNamingStrategy;
import org.hibernate.boot.model.naming.PhysicalNamingStrategySnakeCaseImpl;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.testcontainers.mssqlserver.MSSQLServerContainer;

import javax.sql.DataSource;
import java.sql.DriverManager;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Chỉ test: SQL Server dùng một lần, không nạp application.yaml hoặc thông tin DB của người dùng. */
@TestConfiguration(proxyBeanMethods = false)
@EnableTransactionManagement(proxyTargetClass = true)
public class DashboardSqlServerTestConfig {
    public static final String TEST_DATABASE = "N08_Dashboard_R1_Test";

    @Bean(destroyMethod = "stop")
    MSSQLServerContainer sqlServer() {
        MSSQLServerContainer container = new MSSQLServerContainer("mcr.microsoft.com/mssql/server:2022-latest")
                .acceptLicense().withPassword("R1!" + UUID.randomUUID() + "aA1");
        container.withLabel("n08.scope", "dashboard-r1-disposable-test");
        container.withStartupTimeout(Duration.ofMinutes(3));
        container.start();
        return container;
    }

    @Bean
    DataSource dataSource(MSSQLServerContainer container) throws Exception {
        try (var connection = DriverManager.getConnection(container.getJdbcUrl(), container.getUsername(), container.getPassword());
             var statement = connection.createStatement()) {
            statement.execute("CREATE DATABASE " + TEST_DATABASE);
        }
        return new DriverManagerDataSource(container.getJdbcUrl() + ";databaseName=" + TEST_DATABASE,
                container.getUsername(), container.getPassword());
    }

    @Bean
    LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
        var factory = new LocalContainerEntityManagerFactoryBean();
        factory.setDataSource(dataSource);
        factory.setPackagesToScan("com.manage.nhakhoa.features");
        factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        factory.setJpaPropertyMap(Map.of(
                "hibernate.hbm2ddl.auto", "create-only",
                "hibernate.hbm2ddl.halt_on_error", true,
                // Dùng chính naming strategy mặc định của Spring Boot 4.1.1 như application context thật.
                "hibernate.implicit_naming_strategy", SpringImplicitNamingStrategy.class.getName(),
                "hibernate.physical_naming_strategy", PhysicalNamingStrategySnakeCaseImpl.class.getName(),
                "hibernate.show_sql", false));
        return factory;
    }

    @Bean
    PlatformTransactionManager transactionManager(EntityManagerFactory factory) {
        return new JpaTransactionManager(factory);
    }

    @Bean
    FailureSwitch failureSwitch() { return new FailureSwitch(); }

    @Bean
    DashboardReadRepository dashboardReadRepository(FailureSwitch failures) {
        return new FaultInjectingRepository(failures);
    }

    @Bean
    DashboardQueryService dashboardQueryService(DashboardReadRepository repository) {
        return new DashboardQueryService(repository,
                Clock.fixed(Instant.parse("2026-10-19T08:38:00Z"), DashboardQueryService.BUSINESS_ZONE));
    }

    public static final class FailureSwitch {
        String section;
        boolean insideTransaction;
        int sqlAttempts;
        void reset() { section = null; insideTransaction = false; sqlAttempts = 0; }
    }

    /** Không giả lập kết quả repository: nhánh bình thường gọi JPQL gốc, nhánh lỗi gửi SQL sai tới SQL Server thật. */
    public static class FaultInjectingRepository extends JpaDashboardReadRepository {
        private final FailureSwitch failures;
        @PersistenceContext private EntityManager entityManager;
        FaultInjectingRepository(FailureSwitch failures) { this.failures = failures; }
        private void failIfRequested(String section) {
            if (section.equals(failures.section)) {
                failures.insideTransaction = TransactionSynchronizationManager.isActualTransactionActive();
                failures.sqlAttempts++;
                entityManager.createNativeQuery("SELECT n08_intentionally_missing_column FROM sys.objects", Long.class).getResultList();
                throw new AssertionError("SQL Server phải từ chối cột kiểm thử không tồn tại");
            }
        }
        @Override public List<ShiftRow> findShifts() { failIfRequested("staffing"); return super.findShifts(); }
        @Override public List<StockLotRow> findNonEmptyStockLots() { failIfRequested("inventory"); return super.findNonEmptyStockLots(); }
        @Override public long countActiveRealEmergencyCatalogItems() { failIfRequested("emergencyKit"); return super.countActiveRealEmergencyCatalogItems(); }
        @Override public long countPlansWithPendingItemsDueOnOrBefore(LocalDate date) { failIfRequested("treatmentDue"); return super.countPlansWithPendingItemsDueOnOrBefore(date); }
        @Override public Map<TrangThaiLichHen, Long> countAppointmentsByStatus(LocalDateTime start, LocalDateTime end) {
            failIfRequested("appointments"); return super.countAppointmentsByStatus(start, end);
        }
        @Override public Optional<String> findSystemParameter(String key) { failIfRequested("parameters"); return super.findSystemParameter(key); }
        @Override public long countPendingStockAdjustments() { failIfRequested("stockApprovals"); return super.countPendingStockAdjustments(); }
        @Override public long countPendingInvoiceCancellationRequests() { failIfRequested("invoiceApprovals"); return super.countPendingInvoiceCancellationRequests(); }
    }
}
