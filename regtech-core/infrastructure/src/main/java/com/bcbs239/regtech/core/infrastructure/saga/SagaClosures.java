package com.bcbs239.regtech.core.infrastructure.saga;

import com.bcbs239.regtech.core.domain.saga.TimeoutScheduler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class SagaClosures {

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

}

