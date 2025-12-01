package com.bcbs239.regtech.riskcalculation.infrastructure.database.repositories;

import com.bcbs239.regtech.riskcalculation.infrastructure.database.entities.MitigationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for MitigationEntity
 */
@Repository
public interface SpringDataMitigationRepository extends JpaRepository<MitigationEntity, Long> {
    
    /**
     * Find all mitigations for a given exposure
     * 
     * @param exposureId the exposure identifier
     * @return list of mitigation entities
     */
    List<MitigationEntity> findByExposureId(String exposureId);
    
    /**
     * Find all mitigations for a given batch
     * 
     * @param batchId the batch identifier
     * @return list of mitigation entities
     */
    List<MitigationEntity> findByBatchId(String batchId);
}
