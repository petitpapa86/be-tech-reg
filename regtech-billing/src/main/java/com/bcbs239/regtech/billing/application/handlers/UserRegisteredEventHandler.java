package com.bcbs239.regtech.billing.application.handlers;

import com.bcbs239.regtech.billing.application.policies.PaymentVerificationSaga;
import com.bcbs239.regtech.billing.domain.billing.BillingAccount;
import com.bcbs239.regtech.billing.domain.billing.BillingAccountId;
// import com.bcbs239.regtech.billing.domain.billing.PaymentVerificationSagaData;
import com.bcbs239.regtech.billing.domain.billing.PaymentVerificationSagaData;
import com.bcbs239.regtech.billing.domain.invoices.Invoice;
import com.bcbs239.regtech.billing.domain.invoices.InvoiceId;
import com.bcbs239.regtech.billing.domain.subscriptions.Subscription;
import com.bcbs239.regtech.billing.domain.subscriptions.SubscriptionId;
import com.bcbs239.regtech.core.config.LoggingConfiguration;
import com.bcbs239.regtech.core.events.DomainEventHandler;
import com.bcbs239.regtech.core.events.UserRegisteredIntegrationEvent;
// import com.bcbs239.regtech.core.saga.SagaId;
// import com.bcbs239.regtech.core.saga.SagaManager;
import com.bcbs239.regtech.core.saga.SagaId;
import com.bcbs239.regtech.core.saga.SagaManager;
import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.iam.domain.users.UserId;

import lombok.RequiredArgsConstructor;

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
@RequiredArgsConstructor
public class UserRegisteredEventHandler implements DomainEventHandler<UserRegisteredIntegrationEvent> {
     private final SagaManager sagaManager;
    private final JpaBillingAccountRepository billingAccountRepository;
    private final JpaSubscriptionRepository subscriptionRepository;
    private final JpaInvoiceRepository invoiceRepository;

    @Override
    public boolean handle(UserRegisteredIntegrationEvent event) {
        LoggingConfiguration.logStructured("Received UserRegisteredIntegrationEvent", "USER_REGISTERED_EVENT_RECEIVED", Map.of(
            "userId", event.getUserId(),
            "email", event.getEmail()
        ));

        UserId userId = UserId.fromString(event.getUserId());

        // Check if billing account already exists (idempotency)
        com.bcbs239.regtech.core.shared.Maybe<BillingAccount> existingAccount = billingAccountRepository.billingAccountByUserFinder().apply(userId);
        if (existingAccount.isPresent()) {
            LoggingConfiguration.logStructured("Billing account already exists, skipping processing", "BILLING_ACCOUNT_EXISTS", Map.of(
                "userId", event.getUserId()
            ));
            return true;
        }

        // Create billing account for the user
        Instant now = Instant.now();
        BillingAccount billingAccount = new BillingAccount.Builder().userId(userId).createdAt(now).updatedAt(now).withDefaults().build();
        
        Result<com.bcbs239.regtech.billing.domain.valueobjects.BillingAccountId> saveResult = billingAccountRepository.billingAccountSaver().apply(billingAccount);
        if (saveResult.isFailure()) {
            LoggingConfiguration.logStructured("Billing account creation failed", "BILLING_ACCOUNT_CREATION_FAILED", Map.of(
                "userId", event.getUserId(),
                "error", saveResult.getError().get().getMessage()
            ));
            return false;
        }
        
        com.bcbs239.regtech.billing.domain.valueobjects.BillingAccountId valueObjectId = saveResult.getValue().get();
        BillingAccountId billingAccountId = BillingAccountId.of(valueObjectId.value());
        LoggingConfiguration.logStructured("Billing account created", "BILLING_ACCOUNT_CREATED", Map.of(
            "billingAccountId", billingAccountId.getValue(),
            "userId", event.getUserId()
        ));

        // Save the default subscription
        for (Subscription subscription : billingAccount.getSubscriptions()) {
            Result<SubscriptionId> subscriptionSaveResult = subscriptionRepository.subscriptionSaver().apply(subscription);
            if (subscriptionSaveResult.isFailure()) {
                LoggingConfiguration.logStructured("Subscription creation failed", "SUBSCRIPTION_CREATION_FAILED", Map.of(
                    "billingAccountId", billingAccountId.getValue(),
                    "error", subscriptionSaveResult.getError().get().getMessage()
                ));
                return false;
            }
            LoggingConfiguration.logStructured("Subscription created", "SUBSCRIPTION_CREATED", Map.of(
                "subscriptionId", subscriptionSaveResult.getValue().get().value(),
                "billingAccountId", billingAccountId.getValue()
            ));
        }

        // Save the default invoice
        for (Invoice invoice : billingAccount.getInvoices()) {
            Result<InvoiceId> invoiceSaveResult = invoiceRepository.invoiceSaver().apply(invoice);
            if (invoiceSaveResult.isFailure()) {
                LoggingConfiguration.logStructured("Invoice creation failed", "INVOICE_CREATION_FAILED", Map.of(
                    "billingAccountId", billingAccountId.getValue(),
                    "error", invoiceSaveResult.getError().get().getMessage()
                ));
                return false;
            }
            LoggingConfiguration.logStructured("Invoice created", "INVOICE_CREATED", Map.of(
                "invoiceId", invoiceSaveResult.getValue().get().value(),
                "billingAccountId", billingAccountId.getValue()
            ));
        }


         PaymentVerificationSagaData sagaData = PaymentVerificationSagaData.builder()
             .correlationId(event.getId().toString()) // Convert UUID to String
             .userId(event.getUserId())
             .billingAccountId(billingAccountId.getValue())
             .userEmail(event.getEmail())
             .userName(event.getName())
             .paymentMethodId(event.getPaymentMethodId())
             .build();

        // Start the PaymentVerificationSaga
        LoggingConfiguration.logStructured("About to start PaymentVerificationSaga", "SAGA_START_ATTEMPT", Map.of(
            "userId", event.getUserId(),
            "sagaData", sagaData.toString()
        ));
        
        SagaId sagaId = sagaManager.startSaga(PaymentVerificationSaga.class, sagaData);
        
        LoggingConfiguration.logStructured("PaymentVerificationSaga started successfully", "SAGA_START_SUCCESS", Map.of(
            "userId", event.getUserId(),
            "sagaId", sagaId.id()
        ));

        LoggingConfiguration.logStructured("User registration integration event processing completed", "USER_REGISTRATION_COMPLETED", Map.of(
            "userId", event.getUserId(),
            "fullName", event.getName()
        ));
        
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