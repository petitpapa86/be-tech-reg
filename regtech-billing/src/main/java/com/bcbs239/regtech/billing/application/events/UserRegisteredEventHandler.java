package com.bcbs239.regtech.billing.application.events;

import com.bcbs239.regtech.billing.application.processpayment.ProcessPaymentCommand;
import com.bcbs239.regtech.billing.application.processpayment.ProcessPaymentCommandHandler;
import com.bcbs239.regtech.billing.application.processpayment.ProcessPaymentResponse;
import com.bcbs239.regtech.billing.infrastructure.messaging.BillingEventPublisher;
import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.core.events.UserRegisteredIntegrationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Event handler for UserRegisteredIntegrationEvent from IAM context.
 * Triggers payment processing in the billing context using proper outbox pattern.
 * Now called asynchronously by InboxProcessorJob instead of direct event listening.
 */
@Component
public class UserRegisteredEventHandler {

    private static final Logger logger = LoggerFactory.getLogger(UserRegisteredEventHandler.class);

    private final ProcessPaymentCommandHandler processPaymentCommandHandler;
    private final BillingEventPublisher billingEventPublisher;

    public UserRegisteredEventHandler(
            ProcessPaymentCommandHandler processPaymentCommandHandler,
            BillingEventPublisher billingEventPublisher) {
        this.processPaymentCommandHandler = processPaymentCommandHandler;
        this.billingEventPublisher = billingEventPublisher;
    }

    /**
     * Handle user registered event by triggering payment processing with proper outbox pattern.
     * Called by InboxProcessorJob for asynchronous processing.
     */
    @Transactional
    public void handle(UserRegisteredIntegrationEvent event) {
        logger.info("Received UserRegisteredEvent for user: {} with correlation: {}", 
            event.getUserId(), event.getCorrelationId());

        try {
            // Create enhanced correlation ID with user data for payment processing
            String enhancedCorrelationId = String.format("%s|userId=%s|email=%s|name=%s|bankId=%s|phone=%s|address=%s",
                event.getCorrelationId(),
                event.getUserId(),
                event.getEmail(),
                event.getName(),
                event.getBankId(),
                event.getPhone() != null ? event.getPhone() : "",
                event.getAddress() != null ? event.getAddress().toString() : ""
            );

            // Create and execute payment processing command
            Result<ProcessPaymentCommand> commandResult = ProcessPaymentCommand.create(
                event.getPaymentMethodId(),
                enhancedCorrelationId
            );

            if (commandResult.isFailure()) {
                logger.error("Failed to create ProcessPaymentCommand for user {}: {}", 
                    event.getUserId(), commandResult.getError().get().getMessage());
                return;
            }

            // Execute payment processing - this will automatically publish events via outbox pattern
            Result<ProcessPaymentResponse> result = 
                processPaymentCommandHandler.handle(commandResult.getValue().get());

            if (result.isSuccess()) {
                logger.info("Successfully processed payment for user: {} with correlation: {} - events published via outbox", 
                    event.getUserId(), event.getCorrelationId());
            } else {
                logger.error("Failed to process payment for user {}: {}", 
                    event.getUserId(), result.getError().get().getMessage());
            }

        } catch (Exception e) {
            logger.error("Unexpected error processing UserRegisteredEvent for user {}: {}", 
                event.getUserId(), e.getMessage(), e);
        }
    }
}
