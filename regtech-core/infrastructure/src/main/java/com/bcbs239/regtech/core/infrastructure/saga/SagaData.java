package com.bcbs239.regtech.core.infrastructure.saga;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

/**
 * Base class for saga data objects.
 * Saga data holds the state information for a saga instance.
 */
@Getter
@Setter
public abstract class SagaData {

    private String id;
    private String correlationId;
    private final Map<String, String> metadata = new HashMap<>();
}
