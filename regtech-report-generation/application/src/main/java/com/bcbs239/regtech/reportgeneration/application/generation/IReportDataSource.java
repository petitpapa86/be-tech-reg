package com.bcbs239.regtech.reportgeneration.application.generation;

import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.reportgeneration.application.coordination.CalculationEventData;
import com.bcbs239.regtech.reportgeneration.application.coordination.QualityEventData;
import com.bcbs239.regtech.reportgeneration.domain.generation.CalculationResults;
import com.bcbs239.regtech.reportgeneration.domain.generation.QualityResults;

/**
 * Interface for fetching report data from external sources.
 * This port separates the application logic from the infrastructure concern of data retrieval.
 */
public interface IReportDataSource {
    /**
     * Fetch calculation data based on the event data.
     */
    Result<CalculationResults> fetchCalculationData(CalculationEventData event);

    /**
     * Fetch quality data based on the event data.
     */
    Result<QualityResults> fetchQualityData(QualityEventData event);
    
    /**
     * Fetch quality data based on the event data and a specific bank ID (for canonical overriding).
     */
    Result<QualityResults> fetchQualityData(QualityEventData event, String canonicalBankId);

    /**
     * Fetch all data required for comprehensive report generation
     */
    default Result<ComprehensiveReportData> fetchAllData(
            CalculationEventData calculationEvent,
            QualityEventData qualityEvent) {
        
        return fetchCalculationData(calculationEvent)
            .flatMap(calculationResults -> fetchQualityData(qualityEvent) // Note: Using default fetchQualityData here, might need canonicalBankId version if available in context
                .map(qualityResults -> ComprehensiveReportData.builder()
                    .calculationResults(calculationResults)
                    .qualityResults(qualityResults)
                    .build())
            );
    }
}
