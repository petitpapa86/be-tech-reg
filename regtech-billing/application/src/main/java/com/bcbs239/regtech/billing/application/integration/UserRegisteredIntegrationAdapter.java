package com.bcbs239.regtech.billing.application.integration;

import com.bcbs239.regtech.core.domain.events.DomainEventBus;
import com.bcbs239.regtech.core.domain.events.integration.UserRegisteredIntegrationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Adapter that converts shared UserRegisteredIntegrationEvent from the inbox
 * into a local BillingUserRegisteredEvent for the billing bounded context.
 * 
 * This allows the billing module to:
 * - Keep its internal domain model decoupled from other modules
 * - Process events through its own domain event handlers
 * - Avoid circular dependencies on integration events
 */
@Component("billingUserRegisteredIntegrationAdapter")
public class UserRegisteredIntegrationAdapter {

    private final DomainEventBus domainEventBus;
    private static final Logger log = LoggerFactory.getLogger(UserRegisteredIntegrationAdapter.class);

    public UserRegisteredIntegrationAdapter(DomainEventBus domainEventBus) {
        this.domainEventBus = domainEventBus;
    }

    @EventListener
    public void onIntegrationEvent(UserRegisteredIntegrationEvent integrationEvent) {
        log.info("Adapting UserRegisteredIntegrationEvent to BillingUserRegisteredEvent; details={}", Map.of(
            "eventType", "INTEGRATION_EVENT_ADAPTER",
            "integrationEventId", integrationEvent.getEventId(),
            "userId", integrationEvent.getUserId(),
            "correlationId", integrationEvent.getCorrelationId()
        ));

        // Convert shared integration event to local billing domain event
        BillingUserRegisteredEvent billingEvent = new BillingUserRegisteredEvent(
            integrationEvent.getCorrelationId(),
            integrationEvent.getCausationId().orElse(null),
            integrationEvent.getUserId(),
            integrationEvent.getEmail(),
            integrationEvent.getBankId(),
            integrationEvent.getPaymentMethodId()
        );

        // Publish as replay so existing billing handlers receive it
        domainEventBus.publishAsReplay(billingEvent);

        log.info("Published BillingUserRegisteredEvent to domain event bus; details={}", Map.of(
            "eventType", "BILLING_EVENT_PUBLISHED",
            "eventId", billingEvent.getEventId(),
            "userId", billingEvent.getUserId()
        ));
    }
}
