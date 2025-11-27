package com.bcbs239.regtech.dataquality.infrastructure.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Validates Rules Engine configuration on application startup.
 * 
 * <p>This validator ensures that all required configuration properties are properly set
 * and that the Rules Engine is correctly configured before the application starts processing
 * validation requests.</p>
 * 
 * <p><strong>Validation Checks:</strong></p>
 * <ul>
 *   <li>Rules Engine must be explicitly enabled (no default to false)</li>
 *   <li>Cache TTL must be positive if caching is enabled</li>
 *   <li>Performance thresholds must be reasonable</li>
 *   <li>Logging configuration must be valid</li>
 * </ul>
 * 
 * <p><strong>Requirements:</strong> 7.1, 7.2, 7.3, 7.4, 7.5</p>
 * 
 * @see DataQualityProperties
 * @see RulesEngineConfiguration
 */
@Slf4j
@Component
public class RulesEngineConfigurationValidator {
    
    private final DataQualityProperties properties;
    
    public RulesEngineConfigurationValidator(DataQualityProperties properties) {
        this.properties = properties;
    }
    
    /**
     * Validates Rules Engine configuration after application context is fully initialized.
     * 
     * <p>This method runs after all beans are created and the application is ready to serve requests.
     * It performs comprehensive validation of the Rules Engine configuration and logs warnings
     * or throws exceptions for invalid configurations.</p>
     * 
     * @throws IllegalStateException if configuration is invalid
     */
    @EventListener(ApplicationReadyEvent.class)
    public void validateConfiguration() {
        log.info("=".repeat(80));
        log.info("Validating Rules Engine Configuration");
        log.info("=".repeat(80));
        
        validateRulesEngineEnabled();
        validateCacheConfiguration();
        validatePerformanceThresholds();
        validateLoggingConfiguration();
        validateMigrationConfiguration();
        
        log.info("✓ Rules Engine configuration validation completed successfully");
        log.info("=".repeat(80));
    }
    
    /**
     * Validates that Rules Engine is explicitly enabled.
     * 
     * <p>Requirement 7.1: Rules Engine must be enabled for validation to work.
     * The system should fail fast if Rules Engine is disabled.</p>
     */
    private void validateRulesEngineEnabled() {
        DataQualityProperties.RulesEngineProperties rulesEngine = properties.getRulesEngine();
        
        if (rulesEngine.getEnabled() == null) {
            String errorMsg = "Rules Engine enabled flag is NOT SET. " +
                "Set 'data-quality.rules-engine.enabled=true' in configuration.";
            log.error("✗ {}", errorMsg);
            throw new IllegalStateException(errorMsg);
        }
        
        if (!rulesEngine.getEnabled()) {
            String errorMsg = "Rules Engine is DISABLED. Validation will not work. " +
                "Set 'data-quality.rules-engine.enabled=true' in configuration.";
            log.error("✗ {}", errorMsg);
            throw new IllegalStateException(errorMsg);
        }
        
        log.info("✓ Rules Engine is ENABLED");
    }
    
    /**
     * Validates cache configuration.
     * 
     * <p>Requirement 7.2: Cache configuration must be valid if caching is enabled.</p>
     */
    private void validateCacheConfiguration() {
        DataQualityProperties.RulesEngineProperties rulesEngine = properties.getRulesEngine();
        
        if (rulesEngine.isCacheEnabled()) {
            if (rulesEngine.getCacheTtl() <= 0) {
                String errorMsg = "Cache TTL must be positive when caching is enabled. " +
                    "Current value: " + rulesEngine.getCacheTtl();
                log.error("✗ {}", errorMsg);
                throw new IllegalStateException(errorMsg);
            }
            
            log.info("✓ Cache configuration valid:");
            log.info("  - Cache enabled: true");
            log.info("  - Cache TTL: {}s", rulesEngine.getCacheTtl());
            
            if (rulesEngine.getCacheTtl() < 60) {
                log.warn("⚠ Cache TTL is very low ({}s). Consider increasing to at least 60s for better performance.", 
                    rulesEngine.getCacheTtl());
            }
        } else {
            log.info("✓ Cache is DISABLED (rules will be loaded from database on every validation)");
            log.warn("⚠ Disabling cache may impact performance. Consider enabling cache for production.");
        }
    }
    
    /**
     * Validates performance threshold configuration.
     * 
     * <p>Requirement 7.3: Performance thresholds must be reasonable.</p>
     */
    private void validatePerformanceThresholds() {
        DataQualityProperties.RulesEngineProperties.PerformanceProperties performance = 
            properties.getRulesEngine().getPerformance();
        
        if (performance.getWarnThresholdMs() < 0) {
            String errorMsg = "Warn threshold must be non-negative. Current value: " + 
                performance.getWarnThresholdMs();
            log.error("✗ {}", errorMsg);
            throw new IllegalStateException(errorMsg);
        }
        
        if (performance.getMaxExecutionTimeMs() < 0) {
            String errorMsg = "Max execution time must be non-negative. Current value: " + 
                performance.getMaxExecutionTimeMs();
            log.error("✗ {}", errorMsg);
            throw new IllegalStateException(errorMsg);
        }
        
        if (performance.getMaxExecutionTimeMs() > 0 && 
            performance.getWarnThresholdMs() > performance.getMaxExecutionTimeMs()) {
            log.warn("⚠ Warn threshold ({}ms) is greater than max execution time ({}ms). " +
                "This may cause confusing log messages.",
                performance.getWarnThresholdMs(), performance.getMaxExecutionTimeMs());
        }
        
        log.info("✓ Performance thresholds valid:");
        log.info("  - Warn threshold: {}ms", performance.getWarnThresholdMs());
        log.info("  - Max execution time: {}ms", performance.getMaxExecutionTimeMs());
    }
    
    /**
     * Validates logging configuration.
     * 
     * <p>Requirement 7.4: Logging configuration must be valid.</p>
     */
    private void validateLoggingConfiguration() {
        DataQualityProperties.RulesEngineProperties.LoggingProperties logging = 
            properties.getRulesEngine().getLogging();
        
        log.info("✓ Logging configuration:");
        log.info("  - Log executions: {}", logging.isLogExecutions());
        log.info("  - Log violations: {}", logging.isLogViolations());
        log.info("  - Log summary: {}", logging.isLogSummary());
        
        if (!logging.isLogExecutions() && !logging.isLogViolations() && !logging.isLogSummary()) {
            log.warn("⚠ All logging is disabled. This may make troubleshooting difficult.");
        }
    }
    
    /**
     * Validates migration configuration.
     * 
     * <p>Requirement 7.5: Migration configuration should be checked.</p>
     */
    private void validateMigrationConfiguration() {
        DataQualityProperties.RulesMigrationProperties migration = properties.getRulesMigration();
        
        if (migration.isEnabled()) {
            log.warn("⚠ Rules migration is ENABLED. This should only be enabled during initial setup.");
            log.warn("⚠ After migration completes, set 'data-quality.rules-migration.enabled=false'");
        } else {
            log.info("✓ Rules migration is DISABLED (normal operation mode)");
        }
    }
}
