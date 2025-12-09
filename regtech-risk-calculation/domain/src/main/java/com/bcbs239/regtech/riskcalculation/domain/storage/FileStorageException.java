package com.bcbs239.regtech.riskcalculation.domain.storage;

/**
 * Exception thrown when file storage operations fail.
 * This includes upload failures, connection issues, permission problems, etc.
 * 
 * Requirement: 7.2 - Throw FileStorageException on upload failures
 */
public class FileStorageException extends RuntimeException {
    
    private final String storageUri;
    private final String operation;
    
    public FileStorageException(String message) {
        super(message);
        this.storageUri = null;
        this.operation = null;
    }
    
    public FileStorageException(String message, Throwable cause) {
        super(message, cause);
        this.storageUri = null;
        this.operation = null;
    }
    
    public FileStorageException(String message, String storageUri, String operation, Throwable cause) {
        super(message, cause);
        this.storageUri = storageUri;
        this.operation = operation;
    }
    
    public String getStorageUri() {
        return storageUri;
    }
    
    public String getOperation() {
        return operation;
    }
}
