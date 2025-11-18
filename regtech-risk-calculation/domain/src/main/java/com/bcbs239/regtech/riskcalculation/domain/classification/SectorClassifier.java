package com.bcbs239.regtech.riskcalculation.domain.classification;

import com.bcbs239.regtech.riskcalculation.domain.shared.enums.SectorCategory;
import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.Sector;

import java.util.Objects;

/**
 * Domain service for classifying exposures by economic sector
 * Implements business rules for sector classification using switch expressions
 */
public class SectorClassifier {
    
    /**
     * Classify a sector code into a standardized sector category
     * Uses switch expressions for efficient mapping
     */
    public SectorCategory classify(Sector sector) {
        Objects.requireNonNull(sector, "Sector cannot be null");
        
        String sectorCode = sector.code().toUpperCase();
        
        // Use switch expression for efficient classification
        return switch (sectorCode) {
            // Retail mortgage patterns
            case String s when s.contains("RETAIL") && s.contains("MORTGAGE") -> SectorCategory.RETAIL_MORTGAGE;
            case String s when s.contains("RESIDENTIAL") && s.contains("MORTGAGE") -> SectorCategory.RETAIL_MORTGAGE;
            case String s when s.contains("HOME") && s.contains("LOAN") -> SectorCategory.RETAIL_MORTGAGE;
            
            // Sovereign patterns
            case String s when s.contains("SOVEREIGN") -> SectorCategory.SOVEREIGN;
            case String s when s.contains("GOVERNMENT") -> SectorCategory.SOVEREIGN;
            case String s when s.contains("PUBLIC") && s.contains("SECTOR") -> SectorCategory.SOVEREIGN;
            case String s when s.contains("TREASURY") -> SectorCategory.SOVEREIGN;
            case String s when s.contains("MUNICIPAL") -> SectorCategory.SOVEREIGN;
            
            // Corporate patterns
            case String s when s.contains("CORPORATE") -> SectorCategory.CORPORATE;
            case String s when s.contains("COMPANY") -> SectorCategory.CORPORATE;
            case String s when s.contains("ENTERPRISE") -> SectorCategory.CORPORATE;
            case String s when s.contains("BUSINESS") -> SectorCategory.CORPORATE;
            case String s when s.contains("COMMERCIAL") -> SectorCategory.CORPORATE;
            
            // Banking patterns
            case String s when s.contains("BANK") -> SectorCategory.BANKING;
            case String s when s.contains("FINANCIAL") && s.contains("INSTITUTION") -> SectorCategory.BANKING;
            case String s when s.contains("CREDIT") && s.contains("INSTITUTION") -> SectorCategory.BANKING;
            case String s when s.contains("INTERBANK") -> SectorCategory.BANKING;
            
            // Default to OTHER for unrecognized sectors
            default -> SectorCategory.OTHER;
        };
    }
    
    /**
     * Check if a sector code matches retail mortgage patterns
     */
    public boolean isRetailMortgage(Sector sector) {
        return classify(sector) == SectorCategory.RETAIL_MORTGAGE;
    }
    
    /**
     * Check if a sector code matches sovereign patterns
     */
    public boolean isSovereign(Sector sector) {
        return classify(sector) == SectorCategory.SOVEREIGN;
    }
    
    /**
     * Check if a sector code matches corporate patterns
     */
    public boolean isCorporate(Sector sector) {
        return classify(sector) == SectorCategory.CORPORATE;
    }
    
    /**
     * Check if a sector code matches banking patterns
     */
    public boolean isBanking(Sector sector) {
        return classify(sector) == SectorCategory.BANKING;
    }
}