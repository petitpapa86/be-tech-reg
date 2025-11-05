package com.bcbs239.regtech.dataquality.infrastructure.validation;

import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Utility class for validating currency codes according to ISO 4217 standard.
 */
@Component
public class CurrencyValidator {
    
    /**
     * Set of valid ISO 4217 currency codes.
     * This is a subset of commonly used currencies for demonstration.
     * In a production system, this would be loaded from a comprehensive reference data source.
     */
    private static final Set<String> VALID_CURRENCIES = Set.of(
        "USD", "EUR", "GBP", "JPY", "CHF", "CAD", "AUD", "NZD", "SEK", "NOK", "DKK",
        "PLN", "CZK", "HUF", "RON", "BGN", "HRK", "RSD", "MKD", "BAM", "ALL", "TRY",
        "RUB", "UAH", "BYN", "MDL", "GEL", "AMD", "AZN", "KZT", "KGS", "TJS", "TMT",
        "UZS", "CNY", "HKD", "TWD", "KRW", "SGD", "MYR", "THB", "IDR", "PHP", "VND",
        "LAK", "KHR", "MMK", "BDT", "BTN", "INR", "LKR", "MVR", "NPR", "PKR", "AFN",
        "IRR", "IQD", "JOD", "KWD", "LBP", "OMR", "QAR", "SAR", "SYP", "AED", "YER",
        "BHD", "EGP", "ILS", "LYD", "MAD", "SDG", "TND", "DZD", "AOA", "BWP", "BIF",
        "XAF", "CDF", "DJF", "ERN", "ETB", "GMD", "GHS", "GNF", "KES", "LSL", "LRD",
        "MGA", "MWK", "MUR", "MZN", "NAD", "NGN", "RWF", "STN", "SCR", "SLL", "SOS",
        "SZL", "TZS", "UGX", "XOF", "ZAR", "ZMW", "ZWL", "ARS", "BOB", "BRL", "CLP",
        "COP", "CRC", "CUP", "DOP", "GTQ", "HNL", "JMD", "MXN", "NIO", "PAB", "PEN",
        "PYG", "SVC", "TTD", "UYU", "VES", "BBD", "BZD", "BMD", "KYD", "XCD", "AWG",
        "ANG", "SRD", "GYD", "FKP", "GIP", "SHP", "FJD", "SBD", "TOP", "VUV", "WST",
        "PGK", "NCF", "XPF"
    );
    
    /**
     * Validate if a currency code is a valid ISO 4217 code.
     * 
     * @param currencyCode the currency code to validate
     * @return true if the currency code is valid, false otherwise
     */
    public static boolean isValidISO4217(String currencyCode) {
        if (currencyCode == null || currencyCode.trim().isEmpty()) {
            return false;
        }
        
        String normalizedCode = currencyCode.trim().toUpperCase();
        
        // Check format: exactly 3 alphabetic characters
        if (normalizedCode.length() != 3) {
            return false;
        }
        
        if (!normalizedCode.matches("^[A-Z]{3}$")) {
            return false;
        }
        
        // Check against known valid currencies
        return VALID_CURRENCIES.contains(normalizedCode);
    }
    
    /**
     * Get the normalized currency code (uppercase, trimmed).
     * 
     * @param currencyCode the currency code to normalize
     * @return the normalized currency code, or null if invalid
     */
    public static String normalize(String currencyCode) {
        if (!isValidISO4217(currencyCode)) {
            return null;
        }
        return currencyCode.trim().toUpperCase();
    }
    
    /**
     * Check if a currency is a major trading currency.
     * 
     * @param currencyCode the currency code to check
     * @return true if it's a major currency, false otherwise
     */
    public static boolean isMajorCurrency(String currencyCode) {
        if (!isValidISO4217(currencyCode)) {
            return false;
        }
        
        Set<String> majorCurrencies = Set.of("USD", "EUR", "GBP", "JPY", "CHF", "CAD", "AUD");
        return majorCurrencies.contains(currencyCode.trim().toUpperCase());
    }
    
    /**
     * Get all valid currency codes.
     * 
     * @return set of all valid currency codes
     */
    public static Set<String> getAllValidCurrencies() {
        return Set.copyOf(VALID_CURRENCIES);
    }
}

