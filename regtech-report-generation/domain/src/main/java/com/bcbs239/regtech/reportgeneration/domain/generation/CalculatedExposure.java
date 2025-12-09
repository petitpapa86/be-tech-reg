package com.bcbs239.regtech.reportgeneration.domain.generation;

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
    @NonNull String counterpartyName,
    Optional<String> leiCode,
    @NonNull String identifierType,
    @NonNull String countryCode,
    @NonNull String sectorCode,
    Optional<String> rating,
    @NonNull BigDecimal originalAmount,
    @NonNull String originalCurrency,
    @NonNull BigDecimal amountEur,
    @NonNull BigDecimal amountAfterCrm,
    @NonNull BigDecimal tradingBookPortion,
    @NonNull BigDecimal nonTradingBookPortion,
    @NonNull BigDecimal percentageOfCapital,
    boolean compliant
) {
    
    /**
     * Compact constructor with validation
     */
    public CalculatedExposure {
        if (counterpartyName == null || counterpartyName.isBlank()) {
            throw new IllegalArgumentException("Counterparty name cannot be null or blank");
        }
        if (identifierType == null || identifierType.isBlank()) {
            throw new IllegalArgumentException("Identifier type cannot be null or blank");
        }
        if (countryCode == null || countryCode.isBlank()) {
            throw new IllegalArgumentException("Country code cannot be null or blank");
        }
        if (sectorCode == null || sectorCode.isBlank()) {
            throw new IllegalArgumentException("Sector code cannot be null or blank");
        }
        if (originalAmount == null || originalAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Original amount cannot be negative");
        }
        if (originalCurrency == null || originalCurrency.isBlank()) {
            throw new IllegalArgumentException("Original currency cannot be null or blank");
        }
        if (amountEur == null || amountEur.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount EUR cannot be negative");
        }
        if (amountAfterCrm == null || amountAfterCrm.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount after CRM cannot be negative");
        }
        if (tradingBookPortion == null || tradingBookPortion.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Trading book portion cannot be negative");
        }
        if (nonTradingBookPortion == null || nonTradingBookPortion.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Non-trading book portion cannot be negative");
        }
        if (percentageOfCapital == null || percentageOfCapital.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Percentage of capital cannot be negative");
        }
    }
    
    /**
     * Check if the exposure has a valid LEI code
     */
    public boolean hasLeiCode() {
        return leiCode.isPresent() && !leiCode.get().isBlank();
    }
    
    /**
     * Check if the exposure has a credit rating
     */
    public boolean hasRating() {
        return rating.isPresent() && !rating.get().isBlank();
    }
    
    /**
     * Check if the exposure exceeds the 25% limit
     */
    public boolean exceedsLimit() {
        return percentageOfCapital.compareTo(new BigDecimal("25")) > 0;
    }
    
    /**
     * Get the LEI code or a default value for missing LEI
     */
    public String getLeiCodeOrDefault() {
        return leiCode.orElse("N/A");
    }
    
    /**
     * Get the rating or a default value for missing rating
     */
    public String getRatingOrDefault() {
        return rating.orElse("Not Rated");
    }
}
