package com.bcbs239.regtech.core.infrastructure.s3;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Shared configuration properties for S3 across modules.
 */
@ConfigurationProperties(prefix = "ingestion.s3")
public class S3Properties {

    private String bucket = "regtech-data-storage";
    private String region = "us-east-1";
    private String prefix = "raw/";
    private String accessKey = "";
    private String secretKey = "";
    private String endpoint = "";
    private String kmsKeyId = "";

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getKmsKeyId() {
        return kmsKeyId;
    }

    public void setKmsKeyId(String kmsKeyId) {
        this.kmsKeyId = kmsKeyId;
    }
}

