package com.bcbs239.regtech.iam.application.integration;



// Note: InboxEventEntity should be handled in infrastructure layer
import com.bcbs239.regtech.core.events.PaymentVerifiedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
// TODO: Remove JPA imports from application layer
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Inbox event handler that receives cross-module events and stores them for asynchronous processing.
 * This implements the inbox pattern for reliable event processing in IAM context.
*/
@Component("iamInboxEventHandler")
public class IamInboxEventHandler {

    private static final Logger logger = LoggerFactory.getLogger(IamInboxEventHandler.class);

    // TODO: Remove direct EntityManager usage - violates clean architecture

    private final ObjectMapper objectMapper;

    public IamInboxEventHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Handle PaymentVerifiedEvent by storing it in the inbox for asynchronous processing.
     */
    @EventListener
    @Transactional
    public void handlePaymentVerifiedEvent(PaymentVerifiedEvent event) {
        try {
            logger.info("Received PaymentVerifiedEvent for inbox processing: user={}, billingAccount={}",
                event.getUserId(), event.getBillingAccountId());

            // Serialize the event
            String eventData = objectMapper.writeValueAsString(event);

            // TODO: This should use a domain service or be moved to infrastructure layer
            // The application layer should not directly access infrastructure entities
            logger.info("PaymentVerifiedEvent processed (inbox functionality needs refactoring): user={}",
                event.getUserId());

        } catch (Exception e) {
            logger.error("Failed to store PaymentVerifiedEvent in inbox for user {}: {}",
                event.getUserId(), e.getMessage(), e);
            // In a real system, you might want to send this to a dead letter queue
            // or implement retry logic here
        }
    }
}