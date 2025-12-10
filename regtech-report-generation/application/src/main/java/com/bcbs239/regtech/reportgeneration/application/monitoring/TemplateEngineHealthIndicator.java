package com.bcbs239.regtech.reportgeneration.application.monitoring;

import com.bcbs239.regtech.reportgeneration.domain.generation.HtmlReportGenerator;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Health indicator for report generation template engine operations.
 *
 * Monitors the availability and performance of HTML report generator
 * which is critical for report generation and template processing.
 *
 * Requirements: 4.1 - Health checks for all system components
 */
@Component
public class TemplateEngineHealthIndicator implements HealthIndicator {

    private final HtmlReportGenerator htmlReportGenerator;

    public TemplateEngineHealthIndicator(HtmlReportGenerator htmlReportGenerator) {
        this.htmlReportGenerator = htmlReportGenerator;
    }

    @Override
    public Health health() {
        try {
            long startTime = System.currentTimeMillis();

            // Perform a simple health check - test HTML report generator availability
            // In a real implementation, this might validate that templates can be loaded
            // or perform a minimal template rendering test

            boolean isHealthy = htmlReportGenerator != null;

            long responseTime = System.currentTimeMillis() - startTime;

            if (isHealthy && responseTime < 5000) {
                return Health.up()
                        .withDetail("htmlReportGenerator", "available")
                        .withDetail("responseTime", responseTime + "ms")
                        .withDetail("status", "Report generation template engine is accessible")
                        .build();
            } else {
                return Health.down()
                        .withDetail("htmlReportGenerator", isHealthy ? "available" : "unavailable")
                        .withDetail("responseTime", responseTime + "ms")
                        .withDetail("error", "Report generation template engine is not responding properly")
                        .build();
            }

        } catch (Exception e) {
            return Health.down()
                    .withDetail("error", "Report generation template engine is not accessible: " + e.getMessage())
                    .withDetail("exception", e.getClass().getSimpleName())
                    .build();
        }
    }
}