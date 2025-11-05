package com.bcbs239.regtech.core.infrastructure.saga;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class SagaClosures {

    public static interface TimeoutScheduler {
        void schedule(String key, long delayMillis, Runnable task);
        void cancel(String key);
    }

    @FunctionalInterface
    public static interface MessagePublisher {
        void publish(SagaMessage message);
    }

    @FunctionalInterface
    public static interface Logger {
        void log(String level, String message, Object... args);
    }

    public static TimeoutScheduler timeoutScheduler(ScheduledExecutorService executor) {
        Map<String, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();
        return new TimeoutScheduler() {
            @Override
            public void schedule(String key, long delayMillis, Runnable task) {
                ScheduledFuture<?> future = executor.schedule(task, delayMillis, TimeUnit.MILLISECONDS);
                scheduledTasks.put(key, future);
            }
            @Override
            public void cancel(String key) {
                ScheduledFuture<?> future = scheduledTasks.remove(key);
                if (future != null) {
                    future.cancel(false);
                }
            }
        };
    }

    public static MessagePublisher messagePublisher(EventPublisher publisher) {
        return publisher::publish;
    }

    public static Logger logger(org.slf4j.Logger slf4jLogger) {
        return (level, message, args) -> {
            switch (level) {
                case "warn" -> slf4jLogger.warn(message, args);
                case "error" -> slf4jLogger.error(message, args);
                case "info" -> slf4jLogger.info(message, args);
                case "debug" -> slf4jLogger.debug(message, args);
                default -> slf4jLogger.info(message, args);
            }
        };
    }

    public interface EventPublisher {
        void publish(SagaMessage message);
    }
}
