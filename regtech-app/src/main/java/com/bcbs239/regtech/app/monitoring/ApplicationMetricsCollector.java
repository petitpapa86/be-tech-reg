package com.bcbs239.regtech.app.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Application-wide metrics collector using Micrometer 2.
 * Provides custom metrics for monitoring application behavior.
 * 
 * Requirement 10.1: Micrometer 2 metrics collection
 */
@Component
public class ApplicationMetricsCollector {
    
    private static final Logger logger = LoggerFactory.getLogger(ApplicationMetricsCollector.class);
    
    private final MeterRegistry meterRegistry;
    
    // Application startup metrics
    private Counter applicationStartCounter;
    private Timer applicationStartupTimer;
    
    // Module initialization metrics
    private Counter moduleInitializationCounter;
    private Timer moduleInitializationTimer;
    
    public ApplicationMetricsCollector(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }
    
    @PostConstruct
    public void initializeMetrics() {
        logger.info("Initializing application metrics with Micrometer 2");
        
        // Application startup metrics
        applicationStartCounter = Counter.builder("regtech.application.starts")
            .description("Number of times the application has started")
            .tag("type", "startup")
            .register(meterRegistry);
        
        applicationStartupTimer = Timer.builder("regtech.application.startup.time")
            .description("Time taken for application startup")
            .tag("type", "startup")
            .register(meterRegistry);
        
        // Module initialization metrics
        moduleInitializationCounter = Counter.builder("regtech.module.initializations")
            .description("Number of module initializations")
            .tag("type", "module")
            .register(meterRegistry);
        
        moduleInitializationTimer = Timer.builder("regtech.module.initialization.time")
            .description("Time taken for module initialization")
            .tag("type", "module")
            .register(meterRegistry);
        
        logger.info("Application metrics initialized successfully");
    }
    
    /**
     * Records an application start event.
     */
    public void recordApplicationStart() {
        applicationStartCounter.increment();
        logger.debug("Recorded application start event");
    }
    
    /**
     * Records application startup time.
     */
    public void recordApplicationStartupTime(long durationMillis) {
        applicationStartupTimer.record(durationMillis, TimeUnit.MILLISECONDS);
        logger.info("Recorded application startup time: {}ms", durationMillis);
    }
    
    /**
     * Records a module initialization event.
     */
    public void recordModuleInitialization(String moduleName) {
        moduleInitializationCounter.increment();
        logger.debug("Recorded module initialization: {}", moduleName);
    }
    
    /**
     * Records module initialization time.
     */
    public void recordModuleInitializationTime(String moduleName, long durationMillis) {
        Timer.builder("regtech.module.initialization.time")
            .description("Time taken for module initialization")
            .tag("module", moduleName)
            .register(meterRegistry)
            .record(durationMillis, TimeUnit.MILLISECONDS);
        
        logger.info("Recorded module initialization time for {}: {}ms", moduleName, durationMillis);
    }
    
    /**
     * Creates a custom counter for a specific metric.
     */
    public Counter createCounter(String name, String description, String... tags) {
        Counter.Builder builder = Counter.builder(name)
            .description(description);
        
        // Add tags in pairs
        for (int i = 0; i < tags.length - 1; i += 2) {
            builder.tag(tags[i], tags[i + 1]);
        }
        
        return builder.register(meterRegistry);
    }
    
    /**
     * Creates a custom timer for a specific metric.
     */
    public Timer createTimer(String name, String description, String... tags) {
        Timer.Builder builder = Timer.builder(name)
            .description(description);
        
        // Add tags in pairs
        for (int i = 0; i < tags.length - 1; i += 2) {
            builder.tag(tags[i], tags[i + 1]);
        }
        
        return builder.register(meterRegistry);
    }
}
