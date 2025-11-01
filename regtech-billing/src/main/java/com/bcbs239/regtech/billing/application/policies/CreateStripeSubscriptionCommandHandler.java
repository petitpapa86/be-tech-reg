package com.bcbs239.regtech.billing.application.policies;

import com.bcbs239.regtech.billing.domain.billing.BillingAccount;
import com.bcbs239.regtech.billing.domain.valueobjects.BillingAccountId;
import com.bcbs239.regtech.billing.domain.events.StripeSubscriptionCreatedEvent;
import com.bcbs239.regtech.billing.domain.subscriptions.Subscription;
import com.bcbs239.regtech.billing.domain.subscriptions.SubscriptionId;
import com.bcbs239.regtech.billing.domain.subscriptions.SubscriptionTier;
import com.bcbs239.regtech.billing.domain.subscriptions.StripeSubscriptionId;
import com.bcbs239.regtech.billing.domain.valueobjects.StripeCustomerId;
import com.bcbs239.regtech.billing.infrastructure.external.stripe.StripeService;
import com.bcbs239.regtech.billing.infrastructure.external.stripe.StripeSubscription;
import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.core.shared.Maybe;
import com.bcbs239.regtech.iam.domain.users.UserId;

import java.util.function.Function;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Command handler for creating Stripe subscriptions.
 * Handles CreateStripeSubscriptionCommand and publishes StripeSubscriptionCreatedEvent.
 */
@Component
public class CreateStripeSubscriptionCommandHandler {

    private final StripeService stripeService;
    private final Function<UserId, Maybe<BillingAccount>> billingAccountByUserFinder;
    private final Function<BillingAccount, Result<BillingAccountId>> billingAccountSaver;
    private final Function<BillingAccountId, Function<SubscriptionTier, Maybe<Subscription>>> subscriptionByBillingAccountAndTierFinder;
    private final Function<Subscription, Result<SubscriptionId>> subscriptionSaver;
    private final BillingEventPublisher eventPublisher;

    public CreateStripeSubscriptionCommandHandler(
            StripeService stripeService,
            Function<UserId, Maybe<BillingAccount>> billingAccountByUserFinder,
            Function<BillingAccount, Result<BillingAccountId>> billingAccountSaver,
            Function<BillingAccountId, Function<SubscriptionTier, Maybe<Subscription>>> subscriptionByBillingAccountAndTierFinder,
            Function<Subscription, Result<SubscriptionId>> subscriptionSaver,
            BillingEventPublisher eventPublisher) {
        this.stripeService = stripeService;
        this.billingAccountByUserFinder = billingAccountByUserFinder;
        this.billingAccountSaver = billingAccountSaver;
        this.subscriptionByBillingAccountAndTierFinder = subscriptionByBillingAccountAndTierFinder;
        this.subscriptionSaver = subscriptionSaver;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Handle the CreateStripeSubscriptionCommand
     */
    @EventListener
    @Async("sagaTaskExecutor")
    public void handle(CreateStripeSubscriptionCommand command) {
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
        Result<StripeSubscription> subscriptionResult = stripeService.createSubscription(
            customerId,
            command.getSubscriptionTier()
        );
        if (subscriptionResult.isFailure()) {
            // TODO: Handle failure - perhaps publish a failure event or log
            return;
        }

        StripeSubscription stripeSubscription = subscriptionResult.getValue().get();
        StripeSubscriptionId stripeSubscriptionId = stripeSubscription.subscriptionId();

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
        Result<com.bcbs239.regtech.billing.domain.valueobjects.BillingAccountId> saveAccountResult = billingAccountSaver.apply(billingAccount);
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

        // Publish StripeSubscriptionCreatedEvent
        eventPublisher.publishEvent(new StripeSubscriptionCreatedEvent(
            command.getSagaId(),
            stripeSubscriptionId.value(),
            stripeSubscription.latestInvoiceId().value(),
            subscriptionId
        ));
    }
}