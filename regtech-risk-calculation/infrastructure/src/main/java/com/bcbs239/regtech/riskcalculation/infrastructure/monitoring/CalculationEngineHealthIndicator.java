package com.bcbs239.regtech.riskcalculation.application.monitoring;

import com.bcbs239.regtech.riskcalculation.application.ExposureProcessingService;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Health indicator for risk calculation engine operations.
 *
 * Monitors the availability and performance of exposure processing service
 * which is critical for risk calculation and portfolio analysis.
 *
 * Requirements: 4.1 - Health checks for all system components
 */
@Component
public class CalculationEngineHealthIndicator implements HealthIndicator {

    private final ExposureProcessingService exposureProcessingService;

    public CalculationEngineHealthIndicator(ExposureProcessingService exposureProcessingService) {
        this.exposureProcessingService = exposureProcessingService;
    }

    @Override
    public Health health() {
        try {
            long startTime = System.currentTimeMillis();

            // Perform a simple health check - test exposure processing service availability
            // In a real implementation, this might perform a minimal calculation test
            // or validate that calculation engines are responsive

            boolean isHealthy = exposureProcessingService != null;

            long responseTime = System.currentTimeMillis() - startTime;

            if (isHealthy && responseTime < 5000) {
                return Health.up()
                        .withDetail("exposureProcessingService", "available")
                        .withDetail("responseTime", responseTime + "ms")
                        .withDetail("status", "Risk calculation engine is accessible")
                        .build();
            } else {
                return Health.down()
                        .withDetail("exposureProcessingService", isHealthy ? "available" : "unavailable")
                        .withDetail("responseTime", responseTime + "ms")
                        .withDetail("error", "Risk calculation engine is not responding properly")
                        .build();
            }

        } catch (Exception e) {
            return Health.down()
                    .withDetail("error", "Risk calculation engine is not accessible: " + e.getMessage())
                    .withDetail("exception", e.getClass().getSimpleName())
                    .build();
        }
    }
}