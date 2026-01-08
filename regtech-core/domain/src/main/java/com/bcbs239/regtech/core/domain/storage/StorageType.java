package com.bcbs239.regtech.core.domain.storage;

/**
 * Supported storage backend types
 */
public enum StorageType {
    /**
     * Amazon S3 storage (s3://bucket/key)
     */
    S3,
    
    /**
     * Local filesystem with absolute path (file:///absolute/path)
     */
    LOCAL_ABSOLUTE,
    
    /**
     * Local filesystem with relative path (relative/path or ./relative/path)
     */
    LOCAL_RELATIVE,
    
    /**
     * In-memory storage (for testing)
     */
    MEMORY
}
