package com.bcbs239.regtech.billing.application.payments.compensation;

import com.bcbs239.regtech.billing.domain.subscriptions.Subscription;
import com.bcbs239.regtech.billing.domain.subscriptions.SubscriptionRepository;
import com.bcbs239.regtech.billing.domain.subscriptions.StripeSubscriptionId;
import com.bcbs239.regtech.core.domain.logging.ILogger;
import com.bcbs239.regtech.core.domain.shared.Maybe;
import com.bcbs239.regtech.core.domain.shared.Result;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Map;

/**
 * Handles subscription cancellation events during saga compensation.
 * Executes asynchronously to cancel subscriptions via Stripe API and update local database.
 */
@Component
public class CancelSubscriptionEventHandler {

    private final SubscriptionRepository subscriptionRepository;
    private final ILogger asyncLogger;

    public CancelSubscriptionEventHandler(
            SubscriptionRepository subscriptionRepository,
            ILogger asyncLogger) {
        this.subscriptionRepository = subscriptionRepository;
        this.asyncLogger = asyncLogger;
    }

    @EventListener
    @Async("sagaTaskExecutor")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void handle(CancelSubscriptionEvent event) {
        asyncLogger.asyncStructuredLog("CANCEL_SUBSCRIPTION_COMPENSATION_STARTED", Map.of(
            "sagaId", event.sagaId(),
            "subscriptionId", event.stripeSubscriptionId(),
            "userId", event.userId(),
            "reason", event.reason()
        ));

        try {
            // TODO: Cancel in Stripe first
            // Result<Void> stripeCancelResult = paymentService.cancelSubscription(event.stripeSubscriptionId());
            
            // Update local subscription status
            Result<StripeSubscriptionId> stripeIdResult = StripeSubscriptionId.fromString(event.stripeSubscriptionId());
            if (stripeIdResult.isFailure()) {
                asyncLogger.asyncStructuredErrorLog("INVALID_STRIPE_SUBSCRIPTION_ID", null, Map.of(
                    "sagaId", event.sagaId(),
                    "subscriptionId", event.stripeSubscriptionId()
                ));
                return;
            }

            Maybe<Subscription> subscriptionMaybe = subscriptionRepository.findByStripeSubscriptionId(stripeIdResult.getValue().get());
            
            if (subscriptionMaybe.isPresent()) {
                Subscription subscription = subscriptionMaybe.getValue();
                subscription.cancel(LocalDate.now());
                subscriptionRepository.save(subscription);
                
                asyncLogger.asyncStructuredLog("SUBSCRIPTION_CANCELLED_SUCCESSFULLY", Map.of(
                    "sagaId", event.sagaId(),
                    "stripeSubscriptionId", event.stripeSubscriptionId(),
                    "subscriptionId", subscription.getId().value()
                ));
            } else {
                asyncLogger.asyncStructuredLog("SUBSCRIPTION_NOT_FOUND_FOR_CANCELLATION", Map.of(
                    "sagaId", event.sagaId(),
                    "stripeSubscriptionId", event.stripeSubscriptionId()
                ));
            }

        } catch (Exception e) {
            asyncLogger.asyncStructuredErrorLog("SUBSCRIPTION_CANCELLATION_EXCEPTION", e, Map.of(
                "sagaId", event.sagaId(),
                "subscriptionId", event.stripeSubscriptionId()
            ));
        }
    }
}
