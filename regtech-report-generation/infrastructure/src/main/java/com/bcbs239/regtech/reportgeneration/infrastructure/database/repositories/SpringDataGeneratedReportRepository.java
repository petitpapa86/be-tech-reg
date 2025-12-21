package com.bcbs239.regtech.reportgeneration.infrastructure.database.repositories;

import com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects.ReportStatus;
import com.bcbs239.regtech.reportgeneration.infrastructure.database.entities.GeneratedReportEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for GeneratedReportEntity.
 * Provides standard CRUD operations and custom query methods.
 */
@Repository
public interface SpringDataGeneratedReportRepository extends JpaRepository<GeneratedReportEntity, UUID> {
    
    /**
     * Find a generated report by batch ID
     * 
     * @param batchId the batch identifier
     * @return Optional containing the entity if found, empty otherwise
     */
    Optional<GeneratedReportEntity> findByBatchId(String batchId);
    
    /**
     * Check if a report exists for the given batch ID
     * 
     * @param batchId the batch identifier
     * @return true if a report exists, false otherwise
     */
    boolean existsByBatchId(String batchId);
    
    /**
     * Check if a report exists for the given batch ID with a specific status
     * Used for idempotency checks to prevent duplicate report generation
     * 
     * @param batchId the batch identifier
     * @param status the report status to check
     * @return true if a report exists with the given status, false otherwise
     */
    boolean existsByBatchIdAndStatus(String batchId, ReportStatus status);

    boolean existsByDataQualityEventId(String qualityEventId);
    boolean existsByRiskCalculationEventId(String riskCalculationEventId);
}
