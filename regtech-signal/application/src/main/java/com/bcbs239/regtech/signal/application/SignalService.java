package com.bcbs239.regtech.signal.application;

import com.bcbs239.regtech.signal.domain.SignalEvent;
import com.bcbs239.regtech.signal.infrastructure.SsePublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Simple application service that publishes signal events.
 */
@Service
public class SignalService {
    private static final Logger log = LoggerFactory.getLogger(SignalService.class);
    private final SsePublisher publisher;

    public SignalService(SsePublisher publisher) {
        this.publisher = publisher;
    }

    public void publish(String type, String payload) {
        log.info("SignalService.publish called: type={}", type);
        SignalEvent event = new SignalEvent(type, payload);
        publisher.publish(event);
        log.info("SignalService.publish completed: type={}, eventId={}", type, event.getId());
    }
}
