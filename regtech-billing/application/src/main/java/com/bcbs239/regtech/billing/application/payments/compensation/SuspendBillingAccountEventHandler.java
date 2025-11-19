package com.bcbs239.regtech.billing.application.payments.compensation;

import com.bcbs239.regtech.billing.domain.accounts.BillingAccount;
import com.bcbs239.regtech.billing.domain.accounts.BillingAccountId;
import com.bcbs239.regtech.billing.domain.accounts.BillingAccountRepository;
import com.bcbs239.regtech.core.domain.shared.Maybe;
import com.bcbs239.regtech.core.domain.shared.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final Logger log = LoggerFactory.getLogger(SuspendBillingAccountEventHandler.class);

    public SuspendBillingAccountEventHandler(
            BillingAccountRepository billingAccountRepository) {
        this.billingAccountRepository = billingAccountRepository;
    }

    @EventListener
    @Async("sagaTaskExecutor")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void handle(SuspendBillingAccountEvent event) {
        log.info("SUSPEND_BILLING_ACCOUNT_COMPENSATION_STARTED; details={}", Map.of(
            "sagaId", event.sagaId(),
            "billingAccountId", event.billingAccountId(),
            "userId", event.userId(),
            "reason", event.reason()
        ));

        try {
            Result<BillingAccountId> accountIdResult = BillingAccountId.fromString(event.billingAccountId());
            if (accountIdResult.isFailure()) {
                log.error("INVALID_BILLING_ACCOUNT_ID; details={}", Map.of(
                    "sagaId", event.sagaId(),
                    "billingAccountId", event.billingAccountId()
                ));
                return;
            }

            Maybe<BillingAccount> accountMaybe = billingAccountRepository.findById(accountIdResult.getValue().get());
            
            if (accountMaybe.isEmpty()) {
                log.info("BILLING_ACCOUNT_NOT_FOUND_FOR_SUSPENSION; details={}", Map.of(
                    "sagaId", event.sagaId(),
                    "billingAccountId", event.billingAccountId()
                ));
                return;
            }

            BillingAccount account = accountMaybe.getValue();
            
            // Suspend the account
            Result<Void> suspendResult = account.suspend(event.reason());
            
            if (suspendResult.isFailure()) {
                log.error("BILLING_ACCOUNT_SUSPENSION_FAILED; details={}", Map.of(
                    "sagaId", event.sagaId(),
                    "billingAccountId", event.billingAccountId(),
                    "error", suspendResult.getError().map(e -> e.getMessage()).orElse("Unknown error")
                ));
                return;
            }

            // Save the suspended account
            billingAccountRepository.save(account);

            log.info("BILLING_ACCOUNT_SUSPENDED_SUCCESSFULLY; details={}", Map.of(
                "sagaId", event.sagaId(),
                "billingAccountId", event.billingAccountId()
            ));

        } catch (Exception e) {
            log.error("BILLING_ACCOUNT_SUSPENSION_EXCEPTION; details={}", Map.of(
                "sagaId", event.sagaId(),
                "billingAccountId", event.billingAccountId()
            ), e);
        }
    }
}
