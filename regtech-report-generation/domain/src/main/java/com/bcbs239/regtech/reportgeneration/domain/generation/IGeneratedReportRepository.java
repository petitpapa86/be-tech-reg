package com.bcbs239.regtech.reportgeneration.domain.generation;

import com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects.BatchId;
import com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects.ReportId;

import java.util.Optional;

/**
 * Repository interface for GeneratedReport aggregate
 * Defines the contract for persisting and retrieving generated reports
 */
public interface IGeneratedReportRepository {
    
    /**
     * Find a generated report by batch ID
     * 
     * @param batchId the batch identifier
     * @return Optional containing the report if found, empty otherwise
     */
    Optional<GeneratedReport> findByBatchId(BatchId batchId);
    
    /**
     * Find a generated report by report ID
     * 
     * @param reportId the report identifier
     * @return Optional containing the report if found, empty otherwise
     */
    Optional<GeneratedReport> findByReportId(ReportId reportId);
    
    /**
     * Save a generated report
     * 
     * @param report the report to save
     */
    void save(GeneratedReport report);
    
    /**
     * Check if a report exists for the given batch ID
     * 
     * @param batchId the batch identifier
     * @return true if a report exists, false otherwise
     */
    boolean existsByBatchId(BatchId batchId);
}
