package com.manage.nhakhoa.features.dashboard.config;

import com.manage.nhakhoa.features.dashboard.service.DashboardQueryService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/** Cung cấp {@link Clock} theo múi giờ nghiệp vụ nếu ứng dụng chưa khai báo bean Clock khác (BA V1 Q02). */
@Configuration
public class DashboardClockConfig {

    @Bean
    @ConditionalOnMissingBean(Clock.class)
    public Clock businessClock() {
        return Clock.system(DashboardQueryService.BUSINESS_ZONE);
    }
}
