package com.bcbs239.regtech.billing.application.events;

import com.bcbs239.regtech.billing.application.policies.PaymentVerificationSaga;
import com.bcbs239.regtech.billing.domain.billing.PaymentVerificationSagaData;
import com.bcbs239.regtech.billing.domain.billing.UserId;
import com.bcbs239.regtech.core.events.DomainEventHandler;
import com.bcbs239.regtech.core.events.UserRegisteredIntegrationEvent;
import com.bcbs239.regtech.core.saga.SagaId;
import com.bcbs239.regtech.core.saga.SagaManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Integration event handler for UserRegisteredIntegrationEvent from IAM context.
 * Processes user registration events through the shared inbox infrastructure for reliable delivery.
 * Starts the PaymentVerificationSaga when a user registers.
 */
@Component("billingUserRegisteredIntegrationEventHandler")
public class UserRegisteredEventHandler implements DomainEventHandler<UserRegisteredIntegrationEvent> {

    private static final Logger logger = LoggerFactory.getLogger(UserRegisteredEventHandler.class);

    private final SagaManager sagaManager;

    public UserRegisteredEventHandler(SagaManager sagaManager) {
        this.sagaManager = sagaManager;
    }

    @Override
    public String eventType() {
        return UserRegisteredIntegrationEvent.class.getSimpleName();
    }

    @Override
    public Class<UserRegisteredIntegrationEvent> eventClass() {
        return UserRegisteredIntegrationEvent.class;
    }

    @Override
    public boolean handle(UserRegisteredIntegrationEvent event) {
        logger.info("Processing UserRegisteredIntegrationEvent for user: {} ({}), email: {}, bankId: {}",
            event.getUserId(),
            event.getName(),
            event.getEmail(),
            event.getBankId());

        try {
            // Create saga data from the integration event
            PaymentVerificationSagaData sagaData = PaymentVerificationSagaData.builder()
                .correlationId(event.getId().toString())
                .userId(new UserId(event.getUserId()))
                .userEmail(event.getEmail())
                .userName(event.getName())
                .paymentMethodId(event.getPaymentMethodId())
                .build();

            // Start the PaymentVerificationSaga
            SagaId sagaId = sagaManager.startSaga(PaymentVerificationSaga.class, sagaData);

            logger.info("Started PaymentVerificationSaga with ID: {} for user: {}", sagaId, event.getUserId());

            logger.info("User registration integration event processing completed for billing: userId={}, fullName={}",
                event.getUserId(), event.getName());

            return true;
        } catch (Exception e) {
            logger.error("Unexpected error processing UserRegisteredIntegrationEvent for user {}: {}",
                event.getUserId(), e.getMessage(), e);
            return false;
        }
    }
}