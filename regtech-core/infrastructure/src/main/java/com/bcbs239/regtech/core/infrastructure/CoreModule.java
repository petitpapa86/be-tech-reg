package com.bcbs239.regtech.core.infrastructure;

import com.bcbs239.regtech.core.infrastructure.s3.S3Properties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Core module configuration — provides shared infrastructure features used across
 * the monolith such as transaction management, async processing and scheduling.
 */
@Configuration
@EnableTransactionManagement // Shared transaction management
@EnableAsync                 // Shared async processing
@EnableScheduling            // Shared scheduling
@EnableConfigurationProperties(S3Properties.class)
public class CoreModule {

}
