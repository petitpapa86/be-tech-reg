package com.bcbs239.regtech.billing.infrastructure.validation;

import com.bcbs239.regtech.core.shared.Result;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.math.BigDecimal;

/**
 * Validator implementation for ValidPaymentAmount annotation.
 * Uses BillingValidationUtils for consistent validation logic.
 */
public class PaymentAmountValidator implements ConstraintValidator<ValidPaymentAmount, BigDecimal> {

    @Override
    public void initialize(ValidPaymentAmount constraintAnnotation) {
        // No initialization needed
    }

    @Override
    public boolean isValid(BigDecimal value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // Let @NotNull handle null validation
        }
        
        Result<Void> validationResult = BillingValidationUtils.validatePaymentAmount(value);
        
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
