package com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects;

/**
 * Sector code for economic sector classification
 * Immutable value object that represents granular sector codes from exposure data
 */
public record Sector(String code) {
    
    public Sector {
        if (code == null || code.trim().isEmpty()) {
            throw new IllegalArgumentException("Sector code cannot be null or empty");
        }
        // Normalize whitespace and case
        code = code.trim().toUpperCase();
    }
    
    public static Sector of(String code) {
        return new Sector(code);
    }
    
    public boolean isRetailMortgage() {
        return code.contains("RETAIL") && code.contains("MORTGAGE");
    }
    
    public boolean isSovereign() {
        return code.contains("SOVEREIGN") || code.contains("GOVERNMENT");
    }
    
    public boolean isCorporate() {
        return code.contains("CORPORATE") || code.contains("COMPANY");
    }
    
    public boolean isBanking() {
        return code.contains("BANK") || code.contains("FINANCIAL");
    }
}