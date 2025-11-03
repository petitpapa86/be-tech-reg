package com.bcbs239.regtech.ingestion.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Configuration properties for the Ingestion Module.
 */
@ConfigurationProperties(prefix = "ingestion")
public record IngestionProperties(
    boolean enabled,
    FileProperties file,
    ProcessingProperties processing,
    OutboxProperties outbox
) {
    
    public IngestionProperties {
        // Set defaults if null
        if (enabled == false) enabled = true;
        if (file == null) file = new FileProperties(524288000L, List.of("application/json", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        if (processing == null) processing = new ProcessingProperties(true, 10, 100);
        if (outbox == null) outbox = new OutboxProperties(true, 30000L, 60000L, 3, 86400000L, 30);
    }
    
    public record FileProperties(
        long maxSize,
        List<String> supportedTypes
    ) {}
    
    public record ProcessingProperties(
        boolean asyncEnabled,
        int threadPoolSize,
        int queueCapacity
    ) {}
    
    public record OutboxProperties(
        boolean enabled,
        long processingInterval,
        long retryInterval,
        int maxRetries,
        long cleanupInterval,
        int cleanupRetentionDays
    ) {}
}