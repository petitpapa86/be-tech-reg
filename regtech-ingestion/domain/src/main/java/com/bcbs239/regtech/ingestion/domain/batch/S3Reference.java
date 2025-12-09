package com.bcbs239.regtech.ingestion.domain.batch;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * Value object representing a reference to a file stored in S3.
 */
public record S3Reference(
    @JsonProperty("bucket") String bucket,
    @JsonProperty("key") String key,
    @JsonProperty("versionId") String versionId,
    @JsonProperty("uri") String uri
) {
    
    @JsonCreator
    public S3Reference {
        Objects.requireNonNull(bucket, "S3 bucket cannot be null");
        Objects.requireNonNull(key, "S3 key cannot be null");
        Objects.requireNonNull(versionId, "S3 version ID cannot be null");
        Objects.requireNonNull(uri, "S3 URI cannot be null");
        
        if (bucket.trim().isEmpty()) {
            throw new IllegalArgumentException("S3 bucket cannot be empty");
        }
        if (key.trim().isEmpty()) {
            throw new IllegalArgumentException("S3 key cannot be empty");
        }
        if (versionId.trim().isEmpty()) {
            throw new IllegalArgumentException("S3 version ID cannot be empty");
        }
        if (uri.trim().isEmpty()) {
            throw new IllegalArgumentException("S3 URI cannot be empty");
        }
    }
    
    /**
     * Create an S3Reference with the full URI constructed from bucket and key.
     */
    public static S3Reference of(String bucket, String key, String versionId) {
        String uri = String.format("s3://%s/%s", bucket, key);
        return new S3Reference(bucket, key, versionId, uri);
    }
}

