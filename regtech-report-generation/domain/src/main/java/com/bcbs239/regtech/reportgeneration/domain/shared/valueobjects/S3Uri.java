package com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.NonNull;

import java.util.regex.Pattern;

/**
 * Storage URI value object with validation
 * Supports both S3 URIs (s3://bucket/key) and file URIs (file:///path)
 * for development and production environments
 */
public record S3Uri(@JsonValue @NonNull String value) {
    
    private static final Pattern S3_URI_PATTERN = Pattern.compile("^s3://[a-z0-9][a-z0-9.-]{1,61}[a-z0-9]/.*$");
    private static final Pattern FILE_URI_PATTERN = Pattern.compile("^file:///.*$");
    
    /**
     * Compact constructor with validation
     */
    public S3Uri {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Storage URI cannot be null or blank");
        }
        if (!S3_URI_PATTERN.matcher(value).matches() && !FILE_URI_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException(
                "Invalid storage URI format. Expected s3://bucket/key or file:///path, got: " + value
            );
        }
    }
    
    /**
     * Check if this is an S3 URI
     */
    public boolean isS3Uri() {
        return value.startsWith("s3://");
    }
    
    /**
     * Check if this is a file URI
     */
    public boolean isFileUri() {
        return value.startsWith("file://");
    }
    
    /**
     * Extract bucket name from S3 URI
     * @throws IllegalStateException if not an S3 URI
     */
    public String getBucket() {
        if (!isS3Uri()) {
            throw new IllegalStateException("getBucket() can only be called on S3 URIs");
        }
        String withoutProtocol = value.substring(5); // Remove "s3://"
        int slashIndex = withoutProtocol.indexOf('/');
        return slashIndex > 0 ? withoutProtocol.substring(0, slashIndex) : withoutProtocol;
    }
    
    /**
     * Extract key from S3 URI
     * @throws IllegalStateException if not an S3 URI
     */
    public String getKey() {
        if (!isS3Uri()) {
            throw new IllegalStateException("getKey() can only be called on S3 URIs");
        }
        String withoutProtocol = value.substring(5); // Remove "s3://"
        int slashIndex = withoutProtocol.indexOf('/');
        return slashIndex > 0 ? withoutProtocol.substring(slashIndex + 1) : "";
    }
    
    @Override
    public String toString() {
        return value;
    }
}
