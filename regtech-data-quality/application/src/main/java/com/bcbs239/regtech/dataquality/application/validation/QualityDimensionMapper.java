package com.bcbs239.regtech.dataquality.application.validation;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Maps data-quality domain QualityDimension to regtech-core QualityDimension.
 * 
 * This mapper enables communication between the data-quality module and the 
 * shared recommendation engine in regtech-core, which uses a different QualityDimension enum.
 */
@Component
public class QualityDimensionMapper {
    
    /**
     * Converts dimension scores from data-quality domain to regtech-core domain.
     * 
     * Maps QualityDimension enums between modules:
     * - dataquality.domain.quality.QualityDimension → core.domain.quality.QualityDimension
     */
    public Map<com.bcbs239.regtech.core.domain.quality.QualityDimension, BigDecimal> toCoreQualityDimensions(
            Map<com.bcbs239.regtech.dataquality.domain.quality.QualityDimension, BigDecimal> dataQualityDimensionScores) {
        
        Map<com.bcbs239.regtech.core.domain.quality.QualityDimension, BigDecimal> coreScores = new HashMap<>();
        
        for (Map.Entry<com.bcbs239.regtech.dataquality.domain.quality.QualityDimension, BigDecimal> entry : dataQualityDimensionScores.entrySet()) {
            // Map by name (enums have same values in both modules)
            String dimensionName = entry.getKey().name();
            com.bcbs239.regtech.core.domain.quality.QualityDimension coreDimension = 
                com.bcbs239.regtech.core.domain.quality.QualityDimension.valueOf(dimensionName);
            coreScores.put(coreDimension, entry.getValue());
        }
        
        return coreScores;
    }
}

