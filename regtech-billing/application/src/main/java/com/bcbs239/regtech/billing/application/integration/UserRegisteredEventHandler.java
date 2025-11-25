package com.bcbs239.regtech.billing.application.integration;

import com.bcbs239.regtech.billing.domain.payments.PaymentMethodId;
import com.bcbs239.regtech.core.domain.events.integration.UserRegisteredIntegrationEvent;
import com.bcbs239.regtech.billing.application.subscriptions.CreateStripeCustomerCommandHandler;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.core.domain.shared.Maybe;
import com.bcbs239.regtech.billing.domain.accounts.BillingAccountRepository;
import com.bcbs239.regtech.billing.domain.accounts.BillingAccount;
import com.bcbs239.regtech.core.domain.shared.valueobjects.Email;
import com.bcbs239.regtech.core.domain.shared.valueobjects.UserId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class UserRegisteredEventHandler {

    private final CreateStripeCustomerCommandHandler createStripeCustomerCommandHandler;
    private final BillingAccountRepository billingAccountRepository;
    private static final Logger log = LoggerFactory.getLogger(UserRegisteredEventHandler.class);

    public UserRegisteredEventHandler(CreateStripeCustomerCommandHandler createStripeCustomerCommandHandler,
                                      BillingAccountRepository billingAccountRepository) {
        this.createStripeCustomerCommandHandler = createStripeCustomerCommandHandler;
        this.billingAccountRepository = billingAccountRepository;
    }

    @EventListener
    public void handle(BillingUserRegisteredEvent event) {
        log.info("USER_REGISTERED_EVENT_RECEIVED; details={}", Map.of(
            "userId", event.getUserId(),
            "email", event.getEmail()
        ));

        // create a billing account record for new user
        Maybe<BillingAccount> maybeAccount = billingAccountRepository.findByUserId(UserId.fromString(event.getUserId()));
        if (maybeAccount.isEmpty()) {
            BillingAccount account = BillingAccount.createForUser(UserId.fromString(event.getUserId()), event.getEmail());
            Result<com.bcbs239.regtech.billing.domain.accounts.BillingAccountId> saveResult = billingAccountRepository.save(account);
            if (saveResult.isFailure()) {
                log.error("FAILED_TO_CREATE_BILLING_ACCOUNT; details={}", Map.of("userId", event.getUserId()));
            } else {
                log.info("BILLING_ACCOUNT_CREATED; details={}", Map.of("userId", event.getUserId()));
            }
        } else {
            log.info("BILLING_ACCOUNT_ALREADY_EXISTS; details={}", Map.of("userId", event.getUserId()));
        }

        // Trigger stripe customer creation via command handler
        createStripeCustomerCommandHandler.handle(new com.bcbs239.regtech.billing.application.policies.createstripecustomer.CreateStripeCustomerCommand(
            com.bcbs239.regtech.core.domain.saga.SagaId.generate(),
                UserId.fromString(event.getUserId()),
                Email.create(event.getEmail()).value(),
            "", // name unknown at registration
                PaymentMethodId.fromString(event.getPaymentMethodId()).value()
        ));
    }
}
