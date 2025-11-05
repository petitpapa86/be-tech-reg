package com.bcbs239.regtech.billing.infrastructure.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Custom validation annotation for currency codes.
 * Validates that the currency is supported for billing operations.
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = CurrencyValidator.class)
@Documented
public @interface ValidCurrency {
    
    String message() default "Invalid or unsupported currency";
    
    Class<?>[] groups() default {};
    
    Class<? extends Payload>[] payload() default {};
}

