package com.bcbs239.regtech.core.infrastructure.securityauthorization;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;

import java.util.Map;

/**
 * Registry pattern for modular security configurations.
 * Each bounded context can register its security rules.
 */
public interface SecurityConfigurationRegistry {
    
    /**
     * Register security configuration for a specific module
     */
    void registerModuleSecurityConfiguration(String moduleName, ModuleSecurityConfiguration config);
    
    /**
     * Get all registered security configurations
     */
    Map<String, ModuleSecurityConfiguration> getAllConfigurations();
    
    /**
     * Module-specific security configuration
     */
    interface ModuleSecurityConfiguration {
        String[] getPathPatterns();
        void configure(HttpSecurity http) throws Exception;
        int getOrder(); // For filter chain ordering
    }
}
