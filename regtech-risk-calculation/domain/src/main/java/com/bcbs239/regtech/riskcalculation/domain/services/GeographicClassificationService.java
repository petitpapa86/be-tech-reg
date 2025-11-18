package com.bcbs239.regtech.riskcalculation.domain.services;

import com.bcbs239.regtech.riskcalculation.domain.shared.enums.GeographicRegion;
import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.Country;

/**
 * Domain service for geographic classification of exposures
 * Classifies exposures into ITALY, EU_OTHER, or NON_EUROPEAN regions
 * 
 * Requirements: 3.1 - Geographic region classification based on country codes
 */
public interface GeographicClassificationService {
    
    /**
     * Classify a country into a geographic region
     * 
     * @param country The country to classify
     * @param bankHomeCountry The bank's home country (used to identify home country exposures)
     * @return The geographic region classification (ITALY, EU_OTHER, or NON_EUROPEAN)
     */
    GeographicRegion classify(Country country, Country bankHomeCountry);
}