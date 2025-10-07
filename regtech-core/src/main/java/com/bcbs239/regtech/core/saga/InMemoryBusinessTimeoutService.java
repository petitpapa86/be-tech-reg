package com.bcbs239.regtech.core.saga;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.*;

/**
 * In-memory business timeout service implementation.
 * Manages scheduled timeouts for saga business processes.
 */
@Service
public class InMemoryBusinessTimeoutService implements BusinessTimeoutService {

    private static final Logger logger = LoggerFactory.getLogger(InMemoryBusinessTimeoutService.class);

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(5);
    private final Map<String, ScheduledFuture<?>> activeTimeouts = new ConcurrentHashMap<>();
    private final Map<String, Instant> timeoutStartTimes = new ConcurrentHashMap<>();

    @Override
    public void scheduleTimeout(String sagaId, String timeoutType, Duration duration, Runnable callback) {
        String timeoutKey = createTimeoutKey(sagaId, timeoutType);

        // Cancel existing timeout if present
        cancelTimeout(sagaId, timeoutType);

        logger.debug("Scheduling timeout - Saga: {}, Type: {}, Duration: {}ms",
                sagaId, timeoutType, duration.toMillis());

        ScheduledFuture<?> future = scheduler.schedule(() -> {
            try {
                logger.warn("Timeout triggered - Saga: {}, Type: {}", sagaId, timeoutType);
                activeTimeouts.remove(timeoutKey);
                timeoutStartTimes.remove(timeoutKey);
                callback.run();
            } catch (Exception e) {
                logger.error("Error executing timeout callback for saga: {}", sagaId, e);
            }
        }, duration.toMillis(), TimeUnit.MILLISECONDS);

        activeTimeouts.put(timeoutKey, future);
        timeoutStartTimes.put(timeoutKey, Instant.now());
    }

    @Override
    public void cancelTimeout(String sagaId, String timeoutType) {
        String timeoutKey = createTimeoutKey(sagaId, timeoutType);
        ScheduledFuture<?> future = activeTimeouts.remove(timeoutKey);
        timeoutStartTimes.remove(timeoutKey);

        if (future != null && !future.isDone()) {
            future.cancel(false);
            logger.debug("Timeout cancelled - Saga: {}, Type: {}", sagaId, timeoutType);
        }
    }

    @Override
    public void extendTimeout(String sagaId, String timeoutType, Duration additionalDuration) {
        String timeoutKey = createTimeoutKey(sagaId, timeoutType);
        Instant startTime = timeoutStartTimes.get(timeoutKey);

        if (startTime == null) {
            logger.warn("Cannot extend non-existent timeout - Saga: {}, Type: {}", sagaId, timeoutType);
            return;
        }

        Duration elapsed = Duration.between(startTime, Instant.now());
        Duration remaining = additionalDuration.minus(elapsed);

        if (remaining.isNegative() || remaining.isZero()) {
            // Timeout already expired or will expire immediately
            handleTimeout(sagaId, timeoutType);
        } else {
            // Reschedule with remaining time
            cancelTimeout(sagaId, timeoutType);
            scheduleTimeout(sagaId, timeoutType, remaining, () -> handleTimeout(sagaId, timeoutType));
        }
    }

    @Override
    public boolean isTimeoutActive(String sagaId, String timeoutType) {
        String timeoutKey = createTimeoutKey(sagaId, timeoutType);
        ScheduledFuture<?> future = activeTimeouts.get(timeoutKey);
        return future != null && !future.isDone();
    }

    @Override
    public Duration getRemainingTime(String sagaId, String timeoutType) {
        String timeoutKey = createTimeoutKey(sagaId, timeoutType);
        Instant startTime = timeoutStartTimes.get(timeoutKey);

        if (startTime == null) {
            return Duration.ZERO;
        }

        // This is a simplified implementation. In practice, you'd need to track the original duration
        // For now, return a default duration
        return Duration.ofMinutes(30); // Default assumption
    }

    @Override
    public void handleTimeout(String sagaId, String timeoutType) {
        logger.warn("Handling timeout - Saga: {}, Type: {}", sagaId, timeoutType);
        // This would typically notify the SagaOrchestrator
        // For now, just log the timeout
    }

    private String createTimeoutKey(String sagaId, String timeoutType) {
        return sagaId + ":" + timeoutType;
    }

    /**
     * Cleanup method to be called during application shutdown
     */
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}