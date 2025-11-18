package com.bcbs239.regtech.riskcalculation.application.classification;

import com.bcbs239.regtech.riskcalculation.domain.calculation.CalculatedExposure;
import com.bcbs239.regtech.riskcalculation.domain.classification.SectorClassifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Application service for sector classification of exposures.
 * Uses DDD approach by delegating to domain classifiers.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SectorClassificationService {
    
    private final SectorClassifier sectorClassifier;
    
    /**
     * Classifies an exposure by sector category.
     * Uses DDD approach: ask the domain classifier to do the work.
     * 
     * @param exposure The exposure to classify
     */
    public void classify(CalculatedExposure exposure) {
        log.debug("Classifying exposure {} by sector", exposure.getId().value());
        
        // Ask the domain classifier to perform the classification
        exposure.setSectorCategory(
            sectorClassifier.classify(exposure.getSector())
        );
        
        log.debug("Exposure {} classified as sector category: {}", 
            exposure.getId().value(), exposure.getSectorCategory());
    }
}