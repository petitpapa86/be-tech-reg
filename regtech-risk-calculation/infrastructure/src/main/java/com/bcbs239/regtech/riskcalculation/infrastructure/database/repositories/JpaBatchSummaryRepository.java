package com.bcbs239.regtech.riskcalculation.infrastructure.database.repositories;

import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.core.infrastructure.persistence.LoggingConfiguration;
import com.bcbs239.regtech.riskcalculation.domain.calculation.BatchSummary;
import com.bcbs239.regtech.riskcalculation.domain.calculation.IBatchSummaryRepository;
import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.BatchId;
import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.BatchSummaryId;
import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.BankId;
import com.bcbs239.regtech.riskcalculation.infrastructure.database.entities.BatchSummaryEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * JPA repository implementation for BatchSummary with Result-based methods.
 * Provides clean, straightforward persistence operations for BatchSummary aggregates.
 */
@Repository
public class JpaBatchSummaryRepository implements IBatchSummaryRepository {

    @Autowired
    private SpringDataBatchSummaryRepository springDataRepository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Result<BatchSummary> save(BatchSummary batchSummary) {
        try {
            BatchSummaryEntity entity = BatchSummaryEntity.fromDomain(batchSummary);
            BatchSummaryEntity savedEntity = springDataRepository.save(entity);
            
            LoggingConfiguration.logStructured("Batch summary saved successfully",
                Map.of("batchId", batchSummary.getBatchId().value(),
                       "bankId", batchSummary.getBankId().value(),
                       "status", batchSummary.getStatus().toString(),
                       "eventType", "BATCH_SUMMARY_SAVED"));
            
            return Result.success(savedEntity.toDomain());
            
        } catch (Exception e) {
            LoggingConfiguration.logStructured("Error saving batch summary",
                Map.of("batchId", batchSummary.getBatchId().value(),
                       "eventType", "BATCH_SUMMARY_SAVE_ERROR"), e);
            
            return Result.failure(ErrorDetail.of("BATCH_SUMMARY_SAVE_FAILED", ErrorType.INFRASTRUCTURE_ERROR,
                "Failed to save batch summary: " + e.getMessage(), "batch.summary.save.failed"));
        }
    }



    @Override
    public Optional<BatchSummary> findById(BatchSummaryId batchSummaryId) {
        try {
            return springDataRepository.findByBatchSummaryId(batchSummaryId.value())
                .map(BatchSummaryEntity::toDomain);

        } catch (Exception e) {
            LoggingConfiguration.logStructured("Error finding batch summary by ID",
                Map.of("batchSummaryId", batchSummaryId.value(),
                       "eventType", "BATCH_SUMMARY_FIND_ERROR"), e);
            return Optional.empty();
        }
    }

    @Override
    public Optional<BatchSummary> findByBatchId(BatchId batchId) {
        try {
            return springDataRepository.findByBatchId(batchId.value())
                .map(BatchSummaryEntity::toDomain);

        } catch (Exception e) {
            LoggingConfiguration.logStructured("Error finding batch summary by batch ID",
                Map.of("batchId", batchId.value(),
                       "eventType", "BATCH_SUMMARY_FIND_BY_BATCH_ERROR"), e);
            return Optional.empty();
        }
    }

    @Override
    public boolean existsByBatchId(BatchId batchId) {
        try {
            return springDataRepository.existsByBatchId(batchId.value());

        } catch (Exception e) {
            LoggingConfiguration.logStructured("Error checking batch summary existence",
                Map.of("batchId", batchId.value(),
                       "eventType", "BATCH_SUMMARY_EXISTS_ERROR"), e);
            return false;
        }
    }

    @Override
    public Result<List<BatchSummary>> findByBankId(BankId bankId) {
        try {
            List<BatchSummaryEntity> entities = springDataRepository.findByBankIdOrderByCreatedAtDesc(bankId.value());

            List<BatchSummary> batchSummaries = entities.stream()
                .map(BatchSummaryEntity::toDomain)
                .collect(Collectors.toList());

            return Result.success(batchSummaries);

        } catch (Exception e) {
            LoggingConfiguration.logStructured("Error finding batch summaries by bank ID",
                Map.of("bankId", bankId.value(),
                       "eventType", "BATCH_SUMMARY_FIND_BY_BANK_ERROR"), e);

            return Result.failure(ErrorDetail.of("BATCH_SUMMARY_FIND_BY_BANK_FAILED", ErrorType.INFRASTRUCTURE_ERROR,
                "Failed to find batch summaries for bank: " + e.getMessage(), "batch.summary.find.by.bank.failed"));
        }
    }

    @Override
    public Result<List<BatchSummary>> findAll(int page, int size) {
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
            
            List<BatchSummaryEntity> entities = springDataRepository.findAllByOrderByCreatedAtDesc(pageable)
                .getContent();

            List<BatchSummary> batchSummaries = entities.stream()
                .map(BatchSummaryEntity::toDomain)
                .collect(Collectors.toList());

            return Result.success(batchSummaries);

        } catch (Exception e) {
            LoggingConfiguration.logStructured("Error finding all batch summaries",
                Map.of("page", page, "size", size,
                       "eventType", "BATCH_SUMMARY_FIND_ALL_ERROR"), e);

            return Result.failure(ErrorDetail.of("BATCH_SUMMARY_FIND_ALL_FAILED", ErrorType.INFRASTRUCTURE_ERROR,
                "Failed to find batch summaries: " + e.getMessage(), "batch.summary.find.all.failed"));
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Result<Void> delete(BatchSummaryId batchSummaryId) {
        try {
            if (!springDataRepository.findByBatchSummaryId(batchSummaryId.value()).isPresent()) {
                return Result.failure(ErrorDetail.of("BATCH_SUMMARY_NOT_FOUND", ErrorType.BUSINESS_RULE_ERROR,
                    "Batch summary not found for deletion: " + batchSummaryId.value(),
                    "batch.summary.not.found"));
            }

            springDataRepository.deleteByBatchSummaryId(batchSummaryId.value());

            LoggingConfiguration.logStructured("Batch summary deleted successfully",
                Map.of("batchSummaryId", batchSummaryId.value(),
                       "eventType", "BATCH_SUMMARY_DELETED"));

            return Result.success(null);

        } catch (Exception e) {
            LoggingConfiguration.logStructured("Error deleting batch summary",
                Map.of("batchSummaryId", batchSummaryId.value(),
                       "eventType", "BATCH_SUMMARY_DELETE_ERROR"), e);

            return Result.failure(ErrorDetail.of("BATCH_SUMMARY_DELETE_FAILED", ErrorType.INFRASTRUCTURE_ERROR,
                "Failed to delete batch summary: " + e.getMessage(), "batch.summary.delete.failed"));
        }
    }
}