package com.bcbs239.regtech.riskcalculation.domain.storage;

/**
 * Exception thrown when a requested file cannot be found in storage.
 * This indicates the file was deleted, moved, or the URI is invalid.
 * 
 * Requirement: 7.3 - Throw FileNotFoundException on download failures
 */
public class FileNotFoundException extends RuntimeException {
    
    private final String storageUri;
    
    public FileNotFoundException(String message) {
        super(message);
        this.storageUri = null;
    }
    
    public FileNotFoundException(String message, String storageUri) {
        super(message);
        this.storageUri = storageUri;
    }
    
    public FileNotFoundException(String message, String storageUri, Throwable cause) {
        super(message, cause);
        this.storageUri = storageUri;
    }
    
    public String getStorageUri() {
        return storageUri;
    }
}
