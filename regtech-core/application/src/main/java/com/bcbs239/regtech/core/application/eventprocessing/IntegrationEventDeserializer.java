package com.bcbs239.regtech.core.application.eventprocessing;

import com.bcbs239.regtech.core.domain.core.Result;
import com.bcbs239.regtech.core.domain.events.BaseEvent;
import com.bcbs239.regtech.core.domain.events.IntegrationEvent;
import com.bcbs239.regtech.core.domain.errorhandling.ErrorDetail;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class IntegrationEventDeserializer {

    private final ObjectMapper objectMapper;

    public IntegrationEventDeserializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Result<BaseEvent> deserialize(String typeName, String json)  {
        Class<?> eventClass;
        try {
            eventClass = Class.forName(typeName);
        } catch (ClassNotFoundException e) {
            return Result.failure(ErrorDetail.of("EVENT_CLASS_NOT_FOUND", "Event class not found: " + typeName));
        }
        if (!IntegrationEvent.class.isAssignableFrom(eventClass)) {
            return Result.failure(ErrorDetail.of("INVALID_EVENT_TYPE", "Event class does not implement IntegrationEvent: " + typeName));
        }
        @SuppressWarnings("unchecked")
        Class<? extends IntegrationEvent> integrationEventClass = (Class<? extends IntegrationEvent>) eventClass;
        try {
            IntegrationEvent event = objectMapper.readValue(json, integrationEventClass);
            BaseEvent baseEvent = objectMapper.convertValue(event, BaseEvent.class);

            return Result.success(baseEvent);
        } catch (JsonProcessingException e) {
            return Result.failure(ErrorDetail.of("DESERIALIZATION_ERROR", "Failed to deserialize event JSON: " + e.getMessage()));
        }
    }
}
