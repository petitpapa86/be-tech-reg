package com.bcbs239.regtech.core.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Generic outbox processor. Loads messages using provided repository closures,
 * deserializes payloads and dispatches to registered handlers keyed by eventType.
 * Uses Spring's async capabilities for concurrent message processing within batches.
 */
@Component
public class ProcessOutboxJob {

    private final OutboxMessageRepository repository;
    private final ObjectMapper objectMapper;
    private final Map<String, Function<Object, Boolean>> handlersByEventType;
    private final OutboxOptions options;

    public ProcessOutboxJob(
            OutboxMessageRepository repository,
            ObjectMapper objectMapper,
            @Qualifier("iamOutboxHandlers") Map<String, Function<Object, Boolean>> handlersByEventType,
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
     * Process a provided list of messages concurrently using Spring @Async.
     * Individual message failures don't fail the entire batch - each message is handled independently.
     * Returns number processed successfully.
     */
    public int processMessages(List<OutboxMessage> messages) {
        if (messages.isEmpty()) {
            return 0;
        }

        // For small batches, use sequential processing
        if (messages.size() <= 3) {
            return processMessagesSequentially(messages);
        }

        // Process messages concurrently using CompletableFuture
        List<CompletableFuture<Integer>> futures = messages.stream()
            .map(this::processSingleMessageAsync)
            .toList();

        // Wait for all to complete and collect results
        return futures.stream()
            .mapToInt(future -> {
                try {
                    return future.join(); // Will not throw - exceptions handled internally
                } catch (Exception e) {
                    System.err.println("Async processing failed: " + e.getMessage());
                    return 0;
                }
            })
            .sum();
    }

    /**
     * Process a single message asynchronously.
     * Returns CompletableFuture that resolves to 1 if successful, 0 if failed.
     */
    @Async
    public CompletableFuture<Integer> processSingleMessageAsync(OutboxMessage message) {
        try {
            return CompletableFuture.completedFuture(processSingleMessage(message));
        } catch (Exception e) {
            System.err.println("Failed processing outbox message " + message.getId() + ": " + e.getMessage());
            return CompletableFuture.completedFuture(0);
        }
    }

    /**
     * Process a single message and return 1 if successful, 0 if failed.
     */
    private int processSingleMessage(OutboxMessage message) throws Exception {
        Function<Object, Boolean> handler = handlersByEventType.get(message.getEventType());
        if (handler == null) {
            // No handler registered; skip or mark processed depending on policy
            repository.markProcessed().apply(message.getId());
            return 0; // Not counted as processed since no handler was invoked
        }

        // Attempt to deserialize payload into a generic Object, handler is expected to cast
        Object event = objectMapper.readValue(message.getPayload(), Object.class);

        Boolean ok = handler.apply(event);
        if (Boolean.TRUE.equals(ok)) {
            repository.markProcessed().apply(message.getId());
            return 1;
        } else {
            // handler returned false -> mark as failed (increment retry)
            repository.markFailed().apply(message.getId(), "handler-returned-false");
            return 0;
        }
    }

    /**
     * Fallback sequential processing for small batches or error cases.
     */
    private int processMessagesSequentially(List<OutboxMessage> messages) {
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
