package com.bcbs239.regtech.iam.infrastructure.configuration;

import java.util.regex.Pattern;

/**
 * Type-safe configuration for password policy settings.
 * Defines password strength requirements and validation rules.
 */
public record PasswordPolicyConfiguration(
    int minLength,
    boolean requireUppercase,
    boolean requireLowercase,
    boolean requireDigits,
    boolean requireSpecialChars
) {
    
    // Regex patterns for password validation
    private static final Pattern UPPERCASE_PATTERN = Pattern.compile("[A-Z]");
    private static final Pattern LOWERCASE_PATTERN = Pattern.compile("[a-z]");
    private static final Pattern DIGIT_PATTERN = Pattern.compile("[0-9]");
    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile("[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]");
    
    /**
     * Gets the minimum password length
     */
    public int getMinLength() {
        return minLength;
    }
    
    /**
     * Checks if uppercase letters are required
     */
    public boolean isUppercaseRequired() {
        return requireUppercase;
    }
    
    /**
     * Checks if lowercase letters are required
     */
    public boolean isLowercaseRequired() {
        return requireLowercase;
    }
    
    /**
     * Checks if digits are required
     */
    public boolean areDigitsRequired() {
        return requireDigits;
    }
    
    /**
     * Checks if special characters are required
     */
    public boolean areSpecialCharsRequired() {
        return requireSpecialChars;
    }
    
    /**
     * Validates a password against the configured policy
     */
    public boolean isValidPassword(String password) {
        if (password == null) {
            return false;
        }
        
        // Check minimum length
        if (password.length() < minLength) {
            return false;
        }
        
        // Check uppercase requirement
        if (requireUppercase && !UPPERCASE_PATTERN.matcher(password).find()) {
            return false;
        }
        
        // Check lowercase requirement
        if (requireLowercase && !LOWERCASE_PATTERN.matcher(password).find()) {
            return false;
        }
        
        // Check digit requirement
        if (requireDigits && !DIGIT_PATTERN.matcher(password).find()) {
            return false;
        }
        
        // Check special character requirement
        if (requireSpecialChars && !SPECIAL_CHAR_PATTERN.matcher(password).find()) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Gets a human-readable description of the password policy
     */
    public String getPolicyDescription() {
        StringBuilder description = new StringBuilder();
        description.append("Password must be at least ").append(minLength).append(" characters long");
        
        if (requireUppercase) {
            description.append(", contain at least one uppercase letter");
        }
        if (requireLowercase) {
            description.append(", contain at least one lowercase letter");
        }
        if (requireDigits) {
            description.append(", contain at least one digit");
        }
        if (requireSpecialChars) {
            description.append(", contain at least one special character");
        }
        
        return description.toString();
    }
    
    /**
     * Validates password policy configuration
     */
    public void validate() {
        if (minLength < 8) {
            throw new IllegalStateException("Minimum password length should be at least 8 characters");
        }
        if (minLength > 128) {
            throw new IllegalStateException("Minimum password length should not exceed 128 characters");
        }
        
        // Ensure at least some requirements are enabled for security
        if (!requireUppercase && !requireLowercase && !requireDigits && !requireSpecialChars) {
            throw new IllegalStateException("At least one password requirement should be enabled");
        }
    }
    
    /**
     * Gets the password strength level based on requirements
     */
    public PasswordStrength getStrengthLevel() {
        int requirements = 0;
        if (requireUppercase) requirements++;
        if (requireLowercase) requirements++;
        if (requireDigits) requirements++;
        if (requireSpecialChars) requirements++;
        
        if (minLength >= 12 && requirements >= 3) {
            return PasswordStrength.STRONG;
        } else if (minLength >= 10 && requirements >= 2) {
            return PasswordStrength.MEDIUM;
        } else {
            return PasswordStrength.WEAK;
        }
    }
    
    public enum PasswordStrength {
        WEAK, MEDIUM, STRONG
    }
}

