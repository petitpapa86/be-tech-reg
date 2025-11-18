package com.bcbs239.regtech.dataquality.infrastructure.deprecated.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Async configuration for the data-quality module.
 */
@Deprecated
public class DataQualityAsyncConfig {

    /**
     * Creates a dedicated executor for processing quality-related events asynchronously.
     */
    // Deprecated: bean definitions moved to infrastructure.config.DataQualityAsyncConfig
    // This class is kept for backward-compatibility and should not be used by Spring.
    // Note: intentionally not annotated with @Configuration to avoid registering beans.
    public Executor qualityEventExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("quality-event-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }
}
