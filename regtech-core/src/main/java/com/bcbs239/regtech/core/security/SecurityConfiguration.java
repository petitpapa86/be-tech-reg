package com.bcbs239.regtech.core.security;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * Security configuration for the RegTech application.
 * Configures permission-based authorization filters and security components.
 */
@Configuration
public class SecurityConfiguration {

    /**
     * Register the permission authorization filter.
     */
    @Bean
    public FilterRegistrationBean<PermissionAuthorizationFilter> permissionAuthorizationFilterRegistration(
            PermissionAuthorizationFilter filter) {
        
        FilterRegistrationBean<PermissionAuthorizationFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(filter);
        registration.addUrlPatterns("/api/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 2);
        registration.setName("permissionAuthorizationFilter");
        
        return registration;
    }
}