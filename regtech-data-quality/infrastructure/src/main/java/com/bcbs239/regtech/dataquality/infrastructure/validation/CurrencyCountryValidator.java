package com.bcbs239.regtech.modules.dataquality.infrastructure.validation;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

/**
 * Utility class for validating currency-country consistency.
 * Checks if a currency is commonly used or official in a given country.
 */
@Component
public class CurrencyCountryValidator {
    
    /**
     * Mapping of countries to their primary/official currencies.
     * This is a simplified mapping for demonstration purposes.
     * In a production system, this would be more comprehensive and regularly updated.
     */
    private static final Map<String, Set<String>> COUNTRY_CURRENCIES = Map.of(
        // Major economies
        "US", Set.of("USD"),
        "GB", Set.of("GBP"),
        "DE", Set.of("EUR"),
        "FR", Set.of("EUR"),
        "IT", Set.of("EUR"),
        "ES", Set.of("EUR"),
        "NL", Set.of("EUR"),
        "BE", Set.of("EUR"),
        "AT", Set.of("EUR"),
        "PT", Set.of("EUR"),
        "IE", Set.of("EUR"),
        "FI", Set.of("EUR"),
        "GR", Set.of("EUR"),
        "LU", Set.of("EUR"),
        "MT", Set.of("EUR"),
        "CY", Set.of("EUR"),
        "SK", Set.of("EUR"),
        "SI", Set.of("EUR"),
        "EE", Set.of("EUR"),
        "LV", Set.of("EUR"),
        "LT", Set.of("EUR"),
        "HR", Set.of("EUR"),
        "JP", Set.of("JPY"),
        "CH", Set.of("CHF"),
        "CA", Set.of("CAD"),
        "AU", Set.of("AUD"),
        "NZ", Set.of("NZD"),
        "SE", Set.of("SEK"),
        "NO", Set.of("NOK"),
        "DK", Set.of("DKK"),
        "PL", Set.of("PLN"),
        "CZ", Set.of("CZK"),
        "HU", Set.of("HUF"),
        "RO", Set.of("RON"),
        "BG", Set.of("BGN"),
        "TR", Set.of("TRY"),
        "RU", Set.of("RUB"),
        "CN", Set.of("CNY"),
        "IN", Set.of("INR"),
        "BR", Set.of("BRL"),
        "MX", Set.of("MXN"),
        "AR", Set.of("ARS"),
        "CL", Set.of("CLP"),
        "CO", Set.of("COP"),
        "PE", Set.of("PEN"),
        "ZA", Set.of("ZAR"),
        "KR", Set.of("KRW"),
        "SG", Set.of("SGD"),
        "HK", Set.of("HKD"),
        "TW", Set.of("TWD"),
        "MY", Set.of("MYR"),
        "TH", Set.of("THB"),
        "ID", Set.of("IDR"),
        "PH", Set.of("PHP"),
        "VN", Set.of("VND"),
        "SA", Set.of("SAR"),
        "AE", Set.of("AED"),
        "QA", Set.of("QAR"),
        "KW", Set.of("KWD"),
        "BH", Set.of("BHD"),
        "OM", Set.of("OMR"),
        "JO", Set.of("JOD"),
        "LB", Set.of("LBP"),
        "EG", Set.of("EGP"),
        "IL", Set.of("ILS"),
        "MA", Set.of("MAD"),
        "TN", Set.of("TND"),
        "DZ", Set.of("DZD"),
        "NG", Set.of("NGN"),
        "KE", Set.of("KES"),
        "GH", Set.of("GHS"),
        "UG", Set.of("UGX"),
        "TZ", Set.of("TZS"),
        "ET", Set.of("ETB"),
        "MU", Set.of("MUR"),
        "BW", Set.of("BWP"),
        "NA", Set.of("NAD", "ZAR"), // Namibia uses both NAD and ZAR
        "SZ", Set.of("SZL", "ZAR"), // Eswatini uses both SZL and ZAR
        "LS", Set.of("LSL", "ZAR")  // Lesotho uses both LSL and ZAR
    );
    
    /**
     * Currencies that are widely accepted internationally and might be used
     * in countries other than their primary issuing country.
     */
    private static final Set<String> INTERNATIONAL_CURRENCIES = Set.of(
        "USD", "EUR", "GBP", "JPY", "CHF"
    );
    
    /**
     * Check if a currency is consistent with a country.
     * This includes both official currencies and widely accepted international currencies.
     * 
     * @param currency the currency code (ISO 4217)
     * @param country the country code (ISO 3166-1 alpha-2)
     * @return true if the currency is consistent with the country, false otherwise
     */
    public static boolean isConsistent(String currency, String country) {
        if (!CurrencyValidator.isValidISO4217(currency) || !CountryValidator.isValidISO3166(country)) {
            return false;
        }
        
        String normalizedCurrency = currency.trim().toUpperCase();
        String normalizedCountry = country.trim().toUpperCase();
        
        // Check if the currency is official for the country
        Set<String> countryCurrencies = COUNTRY_CURRENCIES.get(normalizedCountry);
        if (countryCurrencies != null && countryCurrencies.contains(normalizedCurrency)) {
            return true;
        }
        
        // Check if it's a widely accepted international currency
        // International currencies are generally acceptable for cross-border transactions
        if (INTERNATIONAL_CURRENCIES.contains(normalizedCurrency)) {
            return true;
        }
        
        // Special case: EUR is used in many EU countries not explicitly listed
        if ("EUR".equals(normalizedCurrency) && CountryValidator.isEUMember(normalizedCountry)) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Check if a currency is the primary/official currency of a country.
     * 
     * @param currency the currency code
     * @param country the country code
     * @return true if it's the primary currency, false otherwise
     */
    public static boolean isPrimaryCurrency(String currency, String country) {
        if (!CurrencyValidator.isValidISO4217(currency) || !CountryValidator.isValidISO3166(country)) {
            return false;
        }
        
        String normalizedCurrency = currency.trim().toUpperCase();
        String normalizedCountry = country.trim().toUpperCase();
        
        Set<String> countryCurrencies = COUNTRY_CURRENCIES.get(normalizedCountry);
        return countryCurrencies != null && countryCurrencies.contains(normalizedCurrency);
    }
    
    /**
     * Get the primary currencies for a country.
     * 
     * @param country the country code
     * @return set of primary currencies, or empty set if country not found
     */
    public static Set<String> getPrimaryCurrencies(String country) {
        if (!CountryValidator.isValidISO3166(country)) {
            return Set.of();
        }
        
        String normalizedCountry = country.trim().toUpperCase();
        return COUNTRY_CURRENCIES.getOrDefault(normalizedCountry, Set.of());
    }
    
    /**
     * Check if a currency is an international reserve currency.
     * 
     * @param currency the currency code
     * @return true if it's an international currency, false otherwise
     */
    public static boolean isInternationalCurrency(String currency) {
        if (!CurrencyValidator.isValidISO4217(currency)) {
            return false;
        }
        return INTERNATIONAL_CURRENCIES.contains(currency.trim().toUpperCase());
    }
    
    /**
     * Get all international currencies.
     * 
     * @return set of international currency codes
     */
    public static Set<String> getInternationalCurrencies() {
        return Set.copyOf(INTERNATIONAL_CURRENCIES);
    }
    
    /**
     * Get all countries that use a specific currency.
     * 
     * @param currency the currency code
     * @return set of country codes that use this currency
     */
    public static Set<String> getCountriesUsingCurrency(String currency) {
        if (!CurrencyValidator.isValidISO4217(currency)) {
            return Set.of();
        }
        
        String normalizedCurrency = currency.trim().toUpperCase();
        return COUNTRY_CURRENCIES.entrySet().stream()
            .filter(entry -> entry.getValue().contains(normalizedCurrency))
            .map(Map.Entry::getKey)
            .collect(java.util.stream.Collectors.toSet());
    }
}