package com.bcbs239.regtech.billing.infrastructure.validation;

import com.bcbs239.regtech.billing.domain.shared.BillingValidationUtils;
import com.bcbs239.regtech.core.domain.shared.Result;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validator implementation for ValidStripeId annotation.
 * Uses BillingValidationUtils for consistent validation logic.
 */
public class StripeIdValidator implements ConstraintValidator<ValidStripeId, String> {

    private ValidStripeId.StripeIdType idType;

    @Override
    public void initialize(ValidStripeId constraintAnnotation) {
        this.idType = constraintAnnotation.type();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.trim().isEmpty()) {
            return true; // Let @NotBlank handle empty validation
        }
        
        Result<Void> validationResult = switch (idType) {
            case CUSTOMER -> BillingValidationUtils.validateStripeCustomerId(value);
            case SUBSCRIPTION -> BillingValidationUtils.validateStripeSubscriptionId(value);
            case INVOICE -> BillingValidationUtils.validateStripeInvoiceId(value);
            case PAYMENT_METHOD -> BillingValidationUtils.validateStripePaymentMethodId(value);
            case EVENT -> BillingValidationUtils.validateStripeEventId(value);
        };
        
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

