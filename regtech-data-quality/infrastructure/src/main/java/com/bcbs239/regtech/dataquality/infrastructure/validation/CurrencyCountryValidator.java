package com.bcbs239.regtech.dataquality.infrastructure.validation;

import com.bcbs239.regtech.dataquality.domain.validation.validators.CountryValidator;
import com.bcbs239.regtech.dataquality.domain.validation.validators.CurrencyValidator;
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
    private static final Map<String, Set<String>> COUNTRY_CURRENCIES = Map.ofEntries(
        Map.entry("US", Set.of("USD")),
        Map.entry("GB", Set.of("GBP")),
        Map.entry("DE", Set.of("EUR")),
        Map.entry("FR", Set.of("EUR")),
        Map.entry("IT", Set.of("EUR")),
        Map.entry("ES", Set.of("EUR")),
        Map.entry("NL", Set.of("EUR")),
        Map.entry("BE", Set.of("EUR")),
        Map.entry("AT", Set.of("EUR")),
        Map.entry("PT", Set.of("EUR")),
        Map.entry("IE", Set.of("EUR")),
        Map.entry("FI", Set.of("EUR")),
        Map.entry("GR", Set.of("EUR")),
        Map.entry("LU", Set.of("EUR")),
        Map.entry("MT", Set.of("EUR")),
        Map.entry("CY", Set.of("EUR")),
        Map.entry("SK", Set.of("EUR")),
        Map.entry("SI", Set.of("EUR")),
        Map.entry("EE", Set.of("EUR")),
        Map.entry("LV", Set.of("EUR")),
        Map.entry("LT", Set.of("EUR")),
        Map.entry("HR", Set.of("EUR")),
        Map.entry("JP", Set.of("JPY")),
        Map.entry("CH", Set.of("CHF")),
        Map.entry("CA", Set.of("CAD")),
        Map.entry("AU", Set.of("AUD")),
        Map.entry("NZ", Set.of("NZD")),
        Map.entry("SE", Set.of("SEK")),
        Map.entry("NO", Set.of("NOK")),
        Map.entry("DK", Set.of("DKK")),
        Map.entry("PL", Set.of("PLN")),
        Map.entry("CZ", Set.of("CZK")),
        Map.entry("HU", Set.of("HUF")),
        Map.entry("RO", Set.of("RON")),
        Map.entry("BG", Set.of("BGN")),
        Map.entry("TR", Set.of("TRY")),
        Map.entry("RU", Set.of("RUB")),
        Map.entry("CN", Set.of("CNY")),
        Map.entry("IN", Set.of("INR")),
        Map.entry("BR", Set.of("BRL")),
        Map.entry("MX", Set.of("MXN")),
        Map.entry("AR", Set.of("ARS")),
        Map.entry("CL", Set.of("CLP")),
        Map.entry("CO", Set.of("COP")),
        Map.entry("PE", Set.of("PEN")),
        Map.entry("ZA", Set.of("ZAR")),
        Map.entry("KR", Set.of("KRW")),
        Map.entry("SG", Set.of("SGD")),
        Map.entry("HK", Set.of("HKD")),
        Map.entry("TW", Set.of("TWD")),
        Map.entry("MY", Set.of("MYR")),
        Map.entry("TH", Set.of("THB")),
        Map.entry("ID", Set.of("IDR")),
        Map.entry("PH", Set.of("PHP")),
        Map.entry("VN", Set.of("VND")),
        Map.entry("SA", Set.of("SAR")),
        Map.entry("AE", Set.of("AED")),
        Map.entry("QA", Set.of("QAR")),
        Map.entry("KW", Set.of("KWD")),
        Map.entry("BH", Set.of("BHD")),
        Map.entry("OM", Set.of("OMR")),
        Map.entry("JO", Set.of("JOD")),
        Map.entry("LB", Set.of("LBP")),
        Map.entry("EG", Set.of("EGP")),
        Map.entry("IL", Set.of("ILS")),
        Map.entry("MA", Set.of("MAD")),
        Map.entry("TN", Set.of("TND")),
        Map.entry("DZ", Set.of("DZD")),
        Map.entry("NG", Set.of("NGN")),
        Map.entry("KE", Set.of("KES")),
        Map.entry("GH", Set.of("GHS")),
        Map.entry("UG", Set.of("UGX")),
        Map.entry("TZ", Set.of("TZS")),
        Map.entry("ET", Set.of("ETB")),
        Map.entry("MU", Set.of("MUR")),
        Map.entry("BW", Set.of("BWP")),
        Map.entry("NA", Set.of("NAD", "ZAR")), // Namibia uses both NAD and ZAR
        Map.entry("SZ", Set.of("SZL", "ZAR")), // Eswatini uses both SZL and ZAR
        Map.entry("LS", Set.of("LSL", "ZAR"))  // Lesotho uses both LSL and ZAR
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

