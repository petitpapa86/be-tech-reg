package com.bcbs239.regtech.core.saga;

import java.util.Map;
import java.util.function.Function;

import com.bcbs239.regtech.core.config.LoggingConfiguration;
import com.bcbs239.regtech.core.shared.ErrorDetail;
import com.bcbs239.regtech.core.shared.Maybe;
import com.bcbs239.regtech.core.shared.Result;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class JpaSagaRepository {
    @PersistenceContext
    private final EntityManager entityManager;

    private final ObjectMapper objectMapper;

    public <T> Function<AbstractSaga<T>, Result<SagaId>> sagaSaver() {
        return saga -> {
            try {
                SagaEntity entity = toEntity(saga);

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
                return Result.failure(ErrorDetail.of(
                        "SAGA_SAVE_FAILED",
                        "Failed to save saga: " + e.getMessage(),
                        "error.saga.saveFailed"));
            }
        };
    }

    public Function<SagaId, Maybe<AbstractSaga<?>>> sagaLoader() {
        return sagaId -> {
            try {
                SagaEntity entity = entityManager.find(SagaEntity.class, sagaId.id());
                if (entity == null) {
                    return Maybe.none();
                }

                AbstractSaga<?> saga = fromEntity(entity);
                return Maybe.some(saga);

            } catch (Exception e) {
                LoggingConfiguration.createStructuredLog("SAGA_LOAD_FAILED", Map.of(
                    "sagaId", sagaId,
                    "error", e.getMessage()
                ));
                return Maybe.none();
            }
        };
    }

    private <T> SagaEntity toEntity(AbstractSaga<T> saga) throws Exception {
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
                objectMapper.writeValueAsString(saga.getCommandsToDispatch())
        );
    }

    @SuppressWarnings("unchecked")
    private AbstractSaga<?> fromEntity(SagaEntity entity) {
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
    private String getSagaDataClassName(String sagaType) {
        // Simple mapping for now - in production use a registry
        return switch (sagaType) {
            case "TestSaga" -> "java.lang.String";
            default -> "java.lang.Object";
        };
    }

    private String getSagaClassName(String sagaType) {
        // Simple mapping for now - in production use a registry
        return switch (sagaType) {
            case "TestSaga" -> "com.bcbs239.regtech.core.sagav2.TestSaga";
            default -> "com.bcbs239.regtech.core.sagav2.AbstractSaga";
        };
    }
}
