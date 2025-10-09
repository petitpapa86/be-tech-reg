package com.bcbs239.regtech.billing.infrastructure.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

/**
 * Custom validation annotation for payment amounts.
 * Validates that the amount is within acceptable range and precision.
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PaymentAmountValidator.class)
@Documented
public @interface ValidPaymentAmount {
    
    String message() default "Invalid payment amount";
    
    Class<?>[] groups() default {};
    
    Class<? extends Payload>[] payload() default {};
}
