package com.bcbs239.regtech.riskcalculation.application.classification;

import com.bcbs239.regtech.riskcalculation.domain.calculation.CalculatedExposure;
import com.bcbs239.regtech.riskcalculation.domain.classification.GeographicClassifier;
import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.BankId;
import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.Country;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Application service for geographic classification of exposures.
 * Uses DDD approach by delegating to domain classifiers.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GeographicClassificationService {
    
    private final GeographicClassifier geographicClassifier;
    
    /**
     * Classifies an exposure by geographic region.
     * Uses DDD approach: ask the domain classifier to do the work.
     * 
     * @param exposure The exposure to classify
     * @param bankId The bank ID to determine home country context
     */
    public void classify(CalculatedExposure exposure, BankId bankId) {
        log.debug("Classifying exposure {} geographically for bank {}", 
            exposure.getId().value(), bankId.value());
        
        // Determine bank home country (assuming Italian bank for now)
        Country bankHomeCountry = new Country("IT");
        
        // Ask the domain classifier to perform the classification
        exposure.setGeographicRegion(
            geographicClassifier.classify(exposure.getCountry(), bankHomeCountry)
        );
        
        log.debug("Exposure {} classified as geographic region: {}", 
            exposure.getId().value(), exposure.getGeographicRegion());
    }
}