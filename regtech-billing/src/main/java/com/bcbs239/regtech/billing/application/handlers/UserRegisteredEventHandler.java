package com.bcbs239.regtech.billing.application.handlers;

import com.bcbs239.regtech.billing.application.policies.PaymentVerificationSaga;
import com.bcbs239.regtech.billing.domain.billing.BillingAccount;
import com.bcbs239.regtech.billing.domain.billing.BillingAccountId;
import com.bcbs239.regtech.billing.domain.billing.PaymentVerificationSagaData;
import com.bcbs239.regtech.billing.domain.invoices.Invoice;
import com.bcbs239.regtech.billing.domain.invoices.InvoiceId;
import com.bcbs239.regtech.billing.domain.subscriptions.Subscription;
import com.bcbs239.regtech.billing.domain.subscriptions.SubscriptionId;
import com.bcbs239.regtech.core.config.LoggingConfiguration;
import com.bcbs239.regtech.core.events.DomainEventHandler;
import com.bcbs239.regtech.core.events.UserRegisteredIntegrationEvent;
import com.bcbs239.regtech.core.saga.SagaId;
import com.bcbs239.regtech.core.saga.SagaManager;
import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.iam.domain.users.UserId;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.Map;
import com.bcbs239.regtech.billing.infrastructure.database.repositories.JpaBillingAccountRepository;
import com.bcbs239.regtech.billing.infrastructure.database.repositories.JpaSubscriptionRepository;
import com.bcbs239.regtech.billing.infrastructure.database.repositories.JpaInvoiceRepository;

import org.springframework.stereotype.Component;

/**
 * Event handler for UserRegisteredIntegrationEvent.
 * Starts the PaymentVerificationSaga when a user registers.
 */
@Component("billingUserRegisteredEventHandler")
@Slf4j
@RequiredArgsConstructor
public class UserRegisteredEventHandler implements DomainEventHandler<UserRegisteredIntegrationEvent> {
    private final SagaManager sagaManager;
    private final JpaBillingAccountRepository billingAccountRepository;
    private final JpaSubscriptionRepository subscriptionRepository;
    private final JpaInvoiceRepository invoiceRepository;

    @Override
    public boolean handle(UserRegisteredIntegrationEvent event) {
        log.info("Received UserRegisteredIntegrationEvent for user: {}", event.getEmail());

        UserId userId = UserId.fromString(event.getUserId());

        // Check if billing account already exists (idempotency)
        com.bcbs239.regtech.core.shared.Maybe<BillingAccount> existingAccount = billingAccountRepository.billingAccountByUserFinder().apply(userId);
        if (existingAccount.isPresent()) {
            log.info("Billing account already exists for user: {}, skipping processing", event.getUserId());
            return true;
        }

        // Create billing account for the user
        Instant now = Instant.now();
        BillingAccount billingAccount = BillingAccount.create(userId, now, now);
        
        Result<com.bcbs239.regtech.billing.domain.valueobjects.BillingAccountId> saveResult = billingAccountRepository.billingAccountSaver().apply(billingAccount);
        if (saveResult.isFailure()) {
            LoggingConfiguration.createStructuredLog("BILLING_ACCOUNT_CREATION_FAILED", Map.of(
                "userId", event.getUserId(),
                "error", saveResult.getError().get().getMessage()
            ));
            return false;
        }
        
        com.bcbs239.regtech.billing.domain.valueobjects.BillingAccountId valueObjectId = saveResult.getValue().get();
        BillingAccountId billingAccountId = BillingAccountId.of(valueObjectId.value());
        LoggingConfiguration.createStructuredLog("BILLING_ACCOUNT_CREATED", Map.of(
            "billingAccountId", billingAccountId.getValue(),
            "userId", event.getUserId()
        ));

        // Save the default subscription
        for (Subscription subscription : billingAccount.getSubscriptions()) {
            Result<SubscriptionId> subscriptionSaveResult = subscriptionRepository.subscriptionSaver().apply(subscription);
            if (subscriptionSaveResult.isFailure()) {
                LoggingConfiguration.createStructuredLog("SUBSCRIPTION_CREATION_FAILED", Map.of(
                    "billingAccountId", billingAccountId.getValue(),
                    "error", subscriptionSaveResult.getError().get().getMessage()
                ));
                return false;
            }
            LoggingConfiguration.createStructuredLog("SUBSCRIPTION_CREATED", Map.of(
                "subscriptionId", subscriptionSaveResult.getValue().get().value(),
                "billingAccountId", billingAccountId.getValue()
            ));
        }

        // Save the default invoice
        for (Invoice invoice : billingAccount.getInvoices()) {
            Result<InvoiceId> invoiceSaveResult = invoiceRepository.invoiceSaver().apply(invoice);
            if (invoiceSaveResult.isFailure()) {
                LoggingConfiguration.createStructuredLog("INVOICE_CREATION_FAILED", Map.of(
                    "billingAccountId", billingAccountId.getValue(),
                    "error", invoiceSaveResult.getError().get().getMessage()
                ));
                return false;
            }
            LoggingConfiguration.createStructuredLog("INVOICE_CREATED", Map.of(
                "invoiceId", invoiceSaveResult.getValue().get().value(),
                "billingAccountId", billingAccountId.getValue()
            ));
        }

        // Create saga data from the integration event
        PaymentVerificationSagaData sagaData = PaymentVerificationSagaData.builder()
            .correlationId(event.getId().toString()) // Convert UUID to String
            .userId(userId)
            .billingAccountId(billingAccountId)
            .userEmail(event.getEmail())
            .userName(event.getName())
            .paymentMethodId(event.getPaymentMethodId())
            .build();

        // Start the PaymentVerificationSaga
        SagaId sagaId = sagaManager.startSaga(PaymentVerificationSaga.class, sagaData);

        log.info("Started PaymentVerificationSaga with ID: {} for user: {}", sagaId, event.getUserId());

        log.info("User registration integration event processing completed for billing: userId={}, fullName={}",
            event.getUserId(), event.getName());
        
        return true;
    }

    @Override
    public String eventType() {
        return UserRegisteredIntegrationEvent.class.getSimpleName();
    }

    @Override
    public Class<UserRegisteredIntegrationEvent> eventClass() {
        return UserRegisteredIntegrationEvent.class;
    }
}