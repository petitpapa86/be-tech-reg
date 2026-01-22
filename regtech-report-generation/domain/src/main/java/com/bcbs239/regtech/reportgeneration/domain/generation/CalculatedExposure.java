package com.bcbs239.regtech.reportgeneration.domain.generation;

import com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects.*;
import lombok.NonNull;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Calculated Exposure value object
 * 
 * Represents a single calculated large exposure with all required data
 * for regulatory reporting including counterparty information, amounts,
 * and compliance status.
 * 
 * <p><strong>Aggregate-Specific Value Object:</strong> This value object is tightly coupled to the
 * {@link GeneratedReport} aggregate and represents concepts specific to report generation behavior.
 * It is co-located with its aggregate following DDD principles of high cohesion within aggregates.
 * This value object should remain in the generation package as it is only used within the report
 * generation context.</p>
 * 
 * @see GeneratedReport
 * @see com.bcbs239.regtech.reportgeneration.domain.generation
 */
public record CalculatedExposure(
    @NonNull CounterpartyName counterpartyName,
    Optional<LeiCode> leiCode,
    @NonNull IdentifierType identifierType,
    @NonNull CountryCode countryCode,
    @NonNull SectorCode sectorCode,
    Optional<CreditRating> rating,
    @NonNull Amount originalAmount,
    @NonNull CurrencyCode originalCurrency,
    @NonNull AmountEur amountEur,
    @NonNull AmountEur amountAfterCrm,
    @NonNull AmountEur tradingBookPortion,
    @NonNull AmountEur nonTradingBookPortion,
    @NonNull Percentage percentageOfCapital,
    boolean compliant
) {
    
    /**
     * Compact constructor with validation
     */
    public CalculatedExposure {
        if (counterpartyName == null) {
            throw new IllegalArgumentException("Counterparty name cannot be null");
        }
        if (identifierType == null) {
            throw new IllegalArgumentException("Identifier type cannot be null");
        }
        if (countryCode == null) {
            throw new IllegalArgumentException("Country code cannot be null");
        }
        if (sectorCode == null) {
            throw new IllegalArgumentException("Sector code cannot be null");
        }
        if (originalAmount == null) {
            throw new IllegalArgumentException("Original amount cannot be null");
        }
        if (originalCurrency == null) {
            throw new IllegalArgumentException("Original currency cannot be null");
        }
        if (amountEur == null) {
            throw new IllegalArgumentException("Amount EUR cannot be null");
        }
        if (amountAfterCrm == null) {
            throw new IllegalArgumentException("Amount after CRM cannot be null");
        }
        if (tradingBookPortion == null) {
            throw new IllegalArgumentException("Trading book portion cannot be null");
        }
        if (nonTradingBookPortion == null) {
            throw new IllegalArgumentException("Non-trading book portion cannot be null");
        }
        if (percentageOfCapital == null) {
            throw new IllegalArgumentException("Percentage of capital cannot be null");
        }
    }
    
    /**
     * Check if the exposure has a valid LEI code
     */
    public boolean hasLeiCode() {
        return leiCode.isPresent();
    }
    
    /**
     * Check if the exposure has a credit rating
     */
    public boolean hasRating() {
        return rating.isPresent();
    }
    
    /**
     * Check if the exposure exceeds the 25% limit
     */
    public boolean exceedsLimit() {
        return percentageOfCapital.isGreaterThan(new BigDecimal("25"));
    }
    
    /**
     * Get the LEI code or a default value for missing LEI
     */
    public String getLeiCodeOrDefault() {
        return leiCode.map(LeiCode::value).orElse("N/A");
    }
    
    /**
     * Get the rating or a default value for missing rating
     */
    public String getRatingOrDefault() {
        return rating.map(CreditRating::value).orElse("Not Rated");
    }
}
