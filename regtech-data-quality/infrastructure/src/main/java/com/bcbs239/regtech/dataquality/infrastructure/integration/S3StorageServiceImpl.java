package com.bcbs239.regtech.dataquality.infrastructure.integration;

import com.bcbs239.regtech.core.shared.ErrorDetail;
import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.dataquality.application.integration.S3StorageService;
import com.bcbs239.regtech.dataquality.domain.shared.BatchId;
import com.bcbs239.regtech.dataquality.domain.shared.S3Reference;
import com.bcbs239.regtech.dataquality.domain.validation.ExposureRecord;
import com.bcbs239.regtech.dataquality.domain.validation.ExposureValidationResult;
import com.bcbs239.regtech.dataquality.domain.validation.ValidationError;
import com.bcbs239.regtech.dataquality.domain.validation.ValidationResult;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Implementation of S3StorageService that handles downloading exposure data
 * and storing detailed validation results with streaming support and encryption.
 */
@Service
public class S3StorageServiceImpl implements S3StorageService {
    
    private static final Logger logger = LoggerFactory.getLogger(S3StorageServiceImpl.class);
    
    private final S3Client s3Client;
    private final ObjectMapper objectMapper;
    private final JsonFactory jsonFactory;
    private final ExecutorService executorService;
    
    // Configuration properties
    @Value("${data-quality.s3.results-bucket:regtech-quality-results}")
    private String resultsBucket;
    
    @Value("${data-quality.s3.encryption-key-id:alias/regtech-quality-key}")
    private String encryptionKeyId;
    
    @Value("${data-quality.s3.streaming-buffer-size:8192}")
    private int streamingBufferSize;
    
    @Value("${data-quality.s3.max-concurrent-uploads:3}")
    private int maxConcurrentUploads;
    
    public S3StorageServiceImpl(S3Client s3Client) {
        this.s3Client = s3Client;
        this.objectMapper = new ObjectMapper();
        this.jsonFactory = new JsonFactory();
        this.executorService = Executors.newFixedThreadPool(maxConcurrentUploads);
    }
    
    @Override
    @Retryable(value = {S3Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public Result<List<ExposureRecord>> downloadExposures(String s3Uri) {
        logger.info("Starting download of exposures from S3 URI: {}", s3Uri);
        long startTime = System.currentTimeMillis();
        
        try {
            // Parse S3 URI
            S3Reference s3Reference = parseS3Uri(s3Uri);
            if (s3Reference == null) {
                return Result.failure(ErrorDetail.of(
                    "S3_URI_INVALID",
                    "Invalid S3 URI format: " + s3Uri,
                    "s3_uri"
                ));
            }
            
            // Download and parse with streaming
            List<ExposureRecord> exposures = downloadAndParseStreaming(s3Reference);
            
            long duration = System.currentTimeMillis() - startTime;
            logger.info("Successfully downloaded {} exposures from S3 in {} ms", exposures.size(), duration);
            
            return Result.success(exposures);
            
        } catch (S3Exception e) {
            logger.error("S3 error downloading exposures from: {}", s3Uri, e);
            return Result.failure(ErrorDetail.of(
                "S3_DOWNLOAD_ERROR",
                "Failed to download from S3: " + e.getMessage(),
                "s3_download"
            ));
        } catch (IOException e) {
            logger.error("IO error parsing exposures from: {}", s3Uri, e);
            return Result.failure(ErrorDetail.of(
                "S3_PARSE_ERROR",
                "Failed to parse JSON from S3: " + e.getMessage(),
                "json_parsing"
            ));
        } catch (Exception e) {
            logger.error("Unexpected error downloading exposures from: {}", s3Uri, e);
            return Result.failure(ErrorDetail.of(
                "S3_DOWNLOAD_UNEXPECTED_ERROR",
                "Unexpected error downloading exposures: " + e.getMessage(),
                "system"
            ));
        }
    }
    
    @Override
    @Retryable(value = {S3Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public Result<S3Reference> storeDetailedResults(BatchId batchId, ValidationResult validationResult) {
        logger.info("Starting storage of detailed validation results for batch: {}", batchId.value());
        long startTime = System.currentTimeMillis();
        
        try {
            // Create S3 key for detailed results
            String s3Key = String.format("quality/quality_%s.json", batchId.value());
            String s3Uri = String.format("s3://%s/%s", resultsBucket, s3Key);
            
            // Serialize validation result to JSON
            String jsonContent = serializeValidationResult(validationResult);
            
            // Create metadata
            Map<String, String> metadata = createMetadata(batchId, validationResult);
            
            // Upload to S3 with encryption
            uploadWithEncryption(resultsBucket, s3Key, jsonContent, metadata);
            
            // Construct an S3Reference; use a placeholder version id when not available
            S3Reference s3Reference = S3Reference.of(resultsBucket, s3Key, "0");

            long duration = System.currentTimeMillis() - startTime;
            logger.info("Successfully stored detailed results for batch {} in {} ms", batchId.value(), duration);

            return Result.success(s3Reference);
            
        } catch (S3Exception e) {
            logger.error("S3 error storing detailed results for batch: {}", batchId.value(), e);
            return Result.failure(ErrorDetail.of(
                "S3_UPLOAD_ERROR",
                "Failed to upload to S3: " + e.getMessage(),
                "s3_upload"
            ));
        } catch (IOException e) {
            logger.error("IO error serializing results for batch: {}", batchId.value(), e);
            return Result.failure(ErrorDetail.of(
                "S3_SERIALIZE_ERROR",
                "Failed to serialize validation results: " + e.getMessage(),
                "json_serialization"
            ));
        } catch (Exception e) {
            logger.error("Unexpected error storing results for batch: {}", batchId.value(), e);
            return Result.failure(ErrorDetail.of(
                "S3_UPLOAD_UNEXPECTED_ERROR",
                "Unexpected error storing detailed results: " + e.getMessage(),
                "system"
            ));
        }
    }
    
    @Override
    public Result<S3Reference> storeDetailedResults(BatchId batchId, ValidationResult validationResult, java.util.Map<String, String> metadata) {
        logger.info("Starting storage of detailed validation results for batch: {} (with custom metadata)", batchId.value());
        long startTime = System.currentTimeMillis();

        try {
            // Create S3 key for detailed results
            String s3Key = String.format("quality/quality_%s.json", batchId.value());
            String s3Uri = String.format("s3://%s/%s", resultsBucket, s3Key);

            // Serialize validation result to JSON
            String jsonContent = serializeValidationResult(validationResult);

            // Merge provided metadata with generated metadata
            Map<String, String> merged = createMetadata(batchId, validationResult);
            if (metadata != null) merged.putAll(metadata);

            // Upload to S3 with encryption
            uploadWithEncryption(resultsBucket, s3Key, jsonContent, merged);

            // Construct an S3Reference; use a placeholder version id when not available
            S3Reference s3Reference = S3Reference.of(resultsBucket, s3Key, "0");

            long duration = System.currentTimeMillis() - startTime;
            logger.info("Successfully stored detailed results for batch {} in {} ms", batchId.value(), duration);

            return Result.success(s3Reference);

        } catch (S3Exception e) {
            logger.error("S3 error storing detailed results for batch: {}", batchId.value(), e);
            return Result.failure(ErrorDetail.of(
                "S3_UPLOAD_ERROR",
                "Failed to upload to S3: " + e.getMessage(),
                "s3_upload"
            ));
        } catch (IOException e) {
            logger.error("IO error serializing results for batch: {}", batchId.value(), e);
            return Result.failure(ErrorDetail.of(
                "S3_SERIALIZE_ERROR",
                "Failed to serialize validation results: " + e.getMessage(),
                "json_serialization"
            ));
        } catch (Exception e) {
            logger.error("Unexpected error storing results for batch: {}", batchId.value(), e);
            return Result.failure(ErrorDetail.of(
                "S3_UPLOAD_UNEXPECTED_ERROR",
                "Unexpected error storing detailed results: " + e.getMessage(),
                "system"
            ));
        }
    }

    /**
     * Parse S3 URI to extract bucket and key information.
     */
    private S3Reference parseS3Uri(String s3Uri) {
        try {
            if (!s3Uri.startsWith("s3://")) {
                return null;
            }
            
            URI uri = URI.create(s3Uri);
            String bucket = uri.getHost();
            String key = uri.getPath().substring(1); // Remove leading slash
            
            if (bucket == null || bucket.isEmpty() || key.isEmpty()) {
                return null;
            }
            
            // Use placeholder version id "0" when parsing external URIs
            return S3Reference.of(bucket, key, "0");

        } catch (Exception e) {
            logger.error("Error parsing S3 URI: {}", s3Uri, e);
            return null;
        }
    }
    
    /**
     * Download and parse JSON using streaming to handle large files efficiently.
     */
    private List<ExposureRecord> downloadAndParseStreaming(S3Reference s3Reference) throws IOException {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
            .bucket(s3Reference.bucket())
            .key(s3Reference.key())
            .build();
        
        List<ExposureRecord> exposures = new ArrayList<>();
        
        try (ResponseInputStream<GetObjectResponse> s3Object = s3Client.getObject(getObjectRequest);
             JsonParser parser = jsonFactory.createParser(s3Object)) {
            
            // Expect JSON array of exposure objects
            if (parser.nextToken() != JsonToken.START_ARRAY) {
                throw new IOException("Expected JSON array of exposures");
            }
            
            // Parse each exposure object
            while (parser.nextToken() == JsonToken.START_OBJECT) {
                JsonNode exposureNode = objectMapper.readTree(parser);
                ExposureRecord exposure = parseExposureRecord(exposureNode);
                exposures.add(exposure);
                
                // Log progress for large files
                if (exposures.size() % 10000 == 0) {
                    logger.debug("Parsed {} exposures so far", exposures.size());
                }
            }
        }
        
        return exposures;
    }
    
    /**
     * Parse a single exposure record from JSON node.
     */
    private ExposureRecord parseExposureRecord(JsonNode node) {
        return ExposureRecord.builder()
            .exposureId(getTextValue(node, "exposureId"))
            .counterpartyId(getTextValue(node, "counterpartyId"))
            .amount(node.has("amount") ? node.get("amount").decimalValue() : null)
            .currency(getTextValue(node, "currency"))
            .country(getTextValue(node, "country"))
            .sector(getTextValue(node, "sector"))
            .counterpartyType(getTextValue(node, "counterpartyType"))
            .productType(getTextValue(node, "productType"))
            .leiCode(getTextValue(node, "leiCode"))
            .internalRating(getTextValue(node, "internalRating"))
            .riskCategory(getTextValue(node, "riskCategory"))
            .riskWeight(node.has("riskWeight") ? node.get("riskWeight").decimalValue() : null)
            .maturityDate(node.has("maturityDate") ? 
                java.time.LocalDate.parse(node.get("maturityDate").asText()) : null)
            .reportingDate(node.has("reportingDate") ? 
                java.time.LocalDate.parse(node.get("reportingDate").asText()) : null)
            .valuationDate(node.has("valuationDate") ? 
                java.time.LocalDate.parse(node.get("valuationDate").asText()) : null)
            .referenceNumber(getTextValue(node, "referenceNumber"))
            .build();
    }
    
    /**
     * Helper method to safely get text value from JSON node.
     */
    private String getTextValue(JsonNode node, String fieldName) {
        return node.has(fieldName) && !node.get(fieldName).isNull() ? 
            node.get(fieldName).asText() : null;
    }
    
    /**
     * Serialize validation result to JSON string.
     */
    private String serializeValidationResult(ValidationResult validationResult) throws IOException {
        Map<String, Object> resultMap = new HashMap<>();
        
        // Basic statistics
        resultMap.put("totalExposures", validationResult.totalExposures());
        resultMap.put("validExposures", validationResult.validExposures());
        resultMap.put("totalErrors", validationResult.allErrors().size());
        resultMap.put("timestamp", Instant.now().toString());
        
        // Dimension scores
        if (validationResult.dimensionScores() != null) {
            Map<String, Double> dimensionScores = new HashMap<>();
            dimensionScores.put("completeness", validationResult.dimensionScores().completeness());
            dimensionScores.put("accuracy", validationResult.dimensionScores().accuracy());
            dimensionScores.put("consistency", validationResult.dimensionScores().consistency());
            dimensionScores.put("timeliness", validationResult.dimensionScores().timeliness());
            dimensionScores.put("uniqueness", validationResult.dimensionScores().uniqueness());
            dimensionScores.put("validity", validationResult.dimensionScores().validity());
            resultMap.put("dimensionScores", dimensionScores);
        }
        
        // Exposure validation results (limited to avoid huge files)
        List<Map<String, Object>> exposureResults = new ArrayList<>();
        int count = 0;
        for (Map.Entry<String, ExposureValidationResult> entry :
             validationResult.exposureResults().entrySet()) {

            if (count >= 10000) { // Limit to first 10,000 exposures for detailed results
                break;
            }
            
            Map<String, Object> exposureResult = new HashMap<>();
            exposureResult.put("exposureId", entry.getKey());
            exposureResult.put("isValid", entry.getValue().isValid());
            exposureResult.put("errorCount", entry.getValue().errors().size());

            // Include error details for invalid exposures
            if (!entry.getValue().isValid()) {
                List<Map<String, Object>> errors = new ArrayList<>();
                for (ValidationError error : entry.getValue().errors()) {
                    Map<String, Object> errorMap = new HashMap<>();
                    errorMap.put("dimension", error.dimension().toString());
                    errorMap.put("ruleCode", error.code());
                    errorMap.put("message", error.message());
                    errorMap.put("fieldName", error.fieldName());
                    errorMap.put("severity", error.severity().toString());
                    errors.add(errorMap);
                }
                exposureResult.put("errors", errors);
            }
            
            exposureResults.add(exposureResult);
            count++;
        }
        resultMap.put("exposureResults", exposureResults);
        
        // Batch errors
        List<Map<String, Object>> batchErrors = new ArrayList<>();
        for (ValidationError error : validationResult.batchErrors()) {
            Map<String, Object> errorMap = new HashMap<>();
            errorMap.put("dimension", error.dimension().toString());
            errorMap.put("ruleCode", error.code());
            errorMap.put("message", error.message());
            errorMap.put("fieldName", error.fieldName());
            errorMap.put("severity", error.severity().toString());
            batchErrors.add(errorMap);
        }
        resultMap.put("batchErrors", batchErrors);
        
        return objectMapper.writeValueAsString(resultMap);
    }
    
    /**
     * Create metadata for S3 object.
     */
    private Map<String, String> createMetadata(BatchId batchId, ValidationResult validationResult) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("batch-id", batchId.value());
        metadata.put("total-exposures", String.valueOf(validationResult.totalExposures()));
        metadata.put("valid-exposures", String.valueOf(validationResult.validExposures()));
        metadata.put("total-errors", String.valueOf(validationResult.allErrors().size()));
        metadata.put("timestamp", Instant.now().toString());
        metadata.put("content-type", "application/json");
        
        if (validationResult.dimensionScores() != null) {
            // Calculate overall score for metadata
            double overallScore = (
                validationResult.dimensionScores().completeness() * 0.25 +
                validationResult.dimensionScores().accuracy() * 0.25 +
                validationResult.dimensionScores().consistency() * 0.20 +
                validationResult.dimensionScores().timeliness() * 0.15 +
                validationResult.dimensionScores().uniqueness() * 0.10 +
                validationResult.dimensionScores().validity() * 0.05
            );
            metadata.put("overall-score", String.format("%.2f", overallScore));
            metadata.put("compliant", String.valueOf(overallScore >= 70.0));
        }
        
        return metadata;
    }
    
    /**
     * Upload content to S3 with AES-256 encryption.
     */
    private void uploadWithEncryption(String bucket, String key, String content, Map<String, String> metadata) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
            .bucket(bucket)
            .key(key)
            .contentType("application/json")
            .contentEncoding("utf-8")
            .serverSideEncryption(ServerSideEncryption.AWS_KMS)
            .ssekmsKeyId(encryptionKeyId)
            .metadata(metadata)
            .build();
        
        RequestBody requestBody = RequestBody.fromString(content, StandardCharsets.UTF_8);
        
        s3Client.putObject(putObjectRequest, requestBody);
        
        logger.debug("Successfully uploaded encrypted object to s3://{}/{}", bucket, key);
    }
    
    /**
     * Asynchronously upload multiple files (for future use with batch processing).
     */
    public CompletableFuture<Result<List<S3Reference>>> storeDetailedResultsAsync(
        List<BatchId> batchIds, 
        List<ValidationResult> validationResults
    ) {
        return CompletableFuture.supplyAsync(() -> {
            List<S3Reference> references = new ArrayList<>();
            
            for (int i = 0; i < batchIds.size(); i++) {
                Result<S3Reference> result = storeDetailedResults(batchIds.get(i), validationResults.get(i));
                if (result.isSuccess()) {
                    references.add(result.getValue().orElse(null));
                } else {
                    references.add(null); // ensure list size matches
                    return Result.failure(result.getError().orElse(ErrorDetail.of("S3_UPLOAD_ERROR","Failed to upload one result")));
                }
            }

             return Result.success(references);
         }, executorService);
    }
    
    /**
     * Check if S3 object exists.
     */
    public boolean objectExists(String bucket, String key) {
        try {
            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();
            
            s3Client.headObject(headObjectRequest);
            return true;
            
        } catch (NoSuchKeyException e) {
            return false;
        } catch (S3Exception e) {
            logger.error("Error checking if S3 object exists: s3://{}/{}", bucket, key, e);
            return false;
        }
    }
    
    /**
     * Get object metadata.
     */
    public Map<String, String> getObjectMetadata(String bucket, String key) {
        try {
            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();
            
            HeadObjectResponse response = s3Client.headObject(headObjectRequest);
            return response.metadata();
            
        } catch (S3Exception e) {
            logger.error("Error getting S3 object metadata: s3://{}/{}", bucket, key, e);
            return Map.of();
        }
    }
    
    /**
     * Cleanup method to shutdown the executor service.
     */
    public void shutdown() {
        executorService.shutdown();
    }

    @Override
    public Result<Boolean> objectExists(String s3Uri) {
        S3Reference ref = parseS3Uri(s3Uri);
        if (ref == null) {
            return Result.failure(ErrorDetail.of("S3_URI_INVALID", "Invalid S3 URI: " + s3Uri));
        }

        try {
            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                .bucket(ref.bucket())
                .key(ref.key())
                .build();

            s3Client.headObject(headObjectRequest);
            return Result.success(true);
        } catch (NoSuchKeyException e) {
            logger.debug("S3 object not found: s3://{}/{} - {}", ref.bucket(), ref.key(), e.getMessage());
            return Result.success(false);
        } catch (S3Exception e) {
            logger.error("S3 error checking object existence: s3://{}/{}", ref.bucket(), ref.key(), e);
            return Result.failure(ErrorDetail.of("S3_HEAD_ERROR", "Failed to check object: " + e.getMessage()));
        } catch (Exception e) {
            logger.error("Error checking S3 object existence: s3://{}/{}", ref.bucket(), ref.key(), e);
            return Result.failure(ErrorDetail.of("S3_HEAD_ERROR", "Failed to check object: " + e.getMessage()));
        }
    }

    @Override
    public Result<Long> getObjectSize(String s3Uri) {
        S3Reference ref = parseS3Uri(s3Uri);
        if (ref == null) {
            return Result.failure(ErrorDetail.of("S3_URI_INVALID", "Invalid S3 URI: " + s3Uri));
        }

        try {
            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                .bucket(ref.bucket())
                .key(ref.key())
                .build();

            HeadObjectResponse response = s3Client.headObject(headObjectRequest);
            return Result.success(response.contentLength());
        } catch (NoSuchKeyException e) {
            return Result.failure(ErrorDetail.of("S3_OBJECT_NOT_FOUND", "Object not found: " + s3Uri));
        } catch (S3Exception e) {
            logger.error("S3 error getting object size for s3://{}/{}", ref.bucket(), ref.key(), e);
            return Result.failure(ErrorDetail.of("S3_HEAD_ERROR", "Failed to get object size: " + e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error getting object size for s3://{}/{}", ref.bucket(), ref.key(), e);
            return Result.failure(ErrorDetail.of("S3_HEAD_UNEXPECTED", "Unexpected error: " + e.getMessage()));
        }
    }

    @Override
    public Result<List<ExposureRecord>> downloadExposures(String s3Uri, int expectedCount) {
        Result<List<ExposureRecord>> result = downloadExposures(s3Uri);
        if (result.isFailure()) return result;

        List<ExposureRecord> exposures = result.getValue().orElse(List.of());
        if (expectedCount >= 0 && exposures.size() != expectedCount) {
            return Result.failure(ErrorDetail.of(
                "S3_EXPOSURE_COUNT_MISMATCH",
                String.format("Expected %d exposures but found %d in %s", expectedCount, exposures.size(), s3Uri),
                "validation"
            ));
        }

        return Result.success(exposures);
    }
}

