package com.bcbs239.regtech.billing.infrastructure.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom validation annotation for sanitized text input with security checks.
 */
@Documented
@Constraint(validatedBy = SanitizedTextValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidSanitizedText {
    
    String message() default "Invalid text input";
    
    Class<?>[] groups() default {};
    
    Class<? extends Payload>[] payload() default {};
    
    /**
     * Whether to allow empty/null values
     */
    boolean allowEmpty() default false;
    
    /**
     * Maximum length for the text
     */
    int maxLength() default 1000;
    
    /**
     * Minimum length for the text
     */
    int minLength() default 0;
    
    /**
     * Whether to allow only alphanumeric characters (plus spaces, hyphens, underscores, dots)
     */
    boolean alphanumericOnly() default false;
    
    /**
     * Whether to perform SQL injection checks
     */
    boolean checkSqlInjection() default true;
}
