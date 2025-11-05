package com.bcbs239.regtech.core.application.eventprocessing;

public record InboxMessageDto(
    String id,
    String eventType,
    String payload,
    String status,
    String createdAt,
    String processedAt
) {
}
