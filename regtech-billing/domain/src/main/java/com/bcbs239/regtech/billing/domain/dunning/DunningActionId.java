package com.bcbs239.regtech.billing.domain.dunning;



import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Result;

import java.util.UUID;

public record DunningActionId(UUID value) {

    public static Result<DunningActionId> fromString(String value) {
        try {
            UUID uuid = UUID.fromString(value);
            return Result.success(new DunningActionId(uuid));
        } catch (IllegalArgumentException e) {
            return Result.failure("INVALID_DUNNING_ACTION_ID", ErrorType.BUSINESS_RULE_ERROR, "Invalid DunningActionId format: " + value, null);
        }
    }

    public static DunningActionId generate() {
        return new DunningActionId(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return value.toString();
    }
}

