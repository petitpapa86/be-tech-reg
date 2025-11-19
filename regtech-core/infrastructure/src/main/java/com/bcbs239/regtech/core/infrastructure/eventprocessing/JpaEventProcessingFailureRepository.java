package com.bcbs239.regtech.core.infrastructure.eventprocessing;

import com.bcbs239.regtech.core.domain.eventprocessing.EventProcessingFailure;
import com.bcbs239.regtech.core.domain.eventprocessing.EventProcessingStatus;
import com.bcbs239.regtech.core.domain.eventprocessing.IEventProcessingFailureRepository;
import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Result;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * JPA implementation of IEventProcessingFailureRepository.
 * Provides domain repository functionality for event processing failures.
 */
@Repository
public class JpaEventProcessingFailureRepository implements IEventProcessingFailureRepository {
    private final EventProcessingFailureJpaRepository jpaRepository;
    private final EventProcessingFailureMapper mapper;

    private static final Logger log = LoggerFactory.getLogger(JpaEventProcessingFailureRepository.class);

    public JpaEventProcessingFailureRepository(
            EventProcessingFailureJpaRepository jpaRepository,
            EventProcessingFailureMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Result<EventProcessingFailure> save(EventProcessingFailure failure) {
        try {
            EventProcessingFailureEntity entity = mapper.toEntity(failure);
            EventProcessingFailureEntity saved = jpaRepository.save(entity);
            return Result.success(mapper.toDomain(saved));
        } catch (Exception e) {
            log.error("EVENT_PROCESSING_FAILURE_SAVE_FAILED; details={}", Map.of(
                    "eventType", failure.getEventType(),
                    "error", e.getMessage()
            ), e);
            return Result.failure(ErrorDetail.of(
                    "EVENT_PROCESSING_FAILURE_SAVE_FAILED",
                    ErrorType.SYSTEM_ERROR,
                    "Failed to save event processing failure",
                    "event.processing.failure.save.failed"
            ));
        }
    }

    @Override
    public Result<List<EventProcessingFailure>> findEventsReadyForRetry(int batchSize) {
        try {
            List<EventProcessingFailureEntity> entities = jpaRepository.findEventsReadyForRetry(
                    EventProcessingStatus.PENDING,
                    Instant.now()
            ).stream().limit(batchSize).collect(Collectors.toList());

            List<EventProcessingFailure> failures = entities.stream()
                    .map(mapper::toDomain)
                    .collect(Collectors.toList());

            return Result.success(failures);
        } catch (Exception e) {
            log.error("EVENT_PROCESSING_FAILURE_FIND_RETRY_FAILED; details={}", Map.of(
                    "error", e.getMessage()
            ), e);
            return Result.failure(ErrorDetail.of(
                    "EVENT_PROCESSING_FAILURE_FIND_RETRY_FAILED",
                    ErrorType.SYSTEM_ERROR,
                    "Failed to find events ready for retry",
                    "event.processing.failure.find.retry.failed"
            ));
        }
    }

    @Override
    public Result<List<EventProcessingFailure>> findByUserId(String userId) {
        try {
            List<EventProcessingFailureEntity> entities = jpaRepository.findByMetadataUserId(userId);
            List<EventProcessingFailure> failures = entities.stream()
                    .map(mapper::toDomain)
                    .collect(Collectors.toList());
            return Result.success(failures);
        } catch (Exception e) {
            log.error("EVENT_PROCESSING_FAILURE_FIND_BY_USER_FAILED; details={}", Map.of(
                    "userId", userId,
                    "error", e.getMessage()
            ), e);

            return Result.failure(ErrorDetail.of(
                    "EVENT_PROCESSING_FAILURE_FIND_BY_USER_FAILED",
                    ErrorType.SYSTEM_ERROR,
                    "Failed to find event processing failures by user ID",
                    "event.processing.failure.find.by.user.failed"
            ));
        }
    }

    @Override
    public Result<EventProcessingFailure> findById(String id) {
        try {
            Optional<EventProcessingFailureEntity> entityOpt = jpaRepository.findById(id);
            return entityOpt.map(eventProcessingFailureEntity -> Result.success(mapper.toDomain(eventProcessingFailureEntity))).orElseGet(() -> Result.failure(ErrorDetail.of(
                    "EVENT_PROCESSING_FAILURE_NOT_FOUND",
                    ErrorType.NOT_FOUND_ERROR,
                    "Event processing failure not found",
                    "event.processing.failure.not.found"
            )));
        } catch (Exception e) {
            log.error("EVENT_PROCESSING_FAILURE_FIND_BY_ID_FAILED; details={}", Map.of(
                    "id", id,
                    "error", e.getMessage()
            ), e);

            return Result.failure(ErrorDetail.of(
                    "EVENT_PROCESSING_FAILURE_FIND_BY_ID_FAILED",
                    ErrorType.SYSTEM_ERROR,
                    "Failed to find event processing failure by ID",
                    "event.processing.failure.find.by.id.failed"
            ));
        }
    }

    @Override
    @Transactional
    public Result<Integer> deletePermanentlyFailedEvents(int daysOld) {
        try {
            Instant cutoffDate = Instant.now().minusSeconds(daysOld * 24 * 60 * 60L);
            int deletedCount = jpaRepository.deleteByStatusAndUpdatedAtBefore(
                    EventProcessingStatus.FAILED,
                    cutoffDate
            );
            return Result.success(deletedCount);
        } catch (Exception e) {
            log.error("EVENT_PROCESSING_FAILURE_DELETE_FAILED; details={}", Map.of(
                    "daysOld", daysOld,
                    "error", e.getMessage()
            ), e);

            return Result.failure(ErrorDetail.of(
                    "EVENT_PROCESSING_FAILURE_DELETE_FAILED",
                    ErrorType.SYSTEM_ERROR,
                    "Failed to delete permanently failed events",
                    "event.processing.failure.delete.failed"
            ));
        }
    }
}