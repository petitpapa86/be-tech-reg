package com.bcbs239.regtech.core.infrastructure.filestorage;

import com.bcbs239.regtech.core.infrastructure.s3.S3Properties;
import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.time.Instant;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "storage.type", havingValue = "s3", matchIfMissing = true)
public class CoreS3FileStorageService {

    private static final Logger logger = LoggerFactory.getLogger(CoreS3FileStorageService.class);

    private final S3Client s3Client;
    private final S3Properties s3Properties;

    public CoreS3FileStorageService(S3Properties s3Properties) {
        this.s3Properties = s3Properties;

        S3ClientBuilder s3ClientBuilder = S3Client.builder()
                .region(Region.of(s3Properties.getRegion()));

        if (s3Properties.getAccessKey() != null && !s3Properties.getAccessKey().trim().isEmpty()
                && s3Properties.getSecretKey() != null && !s3Properties.getSecretKey().trim().isEmpty()) {
            AwsBasicCredentials credentials = AwsBasicCredentials.create(s3Properties.getAccessKey(), s3Properties.getSecretKey());
            s3ClientBuilder.credentialsProvider(StaticCredentialsProvider.create(credentials));
        }

        if (s3Properties.getEndpoint() != null && !s3Properties.getEndpoint().trim().isEmpty()) {
            s3ClientBuilder.endpointOverride(URI.create(s3Properties.getEndpoint()));
        }

        this.s3Client = s3ClientBuilder.build();
    }

    public Result<CoreS3Reference> storeFile(InputStream fileStream, String fileName, String batchId, String bankId, int exposureCount) {
        try {
            String key = generateFileKey(batchId, bankId, fileName);

            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(s3Properties.getBucket())
                    .key(key)
                    .contentType("application/octet-stream")
                    .metadata(java.util.Map.of(
                            "batch-id", batchId,
                            "bank-id", bankId,
                            "exposure-count", String.valueOf(exposureCount),
                            "original-filename", fileName,
                            "upload-timestamp", Instant.now().toString()
                    ))
                    .build();

            long contentLength = fileStream.available();
            s3Client.putObject(putRequest, RequestBody.fromInputStream(fileStream, contentLength));

            HeadObjectRequest headRequest = HeadObjectRequest.builder()
                    .bucket(s3Properties.getBucket())
                    .key(key)
                    .build();

            HeadObjectResponse headResponse = s3Client.headObject(headRequest);
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
            ListObjectsV2Request request = ListObjectsV2Request.builder()
                    .bucket(s3Properties.getBucket())
                    .maxKeys(1)
                    .build();

            s3Client.listObjectsV2(request);
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

