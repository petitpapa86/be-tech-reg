package com.bcbs239.regtech.billing.domain.dunning;


import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Result;

import java.util.UUID;

public record DunningCaseId(UUID value) {

    public static Result<DunningCaseId> fromString(String value) {
        try {
            UUID uuid = UUID.fromString(value);
            return Result.success(new DunningCaseId(uuid));
        } catch (IllegalArgumentException e) {
            return Result.failure(ErrorDetail.of("INVALID_DUNNING_CASE_ID", ErrorType.BUSINESS_RULE_ERROR, "Invalid DunningCaseId format: " + value, "dunning.case.id.invalid"));
        }
    }

    public static DunningCaseId generate() {
        return new DunningCaseId(UUID.randomUUID());
    }

}

