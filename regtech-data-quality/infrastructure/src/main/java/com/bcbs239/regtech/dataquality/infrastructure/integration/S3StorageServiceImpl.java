package com.bcbs239.regtech.dataquality.infrastructure.integration;

import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.core.domain.shared.dto.BatchDataDTO;
import com.bcbs239.regtech.core.infrastructure.filestorage.CoreS3Service;
import com.bcbs239.regtech.dataquality.application.validation.S3StorageService;
import com.bcbs239.regtech.dataquality.domain.shared.BatchId;
import com.bcbs239.regtech.dataquality.domain.shared.S3Reference;
import com.bcbs239.regtech.dataquality.domain.validation.ExposureRecord;
import com.bcbs239.regtech.dataquality.domain.validation.ExposureValidationResult;
import com.bcbs239.regtech.dataquality.domain.validation.ValidationError;
import com.bcbs239.regtech.dataquality.domain.validation.ValidationResult;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.io.IOException;
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
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "data-quality.storage.type", havingValue = "s3", matchIfMissing = false)
public class S3StorageServiceImpl implements S3StorageService {
    
    private static final Logger logger = LoggerFactory.getLogger(S3StorageServiceImpl.class);
    
    private final CoreS3Service coreS3Service;
    private final ObjectMapper objectMapper;
    private final JsonFactory jsonFactory;
    private ExecutorService executorService;
    
    // Configuration properties
    @Value("${data-quality.storage.type:s3}")
    private String storageType;
    
    @Value("${data-quality.storage.s3.bucket:regtech-data-quality}")
    private String resultsBucket;
    
    @Value("${data-quality.s3.encryption-key-id:alias/regtech-quality-key}")
    private String encryptionKeyId;
    
    @Value("${data-quality.s3.streaming-buffer-size:8192}")
    private int streamingBufferSize;
    
    @Value("${data-quality.s3.max-concurrent-uploads:3}")
    private int maxConcurrentUploads;
    
    @Value("${data-quality.storage.local.base-path:${user.dir}/data/quality}")
    private String localBasePath;

    public S3StorageServiceImpl(CoreS3Service coreS3Service, ObjectMapper objectMapper, JsonFactory jsonFactory) {
        this.coreS3Service = coreS3Service;
        this.objectMapper = objectMapper;
        this.jsonFactory = jsonFactory;
    }


    @PostConstruct
    public void init() {
        this.executorService = Executors.newFixedThreadPool(maxConcurrentUploads);
    }
    
    @Override
    @Retryable(value = {software.amazon.awssdk.services.s3.model.S3Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public Result<List<ExposureRecord>> downloadExposures(String s3Uri) {
        logger.info("Starting download of exposures from URI: {}", s3Uri);
        long startTime = System.currentTimeMillis();
        
        try {
            List<ExposureRecord> exposures;
            
            // Check if it's a local file URI
            if (s3Uri != null && s3Uri.startsWith("file://")) {
                exposures = downloadFromLocalFile(s3Uri);
            } else {
                // Parse S3 URI
                var parsed = com.bcbs239.regtech.core.infrastructure.filestorage.S3Utils.parseS3Uri(s3Uri);
                S3Reference s3Reference = parsed.map(p -> S3Reference.of(p.bucket(), p.key(), "0")).orElse(null);
                if (s3Reference == null) {
                    return Result.failure("S3_URI_INVALID", ErrorType.VALIDATION_ERROR, "Invalid S3 URI format: " + s3Uri, "s3_uri");
                }
                try (ResponseInputStream<GetObjectResponse> s3Object = coreS3Service.getObjectStream(s3Reference.bucket(), s3Reference.key())) {
                    exposures = downloadAndParseStreaming(s3Object);
                }
            }
            
            long duration = System.currentTimeMillis() - startTime;
            logger.info("Successfully downloaded {} exposures in {} ms", exposures.size(), duration);
            
            return Result.success(exposures);
            
        } catch (software.amazon.awssdk.services.s3.model.S3Exception e) {
            logger.error("S3 error downloading exposures from: {}", s3Uri, e);
            return Result.failure("S3_DOWNLOAD_ERROR", ErrorType.SYSTEM_ERROR, "Failed to download from S3: " + e.getMessage(), "s3_download");
        } catch (IOException e) {
            logger.error("IO error parsing exposures from: {}", s3Uri, e);
            return Result.failure("S3_PARSE_ERROR", ErrorType.SYSTEM_ERROR, "Failed to parse JSON: " + e.getMessage(), "json_parsing");
        } catch (Exception e) {
            logger.error("Unexpected error downloading exposures from: {}", s3Uri, e);
            return Result.failure("S3_DOWNLOAD_UNEXPECTED_ERROR", ErrorType.SYSTEM_ERROR, "Unexpected error downloading exposures: " + e.getMessage(), "system");
        }
    }
    
    /**
     * Downloads and parses exposures from a local file URI.
     * Supports multiple formats:
     * 1. New format with "exposures" and "bank_info" fields (BatchDataDTO)
     * 2. Old format with "loan_portfolio" field
     * 3. Direct array format
     */
    private List<ExposureRecord> downloadFromLocalFile(String fileUri) throws IOException {
        // Convert file:// URI to file path using proper parsing
        String filePath = parseFileUri(fileUri);

        logger.info("Reading exposures from local file: {}", filePath);
        
        java.nio.file.Path path = java.nio.file.Paths.get(filePath);
        if (!java.nio.file.Files.exists(path)) {
            throw new IOException("File not found: " + filePath);
        }
        
        // Parse JSON from local file
        try (JsonParser parser = jsonFactory.createParser(java.nio.file.Files.newInputStream(path))) {
            JsonNode rootNode = objectMapper.readTree(parser);
            
            // Support new format with bank_info at top level - deserialize to BatchDataDTO
            if (rootNode.has("exposures") && rootNode.has("bank_info")) {
                logger.info("Detected new BatchDataDTO format with exposures and bank_info fields");
                BatchDataDTO batchData = objectMapper.treeToValue(rootNode, BatchDataDTO.class);
                
                // Log bank information if available
                if (batchData.bankInfo() != null) {
                    logger.info("Processing batch for bank: {} (ABI: {}, LEI: {}), Report date: {}, Total exposures: {}",
                        batchData.bankInfo().bankName(),
                        batchData.bankInfo().abiCode(),
                        batchData.bankInfo().leiCode(),
                        batchData.bankInfo().reportDate(),
                        batchData.bankInfo().totalExposures());
                }
                
                // Convert ExposureDTOs to ExposureRecords
                List<ExposureRecord> exposures = batchData.exposures().stream()
                    .map(ExposureRecord::fromDTO)
                    .toList();
                
                logger.info("Successfully parsed {} exposures from BatchDataDTO format", exposures.size());
                return exposures;
            }
            
            // Backward compatibility: support old direct array format
            if (rootNode.isArray()) {
                logger.info("Detected legacy direct array format");
                return parseExposuresFromArray(rootNode);
            }
            
            // Backward compatibility: support old loan_portfolio format
            if (rootNode.has("loan_portfolio")) {
                logger.info("Detected legacy loan_portfolio format");
                JsonNode loanPortfolioNode = rootNode.get("loan_portfolio");
                return parseExposuresFromArray(loanPortfolioNode);
            }
            
            // Unsupported format
            List<String> fieldNames = new ArrayList<>();
            rootNode.fieldNames().forEachRemaining(fieldNames::add);
            String fieldsDescription = fieldNames.isEmpty() ? 
                "none (possibly empty object)" : 
                String.join(", ", fieldNames);
            
            String errorMessage = String.format(
                "Unsupported JSON format in file '%s'. Expected one of: " +
                "1) New format with 'exposures' and 'bank_info' fields, " +
                "2) Legacy format with 'loan_portfolio' field, " +
                "3) Direct array of exposures. " +
                "Found root node with fields: %s",
                filePath,
                fieldsDescription
            );
            throw new IOException(errorMessage);
        }
    }
    
    /**
     * Stores data to local filesystem
     */
    private S3Reference storeToLocalFile(String key, String content, Map<String, String> metadata) throws IOException {
        // Create full file path
        java.nio.file.Path basePath = java.nio.file.Paths.get(localBasePath);
        java.nio.file.Path fullPath = basePath.resolve(key);
        
        // Create parent directories if they don't exist
        java.nio.file.Files.createDirectories(fullPath.getParent());
        
        // Write content to file
        java.nio.file.Files.writeString(fullPath, content, StandardCharsets.UTF_8);
        
        logger.info("Successfully stored file to local path: {}", fullPath);
        
        // Return a file:// URI as S3Reference (bucket will be "local", key is the relative path)
        return S3Reference.of("local", key, "0");
    }
    
    @Override
    @Retryable(value = {software.amazon.awssdk.services.s3.model.S3Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public Result<S3Reference> storeDetailedResults(BatchId batchId, ValidationResult validationResult) {
        logger.info("Starting storage of detailed validation results for batch: {}", batchId.value());
        long startTime = System.currentTimeMillis();
        
        try {
            // Create key for detailed results
            String key = String.format("quality/quality_%s.json", batchId.value());
            
            // Serialize validation result to JSON
            String jsonContent = serializeValidationResult(validationResult);
            
            // Create metadata
            Map<String, String> metadata = createMetadata(batchId, validationResult);
            
            // Store based on storage type
            S3Reference reference;
            if ("local".equalsIgnoreCase(storageType)) {
                reference = storeToLocalFile(key, jsonContent, metadata);
            } else {
                coreS3Service.putString(resultsBucket, key, jsonContent, "application/json", metadata, encryptionKeyId);
                reference = S3Reference.of(resultsBucket, key, "0");
            }

            long duration = System.currentTimeMillis() - startTime;
            logger.info("Successfully stored detailed results for batch {} in {} ms", batchId.value(), duration);

            return Result.success(reference);
            
        } catch (software.amazon.awssdk.services.s3.model.S3Exception e) {
            logger.error("S3 error storing detailed results for batch: {}", batchId.value(), e);
            return Result.failure("S3_UPLOAD_ERROR", ErrorType.SYSTEM_ERROR, "Failed to upload to S3: " + e.getMessage(), "s3_upload");
        } catch (IOException e) {
            logger.error("IO error serializing results for batch: {}", batchId.value(), e);
            return Result.failure("S3_SERIALIZE_ERROR", ErrorType.SYSTEM_ERROR, "Failed to serialize validation results: " + e.getMessage(), "json_serialization");
        } catch (Exception e) {
            logger.error("Unexpected error storing results for batch: {}", batchId.value(), e);
            return Result.failure("S3_UPLOAD_UNEXPECTED_ERROR", ErrorType.SYSTEM_ERROR, "Unexpected error storing detailed results: " + e.getMessage(), "system");
        }
    }
    
    @Override
    public Result<S3Reference> storeDetailedResults(BatchId batchId, ValidationResult validationResult, java.util.Map<String, String> metadata) {
        logger.info("Starting storage of detailed validation results for batch: {} (with custom metadata)", batchId.value());
        long startTime = System.currentTimeMillis();

        try {
            // Create key for results
            String key = String.format("quality/quality_%s.json", batchId.value());
            
            // Serialize validation result to JSON
            String jsonContent = serializeValidationResult(validationResult);

            // Merge provided metadata with generated metadata
            Map<String, String> merged = createMetadata(batchId, validationResult);
            if (metadata != null) merged.putAll(metadata);

            // Store based on storage type
            S3Reference reference;
            if ("local".equalsIgnoreCase(storageType)) {
                reference = storeToLocalFile(key, jsonContent, merged);
            } else {
                coreS3Service.putString(resultsBucket, key, jsonContent, "application/json", merged, encryptionKeyId);
                reference = S3Reference.of(resultsBucket, key, "0");
            }

            long duration = System.currentTimeMillis() - startTime;
            logger.info("Successfully stored detailed results for batch {} in {} ms", batchId.value(), duration);

            return Result.success(reference);

        } catch (software.amazon.awssdk.services.s3.model.S3Exception e) {
            logger.error("S3 error storing detailed results for batch: {}", batchId.value(), e);
            return Result.failure("S3_UPLOAD_ERROR", ErrorType.SYSTEM_ERROR, "Failed to upload to S3: " + e.getMessage(), "s3_upload");
        } catch (IOException e) {
            logger.error("IO error serializing results for batch: {}", batchId.value(), e);
            return Result.failure("S3_SERIALIZE_ERROR", ErrorType.SYSTEM_ERROR, "Failed to serialize validation results: " + e.getMessage(), "json_serialization");
        } catch (Exception e) {
            logger.error("Unexpected error storing results for batch: {}", batchId.value(), e);
            return Result.failure("S3_UPLOAD_UNEXPECTED_ERROR", ErrorType.SYSTEM_ERROR, "Unexpected error storing detailed results: " + e.getMessage(), "system");
        }
    }
    
    @Override
    public Result<S3Reference> storeDetailedResults(BatchId batchId, ValidationResult validationResult, 
                                                     java.util.Map<String, String> metadata,
                                                     java.util.List<com.bcbs239.regtech.core.domain.recommendations.QualityInsight> recommendations) {
        logger.info("Starting storage of detailed validation results with recommendations for batch: {}", batchId.value());
        long startTime = System.currentTimeMillis();

        try {
            // Create key for results
            String key = String.format("quality/quality_%s.json", batchId.value());
            
            // Serialize validation result to JSON with recommendations
            String jsonContent = serializeValidationResultWithRecommendations(validationResult, recommendations);

            // Merge provided metadata with generated metadata
            Map<String, String> merged = createMetadata(batchId, validationResult);
            if (metadata != null) merged.putAll(metadata);
            merged.put("recommendations-count", String.valueOf(recommendations != null ? recommendations.size() : 0));

            // Store based on storage type
            S3Reference reference;
            if ("local".equalsIgnoreCase(storageType)) {
                reference = storeToLocalFile(key, jsonContent, merged);
            } else {
                coreS3Service.putString(resultsBucket, key, jsonContent, "application/json", merged, encryptionKeyId);
                reference = S3Reference.of(resultsBucket, key, "0");
            }

            long duration = System.currentTimeMillis() - startTime;
            logger.info("Successfully stored detailed results with {} recommendations for batch {} in {} ms", 
                recommendations != null ? recommendations.size() : 0, batchId.value(), duration);

            return Result.success(reference);

        } catch (software.amazon.awssdk.services.s3.model.S3Exception e) {
            logger.error("S3 error storing detailed results for batch: {}", batchId.value(), e);
            return Result.failure("S3_UPLOAD_ERROR", ErrorType.SYSTEM_ERROR, "Failed to upload to S3: " + e.getMessage(), "s3_upload");
        } catch (IOException e) {
            logger.error("IO error serializing results for batch: {}", batchId.value(), e);
            return Result.failure("S3_SERIALIZE_ERROR", ErrorType.SYSTEM_ERROR, "Failed to serialize validation results: " + e.getMessage(), "json_serialization");
        } catch (Exception e) {
            logger.error("Unexpected error storing results for batch: {}", batchId.value(), e);
            return Result.failure("S3_UPLOAD_UNEXPECTED_ERROR", ErrorType.SYSTEM_ERROR, "Unexpected error storing detailed results: " + e.getMessage(), "system");
        }
    }

    /**
     * Download and parse JSON using streaming to handle large files efficiently.
     * Supports multiple formats:
     * 1. New format with "exposures" and "bank_info" fields (BatchDataDTO)
     * 2. Old format with "loan_portfolio" field
     * 3. Direct array format
     */
    private List<ExposureRecord> downloadAndParseStreaming(ResponseInputStream<GetObjectResponse> inputStream) throws IOException {
        try (JsonParser parser = jsonFactory.createParser(inputStream)) {
            JsonNode rootNode = objectMapper.readTree(parser);
            
            // Support new format with bank_info at top level - deserialize to BatchDataDTO
            if (rootNode.has("exposures") && rootNode.has("bank_info")) {
                logger.info("Detected new BatchDataDTO format with exposures and bank_info fields");
                BatchDataDTO batchData = objectMapper.treeToValue(rootNode, BatchDataDTO.class);
                
                // Log bank information if available
                if (batchData.bankInfo() != null) {
                    logger.info("Processing batch for bank: {} (ABI: {}, LEI: {}), Report date: {}, Total exposures: {}",
                        batchData.bankInfo().bankName(),
                        batchData.bankInfo().abiCode(),
                        batchData.bankInfo().leiCode(),
                        batchData.bankInfo().reportDate(),
                        batchData.bankInfo().totalExposures());
                }
                
                // Convert ExposureDTOs to ExposureRecords
                List<ExposureRecord> exposures = batchData.exposures().stream()
                    .map(ExposureRecord::fromDTO)
                    .toList();
                
                logger.info("Successfully parsed {} exposures from BatchDataDTO format", exposures.size());
                return exposures;
            }
            
            // Backward compatibility: support old direct array format
            if (rootNode.isArray()) {
                logger.info("Detected legacy direct array format");
                return parseExposuresFromArray(rootNode);
            }
            
            // Backward compatibility: support old loan_portfolio format
            if (rootNode.has("loan_portfolio")) {
                logger.info("Detected legacy loan_portfolio format");
                JsonNode loanPortfolioNode = rootNode.get("loan_portfolio");
                return parseExposuresFromArray(loanPortfolioNode);
            }
            
            // Unsupported format
            List<String> fieldNames = new ArrayList<>();
            rootNode.fieldNames().forEachRemaining(fieldNames::add);
            String fieldsDescription = fieldNames.isEmpty() ? 
                "none (possibly empty object)" : 
                String.join(", ", fieldNames);
            
            String errorMessage = String.format(
                "Unsupported JSON format. Expected one of: " +
                "1) New format with 'exposures' and 'bank_info' fields, " +
                "2) Legacy format with 'loan_portfolio' field, " +
                "3) Direct array of exposures. " +
                "Found root node with fields: %s",
                fieldsDescription
            );
            throw new IOException(errorMessage);
        }
    }
    
    /**
     * Parse exposures from a JSON array node.
     * Used for backward compatibility with old formats.
     */
    private List<ExposureRecord> parseExposuresFromArray(JsonNode arrayNode) throws IOException {
        if (!arrayNode.isArray()) {
            throw new IOException("Expected JSON array but got: " + arrayNode.getNodeType());
        }
        
        List<ExposureRecord> exposures = new ArrayList<>();
        for (JsonNode exposureNode : arrayNode) {
            ExposureRecord exposure = parseExposureRecord(exposureNode);
            exposures.add(exposure);
            
            // Log progress for large files
            if (exposures.size() % 10000 == 0) {
                logger.debug("Parsed {} exposures so far", exposures.size());
            }
        }
        
        logger.info("Successfully parsed {} exposures from array format", exposures.size());
        return exposures;
    }
    
    /**
     * Parse a single exposure record from JSON node.
     */
    private ExposureRecord parseExposureRecord(JsonNode node) {
        // Require canonical camelCase field names for performance.
        String exposureId = getTextValue(node, "exposureId");
        String counterpartyId = getTextValue(node, "counterpartyId");
        String leiCode = getTextValue(node, "leiCode");
        String country = getTextValue(node, "country");
        String productType = getTextValue(node, "productType");
        
        // Try to get amount from various fields
        java.math.BigDecimal amount = null;
        if (node.has("amount")) {
            amount = node.get("amount").decimalValue();
        } else if (node.has("gross_exposure_amount")) {
            amount = node.get("gross_exposure_amount").decimalValue();
        } else if (node.has("loan_amount")) {
            amount = node.get("loan_amount").decimalValue();
        }
        
        return ExposureRecord.builder()
            .exposureId(exposureId)
            .counterpartyId(counterpartyId)
            .exposureAmount(amount)
            .currency(getTextValue(node, "currency"))
            .countryCode(country)
            .sector(getTextValue(node, "sector"))
            .counterpartyType(getTextValue(node, "counterpartyType"))
            .productType(productType)
            .counterpartyLei(leiCode)
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
     * Serialize validation result with recommendations to JSON string.
     */
    private String serializeValidationResultWithRecommendations(ValidationResult validationResult,
                                                                  java.util.List<com.bcbs239.regtech.core.domain.recommendations.QualityInsight> recommendations) throws IOException {
        // Start with base serialization
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("totalExposures", validationResult.totalExposures());
        resultMap.put("validExposures", validationResult.validExposures());
        resultMap.put("invalidExposures", validationResult.totalExposures() - validationResult.validExposures());
        resultMap.put("overallErrorCount", validationResult.allErrors().size());
        
        // Dimension scores
        if (validationResult.dimensionScores() != null) {
            Map<String, Object> dimensionScores = new HashMap<>();
            dimensionScores.put("accuracy", validationResult.dimensionScores().accuracy());
            dimensionScores.put("completeness", validationResult.dimensionScores().completeness());
            dimensionScores.put("consistency", validationResult.dimensionScores().consistency());
            dimensionScores.put("timeliness", validationResult.dimensionScores().timeliness());
            dimensionScores.put("uniqueness", validationResult.dimensionScores().uniqueness());
            dimensionScores.put("validity", validationResult.dimensionScores().validity());
            resultMap.put("dimensionScores", dimensionScores);
        }
        
        // Add recommendations
        if (recommendations != null && !recommendations.isEmpty()) {
            List<Map<String, Object>> recommendationsList = new ArrayList<>();
            for (com.bcbs239.regtech.core.domain.recommendations.QualityInsight insight : recommendations) {
                Map<String, Object> rec = new HashMap<>();
                rec.put("ruleId", insight.ruleId());
                rec.put("severity", insight.severity().name());
                rec.put("priority", insight.severity().getPriority());
                rec.put("icon", insight.severity().getIcon());
                rec.put("color", insight.severity().getColor());
                rec.put("message", insight.message());
                rec.put("actionItems", insight.actionItems());
                rec.put("locale", insight.locale().toLanguageTag());
                rec.put("hasActions", insight.hasActions());
                recommendationsList.add(rec);
            }
            resultMap.put("recommendations", recommendationsList);
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
                    return Result.failure(result.getError().orElse(ErrorDetail.of("S3_UPLOAD_ERROR", ErrorType.SYSTEM_ERROR, "Failed to upload one result", "s3_upload")));
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
            coreS3Service.headObject(bucket, key);
            return true;
        } catch (software.amazon.awssdk.services.s3.model.NoSuchKeyException e) {
            return false;
        } catch (software.amazon.awssdk.services.s3.model.S3Exception e) {
            logger.error("Error checking if S3 object exists: s3://{}/{}", bucket, key, e);
            return false;
        }
    }
    
    /**
     * Get object metadata.
     */
    public Map<String, String> getObjectMetadata(String bucket, String key) {
        try {
            var resp = coreS3Service.headObject(bucket, key);
            return resp.metadata();
        } catch (software.amazon.awssdk.services.s3.model.S3Exception e) {
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
        var parsed = com.bcbs239.regtech.core.infrastructure.filestorage.S3Utils.parseS3Uri(s3Uri);
        if (parsed.isEmpty()) return Result.failure(ErrorDetail.of("S3_URI_INVALID", ErrorType.VALIDATION_ERROR, "Invalid S3 URI: " + s3Uri, "s3.uri.invalid"));
        try {
            coreS3Service.headObject(parsed.get().bucket(), parsed.get().key());
            return Result.success(true);
        } catch (software.amazon.awssdk.services.s3.model.NoSuchKeyException e) {
            return Result.success(false);
        } catch (software.amazon.awssdk.services.s3.model.S3Exception e) {
            logger.error("S3 error checking object existence: s3://{}/{}", parsed.get().bucket(), parsed.get().key(), e);
            return Result.failure("S3_HEAD_ERROR", ErrorType.SYSTEM_ERROR, "Failed to check object: " + e.getMessage(), "s3_head");
        } catch (Exception e) {
            logger.error("Error checking S3 object existence: s3://{}/{}", parsed.get().bucket(), parsed.get().key(), e);
            return Result.failure("S3_HEAD_ERROR", ErrorType.SYSTEM_ERROR, "Failed to check object: " + e.getMessage(), "s3_head");
        }
    }

    @Override
    public Result<Long> getObjectSize(String s3Uri) {
        var parsed = com.bcbs239.regtech.core.infrastructure.filestorage.S3Utils.parseS3Uri(s3Uri);
        if (parsed.isEmpty()) return Result.failure("S3_URI_INVALID", ErrorType.VALIDATION_ERROR, "Invalid S3 URI: " + s3Uri, "s3_uri");
        try {
            var response = coreS3Service.headObject(parsed.get().bucket(), parsed.get().key());
            return Result.success(response.contentLength());
        } catch (software.amazon.awssdk.services.s3.model.NoSuchKeyException e) {
            return Result.failure("S3_OBJECT_NOT_FOUND", ErrorType.NOT_FOUND_ERROR, "Object not found: " + s3Uri, "s3_object");
        } catch (software.amazon.awssdk.services.s3.model.S3Exception e) {
            logger.error("S3 error getting object size for s3://{}/{}", parsed.get().bucket(), parsed.get().key(), e);
            return Result.failure("S3_HEAD_ERROR", ErrorType.SYSTEM_ERROR, "Failed to get object size: " + e.getMessage(), "s3_head");
        } catch (Exception e) {
            logger.error("Unexpected error getting object size for s3://{}/{}", parsed.get().bucket(), parsed.get().key(), e);
            return Result.failure("S3_HEAD_UNEXPECTED", ErrorType.SYSTEM_ERROR, "Unexpected error: " + e.getMessage(), "system");
        }
    }

    @Override
    public Result<List<ExposureRecord>> downloadExposures(String s3Uri, int expectedCount) {
        Result<List<ExposureRecord>> result = downloadExposures(s3Uri);
        if (result.isFailure()) return result;

        List<ExposureRecord> exposures = result.getValue().orElse(List.of());
        if (expectedCount >= 0 && exposures.size() != expectedCount) {
            return Result.failure("S3_EXPOSURE_COUNT_MISMATCH", ErrorType.VALIDATION_ERROR, String.format("Expected %d exposures but found %d in %s", expectedCount, exposures.size(), s3Uri), "exposure_count");
        }

        return Result.success(exposures);
    }
    
    /**
     * Parses a file URI to extract the file path, handling both Unix and Windows formats.
     * Also handles URL-encoded characters (e.g., %20 for spaces).
     * 
     * @param uri The file URI (e.g., "file:///C:/path" or "file:///path" or just "/path")
     * @return The file path suitable for Paths.get()
     */
    private String parseFileUri(String uri) {
        String path = uri;
        
        if (uri.startsWith("file://")) {
            // Remove file:// prefix
            path = uri.substring(7);
            
            // On Windows, file URIs look like file:///C:/path
            // After removing file://, we get /C:/path which needs to become C:/path
            if (path.startsWith("/") && path.length() > 2 && path.charAt(2) == ':') {
                // Windows path: /C:/path -> C:/path
                path = path.substring(1);
            }
        }
        
        // Decode URL-encoded characters (e.g., %20 -> space)
        try {
            path = java.net.URLDecoder.decode(path, StandardCharsets.UTF_8);
        } catch (Exception e) {
            logger.warn("Failed to URL decode path, using as-is [path:{},error:{}]", path, e.getMessage());
        }
        
        return path;
    }
}

