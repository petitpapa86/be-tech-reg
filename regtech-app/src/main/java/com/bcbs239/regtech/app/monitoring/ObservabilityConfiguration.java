package com.bcbs239.regtech.app.monitoring;

import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.aop.ObservedAspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

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
     * Configures RestTemplate for notification service HTTP calls.
     * Used by NotificationService for Slack and webhook notifications.
     */
    @Bean
    public RestTemplate restTemplate() {
        logger.info("Configuring RestTemplate for notification service");
        return new RestTemplate();
    }
}
