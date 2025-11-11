package com.bcbs239.regtech.billing.application.integration;

import com.bcbs239.regtech.billing.application.payments.PaymentVerificationSaga;
import com.bcbs239.regtech.billing.domain.accounts.BillingAccount;
import com.bcbs239.regtech.billing.domain.accounts.BillingAccountId;
import com.bcbs239.regtech.billing.domain.accounts.BillingAccountRepository;
import com.bcbs239.regtech.billing.domain.payments.PaymentVerificationSagaData;
import com.bcbs239.regtech.billing.domain.subscriptions.Subscription;
import com.bcbs239.regtech.billing.domain.subscriptions.SubscriptionId;

import com.bcbs239.regtech.billing.domain.subscriptions.SubscriptionRepository;
import com.bcbs239.regtech.core.domain.logging.ILogger;
import com.bcbs239.regtech.core.domain.saga.SagaId;
import com.bcbs239.regtech.core.domain.shared.Maybe;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.iam.domain.users.UserId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Map;

/**
 * Event handler for BillingUserRegisteredEvent (local domain event).
 * Starts the PaymentVerificationSaga when a user registers.
 */
@Component("billingUserRegisteredEventHandler")
public class UserRegisteredEventHandler  {

    private final ApplicationContext applicationContext;
    private final BillingAccountRepository billingAccountRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final ILogger asyncLogger;


    @Autowired
    public UserRegisteredEventHandler(ApplicationContext applicationContext,
                                      BillingAccountRepository billingAccountRepository,
                                      SubscriptionRepository subscriptionRepository,
                                      ILogger asyncLogger
    ) {
        this.applicationContext = applicationContext;
        this.billingAccountRepository = billingAccountRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.asyncLogger = asyncLogger;
    }

    @EventListener
    public void handle(BillingUserRegisteredEvent event) {
        try {
            asyncLogger.asyncStructuredLog("Received BillingUserRegisteredEvent", Map.of(
                "eventType", "BILLING_USER_REGISTERED_EVENT_RECEIVED",
                "userId", event.getUserId(),
                "email", event.getEmail()
            ));

            UserId userId = UserId.fromString(event.getUserId());

            // Check if billing account already exists (idempotency)
            Maybe<BillingAccount> existingAccount = billingAccountRepository.findByUserId(userId);
            if (existingAccount.isPresent()) {
                asyncLogger.asyncStructuredLog("Billing account already exists, skipping processing", Map.of(
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
                asyncLogger.asyncStructuredLog("Failed to create billing account", Map.of(
                    "eventType", "BILLING_ACCOUNT_CREATION_FAILED",
                    "userId", event.getUserId(),
                    "error", saveResult.getError().get().getMessage()
                ));
                return;
            }

            BillingAccountId billingAccountId = saveResult.getValue().get();
            asyncLogger.asyncStructuredLog("Billing account created", Map.of(
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
                    asyncLogger.asyncStructuredLog("Subscription creation failed", Map.of(
                        "eventType", "SUBSCRIPTION_CREATION_FAILED",
                        "billingAccountId", billingAccountId.getValue(),
                        "error", subscriptionSaveResult.getError().get().getMessage()
                    ));
                    return;
                }
                asyncLogger.asyncStructuredLog("Subscription created", Map.of(
                    "eventType", "SUBSCRIPTION_CREATED",
                    "subscriptionId", subscriptionSaveResult.getValue().get().value(),
                    "billingAccountId", billingAccountId.getValue()
                ));
            }

            // NOTE: Invoices will be created via Stripe integration (PaymentVerificationSaga)
            // No need to save default invoices here

            PaymentVerificationSagaData sagaData = PaymentVerificationSagaData.builder()
                 .correlationId(event.getEventId().toString()) // Convert UUID to String
                 .userId(event.getUserId())
                 .billingAccountId(billingAccountId.getValue())
                 .userEmail(event.getEmail())
                 .userName(event.getEmail()) // Use email as name since name field doesn't exist
                 .paymentMethodId(event.getPaymentMethodId())
                 .build();

            // Start the PaymentVerificationSaga via runtime lookup of sagaManager bean
            asyncLogger.asyncStructuredLog("About to start PaymentVerificationSaga", Map.of(
                "eventType", "SAGA_START_ATTEMPT",
                "userId", event.getUserId(),
                "sagaData", sagaData.toString()
            ));

            try {

                    Object sagaManagerBean = applicationContext.getBean("sagaManager");
                    Method startSaga = sagaManagerBean.getClass().getMethod("startSaga", Class.class, Object.class);
                    Object sagaIdObj = startSaga.invoke(sagaManagerBean, PaymentVerificationSaga.class, sagaData);
                    SagaId sagaId = (SagaId) sagaIdObj;

                asyncLogger.asyncStructuredLog("PaymentVerificationSaga started successfully", Map.of(
                        "eventType", "SAGA_START_SUCCESS",
                        "userId", event.getUserId(),
                        "sagaId", sagaId.id()
                    ));

            } catch (Exception ex) {
                asyncLogger.asyncStructuredErrorLog("SAGA_START_FAILED", ex, Map.of(
                    "userId", event.getUserId(),
                    "error", ex.getMessage()
                ));
                throw new RuntimeException("Failed to start saga: " + ex.getMessage(), ex);
            }

            asyncLogger.asyncStructuredLog("User registration integration event processing completed", Map.of(
                "eventType", "USER_REGISTRATION_COMPLETED",
                "userId", event.getUserId()
            ));

        } catch (Exception e) {
            // Log full error and rethrow to allow caller to handle transaction properly
            asyncLogger.asyncStructuredErrorLog("billing_user_registration UNHANDLED_HANDLER_EXCEPTION: " + e.getMessage(), e, Map.of(
                "eventId", event.getEventId(),
                "userId", event.getUserId()
            ));
            throw e;
        }
    }

}

