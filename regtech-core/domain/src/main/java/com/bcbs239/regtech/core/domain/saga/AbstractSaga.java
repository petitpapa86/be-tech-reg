package com.bcbs239.regtech.core.domain.saga;

import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.function.Consumer;
@Setter
@Getter
@Slf4j
public abstract class AbstractSaga<T> {
    protected final SagaId id;
    protected final String sagaType;
    protected final Instant startedAt;
    protected SagaStatus status;
    protected Instant completedAt;
    protected final T data;
    
    // New fields for retry and error handling
    private final List<SagaError> errors = new ArrayList<>();
    private int retryCount = 0;
    private static final int MAX_RETRIES = 3;
    
    // Inject the timeout scheduler (for retries and timeouts)
    protected final TimeoutScheduler timeoutScheduler;

    
    private final List<SagaCommand> commandsToDispatch = new ArrayList<>();
    private final List<SagaMessage> processedEvents = new ArrayList<>();
    private final Map<Class<? extends SagaMessage>, Consumer<SagaMessage>> eventHandlers = new HashMap<>();

    protected AbstractSaga(SagaId id, String sagaType, T data, TimeoutScheduler timeoutScheduler) {
        this.id = id;
        this.sagaType = sagaType;
        this.data = data;
        this.status = SagaStatus.STARTED;
        this.startedAt = Instant.now();
        this.timeoutScheduler = timeoutScheduler;
    }

    // Functional handler registration - pure closure approach
    protected <E extends SagaMessage> void onEvent(Class<E> eventType, Consumer<E> handler) {
        eventHandlers.put(eventType, (SagaMessage event) -> {
            @SuppressWarnings("unchecked")
            E typedEvent = (E) event;
            handler.accept(typedEvent);
        });
        log.info("SAGA_EVENT_HANDLER_REGISTERED; details={}", Map.of(
            "sagaId", id,
            "sagaType", sagaType,
            "eventType", eventType.getSimpleName()
        ));
    }

    // Handle incoming events
    public void handle(SagaMessage event) {
        if (isCompleted()) {
            log.info("SAGA_EVENT_IGNORED; details={}", Map.of(
                "sagaId", id,
                "sagaType", sagaType,
                "eventType", event.eventType()
            ));
            return;
        }
        
        // Find a handler registered for the exact event class or a superclass/interface compatible with it
        Consumer<SagaMessage> handler = null;
        for (Class<? extends SagaMessage> registered : eventHandlers.keySet()) {
            if (registered.isInstance(event)) {
                handler = eventHandlers.get(registered);
                break;
            }
        }
         if (handler != null) {
             log.info("SAGA_EVENT_PROCESSED; details={}", Map.of(
                 "sagaId", id,
                 "sagaType", sagaType,
                 "eventType", event.eventType()
             ));
             handler.accept(event);
             processedEvents.add(event);
             updateStatus();
         } else {
             log.info("SAGA_EVENT_IGNORED; details={}", Map.of(
                 "sagaId", id,
                 "sagaType", sagaType,
                 "eventType", event.eventType()
             ));
         }
    }

    protected abstract void updateStatus();

    // Command dispatching
    protected void dispatchCommand(SagaCommand command) {
        commandsToDispatch.add(command);
        log.info("SAGA_COMMAND_DISPATCHED; details={}", Map.of(
            "sagaId", id,
            "sagaType", sagaType,
            "commandType", command.commandType(),
            "createdAt", command.createdAt().toString()
        ));
    }

    public List<SagaCommand> getCommandsToDispatch() {
        List<SagaCommand> commands = new ArrayList<>(commandsToDispatch);
        commandsToDispatch.clear();
        return commands;
    }

    /**
     * Return a snapshot of commands that are pending dispatch without clearing them.
     * Used by persistence logic to inspect pending commands without consuming them.
     */
    public List<SagaCommand> peekCommandsToDispatch() {
        return new ArrayList<>(commandsToDispatch);
    }

    protected boolean isCompleted() {
        return status == SagaStatus.COMPLETED || status == SagaStatus.FAILED;
    }

    protected void complete() {
        this.status = SagaStatus.COMPLETED;
        this.completedAt = Instant.now();
        log.info("SAGA_COMPLETED; details={}", Map.of(
            "sagaId", id,
            "sagaType", sagaType,
            "completedAt", completedAt.toString()
        ));
    }

    // Enhanced failure method
    protected void fail(SagaError error) {
        if (error.isRecoverable() && retryCount < MAX_RETRIES) {
            scheduleRetry(error);
        } else if (error.isSemiRecoverable()) {
            handleSemiRecoverableError(error);
        } else {
            completeWithFailure(error);
        }
    }
    
    private void scheduleRetry(SagaError error) {
        this.retryCount++;
        this.errors.add(error.withRetry());

        
        // Schedule retry with exponential backoff using the timeout scheduler
        long delayMillis = calculateBackoffDelay().toMillis();
        timeoutScheduler.schedule(
            id + "-retry-" + retryCount,
            delayMillis,
            () -> dispatchCommand(new RetrySagaCommand(id))
        );
    }
    
    private void handleSemiRecoverableError(SagaError error) {
        this.errors.add(error);
        
        // For payment issues, we might try a different payment method
        if (error.getErrorType() == ErrorType.BUSINESS_RULE_ERROR) {
        } else {
            completeWithFailure(error);
        }
    }
    
    private void completeWithFailure(SagaError error) {
        this.status = SagaStatus.FAILED;
        this.completedAt = Instant.now();
        this.errors.add(error);
        log.info("SAGA_FAILED; details={}", Map.of(
            "sagaId", id,
            "sagaType", sagaType,
            "completedAt", completedAt.toString(),
            "errorMessage", error.getMessage(),
            "errorType", error.getErrorType().name()
        ));
        // Compensate for completed steps
        compensate();
    }
    
    protected abstract void compensate();
    
    private Duration calculateBackoffDelay() {
        return Duration.ofSeconds((long) Math.pow(2, retryCount)); // Exponential backoff
    }
    
    public Optional<SagaError> getLastError() {
        return errors.isEmpty() ? Optional.empty() : Optional.of(errors.get(errors.size() - 1));
    }
    
    public boolean canRetry() {
        return retryCount < MAX_RETRIES && 
               getLastError().map(SagaError::isRecoverable).orElse(false);
    }

    protected void fail(String reason) {
        this.status = SagaStatus.FAILED;
        this.completedAt = Instant.now();
        log.info("SAGA_FAILED; details={}", Map.of(
            "sagaId", id,
            "sagaType", sagaType,
            "completedAt", completedAt.toString(),
            "reason", reason
        ));
        
        // Trigger compensation when saga fails
        compensate();
    }

    protected boolean hasProcessedEvent(Class<? extends SagaMessage> eventClass) {
        return processedEvents.stream().anyMatch(eventClass::isInstance);
    }

    public SagaSnapshot toSnapshot(ObjectMapper objectMapper) throws Exception {
        String sagaDataJson = objectMapper.writeValueAsString(data);
        String processedEventsJson = objectMapper.writeValueAsString(processedEvents);
        String pendingCommandsJson = objectMapper.writeValueAsString(commandsToDispatch);
        
        return new SagaSnapshot(
            id,
            sagaType,
            status,
            startedAt,
            sagaDataJson,
            processedEventsJson,
            pendingCommandsJson,
            completedAt
        );
    }
    
}
