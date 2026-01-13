package com.bcbs239.regtech.riskcalculation.application.parameters;

import org.jspecify.annotations.NonNull;

/**
 * Query: Get Risk Parameters
 */
public record GetRiskParametersQuery(@NonNull String bankId) {
}
