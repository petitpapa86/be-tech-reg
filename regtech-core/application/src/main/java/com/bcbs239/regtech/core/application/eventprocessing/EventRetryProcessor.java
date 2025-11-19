package com.bcbs239.regtech.core.application.eventprocessing;

import com.bcbs239.regtech.core.domain.eventprocessing.EventProcessingFailure;
import com.bcbs239.regtech.core.domain.eventprocessing.IEventProcessingFailureRepository;
import com.bcbs239.regtech.core.domain.events.IIntegrationEventBus;
import com.bcbs239.regtech.core.domain.events.integration.EventDeserializationFailed;
import com.bcbs239.regtech.core.domain.events.integration.EventHandlerExecutionFailed;
import com.bcbs239.regtech.core.domain.events.integration.EventHandlerInvocationFailed;
import com.bcbs239.regtech.core.domain.events.integration.EventProcessingPermanentlyFailed;
import com.bcbs239.regtech.core.domain.events.integration.EventPublishingFailed;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.core.domain.context.CorrelationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

/**
 * Scheduled processor for retrying failed event processing.
 * Attempts to reprocess events that previously failed with exponential backoff.
 */
@Component
public class EventRetryProcessor {

    private final IEventProcessingFailureRepository failureRepository;
    private final ObjectMapper objectMapper;
    private final EventRetryOptions retryOptions;
    private final ApplicationContext applicationContext;
    private final IIntegrationEventBus eventBus;
    private static final Logger log = LoggerFactory.getLogger(EventRetryProcessor.class);

    @Autowired
    public EventRetryProcessor(
            IEventProcessingFailureRepository failureRepository,
            ObjectMapper objectMapper,
            EventRetryOptions retryOptions,
            ApplicationContext applicationContext,
            IIntegrationEventBus eventBus) {
        this.failureRepository = failureRepository;
        this.objectMapper = objectMapper;
        this.retryOptions = retryOptions;
        this.applicationContext = applicationContext;
        this.eventBus = eventBus;
    }

    @Scheduled(fixedDelayString = "#{@eventRetryOptions.getInterval().toMillis()}")
    @Transactional
    public void processFailedEvents() {
        if (!retryOptions.isEnabled()) {
            return;
        }

        Result<List<EventProcessingFailure>> failuresResult = failureRepository.findEventsReadyForRetry(retryOptions.getBatchSize());

        if (failuresResult.isFailure()) {
            return;
        }

        List<EventProcessingFailure> failures = failuresResult.getValue().orElse(List.of());
        if (failures.isEmpty()) {
            return;
        }

        log.info("event retry processing; details={}", Map.of(
                "failureCount", String.valueOf(failures.size()),
                "batchSize", String.valueOf(retryOptions.getBatchSize())
        ));

        int processedCount = 0;
        for (EventProcessingFailure failure : failures) {
            try {
                // Mark as processing
                EventProcessingFailure processingFailure = failure.markAsProcessing();
                Result<EventProcessingFailure> saveResult = failureRepository.save(processingFailure);
                if (saveResult.isFailure()) {
                    continue;
                }

                // Update failure reference to the saved instance
                failure = saveResult.getValue().orElse(failure);

                // Attempt to reprocess the event
                boolean success = reprocessEvent(failure);

                if (!success) {
                    EventProcessingFailure failedFast = failure.markAsFailed(
                            "Handler invocation failed or no handler found",
                            "",
                            retryOptions.getBackoffIntervalsSeconds()
                    );
                    failureRepository.save(failedFast);

                    // Notify team about handler invocation failure
                    publishHandlerInvocationFailureNotification(failedFast);

                    // If we've exhausted retries, publish a permanent-failure event
                    if (failedFast.getRetryCount() >= retryOptions.getMaxRetries()) {
                        publishPermanentFailureEvent(failedFast);
                    }

                    log.info("event retry failed fast; details={}", Map.of(
                            "failureId", failure.getId(),
                            "eventType", failure.getEventType(),
                            "userId", failure.getUserId(),
                            "retryCount", String.valueOf(failedFast.getRetryCount())
                    ));
                    continue;
                }

                // Success path
                EventProcessingFailure succeededFailure = failure.markAsSucceeded();
                failureRepository.save(succeededFailure);
                processedCount++;

                log.info("event retry succeeded; details={}", Map.of(
                                "failureId", failure.getId(),
                                "eventType", failure.getEventType(),
                                "userId", failure.getUserId(),
                                "retryCount", String.valueOf(failure.getRetryCount())
                        )
                );

            } catch (Exception e) {
                log.error("event retry processing exception; details={}", Map.of(
                        "failureId", failure.getId(),
                        "eventType", failure.getEventType(),
                        "userId", failure.getUserId()
                ), e);
            }
        }

        log.info("event retry completed; details={}", Map.of(
                "successfulRetries", String.valueOf(processedCount)
        ));
    }

    /**
     * Reprocess the event by finding and invoking the appropriate event handler using reflection.
     */
    private boolean reprocessEvent(EventProcessingFailure failure) {
        try {
            Object event = deserializeEvent(failure);
            return invokeEventHandler(event, failure);

        } catch (Exception e) {
            // Deserialization failed - mark as failed and update retry count
            EventProcessingFailure failedFailure = failure.markAsFailed(
                    "Deserialization failed: " + e.getMessage(),
                    getStackTraceAsString(e),
                    retryOptions.getBackoffIntervalsSeconds()
            );
            failureRepository.save(failedFailure);

            // Log deserialization failure
            log.error("event retry deserialization failed; details={}", Map.of(
                    "failureId", failure.getId(),
                    "eventType", failure.getEventType(),
                    "userId", failure.getUserId(),
                    "retryCount", String.valueOf(failedFailure.getRetryCount())
            ), e);

            // Notify team about deserialization failure
            publishDeserializationFailureNotification(failedFailure, e);

            // Check if permanently failed after this retry
            if (failedFailure.getRetryCount() >= retryOptions.getMaxRetries()) {
                publishPermanentFailureEvent(failedFailure);
            }

            return false;
        }
    }

    /**
     * Deserialize the event from JSON payload based on event type.
     */
    private Object deserializeEvent(EventProcessingFailure failure) throws Exception {
        String eventType = failure.getEventType();

        Class<?> eventClass = Class.forName(eventType);

        return objectMapper.readValue(failure.getEventPayload(), eventClass);
    }

    /**
     * Find and invoke the appropriate event handler using Spring bean reflection.
     */
    private boolean invokeEventHandler(Object event, EventProcessingFailure failure) throws Exception {
        // Get all beans that might handle events
        Map<String, Object> eventHandlers = applicationContext.getBeansWithAnnotation(Component.class);

        for (Object handler : eventHandlers.values()) {
            // Look for methods annotated with @EventListener that accept the event type
            Method[] methods = handler.getClass().getMethods();

            for (Method method : methods) {
                if (!method.isAnnotationPresent(org.springframework.context.event.EventListener.class)) {
                    continue; // Fail fast: skip methods without @EventListener
                }

                Class<?>[] parameterTypes = method.getParameterTypes();

                if (!(parameterTypes.length == 1 && parameterTypes[0].isAssignableFrom(event.getClass()))) {
                    continue; // Fail fast: skip methods that can't handle the event type
                }

                try {
                    // Invoke the event handler
                    method.invoke(handler, event);

                    log.info("event retry handler invoked; details={}", Map.of(
                            "failureId", failure.getId(),
                            "eventType", failure.getEventType(),
                            "handlerClass", handler.getClass().getSimpleName(),
                            "handlerMethod", method.getName()
                    ));

                    return true;

                } catch (java.lang.reflect.InvocationTargetException e) {
                    // Unwrap the actual exception thrown by the handler
                    Throwable cause = e.getCause() != null ? e.getCause() : e;
                    
                    // Handler execution failed - mark as failed and update retry count
                    EventProcessingFailure failedFailure = failure.markAsFailed(
                            "Handler execution failed: " + cause.getMessage(),
                            getStackTraceAsString(cause),
                            retryOptions.getBackoffIntervalsSeconds()
                    );
                    failureRepository.save(failedFailure);

                    // Log handler execution failure
                    log.error("event retry handler execution failed; details={}", Map.of(
                            "failureId", failure.getId(),
                            "eventType", failure.getEventType(),
                            "handlerClass", handler.getClass().getSimpleName(),
                            "handlerMethod", method.getName(),
                            "retryCount", String.valueOf(failedFailure.getRetryCount())
                    ), cause);

                    // Notify team about handler execution failure
                    publishHandlerExecutionFailureNotification(failedFailure, handler, method, cause);

                    // Check if permanently failed after this retry
                    if (failedFailure.getRetryCount() >= retryOptions.getMaxRetries()) {
                        publishPermanentFailureEvent(failedFailure);
                    }

                    return false;
                } catch (Exception e) {
                    // Other reflection errors (shouldn't happen in normal flow)
                    EventProcessingFailure failedFailure = failure.markAsFailed(
                            "Handler invocation error: " + e.getMessage(),
                            getStackTraceAsString(e),
                            retryOptions.getBackoffIntervalsSeconds()
                    );
                    failureRepository.save(failedFailure);

                    log.error("event retry handler invocation error; details={}", Map.of(
                            "failureId", failure.getId(),
                            "eventType", failure.getEventType(),
                            "handlerClass", handler.getClass().getSimpleName(),
                            "handlerMethod", method.getName(),
                            "retryCount", String.valueOf(failedFailure.getRetryCount())
                    ), e);

                    // Notify team about handler execution failure
                    publishHandlerExecutionFailureNotification(failedFailure, handler, method, e);

                    // Check if permanently failed after this retry
                    if (failedFailure.getRetryCount() >= retryOptions.getMaxRetries()) {
                        publishPermanentFailureEvent(failedFailure);
                    }

                    return false;
                }
            }
        }

        // No suitable handler found
        log.info("event retry no handler found; details={}", Map.of(
                "failureId", failure.getId(),
                "eventType", failure.getEventType(),
                "userId", failure.getUserId()
        ));

        return false;
    }

    /**
     * Publish permanent failure event and notify team when max retries exhausted.
     */
    private void publishPermanentFailureEvent(EventProcessingFailure failure) {
        try {
            String uniqueId = getUniqueId(failure);

            eventBus.publish(new EventProcessingPermanentlyFailed(
                    failure.getId(),
                    failure.getEventType(),
                    failure.getUserId(),
                    failure.getEventPayload(),
                    failure.getRetryCount(),
                    uniqueId
            ));

            log.info("event retry permanently failed; details={}", Map.of(
                    "failureId", failure.getId(),
                    "eventType", failure.getEventType(),
                    "userId", failure.getUserId(),
                    "retryCount", String.valueOf(failure.getRetryCount()),
                    "uniqueId", uniqueId
            ));
        } catch (Exception pubEx) {
            log.error("event retry publish permanently failed; details={}", Map.of(
                    "failureId", failure.getId(),
                    "eventType", failure.getEventType(),
                    "userId", failure.getUserId()
            ), pubEx);

            // Notify team about event publishing failure
            publishEventPublishingFailureNotification(failure, pubEx);
        }
    }

    /**
     * Notify team about deserialization failure.
     */
    private void publishDeserializationFailureNotification(EventProcessingFailure failure, Exception e) {
        try {
            String uniqueId = getUniqueId(failure);

            eventBus.publish(new EventDeserializationFailed(
                    failure.getId(),
                    failure.getEventType(),
                    failure.getUserId(),
                    failure.getRetryCount(),
                    retryOptions.getMaxRetries(),
                    e.getMessage(),
                    uniqueId
            ));

            log.info("event deserialization failure notification published; details={}", Map.of(
                    "failureId", failure.getId(),
                    "eventType", failure.getEventType(),
                    "userId", failure.getUserId(),
                    "retryCount", String.valueOf(failure.getRetryCount()),
                    "uniqueId", uniqueId
            ));
        } catch (Exception notifyEx) {
            log.error("Failed to publish deserialization failure notification; details={}", Map.of(
                    "failureId", failure.getId()
            ), notifyEx);
        }
    }

    /**
     * Notify team about handler invocation failure.
     */
    private void publishHandlerInvocationFailureNotification(EventProcessingFailure failure) {
        try {
            String uniqueId = getUniqueId(failure);

            eventBus.publish(new EventHandlerInvocationFailed(
                    failure.getId(),
                    failure.getEventType(),
                    failure.getUserId(),
                    failure.getRetryCount(),
                    retryOptions.getMaxRetries(),
                    uniqueId
            ));

            log.info("event handler invocation failure notification published; details={}", Map.of(
                    "failureId", failure.getId(),
                    "eventType", failure.getEventType(),
                    "userId", failure.getUserId(),
                    "retryCount", String.valueOf(failure.getRetryCount()),
                    "uniqueId", uniqueId
            ));
        } catch (Exception notifyEx) {
            log.error("Failed to publish handler invocation failure notification; details={}", Map.of(
                    "failureId", failure.getId()
            ), notifyEx);
        }
    }

    /**
     * Notify team about handler execution failure.
     */
    private void publishHandlerExecutionFailureNotification(
            EventProcessingFailure failure,
            Object handler,
            Method method,
            Throwable e) {
        try {
            String uniqueId = getUniqueId(failure);

            eventBus.publish(new EventHandlerExecutionFailed(
                    failure.getId(),
                    failure.getEventType(),
                    failure.getUserId(),
                    handler.getClass().getSimpleName(),
                    method.getName(),
                    e.getMessage(),
                    failure.getRetryCount(),
                    retryOptions.getMaxRetries(),
                    uniqueId
            ));

            log.info("event handler execution failure notification published; details={}", Map.of(
                    "failureId", failure.getId(),
                    "eventType", failure.getEventType(),
                    "userId", failure.getUserId(),
                    "handlerClass", handler.getClass().getSimpleName(),
                    "handlerMethod", method.getName(),
                    "uniqueId", uniqueId
            ));
        } catch (Exception notifyEx) {
            log.error("Failed to publish handler execution failure notification; details={}", Map.of(
                    "failureId", failure.getId()
            ), notifyEx);
        }
    }

    /**
     * Notify team about event publishing failure.
     */
    private void publishEventPublishingFailureNotification(EventProcessingFailure failure, Exception e) {
        try {
            String uniqueId = getUniqueId(failure);

            eventBus.publish(new EventPublishingFailed(
                    failure.getId(),
                    failure.getEventType(),
                    failure.getUserId(),
                    e.getMessage(),
                    uniqueId
            ));

            log.info("event publishing failure notification published; details={}", Map.of(
                    "failureId", failure.getId(),
                    "eventType", failure.getEventType(),
                    "userId", failure.getUserId(),
                    "uniqueId", uniqueId
            ));
        } catch (Exception notifyEx) {
            log.error("Failed to publish event publishing failure notification; details={}", Map.of(
                    "failureId", failure.getId()
            ), notifyEx);
        }
    }

    /**
     * Get unique identifier - try correlation ID first, fall back to event ID.
     */
    private String getUniqueId(EventProcessingFailure failure) {
        String correlationId = CorrelationContext.correlationId();
        return (correlationId != null && !correlationId.isEmpty())
                ? correlationId
                : failure.getId();
    }

    /**
     * Convert exception stack trace to string.
     */
    private String getStackTraceAsString(Throwable e) {
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement element : e.getStackTrace()) {
            sb.append(element.toString()).append("\n");
        }
        return sb.toString();
    }
}