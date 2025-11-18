package com.bcbs239.regtech.riskcalculation.domain.classification;

import com.bcbs239.regtech.riskcalculation.domain.shared.enums.GeographicRegion;
import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.Country;

import java.util.Objects;

/**
 * Domain service for classifying exposures by geographic region
 * Implements business rules for geographic classification
 */
public class GeographicClassifier {
    
    private final Country homeCountry;
    
    public GeographicClassifier(Country homeCountry) {
        this.homeCountry = Objects.requireNonNull(homeCountry, "Home country cannot be null");
    }
    
    /**
     * Classify a country into a geographic region
     * Business rules:
     * - Home country (Italy) -> ITALY
     * - Other EU countries -> EU_OTHER  
     * - Non-EU countries -> NON_EUROPEAN
     */
    public GeographicRegion classify(Country country) {
        Objects.requireNonNull(country, "Country cannot be null");
        
        // Check if it's the home country (Italy)
        if (country.equals(homeCountry)) {
            return GeographicRegion.ITALY;
        }
        
        // Check if it's in the European Union
        if (country.isInEuropeanUnion()) {
            return GeographicRegion.EU_OTHER;
        }
        
        // All other countries are non-European
        return GeographicRegion.NON_EUROPEAN;
    }
    
    /**
     * Get the home country for this classifier
     */
    public Country getHomeCountry() {
        return homeCountry;
    }
}