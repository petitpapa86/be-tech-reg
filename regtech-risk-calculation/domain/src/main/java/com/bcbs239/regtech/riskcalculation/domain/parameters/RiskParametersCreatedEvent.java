package com.bcbs239.regtech.riskcalculation.domain.parameters;

import org.jspecify.annotations.NonNull;

import java.time.Instant;

/**
 * Domain Event: Risk Parameters Created
 */
public record RiskParametersCreatedEvent(
    @NonNull RiskParametersId parametersId,
    @NonNull String bankId,
    @NonNull String createdBy,
    @NonNull Instant occurredOn
) implements RiskParametersEvent {
}
