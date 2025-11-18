package com.bcbs239.regtech.riskcalculation.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Configuration properties for Risk Calculation module
 * Bound from application-risk-calculation.yml
 */
@Data
@ConfigurationProperties(prefix = "risk-calculation")
public class RiskCalculationProperties {

    private boolean enabled = true;
    private StorageProperties storage = new StorageProperties();
    private ProcessingProperties processing = new ProcessingProperties();
    private CurrencyProperties currency = new CurrencyProperties();
    private PerformanceProperties performance = new PerformanceProperties();
    private GeographicProperties geographic = new GeographicProperties();
    private ConcentrationProperties concentration = new ConcentrationProperties();

    @Data
    public static class StorageProperties {
        private String type = "s3"; // s3 or local
        private S3Properties s3 = new S3Properties();
        private LocalProperties local = new LocalProperties();

        @Data
        public static class S3Properties {
            private String bucket;
            private String region;
            private String prefix;
            private String accessKey;
            private String secretKey;
            private String endpoint;
            private String encryption = "AES256";
        }

        @Data
        public static class LocalProperties {
            private String basePath = "./data/risk-calculations";
            private boolean createDirectories = true;
        }
    }

    @Data
    public static class ProcessingProperties {
        private boolean asyncEnabled = true;
        private int threadPoolSize = 5;
        private int queueCapacity = 50;
        private int timeoutSeconds = 300;
    }

    @Data
    public static class CurrencyProperties {
        private String baseCurrency = "EUR";
        private boolean cacheEnabled = true;
        private int cacheTtl = 3600;
        private ProviderProperties provider = new ProviderProperties();

        @Data
        public static class ProviderProperties {
            private int timeout = 30000;
            private int retryAttempts = 3;
            private boolean mockEnabled = false;
        }
    }

    @Data
    public static class PerformanceProperties {
        private int maxConcurrentCalculations = 3;
        private boolean streamingParserEnabled = true;
        private int memoryThresholdMb = 512;
    }

    @Data
    public static class GeographicProperties {
        private String homeCountry = "IT";
        private List<String> euCountries = List.of(
            "AT", "BE", "BG", "HR", "CY", "CZ", "DK", "EE", "FI", "FR",
            "DE", "GR", "HU", "IE", "IT", "LV", "LT", "LU", "MT", "NL",
            "PL", "PT", "RO", "SK", "SI", "ES", "SE"
        );
    }

    @Data
    public static class ConcentrationProperties {
        private double highThreshold = 0.25;
        private double moderateThreshold = 0.15;
        private int precision = 4;
    }
}