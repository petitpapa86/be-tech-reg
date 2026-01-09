package com.bcbs239.regtech.core.infrastructure.filestorage;

import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.core.infrastructure.s3.S3Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "ingestion.s3.enabled", havingValue = "true", matchIfMissing = true)
public class CoreS3FileStorageService {

    private static final Logger logger = LoggerFactory.getLogger(CoreS3FileStorageService.class);

    private final CoreS3Service coreS3Service;
    private final S3Properties s3Properties;

    public CoreS3FileStorageService(CoreS3Service coreS3Service, S3Properties s3Properties) {
        this.coreS3Service = coreS3Service;
        this.s3Properties = s3Properties;
    }

    public Result<CoreS3Reference> storeFile(InputStream fileStream, String fileName, String batchId, String bankId, int exposureCount) {
        try {
            String key = generateFileKey(batchId, bankId, fileName);

            java.util.Map<String, String> metadata = java.util.Map.of(
                    "batch-id", batchId,
                    "bank-id", bankId,
                    "exposure-count", String.valueOf(exposureCount),
                    "original-filename", fileName,
                    "upload-timestamp", Instant.now().toString()
            );

            byte[] bytes = fileStream.readAllBytes();

            coreS3Service.putBytes(s3Properties.getBucket(), key, bytes, "application/octet-stream", metadata, s3Properties.getKmsKeyId());

            HeadObjectResponse headResponse = coreS3Service.headObject(s3Properties.getBucket(), key);
            String versionId = headResponse.versionId();

            CoreS3Reference ref = new CoreS3Reference("s3://" + s3Properties.getBucket() + "/" + key, s3Properties.getBucket(), key, versionId);
            return Result.success(ref);

        } catch (S3Exception e) {
            logger.error("S3 error while storing file {} for batch {}: {}", fileName, batchId, e.getMessage(), e);
            return Result.failure(ErrorDetail.of("S3_STORAGE_ERROR", ErrorType.SYSTEM_ERROR, "Failed to store file in S3: " + e.getMessage(), "storage.s3.upload.error"));
        } catch (IOException e) {
            logger.error("IO error while storing file {} for batch {}: {}", fileName, batchId, e.getMessage(), e);
            return Result.failure(ErrorDetail.of("FILE_READ_ERROR", ErrorType.SYSTEM_ERROR, "Failed to read file content: " + e.getMessage(), "storage.file.read.error"));
        } catch (Exception e) {
            logger.error("Unexpected error while storing file {} for batch {}: {}", fileName, batchId, e.getMessage(), e);
            return Result.failure(ErrorDetail.of("STORAGE_ERROR", ErrorType.SYSTEM_ERROR, "Unexpected error during file storage: " + e.getMessage(), "storage.unexpected.error"));
        }
    }

    public Result<Boolean> checkServiceHealth() {
        try {
            coreS3Service.headObject(s3Properties.getBucket(), "");
            return Result.success(true);
        } catch (S3Exception e) {
            logger.warn("S3 service health check failed: {}", e.getMessage());
            return Result.failure(ErrorDetail.of("S3_HEALTH_CHECK_FAILED", ErrorType.SYSTEM_ERROR, "S3 service is not available: " + e.getMessage(), "storage.s3.health.check.failed"));
        } catch (Exception e) {
            logger.warn("Unexpected error during S3 health check: {}", e.getMessage());
            return Result.failure(ErrorDetail.of("S3_HEALTH_CHECK_ERROR", ErrorType.SYSTEM_ERROR, "Unexpected error during S3 health check: " + e.getMessage(), "storage.s3.health.check.error"));
        }
    }

    private String generateFileKey(String batchId, String bankId, String fileName) {
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        return String.format("%s%s/%s/%s-%s",
                s3Properties.getPrefix(),
                bankId,
                batchId,
                uuid,
                fileName);
    }
}
