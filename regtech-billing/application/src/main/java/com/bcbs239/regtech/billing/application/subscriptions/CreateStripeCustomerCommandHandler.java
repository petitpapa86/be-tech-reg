package com.bcbs239.regtech.billing.application.subscriptions;

import com.bcbs239.regtech.core.domain.saga.AbstractSaga;

import com.bcbs239.regtech.billing.application.policies.createstripecustomer.CreateStripeCustomerCommand;
import com.bcbs239.regtech.billing.domain.accounts.BillingAccountRepository;
import com.bcbs239.regtech.billing.domain.payments.PaymentMethodId;
import com.bcbs239.regtech.billing.domain.payments.PaymentService;
import com.bcbs239.regtech.billing.domain.payments.StripeCustomerId;
import com.bcbs239.regtech.billing.domain.payments.events.StripeCustomerCreatedEvent;
import com.bcbs239.regtech.billing.domain.payments.events.StripeCustomerCreationFailedEvent;

import com.bcbs239.regtech.core.domain.logging.ILogger;
import com.bcbs239.regtech.core.domain.saga.SagaId;
import com.bcbs239.regtech.core.domain.shared.Maybe;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.core.infrastructure.eventprocessing.CrossModuleEventBus;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.function.Function;

/**
 * Command handler for creating Stripe customers.
 * Simplified version using PaymentService domain interface.
 */
@Component("railwayCreateStripeCustomerCommandHandler")
@SuppressWarnings("unused")
public class CreateStripeCustomerCommandHandler {

    private final ILogger asyncLogger;

    private final PaymentService paymentService;
    private final CrossModuleEventBus crossModuleEventBus;
    private final Function<SagaId, Maybe<AbstractSaga<?>>> sagaLoader;

    @SuppressWarnings("unused")
    public CreateStripeCustomerCommandHandler(
            ILogger asyncLogger, PaymentService paymentService,
            CrossModuleEventBus crossModuleEventBus,
            Function<SagaId, Maybe<AbstractSaga<?>>> sagaLoader,
            BillingAccountRepository billingAccountRepository) {
        this.asyncLogger = asyncLogger;
        this.paymentService = paymentService;
        this.crossModuleEventBus = crossModuleEventBus;
        this.sagaLoader = sagaLoader;
    }

    @EventListener
    @Async("sagaTaskExecutor")
    public void handle(CreateStripeCustomerCommand command) {
        SagaId sagaId = command.sagaId();
        asyncLogger.asyncStructuredLog("CREATE_STRIPE_CUSTOMER_COMMAND_RECEIVED", Map.of(
                "sagaId", sagaId,
                "userEmail", command.getEmail(),
                "userName", command.getName()
        ));

        // Create customer using PaymentService
        PaymentMethodId paymentMethodId = PaymentMethodId.fromString(command.getPaymentMethodId()).getValue().orElse(null);
        if (paymentMethodId == null) {
            publishFailureEvent(sagaId, "Invalid payment method ID");
            return;
        }

        PaymentService.CustomerCreationRequest request = new PaymentService.CustomerCreationRequest(
            command.getEmail(),
            command.getName(),
            paymentMethodId
        );

        Result<PaymentService.CustomerCreationResult> result = paymentService.createCustomer(request);
        
        if (result.isFailure()) {
            String errorMsg = result.getError().get().getMessage();
            asyncLogger.asyncStructuredLog("STRIPE_CUSTOMER_CREATION_FAILED", Map.of(
                    "sagaId", sagaId,
                    "error", errorMsg
            ));
            publishFailureEvent(sagaId, errorMsg);
            return;
        }

        PaymentService.CustomerCreationResult customer = result.getValue().get();
        
        // Update billing account with Stripe customer ID
        updateBillingAccount(sagaId, customer.customerId());
        
        // Publish success event
        StripeCustomerCreatedEvent event = new StripeCustomerCreatedEvent(sagaId, customer.customerId().getValue());
        crossModuleEventBus.publishEventSynchronously(event);
        
        asyncLogger.asyncStructuredLog("STRIPE_CUSTOMER_CREATED_PUBLISHED", Map.of(
                "sagaId", sagaId,
                "stripeCustomerId", customer.customerId().getValue()
        ));
    }

    private void updateBillingAccount(SagaId sagaId, StripeCustomerId customerId) {
        // Find billing account associated with saga
        Maybe<AbstractSaga<?>> maybeSaga = sagaLoader.apply(sagaId);
        if (maybeSaga.isEmpty()) {
            asyncLogger.asyncStructuredLog("SAGA_NOT_FOUND", Map.of("sagaId", sagaId));
            return;
        }

        // In a real implementation, we would extract the billing account ID from the saga data
        // For now, we'll create a mock implementation
        // This would typically be done through proper saga data access
    }

    private void publishFailureEvent(SagaId sagaId, String errorMessage) {
        StripeCustomerCreationFailedEvent failureEvent = new StripeCustomerCreationFailedEvent(sagaId, errorMessage);
        crossModuleEventBus.publishEventSynchronously(failureEvent);
    }
}

