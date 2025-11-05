package com.bcbs239.regtech.billing.infrastructure.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Custom validation annotation for email addresses with enhanced security checks.
 */
@Documented
@Constraint(validatedBy = EmailValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidEmail {
    
    String message() default "Invalid email address";
    
    Class<?>[] groups() default {};
    
    Class<? extends Payload>[] payload() default {};
    
    /**
     * Whether to allow empty/null values
     */
    boolean allowEmpty() default false;
    
    /**
     * Maximum length for the email address
     */
    int maxLength() default 254;
}
