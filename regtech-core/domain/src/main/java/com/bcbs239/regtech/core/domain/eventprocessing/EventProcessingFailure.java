package com.bcbs239.regtech.core.domain.eventprocessing;

import com.bcbs239.regtech.core.domain.shared.Entity;
import lombok.Getter;

import java.time.Instant;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;

/**
 * Domain entity representing a failed event processing attempt.
 * This version stores domain-specific contextual metadata as a generic map so the core
 * module doesn't bake in specific fields (like userId or billingAccountId).
 */
@Getter
public class EventProcessingFailure extends Entity {

    private final String id;
    private final String eventType;
    private final String eventPayload;
    private final Map<String, String> metadata;
    private final String errorMessage;
    private final String errorStacktrace;
    private int retryCount;
    private final int maxRetries;
    private Instant nextRetryAt;
    private EventProcessingStatus status;
    private final Instant createdAt;
    private Instant updatedAt;
    private Instant lastErrorAt;

    private EventProcessingFailure(
            String id,
            String eventType,
            String eventPayload,
            Map<String, String> metadata,
            String errorMessage,
            String errorStacktrace,
            int retryCount,
            int maxRetries,
            Instant nextRetryAt,
            EventProcessingStatus status,
            Instant createdAt,
            Instant updatedAt,
            Instant lastErrorAt) {
        this.id = id;
        this.eventType = eventType;
        this.eventPayload = eventPayload;
        this.metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
        this.errorMessage = errorMessage;
        this.errorStacktrace = errorStacktrace;
        this.retryCount = retryCount;
        this.maxRetries = maxRetries;
        this.nextRetryAt = nextRetryAt;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.lastErrorAt = lastErrorAt;
    }

    /**
     * Factory method (backwards-compatible) that accepts userId and billingAccountId
     * and stores them into metadata. Prefer using the metadata-aware overload when
     * callers have richer context.
     */
    public static EventProcessingFailure create(
            String eventType,
            String eventPayload,
            String errorMessage,
            String errorStacktrace,
            Map<String, String> metadata,
            int maxRetries) {
        return createWithMetadata(eventType, eventPayload, metadata, errorMessage, errorStacktrace, maxRetries);
    }

    /**
     * New factory method that directly accepts a metadata map.
     */
    public static EventProcessingFailure createWithMetadata(
            String eventType,
            String eventPayload,
            Map<String, String> metadata,
            String errorMessage,
            String errorStacktrace,
            int maxRetries) {

        Instant now = Instant.now();
        // Use correlationId as the canonical id when present, otherwise use eventId. Fall back to random UUID.
        String idCandidate = null;
        if (metadata != null) {
            idCandidate = metadata.get("correlationId");
            if (idCandidate == null || idCandidate.isBlank()) {
                idCandidate = metadata.get("eventId");
            }
        }

        String id = (idCandidate != null && !idCandidate.isBlank()) ? idCandidate : UUID.randomUUID().toString();

        return new EventProcessingFailure(
            id,
            eventType,
            eventPayload,
            metadata,
            errorMessage,
            errorStacktrace,
            0, // retryCount
            maxRetries,
            calculateNextRetryAt(0, maxRetries), // nextRetryAt
            EventProcessingStatus.PENDING,
            now, // createdAt
            now, // updatedAt
            now  // lastErrorAt
        );
    }

    /**
     * Reconstruct from persistence (metadata-aware)
     */
    public static EventProcessingFailure reconstitute(
            String id,
            String eventType,
            String eventPayload,
            Map<String, String> metadata,
            String errorMessage,
            String errorStacktrace,
            int retryCount,
            int maxRetries,
            Instant nextRetryAt,
            EventProcessingStatus status,
            Instant createdAt,
            Instant updatedAt,
            Instant lastErrorAt) {

        return new EventProcessingFailure(
            id,
            eventType,
            eventPayload,
            metadata,
            errorMessage,
            errorStacktrace,
            retryCount,
            maxRetries,
            nextRetryAt,
            status,
            createdAt,
            updatedAt,
            lastErrorAt
        );
    }

    /**
     * Mark as processing and return new instance
     */
    public EventProcessingFailure markAsProcessing() {
        return new EventProcessingFailure(
            this.id,
            this.eventType,
            this.eventPayload,
            this.metadata,
            this.errorMessage,
            this.errorStacktrace,
            this.retryCount,
            this.maxRetries,
            this.nextRetryAt,
            EventProcessingStatus.PROCESSING,
            this.createdAt,
            Instant.now(), // updatedAt
            this.lastErrorAt
        );
    }

    /**
     * Mark as succeeded and return new instance
     */
    public EventProcessingFailure markAsSucceeded() {
        return new EventProcessingFailure(
            this.id,
            this.eventType,
            this.eventPayload,
            this.metadata,
            this.errorMessage,
            this.errorStacktrace,
            this.retryCount,
            this.maxRetries,
            this.nextRetryAt,
            EventProcessingStatus.SUCCEEDED,
            this.createdAt,
            Instant.now(), // updatedAt
            this.lastErrorAt
        );
    }

    /**
     * Mark as failed and return new instance with updated retry count.
     * Since this is an immutable domain entity, we return a new instance.
     */
    public EventProcessingFailure markAsFailed(String errorMessage, String errorStacktrace) {
        int newRetryCount = this.retryCount + 1;
        Instant now = Instant.now();

        EventProcessingStatus newStatus;
        Instant newNextRetryAt;

        if (newRetryCount >= this.maxRetries) {
            newStatus = EventProcessingStatus.FAILED;
            newNextRetryAt = null;
        } else {
            newStatus = EventProcessingStatus.PENDING;
            newNextRetryAt = calculateNextRetryAt(newRetryCount, this.maxRetries);
        }

        return new EventProcessingFailure(
            this.id,
            this.eventType,
            this.eventPayload,
            this.metadata,
            errorMessage,
            errorStacktrace,
            newRetryCount,
            this.maxRetries,
            newNextRetryAt,
            newStatus,
            this.createdAt,
            now, // updatedAt
            now  // lastErrorAt
        );
    }

    /**
     * Calculate next retry time using exponential backoff
     * Backoff intervals: 1min, 2min, 5min, 15min, 30min
     */
    private static Instant calculateNextRetryAt(int retryCount, int maxRetries) {
        if (retryCount >= maxRetries) {
            return null;
        }

        long[] backoffIntervalsMinutes = {1, 2, 5, 15, 30};
        int index = Math.min(retryCount, backoffIntervalsMinutes.length - 1);
        long delayMinutes = backoffIntervalsMinutes[index];

        return Instant.now().plusSeconds(delayMinutes * 60);
    }

    /**
     * Check if ready for retry
     */
    public boolean isReadyForRetry() {
        return status == EventProcessingStatus.PENDING &&
               nextRetryAt != null &&
               Instant.now().isAfter(nextRetryAt);
    }

    /**
     * Check if permanently failed
     */
    public boolean isPermanentlyFailed() {
        return status == EventProcessingStatus.FAILED;
    }

    /**
     * Check if succeeded
     */
    public boolean isSucceeded() {
        return status == EventProcessingStatus.SUCCEEDED;
    }

    // Convenience accessor for common metadata keys
    public String getUserId() {
        return metadata == null ? null : metadata.get("userId");
    }

    public String getBillingAccountId() {
        return metadata == null ? null : metadata.get("billingAccountId");
    }

    public Map<String, String> getMetadata() {
        return metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}