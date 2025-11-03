package com.bcbs239.regtech.billing.domain.events;

import com.bcbs239.regtech.billing.domain.valueobjects.BillingAccountId;
import com.bcbs239.regtech.billing.domain.valueobjects.BillingAccountStatus;
import com.bcbs239.regtech.core.events.BaseEvent;
import com.bcbs239.regtech.iam.domain.users.UserId;

/**
 * Domain event published when billing account status changes.
 */
public class BillingAccountStatusChangedEvent extends BaseEvent {

    private final BillingAccountId billingAccountId;
    private final UserId userId;
    private final BillingAccountStatus oldStatus;
    private final BillingAccountStatus newStatus;
    private final String reason;

    public BillingAccountStatusChangedEvent(BillingAccountId billingAccountId,
                                          UserId userId,
                                          BillingAccountStatus oldStatus,
                                          BillingAccountStatus newStatus,
                                          String reason,
                                          String correlationId) {
        super(correlationId, "billing");
        this.billingAccountId = billingAccountId;
        this.userId = userId;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.reason = reason;
    }

    public BillingAccountId getBillingAccountId() {
        return billingAccountId;
    }

    public UserId getUserId() {
        return userId;
    }

    public BillingAccountStatus getOldStatus() {
        return oldStatus;
    }

    public BillingAccountStatus getNewStatus() {
        return newStatus;
    }

    public BillingAccountStatus getPreviousStatus() {
        return oldStatus;
    }

    public String getReason() {
        return reason;
    }

    @Override
    public String toString() {
        return String.format("BillingAccountStatusChangedEvent{billingAccountId=%s, userId=%s, oldStatus=%s, newStatus=%s, reason=%s, correlationId=%s}",
            billingAccountId, userId, oldStatus, newStatus, reason, getCorrelationId());
    }
}