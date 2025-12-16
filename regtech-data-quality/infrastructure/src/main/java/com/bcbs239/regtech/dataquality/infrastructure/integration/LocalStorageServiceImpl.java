package com.bcbs239.regtech.dataquality.infrastructure.integration;

import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.core.domain.shared.dto.BatchDataDTO;
import com.bcbs239.regtech.dataquality.application.integration.S3StorageService;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Local filesystem implementation of S3StorageService for development.
 */
@Service
@ConditionalOnProperty(name = "data-quality.storage.type", havingValue = "local", matchIfMissing = true)
public class LocalStorageServiceImpl implements S3StorageService {
    
    private static final Logger logger = LoggerFactory.getLogger(LocalStorageServiceImpl.class);
    
    private final ObjectMapper objectMapper;
    private final JsonFactory jsonFactory;
    
    @Value("${data-quality.storage.local.base-path:${user.dir}/data/quality}")
    private String localBasePath;

    public LocalStorageServiceImpl(ObjectMapper objectMapper, JsonFactory jsonFactory) {
        this.objectMapper = objectMapper;
        this.jsonFactory = jsonFactory;
    }

    @Override
    public Result<List<ExposureRecord>> downloadExposures(String s3Uri) {
        logger.info("Starting download of exposures from local URI: {}", s3Uri);
        long startTime = System.currentTimeMillis();
        
        try {
            List<ExposureRecord> exposures = downloadFromLocalFile(s3Uri);
            
            long duration = System.currentTimeMillis() - startTime;
            logger.info("Successfully downloaded {} exposures in {} ms", exposures.size(), duration);
            
            return Result.success(exposures);
            
        } catch (IOException e) {
            logger.error("IO error parsing exposures from: {}", s3Uri, e);
            return Result.failure("LOCAL_PARSE_ERROR", ErrorType.SYSTEM_ERROR, "Failed to parse JSON: " + e.getMessage(), "json_parsing");
        } catch (Exception e) {
            logger.error("Unexpected error downloading exposures from: {}", s3Uri, e);
            return Result.failure("LOCAL_DOWNLOAD_UNEXPECTED_ERROR", ErrorType.SYSTEM_ERROR, "Unexpected error downloading exposures: " + e.getMessage(), "system");
        }
    }

    private List<ExposureRecord> downloadFromLocalFile(String fileUri) throws IOException {
        String filePath = fileUri.replace("file:///", "").replace("file://", "");
        filePath = java.net.URLDecoder.decode(filePath, StandardCharsets.UTF_8.name());

        logger.info("Reading exposures from local file: {}", filePath);
        
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            throw new IOException("File not found: " + filePath);
        }
        
        try (JsonParser parser = jsonFactory.createParser(Files.newInputStream(path))) {
            JsonNode rootNode = objectMapper.readTree(parser);
            
            if (rootNode.has("exposures") && rootNode.has("bank_info")) {
                logger.info("Detected new BatchDataDTO format");
                BatchDataDTO batchData = objectMapper.treeToValue(rootNode, BatchDataDTO.class);
                
                List<ExposureRecord> exposures = batchData.exposures().stream()
                    .map(ExposureRecord::fromDTO)
                    .toList();
                
                logger.info("Successfully parsed {} exposures", exposures.size());
                return exposures;
            }
            
            if (rootNode.isArray()) {
                logger.info("Detected legacy array format");
                return parseExposuresFromArray(rootNode);
            }
            
            if (rootNode.has("loan_portfolio")) {
                logger.info("Detected legacy loan_portfolio format");
                return parseExposuresFromArray(rootNode.get("loan_portfolio"));
            }
            
            throw new IOException("Unsupported JSON format");
        }
    }

    private List<ExposureRecord> parseExposuresFromArray(JsonNode arrayNode) throws IOException {
        if (!arrayNode.isArray()) {
            throw new IOException("Expected JSON array");
        }
        
        List<ExposureRecord> exposures = new ArrayList<>();
        for (JsonNode exposureNode : arrayNode) {
            ExposureRecord exposure = parseExposureRecord(exposureNode);
            exposures.add(exposure);
        }
        
        return exposures;
    }

    private ExposureRecord parseExposureRecord(JsonNode node) {
        String exposureId = getTextValue(node, "exposureId");
        String counterpartyId = getTextValue(node, "counterpartyId");
        String leiCode = getTextValue(node, "leiCode");
        String country = getTextValue(node, "country");
        String productType = getTextValue(node, "productType");
        
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
            .amount(amount)
            .currency(getTextValue(node, "currency"))
            .country(country)
            .sector(getTextValue(node, "sector"))
            .counterpartyType(getTextValue(node, "counterpartyType"))
            .productType(productType)
            .leiCode(leiCode)
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

    private String getTextValue(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            if (node.has(fieldName) && !node.get(fieldName).isNull()) {
                return node.get(fieldName).asText();
            }
        }
        return null;
    }

    @Override
    public Result<List<ExposureRecord>> downloadExposures(String s3Uri, int expectedCount) {
        Result<List<ExposureRecord>> result = downloadExposures(s3Uri);
        
        if (result.isSuccess()) {
            List<ExposureRecord> exposures = result.getValue().orElse(List.of());
            if (exposures.size() != expectedCount) {
                logger.warn("Exposure count mismatch: expected {}, got {}", expectedCount, exposures.size());
            }
        }
        
        return result;
    }

    @Override
    public Result<Boolean> objectExists(String s3Uri) {
        try {
            String filePath = s3Uri.replace("file:///", "").replace("file://", "");
            filePath = java.net.URLDecoder.decode(filePath, StandardCharsets.UTF_8.name());
            
            Path path = Paths.get(filePath);
            boolean exists = Files.exists(path);
            
            logger.debug("Checked file existence: {} -> {}", filePath, exists);
            return Result.success(exists);
            
        } catch (Exception e) {
            logger.error("Error checking file existence for: {}", s3Uri, e);
            return Result.failure("LOCAL_EXISTS_CHECK_ERROR", ErrorType.SYSTEM_ERROR, 
                "Failed to check file existence: " + e.getMessage(), "file_check");
        }
    }

    @Override
    public Result<Long> getObjectSize(String s3Uri) {
        try {
            String filePath = s3Uri.replace("file:///", "").replace("file://", "");
            filePath = java.net.URLDecoder.decode(filePath, StandardCharsets.UTF_8.name());
            
            Path path = Paths.get(filePath);
            
            if (!Files.exists(path)) {
                logger.error("File not found: {}", filePath);
                return Result.failure("LOCAL_FILE_NOT_FOUND", ErrorType.SYSTEM_ERROR, 
                    "File not found: " + filePath, "file_not_found");
            }
            
            long size = Files.size(path);
            logger.debug("File size for {}: {} bytes", filePath, size);
            
            return Result.success(size);
            
        } catch (IOException e) {
            logger.error("IO error getting file size for: {}", s3Uri, e);
            return Result.failure("LOCAL_SIZE_CHECK_ERROR", ErrorType.SYSTEM_ERROR, 
                "Failed to get file size: " + e.getMessage(), "file_size_check");
        } catch (Exception e) {
            logger.error("Unexpected error getting file size for: {}", s3Uri, e);
            return Result.failure("LOCAL_SIZE_CHECK_UNEXPECTED_ERROR", ErrorType.SYSTEM_ERROR, 
                "Unexpected error getting file size: " + e.getMessage(), "system");
        }
    }

    @Override
    public Result<S3Reference> storeDetailedResults(BatchId batchId, ValidationResult validationResult) {
        return storeDetailedResults(batchId, validationResult, new HashMap<>());
    }

    @Override
    public Result<S3Reference> storeDetailedResults(BatchId batchId, ValidationResult validationResult, Map<String, String> metadata) {
        logger.info("Starting storage of detailed validation results for batch: {}", batchId.value());
        long startTime = System.currentTimeMillis();
        
        try {
            String key = String.format("quality/quality_%s.json", batchId.value());
            String jsonContent = serializeValidationResult(validationResult);
            
            S3Reference reference = storeToLocalFile(key, jsonContent, metadata);

            long duration = System.currentTimeMillis() - startTime;
            logger.info("Successfully stored detailed results for batch {} in {} ms", batchId.value(), duration);

            return Result.success(reference);
            
        } catch (IOException e) {
            logger.error("IO error storing results for batch: {}", batchId.value(), e);
            return Result.failure("LOCAL_STORE_ERROR", ErrorType.SYSTEM_ERROR, "Failed to store to local filesystem: " + e.getMessage(), "local_storage");
        } catch (Exception e) {
            logger.error("Unexpected error storing results for batch: {}", batchId.value(), e);
            return Result.failure("LOCAL_STORE_UNEXPECTED_ERROR", ErrorType.SYSTEM_ERROR, "Unexpected error storing detailed results: " + e.getMessage(), "system");
        }
    }

    private S3Reference storeToLocalFile(String key, String content, Map<String, String> metadata) throws IOException {
        Path basePath = Paths.get(localBasePath);
        Path fullPath = basePath.resolve(key);
        
        Files.createDirectories(fullPath.getParent());
        Files.writeString(fullPath, content, StandardCharsets.UTF_8);
        
        logger.info("Successfully stored file to local path: {}", fullPath);
        
        return S3Reference.of("local", key, "0");
    }

    private String serializeValidationResult(ValidationResult validationResult) throws IOException {
        Map<String, Object> resultMap = new HashMap<>();
        
        resultMap.put("totalExposures", validationResult.totalExposures());
        resultMap.put("validExposures", validationResult.validExposures());
        resultMap.put("totalErrors", validationResult.allErrors().size());
        resultMap.put("timestamp", Instant.now().toString());
        
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
        
        List<Map<String, Object>> exposureResults = new ArrayList<>();
        int count = 0;
        for (Map.Entry<String, ExposureValidationResult> entry : validationResult.exposureResults().entrySet()) {
            if (count >= 10000) break;
            
            Map<String, Object> exposureResult = new HashMap<>();
            exposureResult.put("exposureId", entry.getKey());
            exposureResult.put("isValid", entry.getValue().isValid());
            exposureResult.put("errorCount", entry.getValue().errors().size());

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
}
