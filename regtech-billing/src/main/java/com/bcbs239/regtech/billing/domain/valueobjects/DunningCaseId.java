package com.bcbs239.regtech.billing.domain.valueobjects;

import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.core.shared.ErrorDetail;

import java.util.UUID;

public record DunningCaseId(UUID value) {

    public static Result<DunningCaseId> fromString(String value) {
        try {
            UUID uuid = UUID.fromString(value);
            return Result.success(new DunningCaseId(uuid));
        } catch (IllegalArgumentException e) {
            return Result.failure(ErrorDetail.of("Invalid DunningCaseId format: " + value));
        }
    }

    public static DunningCaseId generate() {
        return new DunningCaseId(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
