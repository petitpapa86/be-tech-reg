package com.bcbs239.regtech.riskcalculation.application.parameters;

import org.jspecify.annotations.NonNull;

/**
 * Command: Reset Risk Parameters to Default
 */
public record ResetRiskParametersCommand(
    @NonNull String bankId,
    @NonNull String modifiedBy
) {
}
