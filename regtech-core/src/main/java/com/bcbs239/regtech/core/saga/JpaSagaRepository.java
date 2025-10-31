package com.bcbs239.regtech.core.saga;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.function.Function;

import com.bcbs239.regtech.core.config.LoggingConfiguration;
import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.core.shared.Maybe;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;

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

    // Modified to accept a TimeoutScheduler provided by the Spring context
    public static Function<SagaId, Maybe<AbstractSaga<?>>> sagaLoader(EntityManager entityManager, ObjectMapper objectMapper, SagaClosures.TimeoutScheduler timeoutScheduler) {
        return sagaId -> {
            try {
                SagaEntity entity = entityManager.find(SagaEntity.class, sagaId.id());
                if (entity == null) {
                    LoggingConfiguration.createStructuredLog("SAGA_NOT_FOUND_BY_ID", Map.of(
                        "sagaId", sagaId
                    ));
                    return Maybe.none();
                }

                return Maybe.some(fromEntity(entity, objectMapper, timeoutScheduler));

            } catch (Exception e) {
                LoggingConfiguration.createStructuredLog("SAGA_LOAD_FAILED", Map.of(
                    "sagaId", sagaId,
                    "error", e.getMessage()
                ));
                return Maybe.none();
            }
        };
    }

    /**
     * Backwards-compatible overload kept for tests/older callers. Prefer the variant that accepts
     * a shared `SagaClosures.TimeoutScheduler` from the Spring context.
     */
    @Deprecated
    public static Function<SagaId, Maybe<AbstractSaga<?>>> sagaLoader(EntityManager entityManager, ObjectMapper objectMapper) {
        // Create a reasonable fallback scheduler for legacy callers; prefer injection of the real scheduler.
        SagaClosures.TimeoutScheduler fallback = SagaClosures.timeoutScheduler(Executors.newSingleThreadScheduledExecutor());
        return sagaLoader(entityManager, objectMapper, fallback);
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

    @SuppressWarnings("unchecked")
    private static AbstractSaga<?> fromEntity(SagaEntity entity, ObjectMapper objectMapper, SagaClosures.TimeoutScheduler timeoutScheduler) {
        try {
            // Deserialize saga data
            Class<?> dataClass = Class.forName(getSagaDataClassName(entity.getSagaType()));
            Object sagaData = objectMapper.readValue(entity.getSagaData(), dataClass);

            // Get saga class
            Class<? extends AbstractSaga<?>> sagaClass =
                    (Class<? extends AbstractSaga<?>>) Class.forName(getSagaClassName(entity.getSagaType()));

            // Try to find constructor with (SagaId, data, TimeoutScheduler)
            try {
                var ctor3 = sagaClass.getDeclaredConstructor(SagaId.class, dataClass, SagaClosures.TimeoutScheduler.class);
                ctor3.setAccessible(true);
                AbstractSaga<?> saga = (AbstractSaga<?>) ctor3.newInstance(
                        SagaId.of(entity.getSagaId()),
                        sagaData,
                        // use the provided timeout scheduler from the Spring context
                        timeoutScheduler
                );
                saga.setStatus(entity.getStatus());
                saga.setCompletedAt(entity.getCompletedAt());
                return saga;
            } catch (NoSuchMethodException ignore) {
                // fall back
            }

            // Try constructor with (SagaId, data)
            try {
                var ctor2 = sagaClass.getDeclaredConstructor(SagaId.class, dataClass);
                ctor2.setAccessible(true);
                AbstractSaga<?> saga = (AbstractSaga<?>) ctor2.newInstance(
                        SagaId.of(entity.getSagaId()),
                        sagaData
                );
                saga.setStatus(entity.getStatus());
                saga.setCompletedAt(entity.getCompletedAt());
                return saga;
            } catch (NoSuchMethodException ex2) {
                // Last resort: try to instantiate using any available constructor
                for (var ctor : sagaClass.getDeclaredConstructors()) {
                    ctor.setAccessible(true);
                    var params = ctor.getParameterTypes();
                    if (params.length == 2 && params[0] == SagaId.class) {
                        AbstractSaga<?> saga = (AbstractSaga<?>) ctor.newInstance(SagaId.of(entity.getSagaId()), sagaData);
                        saga.setStatus(entity.getStatus());
                        saga.setCompletedAt(entity.getCompletedAt());
                        return saga;
                    } else if (params.length == 3 && params[0] == SagaId.class) {
                        AbstractSaga<?> saga = (AbstractSaga<?>) ctor.newInstance(
                                SagaId.of(entity.getSagaId()),
                                sagaData,
                                timeoutScheduler
                        );
                        saga.setStatus(entity.getStatus());
                        saga.setCompletedAt(entity.getCompletedAt());
                        return saga;
                    }
                }
            }

            throw new IllegalStateException("No suitable constructor found for saga class: " + sagaClass.getName());

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
        if ("PaymentVerificationSaga".equals(sagaType)) {
            return "com.bcbs239.regtech.billing.domain.billing.PaymentVerificationSagaData";
        }
        return "java.lang.Object";
    }

    private static String getSagaClassName(String sagaType) {
        // Simple mapping for now - in production use a registry
        if ("TestSaga".equals(sagaType)) {
            return "com.bcbs239.regtech.core.sagav2.TestSaga";
        }
        if ("PaymentVerificationSaga".equals(sagaType)) {
            return "com.bcbs239.regtech.billing.application.policies.PaymentVerificationSaga";
        }
        return "com.bcbs239.regtech.core.sagav2.AbstractSaga";
    }
}
