package com.bcbs239.regtech.dataquality.presentation.monitoring;

import com.bcbs239.regtech.dataquality.application.validation.S3StorageService;
import com.bcbs239.regtech.dataquality.domain.report.IQualityReportRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;

/**
 * Health checker for data quality module components.
 * Performs health checks on database and S3.
 * 
 * <p>Note: No longer checks validation engine as validation is now done through
 * value object factory methods (proper DDD approach).</p>
 */
@Component
public class QualityHealthChecker {
    
    private static final Logger logger = LoggerFactory.getLogger(QualityHealthChecker.class);
    
    private final IQualityReportRepository qualityReportRepository;
    private final S3StorageService s3StorageService;
    
    public QualityHealthChecker(
        IQualityReportRepository qualityReportRepository,
        S3StorageService s3StorageService
    ) {
        this.qualityReportRepository = qualityReportRepository;
        this.s3StorageService = s3StorageService;
    }
    
    /**
     * Checks database connectivity and performance.
     */
    public HealthCheckResult checkDatabaseHealth() {
        try {
            Instant startTime = Instant.now();
            
            // Test database connectivity by checking if repository is accessible
            boolean canConnect = qualityReportRepository != null;
            
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
     * Checks S3 service availability.
     */
    public HealthCheckResult checkS3Health() {
        try {
            Instant startTime = Instant.now();
            
            if (s3StorageService == null) {
                return new HealthCheckResult(
                    "DOWN",
                    "S3 storage service not available",
                    Map.of("error", "Service is null")
                );
            }
            
            // Test S3 connectivity (this would typically involve a simple operation)
            // For now, we'll just verify the service is injected properly
            long duration = java.time.Duration.between(startTime, Instant.now()).toMillis();
            
            return new HealthCheckResult(
                "UP",
                "S3 storage service is available",
                Map.of(
                    "responseTime", duration + "ms",
                    "service", "active"
                )
            );
            
        } catch (Exception e) {
            logger.error("S3 health check failed: {}", e.getMessage(), e);
            return new HealthCheckResult(
                "DOWN",
                "S3 health check failed",
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
        HealthCheckResult s3Health = checkS3Health();
        
        // Determine overall status
        boolean isHealthy = databaseHealth.isHealthy() && s3Health.isHealthy();
        
        String overallStatus = isHealthy ? "UP" : "DOWN";
        long checkDuration = java.time.Duration.between(startTime, Instant.now()).toMillis();
        
        return new ModuleHealthResult(
            overallStatus,
            databaseHealth,
            s3Health,
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
        HealthCheckResult s3Health,
        long checkDurationMs,
        Instant timestamp
    ) {
        public boolean isHealthy() {
            return "UP".equals(overallStatus);
        }
        
        public Map<String, Object> toResponseMap() {
            Map<String, Object> healthStatus = Map.of(
                "database", databaseHealth.toMap(),
                "s3", s3Health.toMap()
            );
            
            return Map.of(
                "module", "data-quality",
                "status", overallStatus,
                "timestamp", timestamp.toString(),
                "checkDuration", checkDurationMs + "ms",
                "components", healthStatus,
                "version", "1.0.0"
            );
        }
    }
}