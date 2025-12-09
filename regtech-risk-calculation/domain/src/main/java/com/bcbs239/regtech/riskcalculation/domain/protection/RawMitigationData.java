package com.bcbs239.regtech.riskcalculation.domain.protection;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Value object representing raw mitigation data before EUR conversion
 * Part of the Credit Protection bounded context
 * 
 * Contains the original mitigation information as received from the source system
 */
public record RawMitigationData(
    MitigationType type,
    BigDecimal value,
    String currency
) {
    
    public RawMitigationData {
        Objects.requireNonNull(type, "Mitigation type cannot be null");
        Objects.requireNonNull(value, "Mitigation value cannot be null");
        Objects.requireNonNull(currency, "Currency cannot be null");
        
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Mitigation value cannot be negative");
        }
        
        if (currency.isBlank()) {
            throw new IllegalArgumentException("Currency cannot be blank");
        }
    }
    
    public static RawMitigationData of(MitigationType type, BigDecimal value, String currency) {
        return new RawMitigationData(type, value, currency);
    }
    
    public static RawMitigationData of(MitigationType type, double value, String currency) {
        return new RawMitigationData(type, BigDecimal.valueOf(value), currency);
    }
}
