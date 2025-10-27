package com.bcbs239.regtech.core.infrastructure.entities;

import com.bcbs239.regtech.core.saga.SagaStatus;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "sagas")
public class SagaEntity {

    @Id
    private String sagaId;

    @Column(nullable = false)
    private String sagaType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SagaStatus status;

    @Column(nullable = false)
    private Instant startedAt;

    @Column(columnDefinition = "TEXT")
    private String sagaData;

    @Column(columnDefinition = "TEXT")
    private String processedEvents;

    @Column(columnDefinition = "TEXT")
    private String commandsToDispatch;

    @Column
    private Instant completedAt;

    // Default constructor for JPA
    protected SagaEntity() {}

    public SagaEntity(String sagaId, String sagaType, SagaStatus status, Instant startedAt,
                      String sagaData, String processedEvents, String commandsToDispatch, Instant completedAt) {
        this.sagaId = sagaId;
        this.sagaType = sagaType;
        this.status = status;
        this.startedAt = startedAt;
        this.sagaData = sagaData;
        this.processedEvents = processedEvents;
        this.commandsToDispatch = commandsToDispatch;
        this.completedAt = completedAt;
    }

    public String getSagaId() {
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

    public String getCommandsToDispatch() {
        return commandsToDispatch;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }
}