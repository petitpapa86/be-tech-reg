package com.bcbs239.regtech.signal.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Simple domain event representing a signal message.
 */
public final class SignalEvent {
    private final String id;
    private final String type;
    private final String payload;
    private final Instant occurredOn;

    public SignalEvent(String type, String payload) {
        this.id = UUID.randomUUID().toString();
        this.type = type;
        this.payload = payload;
        this.occurredOn = Instant.now();
    }

    public String getId() { return id; }
    public String getType() { return type; }
    public String getPayload() { return payload; }
    public Instant getOccurredOn() { return occurredOn; }
}
