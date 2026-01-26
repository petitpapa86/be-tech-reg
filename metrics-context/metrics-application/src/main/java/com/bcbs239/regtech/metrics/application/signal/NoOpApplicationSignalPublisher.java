package com.bcbs239.regtech.metrics.application.signal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnMissingBean(ApplicationSignalPublisher.class)
public class NoOpApplicationSignalPublisher implements ApplicationSignalPublisher {

    private static final Logger log = LoggerFactory.getLogger(NoOpApplicationSignalPublisher.class);

    @Autowired(required = false)
    private com.bcbs239.regtech.signal.application.SignalService signalService;

    @Override
    public void publish(ApplicationSignal signal) {
        if (signalService != null) {
            try {
                log.debug("Publishing application signal via SignalService: type={}, level={}",
                    signal.type(), signal.level());

                // Convert ApplicationSignal to SignalService format
                String payload = createPayload(signal);
                signalService.publish(signal.type(), payload);

                log.debug("Successfully published application signal: {}", signal.type());
            } catch (Exception e) {
                log.error("Failed to publish application signal via SignalService: {}", signal.type(), e);
                // Fall back to no-op
            }
        } else {
            // Intentionally no-op when SignalService is not available
            log.debug("SignalService not available, no-op for signal: {}", signal.type());
        }
    }

    private String createPayload(ApplicationSignal signal) {
        // Create a simple JSON payload with signal details
        return String.format("{\"type\":\"%s\",\"level\":\"%s\",\"timestamp\":\"%s\"}",
            signal.type(),
            signal.level(),
            java.time.Instant.now().toString()
        );
    }
}
