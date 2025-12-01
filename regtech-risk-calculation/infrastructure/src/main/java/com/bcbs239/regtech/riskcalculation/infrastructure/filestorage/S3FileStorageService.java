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
    public Result<String> retrieveFile(String storageUri) {
        try {
            // Parse S3 URI (format: s3://bucket/key or https://bucket.s3.region.amazonaws.com/key)
            S3Location location = parseS3Uri(storageUri);
            
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
            log.error("File not found in S3 [uri:{},error:{}]", storageUri, e.getMessage());
            return Result.failure(ErrorDetail.of(
                "S3_FILE_NOT_FOUND",
                ErrorType.SYSTEM_ERROR,
                "File not found in S3: " + storageUri,
                "file.storage.s3.not.found"
            ));
            
        } catch (S3Exception e) {
            log.error("S3 error downloading file [uri:{},error:{}]", storageUri, e.getMessage(), e);
            return Result.failure(ErrorDetail.of(
                "S3_DOWNLOAD_ERROR",
                ErrorType.SYSTEM_ERROR,
                "S3 error downloading file: " + e.getMessage(),
                "file.storage.s3.download.error"
            ));
            
        } catch (IOException e) {
            log.error("IO error reading S3 response [uri:{},error:{}]", storageUri, e.getMessage(), e);
            return Result.failure(ErrorDetail.of(
                "S3_READ_ERROR",
                ErrorType.SYSTEM_ERROR,
                "Failed to read S3 response: " + e.getMessage(),
                "file.storage.s3.read.error"
            ));
            
        } catch (Exception e) {
            log.error("Unexpected error downloading from S3 [uri:{},error:{}]", storageUri, e.getMessage(), e);
            return Result.failure(ErrorDetail.of(
                "S3_UNEXPECTED_ERROR",
                ErrorType.SYSTEM_ERROR,
                "Unexpected error downloading from S3: " + e.getMessage(),
                "file.storage.s3.unexpected.error"
            ));
        }
    }
    
    @Override
    public Result<String> storeFile(String fileName, String content) {
        try {
            String bucket = properties.getStorage().getS3().getBucket();
            String prefix = properties.getStorage().getS3().getPrefix();
            
            // Generate S3 key
            String key = prefix + fileName;
            
            log.info("Storing file to S3 [bucket:{},key:{},size:{}bytes]", 
                bucket, key, content.length());
            
            // Add metadata
            Map<String, String> metadata = new HashMap<>();
            metadata.put("file-name", fileName);
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
            
            log.info("Successfully stored file to S3 [bucket:{},key:{},uri:{}]", 
                bucket, key, uri);
            
            return Result.success(uri);
            
        } catch (S3Exception e) {
            log.error("S3 error storing file [fileName:{},error:{}]", fileName, e.getMessage(), e);
            return Result.failure(ErrorDetail.of(
                "S3_UPLOAD_ERROR",
                ErrorType.SYSTEM_ERROR,
                "S3 error storing file: " + e.getMessage(),
                "file.storage.s3.upload.error"
            ));
            
        } catch (Exception e) {
            log.error("Unexpected error storing file to S3 [fileName:{},error:{}]", 
                fileName, e.getMessage(), e);
            return Result.failure(ErrorDetail.of(
                "S3_STORAGE_ERROR",
                ErrorType.SYSTEM_ERROR,
                "Unexpected error storing file to S3: " + e.getMessage(),
                "file.storage.s3.storage.error"
            ));
        }
    }
    
    @Override
    public Result<Void> deleteFile(String storageUri) {
        try {
            // Parse S3 URI
            S3Location location = parseS3Uri(storageUri);
            
            log.info("Deleting file from S3 [bucket:{},key:{}]", location.bucket(), location.key());
            
            // Delete from S3
            coreS3Service.deleteObject(location.bucket(), location.key());
            
            log.info("Successfully deleted file from S3 [bucket:{},key:{}]", 
                location.bucket(), location.key());
            
            return Result.success();
            
        } catch (NoSuchKeyException e) {
            log.warn("File not found for deletion in S3 [uri:{}]", storageUri);
            return Result.failure(ErrorDetail.of(
                "S3_FILE_NOT_FOUND",
                ErrorType.SYSTEM_ERROR,
                "File not found for deletion in S3: " + storageUri,
                "file.storage.s3.not.found"
            ));
            
        } catch (S3Exception e) {
            log.error("S3 error deleting file [uri:{},error:{}]", storageUri, e.getMessage(), e);
            return Result.failure(ErrorDetail.of(
                "S3_DELETE_ERROR",
                ErrorType.SYSTEM_ERROR,
                "S3 error deleting file: " + e.getMessage(),
                "file.storage.s3.delete.error"
            ));
            
        } catch (Exception e) {
            log.error("Unexpected error deleting file from S3 [uri:{},error:{}]", 
                storageUri, e.getMessage(), e);
            
            return Result.failure(ErrorDetail.of(
                "S3_UNEXPECTED_ERROR",
                ErrorType.SYSTEM_ERROR,
                "Unexpected error deleting file from S3: " + e.getMessage(),
                "file.storage.s3.unexpected.error"
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
