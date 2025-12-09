package com.bcbs239.regtech.core.infrastructure.saga;

import com.bcbs239.regtech.core.domain.saga.TimeoutScheduler;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Infrastructure service that implements the TimeoutScheduler domain interface.
 * Provides timeout scheduling functionality using ScheduledExecutorService.
 */
@Service
public class TimeoutSchedulerService implements TimeoutScheduler {

    private final ScheduledExecutorService executor;
    private final Map<String, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    public TimeoutSchedulerService(ScheduledExecutorService executor) {
        this.executor = executor;
    }

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
}

