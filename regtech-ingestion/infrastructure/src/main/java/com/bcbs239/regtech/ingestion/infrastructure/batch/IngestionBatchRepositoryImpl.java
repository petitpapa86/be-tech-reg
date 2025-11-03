package com.bcbs239.regtech.modules.ingestion.infrastructure.batch;

import com.bcbs239.regtech.core.shared.ErrorDetail;
import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.modules.ingestion.domain.batch.BatchId;
import com.bcbs239.regtech.modules.ingestion.domain.batch.BatchStatus;
import com.bcbs239.regtech.modules.ingestion.domain.batch.IngestionBatch;
import com.bcbs239.regtech.modules.ingestion.domain.batch.IIngestionBatchRepository;
import com.bcbs239.regtech.modules.ingestion.domain.bankinfo.BankId;
import com.bcbs239.regtech.modules.ingestion.infrastructure.batch.persistence.IngestionBatchEntity;
import com.bcbs239.regtech.modules.ingestion.infrastructure.batch.persistence.IngestionBatchJpaRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * JPA implementation of IIngestionBatchRepository.
 * Handles persistence operations for IngestionBatch aggregates.
 */
@Repository
@RequiredArgsConstructor
@Transactional
public class IngestionBatchRepositoryImpl implements IIngestionBatchRepository {
    
    private static final Logger log = LoggerFactory.getLogger(IngestionBatchRepositoryImpl.class);
    private final IngestionBatchJpaRepository jpaRepository;
    
    @Override
    @Transactional(readOnly = true)
    public Optional<IngestionBatch> findByBatchId(BatchId batchId) {
        try {
            return jpaRepository.findByBatchId(batchId.value())
                    .map(IngestionBatchEntity::toDomain);
        } catch (DataAccessException e) {
            log.error("Error finding batch by ID: {}", batchId.value(), e);
            return Optional.empty();
        }
    }
    
    @Override
    public Result<IngestionBatch> save(IngestionBatch batch) {
        try {
            IngestionBatchEntity entity = IngestionBatchEntity.fromDomain(batch);
            IngestionBatchEntity savedEntity = jpaRepository.save(entity);
            IngestionBatch savedBatch = savedEntity.toDomain();
            
            log.debug("Successfully saved batch: {}", batch.getBatchId().value());
            return Result.success(savedBatch);
            
        } catch (DataAccessException e) {
            log.error("Error saving batch: {}", batch.getBatchId().value(), e);
            return Result.failure(new ErrorDetail("DATABASE_ERROR", 
                "Failed to save ingestion batch: " + e.getMessage()));
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<IngestionBatch> findByBankId(BankId bankId) {
        try {
            return jpaRepository.findByBankId(bankId.value())
                    .stream()
                    .map(IngestionBatchEntity::toDomain)
                    .collect(Collectors.toList());
        } catch (DataAccessException e) {
            log.error("Error finding batches by bank ID: {}", bankId.value(), e);
            return List.of();
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<IngestionBatch> findByStatus(BatchStatus status) {
        try {
            return jpaRepository.findByStatus(status)
                    .stream()
                    .map(IngestionBatchEntity::toDomain)
                    .collect(Collectors.toList());
        } catch (DataAccessException e) {
            log.error("Error finding batches by status: {}", status, e);
            return List.of();
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<IngestionBatch> findByBankIdAndStatus(BankId bankId, BatchStatus status) {
        try {
            return jpaRepository.findByBankIdAndStatus(bankId.value(), status)
                    .stream()
                    .map(IngestionBatchEntity::toDomain)
                    .collect(Collectors.toList());
        } catch (DataAccessException e) {
            log.error("Error finding batches by bank ID {} and status {}", bankId.value(), status, e);
            return List.of();
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<IngestionBatch> findByUploadedAtBetween(Instant startTime, Instant endTime) {
        try {
            return jpaRepository.findByUploadedAtBetween(startTime, endTime)
                    .stream()
                    .map(IngestionBatchEntity::toDomain)
                    .collect(Collectors.toList());
        } catch (DataAccessException e) {
            log.error("Error finding batches by upload time range", e);
            return List.of();
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<IngestionBatch> findByBankIdAndUploadedAtBetween(BankId bankId, Instant startTime, Instant endTime) {
        try {
            return jpaRepository.findByBankIdAndUploadedAtBetween(bankId.value(), startTime, endTime)
                    .stream()
                    .map(IngestionBatchEntity::toDomain)
                    .collect(Collectors.toList());
        } catch (DataAccessException e) {
            log.error("Error finding batches by bank ID {} and upload time range", bankId.value(), e);
            return List.of();
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public long countByStatus(BatchStatus status) {
        try {
            return jpaRepository.countByStatus(status);
        } catch (DataAccessException e) {
            log.error("Error counting batches by status: {}", status, e);
            return 0;
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public long countByBankIdAndStatus(BankId bankId, BatchStatus status) {
        try {
            return jpaRepository.countByBankIdAndStatus(bankId.value(), status);
        } catch (DataAccessException e) {
            log.error("Error counting batches by bank ID {} and status {}", bankId.value(), status, e);
            return 0;
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<IngestionBatch> findStuckBatches(int minutesAgo) {
        try {
            Instant cutoffTime = Instant.now().minusSeconds(minutesAgo * 60L);
            return jpaRepository.findStuckBatches(cutoffTime)
                    .stream()
                    .map(IngestionBatchEntity::toDomain)
                    .collect(Collectors.toList());
        } catch (DataAccessException e) {
            log.error("Error finding stuck batches", e);
            return List.of();
        }
    }
    
    @Override
    public Result<Void> delete(BatchId batchId) {
        try {
            if (!jpaRepository.existsByBatchId(batchId.value())) {
                return Result.failure(new ErrorDetail("BATCH_NOT_FOUND", 
                    "Batch not found: " + batchId.value()));
            }
            
            jpaRepository.deleteByBatchId(batchId.value());
            log.debug("Successfully deleted batch: {}", batchId.value());
            return Result.success(null);
            
        } catch (DataAccessException e) {
            log.error("Error deleting batch: {}", batchId.value(), e);
            return Result.failure(new ErrorDetail("DATABASE_ERROR", 
                "Failed to delete batch: " + e.getMessage()));
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean existsByBatchId(BatchId batchId) {
        try {
            return jpaRepository.existsByBatchId(batchId.value());
        } catch (DataAccessException e) {
            log.error("Error checking batch existence: {}", batchId.value(), e);
            return false;
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<IngestionBatch> findBatchesInPeriod(Instant startTime, Instant endTime) {
        try {
            return jpaRepository.findByUploadedAtBetween(startTime, endTime)
                    .stream()
                    .map(IngestionBatchEntity::toDomain)
                    .collect(Collectors.toList());
        } catch (DataAccessException e) {
            log.error("Error finding batches in period {} to {}", startTime, endTime, e);
            return List.of();
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<IngestionBatch> findStuckBatches(List<BatchStatus> statuses, Instant cutoffTime) {
        try {
            return jpaRepository.findStuckBatchesByStatuses(statuses, cutoffTime)
                    .stream()
                    .map(IngestionBatchEntity::toDomain)
                    .collect(Collectors.toList());
        } catch (DataAccessException e) {
            log.error("Error finding stuck batches by statuses {} before {}", statuses, cutoffTime, e);
            return List.of();
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public long count() {
        try {
            return jpaRepository.count();
        } catch (DataAccessException e) {
            log.error("Error counting all batches", e);
            return 0;
        }
    }
    
    /**
     * Find batch entities in a period for compliance reporting.
     * This method returns entities directly for performance reasons in compliance reporting.
     */
    @Transactional(readOnly = true)
    public List<IngestionBatchEntity> findBatchEntitiesInPeriod(Instant startTime, Instant endTime) {
        try {
            return jpaRepository.findByUploadedAtBetween(startTime, endTime);
        } catch (DataAccessException e) {
            log.error("Error finding batch entities in period {} to {}", startTime, endTime, e);
            return List.of();
        }
    }
}