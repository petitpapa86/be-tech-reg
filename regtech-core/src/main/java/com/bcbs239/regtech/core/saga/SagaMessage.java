package com.bcbs239.regtech.core.saga;

import java.time.Instant;
import java.util.Map;

/**
 * Message exchanged between saga participants in different bounded contexts.
 * Supports both commands (requests for action) and events (notifications of state changes).
 */
public class SagaMessage implements Message {

    private final String sagaId;
    private final String messageId;
    private final String type;
    private final String source;
    private final String target;
    private final Object payload;
    private final Map<String, String> headers;
    private final Instant timestamp;

    private SagaMessage(Builder builder) {
        this.sagaId = builder.sagaId;
        this.messageId = builder.messageId;
        this.type = builder.type;
        this.source = builder.source;
        this.target = builder.target;
        this.payload = builder.payload;
        this.headers = builder.headers;
        this.timestamp = builder.timestamp != null ? builder.timestamp : Instant.now();
    }

    // Getters
    public String getSagaId() { return sagaId; }
    public String getMessageId() { return messageId; }
    public String getType() { return type; }
    public String getSource() { return source; }
    public String getTarget() { return target; }
    public Object getPayload() { return payload; }
    public Map<String, String> getHeaders() { return headers; }
    public Instant getTimestamp() { return timestamp; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String sagaId;
        private String messageId;
        private String type;
        private String source;
        private String target;
        private Object payload;
        private Map<String, String> headers;
        private Instant timestamp;

        public Builder sagaId(String sagaId) {
            this.sagaId = sagaId;
            return this;
        }

        public Builder messageId(String messageId) {
            this.messageId = messageId;
            return this;
        }

        public Builder type(String type) {
            this.type = type;
            return this;
        }

        public Builder source(String source) {
            this.source = source;
            return this;
        }

        public Builder target(String target) {
            this.target = target;
            return this;
        }

        public Builder payload(Object payload) {
            this.payload = payload;
            return this;
        }

        public Builder headers(Map<String, String> headers) {
            this.headers = headers;
            return this;
        }

        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public SagaMessage build() {
            if (sagaId == null || type == null) {
                throw new IllegalArgumentException("sagaId and type are required");
            }
            return new SagaMessage(this);
        }
    }

    // Message interface implementation
    @Override
    public String getCorrelationId() {
        return sagaId;
    }

    // getMessageId() is already implemented above

    @Override
    public String toString() {
        return String.format("SagaMessage{sagaId='%s', type='%s', source='%s', target='%s'}",
                sagaId, type, source, target);
    }
}