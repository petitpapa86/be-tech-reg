package com.bcbs239.regtech.riskcalculation.domain.persistence;

import com.bcbs239.regtech.riskcalculation.domain.exposure.ExposureRecording;
import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.ExposureId;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for ExposureRecording aggregate
 * Defines persistence operations for exposure records
 * 
 * <p><strong>Migration Notice:</strong> This repository is transitioning to a file-first architecture.
 * Database persistence methods (save, saveAll) are deprecated in favor of JSON file storage.
 * Use {@link #loadFromJson(String)} to load exposures from JSON files.</p>
 * 
 * <p>Requirements: 5.1, 5.5</p>
 */
public interface ExposureRepository {
    
    /**
     * Save a single exposure recording
     * 
     * @param exposure the exposure to save
     * @param batchId the batch identifier this exposure belongs to
     * @deprecated As of version 2.0, database persistence is deprecated. 
     *             Exposure data is now stored in JSON files as the single source of truth.
     *             This method will be removed in a future release.
     *             Use JSON file storage via CalculationResultsStorageService instead.
     *             Requirement: 5.1
     */
    @Deprecated(since = "2.0", forRemoval = true)
    void save(ExposureRecording exposure, String batchId);
    
    /**
     * Save multiple exposure recordings in a batch
     * 
     * @param exposures list of exposures to save
     * @param batchId the batch identifier these exposures belong to
     * @deprecated As of version 2.0, database persistence is deprecated. 
     *             Exposure data is now stored in JSON files as the single source of truth.
     *             This method will be removed in a future release.
     *             Use JSON file storage via CalculationResultsStorageService instead.
     *             Requirement: 5.1
     */
    @Deprecated(since = "2.0", forRemoval = true)
    void saveAll(List<ExposureRecording> exposures, String batchId);
    
    /**
     * Find an exposure by its unique identifier
     * 
     * @param id the exposure identifier
     * @return Optional containing the exposure if found, empty otherwise
     */
    Optional<ExposureRecording> findById(ExposureId id);
    
    /**
     * Find all exposures for a given batch
     * 
     * @param batchId the batch identifier
     * @return list of exposures for the batch
     */
    List<ExposureRecording> findByBatchId(String batchId);
    
    /**
     * Load exposure recordings from JSON content.
     * This method parses the JSON file content and reconstructs ExposureRecording domain objects.
     * 
     * <p>The JSON content should follow the calculation results format with a "calculated_exposures" array.
     * Each exposure in the array will be converted to an ExposureRecording domain object.</p>
     * 
     * <p><strong>File-First Architecture:</strong> This method supports the new architecture where
     * JSON files are the single source of truth for detailed exposure data. The database stores
     * only minimal batch metadata.</p>
     * 
     * @param jsonContent the JSON string containing exposure data in calculation results format
     * @return list of ExposureRecording objects parsed from JSON
     * @throws IllegalArgumentException if JSON content is null, empty, or malformed
     * @throws CalculationResultsDeserializationException 
     *         if JSON structure is invalid or required fields are missing
     * 
     * <p>Requirement: 5.5 - Provide methods to download and parse JSON files for exposure details</p>
     */
    List<ExposureRecording> loadFromJson(String jsonContent);
}
