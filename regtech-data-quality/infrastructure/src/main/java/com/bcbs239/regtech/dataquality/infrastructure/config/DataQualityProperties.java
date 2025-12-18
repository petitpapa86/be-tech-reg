package com.bcbs239.regtech.dataquality.infrastructure.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for Data Quality module
 * Bound from application-data-quality.yml
 * Requirements: 6.2, 6.5, 10.3
 */
@Data
@Validated
@ConfigurationProperties(prefix = "data-quality")
public class DataQualityProperties {

    @NotNull(message = "Data quality enabled flag must be specified")
    private Boolean enabled = true;

    @NotNull(message = "Storage configuration must be specified")
    private StorageProperties storage = new StorageProperties();

    @NotNull(message = "Async configuration must be specified")
    private AsyncProperties async = new AsyncProperties();

    @NotNull(message = "Rules engine configuration must be specified")
    private RulesEngineProperties rulesEngine = new RulesEngineProperties();

    private RulesMigrationProperties rulesMigration = new RulesMigrationProperties();

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
            private String basePath = "./data/quality";

            private boolean createDirectories = true;
        }
    }

    /**
     * Async thread pool configuration
     * Purpose: Handles async quality validation operations
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
        private String threadNamePrefix = "data-quality-async-";

        @Min(value = 0, message = "Await termination seconds must be non-negative")
        private int awaitTerminationSeconds = 60;
    }

    /**
     * Rules engine configuration
     * Requirement 7.1: Rules Engine must be explicitly enabled (no default to false)
     */
    @Data
    public static class RulesEngineProperties {
        /**
         * Enable/disable Rules Engine.
         * REQUIRED: Must be explicitly set to true for validation to work.
         * Requirement 7.1: No default to false - must be explicitly configured.
         */
        @NotNull(message = "Rules Engine enabled flag must be explicitly specified (true or false)")
        private Boolean enabled;
        
        /**
         * Enable/disable in-memory rule caching.
         * Default: true (recommended for performance)
         * Requirement 7.2: Cache configuration
         */
        private boolean cacheEnabled = true;

        /**
         * Cache time-to-live in seconds.
         * Rules are reloaded from database after this period.
         * Requirement 7.2: Cache TTL configuration
         */
        @Min(value = 0, message = "Cache TTL must be non-negative")
        private int cacheTtl = 300; // 5 minutes in seconds

        /**
         * Enable parallel rule execution (future feature).
         * Currently not implemented - rules execute sequentially.
         */
        private boolean parallelExecution = false;

        /**
         * Enable/disable best-effort audit persistence inside the rules engine during rule execution.
         *
         * <p>When running batch validation that persists results afterward (recommended), set this to false
         * to avoid per-rule/per-exposure database writes and REQUIRES_NEW transactions.</p>
         */
        private boolean auditPersistenceEnabled = false;
        
        /**
         * Logging configuration for Rules Engine.
         * Requirement 7.4: Logging configuration
         */
        @NotNull(message = "Logging configuration must be specified")
        private LoggingProperties logging = new LoggingProperties();
        
        /**
         * Performance thresholds for Rules Engine.
         * Requirement 7.3: Performance configuration
         */
        @NotNull(message = "Performance configuration must be specified")
        private PerformanceProperties performance = new PerformanceProperties();
        
        /**
         * Logging configuration for Rules Engine
         * Requirements: 6.1, 6.2, 6.3, 6.4
         */
        @Data
        public static class LoggingProperties {
            private boolean logExecutions = true;
            private boolean logViolations = true;
            private boolean logSummary = true;
        }
        
        /**
         * Performance thresholds for Rules Engine
         * Requirement: 6.5
         */
        @Data
        public static class PerformanceProperties {
            @Min(value = 0, message = "Warn threshold must be non-negative")
            private int warnThresholdMs = 100;
            
            @Min(value = 0, message = "Max execution time must be non-negative")
            private int maxExecutionTimeMs = 5000;
        }
    }

    /**
     * Rules migration configuration
     */
    @Data
    public static class RulesMigrationProperties {
        private boolean enabled = true;
    }
}
