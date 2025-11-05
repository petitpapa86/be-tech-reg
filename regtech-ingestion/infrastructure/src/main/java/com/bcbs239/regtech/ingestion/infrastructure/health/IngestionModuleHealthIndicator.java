package com.bcbs239.regtech.ingestion.infrastructure.health;

import com.bcbs239.regtech.core.health.ModuleHealthIndicator;
import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.ingestion.domain.batch.BatchStatus;
import com.bcbs239.regtech.ingestion.domain.services.FileStorageService;
import com.bcbs239.regtech.ingestion.infrastructure.batch.persistence.IngestionBatchEntity;
import com.bcbs239.regtech.ingestion.infrastructure.batch.persistence.IngestionBatchJpaRepository;
import com.bcbs239.regtech.ingestion.infrastructure.events.IngestionOutboxProcessor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Health indicator for the ingestion module.
 * Checks database connectivity, S3 service availability, and outbox processor status.
 */
@Component
public class IngestionModuleHealthIndicator implements ModuleHealthIndicator {

    private final DataSource dataSource;
    private final FileStorageService fileStorageService;
    private final IngestionOutboxProcessor outboxProcessor;
    private final IngestionBatchJpaRepository batchRepository;

    public IngestionModuleHealthIndicator(
            DataSource dataSource,
            FileStorageService fileStorageService,
            IngestionOutboxProcessor outboxProcessor,
            IngestionBatchJpaRepository batchRepository) {
        this.dataSource = dataSource;
        this.fileStorageService = fileStorageService;
        this.outboxProcessor = outboxProcessor;
        this.batchRepository = batchRepository;
    }

    @Override
    public String getModuleName() {
        return "ingestion";
    }

    @Override
    public Health health() {
        Map<String, Object> details = new HashMap<>();
        boolean allHealthy = true;
        
        // Check database connectivity
        Health.Builder databaseHealth = checkDatabaseHealth();
        details.put("database", databaseHealth.build());
        if (databaseHealth.build().getStatus() != Status.UP) {
            allHealthy = false;
        }
        
        // Check S3 service availability
        Health.Builder s3Health = checkS3ServiceHealth();
        details.put("s3", s3Health.build());
        if (s3Health.build().getStatus() != Status.UP) {
            allHealthy = false;
        }
        
        // Check outbox processor status
        Health.Builder outboxHealth = checkOutboxProcessorHealth();
        details.put("outboxProcessor", outboxHealth.build());
        if (outboxHealth.build().getStatus() != Status.UP) {
            allHealthy = false;
        }
        
        // Check ingestion-specific metrics
        Health.Builder ingestionHealth = checkIngestionHealth();
        details.put("ingestionMetrics", ingestionHealth.build());
        if (ingestionHealth.build().getStatus() != Status.UP) {
            allHealthy = false;
        }
        
        // Overall health status
        Status overallStatus = allHealthy ? Status.UP : Status.DOWN;
        details.put("timestamp", Instant.now().toString());
        details.put("module", "ingestion");
        
        return Health.status(overallStatus)
                .withDetails(details)
                .build();
    }
    
    private Health.Builder checkDatabaseHealth() {
        try {
            // Test database connection
            try (Connection connection = dataSource.getConnection()) {
                if (connection.isValid(5)) { // 5 second timeout
                    // Test ingestion-specific database operations
                    long batchCount = batchRepository.count();
                    
                    return Health.up()
                            .withDetail("status", "UP")
                            .withDetail("connectionValid", true)
                            .withDetail("totalBatches", batchCount)
                            .withDetail("checkTime", Instant.now().toString());
                } else {
                    return Health.down()
                            .withDetail("status", "DOWN")
                            .withDetail("connectionValid", false)
                            .withDetail("error", "Database connection is not valid")
                            .withDetail("checkTime", Instant.now().toString());
                }
            }
        } catch (SQLException e) {
            return Health.down()
                    .withDetail("status", "DOWN")
                    .withDetail("connectionValid", false)
                    .withDetail("error", "Failed to connect to database: " + e.getMessage())
                    .withDetail("checkTime", Instant.now().toString());
        } catch (Exception e) {
            return Health.down()
                    .withDetail("status", "DOWN")
                    .withDetail("error", "Unexpected error checking database health: " + e.getMessage())
                    .withDetail("checkTime", Instant.now().toString());
        }
    }
    
    private Health.Builder checkS3ServiceHealth() {
        try {
            Result<Boolean> s3HealthResult = fileStorageService.checkServiceHealth();
            
            if (s3HealthResult.isSuccess() && s3HealthResult.getValue().orElse(false)) {
                return Health.up()
                        .withDetail("status", "UP")
                        .withDetail("serviceAvailable", true)
                        .withDetail("checkTime", Instant.now().toString());
            } else {
                String errorMessage = s3HealthResult.getError()
                        .map(error -> error.getMessage())
                        .orElse("S3 service health check returned false");
                
                return Health.down()
                        .withDetail("status", "DOWN")
                        .withDetail("serviceAvailable", false)
                        .withDetail("error", errorMessage)
                        .withDetail("checkTime", Instant.now().toString());
            }
        } catch (Exception e) {
            return Health.down()
                    .withDetail("status", "DOWN")
                    .withDetail("serviceAvailable", false)
                    .withDetail("error", "Exception checking S3 health: " + e.getMessage())
                    .withDetail("checkTime", Instant.now().toString());
        }
    }
    
    private Health.Builder checkOutboxProcessorHealth() {
        try {
            // Check if outbox processor is enabled and functioning
            boolean isEnabled = outboxProcessor.isProcessingEnabled();
            
            if (isEnabled) {
                // Get processing statistics from the event publisher
                // Note: We access the event publisher through the processor's protected method
                // For now, we'll just check if it's enabled and running
                
                return Health.up()
                        .withDetail("status", "UP")
                        .withDetail("enabled", true)
                        .withDetail("contextName", "ingestion")
                        .withDetail("checkTime", Instant.now().toString());
            } else {
                return Health.down()
                        .withDetail("status", "DOWN")
                        .withDetail("enabled", false)
                        .withDetail("error", "Outbox processor is disabled")
                        .withDetail("checkTime", Instant.now().toString());
            }
        } catch (Exception e) {
            return Health.down()
                    .withDetail("status", "DOWN")
                    .withDetail("error", "Exception checking outbox processor health: " + e.getMessage())
                    .withDetail("checkTime", Instant.now().toString());
        }
    }
    
    private Health.Builder checkIngestionHealth() {
        try {
            // Check ingestion-specific health metrics
            Map<String, Object> ingestionMetrics = new HashMap<>();
            
            // Check recent batch processing activity
            Instant oneHourAgo = Instant.now().minusSeconds(3600);
            List<IngestionBatchEntity> recentBatchesList = batchRepository.findByUploadedAtBetween(
                oneHourAgo, Instant.now()
            );
            long recentBatches = recentBatchesList.size();
            ingestionMetrics.put("recentBatchesLastHour", recentBatches);
            
            // Check for any failed batches in the last hour
            long failedBatches = recentBatchesList.stream()
                .filter(batch -> batch.getStatus() == BatchStatus.FAILED)
                .count();
            ingestionMetrics.put("failedBatchesLastHour", failedBatches);
            
            // Calculate success rate if there are recent batches
            if (recentBatches > 0) {
                double successRate = (double) (recentBatches - failedBatches) / recentBatches;
                ingestionMetrics.put("successRateLastHour", Math.round(successRate * 100.0) / 100.0);
                
                // Consider unhealthy if success rate is below 90%
                if (successRate < 0.9) {
                    return Health.down()
                            .withDetail("status", "DOWN")
                            .withDetail("error", "Low success rate: " + (successRate * 100) + "%")
                            .withDetail("metrics", ingestionMetrics)
                            .withDetail("checkTime", Instant.now().toString());
                }
            } else {
                ingestionMetrics.put("successRateLastHour", "N/A - No recent batches");
            }
            
            return Health.up()
                    .withDetail("status", "UP")
                    .withDetail("metrics", ingestionMetrics)
                    .withDetail("checkTime", Instant.now().toString());
                    
        } catch (Exception e) {
            return Health.down()
                    .withDetail("status", "DOWN")
                    .withDetail("error", "Exception checking ingestion metrics: " + e.getMessage())
                    .withDetail("checkTime", Instant.now().toString());
        }
    }
}

