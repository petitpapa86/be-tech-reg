package com.bcbs239.regtech.core.saga;

import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory implementation of SagaRepository for modular monolith.
 * In production, this would be replaced with JPA/Hibernate implementation.
 */
@Repository
public class InMemorySagaRepository implements SagaRepository {

    private final Map<String, SagaData> sagaStore = new ConcurrentHashMap<>();

    @Override
    public <T extends SagaData> void save(T sagaData) {
        sagaStore.put(sagaData.getSagaId(), sagaData);
    }

    @Override
    public SagaData findById(String sagaId) {
        return sagaStore.get(sagaId);
    }

    @Override
    public SagaData findByTypeAndCorrelationId(String sagaType, String correlationId) {
        return sagaStore.values().stream()
                .filter(saga -> sagaType.equals(saga.getMetadata("sagaType")) &&
                               correlationId.equals(saga.getMetadata("correlationId")))
                .findFirst()
                .orElse(null);
    }

    @Override
    public void updateStatus(String sagaId, SagaData.SagaStatus status) {
        SagaData sagaData = sagaStore.get(sagaId);
        if (sagaData != null) {
            sagaData.setStatus(status);
        }
    }

    @Override
    public void delete(String sagaId) {
        sagaStore.remove(sagaId);
    }

    @Override
    public Iterable<SagaData> findActiveSagas() {
        return sagaStore.values().stream()
                .filter(SagaData::isActive)
                .toList();
    }
}