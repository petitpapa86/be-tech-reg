package com.bcbs239.regtech.billing.application.policies;

import com.bcbs239.regtech.billing.application.policies.CreateStripeSubscriptionCommandHandler;
import com.bcbs239.regtech.billing.application.policies.CreateStripeSubscriptionCommand;
import com.bcbs239.regtech.billing.domain.billing.BillingAccount;
import com.bcbs239.regtech.billing.domain.subscriptions.Subscription;
import com.bcbs239.regtech.billing.domain.subscriptions.SubscriptionId;
import com.bcbs239.regtech.billing.domain.subscriptions.SubscriptionTier;
import com.bcbs239.regtech.billing.domain.subscriptions.StripeSubscriptionId;
import com.bcbs239.regtech.billing.domain.valueobjects.BillingAccountId;
import com.bcbs239.regtech.billing.domain.valueobjects.StripeCustomerId;
import com.bcbs239.regtech.billing.infrastructure.external.stripe.StripeService;
import com.bcbs239.regtech.billing.infrastructure.external.stripe.StripeSubscription;
import com.bcbs239.regtech.core.events.CrossModuleEventBus;
import com.bcbs239.regtech.core.saga.SagaId;
import com.bcbs239.regtech.core.saga.AbstractSaga;
import com.bcbs239.regtech.core.shared.Maybe;
import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.core.shared.ErrorDetail;
import com.bcbs239.regtech.core.saga.SagaManager;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.function.Function;

import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThat;

public class CreateStripeSubscriptionCommandHandlerTest {

    @Test
    public void handle_successfulFlow_callsSagaManagerProcessEvent() {
        // Mocks
        StripeService stripeService = mock(StripeService.class);
        BillingAccount mockAccount = mock(BillingAccount.class);
        Subscription mockSubscription = mock(Subscription.class);
        CrossModuleEventBus eventBus = mock(CrossModuleEventBus.class);
        SagaManager sagaManager = mock(SagaManager.class);

        // repo/finders
        Function<com.bcbs239.regtech.iam.domain.users.UserId, Maybe<BillingAccount>> billingFinder = id -> Maybe.some(mockAccount);
        Function<BillingAccount, Result<BillingAccountId>> billingSaver = acc -> Result.success(new BillingAccountId("id"));
        Function<BillingAccountId, Function<SubscriptionTier, Maybe<Subscription>>> subscriptionFinder = id -> tier -> Maybe.some(mockSubscription);
        Function<Subscription, Result<SubscriptionId>> subscriptionSaver = s -> Result.success(new SubscriptionId("sub-id"));

        // stripe responses
        StripeSubscription stripeSubscription = mock(StripeSubscription.class);
        when(stripeSubscription.subscriptionId()).thenReturn(new StripeSubscriptionId("stripe-sub-id"));
        when(stripeSubscription.latestInvoiceId()).thenReturn(new com.bcbs239.regtech.billing.domain.valueobjects.StripeInvoiceId("inv-1"));

        when(stripeService.createSubscription(any(), any())).thenReturn(Result.success(stripeSubscription));

        // mock billing account behaviors
        when(mockAccount.getId()).thenReturn(new BillingAccountId("billing-1"));
        when(mockAccount.updateStripeCustomerId(any())).thenReturn(Result.success(null));

        // subscription behaviors
        when(mockSubscription.updateStripeSubscriptionId(any())).thenReturn(Result.success(null));

        // saga loader stub
        Function<SagaId, Maybe<AbstractSaga<?>>> sagaLoader = id -> Maybe.none();

        // handler
        CreateStripeSubscriptionCommandHandler handler = new CreateStripeSubscriptionCommandHandler(
                stripeService,
                billingFinder,
                billingSaver,
                subscriptionFinder,
                subscriptionSaver,
                eventBus,
                sagaLoader,
                sagaManager
        );

        SagaId sagaId = SagaId.generate();
        CreateStripeSubscriptionCommand cmd = new CreateStripeSubscriptionCommand(sagaId, "cust-1", SubscriptionTier.STARTER, "user-1");

        handler.handle(cmd);

        // Verify sagaManager.processEvent called once (or eventBus published if processEvent fails)
        verify(sagaManager, atLeastOnce()).processEvent(any());
    }
}

