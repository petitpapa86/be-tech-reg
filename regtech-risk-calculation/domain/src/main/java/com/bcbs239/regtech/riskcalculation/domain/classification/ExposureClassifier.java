package com.bcbs239.regtech.riskcalculation.domain.classification;

import com.bcbs239.regtech.riskcalculation.domain.shared.enums.GeographicRegion;

import java.util.Objects;
import java.util.Set;

/**
 * Domain service for classifying exposures by geographic region and economic sector
 * Part of the Classification Service bounded context
 * 
 * This service implements the business rules for:
 * - Geographic classification (ITALY, EU_OTHER, NON_EUROPEAN)
 * - Sector classification (RETAIL_MORTGAGE, SOVEREIGN, CORPORATE, BANKING, OTHER)
 * 
 * Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 5.1, 5.2, 5.3, 5.4, 5.5, 5.6
 */
public class ExposureClassifier {
    
    /**
     * Set of EU country codes (excluding Italy)
     * Based on EU member states as of the implementation date
     */
    private static final Set<String> EU_COUNTRIES = Set.of(
        "AT", "BE", "BG", "HR", "CY", "CZ", "DK", "EE", "FI", "FR",
        "DE", "GR", "HU", "IE", "LV", "LT", "LU", "MT", "NL", "PL",
        "PT", "RO", "SK", "SI", "ES", "SE"
    );
    
    /**
     * Classify a country code into a geographic region
     * 
     * Business rules:
     * - "IT" -> ITALY (home country)
     * - EU member states (excluding IT) -> EU_OTHER
     * - All other countries -> NON_EUROPEAN
     * 
     * @param countryCode ISO 3166-1 alpha-2 country code
     * @return the geographic region classification
     * @throws NullPointerException if countryCode is null
     * 
     * Validates: Requirements 4.1, 4.2, 4.3, 4.4
     */
    public GeographicRegion classifyRegion(String countryCode) {
        Objects.requireNonNull(countryCode, "Country code cannot be null");
        
        if ("IT".equals(countryCode)) {
            return GeographicRegion.ITALY;
        } else if (EU_COUNTRIES.contains(countryCode)) {
            return GeographicRegion.EU_OTHER;
        } else {
            return GeographicRegion.NON_EUROPEAN;
        }
    }
    
    /**
     * Classify a product type into an economic sector
     * 
     * Business rules (case-insensitive pattern matching):
     * - Contains "MORTGAGE" -> RETAIL_MORTGAGE
     * - Contains "GOVERNMENT" or "TREASURY" -> SOVEREIGN
     * - Contains "INTERBANK" -> BANKING
     * - Contains "BUSINESS", "EQUIPMENT", or "CREDIT LINE" -> CORPORATE
     * - No match -> OTHER
     * 
     * @param productType the product type description
     * @return the economic sector classification
     * @throws NullPointerException if productType is null
     * 
     * Validates: Requirements 5.1, 5.2, 5.3, 5.4, 5.5, 5.6
     */
    public EconomicSector classifySector(String productType) {
        Objects.requireNonNull(productType, "Product type cannot be null");
        
        String normalized = productType.toUpperCase().trim();
        
        if (normalized.contains("MORTGAGE")) {
            return EconomicSector.RETAIL_MORTGAGE;
        } else if (normalized.contains("GOVERNMENT") || normalized.contains("TREASURY")) {
            return EconomicSector.SOVEREIGN;
        } else if (normalized.contains("INTERBANK")) {
            return EconomicSector.BANKING;
        } else if (normalized.contains("BUSINESS") || 
                   normalized.contains("EQUIPMENT") || 
                   normalized.contains("CREDIT LINE")) {
            return EconomicSector.CORPORATE;
        } else {
            return EconomicSector.OTHER;
        }
    }
    
    /**
     * Get the set of EU country codes (excluding Italy)
     * Useful for testing and validation
     */
    public Set<String> getEuCountries() {
        return Set.copyOf(EU_COUNTRIES);
    }
}
