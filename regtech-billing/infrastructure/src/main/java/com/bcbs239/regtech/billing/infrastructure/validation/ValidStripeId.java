package com.bcbs239.regtech.billing.infrastructure.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Custom validation annotation for Stripe IDs.
 * Validates that the ID follows Stripe's format conventions.
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = StripeIdValidator.class)
@Documented
public @interface ValidStripeId {
    
    String message() default "Invalid Stripe ID format";
    
    /**
     * The type of Stripe ID to validate
     */
    StripeIdType type();
    
    Class<?>[] groups() default {};
    
    Class<? extends Payload>[] payload() default {};
    
    /**
     * Enum defining the types of Stripe IDs that can be validated
     */
    enum StripeIdType {
        CUSTOMER,
        SUBSCRIPTION,
        INVOICE,
        PAYMENT_METHOD,
        EVENT
    }
}

