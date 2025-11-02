package com.bcbs239.regtech.ingestion.infrastructure.service;

import com.bcbs239.regtech.core.shared.ErrorDetail;
import com.bcbs239.regtech.core.shared.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for validating business rules on parsed file data.
 * Validates currency codes, country codes, sector codes, and detects duplicates.
 */
@Service
@Slf4j
public class FileValidationService {

    // ISO 4217 Currency Codes (subset of commonly used ones)
    private static final Set<String> VALID_CURRENCY_CODES = Set.of(
        "USD", "EUR", "GBP", "JPY", "CHF", "CAD", "AUD", "NZD", "SEK", "NOK", "DKK",
        "PLN", "CZK", "HUF", "RON", "BGN", "HRK", "RSD", "MKD", "BAM", "ALL", "TRY",
        "RUB", "UAH", "BYN", "MDL", "GEL", "AMD", "AZN", "KZT", "UZS", "KGS", "TJS",
        "CNY", "HKD", "TWD", "KRW", "THB", "SGD", "MYR", "IDR", "PHP", "VND", "LAK",
        "KHR", "MMK", "BDT", "NPR", "LKR", "MVR", "PKR", "INR", "BTN", "AFN", "IRR",
        "IQD", "JOD", "KWD", "LBP", "OMR", "QAR", "SAR", "SYP", "AED", "YER", "BHD",
        "EGP", "LYD", "MAD", "TND", "DZD", "AOA", "BWP", "BIF", "XAF", "XOF", "CDF",
        "DJF", "ERN", "ETB", "GMD", "GHS", "GNF", "KES", "LSL", "LRD", "MGA", "MWK",
        "MUR", "MZN", "NAD", "NGN", "RWF", "STN", "SCR", "SLL", "SOS", "SZL", "TZS",
        "UGX", "ZAR", "ZMW", "ZWL", "ARS", "BOB", "BRL", "CLP", "COP", "CRC", "CUP",
        "DOP", "GTQ", "HNL", "JMD", "MXN", "NIO", "PAB", "PEN", "PYG", "SVC", "TTD",
        "UYU", "VES", "BBD", "BZD", "BMD", "KYD", "XCD", "AWG", "ANG", "SRD", "GYD",
        "FJD", "PGK", "SBD", "TOP", "VUV", "WST", "XPF"
    );

    // ISO 3166 Country Codes (Alpha-2)
    private static final Set<String> VALID_COUNTRY_CODES = Set.of(
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

    // Internal sector enumeration
    private static final Set<String> VALID_SECTOR_CODES = Set.of(
        "BANKING", "INSURANCE", "SECURITIES", "INVESTMENT_FUNDS", "PENSION_FUNDS", "ASSET_MANAGEMENT",
        "FINTECH", "PAYMENT_SERVICES", "CREDIT_UNIONS", "BUILDING_SOCIETIES", "COOPERATIVE_BANKS",
        "DEVELOPMENT_BANKS", "EXPORT_CREDIT_AGENCIES", "GOVERNMENT", "CENTRAL_BANKS", "MULTILATERAL",
        "SOVEREIGN", "MUNICIPAL", "CORPORATE", "SME", "RETAIL", "REAL_ESTATE", "INFRASTRUCTURE",
        "ENERGY", "UTILITIES", "TELECOMMUNICATIONS", "TECHNOLOGY", "HEALTHCARE", "PHARMACEUTICALS",
        "AUTOMOTIVE", "AEROSPACE", "DEFENSE", "MINING", "METALS", "CHEMICALS", "AGRICULTURE",
        "FOOD_BEVERAGE", "TEXTILES", "CONSTRUCTION", "TRANSPORTATION", "LOGISTICS", "RETAIL_TRADE",
        "WHOLESALE_TRADE", "HOSPITALITY", "ENTERTAINMENT", "MEDIA", "EDUCATION", "RESEARCH",
        "PROFESSIONAL_SERVICES", "OTHER"
    );

    /**
     * Validate business rules for parsed file data
     */
    public Result<ValidationResult> validateBusinessRules(ParsedFileData parsedData) {
        log.info("Starting business rule validation for file: {} with {} exposures", 
            parsedData.getFileName(), parsedData.getTotalCount());
        
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        
        // Track duplicate exposure IDs (should already be caught in parsing, but double-check)
        Set<String> seenExposureIds = new HashSet<>();
        
        for (ParsedFileData.ExposureRecord exposure : parsedData.getExposures()) {
            // Check for duplicates
            if (seenExposureIds.contains(exposure.getExposureId())) {
                errors.add(String.format("Duplicate exposure_id '%s' found at line %d", 
                    exposure.getExposureId(), exposure.getLineNumber()));
            } else {
                seenExposureIds.add(exposure.getExposureId());
            }
            
            // Validate currency code
            if (!VALID_CURRENCY_CODES.contains(exposure.getCurrency())) {
                errors.add(String.format("Invalid currency code '%s' at line %d. Must be a valid ISO 4217 code.", 
                    exposure.getCurrency(), exposure.getLineNumber()));
            }
            
            // Validate country code
            if (!VALID_COUNTRY_CODES.contains(exposure.getCountry())) {
                errors.add(String.format("Invalid country code '%s' at line %d. Must be a valid ISO 3166 Alpha-2 code.", 
                    exposure.getCountry(), exposure.getLineNumber()));
            }
            
            // Validate sector code
            if (!VALID_SECTOR_CODES.contains(exposure.getSector())) {
                errors.add(String.format("Invalid sector code '%s' at line %d. Must be one of: %s", 
                    exposure.getSector(), exposure.getLineNumber(), 
                    String.join(", ", VALID_SECTOR_CODES.stream().sorted().collect(Collectors.toList()))));
            }
        }
        
        // Add summary information
        Map<String, Long> currencyDistribution = parsedData.getExposures().stream()
            .collect(Collectors.groupingBy(ParsedFileData.ExposureRecord::getCurrency, Collectors.counting()));
        
        Map<String, Long> countryDistribution = parsedData.getExposures().stream()
            .collect(Collectors.groupingBy(ParsedFileData.ExposureRecord::getCountry, Collectors.counting()));
        
        Map<String, Long> sectorDistribution = parsedData.getExposures().stream()
            .collect(Collectors.groupingBy(ParsedFileData.ExposureRecord::getSector, Collectors.counting()));
        
        ValidationResult result = ValidationResult.builder()
            .valid(errors.isEmpty())
            .errors(errors)
            .warnings(warnings)
            .totalExposures(parsedData.getTotalCount())
            .uniqueExposureIds(seenExposureIds.size())
            .currencyDistribution(currencyDistribution)
            .countryDistribution(countryDistribution)
            .sectorDistribution(sectorDistribution)
            .build();
        
        if (errors.isEmpty()) {
            log.info("Business rule validation passed for file: {} with {} exposures", 
                parsedData.getFileName(), parsedData.getTotalCount());
            return Result.success(result);
        } else {
            log.warn("Business rule validation failed for file: {} with {} errors", 
                parsedData.getFileName(), errors.size());
            return Result.failure(ErrorDetail.of("BUSINESS_RULE_VALIDATION_FAILED", 
                String.format("File validation failed with %d errors: %s", 
                    errors.size(), String.join("; ", errors.subList(0, Math.min(5, errors.size()))))));
        }
    }

    /**
     * Validate structure of parsed file data (basic checks)
     */
    public Result<ValidationResult> validateStructure(ParsedFileData parsedData) {
        log.info("Starting structure validation for file: {}", parsedData.getFileName());
        
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        
        if (parsedData.getExposures() == null || parsedData.getExposures().isEmpty()) {
            errors.add("File contains no exposure records");
        }
        
        if (parsedData.getTotalCount() <= 0) {
            errors.add("Total count must be positive");
        }
        
        if (parsedData.getExposures() != null && parsedData.getExposures().size() != parsedData.getTotalCount()) {
            errors.add(String.format("Exposure count mismatch: expected %d, found %d", 
                parsedData.getTotalCount(), parsedData.getExposures().size()));
        }
        
        ValidationResult result = ValidationResult.builder()
            .valid(errors.isEmpty())
            .errors(errors)
            .warnings(warnings)
            .totalExposures(parsedData.getTotalCount())
            .uniqueExposureIds(parsedData.getExposures() != null ? 
                (int) parsedData.getExposures().stream().map(ParsedFileData.ExposureRecord::getExposureId).distinct().count() : 0)
            .build();
        
        if (errors.isEmpty()) {
            log.info("Structure validation passed for file: {}", parsedData.getFileName());
            return Result.success(result);
        } else {
            log.warn("Structure validation failed for file: {} with {} errors", 
                parsedData.getFileName(), errors.size());
            return Result.failure(ErrorDetail.of("STRUCTURE_VALIDATION_FAILED", 
                String.format("File structure validation failed: %s", String.join("; ", errors))));
        }
    }

    /**
     * Get list of valid currency codes
     */
    public Set<String> getValidCurrencyCodes() {
        return Collections.unmodifiableSet(VALID_CURRENCY_CODES);
    }

    /**
     * Get list of valid country codes
     */
    public Set<String> getValidCountryCodes() {
        return Collections.unmodifiableSet(VALID_COUNTRY_CODES);
    }

    /**
     * Get list of valid sector codes
     */
    public Set<String> getValidSectorCodes() {
        return Collections.unmodifiableSet(VALID_SECTOR_CODES);
    }
}