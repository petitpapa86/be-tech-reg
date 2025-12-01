package com.bcbs239.regtech.riskcalculation.domain.protection;

import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.ExchangeRate;
import com.bcbs239.regtech.riskcalculation.domain.valuation.EurAmount;
import com.bcbs239.regtech.riskcalculation.domain.valuation.ExchangeRateProvider;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Entity representing a credit risk mitigation with EUR conversion
 * Part of the Credit Protection bounded context
 * 
 * Converts mitigation values to EUR immediately upon construction
 * to ensure all calculations are performed in a common currency
 */
@Getter
public class Mitigation {
    
    private final MitigationType type;
    private final EurAmount eurValue;
    
    /**
     * Private constructor - use factory methods
     */
    private Mitigation(MitigationType type, EurAmount eurValue) {
        this.type = Objects.requireNonNull(type, "Mitigation type cannot be null");
        this.eurValue = Objects.requireNonNull(eurValue, "EUR value cannot be null");
    }
    
    /**
     * Creates a mitigation with EUR conversion
     * 
     * @param type The type of mitigation
     * @param value The mitigation value in original currency
     * @param currency The original currency code
     * @param rateProvider Provider for exchange rates
     * @return A new Mitigation with EUR-converted value
     */
    public static Mitigation create(
        MitigationType type,
        BigDecimal value,
        String currency,
        ExchangeRateProvider rateProvider
    ) {
        Objects.requireNonNull(type, "Mitigation type cannot be null");
        Objects.requireNonNull(value, "Mitigation value cannot be null");
        Objects.requireNonNull(currency, "Currency cannot be null");
        Objects.requireNonNull(rateProvider, "Exchange rate provider cannot be null");
        
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Mitigation value cannot be negative");
        }
        
        // Convert to EUR immediately
        EurAmount eurValue;
        if ("EUR".equals(currency)) {
            eurValue = EurAmount.of(value);
        } else {
            ExchangeRate rate = rateProvider.getRate(currency, "EUR");
            BigDecimal eurVal = value.multiply(rate.rate());
            eurValue = EurAmount.of(eurVal);
        }
        
        return new Mitigation(type, eurValue);
    }
    
    /**
     * Creates a mitigation from raw mitigation data
     * 
     * @param rawData The raw mitigation data
     * @param rateProvider Provider for exchange rates
     * @return A new Mitigation with EUR-converted value
     */
    public static Mitigation fromRawData(
        RawMitigationData rawData,
        ExchangeRateProvider rateProvider
    ) {
        Objects.requireNonNull(rawData, "Raw mitigation data cannot be null");
        return create(rawData.type(), rawData.value(), rawData.currency(), rateProvider);
    }
    
    /**
     * Factory method for reconstituting from persistence (already in EUR)
     */
    public static Mitigation reconstitute(MitigationType type, EurAmount eurValue) {
        return new Mitigation(type, eurValue);
    }
    
    // Getters

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Mitigation that = (Mitigation) o;
        return type == that.type && Objects.equals(eurValue, that.eurValue);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(type, eurValue);
    }
    
    @Override
    public String toString() {
        return "Mitigation{" +
                "type=" + type +
                ", eurValue=" + eurValue +
                '}';
    }
}
