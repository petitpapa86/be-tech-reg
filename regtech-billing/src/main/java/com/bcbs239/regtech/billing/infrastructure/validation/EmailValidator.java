package com.bcbs239.regtech.billing.infrastructure.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

/**
 * Validator for email addresses with enhanced security checks.
 */
public class EmailValidator implements ConstraintValidator<ValidEmail, String> {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
    );
    
    private static final Pattern DANGEROUS_PATTERN = Pattern.compile(
        "(?i)(javascript:|vbscript:|data:|<|>|script|iframe|object|embed)"
    );

    @Autowired
    private InputSanitizer inputSanitizer;

    private boolean allowEmpty;
    private int maxLength;

    @Override
    public void initialize(ValidEmail constraintAnnotation) {
        this.allowEmpty = constraintAnnotation.allowEmpty();
        this.maxLength = constraintAnnotation.maxLength();
    }

    @Override
    public boolean isValid(String email, ConstraintValidatorContext context) {
        // Handle null/empty cases
        if (!StringUtils.hasText(email)) {
            return allowEmpty;
        }

        try {
            // Check for dangerous patterns first
            if (DANGEROUS_PATTERN.matcher(email).find()) {
                addConstraintViolation(context, "Email contains potentially dangerous content");
                return false;
            }

            // Length check
            if (email.length() > maxLength) {
                addConstraintViolation(context, "Email address is too long (max " + maxLength + " characters)");
                return false;
            }

            // Sanitize and validate
            String sanitizedEmail = inputSanitizer.sanitizeEmail(email);
            
            // Basic format validation
            if (!EMAIL_PATTERN.matcher(sanitizedEmail).matches()) {
                addConstraintViolation(context, "Invalid email format");
                return false;
            }

            // Additional security checks
            String[] parts = sanitizedEmail.split("@");
            if (parts.length != 2) {
                addConstraintViolation(context, "Invalid email format");
                return false;
            }

            String localPart = parts[0];
            String domainPart = parts[1];

            // Local part validation
            if (localPart.length() > 64) {
                addConstraintViolation(context, "Email local part is too long");
                return false;
            }

            // Domain part validation
            if (domainPart.length() > 253) {
                addConstraintViolation(context, "Email domain part is too long");
                return false;
            }

            // Check for consecutive dots
            if (sanitizedEmail.contains("..")) {
                addConstraintViolation(context, "Email contains consecutive dots");
                return false;
            }

            // Check for leading/trailing dots in local part
            if (localPart.startsWith(".") || localPart.endsWith(".")) {
                addConstraintViolation(context, "Email local part cannot start or end with a dot");
                return false;
            }

            return true;

        } catch (IllegalArgumentException e) {
            addConstraintViolation(context, e.getMessage());
            return false;
        }
    }

    private void addConstraintViolation(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
    }
}
