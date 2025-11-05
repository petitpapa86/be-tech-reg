package com.bcbs239.regtech.billing.infrastructure.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

/**
 * Validator for phone numbers with enhanced security checks.
 */
public class PhoneNumberValidator implements ConstraintValidator<ValidPhoneNumber, String> {

    private static final Pattern PHONE_PATTERN = Pattern.compile("^[+]?[0-9\\s\\-\\(\\)]+$");
    private static final Pattern DANGEROUS_PATTERN = Pattern.compile(
        "(?i)(javascript:|vbscript:|data:|<|>|script|iframe|object|embed)"
    );

    @Autowired
    private InputSanitizer inputSanitizer;

    private boolean allowEmpty;
    private int maxLength;
    private int minLength;

    @Override
    public void initialize(ValidPhoneNumber constraintAnnotation) {
        this.allowEmpty = constraintAnnotation.allowEmpty();
        this.maxLength = constraintAnnotation.maxLength();
        this.minLength = constraintAnnotation.minLength();
    }

    @Override
    public boolean isValid(String phoneNumber, ConstraintValidatorContext context) {
        // Handle null/empty cases
        if (!StringUtils.hasText(phoneNumber)) {
            return allowEmpty;
        }

        try {
            // Check for dangerous patterns first
            if (DANGEROUS_PATTERN.matcher(phoneNumber).find()) {
                addConstraintViolation(context, "Phone number contains potentially dangerous content");
                return false;
            }

            // Length checks
            if (phoneNumber.length() > maxLength) {
                addConstraintViolation(context, "Phone number is too long (max " + maxLength + " characters)");
                return false;
            }

            if (phoneNumber.length() < minLength) {
                addConstraintViolation(context, "Phone number is too short (min " + minLength + " characters)");
                return false;
            }

            // Sanitize and validate
            String sanitizedPhone = inputSanitizer.sanitizePhoneNumber(phoneNumber);
            
            // Basic format validation
            if (!PHONE_PATTERN.matcher(sanitizedPhone).matches()) {
                addConstraintViolation(context, "Invalid phone number format");
                return false;
            }

            // Extract digits only for additional validation
            String digitsOnly = sanitizedPhone.replaceAll("[^0-9]", "");
            
            // Check minimum digits (excluding country code)
            if (digitsOnly.length() < 7) {
                addConstraintViolation(context, "Phone number must contain at least 7 digits");
                return false;
            }

            // Check maximum digits
            if (digitsOnly.length() > 15) {
                addConstraintViolation(context, "Phone number cannot contain more than 15 digits");
                return false;
            }

            // Check for suspicious patterns (all same digits, sequential digits)
            if (isAllSameDigits(digitsOnly) || isSequentialDigits(digitsOnly)) {
                addConstraintViolation(context, "Phone number appears to be invalid");
                return false;
            }

            return true;

        } catch (IllegalArgumentException e) {
            addConstraintViolation(context, e.getMessage());
            return false;
        }
    }

    private boolean isAllSameDigits(String digits) {
        if (digits.length() < 4) return false;
        char firstDigit = digits.charAt(0);
        return digits.chars().allMatch(c -> c == firstDigit);
    }

    private boolean isSequentialDigits(String digits) {
        if (digits.length() < 4) return false;
        for (int i = 1; i < digits.length(); i++) {
            if (digits.charAt(i) != digits.charAt(i-1) + 1) {
                return false;
            }
        }
        return true;
    }

    private void addConstraintViolation(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
    }
}

