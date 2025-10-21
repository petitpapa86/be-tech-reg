package com.bcbs239.regtech.core.inbox;

import com.bcbs239.regtech.core.application.IntegrationEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class DefaultIntegrationEventDeserializer implements IntegrationEventDeserializer {

    private final ObjectMapper objectMapper;

    public DefaultIntegrationEventDeserializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public IntegrationEvent deserialize(String typeName, String json) throws Exception {
        Class<?> eventClass = Class.forName(typeName);
        if (!IntegrationEvent.class.isAssignableFrom(eventClass)) {
            throw new ClassNotFoundException("Event class does not implement IntegrationEvent: " + typeName);
        }
        @SuppressWarnings("unchecked")
        Class<? extends IntegrationEvent> integrationEventClass = (Class<? extends IntegrationEvent>) eventClass;
        return objectMapper.readValue(json, integrationEventClass);
    }
}

