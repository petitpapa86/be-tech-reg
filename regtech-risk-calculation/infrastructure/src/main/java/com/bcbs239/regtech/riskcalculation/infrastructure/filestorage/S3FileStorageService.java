package com.bcbs239.regtech.riskcalculation.infrastructure.filestorage;

import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.core.infrastructure.persistence.LoggingConfiguration;
import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.FileStorageUri;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.net.URI;
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

    private final S3Client s3Client;
    private final String bucketName;
    private final String keyPrefix;
    private final ObjectMapper objectMapper;
    private final JsonFactory jsonFactory;

    public S3FileStorageService(
            @Value("${aws.s3.bucket-name:regtech-storage}") String bucketName,
            @Value("${aws.s3.key-prefix:risk-calculation/}") String keyPrefix,
            @Value("${aws.s3.region:us-east-1}") String region,
            @Value("${aws.s3.access-key:#{null}}") String accessKey,
            @Value("${aws.s3.secret-key:#{null}}") String secretKey,
            @Value("${aws.s3.endpoint:#{null}}") String endpoint) {
        
        this.bucketName = bucketName;
        this.keyPrefix = keyPrefix;
        this.objectMapper = new ObjectMapper();
        this.jsonFactory = new JsonFactory();

        S3ClientBuilder s3ClientBuilder = S3Client.builder()
                .region(Region.of(region));

        // Configure credentials if provided
        if (accessKey != null && !accessKey.trim().isEmpty() &&
            secretKey != null && !secretKey.trim().isEmpty()) {
            AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);
            s3ClientBuilder.credentialsProvider(StaticCredentialsProvider.create(credentials));
        }

        // Configure endpoint if provided (for local testing)
        if (endpoint != null && !endpoint.trim().isEmpty()) {
            s3ClientBuilder.endpointOverride(URI.create(endpoint));
        }

        this.s3Client = s3ClientBuilder.build();
    }

    @Override
    public Result<List<ExposureRecord>> downloadExposures(FileStorageUri uri) {
        try {
            LoggingConfiguration.logStructured("Starting exposure file download",
                Map.of("uri", uri.uri(), "eventType", "EXPOSURE_DOWNLOAD_START"));

            // Parse S3 URI to extract bucket and key
            String s3Uri = uri.uri();
            if (!s3Uri.startsWith("s3://")) {
                return Result.failure(ErrorDetail.of("INVALID_S3_URI", ErrorType.BUSINESS_RULE_ERROR,
                    "URI must start with s3://: " + s3Uri, "file.storage.invalid.uri"));
            }

            String[] parts = s3Uri.substring(5).split("/", 2);
            if (parts.length != 2) {
                return Result.failure(ErrorDetail.of("INVALID_S3_URI_FORMAT", ErrorType.BUSINESS_RULE_ERROR,
                    "Invalid S3 URI format: " + s3Uri, "file.storage.invalid.uri.format"));
            }

            String bucket = parts[0];
            String key = parts[1];

            // Download file from S3
            GetObjectRequest getRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();

            ResponseInputStream<GetObjectResponse> s3Object = s3Client.getObject(getRequest);

            // Parse JSON using streaming parser for memory efficiency
            List<ExposureRecord> exposures = parseExposuresFromStream(s3Object);

            LoggingConfiguration.logStructured("Exposure file downloaded successfully",
                Map.of("uri", uri.uri(), "exposureCount", exposures.size(), "eventType", "EXPOSURE_DOWNLOAD_SUCCESS"));

            return Result.success(exposures);

        } catch (S3Exception e) {
            LoggingConfiguration.logStructured("S3 error downloading exposure file",
                Map.of("uri", uri.uri(), "eventType", "EXPOSURE_DOWNLOAD_S3_ERROR"), e);
            
            return Result.failure(ErrorDetail.of("S3_DOWNLOAD_ERROR", ErrorType.INFRASTRUCTURE_ERROR,
                "Failed to download file from S3: " + e.getMessage(), "file.storage.s3.download.error"));

        } catch (IOException e) {
            LoggingConfiguration.logStructured("IO error parsing exposure file",
                Map.of("uri", uri.uri(), "eventType", "EXPOSURE_DOWNLOAD_IO_ERROR"), e);
            
            return Result.failure(ErrorDetail.of("FILE_PARSE_ERROR", ErrorType.INFRASTRUCTURE_ERROR,
                "Failed to parse exposure file: " + e.getMessage(), "file.storage.parse.error"));

        } catch (Exception e) {
            LoggingConfiguration.logStructured("Unexpected error downloading exposure file",
                Map.of("uri", uri.uri(), "eventType", "EXPOSURE_DOWNLOAD_ERROR"), e);
            
            return Result.failure(ErrorDetail.of("DOWNLOAD_ERROR", ErrorType.INFRASTRUCTURE_ERROR,
                "Unexpected error downloading file: " + e.getMessage(), "file.storage.download.error"));
        }
    }

    @Override
    public Result<FileStorageUri> uploadCalculationResults(String batchId, String bankId, String calculationResults) {
        try {
            LoggingConfiguration.logStructured("Starting calculation results upload",
                Map.of("batchId", batchId, "bankId", bankId, "eventType", "CALCULATION_UPLOAD_START"));

            // Generate unique key for the results file
            String key = generateResultsKey(batchId, bankId);

            // Upload results to S3 with encryption
            PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType("application/json")
                .serverSideEncryption(ServerSideEncryption.AES256)
                .metadata(Map.of(
                    "batch-id", batchId,
                    "bank-id", bankId,
                    "upload-timestamp", Instant.now().toString(),
                    "content-type", "risk-calculation-results"
                ))
                .build();

            s3Client.putObject(putRequest, RequestBody.fromString(calculationResults));

            // Create storage URI
            String uri = "s3://" + bucketName + "/" + key;
            FileStorageUri storageUri = FileStorageUri.of(uri);

            LoggingConfiguration.logStructured("Calculation results uploaded successfully",
                Map.of("batchId", batchId, "bankId", bankId, "uri", uri, "eventType", "CALCULATION_UPLOAD_SUCCESS"));

            return Result.success(storageUri);

        } catch (S3Exception e) {
            LoggingConfiguration.logStructured("S3 error uploading calculation results",
                Map.of("batchId", batchId, "bankId", bankId, "eventType", "CALCULATION_UPLOAD_S3_ERROR"), e);
            
            return Result.failure(ErrorDetail.of("S3_UPLOAD_ERROR", ErrorType.INFRASTRUCTURE_ERROR,
                "Failed to upload results to S3: " + e.getMessage(), "file.storage.s3.upload.error"));

        } catch (Exception e) {
            LoggingConfiguration.logStructured("Unexpected error uploading calculation results",
                Map.of("batchId", batchId, "bankId", bankId, "eventType", "CALCULATION_UPLOAD_ERROR"), e);
            
            return Result.failure(ErrorDetail.of("UPLOAD_ERROR", ErrorType.INFRASTRUCTURE_ERROR,
                "Unexpected error uploading results: " + e.getMessage(), "file.storage.upload.error"));
        }
    }

    @Override
    public Result<Boolean> checkServiceHealth() {
        try {
            // Try to list objects in the bucket to check connectivity
            ListObjectsV2Request request = ListObjectsV2Request.builder()
                .bucket(bucketName)
                .maxKeys(1)
                .build();

            s3Client.listObjectsV2(request);
            
            LoggingConfiguration.logStructured("S3 service health check passed",
                Map.of("bucket", bucketName, "eventType", "S3_HEALTH_CHECK_SUCCESS"));
            
            return Result.success(true);

        } catch (S3Exception e) {
            LoggingConfiguration.logStructured("S3 service health check failed",
                Map.of("bucket", bucketName, "eventType", "S3_HEALTH_CHECK_FAILED"), e);
            
            return Result.failure(ErrorDetail.of("S3_HEALTH_CHECK_FAILED", ErrorType.INFRASTRUCTURE_ERROR,
                "S3 service is not available: " + e.getMessage(), "file.storage.s3.health.check.failed"));

        } catch (Exception e) {
            LoggingConfiguration.logStructured("Unexpected error during S3 health check",
                Map.of("bucket", bucketName, "eventType", "S3_HEALTH_CHECK_ERROR"), e);
            
            return Result.failure(ErrorDetail.of("S3_HEALTH_CHECK_ERROR", ErrorType.INFRASTRUCTURE_ERROR,
                "Unexpected error during S3 health check: " + e.getMessage(), "file.storage.s3.health.check.error"));
        }
    }

    /**
     * Parse exposures from JSON stream using streaming parser for memory efficiency
     */
    private List<ExposureRecord> parseExposuresFromStream(ResponseInputStream<GetObjectResponse> inputStream) throws IOException {
        List<ExposureRecord> exposures = new ArrayList<>();

        try (JsonParser parser = jsonFactory.createParser(inputStream)) {
            // Expect JSON array of exposure objects
            if (parser.nextToken() != JsonToken.START_ARRAY) {
                throw new IOException("Expected JSON array of exposures");
            }

            // Parse each exposure object
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

    /**
     * Generate unique S3 key for calculation results
     * Format: {prefix}results/{bankId}/{batchId}/{uuid}-results.json
     */
    private String generateResultsKey(String batchId, String bankId) {
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        return String.format("%sresults/%s/%s/%s-results.json", keyPrefix, bankId, batchId, uuid);
    }
}