package com.bcbs239.regtech.billing.application.integration;

import com.bcbs239.regtech.billing.application.payments.PaymentVerificationSaga;
import com.bcbs239.regtech.billing.domain.accounts.BillingAccountId;
import com.bcbs239.regtech.billing.domain.invoices.Invoice;
import com.bcbs239.regtech.billing.domain.invoices.InvoiceId;
import com.bcbs239.regtech.billing.domain.payments.PaymentVerificationSagaData;
import com.bcbs239.regtech.billing.domain.subscriptions.Subscription;
import com.bcbs239.regtech.billing.domain.subscriptions.SubscriptionId;
import com.bcbs239.regtech.billing.domain.subscriptions.SubscriptionRepository;
import com.bcbs239.regtech.core.application.saga.SagaManager;
import com.bcbs239.regtech.core.domain.saga.SagaId;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.core.domain.shared.Maybe;
import com.bcbs239.regtech.billing.domain.accounts.BillingAccountRepository;
import com.bcbs239.regtech.billing.domain.accounts.BillingAccount;
import com.bcbs239.regtech.core.domain.shared.valueobjects.UserId;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Map;

@Component("billingUserRegisteredEventHandler")
@RequiredArgsConstructor
public class UserRegisteredEventHandler extends com.bcbs239.regtech.core.application.eventprocessing.IntegrationEventHandler<BillingUserRegisteredEvent> {
    private static final Logger log = LoggerFactory.getLogger(UserRegisteredEventHandler.class);

    private final ApplicationContext applicationContext;
    private final BillingAccountRepository billingAccountRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final com.bcbs239.regtech.billing.domain.repositories.InvoiceRepository invoiceRepository;
    private final SagaManager sagaManager;

    @EventListener
    public void handle(BillingUserRegisteredEvent event) {
        handleIntegrationEvent(event, this::processEvent);
    }
    
    private void processEvent(BillingUserRegisteredEvent event) {
        try {
            log.info("Processing BillingUserRegisteredEvent {}", Map.of(
                    "eventType", "BILLING_USER_REGISTERED_EVENT_RECEIVED",
                    "userId", event.getUserId(),
                    "email", event.getEmail()
            ));

            UserId userId = UserId.fromString(event.getUserId());

            // Check if billing account already exists (idempotency)
            Maybe<BillingAccount> existingAccount = billingAccountRepository.findByUserId(userId);
            if (existingAccount.isPresent()) {
                log.info("Billing account already exists, skipping processing {}", Map.of(
                        "eventType", "BILLING_ACCOUNT_EXISTS",
                        "userId", event.getUserId()
                ));
                return;
            }

            // Create billing account for the user
            Instant now = Instant.now();
            BillingAccount billingAccount = new BillingAccount.Builder().userId(userId).createdAt(now).updatedAt(now).withDefaults().build();

            Result<BillingAccountId> saveResult = billingAccountRepository.save(billingAccount);
            if (saveResult.isFailure()) {
                return;
            }

            BillingAccountId billingAccountId = saveResult.getValue().get();
            log.info("Billing account created {}", Map.of(
                    "eventType", "BILLING_ACCOUNT_CREATED",
                    "billingAccountId", billingAccountId.getValue(),
                    "userId", event.getUserId()
            ));

            // Save the default subscription (set billing account ID first)
            for (Subscription subscription : billingAccount.getSubscriptions()) {
                // Set the billing account ID on the subscription now that we have it
                subscription.setBillingAccountId(Maybe.some(billingAccountId));

                Result<SubscriptionId> subscriptionSaveResult = subscriptionRepository.save(subscription);
                if (subscriptionSaveResult.isFailure()) {
                    log.error("Subscription creation failed {}", Map.of(
                            "eventType", "SUBSCRIPTION_CREATION_FAILED",
                            "billingAccountId", billingAccountId.getValue(),
                            "error", subscriptionSaveResult.getError().get().getMessage()
                    ));
                    return;
                }
                log.info("Subscription created {}", Map.of(
                        "eventType", "SUBSCRIPTION_CREATED",
                        "subscriptionId", subscriptionSaveResult.getValue().get().value(),
                        "billingAccountId", billingAccountId.getValue()
                ));
            }

            // Save the default invoice
            for (Invoice invoice : billingAccount.getInvoices()) {
                Result<InvoiceId> invoiceSaveResult = invoiceRepository.save(invoice);
                if (invoiceSaveResult.isFailure()) {
                    log.error("Invoice creation failed {}", Map.of(
                            "eventType", "INVOICE_CREATION_FAILED",
                            "billingAccountId", billingAccountId.getValue(),
                            "error", invoiceSaveResult.getError().get().getMessage()
                    ));
                    return;
                }
                log.info("Invoice created {}", Map.of(
                        "eventType", "INVOICE_CREATED",
                        "invoiceId", invoiceSaveResult.getValue().get().value(),
                        "billingAccountId", billingAccountId.getValue()
                ));
            }

            PaymentVerificationSagaData sagaData = PaymentVerificationSagaData.builder()
                    .correlationId(event.getEventId().toString()) // Convert UUID to String
                    .userId(event.getUserId())
                    .billingAccountId(billingAccountId.getValue())
                    .userEmail(event.getEmail())
                    .userName(event.getEmail()) // Use email as name since name field doesn't exist
                    .paymentMethodId(event.getPaymentMethodId())
                    .build();

            // Start the PaymentVerificationSaga via runtime lookup of sagaManager bean
            log.info("About to start PaymentVerificationSaga {}", Map.of(
                    "eventType", "SAGA_START_ATTEMPT",
                    "userId", event.getUserId(),
                    "sagaData", sagaData.toString()
            ));

            try {

                SagaId sagaId = sagaManager.startSaga(PaymentVerificationSaga.class, sagaData);

                log.info("PaymentVerificationSaga started successfully {}", Map.of(
                        "eventType", "SAGA_START_SUCCESS",
                        "userId", event.getUserId(),
                        "sagaId", sagaId.id()
                ));

            } catch (Exception ex) {
                log.error("SAGA_START_FAILED {} {}", ex, Map.of(
                        "userId", event.getUserId(),
                        "error", ex.getMessage()
                ));
                throw new RuntimeException("Failed to start saga: " + ex.getMessage(), ex);
            }

            log.info("User registration integration event processing completed {}", Map.of(
                    "eventType", "USER_REGISTRATION_COMPLETED",
                    "userId", event.getUserId()
            ));

        } catch (Exception e) {
            // Log full error and rethrow to allow caller to handle transaction properly
            log.error("billing_user_registration UNHANDLED_HANDLER_EXCEPTION: {}{}", e, e.getMessage(), Map.of(
                    "eventId", event.getEventId(),
                    "userId", event.getUserId()
            ));
            throw e;
        }
    }
}