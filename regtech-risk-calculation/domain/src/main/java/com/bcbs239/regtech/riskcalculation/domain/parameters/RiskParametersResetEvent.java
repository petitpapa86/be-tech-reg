package com.bcbs239.regtech.riskcalculation.domain.parameters;

import org.jspecify.annotations.NonNull;

import java.time.Instant;

/**
 * Domain Event: Risk Parameters Reset
 */
public record RiskParametersResetEvent(
    @NonNull RiskParametersId parametersId,
    @NonNull String bankId,
    @NonNull String modifiedBy,
    @NonNull Instant occurredOn
) implements RiskParametersEvent {
}
