package com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects;

/**
 * URI reference to stored file (S3 or local filesystem)
 * Immutable value object that represents file storage location
 */
public record FileStorageUri(String uri) {
    
    public FileStorageUri {
        if (uri == null || uri.trim().isEmpty()) {
            throw new IllegalArgumentException("File storage URI cannot be null or empty");
        }
        // Normalize whitespace
        uri = uri.trim();
    }
    
    public static FileStorageUri of(String uri) {
        return new FileStorageUri(uri);
    }
    
    public boolean isS3Uri() {
        return uri.startsWith("s3://");
    }
    
    public boolean isLocalFileUri() {
        return uri.startsWith("file://");
    }
    
    public boolean isHttpUri() {
        return uri.startsWith("http://") || uri.startsWith("https://");
    }
}