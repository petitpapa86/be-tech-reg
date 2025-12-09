package com.bcbs239.regtech.app.monitoring;

import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.contextpropagation.ObservationThreadLocalAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Async Observability Configuration for trace context propagation in async operations.
 * 
 * This configuration ensures that Spring Boot 4 observation context (including traces)
 * is properly propagated across thread boundaries in async operations.
 * 
 * Requirements: 1.1, 1.2 - Async trace context propagation
 */
@Configuration
@EnableAsync
public class AsyncObservabilityConfiguration implements AsyncConfigurer {

    private static final Logger logger = LoggerFactory.getLogger(AsyncObservabilityConfiguration.class);

    private final ObservationRegistry observationRegistry;

    public AsyncObservabilityConfiguration(ObservationRegistry observationRegistry) {
        this.observationRegistry = observationRegistry;
    }

    /**
     * Task decorator that propagates observation context across thread boundaries.
     * 
     * @return TaskDecorator for observation context propagation
     */
    @Bean
    public TaskDecorator observationTaskDecorator() {
        return new ObservationTaskDecorator(observationRegistry);
    }

    /**
     * Default async executor with observation context propagation.
     * 
     * @return Configured async executor
     */
    @Override
    @Bean(name = "taskExecutor")
    public Executor getAsyncExecutor() {
        logger.info("Configuring default async executor with observation context propagation");
        
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("async-obs-");
        executor.setTaskDecorator(observationTaskDecorator());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        
        logger.info("Default async executor configured with {} core threads, {} max threads", 
                   executor.getCorePoolSize(), executor.getMaxPoolSize());
        
        return executor;
    }

    /**
     * Batch processing executor with observation context propagation.
     * 
     * @return Configured batch processing executor
     */
    @Bean("batchProcessingExecutor")
    public Executor batchProcessingExecutor() {
        logger.info("Configuring batch processing executor with observation context propagation");
        
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("batch-obs-");
        executor.setTaskDecorator(observationTaskDecorator());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        
        logger.info("Batch processing executor configured with {} core threads, {} max threads", 
                   executor.getCorePoolSize(), executor.getMaxPoolSize());
        
        return executor;
    }

    /**
     * Risk calculation executor with observation context propagation.
     * 
     * @return Configured risk calculation executor
     */
    @Bean("riskCalculationExecutor")
    public Executor riskCalculationExecutor() {
        logger.info("Configuring risk calculation executor with observation context propagation");
        
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(8);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("risk-obs-");
        executor.setTaskDecorator(observationTaskDecorator());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(120);
        executor.initialize();
        
        logger.info("Risk calculation executor configured with {} core threads, {} max threads", 
                   executor.getCorePoolSize(), executor.getMaxPoolSize());
        
        return executor;
    }

    /**
     * Report generation executor with observation context propagation.
     * 
     * @return Configured report generation executor
     */
    @Bean("reportGenerationExecutor")
    public Executor reportGenerationExecutor() {
        logger.info("Configuring report generation executor with observation context propagation");
        
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(3);
        executor.setMaxPoolSize(6);
        executor.setQueueCapacity(30);
        executor.setThreadNamePrefix("report-obs-");
        executor.setTaskDecorator(observationTaskDecorator());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(180);
        executor.initialize();
        
        logger.info("Report generation executor configured with {} core threads, {} max threads", 
                   executor.getCorePoolSize(), executor.getMaxPoolSize());
        
        return executor;
    }

    /**
     * Custom TaskDecorator that propagates observation context across threads.
     */
    public static class ObservationTaskDecorator implements TaskDecorator {
        
        private static final Logger logger = LoggerFactory.getLogger(ObservationTaskDecorator.class);
        
        private final ObservationRegistry observationRegistry;

        public ObservationTaskDecorator(ObservationRegistry observationRegistry) {
            this.observationRegistry = observationRegistry;
        }

        @Override
        public Runnable decorate(Runnable runnable) {
            // Capture the current observation context
            io.micrometer.context.ContextSnapshot contextSnapshot = 
                io.micrometer.context.ContextSnapshot.captureAll();
            
            return () -> {
                // Restore the observation context in the new thread
                try (io.micrometer.context.ContextSnapshot.Scope scope = contextSnapshot.setThreadLocals()) {
                    logger.debug("Executing task with propagated observation context");
                    runnable.run();
                } catch (Exception e) {
                    logger.error("Error executing task with observation context", e);
                    throw e;
                }
            };
        }
    }

    /**
     * Example service demonstrating async operations with observation context propagation.
     */
    @org.springframework.stereotype.Service
    public static class AsyncObservabilityExampleService {
        
        private static final Logger logger = LoggerFactory.getLogger(AsyncObservabilityExampleService.class);
        
        private final TraceContextManager traceContextManager;

        public AsyncObservabilityExampleService(TraceContextManager traceContextManager) {
            this.traceContextManager = traceContextManager;
        }

        /**
         * Example async method using default executor.
         */
        @org.springframework.scheduling.annotation.Async
        @io.micrometer.observation.annotation.Observed(
            name = "business.async.default",
            contextualName = "async-default-task"
        )
        public java.util.concurrent.CompletableFuture<String> processAsyncDefault(String taskId) {
            logger.info("Processing async task (default): {} in thread: {}", taskId, Thread.currentThread().getName());
            
            // Verify trace context is available
            if (traceContextManager.hasActiveTrace()) {
                String traceContext = traceContextManager.getFormattedTraceContext();
                logger.info("Trace context available in async task: {}", traceContext);
                traceContextManager.addBusinessContext("async.task.id", taskId);
            } else {
                logger.warn("No trace context available in async task: {}", taskId);
            }
            
            // Simulate work
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Task interrupted", e);
            }
            
            return java.util.concurrent.CompletableFuture.completedFuture("Completed: " + taskId);
        }

        /**
         * Example async method using batch processing executor.
         */
        @org.springframework.scheduling.annotation.Async("batchProcessingExecutor")
        @io.micrometer.observation.annotation.Observed(
            name = "business.async.batch",
            contextualName = "async-batch-task"
        )
        public java.util.concurrent.CompletableFuture<String> processBatchAsync(String batchId, int recordCount) {
            logger.info("Processing batch async: {} with {} records in thread: {}", 
                       batchId, recordCount, Thread.currentThread().getName());
            
            // Verify trace context is available
            if (traceContextManager.hasActiveTrace()) {
                String traceContext = traceContextManager.getFormattedTraceContext();
                logger.info("Trace context available in batch async task: {}", traceContext);
                traceContextManager.addBatchContext(batchId, "ASYNC");
                traceContextManager.addPerformanceContext("async", (long) recordCount);
            } else {
                logger.warn("No trace context available in batch async task: {}", batchId);
            }
            
            // Simulate batch processing
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Batch processing interrupted", e);
            }
            
            return java.util.concurrent.CompletableFuture.completedFuture("Batch processed: " + batchId);
        }

        /**
         * Example async method using risk calculation executor.
         */
        @org.springframework.scheduling.annotation.Async("riskCalculationExecutor")
        @io.micrometer.observation.annotation.Observed(
            name = "business.async.risk",
            contextualName = "async-risk-calculation"
        )
        public java.util.concurrent.CompletableFuture<Double> calculateRiskAsync(String portfolioId, int exposureCount) {
            logger.info("Calculating risk async: {} with {} exposures in thread: {}", 
                       portfolioId, exposureCount, Thread.currentThread().getName());
            
            // Verify trace context is available
            if (traceContextManager.hasActiveTrace()) {
                String traceContext = traceContextManager.getFormattedTraceContext();
                logger.info("Trace context available in risk async task: {}", traceContext);
                traceContextManager.addPortfolioContext(portfolioId, "ASYNC");
                traceContextManager.addPerformanceContext("calculation", (long) exposureCount);
            } else {
                logger.warn("No trace context available in risk async task: {}", portfolioId);
            }
            
            // Simulate risk calculation
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Risk calculation interrupted", e);
            }
            
            double riskScore = Math.random() * 100;
            return java.util.concurrent.CompletableFuture.completedFuture(riskScore);
        }
    }
}