package com.bcbs239.regtech.ingestion.infrastructure.service;

import com.bcbs239.regtech.core.shared.ErrorDetail;
import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.ingestion.domain.model.FileMetadata;
import com.bcbs239.regtech.ingestion.domain.model.S3Reference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.InputStream;
import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

/**
 * S3 Storage Service with retry logic and error handling.
 * Implements exponential backoff retry strategy for S3 operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class S3StorageServiceWithRetry {
    
    private final S3StorageService s3StorageService;
    private final S3Client s3Client;
    
    @Value("${regtech.s3.bucket:regtech-data-storage}")
    private String bucketName;
    
    @Value("${regtech.s3.retry.max-attempts:3}")
    private int maxRetryAttempts;
    
    @Value("${regtech.s3.retry.base-delay-ms:1000}")
    private long baseDelayMs;
    
    @Value("${regtech.s3.lifecycle.glacier-transition-days:180}")
    private int glacierTransitionDays;
    
    /**
     * Stores a file in S3 with retry logic and exponential backoff.
     */
    public Result<S3Reference> storeFileWithRetry(InputStream fileStream, FileMetadata fileMetadata, 
                                                 String batchId, String bankId, int exposureCount) {
        
        Exception lastException = null;
        
        for (int attempt = 1; attempt <= maxRetryAttempts; attempt++) {
            try {
                log.info("S3 upload attempt {} of {} for batch {}", attempt, maxRetryAttempts, batchId);
                
                // Reset stream for retry attempts (this assumes the stream can be reset)
                // In production, you might need to buffer the content or use a different approach
                if (attempt > 1) {
                    try {
                        fileStream.reset();
                    } catch (Exception e) {
                        log.warn("Cannot reset input stream for retry attempt {}, this may cause issues", attempt);
                    }
                }
                
                Result<S3Reference> result = s3StorageService.storeFile(
                    fileStream, fileMetadata, batchId, bankId, exposureCount);
                
                if (result.isSuccess()) {
                    log.info("S3 upload succeeded on attempt {} for batch {}", attempt, batchId);
                    
                    // Set up lifecycle policy after successful upload
                    setupLifecyclePolicy();
                    
                    return result;
                }
                
                // If it's a validation error (checksum mismatch, etc.), don't retry
                if (result.getError().isPresent()) {
                    ErrorDetail error = result.getError().get();
                    if (isNonRetryableError(error)) {
                        log.error("Non-retryable error for batch {}: {}", batchId, error.getMessage());
                        return result;
                    }
                }
                
                lastException = new RuntimeException(result.getError()
                    .map(ErrorDetail::getMessage)
                    .orElse("Unknown error"));
                
            } catch (Exception e) {
                lastException = e;
                log.warn("S3 upload attempt {} failed for batch {}: {}", 
                        attempt, batchId, e.getMessage());
                
                // Don't retry for certain types of errors
                if (isNonRetryableException(e)) {
                    log.error("Non-retryable exception for batch {}: {}", batchId, e.getMessage());
                    return Result.failure(ErrorDetail.of("S3_UPLOAD_FAILED", 
                        "S3 upload failed with non-retryable error: " + e.getMessage()));
                }
            }
            
            // Wait before retry (except for the last attempt)
            if (attempt < maxRetryAttempts) {
                long delayMs = calculateBackoffDelay(attempt);
                log.info("Waiting {}ms before retry attempt {} for batch {}", 
                        delayMs, attempt + 1, batchId);
                
                try {
                    Thread.sleep(delayMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return Result.failure(ErrorDetail.of("S3_UPLOAD_INTERRUPTED", 
                        "S3 upload was interrupted"));
                }
            }
        }
        
        // All retry attempts failed
        log.error("All {} retry attempts failed for batch {}", maxRetryAttempts, batchId);
        return Result.failure(ErrorDetail.of("S3_UPLOAD_FAILED_AFTER_RETRIES", 
            String.format("S3 upload failed after %d attempts. Last error: %s", 
                         maxRetryAttempts, 
                         lastException != null ? lastException.getMessage() : "Unknown error")));
    }
    
    /**
     * Sets up lifecycle policy for the S3 bucket to transition objects to Glacier.
     */
    private void setupLifecyclePolicy() {
        try {
            // Check if lifecycle policy already exists
            GetBucketLifecycleConfigurationRequest getRequest = 
                GetBucketLifecycleConfigurationRequest.builder()
                    .bucket(bucketName)
                    .build();
            
            try {
                s3Client.getBucketLifecycleConfiguration(getRequest);
                log.debug("Lifecycle policy already exists for bucket {}", bucketName);
                return; // Policy already exists
            } catch (NoSuchLifecycleConfigurationException e) {
                // Policy doesn't exist, create it
                log.info("Creating lifecycle policy for bucket {}", bucketName);
            }
            
            // Create lifecycle rule to transition to Glacier after specified days
            LifecycleRule glacierRule = LifecycleRule.builder()
                    .id("regtech-glacier-transition")
                    .status(ExpirationStatus.ENABLED)
                    .filter(LifecycleRuleFilter.builder()
                            .prefix("raw/")
                            .build())
                    .transitions(Transition.builder()
                            .days(glacierTransitionDays)
                            .storageClass(TransitionStorageClass.GLACIER)
                            .build())
                    .build();
            
            BucketLifecycleConfiguration lifecycleConfig = BucketLifecycleConfiguration.builder()
                    .rules(glacierRule)
                    .build();
            
            PutBucketLifecycleConfigurationRequest putRequest = 
                PutBucketLifecycleConfigurationRequest.builder()
                    .bucket(bucketName)
                    .lifecycleConfiguration(lifecycleConfig)
                    .build();
            
            s3Client.putBucketLifecycleConfiguration(putRequest);
            log.info("Successfully set up lifecycle policy for bucket {} with {} days transition to Glacier", 
                    bucketName, glacierTransitionDays);
            
        } catch (Exception e) {
            // Don't fail the upload if lifecycle policy setup fails
            log.warn("Failed to set up lifecycle policy for bucket {}: {}", bucketName, e.getMessage());
        }
    }
    
    /**
     * Calculates exponential backoff delay with jitter.
     */
    private long calculateBackoffDelay(int attempt) {
        // Exponential backoff: baseDelay * 2^(attempt-1) with jitter
        long exponentialDelay = baseDelayMs * (1L << (attempt - 1));
        
        // Add jitter (±25% of the delay)
        long jitter = (long) (exponentialDelay * 0.25 * (ThreadLocalRandom.current().nextDouble() - 0.5) * 2);
        
        return Math.max(exponentialDelay + jitter, baseDelayMs);
    }
    
    /**
     * Determines if an error should not be retried.
     */
    private boolean isNonRetryableError(ErrorDetail error) {
        String code = error.getCode();
        return "S3_CHECKSUM_MISMATCH".equals(code) ||
               "S3_ETAG_MISMATCH".equals(code) ||
               code.contains("VALIDATION") ||
               code.contains("AUTHENTICATION") ||
               code.contains("AUTHORIZATION");
    }
    
    /**
     * Determines if an exception should not be retried.
     */
    private boolean isNonRetryableException(Exception e) {
        if (e instanceof S3Exception s3Exception) {
            String errorCode = s3Exception.awsErrorDetails().errorCode();
            
            // Don't retry for client errors (4xx)
            return s3Exception.statusCode() >= 400 && s3Exception.statusCode() < 500 ||
                   "InvalidAccessKeyId".equals(errorCode) ||
                   "SignatureDoesNotMatch".equals(errorCode) ||
                   "AccessDenied".equals(errorCode) ||
                   "BucketNotFound".equals(errorCode) ||
                   "InvalidBucketName".equals(errorCode);
        }
        
        // Don't retry for validation or illegal argument exceptions
        return e instanceof IllegalArgumentException ||
               e instanceof SecurityException;
    }
    
    /**
     * Checks if S3 service is available by performing a simple head bucket operation.
     */
    public Result<Boolean> checkS3ServiceHealth() {
        try {
            HeadBucketRequest headBucketRequest = HeadBucketRequest.builder()
                    .bucket(bucketName)
                    .build();
            
            s3Client.headBucket(headBucketRequest);
            return Result.success(true);
            
        } catch (Exception e) {
            log.error("S3 service health check failed: {}", e.getMessage());
            return Result.failure(ErrorDetail.of("S3_SERVICE_UNAVAILABLE", 
                "S3 service is not available: " + e.getMessage()));
        }
    }
}