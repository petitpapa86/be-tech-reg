package com.bcbs239.regtech.billing.application.integration;

import com.bcbs239.regtech.billing.domain.accounts.BillingAccount;
import com.bcbs239.regtech.billing.domain.accounts.BillingAccountId;
import com.bcbs239.regtech.billing.domain.accounts.BillingAccountRepository;
import com.bcbs239.regtech.billing.domain.subscriptions.SubscriptionTier;
import com.bcbs239.regtech.core.domain.events.IIntegrationEventBus;
import com.bcbs239.regtech.core.domain.events.integration.BillingAccountActivatedEvent;
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

@Component
public class FinalizeBillingAccountCommandHandler {

    private final BillingAccountRepository billingAccountRepository;
    private final IIntegrationEventBus integrationEventBus;
    private static final Logger log = LoggerFactory.getLogger(FinalizeBillingAccountCommandHandler.class);


    public FinalizeBillingAccountCommandHandler(
            BillingAccountRepository billingAccountRepository, IIntegrationEventBus integrationEventBus) {
        this.billingAccountRepository = billingAccountRepository;
        this.integrationEventBus = integrationEventBus;
    }

    @EventListener
    @Async("sagaTaskExecutor")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void handle(FinalizeBillingAccountCommand command) {
        String billingAccountIdStr = (String) command.payload().get("billingAccountId");

        log.info("FINALIZE_BILLING_ACCOUNT_COMMAND_RECEIVED; details={}", Map.of(
                "sagaId", String.valueOf(command.sagaId()),
                "billingAccountId", String.valueOf(billingAccountIdStr)
        ));

        BillingAccountId billingAccountId = BillingAccountId.fromString(billingAccountIdStr).getValue().get();

        Maybe<BillingAccount> maybeAccount = billingAccountRepository.findById(billingAccountId);
        if (maybeAccount.isPresent()) {
            BillingAccount billingAccount = maybeAccount.getValue();
            billingAccount.finalizeAccount();
            Result<BillingAccountId> saveResult = billingAccountRepository.update(billingAccount);
            if (saveResult.isFailure()) {
                log.error("FAILED_TO_FINALIZE_BILLING_ACCOUNT; details={}", Map.of(
                        "sagaId", String.valueOf(command.sagaId()),
                        "billingAccountId", String.valueOf(billingAccountIdStr),
                        "error", String.valueOf(saveResult.getError())
                ));
            } else {
                log.info("BILLING_ACCOUNT_FINALIZED; details={}", Map.of(
                        "sagaId", String.valueOf(command.sagaId()),
                        "billingAccountId", String.valueOf(billingAccountIdStr)
                ));
                publishBillingActivatedEvent(billingAccount.getUserId().getValue(), command.getCorrelationId());
            }
        } else {
            log.info("BILLING_ACCOUNT_NOT_FOUND; details={}", Map.of(
                    "sagaId", String.valueOf(command.sagaId()),
                    "billingAccountId", String.valueOf(billingAccountIdStr)
            ));
        }
    }

    private void publishBillingActivatedEvent(String userId, String correlationId) {
        try {

            BillingAccountActivatedEvent event = new BillingAccountActivatedEvent(
                    userId, correlationId
            );

            integrationEventBus.publish(event);

        } catch (Exception e) {
            log.error("BILLING_ACTIVATED_EVENT_PUBLICATION_FAILED; details={}", java.util.Map.of(
                    "error", e.getMessage()
            ), e);
        }
    }
}
