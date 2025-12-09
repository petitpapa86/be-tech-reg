package com.bcbs239.regtech.riskcalculation.domain.analysis;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Value object representing a breakdown of portfolio shares by category
 * Can represent geographic or sector breakdowns
 * Part of the Portfolio Analysis bounded context
 */
public record Breakdown(Map<String, Share> shares) {
    
    public Breakdown {
        Objects.requireNonNull(shares, "Shares map cannot be null");
        // Create immutable copy
        shares = Map.copyOf(shares);
    }
    
    /**
     * Create a breakdown from amounts grouped by category
     * 
     * @param amounts map of category to amount
     * @param total total portfolio amount
     * @return Breakdown with calculated shares
     */
    public static Breakdown from(Map<?, BigDecimal> amounts, BigDecimal total) {
        Objects.requireNonNull(amounts, "Amounts map cannot be null");
        Objects.requireNonNull(total, "Total cannot be null");
        
        Map<String, Share> shares = amounts.entrySet().stream()
            .collect(Collectors.toMap(
                e -> e.getKey().toString(),
                e -> Share.calculate(e.getValue(), total)
            ));
        
        return new Breakdown(shares);
    }
    
    /**
     * Get share for a specific category
     */
    public Share getShare(String category) {
        return shares.get(category);
    }
    
    /**
     * Check if breakdown contains a category
     */
    public boolean hasCategory(String category) {
        return shares.containsKey(category);
    }
}
