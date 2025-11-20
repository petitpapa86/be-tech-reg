package com.bcbs239.regtech.riskcalculation.infrastructure.filestorage;

import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.core.infrastructure.persistence.LoggingConfiguration;
import com.bcbs239.regtech.core.infrastructure.filestorage.CoreS3Service;
import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.FileStorageUri;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * S3 implementation of file storage service for risk calculation module.
 * Handles downloading exposure data and uploading calculation results to S3.
 * Active when storage.type=s3 or by default in production.
 */
@Service
@ConditionalOnProperty(name = "storage.type", havingValue = "s3", matchIfMissing = true)
public class S3FileStorageService implements IFileStorageService {

    private final CoreS3Service coreS3Service;
    private final String bucketName;
    private final String keyPrefix;
    private final ObjectMapper objectMapper;
    private final JsonFactory jsonFactory;

    public S3FileStorageService(CoreS3Service coreS3Service,
            @Value("${aws.s3.bucket-name:regtech-storage}") String bucketName,
            @Value("${aws.s3.key-prefix:risk-calculation/}") String keyPrefix) {
        this.coreS3Service = coreS3Service;
        this.bucketName = bucketName;
        this.keyPrefix = keyPrefix;
        this.objectMapper = new ObjectMapper();
        this.jsonFactory = new JsonFactory();
    }

    @Override
    public Result<List<ExposureRecord>> downloadExposures(FileStorageUri uri) {
        try {
            LoggingConfiguration.logStructured("Starting exposure file download",
                Map.of("uri", uri.uri(), "eventType", "EXPOSURE_DOWNLOAD_START"));

            String s3Uri = uri.uri();
            var parsed = com.bcbs239.regtech.core.infrastructure.filestorage.S3Utils.parseS3Uri(s3Uri);
            if (parsed.isEmpty()) {
                return Result.failure(ErrorDetail.of("INVALID_S3_URI", ErrorType.BUSINESS_RULE_ERROR,
                    "Invalid S3 URI format: " + s3Uri, "file.storage.invalid.uri"));
            }
            String bucket = parsed.get().bucket();
            String key = parsed.get().key();

            try (ResponseInputStream<GetObjectResponse> s3Object = coreS3Service.getObjectStream(bucket, key)) {
                List<ExposureRecord> exposures = parseExposuresFromStream(s3Object);
                LoggingConfiguration.logStructured("Exposure file downloaded successfully",
                    Map.of("uri", uri.uri(), "exposureCount", exposures.size(), "eventType", "EXPOSURE_DOWNLOAD_SUCCESS"));
                return Result.success(exposures);
            }

        } catch (software.amazon.awssdk.services.s3.model.S3Exception e) {
            LoggingConfiguration.logStructured("S3 error downloading exposure file",
                Map.of("uri", uri.uri(), "eventType", "EXPOSURE_DOWNLOAD_S3_ERROR"), e);
            return Result.failure(ErrorDetail.of("S3_DOWNLOAD_ERROR", ErrorType.SYSTEM_ERROR,
                "Failed to download file from S3: " + e.getMessage(), "file.storage.s3.download.error"));
        } catch (IOException e) {
            LoggingConfiguration.logStructured("IO error parsing exposure file",
                Map.of("uri", uri.uri(), "eventType", "EXPOSURE_DOWNLOAD_IO_ERROR"), e);
            return Result.failure(ErrorDetail.of("FILE_PARSE_ERROR", ErrorType.SYSTEM_ERROR,
                "Failed to parse exposure file: " + e.getMessage(), "file.storage.parse.error"));
        } catch (Exception e) {
            LoggingConfiguration.logStructured("Unexpected error downloading exposure file",
                Map.of("uri", uri.uri(), "eventType", "EXPOSURE_DOWNLOAD_ERROR"), e);
            return Result.failure(ErrorDetail.of("DOWNLOAD_ERROR", ErrorType.SYSTEM_ERROR,
                "Unexpected error downloading file: " + e.getMessage(), "file.storage.download.error"));
        }
    }

    @Override
    public Result<FileStorageUri> uploadCalculationResults(String batchId, String bankId, String calculationResults) {
        try {
            LoggingConfiguration.logStructured("Starting calculation results upload",
                Map.of("batchId", batchId, "bankId", bankId, "eventType", "CALCULATION_UPLOAD_START"));

            String key = generateResultsKey(batchId, bankId);

            Map<String, String> metadata = Map.of(
                "batch-id", batchId,
                "bank-id", bankId,
                "upload-timestamp", Instant.now().toString(),
                "content-type", "risk-calculation-results"
            );

            coreS3Service.putString(bucketName, key, calculationResults, "application/json", metadata, null);

            String uri = "s3://" + bucketName + "/" + key;
            FileStorageUri storageUri = FileStorageUri.of(uri);

            LoggingConfiguration.logStructured("Calculation results uploaded successfully",
                Map.of("batchId", batchId, "bankId", bankId, "uri", uri, "eventType", "CALCULATION_UPLOAD_SUCCESS"));

            return Result.success(storageUri);

        } catch (software.amazon.awssdk.services.s3.model.S3Exception e) {
            LoggingConfiguration.logStructured("S3 error uploading calculation results",
                Map.of("batchId", batchId, "bankId", bankId, "eventType", "CALCULATION_UPLOAD_S3_ERROR"), e);
            return Result.failure(ErrorDetail.of("S3_UPLOAD_ERROR", ErrorType.SYSTEM_ERROR,
                "Failed to upload results to S3: " + e.getMessage(), "file.storage.s3.upload.error"));
        } catch (Exception e) {
            LoggingConfiguration.logStructured("Unexpected error uploading calculation results",
                Map.of("batchId", batchId, "bankId", bankId, "eventType", "CALCULATION_UPLOAD_ERROR"), e);
            return Result.failure(ErrorDetail.of("UPLOAD_ERROR", ErrorType.SYSTEM_ERROR,
                "Unexpected error uploading results: " + e.getMessage(), "file.storage.upload.error"));
        }
    }

    @Override
    public Result<Boolean> checkServiceHealth() {
        try {
            // Use head/list via coreS3Service
            coreS3Service.headObject(bucketName, "");
            LoggingConfiguration.logStructured("S3 service health check passed",
                Map.of("bucket", bucketName, "eventType", "S3_HEALTH_CHECK_SUCCESS"));
            return Result.success(true);
        } catch (software.amazon.awssdk.services.s3.model.S3Exception e) {
            LoggingConfiguration.logStructured("S3 service health check failed",
                Map.of("bucket", bucketName, "eventType", "S3_HEALTH_CHECK_FAILED"), e);
            return Result.failure(ErrorDetail.of("S3_HEALTH_CHECK_FAILED", ErrorType.SYSTEM_ERROR,
                "S3 service is not available: " + e.getMessage(), "file.storage.s3.health.check.failed"));
        } catch (Exception e) {
            LoggingConfiguration.logStructured("Unexpected error during S3 health check",
                Map.of("bucket", bucketName, "eventType", "S3_HEALTH_CHECK_ERROR"), e);
            return Result.failure(ErrorDetail.of("S3_HEALTH_CHECK_ERROR", ErrorType.SYSTEM_ERROR,
                "Unexpected error during S3 health check: " + e.getMessage(), "file.storage.s3.health.check.error"));
        }
    }

    private List<ExposureRecord> parseExposuresFromStream(ResponseInputStream<GetObjectResponse> inputStream) throws IOException {
        List<ExposureRecord> exposures = new ArrayList<>();
        try (JsonParser parser = jsonFactory.createParser(inputStream)) {
            if (parser.nextToken() != JsonToken.START_ARRAY) {
                throw new IOException("Expected JSON array of exposures");
            }
            while (parser.nextToken() == JsonToken.START_OBJECT) {
                JsonNode exposureNode = objectMapper.readTree(parser);
                ExposureRecord exposure = new ExposureRecord(
                    exposureNode.path("exposureId").asText(),
                    exposureNode.path("clientName").asText(),
                    exposureNode.path("originalAmount").asText(),
                    exposureNode.path("originalCurrency").asText(),
                    exposureNode.path("country").asText(),
                    exposureNode.path("sector").asText()
                );
                exposures.add(exposure);
            }
        }
        return exposures;
    }

    private String generateResultsKey(String batchId, String bankId) {
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        return String.format("%sresults/%s/%s/%s-results.json", keyPrefix, bankId, batchId, uuid);
    }
}
