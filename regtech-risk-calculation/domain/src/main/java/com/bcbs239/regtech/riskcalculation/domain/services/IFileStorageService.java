package com.bcbs239.regtech.riskcalculation.domain.services;

import com.bcbs239.regtech.core.domain.shared.Result;

/**
 * Domain service interface for file storage operations.
 * This interface defines the contract for storing and retrieving files.
 */
public interface IFileStorageService {
    
    /**
     * Stores a file with the given content.
     *
     * @param fileName The name of the file to store
     * @param content The content to store
     * @return Result containing the storage URI or error
     */
    Result<String> storeFile(String fileName, String content);
    
    /**
     * Retrieves a file by its storage URI.
     *
     * @param storageUri The URI of the file to retrieve
     * @return Result containing the file content or error
     */
    Result<String> retrieveFile(String storageUri);
    
    /**
     * Deletes a file by its storage URI.
     *
     * @param storageUri The URI of the file to delete
     * @return Result indicating success or error
     */
    Result<Void> deleteFile(String storageUri);
}
