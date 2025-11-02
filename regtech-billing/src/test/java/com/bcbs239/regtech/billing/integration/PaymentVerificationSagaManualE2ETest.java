package com.bcbs239.regtech.billing.integration;

import com.bcbs239.regtech.billing.application.policies.CreateStripeCustomerCommandHandler;
import com.bcbs239.regtech.billing.application.policies.PaymentVerificationSaga;
import com.bcbs239.regtech.billing.domain.billing.PaymentVerificationSagaData;
import com.bcbs239.regtech.billing.infrastructure.external.stripe.StripeCustomer;
import com.bcbs239.regtech.billing.infrastructure.external.stripe.StripeService;
import com.bcbs239.regtech.billing.infrastructure.database.repositories.JpaBillingAccountRepository;
import com.bcbs239.regtech.billing.application.policies.createstripecustomer.CreateStripeCustomerCommand;
import com.bcbs239.regtech.billing.domain.valueobjects.StripeCustomerId;
import com.bcbs239.regtech.core.saga.*;
import com.bcbs239.regtech.core.shared.Maybe;
import com.bcbs239.regtech.core.shared.Result;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class PaymentVerificationSagaManualE2ETest {

    @Test
    public void manual_e2e_flow() throws Exception {
        // In-memory saga store
        ConcurrentHashMap<SagaId, AbstractSaga<?>> store = new ConcurrentHashMap<>();

        Function<AbstractSaga<?>, Result<SagaId>> sagaSaver = saga -> {
            store.put(saga.getId(), saga);
            return Result.success(saga.getId());
        };

        Function<SagaId, Maybe<AbstractSaga<?>>> sagaLoader = id -> {
            var v = store.get(id);
            return v == null ? Maybe.none() : Maybe.some(v);
        };

        // Use a dedicated executor so we can shut it down at the end of the test
        java.util.concurrent.ScheduledExecutorService scheduler = java.util.concurrent.Executors.newSingleThreadScheduledExecutor();
        SagaClosures.TimeoutScheduler timeoutScheduler = SagaClosures.timeoutScheduler(scheduler);

        // Mocks for external dependencies
        StripeService stripeService = Mockito.mock(StripeService.class);
        when(stripeService.createCustomer(any(), any())).thenReturn(Result.success(new StripeCustomer(new StripeCustomerId("cust_test"), "test@example.com", "Test User")));
        when(stripeService.attachPaymentMethod(any(), any())).thenReturn(Result.success(null));
        when(stripeService.setDefaultPaymentMethod(any(), any())).thenReturn(Result.success(null));

        // Create a simple ApplicationEventPublisher that records events for assertions
        java.util.List<Object> publishedEvents = java.util.Collections.synchronizedList(new java.util.ArrayList<>());
        ApplicationEventPublisher simplePublisher = new ApplicationEventPublisher() {
            @Override
            public void publishEvent(Object event) {
                publishedEvents.add(event);
            }

            @Override
            public void publishEvent(org.springframework.context.ApplicationEvent event) {
                publishedEvents.add(event.getSource());
            }
        };

        // CrossModuleEventBus backed by simplePublisher
        com.bcbs239.regtech.core.events.CrossModuleEventBus crossModuleEventBus = new com.bcbs239.regtech.core.events.CrossModuleEventBus(simplePublisher);

        JpaBillingAccountRepository billingAccountRepository = Mockito.mock(JpaBillingAccountRepository.class);
        when(billingAccountRepository.billingAccountFinder()).thenReturn(id -> Maybe.none());
        when(billingAccountRepository.billingAccountSaver()).thenReturn(acc -> Result.success(null));

        // We'll create the CreateStripeCustomerCommandHandler and wire it to a simple ApplicationEventPublisher
        var handler = new CreateStripeCustomerCommandHandler(stripeService, crossModuleEventBus, sagaLoader, billingAccountRepository);

        // CommandDispatcher uses the simplePublisher
        CommandDispatcher commandDispatcher = new CommandDispatcher(simplePublisher);

        // Create saga data and instantiate the saga directly (simulate startSaga behavior)
        PaymentVerificationSagaData data = new PaymentVerificationSagaData();
        data.setUserEmail("test@example.com");
        data.setUserName("Test User");
        data.setPaymentMethodId("pm_123");
        data.setUserId("user-1");

        SagaId sagaId = SagaId.generate();
        PaymentVerificationSaga saga = new PaymentVerificationSaga(sagaId, data, timeoutScheduler);

        // Simulate the SagaStartedEvent being handled to produce commands
        SagaStartedEvent startEvent = new SagaStartedEvent(sagaId, saga.getSagaType(), Instant::now);
        saga.handle(startEvent);

        // Persist saga to in-memory store
        sagaSaver.apply(saga);

        // Dispatch commands produced by the saga to the handler
        for (var cmd : saga.getCommandsToDispatch()) {
            if (cmd instanceof CreateStripeCustomerCommand c) {
                handler.handle(c);
            }
        }

        // Verify that external Stripe calls were attempted
        Mockito.verify(stripeService).createCustomer(any(), any());
        Mockito.verify(stripeService).attachPaymentMethod(any(), any());
        Mockito.verify(stripeService).setDefaultPaymentMethod(any(), any());

        // Check published events captured by simplePublisher
        boolean sawBillingAccountNotFound = publishedEvents.stream().anyMatch(ev -> ev instanceof com.bcbs239.regtech.billing.domain.events.BillingAccountNotFoundEvent);
        boolean sawStripeCustomerCreated = publishedEvents.stream().anyMatch(ev -> ev instanceof com.bcbs239.regtech.billing.domain.events.StripeCustomerCreatedEvent);

        // In this scenario billing account lookup fails so we expect BillingAccountNotFoundEvent and NOT a StripeCustomerCreatedEvent
        assertThat(sawBillingAccountNotFound).isTrue();
        assertThat(sawStripeCustomerCreated).isFalse();

        Maybe<AbstractSaga<?>> maybeSaga = sagaLoader.apply(sagaId);
        assertThat(maybeSaga.isPresent()).isTrue();

        PaymentVerificationSaga loadedSaga = (PaymentVerificationSaga) maybeSaga.getValue();
        assertThat(loadedSaga.getData()).isNotNull();
        assertThat(loadedSaga.getId()).isEqualTo(sagaId);

        // Clean up background scheduler created for the test
        scheduler.shutdownNow();
    }
}
