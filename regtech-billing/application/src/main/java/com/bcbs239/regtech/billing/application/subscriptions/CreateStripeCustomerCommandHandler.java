package com.bcbs239.regtech.billing.application.subscriptions;

import com.bcbs239.regtech.billing.application.policies.createstripecustomer.CreateStripeCustomerCommand;
import com.bcbs239.billing.BillingAccount;
import com.bcbs239.regtech.billing.domain.events.*;
import com.bcbs239.regtech.billing.domain.valueobjects.BillingAccountId;
import com.bcbs239.regtech.billing.domain.valueobjects.PaymentMethodId;
import com.bcbs239.regtech.billing.domain.valueobjects.StripeCustomerId;
import com.bcbs239.regtech.billing.domain.repositories.BillingAccountRepository;
import com.bcbs239.regtech.billing.domain.services.PaymentService;
import com.bcbs239.regtech.core.saga.AbstractSaga;
import com.bcbs239.regtech.core.saga.SagaId;
import com.bcbs239.regtech.core.shared.ErrorDetail;
import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.core.shared.Maybe;
import com.bcbs239.regtech.core.events.CrossModuleEventBus;
import com.bcbs239.regtech.core.config.LoggingConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.function.Function;
import java.util.Map;

/**
 * Command handler for creating Stripe customers.
 * Simplified version using PaymentService domain interface.
 */
@Component("railwayCreateStripeCustomerCommandHandler")
@SuppressWarnings("unused")
public class CreateStripeCustomerCommandHandler {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(CreateStripeCustomerCommandHandler.class);

    private final PaymentService paymentService;
    private final CrossModuleEventBus crossModuleEventBus;
    private final Function<SagaId, Maybe<AbstractSaga<?>>> sagaLoader;
    private final BillingAccountRepository billingAccountRepository;

    @SuppressWarnings("unused")
    @Autowired
    public CreateStripeCustomerCommandHandler(
            PaymentService paymentService,
            CrossModuleEventBus crossModuleEventBus,
            Function<SagaId, Maybe<AbstractSaga<?>>> sagaLoader,
            BillingAccountRepository billingAccountRepository) {
        this.paymentService = paymentService;
        this.crossModuleEventBus = crossModuleEventBus;
        this.sagaLoader = sagaLoader;
        this.billingAccountRepository = billingAccountRepository;
    }

    @EventListener
    @Async("sagaTaskExecutor")
    public void handle(CreateStripeCustomerCommand command) {
        SagaId sagaId = command.getSagaId();
        LoggingConfiguration.logStructured("CREATE_STRIPE_CUSTOMER_COMMAND_RECEIVED", Map.of(
                "sagaId", sagaId,
                "userEmail", command.getEmail(),
                "userName", command.getName()
        ), null);

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
            LoggingConfiguration.logStructured("STRIPE_CUSTOMER_CREATION_FAILED", Map.of(
                    "sagaId", sagaId,
                    "error", errorMsg
            ), null);
            publishFailureEvent(sagaId, errorMsg);
            return;
        }

        PaymentService.CustomerCreationResult customer = result.getValue().get();
        
        // Update billing account with Stripe customer ID
        updateBillingAccount(sagaId, customer.customerId());
        
        // Publish success event
        StripeCustomerCreatedEvent event = new StripeCustomerCreatedEvent(sagaId, customer.customerId().getValue());
        crossModuleEventBus.publishEventSynchronously(event);
        
        LoggingConfiguration.logStructured("STRIPE_CUSTOMER_CREATED_PUBLISHED", Map.of(
                "sagaId", sagaId,
                "stripeCustomerId", customer.customerId().getValue()
        ), null);
    }

    private void updateBillingAccount(SagaId sagaId, StripeCustomerId customerId) {
        // Find billing account associated with saga
        Maybe<AbstractSaga<?>> maybeSaga = sagaLoader.apply(sagaId);
        if (maybeSaga.isEmpty()) {
            LoggingConfiguration.logStructured("SAGA_NOT_FOUND", Map.of("sagaId", sagaId), null);
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