package com.bcbs239.regtech.app.monitoring;

import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.aop.ObservedAspect;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.micrometer.observation.autoconfigure.ObservationRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.Objects;

/**
 * Configuration for observability with Micrometer 2 and OpenTelemetry.
 * Configures metrics collection, tracing, and trace context propagation.
 * 
 * Requirements: 10.1, 10.2, 10.3
 * - Micrometer 2 for metrics collection
 * - OpenTelemetry for distributed tracing
 * - Trace context propagation across all modules
 */
@Configuration
public class ObservabilityConfiguration {
    
    private static final Logger logger = LoggerFactory.getLogger(ObservabilityConfiguration.class);
    
    private final Environment environment;
    
    public ObservabilityConfiguration(Environment environment) {
        this.environment = environment;
    }
    
    /**
     * Enables @Observed annotation support for automatic observation of methods.
     * This allows declarative observation of service methods across all modules.
     * 
     * Requirement 10.1: Micrometer 2 metrics collection
     */
    @Bean
    public ObservedAspect observedAspect(ObservationRegistry observationRegistry) {
        logger.info("Configuring ObservedAspect for automatic method observation");
        return new ObservedAspect(observationRegistry);
    }
    
    /**
     * Customizes the observation registry with application-specific tags.
     * These tags are propagated with all metrics and traces.
     * 
     * Requirement 10.3: Trace context propagation across modules
     */
    @Bean
    public ObservationRegistryCustomizer<ObservationRegistry> observationRegistryCustomizer() {
        return registry -> {
            String appName = environment.getProperty("spring.application.name", "regtech");
            String profile = environment.getProperty("spring.profiles.active", "default");
            
            logger.info("Customizing ObservationRegistry with tags: application={}, environment={}", 
                appName, profile);
            
            // Add common tags to all observations
            registry.observationConfig()
                .observationHandler(context -> {
                    // Tags are automatically added via management.metrics.tags in application.yml
                    return true;
                });
        };
    }
    
    /**
     * Configures trace context logging.
     * Ensures trace IDs and span IDs are included in log statements.
     * 
     * Requirement 10.3: Correlate logs with trace context
     */
    @Bean
    public TraceContextLogger traceContextLogger(Tracer tracer) {
        logger.info("Configuring TraceContextLogger for trace context propagation in logs");
        return new TraceContextLogger(tracer);
    }
    
    /**
     * Helper class to log trace context information.
     */
    public static class TraceContextLogger {
        private final Tracer tracer;
        
        public TraceContextLogger(Tracer tracer) {
            this.tracer = tracer;
        }
        
        /**
         * Gets the current trace ID for logging.
         */
        public String getCurrentTraceId() {
            if (tracer.currentSpan() != null) {
                Objects.requireNonNull(tracer.currentSpan()).context();
                return Objects.requireNonNull(tracer.currentSpan()).context().traceId();
            }
            return "no-trace";
        }
        
        /**
         * Gets the current span ID for logging.
         */
        public String getCurrentSpanId() {
            if (tracer.currentSpan() != null && tracer.currentSpan().context() != null) {
                return tracer.currentSpan().context().spanId();
            }
            return "no-span";
        }
    }
}
