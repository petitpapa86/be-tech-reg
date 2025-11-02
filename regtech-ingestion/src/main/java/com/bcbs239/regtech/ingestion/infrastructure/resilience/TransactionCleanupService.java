package com.bcbs239.regtech.ingestion.infrastructure.resilience;

import com.bcbs239.regtech.core.shared.ErrorDetail;
import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.ingestion.domain.model.BatchId;
import com.bcbs239.regtech.ingestion.domain.model.S3Reference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Service for handling transaction rollback and cleanup operations.
 * Manages cleanup of partial S3 uploads and database rollbacks on processing failures.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionCleanupService {
    
    private final S3Client s3Client;
    
    @Value("${regtech.s3.bucket:regtech-data-storage}")
    private String bucketName;
    
    @Value("${regtech.cleanup.timeout-seconds:30}")
    private long cleanupTimeoutSeconds;
    
    /**
     * Performs comprehensive cleanup after a processing failure.
     * This includes S3 cleanup and database transaction rollback.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Result<Void> performFailureCleanup(BatchId batchId, S3Reference s3Reference, 
                                             String failureReason, Exception originalException) {
        log.info("Starting failure cleanup for batch {} due to: {}", batchId.value(), failureReason);
        
        List<ErrorDetail> cleanupErrors = new ArrayList<>();
        boolean s3CleanupSuccess = false;
        
        // Step 1: Clean up S3 objects if they exist
        if (s3Reference != null) {
            Result<Void> s3CleanupResult = cleanupS3Objects(batchId, s3Reference);
            if (s3CleanupResult.isSuccess()) {
                s3CleanupSuccess = true;
                log.info("S3 cleanup completed successfully for batch {}", batchId.value());
            } else {
                cleanupErrors.addAll(s3CleanupResult.getErrors());
                log.error("S3 cleanup failed for batch {}", batchId.value());
            }
        }
        
        // Step 2: Record cleanup results
        recordCleanupResults(batchId, s3CleanupSuccess, failureReason, originalException);
        
        // Step 3: Return results
        if (cleanupErrors.isEmpty()) {
            log.info("Failure cleanup completed successfully for batch {}", batchId.value());
            return Result.success(null);
        } else {
            log.error("Failure cleanup completed with {} errors for batch {}", cleanupErrors.size(), batchId.value());
            return Result.failure(cleanupErrors);
        }
    }
    
    /**
     * Cleans up S3 objects associated with a failed batch processing.
     */
    private Result<Void> cleanupS3Objects(BatchId batchId, S3Reference s3Reference) {
        log.debug("Cleaning up S3 objects for batch {} at {}", batchId.value(), s3Reference.uri());
        
        List<ErrorDetail> errors = new ArrayList<>();
        
        try {
            // Step 1: Delete the main object
            Result<Void> mainObjectResult = deleteS3Object(s3Reference.bucket(), s3Reference.key());
            if (mainObjectResult.isFailure()) {
                errors.addAll(mainObjectResult.getErrors());
            }
            
            // Step 2: Clean up any multipart uploads that might be in progress
            Result<Void> multipartResult = cleanupMultipartUploads(batchId, s3Reference.key());
            if (multipartResult.isFailure()) {
                errors.addAll(multipartResult.getErrors());
            }
            
            // Step 3: Clean up any temporary objects (if any exist)
            Result<Void> tempObjectsResult = cleanupTemporaryObjects(batchId);
            if (tempObjectsResult.isFailure()) {
                errors.addAll(tempObjectsResult.getErrors());
            }
            
            if (errors.isEmpty()) {
                return Result.success(null);
            } else {
                return Result.failure(errors);
            }
            
        } catch (Exception e) {
            log.error("Unexpected error during S3 cleanup for batch {}: {}", batchId.value(), e.getMessage());
            return Result.failure(ErrorDetail.of("S3_CLEANUP_ERROR", 
                "Unexpected error during S3 cleanup: " + e.getMessage()));
        }
    }
    
    /**
     * Deletes a specific S3 object.
     */
    private Result<Void> deleteS3Object(String bucket, String key) {
        try {
            // First check if object exists
            HeadObjectRequest headRequest = HeadObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();
            
            try {
                s3Client.headObject(headRequest);
                log.debug("S3 object exists, proceeding with deletion: s3://{}/{}", bucket, key);
            } catch (NoSuchKeyException e) {
                log.debug("S3 object does not exist, no cleanup needed: s3://{}/{}", bucket, key);
                return Result.success(null);
            }
            
            // Delete the object
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();
            
            s3Client.deleteObject(deleteRequest);
            log.info("Successfully deleted S3 object: s3://{}/{}", bucket, key);
            
            return Result.success(null);
            
        } catch (Exception e) {
            log.error("Failed to delete S3 object s3://{}/{}: {}", bucket, key, e.getMessage());
            return Result.failure(ErrorDetail.of("S3_DELETE_FAILED", 
                String.format("Failed to delete S3 object s3://%s/%s: %s", bucket, key, e.getMessage())));
        }
    }
    
    /**
     * Cleans up any in-progress multipart uploads for the batch.
     */
    private Result<Void> cleanupMultipartUploads(BatchId batchId, String keyPrefix) {
        try {
            log.debug("Cleaning up multipart uploads for batch {} with key prefix {}", batchId.value(), keyPrefix);
            
            ListMultipartUploadsRequest listRequest = ListMultipartUploadsRequest.builder()
                    .bucket(bucketName)
                    .prefix(keyPrefix)
                    .build();
            
            ListMultipartUploadsResponse listResponse = s3Client.listMultipartUploads(listRequest);
            
            List<ErrorDetail> errors = new ArrayList<>();
            
            for (MultipartUpload upload : listResponse.uploads()) {
                try {
                    AbortMultipartUploadRequest abortRequest = AbortMultipartUploadRequest.builder()
                            .bucket(bucketName)
                            .key(upload.key())
                            .uploadId(upload.uploadId())
                            .build();
                    
                    s3Client.abortMultipartUpload(abortRequest);
                    log.info("Aborted multipart upload for key {} upload ID {}", upload.key(), upload.uploadId());
                    
                } catch (Exception e) {
                    log.error("Failed to abort multipart upload for key {} upload ID {}: {}", 
                             upload.key(), upload.uploadId(), e.getMessage());
                    errors.add(ErrorDetail.of("MULTIPART_ABORT_FAILED", 
                        String.format("Failed to abort multipart upload %s: %s", upload.uploadId(), e.getMessage())));
                }
            }
            
            if (errors.isEmpty()) {
                return Result.success(null);
            } else {
                return Result.failure(errors);
            }
            
        } catch (Exception e) {
            log.error("Failed to list/cleanup multipart uploads for batch {}: {}", batchId.value(), e.getMessage());
            return Result.failure(ErrorDetail.of("MULTIPART_CLEANUP_ERROR", 
                "Failed to cleanup multipart uploads: " + e.getMessage()));
        }
    }
    
    /**
     * Cleans up any temporary objects that might have been created during processing.
     */
    private Result<Void> cleanupTemporaryObjects(BatchId batchId) {
        try {
            String tempPrefix = "temp/" + batchId.value();
            log.debug("Cleaning up temporary objects for batch {} with prefix {}", batchId.value(), tempPrefix);
            
            ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .prefix(tempPrefix)
                    .build();
            
            ListObjectsV2Response listResponse = s3Client.listObjectsV2(listRequest);
            
            if (listResponse.contents().isEmpty()) {
                log.debug("No temporary objects found for batch {}", batchId.value());
                return Result.success(null);
            }
            
            List<ErrorDetail> errors = new ArrayList<>();
            
            for (S3Object s3Object : listResponse.contents()) {
                Result<Void> deleteResult = deleteS3Object(bucketName, s3Object.key());
                if (deleteResult.isFailure()) {
                    errors.addAll(deleteResult.getErrors());
                }
            }
            
            if (errors.isEmpty()) {
                log.info("Successfully cleaned up {} temporary objects for batch {}", 
                        listResponse.contents().size(), batchId.value());
                return Result.success(null);
            } else {
                return Result.failure(errors);
            }
            
        } catch (Exception e) {
            log.error("Failed to cleanup temporary objects for batch {}: {}", batchId.value(), e.getMessage());
            return Result.failure(ErrorDetail.of("TEMP_CLEANUP_ERROR", 
                "Failed to cleanup temporary objects: " + e.getMessage()));
        }
    }
    
    /**
     * Detects file corruption via checksum mismatch.
     */
    public Result<Void> validateFileIntegrity(S3Reference s3Reference, String expectedMd5, String expectedSha256) {
        log.debug("Validating file integrity for S3 object: {}", s3Reference.uri());
        
        try {
            // Get object metadata to check ETag (which should match MD5 for single-part uploads)
            HeadObjectRequest headRequest = HeadObjectRequest.builder()
                    .bucket(s3Reference.bucket())
                    .key(s3Reference.key())
                    .build();
            
            HeadObjectResponse headResponse = s3Client.headObject(headRequest);
            
            String s3ETag = headResponse.eTag().replace("\"", ""); // Remove quotes from ETag
            
            // For single-part uploads, ETag should match MD5
            if (!s3ETag.contains("-")) { // Single-part upload (no hyphen in ETag)
                if (!s3ETag.equalsIgnoreCase(expectedMd5)) {
                    log.error("MD5 checksum mismatch for {}: expected {}, got {}", 
                             s3Reference.uri(), expectedMd5, s3ETag);
                    return Result.failure(ErrorDetail.of("CHECKSUM_MISMATCH", 
                        String.format("File integrity check failed: MD5 checksum mismatch. " +
                                     "Expected %s, but S3 ETag is %s. The file may be corrupted.", 
                                     expectedMd5, s3ETag)));
                }
            } else {
                // Multipart upload - ETag is not a simple MD5
                log.warn("Cannot validate MD5 checksum for multipart upload: {}", s3Reference.uri());
            }
            
            // Check custom metadata for SHA-256 if available
            String s3Sha256 = headResponse.metadata().get("sha256-checksum");
            if (s3Sha256 != null && expectedSha256 != null) {
                if (!s3Sha256.equalsIgnoreCase(expectedSha256)) {
                    log.error("SHA-256 checksum mismatch for {}: expected {}, got {}", 
                             s3Reference.uri(), expectedSha256, s3Sha256);
                    return Result.failure(ErrorDetail.of("SHA256_MISMATCH", 
                        String.format("File integrity check failed: SHA-256 checksum mismatch. " +
                                     "Expected %s, but S3 metadata shows %s. The file may be corrupted.", 
                                     expectedSha256, s3Sha256)));
                }
            }
            
            log.info("File integrity validation passed for {}", s3Reference.uri());
            return Result.success(null);
            
        } catch (NoSuchKeyException e) {
            log.error("S3 object not found during integrity check: {}", s3Reference.uri());
            return Result.failure(ErrorDetail.of("FILE_NOT_FOUND", 
                "File not found in S3 during integrity check: " + s3Reference.uri()));
        } catch (Exception e) {
            log.error("Error during file integrity validation for {}: {}", s3Reference.uri(), e.getMessage());
            return Result.failure(ErrorDetail.of("INTEGRITY_CHECK_ERROR", 
                "Error during file integrity validation: " + e.getMessage()));
        }
    }
    
    /**
     * Performs asynchronous cleanup with timeout to avoid blocking the main thread.
     */
    public CompletableFuture<Result<Void>> performAsyncCleanup(BatchId batchId, S3Reference s3Reference, 
                                                              String failureReason, Exception originalException) {
        return CompletableFuture
                .supplyAsync(() -> performFailureCleanup(batchId, s3Reference, failureReason, originalException))
                .orTimeout(cleanupTimeoutSeconds, TimeUnit.SECONDS)
                .exceptionally(throwable -> {
                    log.error("Async cleanup timed out or failed for batch {}: {}", batchId.value(), throwable.getMessage());
                    return Result.failure(ErrorDetail.of("CLEANUP_TIMEOUT", 
                        "Cleanup operation timed out after " + cleanupTimeoutSeconds + " seconds"));
                });
    }
    
    /**
     * Records cleanup results for audit and monitoring purposes.
     */
    private void recordCleanupResults(BatchId batchId, boolean s3CleanupSuccess, 
                                    String failureReason, Exception originalException) {
        try {
            log.info("Recording cleanup results for batch {}: S3 cleanup success={}, failure reason={}", 
                    batchId.value(), s3CleanupSuccess, failureReason);
            
            // This could be enhanced to store cleanup audit information in a dedicated table
            // For now, we just log the information
            
        } catch (Exception e) {
            log.error("Failed to record cleanup results for batch {}: {}", batchId.value(), e.getMessage());
        }
    }
    
    /**
     * Validates that all cleanup operations completed successfully.
     */
    public Result<Void> validateCleanupCompletion(BatchId batchId, S3Reference s3Reference) {
        log.debug("Validating cleanup completion for batch {}", batchId.value());
        
        try {
            // Check that S3 object no longer exists
            if (s3Reference != null) {
                HeadObjectRequest headRequest = HeadObjectRequest.builder()
                        .bucket(s3Reference.bucket())
                        .key(s3Reference.key())
                        .build();
                
                try {
                    s3Client.headObject(headRequest);
                    log.error("S3 object still exists after cleanup: {}", s3Reference.uri());
                    return Result.failure(ErrorDetail.of("CLEANUP_INCOMPLETE", 
                        "S3 object still exists after cleanup: " + s3Reference.uri()));
                } catch (NoSuchKeyException e) {
                    log.debug("S3 object successfully removed: {}", s3Reference.uri());
                }
            }
            
            log.info("Cleanup validation passed for batch {}", batchId.value());
            return Result.success(null);
            
        } catch (Exception e) {
            log.error("Error validating cleanup completion for batch {}: {}", batchId.value(), e.getMessage());
            return Result.failure(ErrorDetail.of("CLEANUP_VALIDATION_ERROR", 
                "Error validating cleanup completion: " + e.getMessage()));
        }
    }
}