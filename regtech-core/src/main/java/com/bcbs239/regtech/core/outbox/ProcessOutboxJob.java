package com.bcbs239.regtech.core.outbox;

import com.bcbs239.regtech.core.config.LoggingConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
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

    private static final Logger logger = LoggerFactory.getLogger(ProcessOutboxJob.class);

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
        long startTime = System.currentTimeMillis();
        MDC.put("component", "outbox_processor");
        MDC.put("operation", "batch_processing");

        try {
            logger.debug("Starting outbox batch processing", LoggingConfiguration.createStructuredLog("OUTBOX_BATCH_START", Map.of(
                "batchSize", options.getBatchSize()
            )));

            // Claim a batch of messages for processing (atomic transition PENDING -> PROCESSING)
            List<OutboxMessage> messages = repository.claimBatch().apply(options.getBatchSize());
            int processed = processMessages(messages);

            long duration = System.currentTimeMillis() - startTime;
            logger.info("Completed outbox batch processing", LoggingConfiguration.createStructuredLog("OUTBOX_BATCH_COMPLETED", Map.of(
                "messagesClaimed", messages.size(),
                "messagesProcessed", processed,
                "duration", duration
            )));

            return processed;

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            LoggingConfiguration.logError("outbox_batch", "BATCH_FAILED", e.getMessage(), e, Map.of(
                "duration", duration
            ));

            logger.error("Failed outbox batch processing", LoggingConfiguration.createStructuredLog("OUTBOX_BATCH_FAILED", Map.of(
                "error", e.getMessage(),
                "duration", duration
            )), e);

            return 0;
        } finally {
            MDC.remove("component");
            MDC.remove("operation");
        }
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
                    LoggingConfiguration.logError("outbox_async", "ASYNC_PROCESSING_FAILED", e.getMessage(), e, Map.of(
                        "batchSize", messages.size()
                    ));

                    logger.error("Async processing failed", LoggingConfiguration.createStructuredLog("OUTBOX_ASYNC_FAILED", Map.of(
                        "error", e.getMessage(),
                        "batchSize", messages.size()
                    )), e);
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
            LoggingConfiguration.logError("outbox_message", "MESSAGE_PROCESSING_FAILED", e.getMessage(), e, Map.of(
                "messageId", message.getId(),
                "eventType", message.getEventType()
            ));

            logger.error("Failed processing outbox message", LoggingConfiguration.createStructuredLog("OUTBOX_MESSAGE_FAILED", Map.of(
                "messageId", message.getId(),
                "eventType", message.getEventType(),
                "error", e.getMessage()
            )), e);
            return CompletableFuture.completedFuture(0);
        }
    }

    /**
     * Process a single message and return 1 if successful, 0 if failed.
     */
    private int processSingleMessage(OutboxMessage message) throws Exception {
        MDC.put("messageId", message.getId().toString());
        MDC.put("eventType", message.getEventType());

        try {
            Function<Object, Boolean> handler = handlersByEventType.get(message.getEventType());
            if (handler == null) {
                logger.warn("No handler registered for event type", LoggingConfiguration.createStructuredLog("OUTBOX_NO_HANDLER", Map.of(
                    "eventType", message.getEventType()
                )));

                // No handler registered; skip or mark processed depending on policy
                repository.markProcessed().apply(message.getId());
                return 0; // Not counted as processed since no handler was invoked
            }

            // Attempt to deserialize payload into a generic Object, handler is expected to cast
            Object event = objectMapper.readValue(message.getPayload(), Object.class);

            // Extract correlation ID from event if it extends BaseEvent
            String correlationId = extractCorrelationId(event);
            if (correlationId != null) {
                MDC.put("correlationId", correlationId);
            }

            logger.debug("Processing outbox message", LoggingConfiguration.createStructuredLog("OUTBOX_MESSAGE_PROCESSING", Map.of(
                "eventType", message.getEventType(),
                "payloadSize", message.getPayload().length(),
                "correlationId", correlationId != null ? correlationId : "unknown"
            )));

            Boolean ok = handler.apply(event);
            if (Boolean.TRUE.equals(ok)) {
                repository.markProcessed().apply(message.getId());
                logger.debug("Successfully processed outbox message", LoggingConfiguration.createStructuredLog("OUTBOX_MESSAGE_SUCCESS", Map.of(
                    "eventType", message.getEventType(),
                    "correlationId", correlationId != null ? correlationId : "unknown"
                )));
                return 1;
            } else {
                // handler returned false -> mark as failed (increment retry)
                repository.markFailed().apply(message.getId(), "handler-returned-false");
                logger.warn("Handler returned false for outbox message", LoggingConfiguration.createStructuredLog("OUTBOX_HANDLER_FAILED", Map.of(
                    "eventType", message.getEventType(),
                    "reason", "handler-returned-false",
                    "correlationId", correlationId != null ? correlationId : "unknown"
                )));
                return 0;
            }
        } finally {
            MDC.remove("messageId");
            MDC.remove("eventType");
            MDC.remove("correlationId");
        }
    }

    /**
     * Fallback sequential processing for small batches or error cases.
     */
    private int processMessagesSequentially(List<OutboxMessage> messages) {
        int processed = 0;
        for (OutboxMessage m : messages) {
            MDC.put("messageId", m.getId().toString());
            MDC.put("eventType", m.getEventType());

            try {
                Function<Object, Boolean> handler = handlersByEventType.get(m.getEventType());
                if (handler == null) {
                    logger.warn("No handler registered for event type", LoggingConfiguration.createStructuredLog("OUTBOX_NO_HANDLER", Map.of(
                        "eventType", m.getEventType()
                    )));

                    // No handler registered; skip or mark processed depending on policy
                    repository.markProcessed().apply(m.getId());
                    continue;
                }

                // Attempt to deserialize payload into a generic Object, handler is expected to cast
                Object event = objectMapper.readValue(m.getPayload(), Object.class);

                // Extract correlation ID from event if it extends BaseEvent
                String correlationId = extractCorrelationId(event);
                if (correlationId != null) {
                    MDC.put("correlationId", correlationId);
                }

                logger.debug("Processing outbox message sequentially", LoggingConfiguration.createStructuredLog("OUTBOX_SEQUENTIAL_PROCESSING", Map.of(
                    "eventType", m.getEventType(),
                    "payloadSize", m.getPayload().length(),
                    "correlationId", correlationId != null ? correlationId : "unknown"
                )));

                Boolean ok = handler.apply(event);
                if (Boolean.TRUE.equals(ok)) {
                    repository.markProcessed().apply(m.getId());
                    processed++;
                    logger.debug("Successfully processed outbox message sequentially", LoggingConfiguration.createStructuredLog("OUTBOX_SEQUENTIAL_SUCCESS", Map.of(
                        "eventType", m.getEventType(),
                        "correlationId", correlationId != null ? correlationId : "unknown"
                    )));
                } else {
                    // handler returned false -> mark as failed (increment retry)
                    repository.markFailed().apply(m.getId(), "handler-returned-false");
                    logger.warn("Handler returned false for outbox message sequentially", LoggingConfiguration.createStructuredLog("OUTBOX_SEQUENTIAL_HANDLER_FAILED", Map.of(
                        "eventType", m.getEventType(),
                        "reason", "handler-returned-false",
                        "correlationId", correlationId != null ? correlationId : "unknown"
                    )));
                }
            } catch (Exception e) {
                // Log and continue with next message
                LoggingConfiguration.logError("outbox_sequential", "SEQUENTIAL_PROCESSING_FAILED", e.getMessage(), e, Map.of(
                    "messageId", m.getId(),
                    "eventType", m.getEventType()
                ));

                logger.error("Failed processing outbox message sequentially", LoggingConfiguration.createStructuredLog("OUTBOX_SEQUENTIAL_FAILED", Map.of(
                    "messageId", m.getId(),
                    "eventType", m.getEventType(),
                    "error", e.getMessage()
                )), e);

                repository.markFailed().apply(m.getId(), e.getMessage());
            } finally {
                MDC.remove("messageId");
                MDC.remove("eventType");
                MDC.remove("correlationId");
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

    /**
     * Extract correlation ID from event if it extends BaseEvent.
     * This enables end-to-end tracing across the entire event processing pipeline.
     */
    private String extractCorrelationId(Object event) {
        if (event == null) {
            return null;
        }

        try {
            // Check if the event has a getCorrelationId method (BaseEvent pattern)
            var method = event.getClass().getMethod("getCorrelationId");
            if (method != null) {
                Object result = method.invoke(event);
                return result != null ? result.toString() : null;
            }
        } catch (Exception e) {
            // Silently ignore - not all events may have correlation IDs
            logger.trace("Could not extract correlation ID from event of type: {}", event.getClass().getSimpleName());
        }

        return null;
    }
}
