package com.bcbs239.regtech.billing.domain.dunning;

import com.bcbs239.regtech.core.shared.ErrorDetail;
import com.bcbs239.regtech.core.shared.Result;

import java.util.UUID;

public record DunningActionId(UUID value) {

    public static Result<DunningActionId> fromString(String value) {
        try {
            UUID uuid = UUID.fromString(value);
            return Result.success(new DunningActionId(uuid));
        } catch (IllegalArgumentException e) {
            return Result.failure(ErrorDetail.of("Invalid DunningActionId format: " + value));
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

