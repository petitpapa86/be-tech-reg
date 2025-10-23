package com.bcbs239.regtech.core.saga;

import java.time.Instant;
import java.util.Map;

public record SagaCommand(SagaId sagaId, String commandType, Map<String, Object> payload, Instant createdAt) {

}
