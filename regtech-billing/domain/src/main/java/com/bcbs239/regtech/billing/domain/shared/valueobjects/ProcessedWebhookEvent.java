package com.bcbs239.regtech.billing.domain.shared.valueobjects;

import java.time.Instant;

/**
 * Value object representing a processed webhook event for idempotency tracking.
 * Stores the event ID, processing result, and timestamp.
 */
public record ProcessedWebhookEvent(
    String eventId,
    String eventType,
    ProcessingResult result,
    String errorMessage,
    Instant processedAt
) {
    
    public enum ProcessingResult {
        SUCCESS,
        FAILURE
    }
    
    /**
     * Factory method for successful processing
     */
    public static ProcessedWebhookEvent success(String eventId, String eventType) {
        return new ProcessedWebhookEvent(
            eventId,
            eventType,
            ProcessingResult.SUCCESS,
            null,
            Instant.now()
        );
    }
    
    /**
     * Factory method for failed processing
     */
    public static ProcessedWebhookEvent failure(String eventId, String eventType, String errorMessage) {
        return new ProcessedWebhookEvent(
            eventId,
            eventType,
            ProcessingResult.FAILURE,
            errorMessage,
            Instant.now()
        );
    }
    
    /**
     * Check if processing was successful
     */
    public boolean wasSuccessful() {
        return result == ProcessingResult.SUCCESS;
    }
}

