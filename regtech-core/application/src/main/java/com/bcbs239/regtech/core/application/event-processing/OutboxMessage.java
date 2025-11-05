package com.bcbs239.regtech.core.application.eventprocessing;

import com.bcbs239.regtech.core.domain.eventprocessing.OutboxMessage;
import com.bcbs239.regtech.core.domain.events.OutboxMessageStatus;
import java.time.Instant;

/**
 * Application service interface for outbox messages.
 * This extends the domain interface with application-specific methods.
 */
public interface OutboxMessage extends com.bcbs239.regtech.core.domain.eventprocessing.OutboxMessage {

    // Application-specific methods can be added here
    void markAsFailed(String error);
}
    
    /**
     * Default implementation using domain OutboxMessage.
     */
    static OutboxMessage from(com.bcbs239.regtech.core.domain.eventprocessing.OutboxMessage domainMessage) {
        return new OutboxMessageImpl(domainMessage);
    }

    /**
     * Implementation wrapper around domain OutboxMessage.
     */
    class OutboxMessageImpl implements OutboxMessage {
        private final com.bcbs239.regtech.core.domain.eventprocessing.OutboxMessage domainMessage;

        public OutboxMessageImpl(com.bcbs239.regtech.core.domain.eventprocessing.OutboxMessage domainMessage) {
            this.domainMessage = domainMessage;
        }
        
        @Override
        public String getId() {
            return domainMessage.getId();
        }

        @Override
        public String getType() {
            return domainMessage.getType();
        }

        @Override
        public String getContent() {
            return domainMessage.getContent();
        }

        @Override
        public OutboxMessageStatus getStatus() {
            return domainMessage.getStatus();
        }

        @Override
        public Instant getOccurredOnUtc() {
            return domainMessage.getOccurredOnUtc();
        }

        @Override
        public Instant getProcessedOnUtc() {
            return domainMessage.getProcessedOnUtc();
        }

        @Override
        public int getRetryCount() {
            return domainMessage.getRetryCount();
        }

        @Override
        public Instant getNextRetryTime() {
            return domainMessage.getNextRetryTime();
        }

        @Override
        public String getLastError() {
            return domainMessage.getLastError();
        }

        @Override
        public Instant getDeadLetterTime() {
            return domainMessage.getDeadLetterTime();
        }

        @Override
        public boolean isPending() {
            return domainMessage.isPending();
        }

        @Override
        public boolean isProcessed() {
            return domainMessage.isProcessed();
        }

        @Override
        public boolean isFailed() {
            return domainMessage.isFailed();
        }

        @Override
        public boolean canRetry() {
            return domainMessage.canRetry();
        }

        @Override
        public void markAsProcessing() {
            domainMessage.markAsProcessing();
        }

        @Override
        public void incrementRetryCount() {
            domainMessage.incrementRetryCount();
        }

        @Override
        public void markAsFailed(String error) {
            // Application-specific method - delegate to domain interface
            // The domain interface's markAsFailed method should handle setting the error
        }
    }
}
