package com.bcbs239.regtech.core.application.eventprocessing;

import com.bcbs239.regtech.core.domain.events.DomainEvent;
import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class CoreIntegrationEventDeserializer {

    private final ObjectMapper objectMapper;

    public CoreIntegrationEventDeserializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Result<DomainEvent> deserialize(String typeName, String json) {
        String modifiedTypeName = typeName.replace("IntegrationEvent", "InboundEvent");
        Class<?> eventClass;
        try {
            eventClass = Class.forName(modifiedTypeName);
        } catch (ClassNotFoundException e) {
            return Result.failure(ErrorDetail.of(
                    "EVENT_CLASS_NOT_FOUND",
                    ErrorType.SYSTEM_ERROR,
                    "Event class not found: " + typeName,
                    "event.class.not.found"
            ));
        }

        if (!DomainEvent.class.isAssignableFrom(eventClass)) {
            return Result.failure(ErrorDetail.of(
                    "INVALID_EVENT_TYPE",
                    ErrorType.BUSINESS_RULE_ERROR,
                    "Event class does not extend DomainEvent: " + typeName,
                    "event.invalid.type"
            ));
        }

        try {
            @SuppressWarnings("unchecked")
            Class<? extends DomainEvent> domainEventClass =
                    (Class<? extends DomainEvent>) eventClass;

            DomainEvent event = objectMapper.readValue(json, domainEventClass);
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
