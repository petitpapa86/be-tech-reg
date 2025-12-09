package com.bcbs239.regtech.billing.infrastructure.validation;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

/**
 * Utility class for sanitizing user input to prevent injection attacks
 * and ensure data integrity in the billing module.
 */
@Component("billingInputSanitizer")
public class InputSanitizer {

    // Patterns for validation
    private static final Pattern HTML_PATTERN = Pattern.compile("<[^>]+>");
    private static final Pattern SCRIPT_PATTERN = Pattern.compile("(?i)<script[^>]*>.*?</script>");
    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
        "(?i)(union|select|insert|update|delete|drop|create|alter|exec|execute|script|javascript|vbscript|onload|onerror|onclick)"
    );
    private static final Pattern XSS_PATTERN = Pattern.compile(
        "(?i)(javascript:|vbscript:|onload=|onerror=|onclick=|onmouseover=|onfocus=|onblur=)"
    );
    
    // Allowed characters for different input types
    private static final Pattern ALPHANUMERIC_PATTERN = Pattern.compile("^[a-zA-Z0-9\\s\\-_\\.]+$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[+]?[0-9\\s\\-\\(\\)]+$");
    private static final Pattern CURRENCY_CODE_PATTERN = Pattern.compile("^[A-Z]{3}$");
    private static final Pattern STRIPE_ID_PATTERN = Pattern.compile("^[a-zA-Z0-9_]+$");

    /**
     * Sanitizes general text input by removing potentially dangerous content
     */
    public String sanitizeText(String input) {
        if (!StringUtils.hasText(input)) {
            return input;
        }

        String sanitized = input.trim();
        
        // Remove script tags
        sanitized = SCRIPT_PATTERN.matcher(sanitized).replaceAll("");
        
        // Remove HTML tags
        sanitized = HTML_PATTERN.matcher(sanitized).replaceAll("");
        
        // Remove potential XSS patterns
        sanitized = XSS_PATTERN.matcher(sanitized).replaceAll("");
        
        // Remove null bytes
        sanitized = sanitized.replace("\0", "");
        
        // Limit length to prevent DoS
        if (sanitized.length() > 1000) {
            sanitized = sanitized.substring(0, 1000);
        }
        
        return sanitized;
    }

    /**
     * Sanitizes and validates email addresses
     */
    public String sanitizeEmail(String email) {
        if (!StringUtils.hasText(email)) {
            return email;
        }

        String sanitized = email.trim().toLowerCase();
        
        // Basic sanitization
        sanitized = sanitized.replaceAll("[<>\"'&]", "");
        
        // Validate format
        if (!EMAIL_PATTERN.matcher(sanitized).matches()) {
            throw new IllegalArgumentException("Invalid email format: " + email);
        }
        
        // Length check
        if (sanitized.length() > 254) {
            throw new IllegalArgumentException("Email address too long");
        }
        
        return sanitized;
    }

    /**
     * Sanitizes phone numbers
     */
    public String sanitizePhoneNumber(String phone) {
        if (!StringUtils.hasText(phone)) {
            return phone;
        }

        String sanitized = phone.trim();
        
        // Remove potentially dangerous characters
        sanitized = sanitized.replaceAll("[<>\"'&;]", "");
        
        // Validate format
        if (!PHONE_PATTERN.matcher(sanitized).matches()) {
            throw new IllegalArgumentException("Invalid phone number format: " + phone);
        }
        
        // Length check
        if (sanitized.length() > 20) {
            throw new IllegalArgumentException("Phone number too long");
        }
        
        return sanitized;
    }

    /**
     * Sanitizes currency codes
     */
    public String sanitizeCurrencyCode(String currencyCode) {
        if (!StringUtils.hasText(currencyCode)) {
            return currencyCode;
        }

        String sanitized = currencyCode.trim().toUpperCase();
        
        // Validate format
        if (!CURRENCY_CODE_PATTERN.matcher(sanitized).matches()) {
            throw new IllegalArgumentException("Invalid currency code format: " + currencyCode);
        }
        
        return sanitized;
    }

    /**
     * Sanitizes Stripe IDs (customer IDs, subscription IDs, etc.)
     */
    public String sanitizeStripeId(String stripeId) {
        if (!StringUtils.hasText(stripeId)) {
            return stripeId;
        }

        String sanitized = stripeId.trim();
        
        // Remove potentially dangerous characters
        sanitized = sanitized.replaceAll("[<>\"'&;\\s]", "");
        
        // Validate format
        if (!STRIPE_ID_PATTERN.matcher(sanitized).matches()) {
            throw new IllegalArgumentException("Invalid Stripe ID format: " + stripeId);
        }
        
        // Length check (Stripe IDs are typically 18-34 characters)
        if (sanitized.length() < 10 || sanitized.length() > 50) {
            throw new IllegalArgumentException("Invalid Stripe ID length: " + stripeId);
        }
        
        return sanitized;
    }

    /**
     * Sanitizes alphanumeric input (names, descriptions, etc.)
     */
    public String sanitizeAlphanumeric(String input) {
        if (!StringUtils.hasText(input)) {
            return input;
        }

        String sanitized = sanitizeText(input);
        
        // Additional validation for alphanumeric content
        if (!ALPHANUMERIC_PATTERN.matcher(sanitized).matches()) {
            throw new IllegalArgumentException("Input contains invalid characters: " + input);
        }
        
        return sanitized;
    }

    /**
     * Checks for potential SQL injection patterns
     */
    public void validateNoSqlInjection(String input) {
        if (!StringUtils.hasText(input)) {
            return;
        }

        if (SQL_INJECTION_PATTERN.matcher(input.toLowerCase()).find()) {
            throw new IllegalArgumentException("Input contains potentially dangerous SQL patterns");
        }
    }

    /**
     * Validates that input doesn't exceed maximum length
     */
    public void validateMaxLength(String input, int maxLength, String fieldName) {
        if (input != null && input.length() > maxLength) {
            throw new IllegalArgumentException(
                String.format("%s exceeds maximum length of %d characters", fieldName, maxLength)
            );
        }
    }

    /**
     * Validates that input meets minimum length requirement
     */
    public void validateMinLength(String input, int minLength, String fieldName) {
        if (input != null && input.length() < minLength) {
            throw new IllegalArgumentException(
                String.format("%s must be at least %d characters long", fieldName, minLength)
            );
        }
    }
}

