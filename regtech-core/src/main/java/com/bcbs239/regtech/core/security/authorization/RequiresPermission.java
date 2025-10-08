package com.bcbs239.regtech.core.security.authorization;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to specify required permissions for a method or class.
 * Can be used as an alternative to Spring Security's @PreAuthorize.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresPermission {
    
    /**
     * Required permissions (user must have ALL of these)
     */
    String[] value() default {};
    
    /**
     * Alternative permissions (user must have ANY of these)
     */
    String[] anyOf() default {};
    
    /**
     * Resource type for resource-based permissions
     */
    String resourceType() default "";
    
    /**
     * Action for resource-based permissions
     */
    String action() default "";
}