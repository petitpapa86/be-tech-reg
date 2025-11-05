package com.bcbs239.regtech.core.infrastructure.securityauthorization;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to specify required permissions for controller methods.
 * Can be used as an alternative to RouterFunction attributes.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequirePermissions {
    
    /**
     * Array of required permissions.
     * User must have ALL specified permissions to access the endpoint.
     */
    String[] value();
    
    /**
     * Whether to require ALL permissions (AND) or ANY permission (OR).
     * Default is ALL (AND logic).
     */
    boolean requireAll() default true;
}
