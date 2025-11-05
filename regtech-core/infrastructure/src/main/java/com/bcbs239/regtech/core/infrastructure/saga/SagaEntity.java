package com.bcbs239.regtech.core.infrastructure.saga;

import java.time.Instant;

import com.bcbs239.regtech.core.domain.saga.SagaStatus;
import com.bcbs239.regtech.core.infrastructure.systemservices.SystemTimeProvider;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;

@Getter
@Entity
@Table(name = "sagas")
public class SagaEntity {
    private static SystemTimeProvider timeProvider = new SystemTimeProvider();
    @Id
    @Column(name = "saga_id", nullable = false)
    private String sagaId;
    
    @Column(name = "saga_type", nullable = false)
    private String sagaType;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SagaStatus status;
    
    @Column(name = "started_at", nullable = false)
    private Instant startedAt;
    
    @Column(name = "completed_at")
    private Instant completedAt;
    
    @Column(name = "timeout_at")
    private Instant timeoutAt;
    
    @Column(name = "saga_data", columnDefinition = "TEXT")
    private String sagaData; // JSON serialized saga data
    
    @Column(name = "processed_events", columnDefinition = "TEXT")
    private String processedEvents; // JSON array of processed event types
    
    @Column(name = "pending_commands", columnDefinition = "TEXT")
    private String pendingCommands; // JSON array of commands to dispatch
    
    @Version
    @Column(name = "version")
    private Long version;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // Default constructor for JPA
    protected SagaEntity() {}

    // Constructor for creating new saga entities
    public SagaEntity(String sagaId, String sagaType, SagaStatus status, Instant startedAt, 
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
    
    @PrePersist
    protected void onCreate() {
        createdAt = timeProvider.nowInstant();
        updatedAt = timeProvider.nowInstant();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = timeProvider.nowInstant();
    }

    /**
     * Sets the SystemTimeProvider for testing purposes.
     * @param timeProvider the SystemTimeProvider to use
     */
    public static void setTimeProvider(SystemTimeProvider timeProvider) {
        SagaEntity.timeProvider = timeProvider;
    }

    /**
     * Resets the TimeProvider to the default system time provider.
     */
    public static void resetTimeProvider() {
        SagaEntity.timeProvider = new SystemTimeProvider();
    }
}
