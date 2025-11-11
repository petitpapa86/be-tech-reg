package com.bcbs239.regtech.billing.application.payments.compensation;

import com.bcbs239.regtech.billing.domain.accounts.BillingAccount;
import com.bcbs239.regtech.billing.domain.accounts.BillingAccountId;
import com.bcbs239.regtech.billing.domain.accounts.BillingAccountRepository;
import com.bcbs239.regtech.core.domain.logging.ILogger;
import com.bcbs239.regtech.core.domain.shared.Maybe;
import com.bcbs239.regtech.core.domain.shared.Result;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Handles billing account suspension events during saga compensation.
 * Executes asynchronously to suspend billing accounts.
 */
@Component
public class SuspendBillingAccountEventHandler {

    private final BillingAccountRepository billingAccountRepository;
    private final ILogger asyncLogger;

    public SuspendBillingAccountEventHandler(
            BillingAccountRepository billingAccountRepository,
            ILogger asyncLogger) {
        this.billingAccountRepository = billingAccountRepository;
        this.asyncLogger = asyncLogger;
    }

    @EventListener
    @Async("sagaTaskExecutor")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void handle(SuspendBillingAccountEvent event) {
        asyncLogger.asyncStructuredLog("SUSPEND_BILLING_ACCOUNT_COMPENSATION_STARTED", Map.of(
            "sagaId", event.sagaId(),
            "billingAccountId", event.billingAccountId(),
            "userId", event.userId(),
            "reason", event.reason()
        ));

        try {
            Result<BillingAccountId> accountIdResult = BillingAccountId.fromString(event.billingAccountId());
            if (accountIdResult.isFailure()) {
                asyncLogger.asyncStructuredErrorLog("INVALID_BILLING_ACCOUNT_ID", null, Map.of(
                    "sagaId", event.sagaId(),
                    "billingAccountId", event.billingAccountId()
                ));
                return;
            }

            Maybe<BillingAccount> accountMaybe = billingAccountRepository.findById(accountIdResult.getValue().get());
            
            if (accountMaybe.isEmpty()) {
                asyncLogger.asyncStructuredLog("BILLING_ACCOUNT_NOT_FOUND_FOR_SUSPENSION", Map.of(
                    "sagaId", event.sagaId(),
                    "billingAccountId", event.billingAccountId()
                ));
                return;
            }

            BillingAccount account = accountMaybe.getValue();
            
            // Suspend the account
            Result<Void> suspendResult = account.suspend(event.reason());
            
            if (suspendResult.isFailure()) {
                asyncLogger.asyncStructuredErrorLog("BILLING_ACCOUNT_SUSPENSION_FAILED", null, Map.of(
                    "sagaId", event.sagaId(),
                    "billingAccountId", event.billingAccountId(),
                    "error", suspendResult.getError().map(e -> e.getMessage()).orElse("Unknown error")
                ));
                return;
            }

            // Save the suspended account
            billingAccountRepository.save(account);

            asyncLogger.asyncStructuredLog("BILLING_ACCOUNT_SUSPENDED_SUCCESSFULLY", Map.of(
                "sagaId", event.sagaId(),
                "billingAccountId", event.billingAccountId()
            ));

        } catch (Exception e) {
            asyncLogger.asyncStructuredErrorLog("BILLING_ACCOUNT_SUSPENSION_EXCEPTION", e, Map.of(
                "sagaId", event.sagaId(),
                "billingAccountId", event.billingAccountId()
            ));
        }
    }
}
