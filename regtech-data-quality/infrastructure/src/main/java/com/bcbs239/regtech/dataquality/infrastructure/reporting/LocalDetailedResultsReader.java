package com.bcbs239.regtech.dataquality.infrastructure.reporting;

import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.core.domain.storage.IStorageService;
import com.bcbs239.regtech.core.domain.storage.StorageUri;
import com.bcbs239.regtech.dataquality.application.reporting.DetailedExposureResult;
import com.bcbs239.regtech.dataquality.application.reporting.StoredValidationResults;
import com.bcbs239.regtech.dataquality.application.reporting.StoredValidationResultsReader;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Infrastructure adapter that loads detailed validation results using shared storage service.
 *
 * <p>Uses the centralized IStorageService from regtech-core to eliminate duplicate storage logic.</p>
 * <p>Supports S3, local filesystem, and memory storage based on URI scheme.</p>
 */
@Component
public class LocalDetailedResultsReader implements StoredValidationResultsReader {

    private static final Logger logger = LoggerFactory.getLogger(LocalDetailedResultsReader.class);

    private final IStorageService storageService;
    private final ObjectMapper objectMapper;

    public LocalDetailedResultsReader(
        IStorageService storageService,
        ObjectMapper objectMapper
    ) {
        this.storageService = storageService;
        this.objectMapper = objectMapper;
    }

    /**
     * Loads the stored validation results from any supported storage URI.
     *
     * <p>Supported URI formats:</p>
     * <ul>
     *   <li>s3://bucket/key (S3 or S3-compatible storage)</li>
     *   <li>file:///absolute/path (local filesystem)</li>
     *   <li>C:/absolute/path (Windows local filesystem)</li>
     *   <li>s3://local/path (legacy format, mapped to local storage)</li>
     * </ul>
     *
     * @param detailsUri Storage URI pointing to the validation results JSON
     * @return Parsed validation results, or null if loading fails
     */
    @Override
    public StoredValidationResults load(String detailsUri) {
        if (detailsUri == null || detailsUri.isBlank()) {
            logger.warn("Cannot load validation results: detailsUri is null or empty");
            return null;
        }

        try {
            // Parse URI using shared StorageUri
            StorageUri uri = StorageUri.parse(detailsUri);

            // Download JSON content using shared storage service
            Result<String> downloadResult = storageService.download(uri);
            if (downloadResult.isFailure()) {
                logger.warn("Failed to download validation results from '{}': {}", 
                    detailsUri, downloadResult.getError().orElseThrow().getMessage());
                return null;
            }

            String jsonContent = downloadResult.getValueOrThrow();

            // Parse JSON structure
            return parseValidationResults(jsonContent);

        } catch (IllegalArgumentException e) {
            logger.warn("Invalid storage URI '{}': {}", detailsUri, e.getMessage());
            return null;
        } catch (Exception e) {
            logger.warn("Unexpected error loading validation results from '{}': {}", 
                detailsUri, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Convenience method for callers that only need exposureResults.
     *
     * @param detailsUri Storage URI pointing to the validation results JSON
     * @return List of exposure results, or empty list if loading fails
     */
    public List<DetailedExposureResult> loadFromDetailsUri(String detailsUri) {
        StoredValidationResults results = load(detailsUri);
        return (results != null) ? results.exposureResults() : List.of();
    }

    /**
     * Parses the validation results JSON structure.
     *
     * @param jsonContent Raw JSON content
     * @return Parsed validation results
     * @throws Exception if JSON parsing fails
     */
    private StoredValidationResults parseValidationResults(String jsonContent) throws Exception {
        JsonNode rootNode = objectMapper.readTree(jsonContent);

        int totalExposures = rootNode.path("totalExposures").asInt(0);
        int validExposures = rootNode.path("validExposures").asInt(0);
        int totalErrors = rootNode.path("totalErrors").asInt(0);

        JsonNode exposureResultsNode = rootNode.get("exposureResults");
        List<DetailedExposureResult> exposureResults = (exposureResultsNode != null && exposureResultsNode.isArray())
            ? objectMapper.convertValue(exposureResultsNode, new TypeReference<List<DetailedExposureResult>>() {})
            : List.of();

        JsonNode batchErrorsNode = rootNode.get("batchErrors");
        List<DetailedExposureResult.DetailedError> batchErrors = (batchErrorsNode != null && batchErrorsNode.isArray())
            ? objectMapper.convertValue(batchErrorsNode, new TypeReference<List<DetailedExposureResult.DetailedError>>() {})
            : List.of();

        return new StoredValidationResults(totalExposures, validExposures, totalErrors, exposureResults, batchErrors);
    }
}
