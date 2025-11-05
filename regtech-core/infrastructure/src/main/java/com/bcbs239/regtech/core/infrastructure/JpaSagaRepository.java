package com.bcbs239.regtech.core.infrastructure;

import java.util.Map;

import com.bcbs239.regtech.core.domain.core.Maybe;
import com.bcbs239.regtech.core.domain.core.Result;
import com.bcbs239.regtech.core.domain.saga.ISagaRepository;
import com.bcbs239.regtech.core.domain.saga.Saga;
import com.bcbs239.regtech.core.domain.saga.SagaId;
import com.bcbs239.regtech.core.domain.saga.SagaSnapshot;
import com.bcbs239.regtech.core.domain.saga.TimeoutScheduler;
import com.bcbs239.regtech.core.infrastructure.persistence.LoggingConfiguration;
import com.bcbs239.regtech.core.infrastructure.saga.SagaClosures;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

@Repository
public class JpaSagaRepository implements ISagaRepository {

    private final EntityManager entityManager;
    private final ObjectMapper objectMapper;
    private final TimeoutScheduler timeoutScheduler;

    public JpaSagaRepository(EntityManager entityManager, ObjectMapper objectMapper, TimeoutScheduler timeoutScheduler) {
        this.entityManager = entityManager;
        this.objectMapper = objectMapper;
        this.timeoutScheduler = timeoutScheduler;
    }

    @Override
    public Result<SagaId> save(SagaSnapshot snapshot) {
        try {
            SagaEntity entity = toEntity(snapshot);

            if (entityManager.find(SagaEntity.class, entity.getSagaId()) == null) {
                // Insert new saga
                entityManager.persist(entity);
                LoggingConfiguration.createStructuredLog("SAGA_PERSISTED", Map.of(
                    "sagaId", snapshot.getSagaId(),
                    "sagaType", snapshot.getSagaType()
                ));
            } else {
                // Update existing saga
                entityManager.merge(entity);
                LoggingConfiguration.createStructuredLog("SAGA_UPDATED", Map.of(
                    "sagaId", snapshot.getSagaId(),
                    "sagaType", snapshot.getSagaType()
                ));
            }

            entityManager.flush();
            return Result.success(snapshot.getSagaId());

        } catch (Exception e) {
            LoggingConfiguration.createStructuredLog("SAGA_SAVE_FAILED", Map.of(
                "sagaId", snapshot.getSagaId(),
                "sagaType", snapshot.getSagaType(),
                "error", e.getMessage()
            ));
            // Rethrow to surface the failure to transaction manager (avoid silent rollback-only state)
            throw new RuntimeException("Failed to save saga: " + e.getMessage(), e);
        }
    }

    @Override
    public Maybe<SagaSnapshot> load(SagaId sagaId) {
        try {
            SagaEntity entity = entityManager.find(SagaEntity.class, sagaId.id());
            if (entity == null) {
                LoggingConfiguration.createStructuredLog("SAGA_NOT_FOUND_BY_ID", Map.of(
                    "sagaId", sagaId
                ));
                return Maybe.none();
            }

            return Maybe.some(toSnapshot(entity));

        } catch (Exception e) {
            LoggingConfiguration.createStructuredLog("SAGA_LOAD_FAILED", Map.of(
                "sagaId", sagaId,
                "error", e.getMessage()
            ));
            return Maybe.none();
        }
    }

    private SagaEntity toEntity(SagaSnapshot snapshot) throws Exception {
        return new SagaEntity(
                snapshot.getSagaId().id(),
                snapshot.getSagaType(),
                snapshot.getStatus(),
                snapshot.getStartedAt(),
                safeSerialize(objectMapper, snapshot.getSagaData()),
                safeSerialize(objectMapper, snapshot.getProcessedEvents()),
                safeSerialize(objectMapper, snapshot.getPendingCommands()),
                snapshot.getCompletedAt()
        );
    }

    private SagaSnapshot toSnapshot(SagaEntity entity) {
        return new SagaSnapshot(
                SagaId.of(entity.getSagaId()),
                entity.getSagaType(),
                entity.getStatus(),
                entity.getStartedAt(),
                entity.getSagaData(),
                entity.getProcessedEvents(),
                entity.getPendingCommands(),
                entity.getCompletedAt()
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
