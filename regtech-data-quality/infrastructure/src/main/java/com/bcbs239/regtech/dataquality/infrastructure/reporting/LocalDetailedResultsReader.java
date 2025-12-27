package com.bcbs239.regtech.dataquality.infrastructure.reporting;

import com.bcbs239.regtech.dataquality.application.reporting.DetailedExposureResult;
import com.bcbs239.regtech.dataquality.application.reporting.StoredValidationResults;
import com.bcbs239.regtech.dataquality.application.reporting.StoredValidationResultsReader;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Infrastructure helper that loads the detailed validation results JSON.
 *
 * <p>This logic previously lived in the controller. It belongs in infrastructure
 * because it performs I/O and knows about storage paths/URI mapping.</p>
 */
@Component
public class LocalDetailedResultsReader implements StoredValidationResultsReader {

    private static final Logger logger = LoggerFactory.getLogger(LocalDetailedResultsReader.class);

    private final ObjectMapper objectMapper;
    private final String localStorageBasePath;

    public LocalDetailedResultsReader(
        ObjectMapper objectMapper,
        @Value("${data-quality.storage.local.base-path:${user.dir}/data/quality}") String localStorageBasePath
    ) {
        this.objectMapper = objectMapper;
        this.localStorageBasePath = localStorageBasePath;
    }

    /**
     * Loads the stored validation results from a local-storage S3-style URI.
     *
     * <p>Supported URI format: s3://local/&lt;relativePath&gt;</p>
     */
    @Override
    public StoredValidationResults load(String detailsUri) {
        try {
            return loadInternal(detailsUri);
        } catch (Exception e) {
            logger.warn("Failed to load stored validation results from {}: {}", detailsUri, e.getMessage());
            return null;
        }
    }

    /**
     * Convenience method for callers that only need exposureResults.
     */
    public List<DetailedExposureResult> loadFromDetailsUri(String detailsUri) throws IOException {
        return loadInternal(detailsUri).exposureResults();
    }

    private StoredValidationResults loadInternal(String detailsUri) throws IOException {
        if (detailsUri == null || !detailsUri.startsWith("s3://local/")) {
            return null;
        }

        String relativePath = detailsUri.replace("s3://local/", "");
        Path filePath = Paths.get(localStorageBasePath, relativePath);

        if (!Files.exists(filePath)) {
            logger.warn("Detailed results file not found: {}", filePath);
            return null;
        }

        String jsonContent = Files.readString(filePath);
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
