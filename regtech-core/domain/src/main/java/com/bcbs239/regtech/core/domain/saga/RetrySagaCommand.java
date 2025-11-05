package com.bcbs239.regtech.core.domain.saga;

import java.time.Instant;

import java.util.Map;

public class RetrySagaCommand extends SagaCommand {
    public RetrySagaCommand(SagaId sagaId) {
        super(sagaId, "RETRY_SAGA", Map.of("sagaId", sagaId.toString()), Instant.now());
    }
}
