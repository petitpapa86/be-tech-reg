package com.bcbs239.regtech.riskcalculation.infrastructure.filestorage;

import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.core.infrastructure.filestorage.CoreS3Service;
import com.bcbs239.regtech.core.infrastructure.s3.S3Properties;
import com.bcbs239.regtech.riskcalculation.domain.services.ICalculationResultsStorageService;
import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.FileStorageUri;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;

/**
 * S3 implementation for storing calculation results.
 * Stores files in S3 bucket with structure:
 * 
 * s3://risk-analysis-production/calculated/
 *   ├── calc_batch_20240331_001_20240331_143045.json
 *   ├── calc_batch_20240331_002_20240331_143145.json
 *   └── ...
 */
@Service
@Slf4j
@ConditionalOnProperty(name = "risk-calculation.storage.type", havingValue = "s3", matchIfMissing = false)
public class S3CalculationResultsStorageService implements ICalculationResultsStorageService {

    private static final String S3_PREFIX = "calculated/";
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    
    private final CoreS3Service coreS3Service;
    private final S3Properties s3Properties;

    public S3CalculationResultsStorageService(CoreS3Service coreS3Service, S3Properties s3Properties) {
        this.coreS3Service = coreS3Service;
        this.s3Properties = s3Properties;
    }

    @Override
    public Result<FileStorageUri> storeCalculationResults(String jsonContent, String batchId, String bankId) {
        log.debug("Storing calculation results to S3 for batchId: {}, bankId: {}", batchId, bankId);

        if (jsonContent == null || jsonContent.trim().isEmpty()) {
            return Result.failure(ErrorDetail.of("NULL_JSON_CONTENT", ErrorType.VALIDATION_ERROR,
                "JSON content cannot be null or empty", "calculation.results.null.content"));
        }

        if (batchId == null || batchId.trim().isEmpty()) {
            return Result.failure(ErrorDetail.of("INVALID_BATCH_ID", ErrorType.VALIDATION_ERROR,
                "Batch ID cannot be null or empty", "calculation.results.invalid.batch.id"));
        }

        if (bankId == null || bankId.trim().isEmpty()) {
            return Result.failure(ErrorDetail.of("INVALID_BANK_ID", ErrorType.VALIDATION_ERROR,
                "Bank ID cannot be null or empty", "calculation.results.invalid.bank.id"));
        }

        try {
            // Generate S3 key: calculated/calc_batch_20240331_001_20240331_143045.json
            String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
            String fileName = String.format("calc_%s_%s.json", batchId, timestamp);
            String s3Key = S3_PREFIX + fileName;

            // Get bucket name from properties
            String bucket = s3Properties.getBucket();

            // Store in S3 using CoreS3Service
            PutObjectResponse response = coreS3Service.putString(
                bucket, 
                s3Key, 
                jsonContent, 
                "application/json", 
                new HashMap<>(), 
                null
            );

            // Create S3 URI
            String s3Uri = String.format("s3://%s/%s", bucket, s3Key);
            FileStorageUri fileStorageUri = FileStorageUri.of(s3Uri);

            log.info("Successfully stored calculation results for batch {} to S3: {}", batchId, s3Uri);

            return Result.success(fileStorageUri);

        } catch (Exception e) {
            log.error("Unexpected error storing calculation results to S3 for batchId: {}, bankId: {}", 
                batchId, bankId, e);
            return Result.failure(ErrorDetail.of("S3_STORAGE_ERROR", ErrorType.SYSTEM_ERROR,
                String.format("Unexpected S3 storage error: %s", e.getMessage()),
                "calculation.results.s3.unexpected.error"));
        }
    }

    @Override
    public Result<String> retrieveCalculationResults(FileStorageUri fileUri) {
        log.debug("Retrieving calculation results from S3: {}", fileUri.uri());

        if (fileUri == null || fileUri.uri() == null || fileUri.uri().trim().isEmpty()) {
            return Result.failure(ErrorDetail.of("INVALID_FILE_URI", ErrorType.VALIDATION_ERROR,
                "File URI cannot be null or empty", "calculation.results.invalid.uri"));
        }

        try {
            String uriString = fileUri.uri();
            
            // Extract bucket and key from URI
            String bucket;
            String s3Key;
            if (uriString.startsWith("s3://")) {
                // Parse s3://bucket-name/key format
                String[] parts = uriString.substring(5).split("/", 2);
                if (parts.length < 2) {
                    return Result.failure(ErrorDetail.of("INVALID_S3_URI", ErrorType.VALIDATION_ERROR,
                        "Invalid S3 URI format: " + uriString, "calculation.results.invalid.s3.uri"));
                }
                bucket = parts[0];
                s3Key = parts[1];
            } else {
                // Assume it's already a key, use default bucket
                bucket = s3Properties.getBucket();
                s3Key = uriString;
            }

            // Download from S3 using CoreS3Service
            try (InputStream inputStream = coreS3Service.getObjectStream(bucket, s3Key)) {
                String jsonContent = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                
                log.debug("Successfully retrieved calculation results from S3: {}", fileUri.uri());
                
                return Result.success(jsonContent);
            }

        } catch (Exception e) {
            log.error("Unexpected error retrieving calculation results from S3: {}", fileUri.uri(), e);
            return Result.failure(ErrorDetail.of("S3_RETRIEVAL_ERROR", ErrorType.SYSTEM_ERROR,
                String.format("Unexpected S3 retrieval error: %s", e.getMessage()),
                "calculation.results.s3.unexpected.retrieval.error"));
        }
    }

    @Override
    public Result<Boolean> checkServiceHealth() {
        try {
            // Try to perform a simple head operation on the bucket to check connectivity
            String bucket = s3Properties.getBucket();
            String testKey = S3_PREFIX + "health-check-test";
            
            // Try to check if a test object exists (it doesn't need to exist, we just need to verify S3 connectivity)
            try {
                coreS3Service.headObject(bucket, testKey);
            } catch (software.amazon.awssdk.services.s3.model.NoSuchKeyException e) {
                // This is expected - the key doesn't exist, but we successfully connected to S3
                log.debug("S3 health check passed - successfully connected to bucket: {}", bucket);
            }

            log.debug("S3 calculation results storage health check passed");
            return Result.success(true);

        } catch (Exception e) {
            log.warn("S3 storage health check failed: {}", e.getMessage());
            return Result.failure(ErrorDetail.of("S3_STORAGE_HEALTH_CHECK_ERROR", ErrorType.SYSTEM_ERROR,
                "S3 health check failed: " + e.getMessage(),
                "calculation.results.s3.health.check.error"));
        }
    }
}
