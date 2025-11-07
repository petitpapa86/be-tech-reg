package com.bcbs239.regtech.core.domain.inbox;

import com.bcbs239.regtech.core.domain.events.DomainEvent;
import com.bcbs239.regtech.core.domain.events.IntegrationEvent;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Builder
public class InboxMessage {
    private String id;

    private String eventType;

    private String content;

    private InboxMessageStatus status;

    private Instant occurredOnUtc;

    private Instant processedOnUtc;

    private int retryCount = 0;

    private Instant nextRetryTime;

    private String lastError;

    private Instant deadLetterTime;

    private Instant updatedAt;

    private String correlationId;

    private String causationId;

    public static InboxMessage fromIntegrationEvent(IntegrationEvent event, ObjectMapper mapper) {
        return new InboxMessageBuilder()
                .withId(event.getEventId())
                .withEventType(event.eventType())
                .withContent(serializeEventContent(event, mapper))
                .withStatus(InboxMessageStatus.PENDING)
                .withOccurredOnUtc(Instant.now())
                .withUpdatedAt(Instant.now())
                .withCorrelationId(event.getCorrelationId())
                .withCausationId(event.getCausationId().orElse(null))
                .build();
    }

    private static String serializeEventContent(IntegrationEvent event, ObjectMapper mapper) {
        try {
            return mapper.writeValueAsString(event);
        } catch (JacksonException e) {
            throw new RuntimeException("Failed to serialize DomainEvent: " + event.getClass().getSimpleName(), e);
        }
    }
}

