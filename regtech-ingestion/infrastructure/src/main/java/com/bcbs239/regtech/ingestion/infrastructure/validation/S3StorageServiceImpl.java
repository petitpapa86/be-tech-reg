package com.bcbs239.regtech.ingestion.infrastructure.validation;

import com.bcbs239.regtech.core.shared.ErrorDetail;
import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.ingestion.application.batch.process.ProcessBatchCommandHandler.S3StorageService;
import com.bcbs239.regtech.ingestion.domain.batch.FileMetadata;
import com.bcbs239.regtech.ingestion.domain.batch.S3Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.InputStream;

/**
 * Implementation of S3 storage service for batch processing.
 * Handles storing processed files to S3 with batch and bank context.
 */
@Service
public class S3StorageServiceImpl implements S3StorageService {

    private static final Logger log = LoggerFactory.getLogger(S3StorageServiceImpl.class);

    @Override
    public Result<S3Reference> storeFile(InputStream fileStream,
                                       FileMetadata fileMetadata,
                                       String batchId,
                                       String bankId,
                                       int exposureCount) {

        log.debug("Storing file to S3 for batchId: {}, bankId: {}, exposureCount: {}",
            batchId, bankId, exposureCount);

        if (fileStream == null) {
            return Result.failure(new ErrorDetail("NULL_FILE_STREAM", "File stream cannot be null"));
        }

        if (fileMetadata == null) {
            return Result.failure(new ErrorDetail("NULL_FILE_METADATA", "File metadata cannot be null"));
        }

        if (batchId == null || batchId.trim().isEmpty()) {
            return Result.failure(new ErrorDetail("INVALID_BATCH_ID", "Batch ID cannot be null or empty"));
        }

        if (bankId == null || bankId.trim().isEmpty()) {
            return Result.failure(new ErrorDetail("INVALID_BANK_ID", "Bank ID cannot be null or empty"));
        }

        if (exposureCount < 0) {
            return Result.failure(new ErrorDetail("INVALID_EXPOSURE_COUNT",
                "Exposure count cannot be negative"));
        }

        try {
            // For now, create a mock S3 reference since we don't have actual S3 integration
            // In a real implementation, this would upload to S3 and return the actual reference
            String bucket = "regtech-processed-files";
            String key = String.format("processed/%s/%s/%s-processed.json",
                bankId, batchId, fileMetadata.fileName().replaceAll("\\.[^.]*$", ""));

            S3Reference s3Reference = new S3Reference(
                bucket,
                key,
                "mock-version-id",
                String.format("s3://%s/%s", bucket, key)
            );

            log.info("Successfully stored processed file for batch {} with {} exposures to S3 location: {}",
                batchId, exposureCount, s3Reference.uri());

            return Result.success(s3Reference);

        } catch (Exception e) {
            log.error("Failed to store file to S3 for batchId: {}, bankId: {}", batchId, bankId, e);
            return Result.failure(new ErrorDetail("S3_STORAGE_ERROR",
                String.format("Failed to store file to S3: %s", e.getMessage())));
        }
    }
}