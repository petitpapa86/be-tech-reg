package com.bcbs239.regtech.billing.application.handlers;

import com.bcbs239.regtech.billing.domain.events.StripeCustomerCreatedEvent;
import com.bcbs239.regtech.billing.domain.events.StripeInvoiceCreatedEvent;
import com.bcbs239.regtech.billing.domain.events.StripePaymentFailedEvent;
import com.bcbs239.regtech.billing.domain.events.StripePaymentSucceededEvent;
import com.bcbs239.regtech.billing.domain.events.StripeSubscriptionCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Event handler for Stripe webhook events.
 * Bridges Stripe webhook processing to saga event handling.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class StripeWebhookEventHandler {
    private final ApplicationEventPublisher eventPublisher;

    @EventListener
    public void handle(StripeCustomerCreatedEvent event) {
        log.info("Processing StripeCustomerCreatedEvent for saga: {}", event.getSagaId());
        eventPublisher.publishEvent(event);
    }

    @EventListener
    public void handle(StripeSubscriptionCreatedEvent event) {
        log.info("Processing StripeSubscriptionCreatedEvent for saga: {}", event.getSagaId());
        eventPublisher.publishEvent(event);
    }

    @EventListener
    public void handle(StripeInvoiceCreatedEvent event) {
        log.info("Processing StripeInvoiceCreatedEvent for saga: {}", event.getSagaId());
        eventPublisher.publishEvent(event);
    }

    @EventListener
    public void handle(StripePaymentSucceededEvent event) {
        log.info("Processing StripePaymentSucceededEvent for saga: {}", event.getSagaId());
        eventPublisher.publishEvent(event);
    }

    @EventListener
    public void handle(StripePaymentFailedEvent event) {
        log.info("Processing StripePaymentFailedEvent for saga: {}", event.getSagaId());
        eventPublisher.publishEvent(event);
    }
}