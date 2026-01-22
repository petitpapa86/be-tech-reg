package com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects;

public enum IdentifierType {
    LEI,
    REF,
    EXPOSURE_ID,
    NAME,
    CONCAT;
    
    public static IdentifierType fromString(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Identifier type cannot be null or blank");
        }
        try {
            return IdentifierType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid identifier type: " + value);
        }
    }
}
