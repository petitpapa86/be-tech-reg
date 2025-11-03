package com.bcbs239.regtech.billing.application.subscriptions;

import com.bcbs239.regtech.billing.application.policies.createstripecustomer.CreateStripeCustomerCommand;
import com.bcbs239.billing.BillingAccount;
import com.bcbs239.regtech.billing.domain.events.*;
import com.bcbs239.regtech.billing.domain.valueobjects.BillingAccountId;
import com.bcbs239.regtech.billing.domain.valueobjects.PaymentMethodId;
import com.bcbs239.regtech.billing.domain.valueobjects.StripeCustomerId;
import com.bcbs239.regtech.billing.infrastructure.database.repositories.JpaBillingAccountRepository;
import com.bcbs239.regtech.billing.infrastructure.external.stripe.StripeCustomer;
import com.bcbs239.regtech.billing.infrastructure.external.stripe.StripeService;
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
 * Railway-Oriented handler with specific failure events for compensation.
 * Each failure type publishes a distinct event that the saga can handle.
 */
@Component("railwayCreateStripeCustomerCommandHandler")
@SuppressWarnings("unused")
public class CreateStripeCustomerCommandHandler {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(CreateStripeCustomerCommandHandler.class);

    private final StripeService stripeService;
    private final CrossModuleEventBus crossModuleEventBus;
    private final Function<SagaId, Maybe<AbstractSaga<?>>> sagaLoader;
    private final JpaBillingAccountRepository billingAccountRepository;

    @SuppressWarnings("unused")
    @Autowired
    public CreateStripeCustomerCommandHandler(
            StripeService stripeService,
            CrossModuleEventBus crossModuleEventBus,
            Function<SagaId, Maybe<AbstractSaga<?>>> sagaLoader,
            JpaBillingAccountRepository billingAccountRepository) {
        this.stripeService = stripeService;
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
                        .flatMap(data -> configureAndUpdate(data, paymentMethodId, sagaId))
                        .map(data -> new StripeCustomerCreatedEvent(sagaId, data.customer.customerId().value()));

        // Success case
        if (result.isSuccess()) {
            result.getValue().ifPresent(ev -> {
                // Always publish via CrossModuleEventBus (no legacy fallback)
                crossModuleEventBus.publishEventSynchronously(ev);
                LoggingConfiguration.logStructured("STRIPE_CUSTOMER_CREATED_PUBLISHED", Map.of(
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
            return Result.failure(result.getError().orElseThrow());
        }

        return Result.success(customer);
    }

    private Result<CustomerAccountData> findBillingAccount(SagaId sagaId, StripeCustomer customer) {
        Maybe<AbstractSaga<?>> maybeSaga = sagaLoader.apply(sagaId);
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
                return Result.failure(ErrorDetail.of("SAGA_NOT_FOUND", "Saga not found: " + sagaId, "saga.not.found"));
            }
        }

         PaymentVerificationSaga saga = (PaymentVerificationSaga) maybeSaga.getValue();

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
            return Result.failure(ErrorDetail.of("ACCOUNT_NOT_FOUND", "Account not found: " + billingAccountId, "account.not.found"));
        }

        return Result.success(new CustomerAccountData(customer, accountMaybe.getValue(), billingAccountId));
    }

    private Result<CustomerAccountData> configureAndUpdate(CustomerAccountData data, PaymentMethodId paymentMethodId, SagaId sagaId) {
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
            return Result.failure(configureResult.getError().get());
        }

        Result<BillingAccountId> updateResult = billingAccountRepository.billingAccountUpdater().apply(data.account);
        if (updateResult.isFailure()) {
            String errorMsg = updateResult.getError().map(ErrorDetail::getMessage).orElse("Unknown error");
            LoggingConfiguration.logStructured("BILLING_ACCOUNT_UPDATE_FAILED", Map.of(
                    "sagaId", sagaId,
                    "billingAccountId", data.billingAccountId.value(),
                    "stripeCustomerId", data.customer.customerId().value(),
                    "error", errorMsg
            ), null);
            return Result.failure(updateResult.getError().get());
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
