package com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects;

import lombok.NonNull;

import java.util.regex.Pattern;

/**
 * S3 URI value object with validation
 * Ensures URI follows s3://bucket/key format
 */
public record S3Uri(@NonNull String value) {
    
    private static final Pattern S3_URI_PATTERN = Pattern.compile("^s3://[a-z0-9][a-z0-9.-]{1,61}[a-z0-9]/.*$");
    
    /**
     * Compact constructor with validation
     */
    public S3Uri {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("S3 URI cannot be null or blank");
        }
        if (!S3_URI_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException(
                "Invalid S3 URI format. Expected s3://bucket/key, got: " + value
            );
        }
    }
    
    /**
     * Extract bucket name from URI
     */
    public String getBucket() {
        String withoutProtocol = value.substring(5); // Remove "s3://"
        int slashIndex = withoutProtocol.indexOf('/');
        return slashIndex > 0 ? withoutProtocol.substring(0, slashIndex) : withoutProtocol;
    }
    
    /**
     * Extract key from URI
     */
    public String getKey() {
        String withoutProtocol = value.substring(5); // Remove "s3://"
        int slashIndex = withoutProtocol.indexOf('/');
        return slashIndex > 0 ? withoutProtocol.substring(slashIndex + 1) : "";
    }
    
    @Override
    public String toString() {
        return value;
    }
}
