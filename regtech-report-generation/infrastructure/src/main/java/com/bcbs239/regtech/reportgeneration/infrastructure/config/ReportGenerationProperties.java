package com.bcbs239.regtech.reportgeneration.infrastructure.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * Configuration properties for Report Generation module
 * Bound from application-report-generation.yml
 * Requirements: 6.2, 6.5, 10.3
 */
@Data
@Validated
@ConfigurationProperties(prefix = "report-generation")
public class ReportGenerationProperties {

    @NotNull(message = "Report generation enabled flag must be specified")
    private Boolean enabled = true;

    @NotNull(message = "S3 configuration must be specified")
    private S3Properties s3 = new S3Properties();

    @NotNull(message = "Async configuration must be specified")
    private AsyncProperties async = new AsyncProperties();

    @NotNull(message = "File paths configuration must be specified")
    private FilePathsProperties filePaths = new FilePathsProperties();

    @NotNull(message = "Fallback configuration must be specified")
    private FallbackProperties fallback = new FallbackProperties();

    @NotNull(message = "Coordination configuration must be specified")
    private CoordinationProperties coordination = new CoordinationProperties();

    @NotNull(message = "Performance configuration must be specified")
    private PerformanceProperties performance = new PerformanceProperties();

    @NotNull(message = "Retry configuration must be specified")
    private RetryProperties retry = new RetryProperties();

    /**
     * S3 storage configuration
     */
    @Data
    public static class S3Properties {
        @NotBlank(message = "S3 bucket name must be specified")
        private String bucket;

        @NotBlank(message = "HTML prefix must be specified")
        private String htmlPrefix;

        @NotBlank(message = "XBRL prefix must be specified")
        private String xbrlPrefix;

        @NotBlank(message = "Encryption type must be specified")
        private String encryption = "AES256";

        @Min(value = 1, message = "Presigned URL expiration must be at least 1 hour")
        private int presignedUrlExpirationHours = 1;
    }

    /**
     * Async thread pool configuration
     * Purpose: Handles async report generation operations
     */
    @Data
    public static class AsyncProperties {
        @Min(value = 1, message = "Core pool size must be at least 1")
        private int corePoolSize = 2;

        @Min(value = 1, message = "Max pool size must be at least 1")
        private int maxPoolSize = 5;

        @Min(value = 0, message = "Queue capacity must be non-negative")
        private int queueCapacity = 100;

        @NotBlank(message = "Thread name prefix must be specified")
        private String threadNamePrefix = "report-gen-";

        @Min(value = 0, message = "Await termination seconds must be non-negative")
        private int awaitTerminationSeconds = 60;
    }

    /**
     * File path patterns for data retrieval
     */
    @Data
    public static class FilePathsProperties {
        @NotBlank(message = "Calculation pattern must be specified")
        private String calculationPattern;

        @NotBlank(message = "Quality pattern must be specified")
        private String qualityPattern;

        @NotBlank(message = "Local base path must be specified")
        private String localBasePath;

        @NotBlank(message = "Malformed JSON path must be specified")
        private String malformedJsonPath;
    }

    /**
     * Local fallback configuration
     */
    @Data
    public static class FallbackProperties {
        @NotBlank(message = "Local path must be specified")
        private String localPath;

        @Min(value = 1, message = "Retry interval must be at least 1 minute")
        private int retryIntervalMinutes = 30;

        @Min(value = 1, message = "Max retry attempts must be at least 1")
        private int maxRetryAttempts = 5;
    }

    /**
     * Event coordination configuration
     */
    @Data
    public static class CoordinationProperties {
        @Min(value = 1, message = "Event expiration must be at least 1 hour")
        private int eventExpirationHours = 24;

        @Min(value = 1, message = "Cleanup interval must be at least 1 minute")
        private int cleanupIntervalMinutes = 30;
    }

    /**
     * Performance targets (in milliseconds)
     */
    @Data
    public static class PerformanceProperties {
        @Min(value = 1, message = "Data fetch target must be at least 1 ms")
        private int dataFetchTarget = 800;

        @Min(value = 1, message = "HTML generation target must be at least 1 ms")
        private int htmlGenerationTarget = 2000;

        @Min(value = 1, message = "XBRL generation target must be at least 1 ms")
        private int xbrlGenerationTarget = 800;

        @Min(value = 1, message = "S3 upload target must be at least 1 ms")
        private int s3UploadTarget = 800;

        @Min(value = 1, message = "Total generation target must be at least 1 ms")
        private int totalGenerationTarget = 7000;

        @Min(value = 1, message = "File download timeout must be at least 1 ms")
        private int fileDownloadTimeout = 30000;
    }

    /**
     * Retry configuration
     */
    @Data
    public static class RetryProperties {
        @Min(value = 0, message = "Max retries must be non-negative")
        private int maxRetries = 3;

        @NotNull(message = "Backoff intervals must be specified")
        private List<Integer> backoffIntervalsSeconds = List.of(1, 2, 4, 8, 16);

        @NotNull(message = "Retryable exceptions must be specified")
        private List<String> retryableExceptions = List.of(
            "software.amazon.awssdk.services.s3.model.S3Exception",
            "java.io.IOException",
            "java.net.SocketTimeoutException"
        );

        @NotNull(message = "Non-retryable exceptions must be specified")
        private List<String> nonRetryableExceptions = List.of(
            "software.amazon.awssdk.services.s3.model.NoSuchBucketException",
            "software.amazon.awssdk.core.exception.SdkClientException"
        );
    }
}
