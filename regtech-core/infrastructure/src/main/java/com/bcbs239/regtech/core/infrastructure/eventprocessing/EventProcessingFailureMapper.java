package com.bcbs239.regtech.core.infrastructure.eventprocessing;

import com.bcbs239.regtech.core.domain.eventprocessing.EventProcessingFailure;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;

/**
 * Mapper for converting between EventProcessingFailure domain objects and EventProcessingFailureEntity JPA entities.
 */
@Component
public class EventProcessingFailureMapper {

    private final ObjectMapper MAPPER;

    public EventProcessingFailureMapper(ObjectMapper mapper) {
        MAPPER = mapper;
    }


    /**
     * Convert domain EventProcessingFailure to JPA entity.
     */
    public EventProcessingFailureEntity toEntity(EventProcessingFailure domain) {
        if (domain == null) {
            return null;
        }

        EventProcessingFailureEntity entity = new EventProcessingFailureEntity();
        entity.setId(domain.getId());
        entity.setEventType(domain.getEventType());
        entity.setEventPayload(domain.getEventPayload());
        // store metadata as JSON
        try {
            entity.setMetadata(MAPPER.writeValueAsString(domain.getMetadata()));
        } catch (JsonProcessingException e) {
            // fallback to empty JSON object
            entity.setMetadata("{}");
        }
        entity.setErrorMessage(domain.getErrorMessage());
        entity.setErrorStacktrace(domain.getErrorStacktrace());
        entity.setRetryCount(domain.getRetryCount());
        entity.setMaxRetries(domain.getMaxRetries());
        entity.setNextRetryAt(domain.getNextRetryAt());
        entity.setStatus(domain.getStatus());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        entity.setLastErrorAt(domain.getLastErrorAt());

        return entity;
    }

    /**
     * Convert JPA entity to domain EventProcessingFailure.
     */
    public EventProcessingFailure toDomain(EventProcessingFailureEntity entity) {
        if (entity == null) {
            return null;
        }

        Map<String, String> metadata = Collections.emptyMap();
        if (entity.getMetadata() != null && !entity.getMetadata().isBlank()) {
            try {
                metadata = MAPPER.readValue(entity.getMetadata(), new TypeReference<Map<String, String>>(){});
            } catch (JsonProcessingException e) {
                metadata = Collections.emptyMap();
            }
        }

        return EventProcessingFailure.reconstitute(
            entity.getId(),
            entity.getEventType(),
            entity.getEventPayload(),
            metadata,
            entity.getErrorMessage(),
            entity.getErrorStacktrace(),
            entity.getRetryCount(),
            entity.getMaxRetries(),
            entity.getNextRetryAt(),
            entity.getStatus(),
            entity.getCreatedAt(),
            entity.getUpdatedAt(),
            entity.getLastErrorAt()
        );
    }
}