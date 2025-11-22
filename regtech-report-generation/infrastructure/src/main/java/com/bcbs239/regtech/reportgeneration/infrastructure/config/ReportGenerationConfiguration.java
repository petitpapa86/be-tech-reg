package com.bcbs239.regtech.reportgeneration.infrastructure.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Report Generation Configuration.
 * 
 * Configures the thread pool executor for asynchronous event processing and report generation.
 * This configuration supports concurrent processing of multiple batch reports while maintaining
 * thread safety and resource efficiency.
 * 
 * Key Features:
 * - Dedicated thread pool for report generation tasks
 * - Configurable pool sizes for different environments (dev/prod)
 * - Graceful shutdown with task completion
 * - Custom rejection policy for queue overflow
 * - Thread naming for easy identification in logs and monitoring
 * 
 * Requirements: 1.3, 2.1, 2.2
 * - 1.3: Async processing with @Async annotation
 * - 2.1: Dedicated thread pool for concurrent batch processing
 * - 2.2: Named executor for async event processing
 */
@Configuration
@EnableAsync
@EnableScheduling
@Slf4j
public class ReportGenerationConfiguration {
    
    @Value("${report-generation.async.core-pool-size:2}")
    private int corePoolSize;
    
    @Value("${report-generation.async.max-pool-size:5}")
    private int maxPoolSize;
    
    @Value("${report-generation.async.queue-capacity:100}")
    private int queueCapacity;
    
    @Value("${report-generation.async.thread-name-prefix:report-gen-}")
    private String threadNamePrefix;
    
    @Value("${report-generation.async.await-termination-seconds:60}")
    private int awaitTerminationSeconds;
    
    /**
     * Creates the async executor for report generation tasks.
     * 
     * Configuration:
     * - Core pool size: Configurable (default 2 threads for typical load)
     * - Max pool size: Configurable (default 5 threads for peak load)
     * - Queue capacity: Configurable (default 100 tasks to buffer during spikes)
     * - Thread name prefix: Configurable (default "report-gen-" for easy identification)
     * - Graceful shutdown: Waits for tasks to complete before shutdown
     * - Rejection policy: CallerRunsPolicy to prevent task loss during overload
     * 
     * Thread Pool Sizing Rationale:
     * - Core pool size of 2 handles typical concurrent batch processing
     * - Max pool size of 5 accommodates peak loads without excessive resource consumption
     * - Queue capacity of 100 provides sufficient buffering for event bursts
     * - Production environments can increase these values via configuration
     * 
     * @return the configured executor for report generation
     */
    @Bean(name = "reportGenerationExecutor")
    public ThreadPoolTaskExecutor reportGenerationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // Core thread pool configuration
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix(threadNamePrefix);
        
        // Graceful shutdown configuration
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(awaitTerminationSeconds);
        
        // Rejection policy: CallerRunsPolicy ensures tasks are not lost
        // When queue is full, the calling thread executes the task
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        
        // Thread factory configuration for better monitoring
        executor.setThreadGroupName("report-generation");
        executor.setAllowCoreThreadTimeOut(false);
        
        executor.initialize();
        
        log.info("Initialized reportGenerationExecutor: corePoolSize={}, maxPoolSize={}, queueCapacity={}, threadNamePrefix='{}'",
                corePoolSize, maxPoolSize, queueCapacity, threadNamePrefix);
        
        return executor;
    }
    
    /**
     * Custom rejection handler that logs rejected tasks for monitoring.
     * This is used in addition to CallerRunsPolicy to track queue overflow events.
     */
    private static class LoggingRejectedExecutionHandler implements RejectedExecutionHandler {
        private final RejectedExecutionHandler delegate;
        
        public LoggingRejectedExecutionHandler(RejectedExecutionHandler delegate) {
            this.delegate = delegate;
        }
        
        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            log.warn("Task rejected by reportGenerationExecutor. Queue full. Active threads: {}, Queue size: {}, Pool size: {}",
                    executor.getActiveCount(), executor.getQueue().size(), executor.getPoolSize());
            delegate.rejectedExecution(r, executor);
        }
    }
}
