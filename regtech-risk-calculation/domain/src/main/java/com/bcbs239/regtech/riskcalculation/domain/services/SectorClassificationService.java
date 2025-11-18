package com.bcbs239.regtech.riskcalculation.domain.services;

import com.bcbs239.regtech.riskcalculation.domain.shared.enums.SectorCategory;
import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.Sector;

/**
 * Domain service for sector classification of exposures
 * Maps granular sector codes to standardized categories
 * 
 * Requirements: 4.1 - Sector classification into standardized categories
 */
public interface SectorClassificationService {
    
    /**
     * Classify a sector code into a standardized sector category
     * 
     * @param sector The granular sector code from exposure data
     * @return The standardized sector category (RETAIL_MORTGAGE, SOVEREIGN, CORPORATE, BANKING, or OTHER)
     */
    SectorCategory classify(Sector sector);
}