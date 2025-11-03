package com.bcbs239.regtech.modules.dataquality.infrastructure.validation;

import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Utility class for validating sector codes and counterparty types according to regulatory standards.
 */
@Component
public class SectorValidator {
    
    /**
     * Set of valid sector codes based on regulatory classifications.
     * These are typically based on NACE codes or similar industry classifications.
     */
    private static final Set<String> VALID_SECTORS = Set.of(
        // Financial sectors
        "BANKING", "INSURANCE", "INVESTMENT_FUNDS", "PENSION_FUNDS", "OTHER_FINANCIAL",
        "CENTRAL_BANK", "CREDIT_INSTITUTION", "INVESTMENT_FIRM", "ASSET_MANAGEMENT",
        
        // Corporate sectors
        "CORPORATE_FINANCIAL", "CORPORATE_NON_FINANCIAL", "CORPORATE_SME", "CORPORATE_LARGE",
        
        // Government sectors
        "SOVEREIGN", "REGIONAL_GOVERNMENT", "LOCAL_GOVERNMENT", "PUBLIC_SECTOR_ENTITY",
        "MULTILATERAL_DEVELOPMENT_BANK", "INTERNATIONAL_ORGANIZATION",
        
        // Retail sectors
        "RETAIL_INDIVIDUAL", "RETAIL_SME", "RETAIL_MORTGAGE", "RETAIL_CONSUMER_CREDIT",
        
        // Industry sectors (simplified NACE-based)
        "AGRICULTURE", "MINING", "MANUFACTURING", "UTILITIES", "CONSTRUCTION",
        "WHOLESALE_RETAIL", "TRANSPORT", "ACCOMMODATION", "INFORMATION_COMMUNICATION",
        "REAL_ESTATE", "PROFESSIONAL_SERVICES", "ADMINISTRATIVE_SERVICES",
        "PUBLIC_ADMINISTRATION", "EDUCATION", "HEALTH", "ARTS_ENTERTAINMENT", "OTHER_SERVICES"
    );
    
    /**
     * Set of valid counterparty types.
     */
    private static final Set<String> VALID_COUNTERPARTY_TYPES = Set.of(
        "INDIVIDUAL", "SME", "CORPORATE", "BANK", "INSURANCE_COMPANY", "INVESTMENT_FUND",
        "PENSION_FUND", "SOVEREIGN", "REGIONAL_GOVERNMENT", "LOCAL_GOVERNMENT",
        "PUBLIC_SECTOR_ENTITY", "CENTRAL_BANK", "MULTILATERAL_DEVELOPMENT_BANK",
        "INTERNATIONAL_ORGANIZATION", "OTHER_FINANCIAL", "NON_FINANCIAL_CORPORATE"
    );
    
    /**
     * Mapping of sectors to compatible counterparty types.
     */
    private static final Set<String> FINANCIAL_SECTORS = Set.of(
        "BANKING", "INSURANCE", "INVESTMENT_FUNDS", "PENSION_FUNDS", "OTHER_FINANCIAL",
        "CENTRAL_BANK", "CREDIT_INSTITUTION", "INVESTMENT_FIRM", "ASSET_MANAGEMENT"
    );
    
    private static final Set<String> CORPORATE_SECTORS = Set.of(
        "CORPORATE_FINANCIAL", "CORPORATE_NON_FINANCIAL", "CORPORATE_SME", "CORPORATE_LARGE",
        "AGRICULTURE", "MINING", "MANUFACTURING", "UTILITIES", "CONSTRUCTION",
        "WHOLESALE_RETAIL", "TRANSPORT", "ACCOMMODATION", "INFORMATION_COMMUNICATION",
        "REAL_ESTATE", "PROFESSIONAL_SERVICES", "ADMINISTRATIVE_SERVICES", "OTHER_SERVICES"
    );
    
    private static final Set<String> GOVERNMENT_SECTORS = Set.of(
        "SOVEREIGN", "REGIONAL_GOVERNMENT", "LOCAL_GOVERNMENT", "PUBLIC_SECTOR_ENTITY",
        "MULTILATERAL_DEVELOPMENT_BANK", "INTERNATIONAL_ORGANIZATION", "PUBLIC_ADMINISTRATION"
    );
    
    private static final Set<String> RETAIL_SECTORS = Set.of(
        "RETAIL_INDIVIDUAL", "RETAIL_SME", "RETAIL_MORTGAGE", "RETAIL_CONSUMER_CREDIT"
    );
    
    /**
     * Validate if a sector code is valid.
     * 
     * @param sector the sector code to validate
     * @return true if the sector is valid, false otherwise
     */
    public static boolean isValidSector(String sector) {
        if (sector == null || sector.trim().isEmpty()) {
            return false;
        }
        
        String normalizedSector = sector.trim().toUpperCase();
        return VALID_SECTORS.contains(normalizedSector);
    }
    
    /**
     * Validate if a counterparty type is valid.
     * 
     * @param counterpartyType the counterparty type to validate
     * @return true if the counterparty type is valid, false otherwise
     */
    public static boolean isValidCounterpartyType(String counterpartyType) {
        if (counterpartyType == null || counterpartyType.trim().isEmpty()) {
            return false;
        }
        
        String normalizedType = counterpartyType.trim().toUpperCase();
        return VALID_COUNTERPARTY_TYPES.contains(normalizedType);
    }
    
    /**
     * Check if a sector is consistent with a counterparty type.
     * 
     * @param sector the sector code
     * @param counterpartyType the counterparty type
     * @return true if they are consistent, false otherwise
     */
    public static boolean isConsistentWithCounterpartyType(String sector, String counterpartyType) {
        if (!isValidSector(sector) || !isValidCounterpartyType(counterpartyType)) {
            return false;
        }
        
        String normalizedSector = sector.trim().toUpperCase();
        String normalizedType = counterpartyType.trim().toUpperCase();
        
        // Financial sectors should match financial counterparty types
        if (FINANCIAL_SECTORS.contains(normalizedSector)) {
            return Set.of("BANK", "INSURANCE_COMPANY", "INVESTMENT_FUND", "PENSION_FUND", 
                         "OTHER_FINANCIAL", "CENTRAL_BANK").contains(normalizedType);
        }
        
        // Corporate sectors should match corporate counterparty types
        if (CORPORATE_SECTORS.contains(normalizedSector)) {
            return Set.of("CORPORATE", "NON_FINANCIAL_CORPORATE", "SME").contains(normalizedType);
        }
        
        // Government sectors should match government counterparty types
        if (GOVERNMENT_SECTORS.contains(normalizedSector)) {
            return Set.of("SOVEREIGN", "REGIONAL_GOVERNMENT", "LOCAL_GOVERNMENT", 
                         "PUBLIC_SECTOR_ENTITY", "MULTILATERAL_DEVELOPMENT_BANK", 
                         "INTERNATIONAL_ORGANIZATION").contains(normalizedType);
        }
        
        // Retail sectors should match individual counterparty types
        if (RETAIL_SECTORS.contains(normalizedSector)) {
            return Set.of("INDIVIDUAL", "SME").contains(normalizedType);
        }
        
        return false;
    }
    
    /**
     * Get the normalized sector code (uppercase, trimmed).
     * 
     * @param sector the sector code to normalize
     * @return the normalized sector code, or null if invalid
     */
    public static String normalizeSector(String sector) {
        if (!isValidSector(sector)) {
            return null;
        }
        return sector.trim().toUpperCase();
    }
    
    /**
     * Get the normalized counterparty type (uppercase, trimmed).
     * 
     * @param counterpartyType the counterparty type to normalize
     * @return the normalized counterparty type, or null if invalid
     */
    public static String normalizeCounterpartyType(String counterpartyType) {
        if (!isValidCounterpartyType(counterpartyType)) {
            return null;
        }
        return counterpartyType.trim().toUpperCase();
    }
    
    /**
     * Check if a sector is a financial sector.
     * 
     * @param sector the sector code to check
     * @return true if it's a financial sector, false otherwise
     */
    public static boolean isFinancialSector(String sector) {
        if (!isValidSector(sector)) {
            return false;
        }
        return FINANCIAL_SECTORS.contains(sector.trim().toUpperCase());
    }
    
    /**
     * Check if a sector is a corporate sector.
     * 
     * @param sector the sector code to check
     * @return true if it's a corporate sector, false otherwise
     */
    public static boolean isCorporateSector(String sector) {
        if (!isValidSector(sector)) {
            return false;
        }
        return CORPORATE_SECTORS.contains(sector.trim().toUpperCase());
    }
    
    /**
     * Check if a sector is a government sector.
     * 
     * @param sector the sector code to check
     * @return true if it's a government sector, false otherwise
     */
    public static boolean isGovernmentSector(String sector) {
        if (!isValidSector(sector)) {
            return false;
        }
        return GOVERNMENT_SECTORS.contains(sector.trim().toUpperCase());
    }
    
    /**
     * Check if a sector is a retail sector.
     * 
     * @param sector the sector code to check
     * @return true if it's a retail sector, false otherwise
     */
    public static boolean isRetailSector(String sector) {
        if (!isValidSector(sector)) {
            return false;
        }
        return RETAIL_SECTORS.contains(sector.trim().toUpperCase());
    }
    
    /**
     * Get all valid sector codes.
     * 
     * @return set of all valid sector codes
     */
    public static Set<String> getAllValidSectors() {
        return Set.copyOf(VALID_SECTORS);
    }
    
    /**
     * Get all valid counterparty types.
     * 
     * @return set of all valid counterparty types
     */
    public static Set<String> getAllValidCounterpartyTypes() {
        return Set.copyOf(VALID_COUNTERPARTY_TYPES);
    }
}