package com.bcbs239.regtech.billing.application.integration;

import com.bcbs239.regtech.billing.domain.accounts.BillingAccount;
import com.bcbs239.regtech.billing.domain.accounts.BillingAccountId;
import com.bcbs239.regtech.billing.domain.accounts.BillingAccountRepository;
import com.bcbs239.regtech.billing.domain.subscriptions.SubscriptionTier;
import com.bcbs239.regtech.core.domain.events.IIntegrationEventBus;
import com.bcbs239.regtech.core.domain.events.integration.BillingAccountActivatedEvent;
import com.bcbs239.regtech.core.domain.logging.ILogger;
import com.bcbs239.regtech.core.domain.shared.Maybe;
import com.bcbs239.regtech.core.domain.shared.Result;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Component
public class FinalizeBillingAccountCommandHandler {

    private final BillingAccountRepository billingAccountRepository;
    private final ILogger asyncLogger;
    private final IIntegrationEventBus integrationEventBus;


    public FinalizeBillingAccountCommandHandler(
            BillingAccountRepository billingAccountRepository,
            ILogger asyncLogger, IIntegrationEventBus integrationEventBus) {
        this.billingAccountRepository = billingAccountRepository;
        this.asyncLogger = asyncLogger;
        this.integrationEventBus = integrationEventBus;
    }

    @EventListener
    @Async("sagaTaskExecutor")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void handle(FinalizeBillingAccountCommand command) {
        String billingAccountIdStr = (String) command.payload().get("billingAccountId");

        asyncLogger.asyncStructuredLog("FINALIZE_BILLING_ACCOUNT_COMMAND_RECEIVED", Map.of(
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
                asyncLogger.asyncStructuredLog("FAILED_TO_FINALIZE_BILLING_ACCOUNT", Map.of(
                        "sagaId", String.valueOf(command.sagaId()),
                        "billingAccountId", String.valueOf(billingAccountIdStr),
                        "error", String.valueOf(saveResult.getError())
                ));
            } else {
                asyncLogger.asyncStructuredLog("BILLING_ACCOUNT_FINALIZED", Map.of(
                        "sagaId", String.valueOf(command.sagaId()),
                        "billingAccountId", String.valueOf(billingAccountIdStr)
                ));
                publishBillingActivatedEvent(billingAccount.getUserId().getValue(), command.getCorrelationId());
            }
        } else {
            asyncLogger.asyncStructuredLog("BILLING_ACCOUNT_NOT_FOUND", Map.of(
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
            asyncLogger.asyncStructuredErrorLog("BILLING_ACTIVATED_EVENT_PUBLICATION_FAILED", e, java.util.Map.of(
                    "error", e.getMessage()
            ));
        }
    }
}

