package com.bcbs239.regtech.riskcalculation.infrastructure.database.repositories;

import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Maybe;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.riskcalculation.domain.calculation.Batch;
import com.bcbs239.regtech.riskcalculation.domain.calculation.BatchStatus;
import com.bcbs239.regtech.riskcalculation.domain.persistence.BatchRepository;
import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.BankInfo;
import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.BatchId;
import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.FileStorageUri;
import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.ProcessingTimestamps;
import com.bcbs239.regtech.riskcalculation.infrastructure.database.entities.BatchEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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
            log.debug("Saving batch aggregate: {}", batch.getId().value());
            
            // Find existing entity or create new one
            BatchEntity entity = springDataRepository.findById(batch.getId().value())
                .orElseGet(() -> {
                    BatchEntity newEntity = new BatchEntity();
                    return newEntity;
                });
            
            // Let the aggregate populate the entity (Tell, don't ask)
            batch.populateEntity(entity);
            
            // Persist the entity
            springDataRepository.save(entity);
            
            log.debug("Successfully saved batch aggregate: {}", batch.getId().value());
            return Result.success();
            
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
                .map(Maybe::of)
                .orElse(Maybe.empty());
                
        } catch (Exception e) {
            log.error("Failed to find batch by ID: {}", batchId.value(), e);
            return Maybe.empty();
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
            entity.getProcessedAt(),
            null // failedAt - not stored in current schema
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
}
