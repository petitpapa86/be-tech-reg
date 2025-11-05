package com.bcbs239.regtech.dataquality.infrastructure.validation;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

/**
 * Utility class for validating internal ratings and their consistency with risk categories.
 */
@Component
public class RatingValidator {
    
    /**
     * Valid internal rating scales.
     * This represents a typical bank's internal rating system.
     */
    private static final Set<String> VALID_INTERNAL_RATINGS = Set.of(
        // High quality ratings
        "AAA", "AA+", "AA", "AA-", "A+", "A", "A-",
        
        // Investment grade ratings
        "BBB+", "BBB", "BBB-",
        
        // Sub-investment grade ratings
        "BB+", "BB", "BB-", "B+", "B", "B-",
        
        // Poor quality ratings
        "CCC+", "CCC", "CCC-", "CC", "C",
        
        // Default
        "D",
        
        // Numeric scale (1-10, where 1 is best)
        "1", "2", "3", "4", "5", "6", "7", "8", "9", "10",
        
        // Alternative letter scale
        "1A", "1B", "1C", "2A", "2B", "2C", "3A", "3B", "3C", "4A", "4B", "4C", "5"
    );
    
    /**
     * Valid risk categories based on regulatory classifications.
     */
    private static final Set<String> VALID_RISK_CATEGORIES = Set.of(
        "MINIMAL", "LOW", "MODERATE", "HIGH", "VERY_HIGH", "EXTREME", "DEFAULT"
    );
    
    /**
     * Mapping of internal ratings to expected risk categories.
     */
    private static final Map<String, Set<String>> RATING_RISK_CATEGORY_MAPPING = Map.ofEntries(
        Map.entry("AAA", Set.of("MINIMAL", "LOW")),
        Map.entry("AA+", Set.of("MINIMAL", "LOW")),
        Map.entry("AA", Set.of("MINIMAL", "LOW")),
        Map.entry("AA-", Set.of("MINIMAL", "LOW")),
        Map.entry("A+", Set.of("LOW")),
        Map.entry("A", Set.of("LOW")),
        Map.entry("A-", Set.of("LOW")),
        Map.entry("1", Set.of("MINIMAL", "LOW")),
        Map.entry("2", Set.of("LOW")),
        Map.entry("1A", Set.of("MINIMAL", "LOW")),
        Map.entry("1B", Set.of("LOW")),
        Map.entry("1C", Set.of("LOW")),

        // Good ratings -> Low/Moderate risk
        Map.entry("BBB+", Set.of("LOW", "MODERATE")),
        Map.entry("BBB", Set.of("MODERATE")),
        Map.entry("BBB-", Set.of("MODERATE")),
        Map.entry("3", Set.of("LOW", "MODERATE")),
        Map.entry("4", Set.of("MODERATE")),
        Map.entry("2A", Set.of("LOW", "MODERATE")),
        Map.entry("2B", Set.of("MODERATE")),
        Map.entry("2C", Set.of("MODERATE")),

        // Speculative ratings -> Moderate/High risk
        Map.entry("BB+", Set.of("MODERATE", "HIGH")),
        Map.entry("BB", Set.of("HIGH")),
        Map.entry("BB-", Set.of("HIGH")),
        Map.entry("B+", Set.of("HIGH")),
        Map.entry("B", Set.of("HIGH", "VERY_HIGH")),
        Map.entry("B-", Set.of("VERY_HIGH")),
        Map.entry("5", Set.of("MODERATE", "HIGH")),
        Map.entry("6", Set.of("HIGH")),
        Map.entry("7", Set.of("HIGH", "VERY_HIGH")),
        Map.entry("3A", Set.of("MODERATE", "HIGH")),
        Map.entry("3B", Set.of("HIGH")),
        Map.entry("3C", Set.of("HIGH")),

        // Poor ratings -> High/Very High risk
        Map.entry("CCC+", Set.of("VERY_HIGH")),
        Map.entry("CCC", Set.of("VERY_HIGH", "EXTREME")),
        Map.entry("CCC-", Set.of("EXTREME")),
        Map.entry("CC", Set.of("EXTREME")),
        Map.entry("C", Set.of("EXTREME")),
        Map.entry("8", Set.of("VERY_HIGH")),
        Map.entry("9", Set.of("VERY_HIGH", "EXTREME")),
        Map.entry("4A", Set.of("HIGH", "VERY_HIGH")),
        Map.entry("4B", Set.of("VERY_HIGH")),
        Map.entry("4C", Set.of("VERY_HIGH", "EXTREME")),

        // Default
        Map.entry("D", Set.of("DEFAULT")),
        Map.entry("10", Set.of("EXTREME", "DEFAULT"))
    );
    
    /**
     * Validate if an internal rating is valid.
     * 
     * @param internalRating the internal rating to validate
     * @return true if the rating is valid, false otherwise
     */
    public static boolean isValidInternalRating(String internalRating) {
        if (internalRating == null || internalRating.trim().isEmpty()) {
            return false;
        }
        
        String normalizedRating = internalRating.trim().toUpperCase();
        return VALID_INTERNAL_RATINGS.contains(normalizedRating);
    }
    
    /**
     * Validate if a risk category is valid.
     * 
     * @param riskCategory the risk category to validate
     * @return true if the risk category is valid, false otherwise
     */
    public static boolean isValidRiskCategory(String riskCategory) {
        if (riskCategory == null || riskCategory.trim().isEmpty()) {
            return false;
        }
        
        String normalizedCategory = riskCategory.trim().toUpperCase();
        return VALID_RISK_CATEGORIES.contains(normalizedCategory);
    }
    
    /**
     * Check if an internal rating is consistent with a risk category.
     * 
     * @param internalRating the internal rating
     * @param riskCategory the risk category
     * @return true if they are consistent, false otherwise
     */
    public static boolean isConsistentWithRiskCategory(String internalRating, String riskCategory) {
        if (!isValidInternalRating(internalRating) || !isValidRiskCategory(riskCategory)) {
            return false;
        }
        
        String normalizedRating = internalRating.trim().toUpperCase();
        String normalizedCategory = riskCategory.trim().toUpperCase();
        
        Set<String> expectedCategories = RATING_RISK_CATEGORY_MAPPING.get(normalizedRating);
        if (expectedCategories != null) {
            return expectedCategories.contains(normalizedCategory);
        }
        
        // If no specific mapping exists, allow any valid combination
        // This provides flexibility for banks with custom rating systems
        return true;
    }
    
    /**
     * Get the expected risk categories for an internal rating.
     * 
     * @param internalRating the internal rating
     * @return set of expected risk categories, or empty set if rating not found
     */
    public static Set<String> getExpectedRiskCategories(String internalRating) {
        if (!isValidInternalRating(internalRating)) {
            return Set.of();
        }
        
        String normalizedRating = internalRating.trim().toUpperCase();
        return RATING_RISK_CATEGORY_MAPPING.getOrDefault(normalizedRating, Set.of());
    }
    
    /**
     * Get the normalized internal rating (uppercase, trimmed).
     * 
     * @param internalRating the internal rating to normalize
     * @return the normalized internal rating, or null if invalid
     */
    public static String normalizeInternalRating(String internalRating) {
        if (!isValidInternalRating(internalRating)) {
            return null;
        }
        return internalRating.trim().toUpperCase();
    }
    
    /**
     * Get the normalized risk category (uppercase, trimmed).
     * 
     * @param riskCategory the risk category to normalize
     * @return the normalized risk category, or null if invalid
     */
    public static String normalizeRiskCategory(String riskCategory) {
        if (!isValidRiskCategory(riskCategory)) {
            return null;
        }
        return riskCategory.trim().toUpperCase();
    }
    
    /**
     * Check if an internal rating indicates investment grade.
     * 
     * @param internalRating the internal rating to check
     * @return true if it's investment grade, false otherwise
     */
    public static boolean isInvestmentGrade(String internalRating) {
        if (!isValidInternalRating(internalRating)) {
            return false;
        }
        
        String normalizedRating = internalRating.trim().toUpperCase();
        Set<String> investmentGradeRatings = Set.of(
            "AAA", "AA+", "AA", "AA-", "A+", "A", "A-", "BBB+", "BBB", "BBB-",
            "1", "2", "3", "4", "1A", "1B", "1C", "2A", "2B", "2C"
        );
        
        return investmentGradeRatings.contains(normalizedRating);
    }
    
    /**
     * Check if an internal rating indicates default.
     * 
     * @param internalRating the internal rating to check
     * @return true if it indicates default, false otherwise
     */
    public static boolean isDefault(String internalRating) {
        if (!isValidInternalRating(internalRating)) {
            return false;
        }
        
        String normalizedRating = internalRating.trim().toUpperCase();
        return Set.of("D", "10", "5").contains(normalizedRating);
    }
    
    /**
     * Get all valid internal ratings.
     * 
     * @return set of all valid internal ratings
     */
    public static Set<String> getAllValidInternalRatings() {
        return Set.copyOf(VALID_INTERNAL_RATINGS);
    }
    
    /**
     * Get all valid risk categories.
     * 
     * @return set of all valid risk categories
     */
    public static Set<String> getAllValidRiskCategories() {
        return Set.copyOf(VALID_RISK_CATEGORIES);
    }
}


