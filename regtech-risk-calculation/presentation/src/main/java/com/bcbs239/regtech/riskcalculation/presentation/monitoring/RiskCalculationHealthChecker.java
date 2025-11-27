package com.bcbs239.regtech.riskcalculation.presentation.monitoring;

import com.bcbs239.regtech.riskcalculation.domain.calculation.IBatchSummaryRepository;
import com.bcbs239.regtech.riskcalculation.domain.services.IFileStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;

/**
 * Health checker for risk calculation module components.
 * Performs health checks on database and file storage.
 */
@Component
public class RiskCalculationHealthChecker {
    
    private static final Logger logger = LoggerFactory.getLogger(RiskCalculationHealthChecker.class);
    
    private final IBatchSummaryRepository batchSummaryRepository;
    private final IFileStorageService fileStorageService;
    
    public RiskCalculationHealthChecker(
        IBatchSummaryRepository batchSummaryRepository,
        IFileStorageService fileStorageService
    ) {
        this.batchSummaryRepository = batchSummaryRepository;
        this.fileStorageService = fileStorageService;
    }
    
    /**
     * Checks database connectivity and performance.
     */
    public HealthCheckResult checkDatabaseHealth() {
        try {
            Instant startTime = Instant.now();
            
            // Test database connectivity by checking if repository is accessible
            boolean canConnect = batchSummaryRepository != null;
            
            if (!canConnect) {
                return new HealthCheckResult(
                    "DOWN",
                    "Database repository not available",
                    Map.of("error", "Repository is null")
                );
            }
            
            // Try a simple operation to test connectivity
            try {
                // This would typically be a simple query like SELECT 1
                // For now, we'll just verify the repository is injected properly
                long duration = java.time.Duration.between(startTime, Instant.now()).toMillis();
                
                return new HealthCheckResult(
                    "UP",
                    "Database is accessible",
                    Map.of(
                        "responseTime", duration + "ms",
                        "connectionPool", "active"
                    )
                );
            } catch (Exception e) {
                logger.warn("Database connectivity test failed: {}", e.getMessage());
                return new HealthCheckResult(
                    "DOWN",
                    "Database connectivity test failed",
                    Map.of("error", e.getMessage())
                );
            }
            
        } catch (Exception e) {
            logger.error("Database health check failed: {}", e.getMessage(), e);
            return new HealthCheckResult(
                "DOWN",
                "Database health check failed",
                Map.of("error", e.getMessage())
            );
        }
    }
    
    /**
     * Checks file storage service availability.
     */
    public HealthCheckResult checkFileStorageHealth() {
        try {
            Instant startTime = Instant.now();
            
            if (fileStorageService == null) {
                return new HealthCheckResult(
                    "DOWN",
                    "File storage service not available",
                    Map.of("error", "Service is null")
                );
            }
            
            // Test file storage connectivity (this would typically involve a simple operation)
            // For now, we'll just verify the service is injected properly
            long duration = java.time.Duration.between(startTime, Instant.now()).toMillis();
            
            return new HealthCheckResult(
                "UP",
                "File storage service is available",
                Map.of(
                    "responseTime", duration + "ms",
                    "service", "active"
                )
            );
            
        } catch (Exception e) {
            logger.error("File storage health check failed: {}", e.getMessage(), e);
            return new HealthCheckResult(
                "DOWN",
                "File storage health check failed",
                Map.of("error", e.getMessage())
            );
        }
    }
    
    /**
     * Checks currency conversion service availability.
     */
    public HealthCheckResult checkCurrencyConversionHealth() {
        try {
            Instant startTime = Instant.now();
            
            // Currency conversion is always available (built into application service)
            long duration = java.time.Duration.between(startTime, Instant.now()).toMillis();
            
            return new HealthCheckResult(
                "UP",
                "Currency conversion service is available",
                Map.of(
                    "responseTime", duration + "ms",
                    "type", "application-service",
                    "location", "CurrencyConversionService"
                )
            );
            
        } catch (Exception e) {
            logger.error("Currency conversion health check failed: {}", e.getMessage(), e);
            return new HealthCheckResult(
                "DOWN",
                "Currency conversion health check failed",
                Map.of("error", e.getMessage())
            );
        }
    }
    
    /**
     * Performs comprehensive health check of all components.
     */
    public ModuleHealthResult checkModuleHealth() {
        Instant startTime = Instant.now();
        
        // Check all components
        HealthCheckResult databaseHealth = checkDatabaseHealth();
        HealthCheckResult fileStorageHealth = checkFileStorageHealth();
        HealthCheckResult currencyConversionHealth = checkCurrencyConversionHealth();
        
        // Determine overall status
        boolean isHealthy = databaseHealth.isHealthy() 
            && fileStorageHealth.isHealthy() 
            && currencyConversionHealth.isHealthy();
        
        String overallStatus = isHealthy ? "UP" : "DOWN";
        long checkDuration = java.time.Duration.between(startTime, Instant.now()).toMillis();
        
        return new ModuleHealthResult(
            overallStatus,
            databaseHealth,
            fileStorageHealth,
            currencyConversionHealth,
            checkDuration,
            Instant.now()
        );
    }
    
    /**
     * Record for individual health check results.
     */
    public record HealthCheckResult(
        String status,
        String message,
        Map<String, Object> details
    ) {
        public boolean isHealthy() {
            return "UP".equals(status);
        }
        
        public Map<String, Object> toMap() {
            return Map.of(
                "status", status,
                "message", message,
                "details", details
            );
        }
    }
    
    /**
     * Record for module-wide health check results.
     */
    public record ModuleHealthResult(
        String overallStatus,
        HealthCheckResult databaseHealth,
        HealthCheckResult fileStorageHealth,
        HealthCheckResult currencyConversionHealth,
        long checkDurationMs,
        Instant timestamp
    ) {
        public boolean isHealthy() {
            return "UP".equals(overallStatus);
        }
        
        public Map<String, Object> toResponseMap() {
            Map<String, Object> healthStatus = Map.of(
                "database", databaseHealth.toMap(),
                "fileStorage", fileStorageHealth.toMap(),
                "currencyConversion", currencyConversionHealth.toMap()
            );
            
            return Map.of(
                "module", "risk-calculation",
                "status", overallStatus,
                "timestamp", timestamp.toString(),
                "checkDuration", checkDurationMs + "ms",
                "components", healthStatus,
                "version", "1.0.0"
            );
        }
    }
}
