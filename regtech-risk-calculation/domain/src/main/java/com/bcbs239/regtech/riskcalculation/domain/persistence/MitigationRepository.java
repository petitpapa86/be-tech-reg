package com.bcbs239.regtech.riskcalculation.domain.persistence;

import com.bcbs239.regtech.riskcalculation.domain.protection.RawMitigationData;
import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.ExposureId;

import java.util.List;

/**
 * Repository interface for credit risk mitigation data
 * Defines persistence operations for mitigations
 */
public interface MitigationRepository {
    
    /**
     * Save a single mitigation for an exposure
     * 
     * @param exposureId the exposure this mitigation applies to
     * @param batchId the batch identifier
     * @param mitigation the mitigation data to save
     */
    void save(ExposureId exposureId, String batchId, RawMitigationData mitigation);
    
    /**
     * Save multiple mitigations for an exposure
     * 
     * @param exposureId the exposure these mitigations apply to
     * @param batchId the batch identifier
     * @param mitigations list of mitigation data to save
     */
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
}
