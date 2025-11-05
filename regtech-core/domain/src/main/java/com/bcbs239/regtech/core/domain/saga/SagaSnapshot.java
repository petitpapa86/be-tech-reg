package com.bcbs239.regtech.core.domain.saga;

import java.time.Instant;

/**
 * Snapshot of saga state for persistence and reconstruction.
 * Contains all data needed to recreate a saga instance.
 */
public class SagaSnapshot {

    private final SagaId sagaId;
    private final String sagaType;
    private final SagaStatus status;
    private final Instant startedAt;
    private final String sagaData;
    private final String processedEvents;
    private final String pendingCommands;
    private final Instant completedAt;

    public SagaSnapshot(SagaId sagaId, String sagaType, SagaStatus status, Instant startedAt,
                       String sagaData, String processedEvents, String pendingCommands, Instant completedAt) {
        this.sagaId = sagaId;
        this.sagaType = sagaType;
        this.status = status;
        this.startedAt = startedAt;
        this.sagaData = sagaData;
        this.processedEvents = processedEvents;
        this.pendingCommands = pendingCommands;
        this.completedAt = completedAt;
    }

    public SagaId getSagaId() {
        return sagaId;
    }

    public String getSagaType() {
        return sagaType;
    }

    public SagaStatus getStatus() {
        return status;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public String getSagaData() {
        return sagaData;
    }

    public String getProcessedEvents() {
        return processedEvents;
    }

    public String getPendingCommands() {
        return pendingCommands;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }
}