package com.bcbs239.regtech.reportgeneration.infrastructure.database.repositories;

import com.bcbs239.regtech.reportgeneration.domain.generation.GeneratedReport;
import com.bcbs239.regtech.reportgeneration.domain.generation.IGeneratedReportRepository;
import com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects.BatchId;
import com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects.ReportId;
import com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects.ReportStatus;
import com.bcbs239.regtech.reportgeneration.infrastructure.database.entities.GeneratedReportEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * JPA repository implementation for GeneratedReport aggregate.
 * Handles mapping between domain aggregate and JPA entity.
 * 
 * Responsibilities:
 * - Delegate persistence operations to Spring Data repository
 * - Use entity's fromDomain/toDomain methods for mapping
 * - Handle exceptions and logging
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class JpaGeneratedReportRepository implements IGeneratedReportRepository {

    private final SpringDataGeneratedReportRepository springDataRepository;

    @Override
    @Transactional
    public void save(GeneratedReport report) {
        try {
            // CRITICAL FIX: Check for existing report by batch_id since batch_id is unique
            Optional<GeneratedReportEntity> existing = springDataRepository.findByBatchId(report.getBatchId().value());
            
            if (existing.isPresent()) {
                // Report already exists for this batch - update it instead of creating duplicate
                GeneratedReportEntity entity = existing.get();
                // Update entity from domain (preserves version for optimistic locking)
                entity.updateFromDomain(report);
                
                springDataRepository.save(entity);
                log.info("Updated existing generated report: reportId={}, batchId={}, status={}", 
                        report.getReportId().value(), 
                        report.getBatchId().value(), 
                        report.getStatus());
            } else {
                // No existing report for this batch - create new one
                GeneratedReportEntity entity = GeneratedReportEntity.fromDomain(report);
                entity.setVersion(0L);  // CRITICAL: Set version for new entities
                springDataRepository.save(entity);
                
                log.info("Created new generated report: reportId={}, batchId={}, status={}", 
                        report.getReportId().value(), 
                        report.getBatchId().value(), 
                        report.getStatus());
            }
                    
        } catch (Exception e) {
            log.error("Error saving generated report: reportId={}, batchId={}", 
                    report.getReportId().value(), 
                    report.getBatchId().value(), 
                    e);
            throw new RuntimeException("Failed to save generated report", e);
        }
    }

    @Override
    public Optional<GeneratedReport> findByBatchId(BatchId batchId) {
        try {
            return springDataRepository.findByBatchId(batchId.value())
                    .map(GeneratedReportEntity::toDomain);
                    
        } catch (Exception e) {
            log.error("Error finding generated report by batch ID: batchId={}", batchId.value(), e);
            return Optional.empty();
        }
    }

    @Override
    public Optional<GeneratedReport> findByReportId(ReportId reportId) {
        try {
            return springDataRepository.findById(reportId.value())
                    .map(GeneratedReportEntity::toDomain);
                    
        } catch (Exception e) {
            log.error("Error finding generated report by report ID: reportId={}", reportId.value(), e);
            return Optional.empty();
        }
    }

    @Override
    public boolean existsByBatchId(BatchId batchId) {
        try {
            return springDataRepository.existsByBatchId(batchId.value());
            
        } catch (Exception e) {
            log.error("Error checking if generated report exists: batchId={}", batchId.value(), e);
            return false;
        }
    }

    @Override
    public boolean existsByBatchIdAndStatus(BatchId batchId, ReportStatus status) {
        try {
            return springDataRepository.existsByBatchIdAndStatus(batchId.value(), status);
            
        } catch (Exception e) {
            log.error("Error checking if generated report exists with status: batchId={}, status={}", 
                    batchId.value(), status, e);
            return false;
        }
    }

    @Override
    public boolean existsByDataQualityEventId(String dataQualityEventId) {
        try {
            return springDataRepository.existsByDataQualityEventId(dataQualityEventId);

        } catch (Exception e) {
            log.error("Error checking if generated report exists by data quality event ID: dataQualityEventId={}",
                    dataQualityEventId, e);
            return false;
        }
    }

    @Override
    public boolean existsByRiskCalculationEventId(String riskCalculationEventId) {
        try {
            return springDataRepository.existsByRiskCalculationEventId(riskCalculationEventId);
        } catch (Exception e) {
            log.error("Error checking if generated report exists by risk calculation event ID: riskCalculationEventId={}",
                    riskCalculationEventId, e);
            return false;
        }
    }
}
