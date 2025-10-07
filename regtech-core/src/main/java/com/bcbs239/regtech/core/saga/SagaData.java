package com.bcbs239.regtech.core.saga;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Base class for saga data that tracks the state of a distributed transaction.
 * Provides common fields and status management for all saga types.
 */
public abstract class SagaData {
    private String id;
    private String correlationId;
    private SagaStatus status = SagaStatus.ACTIVE;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant endTime;
    private String lastError;
    private Map<String, Object> metadata = new HashMap<>();

    public enum SagaStatus {
        STARTED, ACTIVE, COMPLETED, COMPENSATING, COMPENSATED, COMPENSATION_FAILED, TIMED_OUT, FAILED
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getSagaId() { return id; }

    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }

    public SagaStatus getStatus() { return status; }
    public void setStatus(SagaStatus status) {
        this.status = status;
        this.updatedAt = Instant.now();
        if (status != SagaStatus.ACTIVE) {
            this.endTime = Instant.now();
        }
    }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public Instant getEndTime() { return endTime; }
    public void setEndTime(Instant endTime) { this.endTime = endTime; }

    public void setStartedAt(Instant startedAt) { this.createdAt = startedAt; }
    public void setCompletedAt(Instant completedAt) { this.endTime = completedAt; }

    public String getLastError() { return lastError; }
    public void setLastError(String lastError) { this.lastError = lastError; }

    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }

    // Utility methods
    public boolean isActive() {
        return status == SagaStatus.STARTED || status == SagaStatus.ACTIVE || status == SagaStatus.COMPENSATING;
    }
    public boolean isCompleted() { return status == SagaStatus.COMPLETED; }
    public boolean needsCompensation() {
        return status == SagaStatus.TIMED_OUT || status == SagaStatus.FAILED;
    }

    public void addMetadata(String key, Object value) {
        this.metadata.put(key, value);
    }

    public Object getMetadata(String key) {
        return this.metadata.get(key);
    }

    @Override
    public String toString() {
        return String.format("SagaData{id='%s', correlationId='%s', status=%s}",
                id, correlationId, status);
    }
}