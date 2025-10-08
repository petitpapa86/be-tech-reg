package com.bcbs239.regtech.core.security;

import org.springframework.stereotype.Component;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Implementation of the security configuration registry.
 * Allows modules to register their security configurations dynamically.
 */
@Component
public class SecurityConfigurationRegistryImpl implements SecurityConfigurationRegistry {

    private final Map<String, ModuleSecurityConfiguration> configurations = new ConcurrentHashMap<>();

    @Override
    public void registerModuleSecurityConfiguration(String moduleName, ModuleSecurityConfiguration config) {
        configurations.put(moduleName, config);
    }

    @Override
    public Map<String, ModuleSecurityConfiguration> getAllConfigurations() {
        return Map.copyOf(configurations);
    }
}