package com.bcbs239.regtech.riskcalculation.domain.exposure;

import java.util.Objects;

/**
 * Classification information for an exposure
 * Contains product type, instrument type, balance sheet type, and country
 * Immutable value object
 */
public record ExposureClassification(
    String productType,
    InstrumentType instrumentType,
    BalanceSheetType balanceSheetType,
    String countryCode
) {
    
    public ExposureClassification {
        Objects.requireNonNull(productType, "Product type cannot be null");
        Objects.requireNonNull(instrumentType, "Instrument type cannot be null");
        Objects.requireNonNull(balanceSheetType, "Balance sheet type cannot be null");
        Objects.requireNonNull(countryCode, "Country code cannot be null");
        
        if (productType.trim().isEmpty()) {
            throw new IllegalArgumentException("Product type cannot be empty");
        }
        if (countryCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Country code cannot be empty");
        }
        if (countryCode.length() != 2) {
            throw new IllegalArgumentException("Country code must be 2 characters (ISO 3166-1 alpha-2)");
        }
    }
    
    public static ExposureClassification of(
        String productType,
        InstrumentType instrumentType,
        BalanceSheetType balanceSheetType,
        String countryCode
    ) {
        return new ExposureClassification(
            productType,
            instrumentType,
            balanceSheetType,
            countryCode.toUpperCase()
        );
    }
}
