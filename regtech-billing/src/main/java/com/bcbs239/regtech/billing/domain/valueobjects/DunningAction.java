package com.bcbs239.regtech.billing.domain.valueobjects;

import java.time.Instant;

public record DunningAction(
    DunningActionId id,
    DunningStep step,
    String actionType,
    String actionDetails,
    boolean successful,
    Instant executedAt
) {

    public static DunningAction successful(DunningStep step, String actionType, String actionDetails) {
        return new DunningAction(
            DunningActionId.generate(),
            step,
            actionType,
            actionDetails,
            true,
            Instant.now()
        );
    }

    public static DunningAction failed(DunningStep step, String actionType, String actionDetails) {
        return new DunningAction(
            DunningActionId.generate(),
            step,
            actionType,
            actionDetails,
            false,
            Instant.now()
        );
    }

    public String details() {
        return actionDetails;
    }
}
