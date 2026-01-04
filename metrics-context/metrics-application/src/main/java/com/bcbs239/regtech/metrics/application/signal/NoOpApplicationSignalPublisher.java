package com.bcbs239.regtech.metrics.application.signal;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnMissingBean(ApplicationSignalPublisher.class)
public class NoOpApplicationSignalPublisher implements ApplicationSignalPublisher {
    @Override
    public void publish(ApplicationSignal signal) {
        // Intentionally no-op. Overridden by infrastructure in production.
    }
}
