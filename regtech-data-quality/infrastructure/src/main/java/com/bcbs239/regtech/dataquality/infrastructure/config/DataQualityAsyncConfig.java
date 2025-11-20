package com.bcbs239.regtech.dataquality.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Async configuration for the data-quality module.
 * Configures thread pool executor for async event processing.
 */
@Configuration
//@EnableAsync
public class DataQualityAsyncConfig {

    /**
     * Creates a dedicated executor for processing quality-related events asynchronously.
     * This executor is used by event listeners in the data-quality module.
     * 
     * @return configured ThreadPoolTaskExecutor
     */
    @Bean(name = "qualityEventExecutor")
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
