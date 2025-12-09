package com.bcbs239.regtech.app.monitoring;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Configuration for health monitoring system.
 * Enables scheduling for periodic health checks and configures health monitoring components.
 * 
 * Requirements: 4.1, 4.5
 * - Enable scheduled health checks
 * - Configure health monitoring service
 */
@Configuration
@EnableScheduling
public class HealthMonitoringConfiguration {
    
    // Configuration is handled via @Component annotations on the health indicators
    // and @Service annotation on HealthMonitoringService
    // This class just enables scheduling for the @Scheduled methods
}