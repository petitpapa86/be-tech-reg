package com.bcbs239.regtech.billing.application.subscriptions;

import com.bcbs239.regtech.billing.domain.accounts.BillingAccount;
import com.bcbs239.regtech.billing.domain.accounts.BillingAccountId;
import com.bcbs239.regtech.billing.domain.accounts.BillingAccountRepository;
import com.bcbs239.regtech.billing.domain.payments.PaymentService;
import com.bcbs239.regtech.billing.domain.payments.StripeCustomerId;
import com.bcbs239.regtech.billing.domain.subscriptions.StripeSubscriptionId;
import com.bcbs239.regtech.billing.domain.subscriptions.Subscription;
import com.bcbs239.regtech.billing.domain.subscriptions.SubscriptionId;
import com.bcbs239.regtech.billing.domain.subscriptions.SubscriptionTier;
import com.bcbs239.regtech.billing.domain.subscriptions.events.StripeSubscriptionCreatedEvent;
import com.bcbs239.regtech.billing.domain.subscriptions.SubscriptionRepository;

import com.bcbs239.regtech.core.application.saga.SagaManager;
import com.bcbs239.regtech.core.domain.logging.ILogger;
import com.bcbs239.regtech.core.domain.shared.Maybe;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.core.infrastructure.eventprocessing.CrossModuleEventBus;
import com.bcbs239.regtech.iam.domain.users.UserId;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Command handler for creating Stripe subscriptions.
 * Handles CreateStripeSubscriptionCommand and publishes StripeSubscriptionCreatedEvent.
 */
@Component
public class CreateStripeSubscriptionCommandHandler {

    private final PaymentService paymentService;
    private final BillingAccountRepository billingAccountRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final CrossModuleEventBus crossModuleEventBus;
    private final SagaManager sagaManager;
    private final ILogger asyncLogger;

    public CreateStripeSubscriptionCommandHandler(
            PaymentService paymentService,
            BillingAccountRepository billingAccountRepository,
            SubscriptionRepository subscriptionRepository,
            CrossModuleEventBus crossModuleEventBus,
            SagaManager sagaManager, ILogger asyncLogger
    ) {
        this.paymentService = paymentService;
        this.billingAccountRepository = billingAccountRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.crossModuleEventBus = crossModuleEventBus;
        this.sagaManager = sagaManager;
        this.asyncLogger = asyncLogger;
    }

    /**
     * Handle the CreateStripeSubscriptionCommand
     * NOT_SUPPORTED ensures no transaction context exists when calling repository methods
     * that use REQUIRES_NEW propagation.
     */
    @EventListener
    @Async("sagaTaskExecutor")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void handle(CreateStripeSubscriptionCommand command) {
        // Diagnostic log to confirm the command arrived at the handler
        try {
            asyncLogger.asyncStructuredLog("CREATE_STRIPE_SUBSCRIPTION_COMMAND_RECEIVED", Map.of(
                "sagaId", command.sagaId(),
                "stripeCustomerId", command.getStripeCustomerId(),
                "userId", command.getUserId(),
                "subscriptionTier", command.getSubscriptionTier(),
                "paymentMethodId", command.getPaymentMethodId()
            ));
        } catch (Exception e) {
            // ignore logging failures
        }

        // Get existing billing account
        com.bcbs239.regtech.iam.domain.users.UserId iamUserId = com.bcbs239.regtech.iam.domain.users.UserId.fromString(command.getUserId());
        Maybe<BillingAccount> billingAccountMaybe = billingAccountRepository.findByUserId(iamUserId);
        if (billingAccountMaybe.isEmpty()) {
            asyncLogger.asyncStructuredErrorLog("BILLING_ACCOUNT_NOT_FOUND", null, Map.of(
                "sagaId", command.sagaId(),
                "userId", command.getUserId()
            ));
            return;
        }

        BillingAccount billingAccount = billingAccountMaybe.getValue();

        // Update billing account with Stripe customer ID
        StripeCustomerId customerId = new StripeCustomerId(command.getStripeCustomerId());
        Result<Void> updateResult = billingAccount.updateStripeCustomerId(customerId);
        if (updateResult.isFailure()) {
            asyncLogger.asyncStructuredErrorLog("UPDATE_STRIPE_CUSTOMER_ID_FAILED", null, Map.of(
                "sagaId", command.sagaId(),
                "error", updateResult.getError()
            ));
            return;
        }

        // Resolve PaymentMethodId from command (if provided)
        com.bcbs239.regtech.billing.domain.payments.PaymentMethodId paymentMethodId = null;
        try {
            paymentMethodId = com.bcbs239.regtech.billing.domain.payments.PaymentMethodId.fromString(command.getPaymentMethodId()).getValue().orElse(null);
        } catch (Exception ignored) {
            // leave as null
        }

        // Create Stripe subscription
        PaymentService.SubscriptionCreationRequest subscriptionRequest = new PaymentService.SubscriptionCreationRequest(
            customerId,
            command.getSubscriptionTier(),
            paymentMethodId
        );
        
        Result<PaymentService.SubscriptionCreationResult> subscriptionResult = paymentService.createSubscription(subscriptionRequest);
        if (subscriptionResult.isFailure()) {
            String errorMsg = subscriptionResult.getError()
                .map(err -> err.getMessage())
                .orElse("Unknown error");
            asyncLogger.asyncStructuredErrorLog("CREATE_SUBSCRIPTION_FAILED", null, Map.of(
                "sagaId", command.sagaId(),
                "stripeCustomerId", command.getStripeCustomerId(),
                "errorMessage", errorMsg,
                "subscriptionTier", command.getSubscriptionTier()
            ));
            return;
        }

        PaymentService.SubscriptionCreationResult stripeSubscription = subscriptionResult.getValue().get();
        Result<StripeSubscriptionId> stripeSubscriptionIdResult = StripeSubscriptionId.fromString(stripeSubscription.subscriptionId());
        if (stripeSubscriptionIdResult.isFailure()) {
            asyncLogger.asyncStructuredErrorLog("INVALID_STRIPE_SUBSCRIPTION_ID", null, Map.of(
                "sagaId", command.sagaId(),
                "subscriptionId", stripeSubscription.subscriptionId(),
                "error", stripeSubscriptionIdResult.getError()
            ));
            return;
        }
        StripeSubscriptionId stripeSubscriptionId = stripeSubscriptionIdResult.getValue().get();

        // Find existing subscription to update (should be in PENDING status)
        BillingAccountId billingAccountId = billingAccount.getId();
        
        // Debug: Log what we're looking for
        asyncLogger.asyncStructuredLog("SEARCHING_FOR_PENDING_SUBSCRIPTION", Map.of(
            "sagaId", command.sagaId(),
            "billingAccountId", billingAccountId.value()
        ));
        
        Maybe<Subscription> subscriptionMaybe = subscriptionRepository.findActiveByBillingAccountId(billingAccountId);
        if (subscriptionMaybe.isEmpty()) {
            asyncLogger.asyncStructuredErrorLog("PENDING_SUBSCRIPTION_NOT_FOUND", null, Map.of(
                "sagaId", command.sagaId(),
                "billingAccountId", billingAccountId.value()
            ));
            return;
        }

        Subscription subscription = subscriptionMaybe.getValue();

        // Update subscription with Stripe subscription ID
        Result<Void> updateSubscriptionResult = subscription.updateStripeSubscriptionId(stripeSubscriptionId);
        if (updateSubscriptionResult.isFailure()) {
            asyncLogger.asyncStructuredErrorLog("UPDATE_STRIPE_SUBSCRIPTION_ID_FAILED", null, Map.of(
                "sagaId", command.sagaId(),
                "error", updateSubscriptionResult.getError()
            ));
            return;
        }
        
        // Activate subscription after successful Stripe subscription creation
        Result<Void> activateResult = subscription.activate();
        if (activateResult.isFailure()) {
            asyncLogger.asyncStructuredErrorLog("SUBSCRIPTION_ACTIVATION_FAILED", null, Map.of(
                "sagaId", command.sagaId(),
                "error", activateResult.getError()
            ));
            return;
        }

        // Save updated billing account (use update since it already has an ID)
        Result<BillingAccountId> saveAccountResult;
        try {
            asyncLogger.asyncStructuredLog("UPDATING_BILLING_ACCOUNT", Map.of(
                "sagaId", command.sagaId(),
                "billingAccountId", billingAccount.getId().value()
            ));
            saveAccountResult = billingAccountRepository.update(billingAccount);
            asyncLogger.asyncStructuredLog("BILLING_ACCOUNT_UPDATED", Map.of(
                "sagaId", command.sagaId(),
                "billingAccountId", billingAccount.getId().value()
            ));
        } catch (Exception e) {
            asyncLogger.asyncStructuredErrorLog("BILLING_ACCOUNT_UPDATE_EXCEPTION", e, Map.of(
                "sagaId", command.sagaId(),
                "billingAccountId", billingAccount.getId().value(),
                "exceptionType", e.getClass().getName(),
                "exceptionMessage", e.getMessage()
            ));
            return;
        }
        
        if (saveAccountResult.isFailure()) {
            String errorMsg = saveAccountResult.getError()
                .map(err -> err.getMessage())
                .orElse("Unknown error");
            asyncLogger.asyncStructuredErrorLog("SAVE_BILLING_ACCOUNT_FAILED", null, Map.of(
                "sagaId", command.sagaId(),
                "errorMessage", errorMsg
            ));
            return;
        }

        // Save subscription (use update since it already has an ID)
        Result<SubscriptionId> saveSubscriptionResult;
        try {
            asyncLogger.asyncStructuredLog("UPDATING_SUBSCRIPTION", Map.of(
                "sagaId", command.sagaId(),
                "subscriptionId", subscription.getId().value(),
                "status", subscription.getStatus().name()
            ));
            saveSubscriptionResult = subscriptionRepository.update(subscription);
            asyncLogger.asyncStructuredLog("SUBSCRIPTION_UPDATED", Map.of(
                "sagaId", command.sagaId(),
                "subscriptionId", subscription.getId().value()
            ));
        } catch (Exception e) {
            asyncLogger.asyncStructuredErrorLog("SUBSCRIPTION_UPDATE_EXCEPTION", e, Map.of(
                "sagaId", command.sagaId(),
                "subscriptionId", subscription.getId().value(),
                "exceptionType", e.getClass().getName(),
                "exceptionMessage", e.getMessage()
            ));
            return;
        }
        
        if (saveSubscriptionResult.isFailure()) {
            String errorMsg = saveSubscriptionResult.getError()
                .map(err -> err.getMessage())
                .orElse("Unknown error");
            asyncLogger.asyncStructuredErrorLog("SAVE_SUBSCRIPTION_FAILED", null, Map.of(
                "sagaId", command.sagaId(),
                "errorMessage", errorMsg
            ));
            return;
        }

        SubscriptionId subscriptionId = saveSubscriptionResult.getValue().get();

        // Notify saga of subscription creation: let SagaManager process event so commands are dispatched
        StripeSubscriptionCreatedEvent ev = new StripeSubscriptionCreatedEvent(
            command.sagaId(),
            stripeSubscriptionId.value(),
            "in_" + stripeSubscriptionId.value(), // Mock invoice ID - would need to be provided by payment service
            subscriptionId
        );

        try {
            sagaManager.processEvent(ev);
            asyncLogger.asyncStructuredLog("STRIPE_SUBSCRIPTION_CREATED_PROCESSED_BY_SAGAMANAGER", Map.of(
                "sagaId", command.sagaId(),
                "stripeSubscriptionId", stripeSubscriptionId.value()
            ));
        } catch (Exception e) {
            asyncLogger.asyncStructuredErrorLog("SAGA_MANAGER_PROCESS_FAILED", e, Map.of(
                "sagaId", command.sagaId(),
                "error", e.getMessage()
            ));
            crossModuleEventBus.publishEventSynchronously(ev);
        }
    }
}
