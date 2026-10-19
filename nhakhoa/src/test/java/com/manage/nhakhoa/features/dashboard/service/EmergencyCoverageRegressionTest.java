package com.manage.nhakhoa.features.dashboard.service;

import com.manage.nhakhoa.features.capcuu.enums.KetQuaKiemTra;
import com.manage.nhakhoa.features.dashboard.dto.response.EmergencyCheckState;
import com.manage.nhakhoa.features.dashboard.dto.response.SectionStatus;
import com.manage.nhakhoa.features.dashboard.repository.EmergencyCheckItemRow;
import com.manage.nhakhoa.features.dashboard.service.InMemoryDashboardReadRepository.Catalog;
import com.manage.nhakhoa.features.dashboard.service.InMemoryDashboardReadRepository.Check;
import com.manage.nhakhoa.features.dashboard.service.InMemoryDashboardReadRepository.CheckItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class EmergencyCoverageRegressionTest {
    static final LocalDate D=LocalDate.of(2026,10,19);
    InMemoryDashboardReadRepository repository;
    DashboardQueryService service;
    @BeforeEach void setup() {
        repository=new InMemoryDashboardReadRepository();
        repository.catalog.add(new Catalog("REAL",true,false,1));
        repository.checks.add(new Check("KT1",D.atTime(7,45),KetQuaKiemTra.DAT));
        service=new DashboardQueryService(repository,Clock.fixed(Instant.parse("2026-10-19T08:38:00Z"),DashboardQueryService.BUSINESS_ZONE));
    }
    @Test void zeroDetailsNeverMeansWithinCycle() { assertNeedsCheck(); }
    @Test void sampleDetailsCannotCoverRealCatalog() {
        repository.catalog.add(new Catalog("SAMPLE",true,true,1));
        repository.checkItems.add(new CheckItem("KT1","SAMPLE",true,D.plusDays(90),1)); assertNeedsCheck();
    }
    @Test void newlyAddedOfficialItemInvalidatesOldCoverage() {
        repository.checkItems.add(new CheckItem("KT1","REAL",true,D.plusDays(90),1));
        assertEquals(EmergencyCheckState.TRONG_CHU_KY,service.getAdminDashboard().emergencyKit().checkState());
        repository.catalog.add(new Catalog("NEW",true,false,1)); assertNeedsCheck();
    }
    @Test void missingExpiryIsUnknownEvenIfPassedFlagIsTrue() {
        repository.checkItems.add(new CheckItem("KT1","REAL",true,null,1)); assertNeedsCheck();
    }
    @Test void mixedCatalogOnlyOfficialActiveItemsMustBeCovered() {
        repository.catalog.add(new Catalog("SAMPLE",true,true,1)); repository.catalog.add(new Catalog("OLD",false,false,1));
        repository.checkItems.add(new CheckItem("KT1","REAL",true,D.plusDays(90),1));
        repository.checkItems.add(new CheckItem("KT1","SAMPLE",false,null,0));
        assertEquals(EmergencyCheckState.TRONG_CHU_KY,service.getAdminDashboard().emergencyKit().checkState());
    }
    @Test void latestIncompleteCheckDoesNotFallBackToOlderCompleteCheck() {
        repository.checkItems.add(new CheckItem("KT1","REAL",true,D.plusDays(90),1));
        repository.checks.add(new Check("KT2",D.atTime(8,0),KetQuaKiemTra.DAT)); assertNeedsCheck();
    }
    @Test void missingQuantityDoesNotMeanConfirmedCheck() {
        repository.checkItems.add(new CheckItem("KT1","REAL",true,D.plusDays(90),null)); assertNeedsCheck();
    }
    @Test void fullyCoveredFailingItemRequiresAction() {
        repository.checkItems.add(new CheckItem("KT1","REAL",false,D.plusDays(90),1));
        assertEquals(EmergencyCheckState.CAN_XU_LY,service.getAdminDashboard().emergencyKit().checkState());
    }
    @Test void coverageCountDriftAndDuplicateRowsCannotMeanComplete() {
        var row=new EmergencyCheckItemRow("REAL","REAL",true,D.plusDays(90),1,1);
        assertFalse(EmergencyCoverageCalculator.calculate(2,List.of(row),D).complete());
        assertFalse(EmergencyCoverageCalculator.calculate(2,List.of(row,row),D).complete());
    }
    private void assertNeedsCheck() {
        var result=service.getAdminDashboard();
        assertEquals(SectionStatus.AVAILABLE,result.emergencyKit().status());
        assertEquals(EmergencyCheckState.CHUA_DU_THONG_TIN,result.emergencyKit().checkState());
        assertTrue(result.emergencyKit().message().contains("cần kiểm tra"));
        assertTrue(result.warnings().stream().anyMatch(w -> w.code().equals("CAP_CUU_CHUA_DU_THONG_TIN")));
        assertTrue(result.workQueue().items().stream().anyMatch(w -> w.code().equals("CAP_CUU_CAN_KIEM_TRA")));
    }
}
