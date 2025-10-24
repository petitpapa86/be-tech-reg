package com.bcbs239.regtech.billing.application.policies;

import com.bcbs239.regtech.billing.domain.billing.BillingAccount;
import com.bcbs239.regtech.billing.domain.valueobjects.BillingAccountId;
import com.bcbs239.regtech.billing.domain.events.StripeSubscriptionCreatedEvent;
import com.bcbs239.regtech.billing.domain.subscriptions.Subscription;
import com.bcbs239.regtech.billing.domain.subscriptions.SubscriptionId;
import com.bcbs239.regtech.billing.domain.subscriptions.StripeSubscriptionId;
import com.bcbs239.regtech.billing.domain.valueobjects.StripeCustomerId;
import com.bcbs239.regtech.billing.infrastructure.database.repositories.JpaBillingAccountRepository;
import com.bcbs239.regtech.billing.infrastructure.database.repositories.JpaSubscriptionRepository;
import com.bcbs239.regtech.billing.infrastructure.external.stripe.StripeService;
import com.bcbs239.regtech.billing.infrastructure.external.stripe.StripeSubscription;
import com.bcbs239.regtech.billing.infrastructure.messaging.BillingEventPublisher;
import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.iam.domain.users.UserId;
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
    private final JpaBillingAccountRepository billingAccountRepository;
    private final JpaSubscriptionRepository subscriptionRepository;
    private final BillingEventPublisher eventPublisher;

    public CreateStripeSubscriptionCommandHandler(
            StripeService stripeService,
            JpaBillingAccountRepository billingAccountRepository,
            JpaSubscriptionRepository subscriptionRepository,
            BillingEventPublisher eventPublisher) {
        this.stripeService = stripeService;
        this.billingAccountRepository = billingAccountRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Handle the CreateStripeSubscriptionCommand
     */
    @EventListener
    @Async("sagaTaskExecutor")
    public void handle(CreateStripeSubscriptionCommand command) {
        // Create Stripe subscription
        StripeCustomerId customerId = new StripeCustomerId(command.getStripeCustomerId());
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

        // Create billing account
        UserId userId = UserId.fromString(command.getUserId());
        BillingAccount billingAccount = BillingAccount.create(userId, customerId);

        // Activate billing account (assuming payment method was set in customer creation)
        // For now, we'll skip activation as it requires a payment method ID
        // This might need to be handled differently

        Result<BillingAccountId> saveAccountResult = billingAccountRepository.billingAccountSaver().apply(billingAccount);
        if (saveAccountResult.isFailure()) {
            // TODO: Handle failure
            return;
        }

        BillingAccountId billingAccountId = saveAccountResult.getValue().get();

        // Create subscription domain object
        Subscription subscription = Subscription.create(
            billingAccountId,
            stripeSubscriptionId,
            command.getSubscriptionTier()
        );

        Result<SubscriptionId> saveSubscriptionResult = subscriptionRepository.subscriptionSaver().apply(subscription);
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