package com.bcbs239.regtech.core.infrastructure.saga;

import java.util.HashMap;
import java.util.Map;

/**
 * Base class for saga data objects.
 * Saga data holds the state information for a saga instance.
 */
public abstract class SagaData {

    private String id;
    private String correlationId;
    private final Map<String, String> metadata = new HashMap<>();

    /**
     * Get the saga ID.
     */
    public String getId() {
        return id;
    }

    /**
     * Set the saga ID.
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Get the correlation ID.
     */
    public String getCorrelationId() {
        return correlationId;
    }

    /**
     * Set the correlation ID.
     */
    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    /**
     * Get metadata value by key.
     */
    public String getMetadata(String key) {
        return metadata.get(key);
    }

    /**
     * Add metadata key-value pair.
     */
    public void addMetadata(String key, String value) {
        this.metadata.put(key, value);
    }

    /**
     * Get all metadata.
     */
    public Map<String, String> getMetadata() {
        return new HashMap<>(metadata);
    }
}
