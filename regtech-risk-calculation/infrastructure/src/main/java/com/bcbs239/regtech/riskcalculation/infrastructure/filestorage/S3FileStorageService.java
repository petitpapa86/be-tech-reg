package com.bcbs239.regtech.riskcalculation.infrastructure.filestorage;

import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.core.infrastructure.filestorage.CoreS3Service;
import com.bcbs239.regtech.riskcalculation.domain.services.IFileStorageService;
import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.BatchId;
import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.FileStorageUri;
import com.bcbs239.regtech.riskcalculation.infrastructure.config.RiskCalculationProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * S3 implementation of file storage service for risk calculation module.
 * Used in production profile for storing calculation results in AWS S3.
 */
@Service("riskCalculationS3FileStorageService")
@ConditionalOnProperty(name = "risk-calculation.storage.type", havingValue = "s3", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class S3FileStorageService implements IFileStorageService {
    
    private final CoreS3Service coreS3Service;
    private final RiskCalculationProperties properties;
    
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    
    @Override
    public Result<String> downloadFileContent(FileStorageUri uri) {
        try {
            // Parse S3 URI (format: s3://bucket/key or https://bucket.s3.region.amazonaws.com/key)
            S3Location location = parseS3Uri(uri.uri());
            
            log.info("Downloading file from S3 [bucket:{},key:{}]", location.bucket(), location.key());
            
            // Download from S3
            ResponseInputStream<GetObjectResponse> response = coreS3Service.getObjectStream(
                location.bucket(), 
                location.key()
            );
            
            // Read content
            String content = new String(response.readAllBytes(), StandardCharsets.UTF_8);
            
            log.info("Successfully downloaded file from S3 [bucket:{},key:{},size:{}bytes]", 
                location.bucket(), location.key(), content.length());
            
            return Result.success(content);
            
        } catch (NoSuchKeyException e) {
            log.error("File not found in S3 [uri:{},error:{}]", uri.uri(), e.getMessage());
            return Result.failure(ErrorDetail.of(
                "S3_FILE_NOT_FOUND",
                ErrorType.SYSTEM_ERROR,
                "File not found in S3: " + uri.uri(),
                "file.storage.s3.not.found"
            ));
            
        } catch (S3Exception e) {
            log.error("S3 error downloading file [uri:{},error:{}]", uri.uri(), e.getMessage(), e);
            return Result.failure(ErrorDetail.of(
                "S3_DOWNLOAD_ERROR",
                ErrorType.SYSTEM_ERROR,
                "S3 error downloading file: " + e.getMessage(),
                "file.storage.s3.download.error"
            ));
            
        } catch (IOException e) {
            log.error("IO error reading S3 response [uri:{},error:{}]", uri.uri(), e.getMessage(), e);
            return Result.failure(ErrorDetail.of(
                "S3_READ_ERROR",
                ErrorType.SYSTEM_ERROR,
                "Failed to read S3 response: " + e.getMessage(),
                "file.storage.s3.read.error"
            ));
            
        } catch (Exception e) {
            log.error("Unexpected error downloading from S3 [uri:{},error:{}]", uri.uri(), e.getMessage(), e);
            return Result.failure(ErrorDetail.of(
                "S3_UNEXPECTED_ERROR",
                ErrorType.SYSTEM_ERROR,
                "Unexpected error downloading from S3: " + e.getMessage(),
                "file.storage.s3.unexpected.error"
            ));
        }
    }
    
    @Override
    public Result<FileStorageUri> storeCalculationResults(BatchId batchId, String content) {
        try {
            String bucket = properties.getStorage().getS3().getBucket();
            String prefix = properties.getStorage().getS3().getPrefix();
            
            // Generate S3 key with timestamp
            String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
            String key = String.format("%s%s/results_%s.json", prefix, batchId.value(), timestamp);
            
            log.info("Storing calculation results to S3 [bucket:{},key:{},size:{}bytes]", 
                bucket, key, content.length());
            
            // Add metadata
            Map<String, String> metadata = new HashMap<>();
            metadata.put("batch-id", batchId.value());
            metadata.put("timestamp", timestamp);
            metadata.put("content-type", "application/json");
            
            // Upload to S3
            coreS3Service.putString(
                bucket, 
                key, 
                content, 
                "application/json", 
                metadata,
                properties.getStorage().getS3().getEncryption()
            );
            
            // Generate URI
            String uri = String.format("s3://%s/%s", bucket, key);
            
            log.info("Successfully stored calculation results to S3 [bucket:{},key:{},uri:{}]", 
                bucket, key, uri);
            
            return Result.success(new FileStorageUri(uri));
            
        } catch (S3Exception e) {
            log.error("S3 error storing results [batchId:{},error:{}]", batchId.value(), e.getMessage(), e);
            return Result.failure(ErrorDetail.of(
                "S3_UPLOAD_ERROR",
                ErrorType.SYSTEM_ERROR,
                "S3 error storing results: " + e.getMessage(),
                "file.storage.s3.upload.error"
            ));
            
        } catch (Exception e) {
            log.error("Unexpected error storing results to S3 [batchId:{},error:{}]", 
                batchId.value(), e.getMessage(), e);
            return Result.failure(ErrorDetail.of(
                "S3_STORAGE_ERROR",
                ErrorType.SYSTEM_ERROR,
                "Unexpected error storing results to S3: " + e.getMessage(),
                "file.storage.s3.storage.error"
            ));
        }
    }
    
    @Override
    public Result<Boolean> checkServiceHealth() {
        try {
            String bucket = properties.getStorage().getS3().getBucket();
            
            // Try to head the bucket to verify access
            coreS3Service.headObject(bucket, ".health-check");
            
            log.debug("S3 file storage health check passed [bucket:{}]", bucket);
            return Result.success(true);
            
        } catch (NoSuchKeyException e) {
            // Key not found is OK - we just want to verify bucket access
            log.debug("S3 file storage health check passed (key not found is expected) [bucket:{}]", 
                properties.getStorage().getS3().getBucket());
            return Result.success(true);
            
        } catch (S3Exception e) {
            log.warn("S3 file storage health check failed [error:{}]", e.getMessage());
            return Result.failure(ErrorDetail.of(
                "S3_HEALTH_CHECK_FAILED",
                ErrorType.SYSTEM_ERROR,
                "S3 storage health check failed: " + e.getMessage(),
                "file.storage.s3.health.error"
            ));
            
        } catch (Exception e) {
            log.warn("Unexpected error during S3 health check [error:{}]", e.getMessage());
            return Result.failure(ErrorDetail.of(
                "S3_HEALTH_CHECK_ERROR",
                ErrorType.SYSTEM_ERROR,
                "Unexpected error during S3 health check: " + e.getMessage(),
                "file.storage.s3.health.unexpected.error"
            ));
        }
    }
    
    /**
     * Parses S3 URI into bucket and key components.
     * Supports both s3:// and https:// formats.
     */
    private S3Location parseS3Uri(String uri) {
        if (uri.startsWith("s3://")) {
            // Format: s3://bucket/key
            String withoutProtocol = uri.substring(5);
            int slashIndex = withoutProtocol.indexOf('/');
            if (slashIndex == -1) {
                throw new IllegalArgumentException("Invalid S3 URI format: " + uri);
            }
            String bucket = withoutProtocol.substring(0, slashIndex);
            String key = withoutProtocol.substring(slashIndex + 1);
            return new S3Location(bucket, key);
            
        } else if (uri.startsWith("https://")) {
            // Format: https://bucket.s3.region.amazonaws.com/key
            String withoutProtocol = uri.substring(8);
            int firstDot = withoutProtocol.indexOf('.');
            int firstSlash = withoutProtocol.indexOf('/');
            
            if (firstDot == -1 || firstSlash == -1) {
                throw new IllegalArgumentException("Invalid S3 HTTPS URI format: " + uri);
            }
            
            String bucket = withoutProtocol.substring(0, firstDot);
            String key = withoutProtocol.substring(firstSlash + 1);
            return new S3Location(bucket, key);
            
        } else {
            throw new IllegalArgumentException("Unsupported URI format (must be s3:// or https://): " + uri);
        }
    }
    
    /**
     * Record for S3 location (bucket and key).
     */
    private record S3Location(String bucket, String key) {}
}
