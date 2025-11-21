package com.bcbs239.regtech.reportgeneration.application.aggregation;

import com.bcbs239.regtech.reportgeneration.domain.generation.CalculationResults;
import com.bcbs239.regtech.reportgeneration.domain.generation.QualityResults;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

/**
 * Comprehensive Report Data DTO
 * 
 * Aggregates both calculation and quality results for comprehensive report generation.
 * This DTO is the output of the ComprehensiveReportDataAggregator service.
 */
@Getter
@Builder
public class ComprehensiveReportData {
    
    private final String batchId;
    private final String bankId;
    private final String bankName;
    private final LocalDate reportingDate;
    private final CalculationResults calculationResults;
    private final QualityResults qualityResults;
    
    /**
     * Validate that both calculation and quality results are present
     */
    public void validate() {
        if (calculationResults == null) {
            throw new IllegalStateException("Calculation results are missing");
        }
        if (qualityResults == null) {
            throw new IllegalStateException("Quality results are missing");
        }
        
        // Validate consistency between calculation and quality results
        if (!calculationResults.batchId().value().equals(qualityResults.getBatchId().value())) {
            throw new IllegalStateException(
                String.format("Batch ID mismatch: calculation=%s, quality=%s",
                    calculationResults.batchId().value(),
                    qualityResults.getBatchId().value())
            );
        }
        
        if (!calculationResults.bankId().value().equals(qualityResults.getBankId().value())) {
            throw new IllegalStateException(
                String.format("Bank ID mismatch: calculation=%s, quality=%s",
                    calculationResults.bankId().value(),
                    qualityResults.getBankId().value())
            );
        }
    }
}
