package com.bcbs239.regtech.reportgeneration.infrastructure.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Async configuration for report generation module.
 * 
 * Configures the thread pool executor for asynchronous event processing.
 * This is a minimal configuration to support task 7.1 (ReportEventListener).
 * Full configuration will be implemented in task 19.1.
 * 
 * Requirements: 1.3, 2.1, 2.2
 */
@Configuration
@EnableAsync
@Slf4j
public class AsyncConfiguration {
    
    /**
     * Creates the async executor for report generation tasks.
     * 
     * Configuration:
     * - Core pool size: 2 threads (handles typical load)
     * - Max pool size: 5 threads (handles peak load)
     * - Queue capacity: 100 tasks (buffers during spikes)
     * - Thread name prefix: "report-gen-" (for easy identification in logs)
     * 
     * @return the configured executor
     */
    @Bean(name = "reportGenerationExecutor")
    public ThreadPoolTaskExecutor reportGenerationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("report-gen-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        
        log.info("Initialized reportGenerationExecutor with corePoolSize=2, maxPoolSize=5, queueCapacity=100");
        
        return executor;
    }
}
