package com.bcbs239.regtech.core.saga;

import java.util.Map;
import java.util.function.Function;

import com.bcbs239.regtech.core.config.LoggingConfiguration;
import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.core.shared.Maybe;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.EntityManager;

import com.bcbs239.regtech.core.infrastructure.entities.SagaEntity;

public class JpaSagaRepository {

    public static Function<AbstractSaga<?>, Result<SagaId>> sagaSaver(EntityManager entityManager, ObjectMapper objectMapper) {
        return saga -> {
            try {
                SagaEntity entity = toEntity(saga, objectMapper);

                if (entityManager.find(SagaEntity.class, entity.getSagaId()) == null) {
                    // Insert new saga
                    entityManager.persist(entity);
                    LoggingConfiguration.createStructuredLog("SAGA_PERSISTED", Map.of(
                        "sagaId", saga.getId(),
                        "sagaType", saga.getSagaType()
                    ));
                } else {
                    // Update existing saga
                    entityManager.merge(entity);
                    LoggingConfiguration.createStructuredLog("SAGA_UPDATED", Map.of(
                        "sagaId", saga.getId(),
                        "sagaType", saga.getSagaType()
                    ));
                }

                entityManager.flush();
                return Result.success(saga.getId());

            } catch (Exception e) {
                LoggingConfiguration.createStructuredLog("SAGA_SAVE_FAILED", Map.of(
                    "sagaId", saga.getId(),
                    "sagaType", saga.getClass().getSimpleName(),
                    "error", e.getMessage()
                ));
                // Rethrow to surface the failure to transaction manager (avoid silent rollback-only state)
                throw new RuntimeException("Failed to save saga: " + e.getMessage(), e);
            }
        };
    }

    public static Function<SagaId, Maybe<AbstractSaga<?>>> sagaLoader(EntityManager entityManager, ObjectMapper objectMapper) {
        return sagaId -> {
            try {
                SagaEntity entity = entityManager.find(SagaEntity.class, sagaId.id());
                if (entity == null) {
                    return Maybe.none();
                }

                return Maybe.some(fromEntity(entity, objectMapper));

            } catch (Exception e) {
                LoggingConfiguration.createStructuredLog("SAGA_LOAD_FAILED", Map.of(
                    "sagaId", sagaId,
                    "error", e.getMessage()
                ));
                return Maybe.none();
            }
        };
    }

    private static SagaEntity toEntity(AbstractSaga<?> saga, ObjectMapper objectMapper) throws Exception {
        return new SagaEntity(
                saga.getId().id(),
                saga.getSagaType(),
                saga.getStatus(),
                saga.getStartedAt(),
                objectMapper.writeValueAsString(saga.getData()),
                objectMapper.writeValueAsString(
                        saga.getProcessedEvents().stream()
                                .map(e -> e.getClass().getSimpleName())
                                .toList()
                ),
                // Use a snapshot so persistence does not consume commands (getCommandsToDispatch clears the list)
                objectMapper.writeValueAsString(saga.peekCommandsToDispatch()),
                saga.getCompletedAt()
        );
    }

    @SuppressWarnings("unchecked")
    private static AbstractSaga<?> fromEntity(SagaEntity entity, ObjectMapper objectMapper) {
        try {
            // Deserialize saga data
            // In production, use a registry pattern => getSagaDataClassName
            Class<?> dataClass = Class.forName(getSagaDataClassName(entity.getSagaType()));
            Object sagaData = objectMapper.readValue(entity.getSagaData(), dataClass);

            // Get saga class
            Class<? extends AbstractSaga<?>> sagaClass =
                    (Class<? extends AbstractSaga<?>>) Class.forName(getSagaClassName(entity.getSagaType()));

            // Create saga instance using reflection
            var constructor = sagaClass.getDeclaredConstructor(
                    SagaId.class,
                    dataClass
            );

            AbstractSaga<?> saga = constructor.newInstance(
                    SagaId.of(entity.getSagaId()),
                    sagaData
            );

            // Restore saga state
            saga.setStatus(entity.getStatus());
            saga.setCompletedAt(entity.getCompletedAt());

            return saga;

        } catch (Exception e) {
            LoggingConfiguration.createStructuredLog("SAGA_DESERIALIZE_FAILED", Map.of(
                "sagaId", entity.getSagaId(),
                "sagaType", entity.getSagaType(),
                "error", e.getMessage()
            ));
            throw new RuntimeException("Failed to deserialize saga", e);
        }
    }

    // TODO: Implement proper saga class registry
    private static String getSagaDataClassName(String sagaType) {
        // Simple mapping for now - in production use a registry
        if ("TestSaga".equals(sagaType)) {
            return "java.lang.String";
        }
        return "java.lang.Object";
    }

    private static String getSagaClassName(String sagaType) {
        // Simple mapping for now - in production use a registry
        if ("TestSaga".equals(sagaType)) {
            return "com.bcbs239.regtech.core.sagav2.TestSaga";
        }
        return "com.bcbs239.regtech.core.sagav2.AbstractSaga";
    }
}
