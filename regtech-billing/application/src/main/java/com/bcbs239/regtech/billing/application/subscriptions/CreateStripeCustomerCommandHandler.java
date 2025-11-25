package com.bcbs239.regtech.billing.application.subscriptions;

import com.bcbs239.regtech.billing.application.policies.createstripecustomer.CreateStripeCustomerCommand;
import com.bcbs239.regtech.billing.domain.accounts.BillingAccount;
import com.bcbs239.regtech.billing.domain.accounts.BillingAccountId;
import com.bcbs239.regtech.billing.domain.accounts.BillingAccountRepository;
import com.bcbs239.regtech.billing.domain.payments.PaymentMethodId;
import com.bcbs239.regtech.billing.domain.payments.PaymentService;
import com.bcbs239.regtech.billing.domain.payments.PaymentVerificationSagaData;
import com.bcbs239.regtech.billing.domain.payments.StripeCustomerId;
import com.bcbs239.regtech.billing.domain.payments.events.StripeCustomerCreatedEvent;
import com.bcbs239.regtech.billing.domain.payments.events.StripeCustomerCreationFailedEvent;

import com.bcbs239.regtech.core.application.saga.SagaManager;
import com.bcbs239.regtech.core.domain.saga.ISagaRepository;
import com.bcbs239.regtech.core.domain.saga.SagaId;
import com.bcbs239.regtech.core.domain.saga.SagaSnapshot;
import com.bcbs239.regtech.core.domain.shared.Maybe;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.core.domain.shared.valueobjects.UserId;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Command handler for creating Stripe customers.
 * Simplified version using PaymentService domain interface.
 */
@Component()
@SuppressWarnings("unused")
public class CreateStripeCustomerCommandHandler {

    private static final Logger log = LoggerFactory.getLogger(CreateStripeCustomerCommandHandler.class);

    private final PaymentService paymentService;
    private final SagaManager sagaManager;
    private final ISagaRepository sagaRepository;
    private final BillingAccountRepository billingAccountRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @SuppressWarnings("unused")
    public CreateStripeCustomerCommandHandler(
            PaymentService paymentService,
            SagaManager sagaManager,
            ISagaRepository sagaRepository,
            BillingAccountRepository billingAccountRepository) {
        this.paymentService = paymentService;
        this.sagaManager = sagaManager;
        this.sagaRepository = sagaRepository;
        this.billingAccountRepository = billingAccountRepository;
    }

    @EventListener
    @Async("sagaTaskExecutor")
    public void handle(CreateStripeCustomerCommand command) {
        SagaId sagaId = command.sagaId();
        log.info("CREATE_STRIPE_CUSTOMER_COMMAND_RECEIVED; details={}", Map.of(
                "sagaId", sagaId,
                "userEmail", command.getEmail().getValue(),
                "userName", command.getName()
        ));

        // Validate payment method ID
        PaymentMethodId paymentMethodId = command.getPaymentMethodId();
        if (paymentMethodId == null) {
            publishFailureEvent(sagaId, "Payment method ID cannot be null");
            return;
        }

        PaymentService.CustomerCreationRequest request = new PaymentService.CustomerCreationRequest(
            command.getEmail().getValue(),
            command.getName(),
            paymentMethodId
        );

        Result<PaymentService.CustomerCreationResult> result = paymentService.createCustomer(request);
        
        if (result.isFailure()) {
            String errorMsg = result.getError().get().getMessage();
            log.info("STRIPE_CUSTOMER_CREATION_FAILED; details={}", Map.of(
                    "sagaId", sagaId,
                    "error", errorMsg
            ));
            publishFailureEvent(sagaId, errorMsg);
            return;
        }

        PaymentService.CustomerCreationResult customer = result.getValue().get();
        
        // Update billing account with Stripe customer ID and get the account ID
        String billingAccountId = updateBillingAccount(sagaId, customer.customerId());
        
        // Publish success event to saga via SagaManager with billing account ID
        StripeCustomerCreatedEvent event = new StripeCustomerCreatedEvent(
            sagaId, 
            customer.customerId().getValue(),
            billingAccountId
        );
        sagaManager.processEvent(event);
        
        log.info("STRIPE_CUSTOMER_CREATED_PUBLISHED; details={}", Map.of(
                "sagaId", sagaId,
                "stripeCustomerId", customer.customerId().getValue(),
                "billingAccountId", billingAccountId
        ));
    }

    private String updateBillingAccount(SagaId sagaId, StripeCustomerId customerId) {
        // Load saga to get user ID
        Maybe<SagaSnapshot> maybeSaga = sagaRepository.load(sagaId);
        if (maybeSaga.isEmpty()) {
            log.info("SAGA_NOT_FOUND; details={}", Map.of("sagaId", sagaId));
            throw new RuntimeException("Saga not found: " + sagaId);
        }
        
        // Extract saga data
        SagaSnapshot snapshot = maybeSaga.getValue();
        String sagaDataJson = snapshot.getSagaData();
        
        try {
            PaymentVerificationSagaData sagaData = objectMapper.readValue(
                sagaDataJson, 
                PaymentVerificationSagaData.class
            );
            
            String userId = sagaData.getUserId();
            
            // Find billing account by user ID
            Maybe<BillingAccount> maybeAccount = billingAccountRepository.findByUserId(
                UserId.fromString(userId)
            );
            
            if (maybeAccount.isEmpty()) {
                log.info("BILLING_ACCOUNT_NOT_FOUND; details={}", Map.of(
                    "sagaId", sagaId,
                    "userId", userId
                ));
                throw new RuntimeException("Billing account not found for user: " + userId);
            }
            
            // Update billing account with Stripe customer ID
            BillingAccount account = maybeAccount.getValue();
            account.setStripeCustomerId(Maybe.some(customerId));
            Result<BillingAccountId> updateResult = billingAccountRepository.update(account);
            
            if (updateResult.isFailure()) {
                log.info("BILLING_ACCOUNT_UPDATE_FAILED; details={}", Map.of(
                    "sagaId", sagaId,
                    "error", updateResult.getError().get().getMessage()
                ));
                throw new RuntimeException("Failed to update billing account");
            }
            
            String billingAccountId = account.getId().value();
            log.info("BILLING_ACCOUNT_UPDATED; details={}", Map.of(
                "sagaId", sagaId,
                "billingAccountId", billingAccountId,
                "stripeCustomerId", customerId.value()
            ));
            
            return billingAccountId;
            
        } catch (Exception e) {
            log.info("SAGA_DATA_PARSE_ERROR; details={}", Map.of(
                "sagaId", sagaId,
                "error", e.getMessage()
            ));
            throw new RuntimeException("Failed to parse saga data", e);
        }
    }

    private void publishFailureEvent(SagaId sagaId, String errorMessage) {
        StripeCustomerCreationFailedEvent failureEvent = new StripeCustomerCreationFailedEvent(sagaId, errorMessage);
        sagaManager.processEvent(failureEvent);
    }
}
