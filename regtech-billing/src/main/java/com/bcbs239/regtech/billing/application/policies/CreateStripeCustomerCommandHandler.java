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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Railway-Oriented handler with specific failure events for compensation.
 * Each failure type publishes a distinct event that the saga can handle.
 */
@Component("railwayCreateStripeCustomerCommandHandler")
public class CreateStripeCustomerCommandHandler {

    private static final Logger logger = LoggerFactory.getLogger(CreateStripeCustomerCommandHandler.class);

    private final StripeService stripeService;
    private final BillingEventPublisher eventPublisher;
    private final Function<SagaId, AbstractSaga<?>> sagaLoader;
    private final JpaBillingAccountRepository billingAccountRepository;

    public CreateStripeCustomerCommandHandler(
            StripeService stripeService,
            BillingEventPublisher eventPublisher,
            Function<SagaId, AbstractSaga<?>> sagaLoader,
            JpaBillingAccountRepository billingAccountRepository) {
        this.stripeService = stripeService;
        this.eventPublisher = eventPublisher;
        this.sagaLoader = sagaLoader;
        this.billingAccountRepository = billingAccountRepository;
    }

    @EventListener
    @Async("sagaTaskExecutor")
    public void handle(CreateStripeCustomerCommand command) {
        logger.info("CreateStripeCustomerCommandHandler received command for saga: {}", command.getSagaId());
        PaymentMethodId paymentMethodId = new PaymentMethodId(command.getPaymentMethodId());
        SagaId sagaId = command.getSagaId();

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
            eventPublisher.publishEvent(result.getValue().get());
            logger.info("Successfully created Stripe customer for saga: {}", sagaId);
        }
        // Note: Failures already published specific events in each step
    }

    private Result<StripeCustomer> createStripeCustomer(CreateStripeCustomerCommand command, SagaId sagaId) {
        Result<StripeCustomer> result = stripeService.createCustomer(command.getUserEmail(), command.getUserName());

        if (result.isFailure()) {
            logger.error("Failed to create Stripe customer for saga {}: {}", sagaId, result.getError().get().getMessage());
            // Publish specific failure event - no compensation needed (nothing created yet)
            eventPublisher.publishEvent(new StripeCustomerCreationFailedEvent(
                sagaId,
                result.getError().get().getMessage()
            ));
        }

        return result;
    }

    private Result<StripeCustomer> attachPaymentMethod(StripeCustomer customer, PaymentMethodId paymentMethodId, SagaId sagaId) {
        Result<Void> result = stripeService.attachPaymentMethod(customer.customerId(), paymentMethodId);

        if (result.isFailure()) {
            logger.error("Failed to attach payment method for saga {}: {}", sagaId, result.getError().get().getMessage());
            // Publish event to trigger compensation: delete the Stripe customer we just created
            eventPublisher.publishEvent(new PaymentMethodAttachmentFailedEvent(
                sagaId,
                customer.customerId().value(),
                result.getError().get().getMessage()
            ));
            return Result.failure(result.getError().get());
        }

        return Result.success(customer);
    }

    private Result<StripeCustomer> setDefaultPaymentMethod(StripeCustomer customer, PaymentMethodId paymentMethodId, SagaId sagaId) {
        Result<Void> result = stripeService.setDefaultPaymentMethod(customer.customerId(), paymentMethodId);

        if (result.isFailure()) {
            logger.error("Failed to set default payment method for saga {}: {}", sagaId, result.getError().get().getMessage());
            // Publish event to trigger compensation: detach payment method and delete customer
            eventPublisher.publishEvent(new PaymentMethodDefaultFailedEvent(
                sagaId,
                customer.customerId().value(),
                paymentMethodId.value(),
                result.getError().get().getMessage()
            ));
            return Result.failure(result.getError().get());
        }

        return Result.success(customer);
    }

    private Result<CustomerAccountData> findBillingAccount(SagaId sagaId, StripeCustomer customer) {
        PaymentVerificationSaga saga = (PaymentVerificationSaga) sagaLoader.apply(sagaId);
        if (saga == null) {
            logger.error("Saga not found: {}", sagaId);
            // Publish event to trigger compensation: cleanup Stripe customer
            eventPublisher.publishEvent(new SagaNotFoundEvent(
                sagaId,
                customer.customerId().value()
            ));
            return Result.failure(ErrorDetail.of("SAGA_NOT_FOUND", "Saga not found: " + sagaId, "saga.not.found"));
        }

        BillingAccountId billingAccountId = new BillingAccountId(saga.getData().getBillingAccountId());
        Maybe<BillingAccount> accountMaybe = billingAccountRepository.billingAccountFinder().apply(billingAccountId);

        if (accountMaybe.isEmpty()) {
            logger.error("Billing account not found: {}", billingAccountId);
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
            logger.error("Failed to configure billing account for saga {}: {}", sagaId, configureResult.getError().get().getMessage());
            // Publish event to trigger compensation: cleanup Stripe customer
            eventPublisher.publishEvent(new BillingAccountConfigurationFailedEvent(
                sagaId,
                data.billingAccountId.value(),
                data.customer.customerId().value(),
                configureResult.getError().get().getMessage()
            ));
            return Result.failure(configureResult.getError().get());
        }

        Result<BillingAccountId> saveResult = billingAccountRepository.billingAccountSaver().apply(data.account);
        if (saveResult.isFailure()) {
            logger.error("Failed to save billing account for saga {}: {}", sagaId, saveResult.getError().get().getMessage());
            // Publish event to trigger compensation: cleanup Stripe customer
            eventPublisher.publishEvent(new BillingAccountSaveFailedEvent(
                sagaId,
                data.billingAccountId.value(),
                data.customer.customerId().value(),
                saveResult.getError().get().getMessage()
            ));
            return Result.failure(saveResult.getError().get());
        }

        return Result.success(data);
    }

    private record CustomerAccountData(
        StripeCustomer customer,
        BillingAccount account,
        BillingAccountId billingAccountId
    ) {}
}