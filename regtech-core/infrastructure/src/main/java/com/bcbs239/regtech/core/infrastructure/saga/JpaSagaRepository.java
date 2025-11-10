package com.bcbs239.regtech.core.infrastructure.saga;

import java.util.Map;

import com.bcbs239.regtech.core.domain.saga.*;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.core.domain.shared.Maybe;
import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.logging.ILogger;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import org.springframework.context.annotation.Primary;

@Repository
@Primary
public class JpaSagaRepository implements ISagaRepository {

    private final EntityManager entityManager;
    private final ObjectMapper objectMapper;
    private final ILogger logger;

    public JpaSagaRepository(EntityManager entityManager, ObjectMapper objectMapper, ILogger logger) {
        this.entityManager = entityManager;
        this.objectMapper = objectMapper;
        this.logger = logger;
    }

    @Override
    public Result<SagaId> save(SagaSnapshot snapshot) {
        try {
            SagaEntity existingEntity = entityManager.find(SagaEntity.class, snapshot.getSagaId().id());
            
            if (existingEntity == null) {
                // Insert new saga
                SagaEntity newEntity = toEntity(snapshot);
                entityManager.persist(newEntity);
                logger.asyncStructuredLog("SAGA_PERSISTED", Map.of(
                    "sagaId", snapshot.getSagaId(),
                    "sagaType", snapshot.getSagaType()
                ));
            } else {
                // Update existing saga - update fields on the managed entity
                existingEntity.updateFrom(snapshot);
                // No need to call merge - entity is already managed
                logger.asyncStructuredLog("SAGA_UPDATED", Map.of(
                    "sagaId", snapshot.getSagaId(),
                    "sagaType", snapshot.getSagaType(),
                    "version", existingEntity.getVersion()
                ));
            }

            return Result.success(snapshot.getSagaId());
        } catch (Exception e) {
            logger.asyncStructuredErrorLog("SAGA_SAVE_FAILED", e, Map.of(
                "sagaId", snapshot.getSagaId(),
                "sagaType", snapshot.getSagaType()
            ));
            return Result.failure(ErrorDetail.of("INTERNAL_ERROR", ErrorType.SYSTEM_ERROR,"Failed to save saga: " + e.getMessage(),
                    "internal.server.error"));
        }
    }

    public Result<SagaId> save(AbstractSaga<?> saga) {
        try {
            SagaEntity existingEntity = entityManager.find(SagaEntity.class, saga.getId().id());
            
            if (existingEntity == null) {
                // Insert new saga
                SagaEntity newEntity = toEntity(saga, objectMapper);
                entityManager.persist(newEntity);
                logger.asyncStructuredLog("SAGA_PERSISTED", Map.of(
                    "sagaId", saga.getId(),
                    "sagaType", saga.getSagaType()
                ));
            } else {
                // Update existing saga - update fields on the managed entity
                existingEntity.updateFrom(saga, objectMapper);
                // No need to call merge - entity is already managed
                logger.asyncStructuredLog("SAGA_UPDATED", Map.of(
                    "sagaId", saga.getId(),
                    "sagaType", saga.getSagaType(),
                    "version", existingEntity.getVersion()
                ));
            }

            entityManager.flush();
            return Result.success(saga.getId());

        } catch (Exception e) {
            logger.asyncStructuredLog("SAGA_SAVE_FAILED", Map.of(
                "sagaId", saga.getId(),
                "sagaType", saga.getClass().getSimpleName(),
                "error", e.getMessage()
            ));
            // Rethrow to surface the failure to transaction manager (avoid silent rollback-only state)
            throw new RuntimeException("Failed to save saga: " + e.getMessage(), e);
        }
    }

    public Maybe<SagaSnapshot> load(SagaId sagaId) {
        try {
            SagaEntity entity = entityManager.find(SagaEntity.class, sagaId.id());
            if (entity == null) {
                logger.asyncStructuredLog("SAGA_NOT_FOUND_BY_ID", Map.of(
                    "sagaId", sagaId
                ));
                return Maybe.none();
            }

            SagaSnapshot snapshot = new SagaSnapshot(
                SagaId.of(entity.getSagaId()),
                entity.getSagaType(),
                entity.getStatus(),
                entity.getStartedAt(),
                entity.getSagaData(),
                entity.getProcessedEvents(),
                entity.getPendingCommands(),
                entity.getCompletedAt()
            );

            return Maybe.some(snapshot);

        } catch (Exception e) {
            logger.asyncStructuredLog("SAGA_LOAD_FAILED", Map.of(
                "sagaId", sagaId,
                "error", e.getMessage()
            ));
            return Maybe.none();
        }
    }

    private static SagaEntity toEntity(AbstractSaga<?> saga, ObjectMapper objectMapper) throws Exception {
        return new SagaEntity(
                saga.getId().id(),
                saga.getSagaType(),
                saga.getStatus(),
                saga.getStartedAt(),
                safeSerialize(objectMapper, saga.getData()),
                safeSerialize(objectMapper,
                        saga.getProcessedEvents().stream()
                                .map(e -> e.getClass().getSimpleName())
                                .toList()
                ),
                // Use a snapshot so persistence does not consume commands (getCommandsToDispatch clears the list)
                safeSerialize(objectMapper, saga.peekCommandsToDispatch()),
                saga.getCompletedAt()
        );
    }

    private SagaEntity toEntity(SagaSnapshot snapshot) throws Exception {
        return new SagaEntity(
                snapshot.getSagaId().id(),
                snapshot.getSagaType(),
                snapshot.getStatus(),
                snapshot.getStartedAt(),
                snapshot.getSagaData(),
                snapshot.getProcessedEvents(),
                snapshot.getPendingCommands(),
                snapshot.getCompletedAt()
        );
    }

    private static String safeSerialize(ObjectMapper mapper, Object value) throws Exception {
        try {
            return mapper.writeValueAsString(value);
        } catch (InvalidDefinitionException e) {
            // likely Java 8 time types not supported by this mapper - create a temporary mapper with JavaTimeModule
            ObjectMapper tmp = mapper.copy();
            tmp.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
            tmp.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            return tmp.writeValueAsString(value);
        }
    }

}
