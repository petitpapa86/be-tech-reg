package com.bcbs239.regtech.reportgeneration.infrastructure.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Async configuration for Report Generation module
 * Creates a dedicated thread pool for async report generation operations
 * Requirements: 13.1, 13.2, 13.5
 */
@Slf4j
@Configuration
@EnableAsync
@EnableConfigurationProperties(ReportGenerationProperties.class)
public class ReportGenerationAsyncConfiguration {

    private final ReportGenerationProperties properties;

    public ReportGenerationAsyncConfiguration(ReportGenerationProperties properties) {
        this.properties = properties;
        validatePoolSizes();
    }

    /**
     * Creates the report generation task executor bean
     * Purpose: Handles async HTML and XBRL report generation operations
     * 
     * @return configured ThreadPoolTaskExecutor
     */
    @Bean(name = "reportGenerationTaskExecutor")
    public Executor reportGenerationTaskExecutor() {
        ReportGenerationProperties.AsyncProperties async = properties.getAsync();

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(async.getCorePoolSize());
        executor.setMaxPoolSize(async.getMaxPoolSize());
        executor.setQueueCapacity(async.getQueueCapacity());
        executor.setThreadNamePrefix(async.getThreadNamePrefix());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(async.getAwaitTerminationSeconds());
        executor.initialize();

        log.info("Initialized report generation task executor - core: {}, max: {}, queue: {}", 
            async.getCorePoolSize(), 
            async.getMaxPoolSize(), 
            async.getQueueCapacity());

        return executor;
    }

    /**
     * Validates that pool sizes are configured correctly
     * Requirement 13.5: Validate thread pool sizes are positive integers at startup
     */
    private void validatePoolSizes() {
        ReportGenerationProperties.AsyncProperties async = properties.getAsync();
        
        if (async.getCorePoolSize() <= 0) {
            throw new IllegalArgumentException(
                "Report generation async core-pool-size must be positive, got: " + async.getCorePoolSize());
        }
        
        if (async.getMaxPoolSize() <= 0) {
            throw new IllegalArgumentException(
                "Report generation async max-pool-size must be positive, got: " + async.getMaxPoolSize());
        }
        
        if (async.getCorePoolSize() > async.getMaxPoolSize()) {
            throw new IllegalArgumentException(
                String.format("Report generation async core-pool-size (%d) must be <= max-pool-size (%d)",
                    async.getCorePoolSize(), async.getMaxPoolSize()));
        }
        
        if (async.getQueueCapacity() < 0) {
            throw new IllegalArgumentException(
                "Report generation async queue-capacity must be non-negative, got: " + async.getQueueCapacity());
        }
    }
}
