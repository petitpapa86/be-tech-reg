package com.bcbs239.regtech.billing.infrastructure.messaging;

import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.core.shared.ErrorDetail;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.stereotype.Component;

/**
 * Service for serializing and deserializing domain events for the outbox pattern.
 */
@Component
public class EventSerializer {

    private final ObjectMapper objectMapper;

    public EventSerializer() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    /**
     * Serialize an event object to JSON string.
     */
    public Result<String> serialize(Object event) {
        try {
            String json = objectMapper.writeValueAsString(event);
            return Result.success(json);
        } catch (JsonProcessingException e) {
            return Result.failure(ErrorDetail.of(
                "EVENT_SERIALIZATION_FAILED",
                "Failed to serialize event: " + e.getMessage(),
                "event.serialization.failed"
            ));
        }
    }

    /**
     * Deserialize a JSON string to an event object of the specified type.
     */
    public <T> Result<T> deserialize(String json, Class<T> eventType) {
        try {
            T event = objectMapper.readValue(json, eventType);
            return Result.success(event);
        } catch (JsonProcessingException e) {
            return Result.failure(ErrorDetail.of(
                "EVENT_DESERIALIZATION_FAILED",
                "Failed to deserialize event: " + e.getMessage(),
                "event.deserialization.failed"
            ));
        }
    }

    /**
     * Deserialize a JSON string to an event object using the event type name.
     */
    public Result<Object> deserialize(String json, String eventTypeName) {
        try {
            Class<?> eventClass = Class.forName(eventTypeName);
            Object event = objectMapper.readValue(json, eventClass);
            return Result.success(event);
        } catch (ClassNotFoundException e) {
            return Result.failure(ErrorDetail.of(
                "EVENT_TYPE_NOT_FOUND",
                "Event type not found: " + eventTypeName,
                "event.type.not.found"
            ));
        } catch (JsonProcessingException e) {
            return Result.failure(ErrorDetail.of(
                "EVENT_DESERIALIZATION_FAILED",
                "Failed to deserialize event: " + e.getMessage(),
                "event.deserialization.failed"
            ));
        }
    }

    /**
     * Get the simple class name for an event type.
     */
    public String getEventTypeName(Object event) {
        return event.getClass().getSimpleName();
    }

    /**
     * Get the full class name for an event type.
     */
    public String getFullEventTypeName(Object event) {
        return event.getClass().getName();
    }
}
