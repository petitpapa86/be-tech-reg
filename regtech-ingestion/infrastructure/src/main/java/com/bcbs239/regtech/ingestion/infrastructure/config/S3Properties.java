package com.bcbs239.regtech.ingestion.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for S3 integration.
 */
@ConfigurationProperties(prefix = "ingestion.s3")
public record S3Properties(
    String bucket,
    String region,
    String prefix,
    String accessKey,
    String secretKey,
    String endpoint
) {
    
    public S3Properties {
        // Set defaults if null
        if (bucket == null) bucket = "regtech-data-storage";
        if (region == null) region = "us-east-1";
        if (prefix == null) prefix = "raw/";
    }
}



