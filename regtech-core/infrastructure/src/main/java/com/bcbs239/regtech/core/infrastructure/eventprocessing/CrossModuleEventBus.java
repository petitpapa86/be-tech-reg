package com.bcbs239.regtech.core.infrastructure.eventprocessing;

import com.bcbs239.regtech.core.domain.context.CorrelationContext;
import com.bcbs239.regtech.core.domain.events.IIntegrationEventBus;
import com.bcbs239.regtech.core.domain.events.IntegrationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

@Component
public class CrossModuleEventBus implements IIntegrationEventBus, InitializingBean {

    private static final Logger logger = LoggerFactory.getLogger(CrossModuleEventBus.class);

    private final ApplicationEventPublisher eventPublisher;

    // Capacity configuration (architecture-level concern)
    @Value("${events.cross-module.max-concurrent-publishes:50}")
    private int maxConcurrentPublishes;

    @Value("${events.cross-module.acquire-timeout-ms:2000}")
    private long acquireTimeoutMs;

    private Semaphore publishSemaphore;

    public CrossModuleEventBus(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void afterPropertiesSet() {
        // Initialize a semaphore to bound concurrent async publish operations
        int permits = Math.max(1, maxConcurrentPublishes);
        this.publishSemaphore = new Semaphore(permits);
        logger.info("CrossModuleEventBus initialized with maxConcurrentPublishes={}", permits);
    }

    /**
     * Asynchronously publish an event using a virtual thread but apply capacity controls
     * so the system does not spawn an unbounded number of threads when under load.
     * If a permit cannot be acquired within the configured timeout, the method falls back
     * to synchronous publishing to avoid dropping events.
     */
    public void publishEvent(IntegrationEvent event) {
        boolean permitAcquired;
        try {
            permitAcquired = publishSemaphore.tryAcquire(acquireTimeoutMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("Interrupted while trying to acquire publish permit, will publish synchronously");
            publishEventSynchronously(event);
            return;
        }

        if (!permitAcquired) {
            logger.warn("Could not acquire publish permit within {} ms (maxConcurrentPublishes={}), falling back to synchronous publish for event {}",
                    acquireTimeoutMs, maxConcurrentPublishes, event.getClass().getSimpleName());
            publishEventSynchronously(event);
            return;
        }

        // We acquired a permit; publish in a virtual thread and release permit when done
        // Capture the current ScopedValue context to propagate to the virtual thread
        String correlationId = CorrelationContext.correlationId();
        String causationId = CorrelationContext.causationId();
        String boundedContext = CorrelationContext.boundedContext();
        boolean isOutboxReplay = CorrelationContext.isOutboxReplay();
        boolean isInboxReplay = CorrelationContext.isInboxReplay();
        
        Thread.ofVirtual().start(() -> {
            try {
                // Re-establish the ScopedValue context in the virtual thread
                var scope = java.lang.ScopedValue.where(CorrelationContext.CORRELATION_ID, correlationId)
                        .where(CorrelationContext.CAUSATION_ID, causationId)
                        .where(CorrelationContext.BOUNDED_CONTEXT, boundedContext)
                        .where(CorrelationContext.OUTBOX_REPLAY, isOutboxReplay)
                        .where(CorrelationContext.INBOX_REPLAY, isInboxReplay);
                
                scope.run(() -> {
                    logger.debug("Publishing cross-module event in virtual thread: {} (correlationId={}, isOutboxReplay={}, isInboxReplay={})", 
                            event.getClass().getSimpleName(), correlationId, isOutboxReplay, isInboxReplay);
                    eventPublisher.publishEvent(event);
                    logger.debug("Successfully published cross-module event: {}", event.getClass().getSimpleName());
                });
            } catch (Exception e) {
                logger.error("❌ Failed to publish cross-module event: {} - {}", event.getClass().getSimpleName(), e.getMessage(), e);
            } finally {
                publishSemaphore.release();
            }
        });
    }

    public void publishEventSynchronously(Object event) {
        logger.debug("Publishing cross-module event synchronously: {}", event.getClass().getSimpleName());
        eventPublisher.publishEvent(event);
    }

    @Override
    public void publish(IntegrationEvent event) {
        // If a transaction is active, publish synchronously in the same thread so
        // @TransactionalEventListener handlers can participate in the transaction lifecycle.
        // If we publish on another thread (virtual/async), transactional listeners won't fire.
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            publishEventSynchronously(event);
            return;
        }

        // No transaction active: publish asynchronously with capacity controls.
        publishEvent(event);
    }
}

