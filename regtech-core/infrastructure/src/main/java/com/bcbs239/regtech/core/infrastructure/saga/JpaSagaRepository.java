package com.bcbs239.regtech.core.infrastructure.saga;

import com.bcbs239.regtech.core.domain.saga.AbstractSaga;
import com.bcbs239.regtech.core.domain.saga.ISagaRepository;
import com.bcbs239.regtech.core.domain.saga.SagaId;
import com.bcbs239.regtech.core.domain.saga.SagaSnapshot;
import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Maybe;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.util.Map;

@Repository
@Primary
public class JpaSagaRepository implements ISagaRepository {

    private final EntityManager entityManager;
    private final ObjectMapper objectMapper;
    private static final Logger log = LoggerFactory.getLogger(JpaSagaRepository.class);

    public JpaSagaRepository(EntityManager entityManager, ObjectMapper objectMapper) {
        this.entityManager = entityManager;
        this.objectMapper = objectMapper;
    }

    @Override
    public Result<SagaId> save(SagaSnapshot snapshot) {
        try {
            SagaEntity existingEntity = entityManager.find(SagaEntity.class, snapshot.getSagaId().id());
            
            if (existingEntity == null) {
                // Insert new saga
                SagaEntity newEntity = toEntity(snapshot);
                entityManager.persist(newEntity);
                log.info("SAGA_PERSISTED; details={}", Map.of(
                    "sagaId", snapshot.getSagaId(),
                    "sagaType", snapshot.getSagaType()
                ));
            } else {
                // Update existing saga - update fields on the managed entity
                existingEntity.updateFrom(snapshot);
                // No need to call merge - entity is already managed
                log.info("SAGA_UPDATED; details={}", Map.of(
                    "sagaId", snapshot.getSagaId(),
                    "sagaType", snapshot.getSagaType(),
                    "version", existingEntity.getVersion()
                ));
            }

            return Result.success(snapshot.getSagaId());
        } catch (Exception e) {
            log.error("SAGA_SAVE_FAILED; details={}", Map.of(
                "sagaId", snapshot.getSagaId(),
                "sagaType", snapshot.getSagaType(),
                "error", e.getMessage()
            ), e);
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
                log.info("SAGA_PERSISTED; details={}", Map.of(
                    "sagaId", saga.getId(),
                    "sagaType", saga.getSagaType()
                ));
            } else {
                // Update existing saga - update fields on the managed entity
                existingEntity.updateFrom(saga, objectMapper);
                // No need to call merge - entity is already managed
                log.info("SAGA_UPDATED; details={}", Map.of(
                    "sagaId", saga.getId(),
                    "sagaType", saga.getSagaType(),
                    "version", existingEntity.getVersion()
                ));
            }

            entityManager.flush();
            return Result.success(saga.getId());

        } catch (Exception e) {
            log.error("SAGA_SAVE_FAILED; details={}", Map.of(
                "sagaId", saga.getId(),
                "sagaType", saga.getClass().getSimpleName(),
                "error", e.getMessage()
            ), e);
            // Rethrow to surface the failure to transaction manager (avoid silent rollback-only state)
            throw new RuntimeException("Failed to save saga: " + e.getMessage(), e);
        }
    }

    public Maybe<SagaSnapshot> load(SagaId sagaId) {
        try {
            SagaEntity entity = entityManager.find(SagaEntity.class, sagaId.id());
            if (entity == null) {
                log.info("SAGA_NOT_FOUND_BY_ID; details={}", Map.of(
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
            log.error("SAGA_LOAD_FAILED; details={}", Map.of(
                "sagaId", sagaId,
                "error", e.getMessage()
            ), e);
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
