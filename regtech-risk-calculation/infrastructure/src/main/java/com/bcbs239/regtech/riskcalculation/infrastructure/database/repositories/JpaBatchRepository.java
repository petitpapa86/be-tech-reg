package com.bcbs239.regtech.riskcalculation.infrastructure.database.repositories;

import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Maybe;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.riskcalculation.domain.calculation.Batch;
import com.bcbs239.regtech.riskcalculation.domain.calculation.BatchStatus;
import com.bcbs239.regtech.riskcalculation.domain.calculation.BatchRepository;
import com.bcbs239.regtech.core.domain.shared.valueobjects.BankInfo;
import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.BatchId;
import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.FileStorageUri;
import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.ProcessingTimestamps;
import com.bcbs239.regtech.riskcalculation.infrastructure.database.entities.BatchEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * JPA implementation of BatchRepository.
 * Implements the DDD repository pattern by working with Batch aggregates
 * rather than exposing low-level persistence operations.
 * 
 * The aggregate controls its own persistence representation through the
 * populateEntity() method, following the "Tell, don't ask" principle.
 * 
 * Requirements: 7.1, 7.2
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class JpaBatchRepository implements BatchRepository {
    
    private final SpringDataBatchRepository springDataRepository;
    
    @Override
    @Transactional
    public Result<Void> save(Batch batch) {
        try {
            // Upsert: update existing row if present, otherwise insert.
            // This is required because the same Batch aggregate is saved multiple times
            // (created -> processing, then completed/failed).
            BatchEntity entity = springDataRepository.findById(batch.getId().value())
                .orElseGet(BatchEntity::new);

            // Let the aggregate populate the entity (Tell, don't ask)
            batch.populateEntity(entity);
            
            // Persist the entity
            springDataRepository.save(entity);
            
            log.debug("Successfully saved batch aggregate: {}", batch.getId().value());
            return Result.success();
            
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            return handleDuplicateKeyOrConstraintViolation(e, batch);
            
        } catch (Exception e) {
            log.error("Failed to save batch aggregate: {}", batch.getId().value(), e);
            return Result.failure(ErrorDetail.of(
                "BATCH_SAVE_FAILED",
                ErrorType.SYSTEM_ERROR,
                "Failed to save batch: " + e.getMessage(),
                "batch.save.failed"
            ));
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public Maybe<Batch> findById(BatchId batchId) {
        try {
            log.debug("Finding batch by ID: {}", batchId.value());
            
            return springDataRepository.findById(batchId.value())
                .map(this::reconstituteBatch)
                .map(Maybe::some)
                .orElse(Maybe.none());
                
        } catch (Exception e) {
            log.error("Failed to find batch by ID: {}", batchId.value(), e);
            return Maybe.none();
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Batch> findByStatus(BatchStatus status) {
        try {
            log.debug("Finding batches by status: {}", status);
            
            return springDataRepository.findAll().stream()
                .filter(entity -> status.name().equals(entity.getStatus()))
                .map(this::reconstituteBatch)
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            log.error("Failed to find batches by status: {}", status, e);
            return List.of();
        }
    }
    
    /**
     * Reconstitutes a Batch aggregate from a BatchEntity.
     * Maps persistence model to domain model.
     * 
     * @param entity the persistence entity
     * @return reconstituted Batch aggregate
     */
    private Batch reconstituteBatch(BatchEntity entity) {
        // Map entity fields to domain value objects
        BatchId id = BatchId.of(entity.getBatchId());
        
        BankInfo bankInfo = BankInfo.of(
            entity.getBankName(),
            entity.getAbiCode(),
            entity.getLeiCode()
        );
        
        BatchStatus status = BatchStatus.valueOf(entity.getStatus());
        
        int totalExposures = entity.getTotalExposures();
        
        // Source data URI - using calculation results URI as fallback
        // (In the current schema, we don't have a separate source_data_uri field)
        FileStorageUri sourceDataUri = entity.getCalculationResultsUri() != null
            ? FileStorageUri.of(entity.getCalculationResultsUri())
            : null;
        
        FileStorageUri calculationResultsUri = entity.getCalculationResultsUri() != null
            ? FileStorageUri.of(entity.getCalculationResultsUri())
            : null;
        
        // Reconstitute timestamps
        ProcessingTimestamps timestamps = ProcessingTimestamps.reconstitute(
            entity.getIngestedAt(),
            java.util.Optional.ofNullable(entity.getProcessedAt()),
            java.util.Optional.empty() // failedAt - not stored in current schema
        );
        
        String failureReason = null; // Not stored in current schema
        
        // Use reconstitute method to create aggregate without raising events
        return Batch.reconstitute(
            id,
            bankInfo,
            status,
            totalExposures,
            sourceDataUri,
            calculationResultsUri,
            timestamps,
            failureReason
        );
    }
    
    @Override
    @Transactional(readOnly = true)
    public java.util.Optional<String> getCalculationResultsUri(String batchId) {
        try {
            log.debug("Getting calculation results URI for batch: {}", batchId);
            
            return springDataRepository.findById(batchId)
                .map(BatchEntity::getCalculationResultsUri)
                .filter(uri -> uri != null && !uri.trim().isEmpty());
                
        } catch (Exception e) {
            log.error("Failed to get calculation results URI for batch: {}", batchId, e);
            return java.util.Optional.empty();
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean exists(String batchId) {
        try {
            log.debug("Checking if batch exists: {}", batchId);
            
            return springDataRepository.existsById(batchId);
            
        } catch (Exception e) {
            log.error("Failed to check if batch exists: {}", batchId, e);
            return false;
        }
    }
    
    /**
     * Handle duplicate key (batch_id) or other constraint violations
     */
    private Result<Void> handleDuplicateKeyOrConstraintViolation(
            org.springframework.dao.DataIntegrityViolationException e, Batch batch) {
        
        // Check if this is a duplicate key violation (race condition during concurrent processing)
        if (e.getMessage() != null && e.getMessage().contains("batches_pkey")) {
            log.debug("Duplicate batch_id detected: {} - This is expected in concurrent processing", 
                batch.getId().value());
            // Signal to caller that the batch already exists so it can short-circuit work.
            return Result.failure(ErrorDetail.of(
                "BATCH_ALREADY_EXISTS",
                ErrorType.BUSINESS_RULE_ERROR,
                "Batch already exists: " + batch.getId().value(),
                "batch.already.exists"
            ));
        }
        
        log.error("Data integrity violation while saving batch aggregate: {}", batch.getId().value(), e);
        return Result.failure(ErrorDetail.of(
            "BATCH_SAVE_FAILED",
            ErrorType.SYSTEM_ERROR,
            "Failed to save batch due to data integrity violation: " + e.getMessage(),
            "batch.save.failed"
        ));
    }
    
    /**
     * Handle optimistic lock conflicts after retries exhausted
     */
    private Result<Void> handleOptimisticLockConflict(Batch batch) {
        log.error("Optimistic lock conflict for batch: {} after all retries", batch.getId().value());
        
        // Try to get the latest version
        try {
            Optional<BatchEntity> latest = springDataRepository.findById(batch.getId().value());
            if (latest.isPresent()) {
                log.debug("Latest version exists after optimistic lock conflict: {}", 
                    batch.getId().value());
            }
        } catch (Exception ex) {
            log.error("Failed to fetch latest after optimistic lock conflict: {}", 
                batch.getId().value(), ex);
        }
        
        return Result.failure(ErrorDetail.of(
            "BATCH_OPTIMISTIC_LOCK_CONFLICT",
            ErrorType.SYSTEM_ERROR,
            "Batch was modified by another transaction",
            "batch.optimistic.lock.conflict"
        ));
    }
}
