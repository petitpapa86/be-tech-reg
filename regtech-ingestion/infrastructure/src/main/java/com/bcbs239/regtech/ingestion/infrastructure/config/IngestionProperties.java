package com.bcbs239.regtech.ingestion.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Configuration properties for the Ingestion Module.
 */
@ConfigurationProperties(prefix = "ingestion")
public record IngestionProperties(
    FileProperties file,
    ProcessingProperties processing
) {
    
    public IngestionProperties {
        if (file == null) file = new FileProperties(524288000L, List.of("application/json", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        if (processing == null) processing = new ProcessingProperties(true, 10, 100);
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

}



