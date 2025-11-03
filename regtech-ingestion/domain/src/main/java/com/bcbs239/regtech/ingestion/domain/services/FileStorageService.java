package com.bcbs239.regtech.ingestion.domain.services;

import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.ingestion.domain.batch.FileMetadata;
import com.bcbs239.regtech.ingestion.domain.batch.S3Reference;

import java.io.InputStream;

/**
 * Domain service interface for file storage operations.
 * This interface abstracts the storage implementation details from the domain layer.
 */
public interface FileStorageService {
    
    /**
     * Stores a file with the given metadata and returns a reference to the stored file.
     * 
     * @param fileStream the file content as an input stream
     * @param fileMetadata metadata about the file including checksums
     * @param batchId the batch identifier
     * @param bankId the bank identifier
     * @param exposureCount number of exposures in the file
     * @return Result containing S3Reference on success or ErrorDetail on failure
     */
    Result<S3Reference> storeFile(InputStream fileStream, FileMetadata fileMetadata, 
                                 String batchId, String bankId, int exposureCount);
    
    /**
     * Checks if the storage service is available and healthy.
     * 
     * @return Result containing true if service is healthy, ErrorDetail otherwise
     */
    Result<Boolean> checkServiceHealth();
}