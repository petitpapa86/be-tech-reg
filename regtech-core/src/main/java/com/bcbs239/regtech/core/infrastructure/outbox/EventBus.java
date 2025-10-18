package com.bcbs239.regtech.core.infrastructure.outbox;

import com.bcbs239.regtech.core.events.DomainEvent;
import com.bcbs239.regtech.core.shared.Result;

/**
 * Functional interface for event publishing using closures.
 * Supports publishing single events or arrays of events.
 */
@FunctionalInterface
public interface EventBus {

    /**
     * Publish a single domain event.
     */
    Result<Void> publish(DomainEvent event);

    /**
     * Publish multiple events. Default implementation publishes each individually.
     */
    default Result<Void> publishAll(DomainEvent[] events) {
        for (DomainEvent event : events) {
            Result<Void> result = publish(event);
            if (result.isFailure()) {
                return result; // Fail fast on first error
            }
        }
        return Result.success(null);
    }
}