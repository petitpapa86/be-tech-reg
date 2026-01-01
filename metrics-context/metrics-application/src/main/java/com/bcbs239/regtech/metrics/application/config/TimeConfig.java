package com.bcbs239.regtech.metrics.application.config;

import com.bcbs239.regtech.core.application.TimeProvider;
import com.bcbs239.regtech.core.infrastructure.systemservices.SystemTimeProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TimeConfig {

    @Bean
    public TimeProvider timeProvider() {
        return new SystemTimeProvider();
    }
}
