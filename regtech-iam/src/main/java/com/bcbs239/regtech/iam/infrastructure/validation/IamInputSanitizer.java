package com.bcbs239.regtech.iam.infrastructure.validation;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

/**
 * Utility class for sanitizing user input in the IAM module
 * to prevent injection attacks and ensure data integrity.
 */
@Component
public class IamInputSanitizer {

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
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9\\._\\-]+$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
    private static final Pattern ORGANIZATION_PATTERN = Pattern.compile("^[a-zA-Z0-9\\s\\-_\\.]+$");

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
     * Sanitizes and validates usernames
     */
    public String sanitizeUsername(String username) {
        if (!StringUtils.hasText(username)) {
            return username;
        }

        String sanitized = username.trim().toLowerCase();
        
        // Remove potentially dangerous characters
        sanitized = sanitized.replaceAll("[<>\"'&;\\s]", "");
        
        // Validate format
        if (!USERNAME_PATTERN.matcher(sanitized).matches()) {
            throw new IllegalArgumentException("Invalid username format: " + username);
        }
        
        // Length checks
        if (sanitized.length() < 3) {
            throw new IllegalArgumentException("Username must be at least 3 characters long");
        }
        if (sanitized.length() > 50) {
            throw new IllegalArgumentException("Username cannot exceed 50 characters");
        }
        
        return sanitized;
    }
}    /**
   
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
     * Sanitizes organization names
     */
    public String sanitizeOrganizationName(String organizationName) {
        if (!StringUtils.hasText(organizationName)) {
            return organizationName;
        }

        String sanitized = organizationName.trim();
        
        // Remove potentially dangerous characters
        sanitized = sanitized.replaceAll("[<>\"'&;]", "");
        
        // Validate format
        if (!ORGANIZATION_PATTERN.matcher(sanitized).matches()) {
            throw new IllegalArgumentException("Invalid organization name format: " + organizationName);
        }
        
        // Length checks
        if (sanitized.length() < 2) {
            throw new IllegalArgumentException("Organization name must be at least 2 characters long");
        }
        if (sanitized.length() > 100) {
            throw new IllegalArgumentException("Organization name cannot exceed 100 characters");
        }
        
        return sanitized;
    }

    /**
     * Validates password strength and sanitizes if needed
     */
    public void validatePassword(String password) {
        if (!StringUtils.hasText(password)) {
            throw new IllegalArgumentException("Password cannot be empty");
        }

        // Check for null bytes and other dangerous characters
        if (password.contains("\0")) {
            throw new IllegalArgumentException("Password contains invalid characters");
        }

        // Length checks
        if (password.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters long");
        }
        if (password.length() > 128) {
            throw new IllegalArgumentException("Password cannot exceed 128 characters");
        }

        // Check for common weak passwords
        String lowerPassword = password.toLowerCase();
        String[] commonPasswords = {"password", "123456", "qwerty", "admin", "letmein", "welcome"};
        for (String common : commonPasswords) {
            if (lowerPassword.contains(common)) {
                throw new IllegalArgumentException("Password contains common weak patterns");
            }
        }
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