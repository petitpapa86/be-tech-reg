package com.bcbs239.regtech.modules.dataquality.infrastructure.validation;

import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Utility class for validating country codes according to ISO 3166-1 alpha-2 standard.
 */
@Component
public class CountryValidator {
    
    /**
     * Set of valid ISO 3166-1 alpha-2 country codes.
     * This includes all officially assigned country codes.
     */
    private static final Set<String> VALID_COUNTRIES = Set.of(
        "AD", "AE", "AF", "AG", "AI", "AL", "AM", "AO", "AQ", "AR", "AS", "AT", "AU", "AW", "AX", "AZ",
        "BA", "BB", "BD", "BE", "BF", "BG", "BH", "BI", "BJ", "BL", "BM", "BN", "BO", "BQ", "BR", "BS",
        "BT", "BV", "BW", "BY", "BZ", "CA", "CC", "CD", "CF", "CG", "CH", "CI", "CK", "CL", "CM", "CN",
        "CO", "CR", "CU", "CV", "CW", "CX", "CY", "CZ", "DE", "DJ", "DK", "DM", "DO", "DZ", "EC", "EE",
        "EG", "EH", "ER", "ES", "ET", "FI", "FJ", "FK", "FM", "FO", "FR", "GA", "GB", "GD", "GE", "GF",
        "GG", "GH", "GI", "GL", "GM", "GN", "GP", "GQ", "GR", "GS", "GT", "GU", "GW", "GY", "HK", "HM",
        "HN", "HR", "HT", "HU", "ID", "IE", "IL", "IM", "IN", "IO", "IQ", "IR", "IS", "IT", "JE", "JM",
        "JO", "JP", "KE", "KG", "KH", "KI", "KM", "KN", "KP", "KR", "KW", "KY", "KZ", "LA", "LB", "LC",
        "LI", "LK", "LR", "LS", "LT", "LU", "LV", "LY", "MA", "MC", "MD", "ME", "MF", "MG", "MH", "MK",
        "ML", "MM", "MN", "MO", "MP", "MQ", "MR", "MS", "MT", "MU", "MV", "MW", "MX", "MY", "MZ", "NA",
        "NC", "NE", "NF", "NG", "NI", "NL", "NO", "NP", "NR", "NU", "NZ", "OM", "PA", "PE", "PF", "PG",
        "PH", "PK", "PL", "PM", "PN", "PR", "PS", "PT", "PW", "PY", "QA", "RE", "RO", "RS", "RU", "RW",
        "SA", "SB", "SC", "SD", "SE", "SG", "SH", "SI", "SJ", "SK", "SL", "SM", "SN", "SO", "SR", "SS",
        "ST", "SV", "SX", "SY", "SZ", "TC", "TD", "TF", "TG", "TH", "TJ", "TK", "TL", "TM", "TN", "TO",
        "TR", "TT", "TV", "TW", "TZ", "UA", "UG", "UM", "US", "UY", "UZ", "VA", "VC", "VE", "VG", "VI",
        "VN", "VU", "WF", "WS", "YE", "YT", "ZA", "ZM", "ZW"
    );
    
    /**
     * Set of EU member countries (as of 2024).
     */
    private static final Set<String> EU_COUNTRIES = Set.of(
        "AT", "BE", "BG", "HR", "CY", "CZ", "DK", "EE", "FI", "FR", "DE", "GR", "HU", "IE", "IT", "LV",
        "LT", "LU", "MT", "NL", "PL", "PT", "RO", "SK", "SI", "ES", "SE"
    );
    
    /**
     * Set of G20 countries.
     */
    private static final Set<String> G20_COUNTRIES = Set.of(
        "AR", "AU", "BR", "CA", "CN", "FR", "DE", "IN", "ID", "IT", "JP", "KR", "MX", "RU", "SA", "ZA",
        "TR", "GB", "US"
    );
    
    /**
     * Validate if a country code is a valid ISO 3166-1 alpha-2 code.
     * 
     * @param countryCode the country code to validate
     * @return true if the country code is valid, false otherwise
     */
    public static boolean isValidISO3166(String countryCode) {
        if (countryCode == null || countryCode.trim().isEmpty()) {
            return false;
        }
        
        String normalizedCode = countryCode.trim().toUpperCase();
        
        // Check format: exactly 2 alphabetic characters
        if (normalizedCode.length() != 2) {
            return false;
        }
        
        if (!normalizedCode.matches("^[A-Z]{2}$")) {
            return false;
        }
        
        // Check against known valid countries
        return VALID_COUNTRIES.contains(normalizedCode);
    }
    
    /**
     * Get the normalized country code (uppercase, trimmed).
     * 
     * @param countryCode the country code to normalize
     * @return the normalized country code, or null if invalid
     */
    public static String normalize(String countryCode) {
        if (!isValidISO3166(countryCode)) {
            return null;
        }
        return countryCode.trim().toUpperCase();
    }
    
    /**
     * Check if a country is an EU member.
     * 
     * @param countryCode the country code to check
     * @return true if it's an EU member, false otherwise
     */
    public static boolean isEUMember(String countryCode) {
        if (!isValidISO3166(countryCode)) {
            return false;
        }
        return EU_COUNTRIES.contains(countryCode.trim().toUpperCase());
    }
    
    /**
     * Check if a country is a G20 member.
     * 
     * @param countryCode the country code to check
     * @return true if it's a G20 member, false otherwise
     */
    public static boolean isG20Member(String countryCode) {
        if (!isValidISO3166(countryCode)) {
            return false;
        }
        return G20_COUNTRIES.contains(countryCode.trim().toUpperCase());
    }
    
    /**
     * Get all valid country codes.
     * 
     * @return set of all valid country codes
     */
    public static Set<String> getAllValidCountries() {
        return Set.copyOf(VALID_COUNTRIES);
    }
    
    /**
     * Get all EU member country codes.
     * 
     * @return set of EU member country codes
     */
    public static Set<String> getEUCountries() {
        return Set.copyOf(EU_COUNTRIES);
    }
    
    /**
     * Get all G20 member country codes.
     * 
     * @return set of G20 member country codes
     */
    public static Set<String> getG20Countries() {
        return Set.copyOf(G20_COUNTRIES);
    }
}