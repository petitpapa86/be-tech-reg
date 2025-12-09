package com.bcbs239.regtech.riskcalculation.infrastructure.database.repositories;

import com.bcbs239.regtech.riskcalculation.infrastructure.database.entities.ExposureEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for ExposureEntity
 */
@Repository
public interface SpringDataExposureRepository extends JpaRepository<ExposureEntity, String> {
    
    /**
     * Find all exposures for a given batch
     * 
     * @param batchId the batch identifier
     * @return list of exposure entities
     */
    List<ExposureEntity> findByBatchId(String batchId);
}
