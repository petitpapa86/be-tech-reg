package com.bcbs239.regtech.billing.application.subscriptions;

import com.bcbs239.regtech.billing.domain.accounts.BillingAccount;
import com.bcbs239.regtech.billing.domain.accounts.BillingAccountId;
import com.bcbs239.regtech.billing.domain.payments.PaymentService;
import com.bcbs239.regtech.billing.domain.payments.StripeCustomerId;
import com.bcbs239.regtech.billing.domain.subscriptions.StripeSubscriptionId;
import com.bcbs239.regtech.billing.domain.subscriptions.Subscription;
import com.bcbs239.regtech.billing.domain.subscriptions.SubscriptionId;
import com.bcbs239.regtech.billing.domain.subscriptions.SubscriptionTier;
import com.bcbs239.regtech.billing.domain.subscriptions.events.StripeSubscriptionCreatedEvent;

import com.bcbs239.regtech.core.application.saga.SagaManager;
import com.bcbs239.regtech.core.domain.logging.ILogger;
import com.bcbs239.regtech.core.domain.shared.Maybe;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.core.infrastructure.eventprocessing.CrossModuleEventBus;
import com.bcbs239.regtech.iam.domain.users.UserId;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.function.Function;

/**
 * Command handler for creating Stripe subscriptions.
 * Handles CreateStripeSubscriptionCommand and publishes StripeSubscriptionCreatedEvent.
 */
@Component
public class CreateStripeSubscriptionCommandHandler {

    private final PaymentService paymentService;
    private final Function<UserId, Maybe<BillingAccount>> billingAccountByUserFinder;
    private final Function<BillingAccount, Result<BillingAccountId>> billingAccountSaver;
    private final Function<BillingAccountId, Function<SubscriptionTier, Maybe<Subscription>>> subscriptionByBillingAccountAndTierFinder;
    private final Function<Subscription, Result<SubscriptionId>> subscriptionSaver;
    private final CrossModuleEventBus crossModuleEventBus;
    private final SagaManager sagaManager;
    private final ILogger asyncLogger;

    public CreateStripeSubscriptionCommandHandler(
            PaymentService paymentService,
            Function<UserId, Maybe<BillingAccount>> billingAccountByUserFinder,
            Function<BillingAccount, Result<BillingAccountId>> billingAccountSaver,
            Function<BillingAccountId, Function<SubscriptionTier, Maybe<Subscription>>> subscriptionByBillingAccountAndTierFinder,
            Function<Subscription, Result<SubscriptionId>> subscriptionSaver,
            CrossModuleEventBus crossModuleEventBus,
            SagaManager sagaManager, ILogger asyncLogger
    ) {
        this.paymentService = paymentService;
        this.billingAccountByUserFinder = billingAccountByUserFinder;
        this.billingAccountSaver = billingAccountSaver;
        this.subscriptionByBillingAccountAndTierFinder = subscriptionByBillingAccountAndTierFinder;
        this.subscriptionSaver = subscriptionSaver;
        this.crossModuleEventBus = crossModuleEventBus;
        this.sagaManager = sagaManager;
        this.asyncLogger = asyncLogger;
    }

    /**
     * Handle the CreateStripeSubscriptionCommand
     */
    @EventListener
    @Async("sagaTaskExecutor")
    public void handle(CreateStripeSubscriptionCommand command) {
        // Diagnostic log to confirm the command arrived at the handler
        try {
            asyncLogger.asyncStructuredLog("CREATE_STRIPE_SUBSCRIPTION_COMMAND_RECEIVED", Map.of(
                "sagaId", command.sagaId(),
                "stripeCustomerId", command.getStripeCustomerId(),
                "userId", command.getUserId(),
                "subscriptionTier", command.getSubscriptionTier()
            ));
        } catch (Exception e) {
            // ignore logging failures
        }

        // Get existing billing account
        com.bcbs239.regtech.iam.domain.users.UserId iamUserId = com.bcbs239.regtech.iam.domain.users.UserId.fromString(command.getUserId());
        Maybe<BillingAccount> billingAccountMaybe = billingAccountByUserFinder.apply(iamUserId);
        if (billingAccountMaybe.isEmpty()) {
            // TODO: Handle case where billing account doesn't exist
            return;
        }

        BillingAccount billingAccount = billingAccountMaybe.getValue();

        // Update billing account with Stripe customer ID
        StripeCustomerId customerId = new StripeCustomerId(command.getStripeCustomerId());
        Result<Void> updateResult = billingAccount.updateStripeCustomerId(customerId);
        if (updateResult.isFailure()) {
            // TODO: Handle failure
            return;
        }

        // Create Stripe subscription
        PaymentService.SubscriptionCreationRequest subscriptionRequest = new PaymentService.SubscriptionCreationRequest(
            customerId,
            command.getSubscriptionTier(),
            null // PaymentMethodId - would need to be provided in command
        );
        
        Result<PaymentService.SubscriptionCreationResult> subscriptionResult = paymentService.createSubscription(subscriptionRequest);
        if (subscriptionResult.isFailure()) {
            // TODO: Handle failure - perhaps publish a failure event or log
            return;
        }

        PaymentService.SubscriptionCreationResult stripeSubscription = subscriptionResult.getValue().get();
        Result<StripeSubscriptionId> stripeSubscriptionIdResult = StripeSubscriptionId.fromString(stripeSubscription.subscriptionId());
        if (stripeSubscriptionIdResult.isFailure()) {
            // TODO: Handle failure
            return;
        }
        StripeSubscriptionId stripeSubscriptionId = stripeSubscriptionIdResult.getValue().get();

        // Find existing subscription to update
        BillingAccountId billingAccountId = billingAccount.getId();
        Maybe<Subscription> subscriptionMaybe = subscriptionByBillingAccountAndTierFinder.apply(billingAccountId).apply(command.getSubscriptionTier());
        if (subscriptionMaybe.isEmpty()) {
            // TODO: Handle case where subscription doesn't exist
            return;
        }

        Subscription subscription = subscriptionMaybe.getValue();

        // Update subscription with Stripe subscription ID
        Result<Void> updateSubscriptionResult = subscription.updateStripeSubscriptionId(stripeSubscriptionId);
        if (updateSubscriptionResult.isFailure()) {
            // TODO: Handle failure
            return;
        }

        // Save updated billing account
        Result<BillingAccountId> saveAccountResult = billingAccountSaver.apply(billingAccount);
        if (saveAccountResult.isFailure()) {
            // TODO: Handle failure
            return;
        }

        // Save subscription
        Result<SubscriptionId> saveSubscriptionResult = subscriptionSaver.apply(subscription);
        if (saveSubscriptionResult.isFailure()) {
            // TODO: Handle failure
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

