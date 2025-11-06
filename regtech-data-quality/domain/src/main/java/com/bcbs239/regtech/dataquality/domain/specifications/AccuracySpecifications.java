package com.bcbs239.regtech.dataquality.domain.specifications;

import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.specifications.Specification;
import com.bcbs239.regtech.dataquality.domain.validation.ExposureRecord;

import java.math.BigDecimal;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Specifications for Accuracy quality dimension validation.
 * 
 * Accuracy ensures that data formats, ranges, and business logic correctness
 * are validated according to BCBS 239 regulatory requirements.
 */
public class AccuracySpecifications {

    // Maximum reasonable amount: 10 billion EUR
    private static final BigDecimal MAX_REASONABLE_AMOUNT = new BigDecimal("10000000000");
    
    // LEI format pattern: 20 alphanumeric characters
    private static final Pattern LEI_PATTERN = Pattern.compile("^[A-Z0-9]{20}$");
    
    // ISO 4217 currency codes (subset of most common ones)
    private static final Set<String> VALID_CURRENCIES = Set.of(
        "USD", "EUR", "GBP", "JPY", "CHF", "CAD", "AUD", "SEK", "NOK", "DKK",
        "PLN", "CZK", "HUF", "BGN", "RON", "HRK", "RSD", "BAM", "MKD", "ALL",
        "CNY", "HKD", "SGD", "KRW", "INR", "THB", "MYR", "IDR", "PHP", "VND",
        "BRL", "MXN", "ARS", "CLP", "COP", "PEN", "UYU", "ZAR", "EGP", "MAD",
        "TND", "NGN", "GHS", "KES", "UGX", "TZS", "ZMW", "BWP", "MUR", "SCR"
    );
    
    // ISO 3166-1 alpha-2 country codes (subset of most common ones)
    private static final Set<String> VALID_COUNTRIES = Set.of(
        "US", "GB", "DE", "FR", "IT", "ES", "NL", "BE", "AT", "CH", "SE", "NO",
        "DK", "FI", "IE", "PT", "GR", "PL", "CZ", "HU", "SK", "SI", "EE", "LV",
        "LT", "BG", "RO", "HR", "CY", "MT", "LU", "JP", "CN", "HK", "SG", "KR",
        "IN", "TH", "MY", "ID", "PH", "VN", "AU", "NZ", "CA", "MX", "BR", "AR",
        "CL", "CO", "PE", "UY", "ZA", "EG", "MA", "TN", "NG", "GH", "KE", "UG",
        "TZ", "ZM", "BW", "MU", "SC", "RU", "UA", "BY", "MD", "GE", "AM", "AZ",
        "KZ", "UZ", "KG", "TJ", "TM", "MN", "TR", "IL", "SA", "AE", "QA", "KW",
        "BH", "OM", "JO", "LB", "SY", "IQ", "IR", "AF", "PK", "BD", "LK", "NP",
        "BT", "MM", "LA", "KH", "BN", "TL", "FJ", "PG", "SB", "VU", "NC", "PF"
    );

    /**
     * Validates that the exposure amount is positive.
     * Negative or zero amounts are not valid for exposure reporting.
     * 
     * @return Specification that validates positive amount
     */
    public static Specification<ExposureRecord> hasPositiveAmount() {
        return exposure -> {
            if (exposure.amount() != null && 
                exposure.amount().compareTo(BigDecimal.ZERO) <= 0) {
                return Result.failure(ErrorDetail.of("ACCURACY_AMOUNT_NOT_POSITIVE", 
                    ErrorType.BUSINESS_RULE_ERROR, "Amount must be positive", "amount"));
            }
            return Result.success();
        };
    }

    /**
     * Validates that the currency code is a valid ISO 4217 code.
     * Only standard international currency codes are accepted.
     * 
     * @return Specification that validates currency format
     */
    public static Specification<ExposureRecord> hasValidCurrency() {
        return exposure -> {
            if (exposure.currency() != null && 
                !isValidISO4217Currency(exposure.currency())) {
                return Result.failure(ErrorDetail.of("ACCURACY_INVALID_CURRENCY", 
                    "Currency must be valid ISO 4217 code", "currency"));
            }
            return Result.success();
        };
    }

    /**
     * Validates that the country code is a valid ISO 3166-1 alpha-2 code.
     * Only standard international country codes are accepted.
     * 
     * @return Specification that validates country format
     */
    public static Specification<ExposureRecord> hasValidCountry() {
        return exposure -> {
            if (exposure.country() != null && 
                !isValidISO3166Country(exposure.country())) {
                return Result.failure(ErrorDetail.of("ACCURACY_INVALID_COUNTRY", 
                    "Country must be valid ISO 3166-1 alpha-2 code", "country"));
            }
            return Result.success();
        };
    }

    /**
     * Validates that the LEI code has the correct format.
     * LEI codes must be exactly 20 alphanumeric characters.
     * 
     * @return Specification that validates LEI format
     */
    public static Specification<ExposureRecord> hasValidLeiFormat() {
        return exposure -> {
            if (exposure.leiCode() != null && 
                !isValidLeiFormat(exposure.leiCode())) {
                return Result.failure(ErrorDetail.of("ACCURACY_INVALID_LEI_FORMAT", 
                    "LEI code must be 20 alphanumeric characters", "lei_code"));
            }
            return Result.success();
        };
    }

    /**
     * Validates that the exposure amount is within reasonable bounds.
     * Amounts of 10 billion EUR or more are considered unreasonable and likely errors.
     * 
     * @return Specification that validates reasonable amount bounds
     */
    public static Specification<ExposureRecord> hasReasonableAmount() {
        return exposure -> {
            if (exposure.amount() != null && 
                exposure.amount().compareTo(MAX_REASONABLE_AMOUNT) >= 0) {
                return Result.failure(ErrorDetail.of("ACCURACY_AMOUNT_UNREASONABLE", 
                    "Amount exceeds reasonable bounds (10B EUR)", "amount"));
            }
            return Result.success();
        };
    }

    /**
     * Validates if a currency code is a valid ISO 4217 code.
     * 
     * @param currency The currency code to validate
     * @return true if valid, false otherwise
     */
    private static boolean isValidISO4217Currency(String currency) {
        if (currency == null || currency.length() != 3) {
            return false;
        }
        return VALID_CURRENCIES.contains(currency.toUpperCase());
    }

    /**
     * Validates if a country code is a valid ISO 3166-1 alpha-2 code.
     * 
     * @param country The country code to validate
     * @return true if valid, false otherwise
     */
    private static boolean isValidISO3166Country(String country) {
        if (country == null || country.length() != 2) {
            return false;
        }
        return VALID_COUNTRIES.contains(country.toUpperCase());
    }

    /**
     * Validates if a LEI code has the correct format.
     * LEI codes must be exactly 20 alphanumeric characters.
     * 
     * @param leiCode The LEI code to validate
     * @return true if valid format, false otherwise
     */
    private static boolean isValidLeiFormat(String leiCode) {
        if (leiCode == null) {
            return false;
        }
        return LEI_PATTERN.matcher(leiCode.toUpperCase()).matches();
    }
}

