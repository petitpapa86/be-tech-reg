package com.bcbs239.regtech.core.infrastructure.eventprocessing;

import com.bcbs239.regtech.core.domain.events.DomainEvent;
import com.bcbs239.regtech.core.domain.context.CorrelationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class DomainEventBus implements com.bcbs239.regtech.core.domain.events.DomainEventBus {

    private final ApplicationEventPublisher delegate;

    public DomainEventBus(ApplicationEventPublisher delegate) {
        this.delegate = delegate;
    }

//    @Override
//    public boolean publish(DomainEvent event) {
//    java.lang.ScopedValue.where(CorrelationContext.CORRELATION_ID, event.getCorrelationId())
//           .where(CorrelationContext.CAUSATION_ID, event.getEventId()) // This event becomes the causation for subsequent events
//           .run(() -> delegate.publishEvent(event));
//        return true;
//    }

    @Override
    public void publishAsReplay(DomainEvent event) {
    java.lang.ScopedValue.where(CorrelationContext.OUTBOX_REPLAY, Boolean.TRUE)
           .where(CorrelationContext.CORRELATION_ID, event.getCorrelationId())
           .where(CorrelationContext.CAUSATION_ID, event.getEventId())
           .run(() -> delegate.publishEvent(event));
    }

    @Override
    public void publishFromInbox(DomainEvent event) {
    java.lang.ScopedValue.where(CorrelationContext.INBOX_REPLAY, Boolean.TRUE)
           .where(CorrelationContext.OUTBOX_REPLAY, Boolean.FALSE) // Explicitly clear outbox replay flag
           .where(CorrelationContext.CORRELATION_ID, event.getCorrelationId())
           .where(CorrelationContext.CAUSATION_ID, event.getEventId())
           .run(() -> delegate.publishEvent(event));
    }
}