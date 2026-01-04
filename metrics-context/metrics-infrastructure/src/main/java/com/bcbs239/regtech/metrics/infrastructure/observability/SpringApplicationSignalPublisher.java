package com.bcbs239.regtech.metrics.infrastructure.observability;

import com.bcbs239.regtech.metrics.application.signal.ApplicationSignal;
import com.bcbs239.regtech.metrics.application.signal.ApplicationSignalPublisher;
import org.springframework.context.ApplicationEventPublisher;

/**
 * Infrastructure adapter that turns application-layer semantic signals into Spring events.
 *
 * Observability (logging/metrics/tracing) should be implemented by infrastructure observers.
 */
public class SpringApplicationSignalPublisher implements ApplicationSignalPublisher {

    private final ApplicationEventPublisher eventPublisher;

    public SpringApplicationSignalPublisher(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void publish(ApplicationSignal signal) {
        if (signal == null) {
            return;
        }
        eventPublisher.publishEvent(signal);
    }
}
