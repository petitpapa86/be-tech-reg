package com.bcbs239.regtech.core.application.eventprocessing;

import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.events.DomainEvent;
import com.bcbs239.regtech.core.domain.events.IntegrationEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component("coreIntegrationEventDeserializer")
public class CoreIntegrationEventDeserializer {

    private final ObjectMapper objectMapper;

    public CoreIntegrationEventDeserializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Result<DomainEvent> deserialize(String typeName, String json) {
        Class<?> eventClass;
        try {
            eventClass = Class.forName(typeName);
        } catch (ClassNotFoundException e) {
            return Result.failure(ErrorDetail.of(
                    "EVENT_CLASS_NOT_FOUND",
                    ErrorType.SYSTEM_ERROR,
                    "Event class not found: " + typeName,
                    "event.class.not.found"
            ));
        }

        if (!IntegrationEvent.class.isAssignableFrom(eventClass)) {
            return Result.failure(ErrorDetail.of(
                    "INVALID_EVENT_TYPE",
                    ErrorType.BUSINESS_RULE_ERROR,
                    "Event class does not extend IntegrationEvent: " + typeName,
                    "event.invalid.type"
            ));
        }

        try {
            @SuppressWarnings("unchecked")
            Class<? extends IntegrationEvent> integrationEventClass =
                    (Class<? extends IntegrationEvent>) eventClass;

            IntegrationEvent event = objectMapper.readValue(json, integrationEventClass);
            // IntegrationEvent IS a DomainEvent
            return Result.success(event);

        } catch (JsonProcessingException e) {
            return Result.failure(ErrorDetail.of(
                    "DESERIALIZATION_ERROR",
                    ErrorType.SYSTEM_ERROR,
                    "Failed to deserialize event JSON: " + e.getMessage(),
                    "event.deserialization.failed"
            ));
        }
    }
}

