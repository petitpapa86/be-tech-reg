package com.bcbs239.regtech.core.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Generic outbox processor. Loads messages using provided repository closures,
 * deserializes payloads and dispatches to registered handlers keyed by eventType.
 */
public class ProcessOutboxJob {

    private final OutboxMessageRepository repository;
    private final ObjectMapper objectMapper;
    private final Map<String, Function<Object, Boolean>> handlersByEventType;
    private final OutboxOptions options;

    public ProcessOutboxJob(
            OutboxMessageRepository repository,
            ObjectMapper objectMapper,
            Map<String, Function<Object, Boolean>> handlersByEventType,
            OutboxOptions options
    ) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.handlersByEventType = handlersByEventType;
        this.options = options;
    }

    /**
     * Run a single batch of processing. Returns number of processed messages.
     */
    public int runOnce() {
        // Claim a batch of messages for processing (atomic transition PENDING -> PROCESSING)
        List<OutboxMessage> messages = repository.claimBatch().apply(options.getBatchSize());
        return processMessages(messages);
    }

    /**
     * Process a provided list of messages (useful for failed/retry batches). Returns number processed.
     */
    public int processMessages(List<OutboxMessage> messages) {
        int processed = 0;
        for (OutboxMessage m : messages) {
            try {
                Function<Object, Boolean> handler = handlersByEventType.get(m.getEventType());
                if (handler == null) {
                    // No handler registered; skip or mark processed depending on policy
                    repository.markProcessed().apply(m.getId());
                    continue;
                }

                // Attempt to deserialize payload into a generic Object, handler is expected to cast
                Object event = objectMapper.readValue(m.getPayload(), Object.class);

                Boolean ok = handler.apply(event);
                if (Boolean.TRUE.equals(ok)) {
                    repository.markProcessed().apply(m.getId());
                    processed++;
                } else {
                    // handler returned false -> mark as failed (increment retry)
                    repository.markFailed().apply(m.getId(), "handler-returned-false");
                }
            } catch (Exception e) {
                // Log and continue with next message
                // In a real implementation use a logger and backoff/poison queue handling
                System.err.println("Failed processing outbox message " + m.getId() + ": " + e.getMessage());
                repository.markFailed().apply(m.getId(), e.getMessage());
            }
        }

        // Attempt to reset eligible failed messages for next runs (simple retry loop)
        // Note: repository.resetForRetry should check retry_count < max and set status to PENDING
        // We don't know max here; repository implementation is responsible for enforcing limits.
        // For simplicity we won't iterate reset here to avoid long-running job.
        return processed;
    }

    /**
     * Return basic statistics using repository's supplier if provided.
     */
    public OutboxStats stats() {
        return repository.statsSupplier().get();
    }

    /**
     * Expose repository for adapter usages (e.g., loading failed messages).
     */
    public OutboxMessageRepository repository() {
        return repository;
    }

    /**
     * Expose options so adapters can inspect batch size or poll interval.
     */
    public OutboxOptions options() {
        return options;
    }

    // Uses top-level OutboxStats record
}
