package com.bcbs239.regtech.core.infrastructure.outbox;

import java.time.Instant;

/**
 * Interface representing an outbox message for the recovery service.
 * This provides a clean abstraction over the OutboxMessageEntity.
 */
public interface OutboxMessage {
    
    Long getId();
    String getEventType();
    String getEventPayload();
    OutboxMessageStatus getStatus();
    void setStatus(OutboxMessageStatus status);
    int getRetryCount();
    void setRetryCount(int retryCount);
    Instant getNextRetryTime();
    void setNextRetryTime(Instant nextRetryTime);
    String getLastError();
    void setLastError(String lastError);
    Instant getDeadLetterTime();
    void setDeadLetterTime(Instant deadLetterTime);
    Instant getUpdatedAt();
    Instant getProcessedAt();
    void setProcessedAt(Instant processedAt);
    
    /**
     * Default implementation using OutboxMessageEntity.
     */
    static OutboxMessage from(OutboxMessageEntity entity) {
        return new OutboxMessageImpl(entity);
    }
    
    /**
     * Implementation wrapper around OutboxMessageEntity.
     */
    class OutboxMessageImpl implements OutboxMessage {
        private final OutboxMessageEntity entity;
        
        public OutboxMessageImpl(OutboxMessageEntity entity) {
            this.entity = entity;
        }
        
        @Override
        public Long getId() {
            return entity.getId() != null ? Long.valueOf(entity.getId().hashCode()) : null;
        }
        
        @Override
        public String getEventType() {
            return entity.getEventType();
        }
        
        @Override
        public String getEventPayload() {
            return entity.getEventPayload();
        }
        
        @Override
        public OutboxMessageStatus getStatus() {
            return entity.getStatus();
        }
        
        @Override
        public void setStatus(OutboxMessageStatus status) {
            entity.setStatus(status);
        }
        
        @Override
        public int getRetryCount() {
            return entity.getRetryCount();
        }
        
        @Override
        public void setRetryCount(int retryCount) {
            entity.setRetryCount(retryCount);
        }
        
        @Override
        public Instant getNextRetryTime() {
            return entity.getNextRetryTime();
        }
        
        @Override
        public void setNextRetryTime(Instant nextRetryTime) {
            entity.setNextRetryTime(nextRetryTime);
        }
        
        @Override
        public String getLastError() {
            return entity.getLastError();
        }
        
        @Override
        public void setLastError(String lastError) {
            entity.setLastError(lastError);
        }
        
        @Override
        public Instant getDeadLetterTime() {
            return entity.getDeadLetterTime();
        }
        
        @Override
        public void setDeadLetterTime(Instant deadLetterTime) {
            entity.setDeadLetterTime(deadLetterTime);
        }
        
        @Override
        public Instant getUpdatedAt() {
            return entity.getUpdatedAt();
        }
        
        @Override
        public Instant getProcessedAt() {
            return entity.getProcessedAt();
        }
        
        @Override
        public void setProcessedAt(Instant processedAt) {
            entity.setProcessedOnUtc(processedAt);
        }
        
        public OutboxMessageEntity getEntity() {
            return entity;
        }
    }
}