package com.bcbs239.regtech.riskcalculation.domain.calculation;

import com.bcbs239.regtech.core.domain.shared.valueobjects.BankInfo;
import com.bcbs239.regtech.riskcalculation.domain.exposure.ExposureRecording;
import java.util.List;

/**
 * Domain interface for batch data parsing.
 * Parses JSON directly to domain objects for efficiency.
 */
public interface BatchDataParsing {
    /**
     * Parse batch data from JSON content directly to domain objects.
     * 
     * @param jsonContent The JSON content to parse
     * @return Parsed batch data containing domain objects
     * @throws BatchDataParsingException if parsing fails
     */
    ParsedBatchDomainData parseBatchData(String jsonContent);
    
    /**
     * Record containing parsed domain objects.
     * Replaces the DTO-based ParsedBatchData for better performance.
     */
    record ParsedBatchDomainData(
        List<ExposureRecording> exposures,
        java.util.Map<String, List<com.bcbs239.regtech.core.domain.shared.dto.CreditRiskMitigationDTO>> mitigationsByExposure,
        BankInfo bankInfo,
        int totalExposures
    ) {}
}
