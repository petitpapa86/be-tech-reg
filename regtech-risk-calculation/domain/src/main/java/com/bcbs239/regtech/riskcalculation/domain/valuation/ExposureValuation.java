package com.bcbs239.regtech.riskcalculation.domain.valuation;

import com.bcbs239.regtech.riskcalculation.domain.exposure.MonetaryAmount;
import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.ExchangeRate;
import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.ExposureId;
import lombok.Getter;

import java.time.Instant;
import java.util.Objects;

/**
 * Aggregate root for the Valuation Engine bounded context
 * Represents the result of converting an exposure amount to EUR
 * 
 * This is an immutable aggregate that captures:
 * - The original monetary amount in its native currency
 * - The converted amount in EUR
 * - The exchange rate used for conversion
 * - The timestamp of valuation for audit purposes
 * 
 * Requirements: 2.1, 2.2, 2.3, 2.4
 */
@Getter
public class ExposureValuation {
    
    private final ExposureId exposureId;
    /**
     * -- GETTER --
     *  Returns the original monetary amount (Requirement 2.3)
     *  The original amount is preserved for audit purposes
     */
    private final MonetaryAmount original;
    private final EurAmount converted;
    /**
     * -- GETTER --
     *  Returns the exchange rate used for conversion (Requirement 2.4)
     *  Includes the effective date for audit purposes
     */
    private final ExchangeRate rateUsed;
    private final Instant valuedAt;
    
    private ExposureValuation(
        ExposureId exposureId,
        MonetaryAmount original,
        EurAmount converted,
        ExchangeRate rateUsed,
        Instant valuedAt
    ) {
        this.exposureId = Objects.requireNonNull(exposureId, "Exposure ID cannot be null");
        this.original = Objects.requireNonNull(original, "Original amount cannot be null");
        this.converted = Objects.requireNonNull(converted, "Converted amount cannot be null");
        this.rateUsed = Objects.requireNonNull(rateUsed, "Exchange rate cannot be null");
        this.valuedAt = Objects.requireNonNull(valuedAt, "Valuation timestamp cannot be null");
    }
    
    /**
     * Factory method to convert an exposure to EUR
     * 
     * If the exposure is already in EUR, creates an identity exchange rate (EUR to EUR = 1.0)
     * Otherwise, retrieves the exchange rate from the provider and performs conversion
     * 
     * @param exposureId The unique identifier of the exposure
     * @param original The original monetary amount in its native currency
     * @param rateProvider The exchange rate provider service
     * @return A new ExposureValuation with the converted amount
     * @throws ExchangeRateUnavailableException if the exchange rate cannot be retrieved
     */
    public static ExposureValuation convert(
        ExposureId exposureId,
        MonetaryAmount original,
        ExchangeRateProvider rateProvider
    ) {
        Objects.requireNonNull(exposureId, "Exposure ID cannot be null");
        Objects.requireNonNull(original, "Original amount cannot be null");
        Objects.requireNonNull(rateProvider, "Rate provider cannot be null");
        
        // Check if already in EUR - no conversion needed (Requirement 2.2)
        if ("EUR".equals(original.currencyCode())) {
            return new ExposureValuation(
                exposureId,
                original,
                EurAmount.of(original.amount()),
                ExchangeRate.identity(),
                Instant.now()
            );
        }
        
        // Get exchange rate and convert (Requirement 2.1)
        ExchangeRate rate = rateProvider.getRate(original.currencyCode(), "EUR");
        java.math.BigDecimal eurAmount = original.amount().multiply(rate.rate());
        
        return new ExposureValuation(
            exposureId,
            original,
            EurAmount.of(eurAmount),
            rate,
            Instant.now()
        );
    }
    
    // Getters

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExposureValuation that = (ExposureValuation) o;
        return Objects.equals(exposureId, that.exposureId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(exposureId);
    }
    
    @Override
    public String toString() {
        return "ExposureValuation{" +
                "exposureId=" + exposureId +
                ", original=" + original +
                ", converted=" + converted +
                ", rateUsed=" + rateUsed +
                ", valuedAt=" + valuedAt +
                '}';
    }
}
