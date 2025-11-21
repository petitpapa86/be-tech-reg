package com.bcbs239.regtech.reportgeneration.domain.generation;

import com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects.AmountEur;
import lombok.NonNull;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Geographic breakdown of exposures by region
 * Immutable value object containing geographic distribution metrics
 */
public record GeographicBreakdown(
    @NonNull AmountEur italyAmount,
    @NonNull BigDecimal italyPercentage,
    int italyCount,
    
    @NonNull AmountEur euOtherAmount,
    @NonNull BigDecimal euOtherPercentage,
    int euOtherCount,
    
    @NonNull AmountEur nonEuropeanAmount,
    @NonNull BigDecimal nonEuropeanPercentage,
    int nonEuropeanCount
) {
    
    /**
     * Compact constructor with validation
     */
    public GeographicBreakdown {
        if (italyCount < 0 || euOtherCount < 0 || nonEuropeanCount < 0) {
            throw new IllegalArgumentException("Geographic counts cannot be negative");
        }
    }
    
    /**
     * Create from a map of region codes to amounts
     */
    public static GeographicBreakdown fromMap(Map<String, BigDecimal> geographicMap, BigDecimal totalAmount) {
        BigDecimal italyAmt = geographicMap.getOrDefault("IT", BigDecimal.ZERO);
        BigDecimal euOtherAmt = geographicMap.entrySet().stream()
            .filter(e -> !e.getKey().equals("IT") && isEuropean(e.getKey()))
            .map(Map.Entry::getValue)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal nonEuAmt = geographicMap.entrySet().stream()
            .filter(e -> !isEuropean(e.getKey()))
            .map(Map.Entry::getValue)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal italyPct = calculatePercentage(italyAmt, totalAmount);
        BigDecimal euOtherPct = calculatePercentage(euOtherAmt, totalAmount);
        BigDecimal nonEuPct = calculatePercentage(nonEuAmt, totalAmount);
        
        return new GeographicBreakdown(
            AmountEur.of(italyAmt),
            italyPct,
            0, // Count would need to be calculated separately
            AmountEur.of(euOtherAmt),
            euOtherPct,
            0,
            AmountEur.of(nonEuAmt),
            nonEuPct,
            0
        );
    }
    
    /**
     * Calculate total amount across all regions
     */
    public AmountEur getTotalAmount() {
        return AmountEur.of(
            italyAmount.value()
                .add(euOtherAmount.value())
                .add(nonEuropeanAmount.value())
        );
    }
    
    /**
     * Calculate total exposure count across all regions
     */
    public int getTotalCount() {
        return italyCount + euOtherCount + nonEuropeanCount;
    }
    
    /**
     * Get the largest regional exposure by amount
     */
    public AmountEur getLargestRegionalAmount() {
        AmountEur largest = italyAmount;
        if (euOtherAmount.value().compareTo(largest.value()) > 0) {
            largest = euOtherAmount;
        }
        if (nonEuropeanAmount.value().compareTo(largest.value()) > 0) {
            largest = nonEuropeanAmount;
        }
        return largest;
    }
    
    private static boolean isEuropean(String countryCode) {
        // EU country codes (simplified list)
        return countryCode.matches("AT|BE|BG|HR|CY|CZ|DK|EE|FI|FR|DE|GR|HU|IE|IT|LV|LT|LU|MT|NL|PL|PT|RO|SK|SI|ES|SE");
    }
    
    private static BigDecimal calculatePercentage(BigDecimal amount, BigDecimal total) {
        if (total.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return amount.divide(total, 4, java.math.RoundingMode.HALF_UP)
            .multiply(new BigDecimal("100"));
    }
}
