package com.bcbs239.regtech.billing.infrastructure.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom validation annotation for phone numbers with enhanced security checks.
 */
@Documented
@Constraint(validatedBy = PhoneNumberValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPhoneNumber {
    
    String message() default "Invalid phone number";
    
    Class<?>[] groups() default {};
    
    Class<? extends Payload>[] payload() default {};
    
    /**
     * Whether to allow empty/null values
     */
    boolean allowEmpty() default false;
    
    /**
     * Maximum length for the phone number
     */
    int maxLength() default 20;
    
    /**
     * Minimum length for the phone number
     */
    int minLength() default 7;
}
