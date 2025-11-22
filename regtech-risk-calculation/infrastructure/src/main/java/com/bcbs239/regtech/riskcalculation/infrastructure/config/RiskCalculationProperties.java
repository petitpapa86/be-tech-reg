package com.bcbs239.regtech.riskcalculation.infrastructure.config;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * Configuration properties for Risk Calculation module
 * Bound from application-risk-calculation.yml
 * Requirements: 6.2, 6.5, 10.3
 */
@Data
@Validated
@ConfigurationProperties(prefix = "risk-calculation")
public class RiskCalculationProperties {

    @NotNull(message = "Risk calculation enabled flag must be specified")
    private Boolean enabled = true;

    @NotNull(message = "Storage configuration must be specified")
    private StorageProperties storage = new StorageProperties();

    @NotNull(message = "Async configuration must be specified")
    private AsyncProperties async = new AsyncProperties();

    @NotNull(message = "Processing configuration must be specified")
    private ProcessingProperties processing = new ProcessingProperties();

    @NotNull(message = "Currency configuration must be specified")
    private CurrencyProperties currency = new CurrencyProperties();

    @NotNull(message = "Performance configuration must be specified")
    private PerformanceProperties performance = new PerformanceProperties();

    @NotNull(message = "Geographic configuration must be specified")
    private GeographicProperties geographic = new GeographicProperties();

    @NotNull(message = "Concentration configuration must be specified")
    private ConcentrationProperties concentration = new ConcentrationProperties();

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
            private String basePath = "./data/risk-calculations";

            private boolean createDirectories = true;
        }
    }

    /**
     * Async thread pool configuration
     * Purpose: Handles async risk calculation and aggregation operations
     */
    @Data
    public static class AsyncProperties {
        private boolean enabled = true;

        @Min(value = 1, message = "Core pool size must be at least 1")
        private int corePoolSize = 5;

        @Min(value = 1, message = "Max pool size must be at least 1")
        private int maxPoolSize = 10;

        @Min(value = 0, message = "Queue capacity must be non-negative")
        private int queueCapacity = 50;

        @NotBlank(message = "Thread name prefix must be specified")
        private String threadNamePrefix = "risk-calc-async-";

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
        private int threadPoolSize = 5;

        @Min(value = 0, message = "Queue capacity must be non-negative")
        private int queueCapacity = 50;

        @Min(value = 1, message = "Timeout seconds must be at least 1")
        private int timeoutSeconds = 300;
    }

    /**
     * Currency conversion configuration
     */
    @Data
    public static class CurrencyProperties {
        @NotBlank(message = "Base currency must be specified")
        private String baseCurrency = "EUR";

        private boolean cacheEnabled = true;

        @Min(value = 0, message = "Cache TTL must be non-negative")
        private int cacheTtl = 3600;

        @NotNull(message = "Provider configuration must be specified")
        private ProviderProperties provider = new ProviderProperties();

        @Data
        public static class ProviderProperties {
            @Min(value = 1, message = "Timeout must be at least 1 millisecond")
            private int timeout = 30000;

            @Min(value = 0, message = "Retry attempts must be non-negative")
            private int retryAttempts = 3;

            private boolean mockEnabled = false;
        }
    }

    /**
     * Performance optimization settings
     */
    @Data
    public static class PerformanceProperties {
        @Min(value = 1, message = "Max concurrent calculations must be at least 1")
        private int maxConcurrentCalculations = 3;

        private boolean streamingParserEnabled = true;

        @Min(value = 1, message = "Memory threshold must be at least 1 MB")
        private int memoryThresholdMb = 512;
    }

    /**
     * Geographic classification configuration
     */
    @Data
    public static class GeographicProperties {
        @NotBlank(message = "Home country must be specified")
        private String homeCountry = "IT";

        @NotNull(message = "EU countries list must be specified")
        private List<String> euCountries = List.of(
            "AT", "BE", "BG", "HR", "CY", "CZ", "DK", "EE", "FI", "FR",
            "DE", "GR", "HU", "IE", "IT", "LV", "LT", "LU", "MT", "NL",
            "PL", "PT", "RO", "SK", "SI", "ES", "SE"
        );
    }

    /**
     * Concentration thresholds configuration
     */
    @Data
    public static class ConcentrationProperties {
        @DecimalMin(value = "0.0", message = "High threshold must be non-negative")
        @DecimalMax(value = "1.0", message = "High threshold must not exceed 1.0")
        private double highThreshold = 0.25;

        @DecimalMin(value = "0.0", message = "Moderate threshold must be non-negative")
        @DecimalMax(value = "1.0", message = "Moderate threshold must not exceed 1.0")
        private double moderateThreshold = 0.15;

        @Min(value = 0, message = "Precision must be non-negative")
        private int precision = 4;
    }
}