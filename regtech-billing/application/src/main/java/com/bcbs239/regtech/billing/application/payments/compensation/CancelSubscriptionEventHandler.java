package com.bcbs239.regtech.billing.application.payments.compensation;

import com.bcbs239.regtech.billing.domain.subscriptions.StripeSubscriptionId;
import com.bcbs239.regtech.billing.domain.subscriptions.Subscription;
import com.bcbs239.regtech.billing.domain.subscriptions.SubscriptionRepository;
import com.bcbs239.regtech.core.domain.shared.Maybe;
import com.bcbs239.regtech.core.domain.shared.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final Logger log = LoggerFactory.getLogger(CancelSubscriptionEventHandler.class);

    public CancelSubscriptionEventHandler(
            SubscriptionRepository subscriptionRepository) {
        this.subscriptionRepository = subscriptionRepository;
    }

    @EventListener
    @Async("sagaTaskExecutor")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void handle(CancelSubscriptionEvent event) {
        log.info("CANCEL_SUBSCRIPTION_COMPENSATION_STARTED; details={}", Map.of(
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
                log.error("INVALID_STRIPE_SUBSCRIPTION_ID; details={}", Map.of(
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
                
                log.info("SUBSCRIPTION_CANCELLED_SUCCESSFULLY; details={}", Map.of(
                    "sagaId", event.sagaId(),
                    "stripeSubscriptionId", event.stripeSubscriptionId(),
                    "subscriptionId", subscription.getId().value()
                ));
            } else {
                log.info("SUBSCRIPTION_NOT_FOUND_FOR_CANCELLATION; details={}", Map.of(
                    "sagaId", event.sagaId(),
                    "stripeSubscriptionId", event.stripeSubscriptionId()
                ));
            }

        } catch (Exception e) {
            log.error("SUBSCRIPTION_CANCELLATION_EXCEPTION; details={}", Map.of(
                "sagaId", event.sagaId(),
                "subscriptionId", event.stripeSubscriptionId()
            ), e);
        }
    }
}
