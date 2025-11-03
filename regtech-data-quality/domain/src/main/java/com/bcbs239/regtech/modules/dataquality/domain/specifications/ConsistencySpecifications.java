package com.bcbs239.regtech.modules.dataquality.domain.specifications;

import com.bcbs239.regtech.core.shared.ErrorDetail;
import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.core.shared.Specification;
import com.bcbs239.regtech.modules.dataquality.domain.validation.ExposureRecord;

import java.util.Map;
import java.util.Set;

/**
 * Specifications for Consistency quality dimension validation.
 * 
 * Consistency ensures that cross-field relationships and referential integrity
 * are maintained according to BCBS 239 regulatory requirements and business rules.
 */
public class ConsistencySpecifications {

    // Currency-Country mappings for major economies
    private static final Map<String, Set<String>> CURRENCY_COUNTRY_MAPPINGS = createCurrencyCountryMappings();

    // Sector-CounterpartyType mappings for business logic consistency
    private static final Map<String, Set<String>> SECTOR_COUNTERPARTY_MAPPINGS = createSectorCounterpartyMappings();

    // Rating-RiskCategory mappings for rating validation
    private static final Map<String, Set<String>> RATING_RISK_MAPPINGS = createRatingRiskMappings();

    // ProductType-Maturity consistency rules
    private static final Set<String> PRODUCTS_REQUIRING_MATURITY = Set.of(
        "LOAN", "BOND", "DEPOSIT", "DERIVATIVE", "SWAP", "FORWARD", "FUTURE", "OPTION"
    );

    private static final Set<String> PRODUCTS_WITHOUT_MATURITY = Set.of(
        "EQUITY", "PERPETUAL_BOND", "DEMAND_DEPOSIT", "CURRENT_ACCOUNT"
    );

    /**
     * Validates that currency matches the country where it's typically used.
     * This helps identify potential data entry errors or inconsistencies.
     * 
     * @return Specification that validates currency-country consistency
     */
    public static Specification<ExposureRecord> currencyMatchesCountry() {
        return exposure -> {
            if (exposure.currency() != null && exposure.country() != null) {
                String currency = exposure.currency().toUpperCase();
                String country = exposure.country().toUpperCase();
                
                Set<String> validCountries = CURRENCY_COUNTRY_MAPPINGS.get(currency);
                if (validCountries != null && !validCountries.contains(country)) {
                    return Result.failure(ErrorDetail.of("CONSISTENCY_CURRENCY_COUNTRY_MISMATCH", 
                        String.format("Currency %s is not typically used in country %s", currency, country), 
                        "currency"));
                }
            }
            return Result.success();
        };
    }

    /**
     * Validates that sector classification is consistent with counterparty type.
     * This ensures business logic consistency in exposure classification.
     * 
     * @return Specification that validates sector-counterparty type consistency
     */
    public static Specification<ExposureRecord> sectorMatchesCounterpartyType() {
        return exposure -> {
            if (exposure.sector() != null && exposure.counterpartyType() != null) {
                String sector = exposure.sector().toUpperCase();
                String counterpartyType = exposure.counterpartyType().toUpperCase();
                
                Set<String> validCounterpartyTypes = SECTOR_COUNTERPARTY_MAPPINGS.get(sector);
                if (validCounterpartyTypes != null && !validCounterpartyTypes.contains(counterpartyType)) {
                    return Result.failure(ErrorDetail.of("CONSISTENCY_SECTOR_COUNTERPARTY_MISMATCH", 
                        String.format("Sector %s is inconsistent with counterparty type %s", sector, counterpartyType), 
                        "sector"));
                }
            }
            return Result.success();
        };
    }

    /**
     * Validates that internal rating is consistent with risk category.
     * This ensures proper risk assessment alignment across rating systems.
     * 
     * @return Specification that validates rating-risk category consistency
     */
    public static Specification<ExposureRecord> ratingMatchesRiskCategory() {
        return exposure -> {
            if (exposure.internalRating() != null && exposure.riskCategory() != null) {
                String rating = exposure.internalRating().toUpperCase();
                String riskCategory = exposure.riskCategory().toUpperCase();
                
                // Handle rating modifiers (+ and -)
                String baseRating = rating.replaceAll("[+-]", "");
                
                Set<String> validRiskCategories = RATING_RISK_MAPPINGS.get(baseRating);
                if (validRiskCategories != null && !validRiskCategories.contains(riskCategory)) {
                    return Result.failure(ErrorDetail.of("CONSISTENCY_RATING_RISK_MISMATCH", 
                        String.format("Internal rating %s is inconsistent with risk category %s", rating, riskCategory), 
                        "internal_rating"));
                }
            }
            return Result.success();
        };
    }

    /**
     * Validates that product type is consistent with maturity date presence.
     * Some products require maturity dates while others should not have them.
     * 
     * @return Specification that validates product type-maturity consistency
     */
    public static Specification<ExposureRecord> productTypeMatchesMaturity() {
        return exposure -> {
            if (exposure.productType() != null) {
                String productType = exposure.productType().toUpperCase();
                boolean hasMaturity = exposure.maturityDate() != null;
                
                // Products that should have maturity dates
                if (PRODUCTS_REQUIRING_MATURITY.contains(productType) && !hasMaturity) {
                    return Result.failure(ErrorDetail.of("CONSISTENCY_PRODUCT_MATURITY_MISSING", 
                        String.format("Product type %s requires a maturity date", productType), 
                        "maturity_date"));
                }
                
                // Products that should not have maturity dates
                if (PRODUCTS_WITHOUT_MATURITY.contains(productType) && hasMaturity) {
                    return Result.failure(ErrorDetail.of("CONSISTENCY_PRODUCT_MATURITY_UNEXPECTED", 
                        String.format("Product type %s should not have a maturity date", productType), 
                        "maturity_date"));
                }
            }
            return Result.success();
        };
    }

    /**
     * Creates the currency-country mappings map
     */
    private static Map<String, Set<String>> createCurrencyCountryMappings() {
        Map<String, Set<String>> mappings = new java.util.HashMap<>();
        mappings.put("USD", Set.of("US"));
        mappings.put("EUR", Set.of("DE", "FR", "IT", "ES", "NL", "BE", "AT", "IE", "PT", "GR", "FI", "SK", "SI", "EE", "LV", "LT", "CY", "MT", "LU"));
        mappings.put("GBP", Set.of("GB"));
        mappings.put("JPY", Set.of("JP"));
        mappings.put("CHF", Set.of("CH"));
        mappings.put("CAD", Set.of("CA"));
        mappings.put("AUD", Set.of("AU"));
        mappings.put("CNY", Set.of("CN"));
        mappings.put("HKD", Set.of("HK"));
        mappings.put("SGD", Set.of("SG"));
        mappings.put("KRW", Set.of("KR"));
        mappings.put("INR", Set.of("IN"));
        mappings.put("BRL", Set.of("BR"));
        mappings.put("MXN", Set.of("MX"));
        mappings.put("ZAR", Set.of("ZA"));
        mappings.put("RUB", Set.of("RU"));
        mappings.put("SEK", Set.of("SE"));
        mappings.put("NOK", Set.of("NO"));
        mappings.put("DKK", Set.of("DK"));
        mappings.put("PLN", Set.of("PL"));
        mappings.put("CZK", Set.of("CZ"));
        mappings.put("HUF", Set.of("HU"));
        return Map.copyOf(mappings);
    }

    /**
     * Creates the sector-counterparty type mappings map
     */
    private static Map<String, Set<String>> createSectorCounterpartyMappings() {
        Map<String, Set<String>> mappings = new java.util.HashMap<>();
        mappings.put("BANKING", Set.of("BANK", "FINANCIAL_INSTITUTION"));
        mappings.put("CORPORATE_MANUFACTURING", Set.of("CORPORATE", "COMPANY"));
        mappings.put("CORPORATE_SERVICES", Set.of("CORPORATE", "COMPANY"));
        mappings.put("CORPORATE_RETAIL", Set.of("CORPORATE", "COMPANY"));
        mappings.put("CORPORATE_TECHNOLOGY", Set.of("CORPORATE", "COMPANY"));
        mappings.put("SOVEREIGN", Set.of("GOVERNMENT", "PUBLIC_SECTOR"));
        mappings.put("RETAIL", Set.of("INDIVIDUAL", "PRIVATE"));
        mappings.put("SME", Set.of("SMALL_BUSINESS", "CORPORATE"));
        mappings.put("REAL_ESTATE", Set.of("CORPORATE", "COMPANY", "INDIVIDUAL"));
        mappings.put("INSURANCE", Set.of("INSURANCE_COMPANY", "FINANCIAL_INSTITUTION"));
        return Map.copyOf(mappings);
    }

    /**
     * Creates the rating-risk category mappings map
     */
    private static Map<String, Set<String>> createRatingRiskMappings() {
        Map<String, Set<String>> mappings = new java.util.HashMap<>();
        mappings.put("AAA", Set.of("LOW_RISK", "INVESTMENT_GRADE"));
        mappings.put("AA", Set.of("LOW_RISK", "INVESTMENT_GRADE"));
        mappings.put("A", Set.of("LOW_RISK", "INVESTMENT_GRADE"));
        mappings.put("BBB", Set.of("MEDIUM_RISK", "INVESTMENT_GRADE"));
        mappings.put("BB", Set.of("MEDIUM_RISK", "SPECULATIVE_GRADE"));
        mappings.put("B", Set.of("HIGH_RISK", "SPECULATIVE_GRADE"));
        mappings.put("CCC", Set.of("HIGH_RISK", "SPECULATIVE_GRADE"));
        mappings.put("CC", Set.of("VERY_HIGH_RISK", "DEFAULT_RISK"));
        mappings.put("C", Set.of("VERY_HIGH_RISK", "DEFAULT_RISK"));
        mappings.put("D", Set.of("DEFAULT", "DEFAULT_RISK"));
        return Map.copyOf(mappings);
    }

    /**
     * Utility method to check if a string is blank (null, empty, or whitespace only)
     * 
     * @param value The string to check
     * @return true if the string is blank, false otherwise
     */
    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}