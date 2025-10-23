package com.bcbs239.regtech.core.saga;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.bcbs239.regtech.core.config.LoggingConfiguration;
import lombok.Getter;

@Getter
public abstract class AbstractSaga<T> {
    protected final SagaId id;
    protected final String sagaType;
    protected final Instant startedAt;
    protected SagaStatus status;
    protected Instant completedAt;
    protected final T data;
    
    // Setters for status and completedAt
    protected void setStatus(SagaStatus status) {
        this.status = status;
    }
    
    protected void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }
    
    private final List<SagaCommand> commandsToDispatch = new ArrayList<>();
    private final List<SagaMessage> processedEvents = new ArrayList<>();
    private final Map<Class<? extends SagaMessage>, Consumer<SagaMessage>> eventHandlers = new HashMap<>();

    protected AbstractSaga(SagaId id, String sagaType, T data) {
        this.id = id;
        this.sagaType = sagaType;
        this.data = data;
        this.status = SagaStatus.STARTED;
        this.startedAt = Instant.now();
    }

    // Functional handler registration - pure closure approach
    protected <E extends SagaMessage> void onEvent(Class<E> eventType, Consumer<E> handler) {
        eventHandlers.put(eventType, (SagaMessage event) -> {
            @SuppressWarnings("unchecked")
            E typedEvent = (E) event;
            handler.accept(typedEvent);
        });
        LoggingConfiguration.createStructuredLog("SAGA_EVENT_HANDLER_REGISTERED", Map.of(
            "sagaId", id,
            "sagaType", sagaType,
            "eventType", eventType.getSimpleName()
        ));
    }

    // Handle incoming events
    public void handle(SagaMessage event) {
        if (isCompleted()) {
            LoggingConfiguration.createStructuredLog("SAGA_EVENT_IGNORED", Map.of(
                "sagaId", id,
                "sagaType", sagaType,
                "eventType", event.eventType()
            ));
            return;
        }
        
        Consumer<SagaMessage> handler = eventHandlers.get(event.getClass());
        if (handler != null) {
            LoggingConfiguration.createStructuredLog("SAGA_EVENT_PROCESSED", Map.of(
                "sagaId", id,
                "sagaType", sagaType,
                "eventType", event.eventType()
            ));
            handler.accept(event);
            processedEvents.add(event);
            updateStatus();
        } else {
            LoggingConfiguration.createStructuredLog("SAGA_EVENT_IGNORED", Map.of(
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
        LoggingConfiguration.createStructuredLog("SAGA_COMMAND_DISPATCHED", Map.of(
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

    protected boolean isCompleted() {
        return status == SagaStatus.COMPLETED || status == SagaStatus.FAILED;
    }

    protected void complete() {
        this.status = SagaStatus.COMPLETED;
        this.completedAt = Instant.now();
        LoggingConfiguration.createStructuredLog("SAGA_COMPLETED", Map.of(
            "sagaId", id,
            "sagaType", sagaType,
            "completedAt", completedAt.toString()
        ));
    
    }

    protected void fail(String reason) {
        this.status = SagaStatus.FAILED;
        this.completedAt = Instant.now();
        LoggingConfiguration.createStructuredLog("SAGA_FAILED", Map.of(
            "sagaId", id,
            "sagaType", sagaType,
            "completedAt", completedAt.toString(),
            "reason", reason
        ));
    }

    protected boolean hasProcessedEvent(Class<? extends SagaMessage> eventClass) {
        return processedEvents.stream().anyMatch(eventClass::isInstance);
    }
    
}
