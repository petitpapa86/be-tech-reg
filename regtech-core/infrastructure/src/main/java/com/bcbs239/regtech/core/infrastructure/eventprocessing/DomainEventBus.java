package com.bcbs239.regtech.core.infrastructure.eventprocessing;

import com.bcbs239.regtech.core.domain.events.DomainEvent;
import com.bcbs239.regtech.core.infrastructure.context.CorrelationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.lang.ScopedValue;

import static com.bcbs239.regtech.core.infrastructure.context.CorrelationContext.*;

@Component
public class DomainEventBus {

    private final ApplicationEventPublisher delegate;

    public DomainEventBus(ApplicationEventPublisher delegate) {
        this.delegate = delegate;
    }

    public void publish(DomainEvent event) {
        ScopedValue.where(CORRELATION_ID, event.getCorrelationId())
                   .where(CAUSATION_ID, event.getEventId()) // This event becomes the causation for subsequent events
                   .run(() -> delegate.publishEvent(event));
    }
}