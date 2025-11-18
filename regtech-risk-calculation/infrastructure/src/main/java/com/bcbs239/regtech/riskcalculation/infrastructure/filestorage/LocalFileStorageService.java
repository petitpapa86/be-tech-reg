package com.bcbs239.regtech.riskcalculation.infrastructure.filestorage;

import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.BatchId;
import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.FileStorageUri;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Local filesystem implementation of file storage service.
 * Used in development profile for storing calculation results.
 */
@Service
@Profile("development")
@RequiredArgsConstructor
@Slf4j
public class LocalFileStorageService {
    
    private final ObjectMapper objectMapper;
    
    @Value("${risk-calculation.storage.local.base-path:./data/risk-calculation}")
    private String basePath;
    
    /**
     * Stores calculation results to local filesystem.
     * 
     * @param batchId The batch ID
     * @param content The content to store
     * @return Result containing the storage URI or error details
     */
    public Result<FileStorageUri> storeResults(BatchId batchId, Object content) {
        try {
            // Create directory if it doesn't exist
            Path directoryPath = Paths.get(basePath);
            if (!Files.exists(directoryPath)) {
                Files.createDirectories(directoryPath);
            }
            
            // Create file path
            String fileName = String.format("%s-results.json", batchId.value());
            Path filePath = directoryPath.resolve(fileName);
            
            // Write content to file
            objectMapper.writeValue(filePath.toFile(), content);
            
            // Generate URI
            String uri = String.format("file://%s", filePath.toAbsolutePath());
            
            log.info("Stored calculation results locally [batchId:{},path:{}]", 
                batchId.value(), filePath.toAbsolutePath());
            
            return Result.success(new FileStorageUri(uri));
            
        } catch (IOException e) {
            log.error("Failed to store results locally [batchId:{},error:{}]", 
                batchId.value(), e.getMessage(), e);
            
            return Result.failure(ErrorDetail.of(
                "LOCAL_STORAGE_ERROR",
                ErrorType.SYSTEM_ERROR,
                "Failed to store results to local filesystem: " + e.getMessage(),
                "file.storage.local.error"
            ));
        }
    }
    
    /**
     * Reads calculation results from local filesystem.
     * 
     * @param uri The file URI
     * @return Result containing the file content or error details
     */
    public Result<String> readResults(FileStorageUri uri) {
        try {
            // Extract file path from URI
            String filePath = uri.uri().replace("file://", "");
            Path path = Paths.get(filePath);
            
            // Read file content
            String content = Files.readString(path);
            
            log.debug("Read calculation results from local storage [uri:{}]", uri.uri());
            
            return Result.success(content);
            
        } catch (IOException e) {
            log.error("Failed to read results from local storage [uri:{},error:{}]", 
                uri.uri(), e.getMessage(), e);
            
            return Result.failure(ErrorDetail.of(
                "LOCAL_READ_ERROR",
                ErrorType.SYSTEM_ERROR,
                "Failed to read results from local filesystem: " + e.getMessage(),
                "file.storage.local.read.error"
            ));
        }
    }
}
