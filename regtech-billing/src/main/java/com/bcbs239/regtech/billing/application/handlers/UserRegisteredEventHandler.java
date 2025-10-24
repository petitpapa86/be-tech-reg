package com.bcbs239.regtech.billing.application.handlers;

import com.bcbs239.regtech.billing.application.policies.PaymentVerificationSaga;
import com.bcbs239.regtech.billing.domain.billing.PaymentVerificationSagaData;
import com.bcbs239.regtech.billing.domain.billing.UserId;
import com.bcbs239.regtech.core.events.UserRegisteredEvent;
import com.bcbs239.regtech.core.saga.SagaId;
import com.bcbs239.regtech.core.saga.SagaManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Event handler for UserRegisteredEvent.
 * Starts the PaymentVerificationSaga when a user registers.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class UserRegisteredEventHandler {
    private final SagaManager sagaManager;

    @EventListener
    public void handle(UserRegisteredEvent event) {
        log.info("Received UserRegisteredEvent for user: {}", event.getEmail());

        PaymentVerificationSagaData sagaData = PaymentVerificationSagaData.builder()
                .correlationId(event.getCorrelationId())
                .userId(new UserId(event.getUserId()))
                .userEmail(event.getEmail())
                .userName(event.getName())
                .paymentMethodId(event.getPaymentMethodId())
                .build();

        SagaId sagaId = sagaManager.startSaga(PaymentVerificationSaga.class, sagaData);
        log.info("Started PaymentVerificationSaga: {} for user: {}", sagaId, event.getEmail());
    }
}