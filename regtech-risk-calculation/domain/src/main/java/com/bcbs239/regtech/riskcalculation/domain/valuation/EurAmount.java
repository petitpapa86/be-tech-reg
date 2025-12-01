package com.bcbs239.regtech.riskcalculation.domain.valuation;

import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.AmountEur;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Value object representing a monetary amount in EUR currency
 * Part of the Valuation Engine bounded context
 * 
 * This wraps AmountEur to provide context-specific naming while
 * maintaining compatibility with the shared value object
 */
public record EurAmount(BigDecimal value) {
    
    public EurAmount {
        Objects.requireNonNull(value, "EUR amount value cannot be null");
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("EUR amount cannot be negative");
        }
    }
    
    public static EurAmount of(BigDecimal value) {
        return new EurAmount(value);
    }
    
    public static EurAmount of(double value) {
        return new EurAmount(BigDecimal.valueOf(value));
    }
    
    public static EurAmount zero() {
        return new EurAmount(BigDecimal.ZERO);
    }
    
    public static EurAmount fromAmountEur(AmountEur amountEur) {
        return new EurAmount(amountEur.value());
    }
    
    public AmountEur toAmountEur() {
        return AmountEur.of(value);
    }
}
