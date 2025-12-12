package com.bcbs239.regtech.core.domain.inbox;

import com.bcbs239.regtech.core.domain.events.IntegrationEvent;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InboxMessage {
    private String id;

    private String eventType;

    private String content;

    private InboxMessageStatus status;

    private Instant occurredOnUtc;

    private Instant processedOnUtc;

    @Builder.Default
    private int retryCount = 0;

    private Instant nextRetryTime;

    private String lastError;

    private Instant deadLetterTime;

    private Instant updatedAt;

    private String correlationId;

    private String causationId;

    public static InboxMessage fromIntegrationEvent(IntegrationEvent event, ObjectMapper mapper) {
        return InboxMessage.builder()
                .id(event.getEventId())
                .eventType(event.getClass().getName()) // Store fully qualified class name for deserialization
                .content(serializeEventContent(event, mapper))
                .status(InboxMessageStatus.PENDING)
                .occurredOnUtc(Instant.now())
                .updatedAt(Instant.now())
                .correlationId(event.getCorrelationId())
               // .causationId(event.getCausationId().orElse(""))
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

