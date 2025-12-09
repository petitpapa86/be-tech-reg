package com.bcbs239.regtech.riskcalculation.domain.calculation;

import com.bcbs239.regtech.core.domain.shared.Entity;
import com.bcbs239.regtech.riskcalculation.domain.calculation.events.BatchCalculationCompletedEvent;
import com.bcbs239.regtech.riskcalculation.domain.calculation.events.BatchCalculationFailedEvent;
import com.bcbs239.regtech.riskcalculation.domain.calculation.events.BatchCalculationStartedEvent;
import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.BankInfo;
import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.BatchId;
import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.FileStorageUri;
import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.ProcessingTimestamps;

import java.time.Instant;

/**
 * Batch Aggregate Root
 * Represents a batch of exposures being processed for risk calculation.
 * 
 * This aggregate encapsulates all business logic related to batch processing
 * and raises domain events for significant state changes.
 * 
 * Following DDD principles:
 * - Encapsulates business logic
 * - Raises domain events
 * - Enforces invariants
 * - Provides factory methods for creation
 * - Provides reconstitution method for persistence
 */
public class Batch extends Entity {
    
    private BatchId id;
    private BankInfo bankInfo;
    private BatchStatus status;
    private int totalExposures;
    private FileStorageUri sourceDataUri;
    private FileStorageUri calculationResultsUri;
    private ProcessingTimestamps timestamps;
    private String failureReason;
    
    // Private constructor - use factory methods
    private Batch() {}
    
    /**
     * Factory method to create a new batch.
     * Raises BatchCalculationStartedEvent.
     * 
     * @param batchId unique identifier for the batch
     * @param bankInfo bank information
     * @param totalExposures total number of exposures to process
     * @param sourceDataUri URI to the source data file
     * @return new Batch aggregate in PROCESSING state
     */
    public static Batch create(
            String batchId,
            BankInfo bankInfo,
            int totalExposures,
            String sourceDataUri) {
        
        if (totalExposures < 0) {
            throw new IllegalArgumentException("Total exposures cannot be negative");
        }
        
        Batch batch = new Batch();
        batch.id = BatchId.of(batchId);
        batch.bankInfo = bankInfo;
        batch.totalExposures = totalExposures;
        batch.sourceDataUri = FileStorageUri.of(sourceDataUri);
        batch.status = BatchStatus.PROCESSING;
        batch.timestamps = ProcessingTimestamps.started(Instant.now());
        
        // Raise domain event
        batch.addDomainEvent(new BatchCalculationStartedEvent(
            batchId,
            bankInfo.abiCode(),
            totalExposures,
            Instant.now()
        ));
        
        return batch;
    }
    
    /**
     * Mark calculation as completed.
     * Raises BatchCalculationCompletedEvent.
     * 
     * @param resultsUri URI to the calculation results file
     * @param processedExposures number of exposures actually processed
     * @throws IllegalStateException if batch is not in PROCESSING state
     */
    public void completeCalculation(String resultsUri, int processedExposures) {
        if (this.status != BatchStatus.PROCESSING) {
            throw new IllegalStateException(
                "Cannot complete batch that is not in PROCESSING state. Current state: " + this.status
            );
        }
        
        if (processedExposures < 0) {
            throw new IllegalArgumentException("Processed exposures cannot be negative");
        }
        
        this.status = BatchStatus.COMPLETED;
        this.calculationResultsUri = FileStorageUri.of(resultsUri);
        this.timestamps = this.timestamps.withCompleted(Instant.now());
        
        // Raise domain event
        this.addDomainEvent(new BatchCalculationCompletedEvent(
            this.id.value(),
            this.bankInfo.abiCode(),
            processedExposures,
            resultsUri,
            this.timestamps.getCompletedAt().orElseThrow()
        ));
    }
    
    /**
     * Mark calculation as failed.
     * Raises BatchCalculationFailedEvent.
     * 
     * @param reason description of why the calculation failed
     * @throws IllegalStateException if batch is not in PROCESSING state
     */
    public void failCalculation(String reason) {
        if (this.status != BatchStatus.PROCESSING) {
            throw new IllegalStateException(
                "Cannot fail batch that is not in PROCESSING state. Current state: " + this.status
            );
        }
        
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("Failure reason cannot be null or empty");
        }
        
        this.status = BatchStatus.FAILED;
        this.failureReason = reason;
        this.timestamps = this.timestamps.withFailed(Instant.now());
        
        // Raise domain event
        this.addDomainEvent(new BatchCalculationFailedEvent(
            this.id.value(),
            this.bankInfo.abiCode(),
            reason,
            this.timestamps.getFailedAt().orElseThrow()
        ));
    }
    
    /**
     * Reconstitute a batch from persistence.
     * Used by infrastructure layer when loading from database.
     * Does not raise domain events.
     * 
     * @param id batch identifier
     * @param bankInfo bank information
     * @param status current status
     * @param totalExposures total number of exposures
     * @param sourceDataUri URI to source data
     * @param calculationResultsUri URI to calculation results (may be null)
     * @param timestamps processing timestamps
     * @param failureReason failure reason (may be null)
     * @return reconstituted Batch aggregate
     */
    public static Batch reconstitute(
            BatchId id,
            BankInfo bankInfo,
            BatchStatus status,
            int totalExposures,
            FileStorageUri sourceDataUri,
            FileStorageUri calculationResultsUri,
            ProcessingTimestamps timestamps,
            String failureReason) {
        
        Batch batch = new Batch();
        batch.id = id;
        batch.bankInfo = bankInfo;
        batch.status = status;
        batch.totalExposures = totalExposures;
        batch.sourceDataUri = sourceDataUri;
        batch.calculationResultsUri = calculationResultsUri;
        batch.timestamps = timestamps;
        batch.failureReason = failureReason;
        
        return batch;
    }
    
    // Getters
    public BatchId getId() {
        return id;
    }
    
    public BankInfo getBankInfo() {
        return bankInfo;
    }
    
    public BatchStatus getStatus() {
        return status;
    }
    
    public int getTotalExposures() {
        return totalExposures;
    }
    
    public FileStorageUri getSourceDataUri() {
        return sourceDataUri;
    }
    
    public FileStorageUri getCalculationResultsUri() {
        return calculationResultsUri;
    }
    
    public ProcessingTimestamps getTimestamps() {
        return timestamps;
    }
    
    public String getFailureReason() {
        return failureReason;
    }
    
    /**
     * Populates a persistence entity with this aggregate's state.
     * Implements "Tell, don't ask" principle - the aggregate controls its own persistence representation.
     * 
     * This method is called by the infrastructure layer when persisting the aggregate.
     * The aggregate knows how to map its domain state to the persistence model.
     * 
     * @param entity the entity to populate (must not be null)
     * @throws IllegalArgumentException if entity is null
     */
    public void populateEntity(Object entity) {
        if (entity == null) {
            throw new IllegalArgumentException("Entity cannot be null");
        }
        
        // Use reflection to avoid domain layer depending on infrastructure
        // The infrastructure layer will pass the correct entity type
        try {
            Class<?> entityClass = entity.getClass();
            
            // Set batch ID
            entityClass.getMethod("setBatchId", String.class)
                .invoke(entity, this.id.value());
            
            // Set bank info
            entityClass.getMethod("setBankName", String.class)
                .invoke(entity, this.bankInfo.bankName());
            entityClass.getMethod("setAbiCode", String.class)
                .invoke(entity, this.bankInfo.abiCode());
            entityClass.getMethod("setLeiCode", String.class)
                .invoke(entity, this.bankInfo.leiCode());
            
            // Set status
            entityClass.getMethod("setStatus", String.class)
                .invoke(entity, this.status.name());
            
            // Set total exposures
            entityClass.getMethod("setTotalExposures", Integer.class)
                .invoke(entity, this.totalExposures);
            
            // Note: Source data URI is not stored in BatchEntity
            // It's only used during processing and not persisted
            
            // Set calculation results URI (if present)
            if (this.calculationResultsUri != null) {
                entityClass.getMethod("setCalculationResultsUri", String.class)
                    .invoke(entity, this.calculationResultsUri.uri());
            }
            
            // Set timestamps
            entityClass.getMethod("setIngestedAt", Instant.class)
                .invoke(entity, this.timestamps.startedAt());
            
            if (this.timestamps.getCompletedAt().isPresent()) {
                entityClass.getMethod("setProcessedAt", Instant.class)
                    .invoke(entity, this.timestamps.getCompletedAt().get());
            }
            
            // Set failure reason (if present)
            if (this.failureReason != null) {
                // Note: BatchEntity doesn't have a failureReason field yet
                // This is for future extensibility
            }
            
            // Set report date (using current date as default since Batch doesn't store it)
            entityClass.getMethod("setReportDate", java.time.LocalDate.class)
                .invoke(entity, java.time.LocalDate.now());
            
        } catch (Exception e) {
            throw new IllegalStateException("Failed to populate entity from Batch aggregate", e);
        }
    }
}
