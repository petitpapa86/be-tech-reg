package com.bcbs239.regtech.core.infrastructure;

import com.bcbs239.regtech.core.domain.events.IIntegrationEventBus;
import com.bcbs239.regtech.core.domain.events.IntegrationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

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
    public void publishEvent(Object event) {
        boolean permitAcquired = false;
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
        Thread.ofVirtual().start(() -> {
            try {
                logger.info("📤 ASYNC Publishing cross-module event: {} with data: {}", event.getClass().getSimpleName(), event);
                eventPublisher.publishEvent(event);
                logger.debug("✅ Successfully published cross-module event: {}", event.getClass().getSimpleName());
            } catch (Exception e) {
                logger.error("❌ Failed to publish cross-module event: {} - {}", event.getClass().getSimpleName(), e.getMessage(), e);
            } finally {
                publishSemaphore.release();
            }
        });
    }

    public void publishEventSynchronously(Object event) {
        logger.info("📤 SYNC Publishing cross-module event: {} with data: {}", event.getClass().getSimpleName(), event);
        eventPublisher.publishEvent(event);
    }

    @Override
    public void publish(IntegrationEvent event) {
        // Default integration event publishing delegates to synchronous publish so consumers (Inbox) see them reliably
        publishEventSynchronously(event);
    }
}
