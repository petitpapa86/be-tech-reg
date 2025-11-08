package com.bcbs239.regtech.core.infrastructure;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Core module configuration — provides shared infrastructure features used across
 * the monolith such as transaction management, async processing and scheduling.
 */
@Configuration
@ComponentScan(basePackages = "com.bcbs239.regtech.core")
@EnableTransactionManagement // Shared transaction management
@EnableAsync                 // Shared async processing
@EnableScheduling            // Shared scheduling
public class CoreModule {

}
