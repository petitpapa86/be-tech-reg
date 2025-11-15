package com.bcbs239.regtech.ingestion.infrastructure.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Configuration properties for the Ingestion Module.
 */
@ConfigurationProperties(prefix = "ingestion")
public record IngestionProperties(
    FileProperties file,
    ProcessingProperties processing,
    PerformanceProperties performance,
    ParserProperties parser
) {
    
    public IngestionProperties {
        if (file == null) file = new FileProperties(524288000L, List.of("application/json", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        if (processing == null) processing = new ProcessingProperties(true, 10, 100);
        if (performance == null) performance = new PerformanceProperties(4, 10000);
        if (parser == null) parser = new ParserProperties(10000);
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
    
    public record PerformanceProperties(
        int maxConcurrentFiles,
        int chunkSize
    ) {}
    
    public record ParserProperties(
        int defaultMaxRecords
    ) {}

}



