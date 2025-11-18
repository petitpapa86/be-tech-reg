package com.bcbs239.regtech.riskcalculation.domain.classification;

import com.bcbs239.regtech.riskcalculation.domain.shared.enums.GeographicRegion;
import com.bcbs239.regtech.riskcalculation.domain.shared.enums.SectorCategory;
import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.Country;
import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.Sector;

/**
 * Business rules for classification logic
 * Encapsulates the business rules and validation for geographic and sector classification
 */
public class ClassificationRules {
    
    /**
     * Validate that a country can be classified
     */
    public static boolean isValidCountryForClassification(Country country) {
        if (country == null) {
            return false;
        }
        
        // Country code must be 2 characters (ISO 3166-1 alpha-2)
        return country.code().length() == 2 && !country.code().trim().isEmpty();
    }
    
    /**
     * Validate that a sector can be classified
     */
    public static boolean isValidSectorForClassification(Sector sector) {
        if (sector == null) {
            return false;
        }
        
        // Sector code must not be empty
        return !sector.code().trim().isEmpty();
    }
    
    /**
     * Check if a geographic region represents home country exposure
     */
    public static boolean isHomeCountryExposure(GeographicRegion region) {
        return region == GeographicRegion.ITALY;
    }
    
    /**
     * Check if a geographic region represents European exposure
     */
    public static boolean isEuropeanExposure(GeographicRegion region) {
        return region == GeographicRegion.ITALY || region == GeographicRegion.EU_OTHER;
    }
    
    /**
     * Check if a sector category represents retail exposure
     */
    public static boolean isRetailExposure(SectorCategory category) {
        return category == SectorCategory.RETAIL_MORTGAGE;
    }
    
    /**
     * Check if a sector category represents institutional exposure
     */
    public static boolean isInstitutionalExposure(SectorCategory category) {
        return category == SectorCategory.SOVEREIGN || 
               category == SectorCategory.BANKING;
    }
    
    /**
     * Check if a sector category represents private sector exposure
     */
    public static boolean isPrivateSectorExposure(SectorCategory category) {
        return category == SectorCategory.CORPORATE || 
               category == SectorCategory.RETAIL_MORTGAGE;
    }
    
    /**
     * Get the risk weight factor for a sector category (for future use)
     */
    public static double getRiskWeightFactor(SectorCategory category) {
        return switch (category) {
            case SOVEREIGN -> 0.0;        // Sovereign typically has 0% risk weight
            case BANKING -> 0.2;          // Banks typically have 20% risk weight
            case RETAIL_MORTGAGE -> 0.35; // Retail mortgages typically have 35% risk weight
            case CORPORATE -> 1.0;        // Corporate typically has 100% risk weight
            case OTHER -> 1.0;            // Other sectors default to 100% risk weight
        };
    }
}