package com.bcbs239.regtech.riskcalculation.domain.parameters;

import com.bcbs239.regtech.riskcalculation.domain.shared.Money;
import org.jspecify.annotations.NonNull;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Value Object: Capital Base Parameters
 * 
 * Defines capital base parameters for risk calculations
 */
public record CapitalBaseParameters(
    @NonNull Money eligibleCapital,
    @NonNull Money tier1Capital,
    @NonNull Money tier2Capital,
    @NonNull CalculationMethod calculationMethod,
    @NonNull LocalDate capitalReferenceDate,
    @NonNull UpdateFrequency updateFrequency,
    @NonNull LocalDate nextUpdateDate
) {
    
    public CapitalBaseParameters {
        if (eligibleCapital == null) {
            throw new IllegalArgumentException("Eligible capital cannot be null");
        }
        if (tier1Capital == null) {
            throw new IllegalArgumentException("Tier 1 capital cannot be null");
        }
        if (tier2Capital == null) {
            throw new IllegalArgumentException("Tier 2 capital cannot be null");
        }
        if (calculationMethod == null) {
            throw new IllegalArgumentException("Calculation method cannot be null");
        }
        if (capitalReferenceDate == null) {
            throw new IllegalArgumentException("Capital reference date cannot be null");
        }
        if (updateFrequency == null) {
            throw new IllegalArgumentException("Update frequency cannot be null");
        }
        if (nextUpdateDate == null) {
            throw new IllegalArgumentException("Next update date cannot be null");
        }
        
        // Validate capital structure: eligible = tier1 + tier2
        BigDecimal sum = tier1Capital.amount().add(tier2Capital.amount());
        if (sum.compareTo(eligibleCapital.amount()) != 0) {
            throw new IllegalArgumentException(
                "Eligible capital must equal Tier 1 + Tier 2 capital"
            );
        }
    }
    
    @NonNull
    public static CapitalBaseParameters createDefault() {
        Money eligibleCapital = Money.of(new BigDecimal("2500000000"), "EUR");
        Money tier1Capital = Money.of(new BigDecimal("1875000000"), "EUR");
        Money tier2Capital = Money.of(new BigDecimal("625000000"), "EUR");
        
        LocalDate today = LocalDate.now();
        LocalDate capitalReferenceDate = LocalDate.of(today.getYear() - 1, 12, 31);
        LocalDate nextUpdateDate = capitalReferenceDate.plusMonths(6);
        
        return new CapitalBaseParameters(
            eligibleCapital,
            tier1Capital,
            tier2Capital,
            CalculationMethod.STANDARDISED_APPROACH,
            capitalReferenceDate,
            UpdateFrequency.SEMESTRALE,
            nextUpdateDate
        );
    }
    
    public boolean isValid() {
        return eligibleCapital.amount().compareTo(BigDecimal.ZERO) > 0 &&
               tier1Capital.amount().compareTo(BigDecimal.ZERO) > 0 &&
               tier2Capital.amount().compareTo(BigDecimal.ZERO) >= 0;
    }
    
    public boolean isUpToDate() {
        LocalDate today = LocalDate.now();
        return !nextUpdateDate.isBefore(today);
    }
    
    public enum CalculationMethod {
        STANDARDISED_APPROACH,
        INTERNAL_RATINGS_BASED,
        ADVANCED_MEASUREMENT_APPROACH
    }
    
    public enum UpdateFrequency {
        TRIMESTRALE,
        SEMESTRALE,
        ANNUALE
    }
}
