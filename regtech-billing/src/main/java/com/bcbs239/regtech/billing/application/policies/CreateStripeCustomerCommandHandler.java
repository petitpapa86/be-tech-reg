package com.bcbs239.regtech.billing.application.policies;

import com.bcbs239.regtech.billing.application.policies.createstripecustomer.CreateStripeCustomerCommand;
import com.bcbs239.regtech.billing.domain.billing.BillingAccount;
import com.bcbs239.regtech.billing.domain.events.*;
import com.bcbs239.regtech.billing.domain.valueobjects.BillingAccountId;
import com.bcbs239.regtech.billing.domain.valueobjects.PaymentMethodId;
import com.bcbs239.regtech.billing.domain.valueobjects.StripeCustomerId;
import com.bcbs239.regtech.billing.infrastructure.database.repositories.JpaBillingAccountRepository;
import com.bcbs239.regtech.billing.infrastructure.external.stripe.StripeCustomer;
import com.bcbs239.regtech.billing.infrastructure.external.stripe.StripeService;
import com.bcbs239.regtech.billing.infrastructure.messaging.BillingEventPublisher;
import com.bcbs239.regtech.core.saga.AbstractSaga;
import com.bcbs239.regtech.core.saga.SagaId;
import com.bcbs239.regtech.core.shared.ErrorDetail;
import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.core.shared.Maybe;

import java.util.function.Function;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.bcbs239.regtech.core.config.LoggingConfiguration;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Railway-Oriented handler with specific failure events for compensation.
 * Each failure type publishes a distinct event that the saga can handle.
 */
@Component("railwayCreateStripeCustomerCommandHandler")
@SuppressWarnings("unused")
public class CreateStripeCustomerCommandHandler {

    private static final Logger logger = LoggerFactory.getLogger(CreateStripeCustomerCommandHandler.class);

    private final StripeService stripeService;
    private final BillingEventPublisher eventPublisher;
    private final Function<SagaId, Maybe<AbstractSaga<?>>> sagaLoader;
    private final JpaBillingAccountRepository billingAccountRepository;

    @SuppressWarnings("unused")
    public CreateStripeCustomerCommandHandler(
            StripeService stripeService,
            BillingEventPublisher eventPublisher,
            Function<SagaId, Maybe<AbstractSaga<?>>> sagaLoader,
            JpaBillingAccountRepository billingAccountRepository) {
        this.stripeService = stripeService;
        this.eventPublisher = eventPublisher;
        this.sagaLoader = sagaLoader;
        this.billingAccountRepository = billingAccountRepository;
    }

    @EventListener
    @Async("sagaTaskExecutor")
    public void handle(CreateStripeCustomerCommand command) {
        SagaId sagaId = command.getSagaId();
        LoggingConfiguration.logStructured("CREATE_STRIPE_CUSTOMER_COMMAND_RECEIVED", Map.of(
                "sagaId", sagaId,
                "userEmail", command.getUserEmail(),
                "paymentMethodId", command.getPaymentMethodId()
        ), null);
        PaymentMethodId paymentMethodId = new PaymentMethodId(command.getPaymentMethodId());

        // Railway chain with specific failure handling
        Result<StripeCustomerCreatedEvent> result =
                createStripeCustomer(command, sagaId)
                        .flatMap(customer -> attachPaymentMethod(customer, paymentMethodId, sagaId))
                        .flatMap(customer -> setDefaultPaymentMethod(customer, paymentMethodId, sagaId))
                        .flatMap(customer -> findBillingAccount(sagaId, customer))
                        .flatMap(data -> configureAndSave(data, paymentMethodId, sagaId))
                        .map(data -> new StripeCustomerCreatedEvent(sagaId, data.customer.customerId().value()));

        // Success case
        if (result.isSuccess()) {
            result.getValue().ifPresent(ev -> {
                eventPublisher.publishEvent(ev);
                LoggingConfiguration.logStructured("STRIPE_CUSTOMER_CREATED", Map.of(
                        "sagaId", sagaId,
                        "stripeCustomerId", ev.getStripeCustomerId()
                ), null);
            });
        } else {
            LoggingConfiguration.logStructured("CREATE_STRIPE_CUSTOMER_COMMAND_FAILED", Map.of(
                    "sagaId", sagaId,
                    "error", result.getError().map(ErrorDetail::getMessage).orElse("Unknown error")
            ), null);
        }
        // Note: Failures already published specific events in each step
    }

    private Result<StripeCustomer> createStripeCustomer(CreateStripeCustomerCommand command, SagaId sagaId) {
        Result<StripeCustomer> result = stripeService.createCustomer(command.getUserEmail(), command.getUserName());

        if (result.isFailure()) {
            String errorMsg = result.getError().map(ErrorDetail::getMessage).orElse("Unknown error");
            LoggingConfiguration.logStructured("STRIPE_CUSTOMER_CREATION_FAILED", Map.of(
                    "sagaId", sagaId,
                    "error", errorMsg
            ), null);
            // Publish specific failure event - no compensation needed (nothing created yet)
            eventPublisher.publishEvent(new StripeCustomerCreationFailedEvent(
                    sagaId,
                    errorMsg
            ));
        }

        return result;
    }

    private Result<StripeCustomer> attachPaymentMethod(StripeCustomer customer, PaymentMethodId paymentMethodId, SagaId sagaId) {
        Result<Void> result = stripeService.attachPaymentMethod(customer.customerId(), paymentMethodId);

        if (result.isFailure()) {
            String errorMsg = result.getError().map(ErrorDetail::getMessage).orElse("Unknown error");
            LoggingConfiguration.logStructured("PAYMENT_METHOD_ATTACHMENT_FAILED", Map.of(
                    "sagaId", sagaId,
                    "stripeCustomerId", customer.customerId().value(),
                    "error", errorMsg
            ), null);
            // Publish event to trigger compensation: delete the Stripe customer we just created
            eventPublisher.publishEvent(new PaymentMethodAttachmentFailedEvent(
                    sagaId,
                    customer.customerId().value(),
                    errorMsg
            ));
            return Result.failure(result.getError().orElseThrow());
        }

        return Result.success(customer);
    }

    private Result<StripeCustomer> setDefaultPaymentMethod(StripeCustomer customer, PaymentMethodId paymentMethodId, SagaId sagaId) {
        Result<Void> result = stripeService.setDefaultPaymentMethod(customer.customerId(), paymentMethodId);

        if (result.isFailure()) {
            String errorMsg = result.getError().map(ErrorDetail::getMessage).orElse("Unknown error");
            LoggingConfiguration.logStructured("PAYMENT_METHOD_DEFAULT_FAILED", Map.of(
                    "sagaId", sagaId,
                    "stripeCustomerId", customer.customerId().value(),
                    "paymentMethodId", paymentMethodId.value(),
                    "error", errorMsg
            ), null);
            // Publish event to trigger compensation: detach payment method and delete customer
            eventPublisher.publishEvent(new PaymentMethodDefaultFailedEvent(
                    sagaId,
                    customer.customerId().value(),
                    paymentMethodId.value(),
                    errorMsg
            ));
            return Result.failure(result.getError().orElseThrow());
        }

        return Result.success(customer);
    }

    private Result<CustomerAccountData> findBillingAccount(SagaId sagaId, StripeCustomer customer) {
        Maybe<AbstractSaga<?>> maybeSaga = sagaLoader.apply(sagaId);
        // If not found, retry a few times to handle possible race between commit and async handlers
        if (maybeSaga.isEmpty()) {
            LoggingConfiguration.createStructuredLog("SAGA_LOADER_MISS", Map.of(
                "sagaId", sagaId,
                "attempts", 0
            ));
            int maxAttempts = 5;
            for (int attempt = 1; attempt <= maxAttempts && maybeSaga.isEmpty(); attempt++) {
                try {
                    Thread.sleep(50L); // small backoff
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                maybeSaga = sagaLoader.apply(sagaId);
                LoggingConfiguration.createStructuredLog("SAGA_LOADER_RETRY", Map.of(
                    "sagaId", sagaId,
                    "attempt", attempt,
                    "found", maybeSaga.isPresent()
                ));
            }

            if (maybeSaga.isEmpty()) {
                logger.error("Saga not found after retries: {}", sagaId);
                // Publish event to trigger compensation: cleanup Stripe customer
                eventPublisher.publishEvent(new SagaNotFoundEvent(
                        sagaId,
                        customer.customerId().value()
                ));
                return Result.failure(ErrorDetail.of("SAGA_NOT_FOUND", "Saga not found: " + sagaId, "saga.not.found"));
            }
        }

         PaymentVerificationSaga saga = (PaymentVerificationSaga) maybeSaga.getValue();

         // Use billingAccountId from saga data if present, otherwise fall back to userId
         String billingAccountIdValue = saga.getData().getBillingAccountId();
         if (billingAccountIdValue == null) {
             billingAccountIdValue = saga.getData().getUserId();
         }
         BillingAccountId billingAccountId = new BillingAccountId(billingAccountIdValue);
        Maybe<BillingAccount> accountMaybe = billingAccountRepository.billingAccountFinder().apply(billingAccountId);

        if (accountMaybe.isEmpty()) {
            LoggingConfiguration.logStructured("BILLING_ACCOUNT_NOT_FOUND", Map.of(
                    "sagaId", sagaId,
                    "billingAccountId", billingAccountId.value(),
                    "stripeCustomerId", customer.customerId().value()
            ), null);
            // Publish event to trigger compensation: cleanup Stripe customer
            eventPublisher.publishEvent(new BillingAccountNotFoundEvent(
                    sagaId,
                    billingAccountId.value(),
                    customer.customerId().value()
            ));
            return Result.failure(ErrorDetail.of("ACCOUNT_NOT_FOUND", "Account not found: " + billingAccountId, "account.not.found"));
        }

        return Result.success(new CustomerAccountData(customer, accountMaybe.getValue(), billingAccountId));
    }

    private Result<CustomerAccountData> configureAndSave(CustomerAccountData data, PaymentMethodId paymentMethodId, SagaId sagaId) {
        StripeCustomerId stripeCustomerId = new StripeCustomerId(data.customer.customerId().value());

        Result<Void> configureResult = data.account.configureStripeCustomer(paymentMethodId, stripeCustomerId);
        if (configureResult.isFailure()) {
            String errorMsg = configureResult.getError().map(ErrorDetail::getMessage).orElse("Unknown error");
            LoggingConfiguration.logStructured("BILLING_ACCOUNT_CONFIGURATION_FAILED", Map.of(
                    "sagaId", sagaId,
                    "billingAccountId", data.billingAccountId.value(),
                    "stripeCustomerId", data.customer.customerId().value(),
                    "error", errorMsg
            ), null);
            // Publish event to trigger compensation: cleanup Stripe customer
            eventPublisher.publishEvent(new BillingAccountConfigurationFailedEvent(
                    sagaId,
                    data.billingAccountId.value(),
                    data.customer.customerId().value(),
                    errorMsg
            ));
            return Result.failure(configureResult.getError().get());
        }

        Result<BillingAccountId> saveResult = billingAccountRepository.billingAccountSaver().apply(data.account);
        if (saveResult.isFailure()) {
            String errorMsg = saveResult.getError().map(ErrorDetail::getMessage).orElse("Unknown error");
            LoggingConfiguration.logStructured("BILLING_ACCOUNT_SAVE_FAILED", Map.of(
                    "sagaId", sagaId,
                    "billingAccountId", data.billingAccountId.value(),
                    "stripeCustomerId", data.customer.customerId().value(),
                    "error", errorMsg
            ), null);
            // Publish event to trigger compensation: cleanup Stripe customer
            eventPublisher.publishEvent(new BillingAccountSaveFailedEvent(
                    sagaId,
                    data.billingAccountId.value(),
                    data.customer.customerId().value(),
                    errorMsg
            ));
            return Result.failure(saveResult.getError().get());
        }

        return Result.success(data);
    }

    private record CustomerAccountData(
            StripeCustomer customer,
            BillingAccount account,
            BillingAccountId billingAccountId
    ) {
    }

}
