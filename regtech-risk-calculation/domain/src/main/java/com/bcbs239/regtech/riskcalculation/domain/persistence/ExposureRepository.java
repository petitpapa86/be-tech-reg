package com.bcbs239.regtech.riskcalculation.domain.persistence;

import com.bcbs239.regtech.riskcalculation.domain.exposure.ExposureRecording;
import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.ExposureId;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for ExposureRecording aggregate
 * Defines persistence operations for exposure records
 */
public interface ExposureRepository {
    
    /**
     * Save a single exposure recording
     * 
     * @param exposure the exposure to save
     * @param batchId the batch identifier this exposure belongs to
     */
    void save(ExposureRecording exposure, String batchId);
    
    /**
     * Save multiple exposure recordings in a batch
     * 
     * @param exposures list of exposures to save
     * @param batchId the batch identifier these exposures belong to
     */
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
}
