package com.bcbs239.regtech.signal.application;

import com.bcbs239.regtech.signal.domain.SignalEvent;
import com.bcbs239.regtech.signal.infrastructure.SsePublisher;
import org.springframework.stereotype.Service;

/**
 * Simple application service that publishes signal events.
 */
@Service
public class SignalService {
    private final SsePublisher publisher;

    public SignalService(SsePublisher publisher) {
        this.publisher = publisher;
    }

    public void publish(String type, String payload) {
        SignalEvent event = new SignalEvent(type, payload);
        publisher.publish(event);
    }
}
