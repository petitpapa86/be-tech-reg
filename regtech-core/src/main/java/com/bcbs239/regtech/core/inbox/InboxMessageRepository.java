package com.bcbs239.regtech.core.inbox;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.BiFunction;

/**
 * Generic repository closures for inbox operations.
 * Implementations should be provided per bounded context (schema-aware).
 */
public interface InboxMessageRepository {

    /**
     * Load next pending messages (honoring batchSize). Implementations may filter by schema.
     */
    Function<Integer, List<InboxMessage>> messageLoader();

    /**
     * Mark a message as processed (delete or flag) by id.
     */
    Function<String, Boolean> markProcessed();

    /**
     * Load failed messages eligible for retry.
     */
    default Function<Integer, List<InboxMessage>> failedMessageLoader() { return batch -> List.of(); }

    /**
     * Atomically claim a batch of pending messages and mark them as PROCESSING.
     * Implementations should ensure this operation is transactional to avoid double-claims.
     */
    default Function<Integer, List<InboxMessage>> claimBatch() { return messageLoader(); }

    /**
     * Mark message as failed and record error (increment retry counter). Returns true if updated.
     */
    default BiFunction<String, String, Boolean> markFailed() { return (id, err) -> false; }

    /**
     * Reset a failed message to PENDING for retry. Returns true if the reset occurred (e.g., retryCount < max).
     */
    default Function<String, Boolean> resetForRetry() { return id -> false; }

    /**
     * Supplier for inbox statistics; optional.
     */
    default Supplier<InboxStats> statsSupplier() { return () -> new InboxStats(0,0,0,0,0); }
}