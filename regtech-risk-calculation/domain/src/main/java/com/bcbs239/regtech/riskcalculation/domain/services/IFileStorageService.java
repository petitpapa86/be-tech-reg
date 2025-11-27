package com.bcbs239.regtech.riskcalculation.domain.services;

import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.BatchId;
import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.FileStorageUri;

/**
 * Domain service interface for file storage operations in risk calculation module.
 * Abstracts storage implementation (S3 or local filesystem) from domain logic.
 */
public interface IFileStorageService {
    
    /**
     * Downloads file content from the given URI.
     * 
     * @param uri The file storage URI
     * @return Result containing file content as string or error details
     */
    Result<String> downloadFileContent(FileStorageUri uri);
    
    /**
     * Stores calculation results to file storage.
     * 
     * @param batchId The batch ID
     * @param content The content to store (JSON string)
     * @return Result containing the storage URI or error details
     */
    Result<FileStorageUri> storeCalculationResults(BatchId batchId, String content);
    
    /**
     * Checks if the storage service is available and healthy.
     * 
     * @return Result containing true if service is healthy, error details otherwise
     */
    Result<Boolean> checkServiceHealth();
}
