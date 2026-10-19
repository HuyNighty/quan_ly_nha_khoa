package com.manage.nhakhoa.features.dashboard.service;

import com.manage.nhakhoa.features.dashboard.dto.response.AdminDashboardResponse;
import com.manage.nhakhoa.features.dashboard.dto.response.WorkQueueItem;
import com.manage.nhakhoa.features.khamchuabenh.enums.TrangThaiChiTietDieuTri;
import com.manage.nhakhoa.features.khamchuabenh.enums.TrangThaiKeHoach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/** Jackson 3 được cấu hình bởi Spring Boot thật; không dùng API serialization stub hoặc mở HTTP. */
class DashboardContractSerializationTest {
    private static final LocalDate D=LocalDate.of(2026,10,19);
    private static final Clock CLOCK=Clock.fixed(Instant.parse("2026-10-19T08:38:00Z"),ZoneOffset.UTC);
    private final ApplicationContextRunner context=new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class));

    private static AdminDashboardResponse read(InMemoryDashboardReadRepository repository) {
        return new DashboardQueryService(repository,CLOCK).getAdminDashboard();
    }

    @Test
    void acceptedV11FieldsSerializeWithoutLosingAnyV1Field() {
        AdminDashboardResponse response=read(new InMemoryDashboardReadRepository());
        context.run(c -> {
            JsonMapper mapper=c.getBean(JsonMapper.class);
            String serialized=mapper.writeValueAsString(response);
            JsonNode json=mapper.readTree(serialized);
            for(String field:List.of("source","businessDate","zoneId","generatedAt","period","appointments","exams",
                    "collections","receivables","staffing","inventory","emergencyKit","treatmentDue","workQueue","warnings")) {
                assertTrue(json.has(field),field);
            }
            assertEquals("LIVE",json.get("source").asString());
            assertEquals("2026-10-19",json.get("businessDate").asString());
            assertTrue(json.get("generatedAt").asString().endsWith("+07:00"));
            assertEquals(0,json.get("appointments").get("activeCount").asLong());
            assertEquals(7,json.get("appointments").get("byStatus").size());
            assertEquals(0,json.get("receivables").get("overpaidInvoiceCount").asLong());
            assertTrue(json.get("staffing").has("understaffedShiftCount"));
            assertTrue(json.get("inventory").has("anomalyLotCount"));
            assertTrue(json.get("emergencyKit").has("checkState"));
            assertEquals(7,json.get("emergencyKit").get("checkCycleDays").asInt());
            assertEquals("0",json.get("collections").get("amountVnd").asString());
            assertTrue(json.get("collections").get("amountVnd").isString());
            assertTrue(json.get("warnings").size()>0);
            for(JsonNode warning:json.get("warnings")) {
                assertEquals(3,warning.size());
                for(String field:List.of("code","section","message")) assertTrue(warning.has(field));
            }
            AdminDashboardResponse decoded=mapper.readValue(serialized,AdminDashboardResponse.class);
            assertEquals(response.appointments(),decoded.appointments());
            assertEquals(response.receivables(),decoded.receivables());
            assertEquals(response.warnings(),decoded.warnings());
            assertFalse(serialized.contains("matKhau")); assertFalse(serialized.contains("soDienThoai"));
        });
    }

    @Test
    void unavailableCountsAreExplicitJsonNullNotHiddenOrZero() {
        var repository=new InMemoryDashboardReadRepository();
        repository.failingMethods.addAll(List.of("findShifts","findNonEmptyStockLots","countActiveRealEmergencyCatalogItems",
                "countPlansWithPendingItemsDueBefore"));
        AdminDashboardResponse response=read(repository);
        context.run(c -> {
            JsonNode json=c.getBean(JsonMapper.class).valueToTree(response);
            assertEquals("UNAVAILABLE",json.get("staffing").get("status").asString());
            assertTrue(json.get("staffing").get("understaffedShiftCount").isNull());
            assertEquals(0,json.get("staffing").get("shifts").size());
            for(String count:List.of("belowMinimumItemCount","nearExpiryLotCount","expiredLotCount","unknownExpiryLotCount","anomalyLotCount")) {
                assertTrue(json.get("inventory").has(count)); assertTrue(json.get("inventory").get(count).isNull());
            }
            assertTrue(json.get("emergencyKit").get("checkState").isNull());
            assertTrue(json.get("treatmentDue").get("duePlanCount").isNull());
            assertTrue(json.get("treatmentDue").get("overduePlanCount").isNull());
            assertFalse(json.toString().contains("secret_column"));
        });
    }

    @Test
    void treatmentOverlapAndAcceptedThresholdKeyAreExplicit() {
        var repository=new InMemoryDashboardReadRepository();
        repository.parameters.put(DashboardThresholds.KEY_TREATMENT_HORIZON_DAYS,"21");
        repository.planItems.add(new InMemoryDashboardReadRepository.PlanItem("KH1",TrangThaiKeHoach.DANG_THUC_HIEN,
                TrangThaiChiTietDieuTri.CHUA_THUC_HIEN,D.minusDays(1)));
        repository.planItems.add(new InMemoryDashboardReadRepository.PlanItem("KH2",TrangThaiKeHoach.DANG_THUC_HIEN,
                TrangThaiChiTietDieuTri.CHUA_THUC_HIEN,D.plusDays(21)));
        var response=read(repository);
        assertEquals("SO_NGAY_KE_HOACH_DEN_HAN",DashboardThresholds.KEY_TREATMENT_HORIZON_DAYS);
        assertEquals(2L,response.treatmentDue().duePlanCount()); assertEquals(1L,response.treatmentDue().overduePlanCount());
        assertEquals(21,response.treatmentDue().horizonDays());
        assertTrue(response.workQueue().items().stream().filter(i -> i.code().equals("KE_HOACH_DEN_HAN"))
                .allMatch(i -> i.title().contains("bao gồm quá hạn")));
        assertTrue(response.workQueue().relatedObjectCount()>=3);
        assertFalse(response.warnings().stream().anyMatch(w -> w.section().equals("treatmentDue")));
        repository.parameters.clear();
        var fallback=read(repository);
        assertEquals(14,fallback.treatmentDue().horizonDays());
        assertTrue(fallback.warnings().stream().anyMatch(w -> w.code().equals("THAM_SO_CHUA_CAU_HINH") && w.section().equals("treatmentDue")));
    }

    @Test
    void targetScreensMatchActualSwingMenuSourceWhenItExists() throws Exception {
        Path menu=Path.of("..","admin-desktop-preview","src","main","java","com","manage","nhakhoa","desktop","MenuDefinition.java");
        assumeTrue(Files.isRegularFile(menu),"Gate UI-M1 PENDING: chưa có MenuDefinition thực tế");
        Set<String> actual=new HashSet<>();
        var matcher=Pattern.compile("e\\(\"([A-Z0-9_]+)\"").matcher(Files.readString(menu));
        while(matcher.find()) actual.add(matcher.group(1));
        assertEquals(20,actual.size());
        for(var field:DashboardTargetScreens.class.getFields()) {
            if(field.getType()==String.class) assertTrue(actual.contains((String)field.get(null)),field.getName());
        }
        var repository=new InMemoryDashboardReadRepository();
        repository.cancellationRequests.add(com.manage.nhakhoa.shared.enums.TrangThaiDuyet.CHO_DUYET);
        WorkQueueItem cancellation=read(repository).workQueue().items().stream()
                .filter(i -> i.code().equals("HOA_DON_CHO_DUYET_HUY")).findFirst().orElseThrow();
        assertEquals("DUYET_HUY",cancellation.targetScreen());
    }

    @Test
    void acceptedExtensionsKeepNonZeroValuesAndUnknownEmergencyStateInJson() {
        var repository=new InMemoryDashboardReadRepository();
        repository.appointments.add(new InMemoryDashboardReadRepository.Appointment(D.atStartOfDay(),
                com.manage.nhakhoa.features.lichhen.enums.TrangThaiLichHen.DA_DEN));
        repository.invoices.put("HD1",new InMemoryDashboardReadRepository.Invoice("HD1",
                com.manage.nhakhoa.features.hoadon.enums.TrangThaiHoaDon.DA_THANH_TOAN));
        repository.invoiceLines.add(new InMemoryDashboardReadRepository.InvoiceLine("HD1",1,new java.math.BigDecimal("100")));
        repository.payments.add(new InMemoryDashboardReadRepository.Payment("HD1",D.atTime(8,0),new java.math.BigDecimal("120"),
                com.manage.nhakhoa.features.hoadon.enums.PhuongThucThanhToan.TIEN_MAT,
                com.manage.nhakhoa.features.hoadon.enums.TrangThaiThanhToan.THANH_CONG));
        repository.shifts.add(new com.manage.nhakhoa.features.dashboard.repository.ShiftRow("CA1","Ca thử",
                java.time.LocalTime.of(7,0),java.time.LocalTime.NOON,1,0,0));
        repository.lots.add(new InMemoryDashboardReadRepository.Lot("BAD",null,null,D.plusDays(90),1));
        repository.catalog.add(new InMemoryDashboardReadRepository.Catalog(true,false));
        repository.checks.add(new InMemoryDashboardReadRepository.Check("KT1",D.atTime(7,45),
                com.manage.nhakhoa.features.capcuu.enums.KetQuaKiemTra.DAT));
        var response=read(repository);
        context.run(c -> {
            JsonNode json=c.getBean(JsonMapper.class).valueToTree(response);
            assertEquals(1,json.get("appointments").get("activeCount").asLong());
            assertEquals(1,json.get("receivables").get("overpaidInvoiceCount").asLong());
            assertEquals(1,json.get("staffing").get("understaffedShiftCount").asInt());
            assertEquals(1,json.get("inventory").get("anomalyLotCount").asInt());
            assertEquals("CHUA_DU_THONG_TIN",json.get("emergencyKit").get("checkState").asString());
        });
    }
}
