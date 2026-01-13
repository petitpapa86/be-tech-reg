package com.bcbs239.regtech.riskcalculation.domain.parameters;

import org.jspecify.annotations.NonNull;

import java.time.Instant;

/**
 * Base interface for Risk Parameters domain events
 */
public sealed interface RiskParametersEvent permits 
    RiskParametersCreatedEvent,
    RiskParametersUpdatedEvent,
    RiskParametersResetEvent {
    
    @NonNull
    RiskParametersId parametersId();
    
    @NonNull
    String bankId();
    
    @NonNull
    Instant occurredOn();
}
