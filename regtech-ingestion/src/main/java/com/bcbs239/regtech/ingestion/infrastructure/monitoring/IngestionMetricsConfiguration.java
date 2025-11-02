package com.bcbs239.regtech.ingestion.infrastructure.monitoring;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.config.MeterFilter;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for ingestion module metrics.
 * Sets up custom meter filters and registry customizations.
 */
@Configuration
public class IngestionMetricsConfiguration {

    /**
     * Customize the meter registry for ingestion-specific metrics.
     */
    @Bean
    public MeterRegistryCustomizer<MeterRegistry> ingestionMeterRegistryCustomizer() {
        return registry -> {
            // Add common tags for all ingestion metrics
            registry.config()
                    .commonTags("module", "ingestion", "service", "regtech")
                    .meterFilter(MeterFilter.deny(id -> {
                        // Filter out noisy metrics that aren't relevant for ingestion monitoring
                        String name = id.getName();
                        return name.startsWith("jvm.gc.pause") ||
                               name.startsWith("jvm.gc.concurrent.phase.time") ||
                               name.startsWith("tomcat.sessions");
                    }))
                    .meterFilter(MeterFilter.maximumExpectedValue("ingestion.file.processing.duration", 
                            java.time.Duration.ofMinutes(10)))
                    .meterFilter(MeterFilter.maximumExpectedValue("ingestion.s3.upload.duration", 
                            java.time.Duration.ofMinutes(5)))
                    .meterFilter(MeterFilter.maximumExpectedValue("ingestion.database.query.duration", 
                            java.time.Duration.ofSeconds(30)));
        };
    }
}