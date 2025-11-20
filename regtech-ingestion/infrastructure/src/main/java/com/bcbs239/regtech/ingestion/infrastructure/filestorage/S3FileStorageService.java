package com.bcbs239.regtech.ingestion.infrastructure.filestorage;

import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.ingestion.domain.batch.FileMetadata;
import com.bcbs239.regtech.ingestion.domain.batch.S3Reference;
import com.bcbs239.regtech.ingestion.domain.services.FileStorageService;
import com.bcbs239.regtech.core.infrastructure.filestorage.CoreS3FileStorageService;
import com.bcbs239.regtech.core.infrastructure.filestorage.CoreS3Reference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.InputStream;

/**
 * Adapter implementation of the FileStorageService that delegates to the
 * central CoreS3FileStorageService. Keeps ingestion domain types as API.
 */
@Service
@ConditionalOnProperty(name = "storage.type", havingValue = "s3", matchIfMissing = true)
public class S3FileStorageService implements FileStorageService {

    private static final Logger logger = LoggerFactory.getLogger(S3FileStorageService.class);

    private final CoreS3FileStorageService coreS3Service;

    public S3FileStorageService(CoreS3FileStorageService coreS3Service) {
        this.coreS3Service = coreS3Service;
    }

    @Override
    public Result<com.bcbs239.regtech.ingestion.domain.batch.S3Reference> storeFile(InputStream fileStream, FileMetadata fileMetadata,
                                                                                     String batchId, String bankId, int exposureCount) {
        // Delegate to core service
        Result<CoreS3Reference> coreResult = coreS3Service.storeFile(fileStream, fileMetadata.fileName(), batchId, bankId, exposureCount);
        if (coreResult.isFailure()) {
            // propagate failure (reuse the same ErrorDetail list)
            return Result.failure(coreResult.errors());
        }

        CoreS3Reference coreRef = coreResult.getValueOrThrow();
        // Map CoreS3Reference -> ingestion.domain.batch.S3Reference (record expects bucket,key,versionId,uri)
        S3Reference domainRef = new S3Reference(coreRef.bucket(), coreRef.key(), coreRef.versionId(), coreRef.uri());
        logger.info("Stored file {} for batch {} in S3 (bucket={}, key={})", fileMetadata.fileName(), batchId, coreRef.bucket(), coreRef.key());
        return Result.success(domainRef);
    }

    @Override
    public Result<Boolean> checkServiceHealth() {
        return coreS3Service.checkServiceHealth();
    }
}
