package com.bcbs239.regtech.billing.infrastructure.validation;

import com.bcbs239.regtech.billing.domain.shared.BillingValidationUtils;
import com.bcbs239.regtech.core.domain.shared.Result;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Currency;

/**
 * Validator implementation for ValidCurrency annotation.
 * Uses BillingValidationUtils for consistent validation logic.
 */
public class CurrencyValidator implements ConstraintValidator<ValidCurrency, String> {

    @Override
    public void initialize(ValidCurrency constraintAnnotation) {
        // No initialization needed
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.trim().isEmpty()) {
            return true; // Let @NotBlank handle empty validation
        }
        
        Result<Currency> validationResult = BillingValidationUtils.validateCurrency(value);
        
        if (validationResult.isFailure()) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                validationResult.getError().get().getMessage()
            ).addConstraintViolation();
            return false;
        }
        
        return true;
    }
}

