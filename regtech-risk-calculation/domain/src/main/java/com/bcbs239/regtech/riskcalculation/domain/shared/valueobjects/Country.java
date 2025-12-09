package com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects;

/**
 * Country code for geographic classification
 * Immutable value object that represents ISO country codes
 */
public record Country(String code) {
    
    public Country {
        if (code == null || code.trim().isEmpty()) {
            throw new IllegalArgumentException("Country code cannot be null or empty");
        }
        if (code.length() != 2) {
            throw new IllegalArgumentException("Country code must be exactly 2 characters (ISO 3166-1 alpha-2)");
        }
        // Normalize to uppercase
        code = code.toUpperCase();
    }
    
    public static Country of(String code) {
        return new Country(code);
    }
    
    public boolean isItaly() {
        return "IT".equals(code);
    }
    
    public boolean isInEuropeanUnion() {
        // EU country codes as of 2024
        return switch (code) {
            case "AT", "BE", "BG", "HR", "CY", "CZ", "DK", "EE", "FI", "FR",
                 "DE", "GR", "HU", "IE", "IT", "LV", "LT", "LU", "MT", "NL",
                 "PL", "PT", "RO", "SK", "SI", "ES", "SE" -> true;
            default -> false;
        };
    }
}