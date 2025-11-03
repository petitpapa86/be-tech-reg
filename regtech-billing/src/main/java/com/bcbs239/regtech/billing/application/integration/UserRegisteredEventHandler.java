package com.bcbs239.regtech.billing.application.integration;

import com.bcbs239.regtech.billing.application.policies.PaymentVerificationSaga;
import com.bcbs239.regtech.billing.domain.billing.BillingAccount;
import com.bcbs239.regtech.billing.domain.billing.BillingAccountId;
import com.bcbs239.regtech.billing.domain.billing.PaymentVerificationSagaData;
import com.bcbs239.regtech.billing.domain.invoices.Invoice;
import com.bcbs239.regtech.billing.domain.invoices.InvoiceId;
import com.bcbs239.regtech.billing.domain.subscriptions.Subscription;
import com.bcbs239.regtech.billing.domain.subscriptions.SubscriptionId;
import com.bcbs239.regtech.billing.infrastructure.database.repositories.JpaBillingAccountRepository;
import com.bcbs239.regtech.billing.infrastructure.database.repositories.JpaInvoiceRepository;
import com.bcbs239.regtech.billing.infrastructure.database.repositories.JpaSubscriptionRepository;
import com.bcbs239.regtech.core.config.LoggingConfiguration;
import com.bcbs239.regtech.core.application.IIntegrationEventHandler;
import com.bcbs239.regtech.core.events.UserRegisteredIntegrationEvent;
import com.bcbs239.regtech.core.saga.SagaId;
import com.bcbs239.regtech.core.saga.SagaManager;
import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.iam.domain.users.UserId;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * Event handler for UserRegisteredIntegrationEvent.
 * Starts the PaymentVerificationSaga when a user registers.
 */
@Component("billingUserRegisteredEventHandler")
public class UserRegisteredEventHandler implements IIntegrationEventHandler<UserRegisteredIntegrationEvent> {

    private final ApplicationContext applicationContext;
    private final JpaBillingAccountRepository billingAccountRepository;
    private final JpaSubscriptionRepository subscriptionRepository;
    private final JpaInvoiceRepository invoiceRepository;
    private final SagaManager sagaManagerForTest; // optional

    @Autowired
    public UserRegisteredEventHandler(ApplicationContext applicationContext,
                                      JpaBillingAccountRepository billingAccountRepository,
                                      JpaSubscriptionRepository subscriptionRepository,
                                      JpaInvoiceRepository invoiceRepository,
                                      @Autowired(required = false) SagaManager sagaManagerForTest) {
        this.applicationContext = applicationContext;
        this.billingAccountRepository = billingAccountRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.invoiceRepository = invoiceRepository;
        this.sagaManagerForTest = sagaManagerForTest;
    }

    // Backwards-compatible constructor for tests that instantiate handler with SagaManager first
    public UserRegisteredEventHandler(SagaManager sagaManager,
                                      JpaBillingAccountRepository billingAccountRepository,
                                      JpaSubscriptionRepository subscriptionRepository,
                                      JpaInvoiceRepository invoiceRepository) {
        this.applicationContext = null;
        this.billingAccountRepository = billingAccountRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.invoiceRepository = invoiceRepository;
        this.sagaManagerForTest = sagaManager;
    }

    @Override
    public void handle(UserRegisteredIntegrationEvent event) {
        try {
            LoggingConfiguration.logStructured("Received UserRegisteredIntegrationEvent", Map.of(
                "eventType", "USER_REGISTERED_EVENT_RECEIVED",
                "userId", event.getUserId(),
                "email", event.getEmail()
            ));

            UserId userId = UserId.fromString(event.getUserId());

            // Check if billing account already exists (idempotency)
            com.bcbs239.regtech.core.shared.Maybe<BillingAccount> existingAccount = billingAccountRepository.billingAccountByUserFinder().apply(userId);
            if (existingAccount.isPresent()) {
                LoggingConfiguration.logStructured("Billing account already exists, skipping processing", Map.of(
                    "eventType", "BILLING_ACCOUNT_EXISTS",
                    "userId", event.getUserId()
                ));
                return;
            }

            // Create billing account for the user
            Instant now = Instant.now();
            BillingAccount billingAccount = new BillingAccount.Builder().userId(userId).createdAt(now).updatedAt(now).withDefaults().build();

            Result<com.bcbs239.regtech.billing.domain.valueobjects.BillingAccountId> saveResult = billingAccountRepository.billingAccountSaver().apply(billingAccount);
            if (saveResult.isFailure()) {
                LoggingConfiguration.logStructured("Failed to create billing account", Map.of(
                    "eventType", "BILLING_ACCOUNT_CREATION_FAILED",
                    "userId", event.getUserId(),
                    "error", saveResult.getError().get().getMessage()
                ));
                return;
            }

            com.bcbs239.regtech.billing.domain.valueobjects.BillingAccountId valueObjectId = saveResult.getValue().get();
            BillingAccountId billingAccountId = BillingAccountId.of(valueObjectId.value());
            LoggingConfiguration.logStructured("Billing account created", Map.of(
                "eventType", "BILLING_ACCOUNT_CREATED",
                "billingAccountId", billingAccountId.getValue(),
                "userId", event.getUserId()
            ));

            // Save the default subscription
            for (Subscription subscription : billingAccount.getSubscriptions()) {
                Result<SubscriptionId> subscriptionSaveResult = subscriptionRepository.subscriptionSaver().apply(subscription);
                if (subscriptionSaveResult.isFailure()) {
                    LoggingConfiguration.logStructured("Subscription creation failed", Map.of(
                        "eventType", "SUBSCRIPTION_CREATION_FAILED",
                        "billingAccountId", billingAccountId.getValue(),
                        "error", subscriptionSaveResult.getError().get().getMessage()
                    ));
                    return;
                }
                LoggingConfiguration.logStructured("Subscription created", Map.of(
                    "eventType", "SUBSCRIPTION_CREATED",
                    "subscriptionId", subscriptionSaveResult.getValue().get().value(),
                    "billingAccountId", billingAccountId.getValue()
                ));
            }

            // Save the default invoice
            for (Invoice invoice : billingAccount.getInvoices()) {
                Result<InvoiceId> invoiceSaveResult = invoiceRepository.invoiceSaver().apply(invoice);
                if (invoiceSaveResult.isFailure()) {
                    LoggingConfiguration.logStructured("Invoice creation failed", Map.of(
                        "eventType", "INVOICE_CREATION_FAILED",
                        "billingAccountId", billingAccountId.getValue(),
                        "error", invoiceSaveResult.getError().get().getMessage()
                    ));
                    return;
                }
                LoggingConfiguration.logStructured("Invoice created", Map.of(
                    "eventType", "INVOICE_CREATED",
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

            // Start the PaymentVerificationSaga via runtime lookup of sagaManager bean
            LoggingConfiguration.logStructured("About to start PaymentVerificationSaga", Map.of(
                "eventType", "SAGA_START_ATTEMPT",
                "userId", event.getUserId(),
                "sagaData", sagaData.toString()
            ));

            try {
                if (sagaManagerForTest != null) {
                    Method startSaga = sagaManagerForTest.getClass().getMethod("startSaga", Class.class, Object.class);
                    Object sagaIdObj = startSaga.invoke(sagaManagerForTest, PaymentVerificationSaga.class, sagaData);
                    SagaId sagaId = (SagaId) sagaIdObj;

                    LoggingConfiguration.logStructured("PaymentVerificationSaga started successfully", Map.of(
                        "eventType", "SAGA_START_SUCCESS",
                        "userId", event.getUserId(),
                        "sagaId", sagaId.id()
                    ));
                } else {
                    Object sagaManagerBean = applicationContext.getBean("sagaManager");
                    Method startSaga = sagaManagerBean.getClass().getMethod("startSaga", Class.class, Object.class);
                    Object sagaIdObj = startSaga.invoke(sagaManagerBean, PaymentVerificationSaga.class, sagaData);
                    SagaId sagaId = (SagaId) sagaIdObj;

                    LoggingConfiguration.logStructured("PaymentVerificationSaga started successfully", Map.of(
                        "eventType", "SAGA_START_SUCCESS",
                        "userId", event.getUserId(),
                        "sagaId", sagaId.id()
                    ));
                }
            } catch (Exception ex) {
                LoggingConfiguration.createStructuredLog("SAGA_START_FAILED", Map.of(
                    "userId", event.getUserId(),
                    "error", ex.getMessage()
                ));
                throw new RuntimeException("Failed to start saga: " + ex.getMessage(), ex);
            }

            LoggingConfiguration.logStructured("User registration integration event processing completed", Map.of(
                "eventType", "USER_REGISTRATION_COMPLETED",
                "userId", event.getUserId(),
                "fullName", event.getName()
            ));

        } catch (Exception e) {
            // Log full error and rethrow to allow caller to handle transaction properly
            LoggingConfiguration.logError("billing_user_registration", "UNHANDLED_HANDLER_EXCEPTION", e.getMessage(), e, Map.of(
                "eventId", event.getId(),
                "userId", event.getUserId()
            ));
            throw e;
        }
    }

    @Override
    public Class<UserRegisteredIntegrationEvent> getEventClass() {
        return UserRegisteredIntegrationEvent.class;
    }

    @Override
    public String getHandlerName() {
        return "billingUserRegisteredEventHandler";
    }
}
