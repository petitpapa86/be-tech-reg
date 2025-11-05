package com.bcbs239.regtech.ingestion.infrastructure.storage;

import com.bcbs239.regtech.core.shared.ErrorDetail;
import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.ingestion.domain.batch.FileMetadata;
import com.bcbs239.regtech.ingestion.domain.batch.S3Reference;
import com.bcbs239.regtech.ingestion.domain.services.FileStorageService;
import com.bcbs239.regtech.ingestion.infrastructure.config.S3Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

/**
 * S3 implementation of the FileStorageService.
 * Handles file storage operations using AWS S3.
 */
@Service
public class S3FileStorageService implements FileStorageService {

    private static final Logger logger = LoggerFactory.getLogger(S3FileStorageService.class);

    private final S3Client s3Client;
    private final S3Properties s3Properties;

    public S3FileStorageService(S3Properties s3Properties) {
        this.s3Properties = s3Properties;

        S3ClientBuilder s3ClientBuilder = S3Client.builder()
                .region(Region.of(s3Properties.region()));

        // Configure credentials only if both access key and secret key are provided
        if (s3Properties.accessKey() != null && !s3Properties.accessKey().trim().isEmpty() &&
            s3Properties.secretKey() != null && !s3Properties.secretKey().trim().isEmpty()) {
            AwsBasicCredentials credentials = AwsBasicCredentials.create(
                s3Properties.accessKey(),
                s3Properties.secretKey()
            );
            s3ClientBuilder.credentialsProvider(StaticCredentialsProvider.create(credentials));
        }
        // If no explicit credentials provided, AWS SDK will use default credential provider chain
        // (environment variables, system properties, credential files, IAM roles, etc.)

        // Configure endpoint if provided (for local testing with MinIO, LocalStack, etc.)
        if (s3Properties.endpoint() != null && !s3Properties.endpoint().trim().isEmpty()) {
            s3ClientBuilder.endpointOverride(URI.create(s3Properties.endpoint()));
        }

        this.s3Client = s3ClientBuilder.build();
    }

    @Override
    public Result<S3Reference> storeFile(InputStream fileStream, FileMetadata fileMetadata,
                                       String batchId, String bankId, int exposureCount) {
        try {
            // Generate unique key for the file
            String key = generateFileKey(batchId, bankId, fileMetadata.fileName());

            // Prepare S3 put request
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(s3Properties.bucket())
                    .key(key)
                    .contentType(fileMetadata.contentType())
                    .metadata(java.util.Map.of(
                        "batch-id", batchId,
                        "bank-id", bankId,
                        "exposure-count", String.valueOf(exposureCount),
                        "original-filename", fileMetadata.fileName(),
                        "upload-timestamp", Instant.now().toString()
                    ))
                    .build();

            // Upload file to S3
            long contentLength = fileStream.available();
            s3Client.putObject(putRequest, RequestBody.fromInputStream(fileStream, contentLength));

            // Get the object to retrieve version ID
            HeadObjectRequest headRequest = HeadObjectRequest.builder()
                    .bucket(s3Properties.bucket())
                    .key(key)
                    .build();

            HeadObjectResponse headResponse = s3Client.headObject(headRequest);
            String versionId = headResponse.versionId();

            // Create S3 reference
            S3Reference s3Reference = new S3Reference(
                "s3://" + s3Properties.bucket() + "/" + key,
                s3Properties.bucket(),
                key,
                versionId
            );

            logger.info("Successfully stored file {} for batch {} in S3", fileMetadata.fileName(), batchId);
            return Result.success(s3Reference);

        } catch (S3Exception e) {
            logger.error("S3 error while storing file {} for batch {}: {}", fileMetadata.fileName(), batchId, e.getMessage(), e);
            return Result.failure(ErrorDetail.of("S3_STORAGE_ERROR", "Failed to store file in S3: " + e.getMessage()));
        } catch (IOException e) {
            logger.error("IO error while storing file {} for batch {}: {}", fileMetadata.fileName(), batchId, e.getMessage(), e);
            return Result.failure(ErrorDetail.of("FILE_READ_ERROR", "Failed to read file content: " + e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error while storing file {} for batch {}: {}", fileMetadata.fileName(), batchId, e.getMessage(), e);
            return Result.failure(ErrorDetail.of("STORAGE_ERROR", "Unexpected error during file storage: " + e.getMessage()));
        }
    }

    @Override
    public Result<Boolean> checkServiceHealth() {
        try {
            // Try to list objects in the bucket to check connectivity
            ListObjectsV2Request request = ListObjectsV2Request.builder()
                    .bucket(s3Properties.bucket())
                    .maxKeys(1)
                    .build();

            s3Client.listObjectsV2(request);
            logger.debug("S3 service health check passed");
            return Result.success(true);

        } catch (S3Exception e) {
            logger.warn("S3 service health check failed: {}", e.getMessage());
            return Result.failure(ErrorDetail.of("S3_HEALTH_CHECK_FAILED", "S3 service is not available: " + e.getMessage()));
        } catch (Exception e) {
            logger.warn("Unexpected error during S3 health check: {}", e.getMessage());
            return Result.failure(ErrorDetail.of("S3_HEALTH_CHECK_ERROR", "Unexpected error during S3 health check: " + e.getMessage()));
        }
    }

    /**
     * Generates a unique S3 key for the file.
     * Format: {prefix}/{bankId}/{batchId}/{uuid}-{filename}
     */
    private String generateFileKey(String batchId, String bankId, String fileName) {
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        return String.format("%s%s/%s/%s-%s",
                s3Properties.prefix(),
                bankId,
                batchId,
                uuid,
                fileName);
    }
}