package com.bcbs239.regtech.ingestion.infrastructure.service;

import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.ingestion.domain.model.FileMetadata;
import com.bcbs239.regtech.ingestion.domain.model.S3Reference;
import com.bcbs239.regtech.ingestion.domain.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.InputStream;

/**
 * Implementation of FileStorageService using S3 with retry logic.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class S3FileStorageServiceImpl implements FileStorageService {
    
    private final S3StorageServiceWithRetry s3StorageServiceWithRetry;
    
    @Override
    public Result<S3Reference> storeFile(InputStream fileStream, FileMetadata fileMetadata, 
                                        String batchId, String bankId, int exposureCount) {
        log.info("Storing file for batch {} with {} exposures", batchId, exposureCount);
        
        return s3StorageServiceWithRetry.storeFileWithRetry(
            fileStream, fileMetadata, batchId, bankId, exposureCount);
    }
    
    @Override
    public Result<Boolean> checkServiceHealth() {
        return s3StorageServiceWithRetry.checkS3ServiceHealth();
    }
}