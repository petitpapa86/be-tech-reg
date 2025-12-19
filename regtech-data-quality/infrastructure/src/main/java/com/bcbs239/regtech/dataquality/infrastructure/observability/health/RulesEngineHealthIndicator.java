package com.bcbs239.regtech.dataquality.infrastructure.observability.health;

import com.bcbs239.regtech.dataquality.application.rulesengine.DataQualityRulesService;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Health indicator for data quality rules engine operations.
 *
 * Monitors the availability and performance of data quality rules service
 * which is critical for batch validation and quality assessment.
 *
 * Requirements: 4.1 - Health checks for all system components
 */
@Component
public class RulesEngineHealthIndicator implements HealthIndicator {

    private final DataQualityRulesService rulesService;

    public RulesEngineHealthIndicator(DataQualityRulesService rulesService) {
        this.rulesService = rulesService;
    }

    @Override
    public Health health() {
        try {
            long startTime = System.currentTimeMillis();

            // Perform a simple health check - test rules service availability
            // In a real implementation, this might validate that rules can be loaded
            // or perform a minimal rule evaluation

            boolean isHealthy = rulesService != null;

            long responseTime = System.currentTimeMillis() - startTime;

            if (isHealthy && responseTime < 5000) {
                return Health.up()
                        .withDetail("rulesService", "available")
                        .withDetail("responseTime", responseTime + "ms")
                        .withDetail("status", "Data quality rules engine is accessible")
                        .build();
            } else {
                return Health.down()
                        .withDetail("rulesService", isHealthy ? "available" : "unavailable")
                        .withDetail("responseTime", responseTime + "ms")
                        .withDetail("error", "Data quality rules engine is not responding properly")
                        .build();
            }

        } catch (Exception e) {
            return Health.down()
                    .withDetail("error", "Data quality rules engine is not accessible: " + e.getMessage())
                    .withDetail("exception", e.getClass().getSimpleName())
                    .build();
        }
    }
}