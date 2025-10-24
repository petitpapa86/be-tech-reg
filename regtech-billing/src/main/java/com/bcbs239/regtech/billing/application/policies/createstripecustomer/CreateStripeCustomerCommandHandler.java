package com.bcbs239.regtech.billing.application.policies.createstripecustomer;

import com.bcbs239.regtech.billing.domain.events.StripeCustomerCreatedEvent;
import com.bcbs239.regtech.billing.domain.valueobjects.PaymentMethodId;
import com.bcbs239.regtech.billing.infrastructure.external.stripe.StripeCustomer;
import com.bcbs239.regtech.billing.infrastructure.external.stripe.StripeService;
import com.bcbs239.regtech.billing.infrastructure.messaging.BillingEventPublisher;
import com.bcbs239.regtech.core.shared.Result;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Command handler for creating Stripe customers.
 * Handles CreateStripeCustomerCommand and publishes StripeCustomerCreatedEvent.
 */
@Component
public class CreateStripeCustomerCommandHandler {

    private final StripeService stripeService;
    private final BillingEventPublisher eventPublisher;

    public CreateStripeCustomerCommandHandler(
            StripeService stripeService,
            BillingEventPublisher eventPublisher) {
        this.stripeService = stripeService;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Handle the CreateStripeCustomerCommand
     */
    @EventListener
    @Async("sagaTaskExecutor")
    public void handle(CreateStripeCustomerCommand command) {
        // Create PaymentMethodId from string
        PaymentMethodId paymentMethodId = new PaymentMethodId(command.getPaymentMethodId());

        // Create Stripe customer
        Result<StripeCustomer> customerResult = stripeService.createCustomer(
            command.getUserEmail(),
            command.getUserName()
        );
        if (customerResult.isFailure()) {
            // TODO: Handle failure - perhaps publish a failure event or log
            return;
        }

        StripeCustomer customer = customerResult.getValue().get();

        // Attach payment method
        Result<Void> attachResult = stripeService.attachPaymentMethod(
            customer.customerId(),
            paymentMethodId
        );
        if (attachResult.isFailure()) {
            // TODO: Handle failure
            return;
        }

        // Set as default payment method
        Result<Void> defaultResult = stripeService.setDefaultPaymentMethod(
            customer.customerId(),
            paymentMethodId
        );
        if (defaultResult.isFailure()) {
            // TODO: Handle failure
            return;
        }

        // Publish StripeCustomerCreatedEvent
        eventPublisher.publishEvent(new StripeCustomerCreatedEvent(
            command.getSagaId(),
            customer.customerId().value()
        ));
    }
}