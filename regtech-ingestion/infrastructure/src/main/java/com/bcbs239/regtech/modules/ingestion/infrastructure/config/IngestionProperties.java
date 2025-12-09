package com.bcbs239.regtech.modules.ingestion.infrastructure.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * Configuration properties for Ingestion module
 * Bound from application-ingestion.yml
 * Requirements: 6.2, 6.5, 10.3
 */
@Data
@Validated
@ConfigurationProperties(prefix = "ingestion")
public class IngestionProperties {

    @NotNull(message = "Ingestion enabled flag must be specified")
    private Boolean enabled = true;

    @NotNull(message = "File configuration must be specified")
    private FileProperties file = new FileProperties();

    @NotNull(message = "Storage configuration must be specified")
    private StorageProperties storage = new StorageProperties();

    @NotNull(message = "Async configuration must be specified")
    private AsyncProperties async = new AsyncProperties();

    @NotNull(message = "Processing configuration must be specified")
    private ProcessingProperties processing = new ProcessingProperties();

    @NotNull(message = "Performance configuration must be specified")
    private PerformanceProperties performance = new PerformanceProperties();

    private ParserProperties parser = new ParserProperties();

    @NotNull(message = "Retry configuration must be specified")
    private RetryProperties retry = new RetryProperties();

    /**
     * File upload settings
     */
    @Data
    public static class FileProperties {
        @Min(value = 1, message = "Max file size must be at least 1 byte")
        private long maxSize = 524288000L; // 500MB in bytes

        @NotNull(message = "Supported file types must be specified")
        private List<String> supportedTypes = List.of(
            "application/json",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        );
    }

    /**
     * Storage configuration supporting both S3 and local filesystem
     */
    @Data
    public static class StorageProperties {
        @NotBlank(message = "Storage type must be specified (s3 or local)")
        private String type = "s3";

        @NotNull(message = "S3 configuration must be specified")
        private S3Properties s3 = new S3Properties();

        @NotNull(message = "Local storage configuration must be specified")
        private LocalProperties local = new LocalProperties();

        @Data
        public static class S3Properties {
            @NotBlank(message = "S3 bucket name must be specified")
            private String bucket;

            @NotBlank(message = "S3 region must be specified")
            private String region;

            @NotBlank(message = "S3 prefix must be specified")
            private String prefix;

            private String accessKey;
            private String secretKey;
            private String endpoint;

            @NotBlank(message = "S3 encryption type must be specified")
            private String encryption = "AES256";
        }

        @Data
        public static class LocalProperties {
            @NotBlank(message = "Local storage base path must be specified")
            private String basePath = "./data/ingestion";

            private boolean createDirectories = true;
        }
    }

    /**
     * Async thread pool configuration
     * Purpose: Handles async file upload and processing operations
     */
    @Data
    public static class AsyncProperties {
        private boolean enabled = true;

        @Min(value = 1, message = "Core pool size must be at least 1")
        private int corePoolSize = 5;

        @Min(value = 1, message = "Max pool size must be at least 1")
        private int maxPoolSize = 10;

        @Min(value = 0, message = "Queue capacity must be non-negative")
        private int queueCapacity = 100;

        @NotBlank(message = "Thread name prefix must be specified")
        private String threadNamePrefix = "ingestion-async-";

        @Min(value = 0, message = "Await termination seconds must be non-negative")
        private int awaitTerminationSeconds = 60;
    }

    /**
     * Processing configuration
     */
    @Data
    public static class ProcessingProperties {
        private boolean asyncEnabled = true;

        @Min(value = 1, message = "Thread pool size must be at least 1")
        private int threadPoolSize = 10;

        @Min(value = 0, message = "Queue capacity must be non-negative")
        private int queueCapacity = 100;
    }

    /**
     * Performance optimization settings
     */
    @Data
    public static class PerformanceProperties {
        @Min(value = 1, message = "Max concurrent files must be at least 1")
        private int maxConcurrentFiles = 4;

        @Min(value = 1, message = "Chunk size must be at least 1")
        private int chunkSize = 10000;
    }

    /**
     * Parser settings
     */
    @Data
    public static class ParserProperties {
        @Min(value = 1, message = "Default max records must be at least 1")
        private int defaultMaxRecords = 10000;
    }

    /**
     * Retry settings for failed batch processing
     */
    @Data
    public static class RetryProperties {
        @Min(value = 1, message = "Max retries must be at least 1")
        private int maxRetries = 5;
    }
}
