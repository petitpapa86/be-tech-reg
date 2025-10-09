package com.bcbs239.regtech.billing.infrastructure.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

/**
 * Validator for sanitized text input with enhanced security checks.
 */
public class SanitizedTextValidator implements ConstraintValidator<ValidSanitizedText, String> {

    @Autowired
    private InputSanitizer inputSanitizer;

    private boolean allowEmpty;
    private int maxLength;
    private int minLength;
    private boolean alphanumericOnly;
    private boolean checkSqlInjection;

    @Override
    public void initialize(ValidSanitizedText constraintAnnotation) {
        this.allowEmpty = constraintAnnotation.allowEmpty();
        this.maxLength = constraintAnnotation.maxLength();
        this.minLength = constraintAnnotation.minLength();
        this.alphanumericOnly = constraintAnnotation.alphanumericOnly();
        this.checkSqlInjection = constraintAnnotation.checkSqlInjection();
    }

    @Override
    public boolean isValid(String text, ConstraintValidatorContext context) {
        // Handle null/empty cases
        if (!StringUtils.hasText(text)) {
            return allowEmpty;
        }

        try {
            // Length checks before sanitization
            if (text.length() > maxLength) {
                addConstraintViolation(context, "Text is too long (max " + maxLength + " characters)");
                return false;
            }

            // SQL injection check
            if (checkSqlInjection) {
                inputSanitizer.validateNoSqlInjection(text);
            }

            // Sanitize the text
            String sanitizedText;
            if (alphanumericOnly) {
                sanitizedText = inputSanitizer.sanitizeAlphanumeric(text);
            } else {
                sanitizedText = inputSanitizer.sanitizeText(text);
            }

            // Length checks after sanitization
            if (sanitizedText.length() < minLength) {
                addConstraintViolation(context, "Text is too short (min " + minLength + " characters)");
                return false;
            }

            // Check if sanitization removed too much content
            if (sanitizedText.length() < text.length() * 0.5) {
                addConstraintViolation(context, "Text contains too much invalid content");
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
