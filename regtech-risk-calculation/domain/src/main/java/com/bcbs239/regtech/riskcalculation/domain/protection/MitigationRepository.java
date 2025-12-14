package com.bcbs239.regtech.riskcalculation.domain.protection;

import com.bcbs239.regtech.riskcalculation.domain.exposure.CalculationResultsDeserializationException;
import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.ExposureId;

import java.util.List;

/**
 * Repository interface for credit risk mitigation data
 * Defines persistence operations for mitigations
 * 
 * <p><strong>Migration Notice:</strong> This repository is transitioning to a file-first architecture.
 * Database persistence methods (save, saveAll) are deprecated in favor of JSON file storage.
 * Use {@link #loadFromJson(String)} to load mitigations from JSON files.</p>
 * 
 * <p>Requirements: 5.2, 5.5</p>
 */
public interface MitigationRepository {
    
    /**
     * Save a single mitigation for an exposure
     * 
     * @param exposureId the exposure this mitigation applies to
     * @param batchId the batch identifier
     * @param mitigation the mitigation data to save
     * @deprecated As of version 2.0, database persistence is deprecated. 
     *             Mitigation data is now stored in JSON files as the single source of truth.
     *             This method will be removed in a future release.
     *             Use JSON file storage via CalculationResultsStorageService instead.
     *             Requirement: 5.2
     */
    @Deprecated(since = "2.0", forRemoval = true)
    void save(ExposureId exposureId, String batchId, RawMitigationData mitigation);
    
    /**
     * Save multiple mitigations for an exposure
     * 
     * @param exposureId the exposure these mitigations apply to
     * @param batchId the batch identifier
     * @param mitigations list of mitigation data to save
     * @deprecated As of version 2.0, database persistence is deprecated. 
     *             Mitigation data is now stored in JSON files as the single source of truth.
     *             This method will be removed in a future release.
     *             Use JSON file storage via CalculationResultsStorageService instead.
     *             Requirement: 5.2
     */
    @Deprecated(since = "2.0", forRemoval = true)
    void saveAll(ExposureId exposureId, String batchId, List<RawMitigationData> mitigations);
    
    /**
     * Find all mitigations for a given exposure
     * 
     * @param exposureId the exposure identifier
     * @return list of mitigations for the exposure
     */
    List<RawMitigationData> findByExposureId(ExposureId exposureId);
    
    /**
     * Find all mitigations for a given batch
     * 
     * @param batchId the batch identifier
     * @return list of mitigations for the batch
     */
    List<RawMitigationData> findByBatchId(String batchId);
    
    /**
     * Load mitigation data from JSON content.
     * This method parses the JSON file content and reconstructs RawMitigationData domain objects.
     * 
     * <p>The JSON content should follow the calculation results format with a "calculated_exposures" array.
     * Each exposure in the array contains a "mitigations" array with mitigation details.
     * The mitigations will be extracted and converted to RawMitigationData domain objects.</p>
     * 
     * <p><strong>File-First Architecture:</strong> This method supports the new architecture where
     * JSON files are the single source of truth for detailed mitigation data. The database stores
     * only minimal batch metadata.</p>
     * 
     * <p><strong>Note:</strong> The JSON format stores EUR-converted mitigation values from calculation
     * results. Since RawMitigationData expects original currency values, this method reconstructs
     * mitigations with EUR as the currency. This is acceptable because the JSON serves as the
     * source of truth for calculated mitigation amounts.</p>
     * 
     * @param jsonContent the JSON string containing mitigation data in calculation results format
     * @return list of RawMitigationData objects parsed from JSON
     * @throws IllegalArgumentException if JSON content is null, empty, or malformed
     * @throws CalculationResultsDeserializationException
     *         if JSON structure is invalid or required fields are missing
     * 
     * <p>Requirement: 5.5 - Provide methods to download and parse JSON files for mitigation details</p>
     */
    List<RawMitigationData> loadFromJson(String jsonContent);
}
