package com.bcbs239.regtech.billing.domain.dunning;

import java.time.Period;

public enum DunningStep {
    FIRST_REMINDER(Period.ofDays(7)),
    SECOND_REMINDER(Period.ofDays(14)),
    FINAL_NOTICE(Period.ofDays(30)),
    COLLECTION_AGENCY(Period.ofDays(60)),
    LEGAL_ACTION(Period.ofDays(90));

    private final Period delayFromPrevious;

    DunningStep(Period delayFromPrevious) {
        this.delayFromPrevious = delayFromPrevious;
    }

    public static DunningStep getFirstStep() {
        return FIRST_REMINDER;
    }

    public DunningStep getNextStep() {
        switch (this) {
            case FIRST_REMINDER:
                return SECOND_REMINDER;
            case SECOND_REMINDER:
                return FINAL_NOTICE;
            case FINAL_NOTICE:
                return COLLECTION_AGENCY;
            case COLLECTION_AGENCY:
                return LEGAL_ACTION;
            case LEGAL_ACTION:
                return null; // No next step
            default:
                return null;
        }
    }

    public Period getDelayFromPrevious() {
        return delayFromPrevious;
    }
}

