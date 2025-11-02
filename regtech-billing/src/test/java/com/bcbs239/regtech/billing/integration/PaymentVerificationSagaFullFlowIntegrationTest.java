package com.bcbs239.regtech.billing.integration;

import com.bcbs239.regtech.billing.application.policies.CreateStripeCustomerCommandHandler;
import com.bcbs239.regtech.billing.application.policies.PaymentVerificationSaga;
import com.bcbs239.regtech.billing.domain.billing.PaymentVerificationSagaData;
import com.bcbs239.regtech.billing.application.policies.createstripecustomer.CreateStripeCustomerCommand;
import com.bcbs239.regtech.billing.application.policies.CreateStripeSubscriptionCommand;
import com.bcbs239.regtech.billing.infrastructure.external.stripe.StripeCustomer;
import com.bcbs239.regtech.billing.infrastructure.external.stripe.StripeService;
import com.bcbs239.regtech.billing.infrastructure.database.repositories.JpaBillingAccountRepository;
import com.bcbs239.regtech.core.saga.*;
import com.bcbs239.regtech.core.shared.Maybe;
import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.core.events.CrossModuleEventBus;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ApplicationEvent;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Integration test that verifies full saga flow: start PaymentVerificationSaga -> CreateStripeCustomer -> CreateStripeSubscription command dispatched
 */
public class PaymentVerificationSagaFullFlowIntegrationTest {

    @TestConfiguration
    static class TestConfig {
        private final ConcurrentHashMap<SagaId, AbstractSaga<?>> store = new ConcurrentHashMap<>();

        // Collector bean to capture published events during the test
        @Bean
        public EventCollector eventCollector() {
            return new EventCollector();
        }

        @Bean
        public ApplicationListener<ApplicationEvent> globalEventListener(EventCollector collector) {
            return event -> {
                Object payload = event;
                if (event instanceof org.springframework.context.PayloadApplicationEvent<?> pae) {
                    payload = pae.getPayload();
                }
                collector.published.add(payload);
            };
        }

        @Bean
        public Function<AbstractSaga<?>, Result<SagaId>> sagaSaver() {
            return saga -> {
                store.put(saga.getId(), saga);
                return Result.success(saga.getId());
            };
        }

        @Bean
        public Function<SagaId, Maybe<AbstractSaga<?>>> sagaLoader() {
            return id -> {
                var v = store.get(id);
                return v == null ? Maybe.none() : Maybe.some(v);
            };
        }

        @Bean
        public SagaClosures.TimeoutScheduler timeoutScheduler() {
            return SagaClosures.timeoutScheduler(java.util.concurrent.Executors.newSingleThreadScheduledExecutor());
        }

        @Bean
        public java.util.function.Supplier<java.time.Instant> currentTimeSupplier() {
            return java.time.Instant::now;
        }

        @Bean
        public SagaManager sagaManager(Function<AbstractSaga<?>, Result<SagaId>> sagaSaver,
                                      Function<SagaId, Maybe<AbstractSaga<?>>> sagaLoader,
                                      CommandDispatcher commandDispatcher,
                                      ApplicationEventPublisher eventPublisher,
                                      java.util.function.Supplier<java.time.Instant> currentTimeSupplier,
                                      SagaClosures.TimeoutScheduler timeoutScheduler) {
            return new SagaManager(sagaSaver, sagaLoader, commandDispatcher, eventPublisher, currentTimeSupplier, timeoutScheduler);
        }

        @Bean
        public org.springframework.core.task.TaskExecutor sagaTaskExecutor() {
            return new SyncTaskExecutor();
        }

        @Bean
        public CommandDispatcher commandDispatcher(ApplicationEventPublisher applicationEventPublisher) {
            return new CommandDispatcher(applicationEventPublisher);
        }

        @Bean
        public StripeService stripeService() {
            return Mockito.mock(StripeService.class);
        }

        @Bean
        public JpaBillingAccountRepository billingAccountRepository() {
            return Mockito.mock(JpaBillingAccountRepository.class);
        }

        // Provide mocks for JPA infrastructure to satisfy PersistenceAnnotationBeanPostProcessor
        @Bean
        public jakarta.persistence.EntityManagerFactory entityManagerFactory() {
            jakarta.persistence.EntityManagerFactory emf = Mockito.mock(jakarta.persistence.EntityManagerFactory.class);
            jakarta.persistence.EntityManager em = Mockito.mock(jakarta.persistence.EntityManager.class);
            try {
                Mockito.when(emf.createEntityManager()).thenReturn(em);
            } catch (Exception ignored) {
            }
            return emf;
        }

        @Bean
        public jakarta.persistence.EntityManager entityManager() {
            return Mockito.mock(jakarta.persistence.EntityManager.class);
        }

        @Bean
        public org.springframework.transaction.PlatformTransactionManager transactionManager() {
            return Mockito.mock(org.springframework.transaction.PlatformTransactionManager.class);
        }

        @Bean
        public org.springframework.transaction.support.TransactionTemplate transactionTemplate(org.springframework.transaction.PlatformTransactionManager tm) {
            return new org.springframework.transaction.support.TransactionTemplate(tm);
        }

        @Bean
        public CrossModuleEventBus crossModuleEventBus(ApplicationEventPublisher applicationEventPublisher) {
            return new CrossModuleEventBus(applicationEventPublisher);
        }

        @Bean
        public CreateStripeCustomerCommandHandler createStripeCustomerCommandHandler(StripeService stripeService,
                                                                                    CrossModuleEventBus crossModuleEventBus,
                                                                                    Function<SagaId, Maybe<AbstractSaga<?>>> sagaLoader,
                                                                                    JpaBillingAccountRepository billingAccountRepository) {
            return new CreateStripeCustomerCommandHandler(stripeService, crossModuleEventBus, sagaLoader, billingAccountRepository);
        }

        @Bean
        public Object createStripeSubscriptionHandlerMock() {
            // We'll not register the real Spring bean; tests will use a captured ApplicationEventPublisher to verify command
            return new Object();
        }
    }

    // Simple collector used by the test to inspect published events
    public static class EventCollector {
        public final java.util.List<Object> published = java.util.Collections.synchronizedList(new java.util.ArrayList<>());
    }

    @Test
    public void fullFlow_dispatches_CreateStripeSubscription() throws Exception {
        org.springframework.context.annotation.AnnotationConfigApplicationContext ctx = new org.springframework.context.annotation.AnnotationConfigApplicationContext();
        ctx.register(TestConfig.class);
        ctx.refresh();

        SagaManager sagaManager = ctx.getBean(SagaManager.class);
        StripeService stripeService = ctx.getBean(StripeService.class);
        JpaBillingAccountRepository billingAccountRepository = ctx.getBean(JpaBillingAccountRepository.class);
        EventCollector collector = ctx.getBean(EventCollector.class);

        // configure stripe service to return a customer
        when(stripeService.createCustomer(any(), any())).thenReturn(Result.success(new StripeCustomer(new com.bcbs239.regtech.billing.domain.valueobjects.StripeCustomerId("cust_test"), "t@e.com", "Test")));
        when(stripeService.attachPaymentMethod(any(), any())).thenReturn(Result.success(null));
        when(stripeService.setDefaultPaymentMethod(any(), any())).thenReturn(Result.success(null));

        when(billingAccountRepository.billingAccountFinder()).thenReturn(id -> Maybe.none());
        when(billingAccountRepository.billingAccountSaver()).thenReturn(acc -> Result.success(null));

        // Provide a mocked BillingAccount so the handler can configure and update it, allowing the flow to continue
        com.bcbs239.regtech.billing.domain.billing.BillingAccount mockAccount = Mockito.mock(com.bcbs239.regtech.billing.domain.billing.BillingAccount.class);
        // configureStripeCustomer should succeed
        when(mockAccount.configureStripeCustomer(any(), any())).thenReturn(Result.success(null));
        // return the mock from the finder
        when(billingAccountRepository.billingAccountFinder()).thenReturn(id -> Maybe.some(mockAccount));
        // billingAccountUpdater should return a successful BillingAccountId
        when(billingAccountRepository.billingAccountUpdater()).thenReturn(acc -> Result.success(new com.bcbs239.regtech.billing.domain.valueobjects.BillingAccountId("faked-bill-id")));

        // start saga
        PaymentVerificationSagaData data = new PaymentVerificationSagaData();
        data.setUserEmail("t@e.com");
        data.setUserName("Test");
        data.setPaymentMethodId("pm_1");
        data.setUserId("user-1");

        SagaId sagaId = sagaManager.startSaga(PaymentVerificationSaga.class, data);

        // First wait for the handler to publish StripeCustomerCreatedEvent
        long createEvTimeoutMs = 2000;
        long createDeadline = System.nanoTime() + createEvTimeoutMs * 1_000_000L;
        Object stripeCreatedEv = null;
        while (System.nanoTime() < createDeadline) {
            synchronized (collector.published) {
                for (Object ev : collector.published) {
                    if (ev instanceof com.bcbs239.regtech.billing.domain.events.StripeCustomerCreatedEvent) {
                        stripeCreatedEv = ev;
                        break;
                    }
                }
            }
            if (stripeCreatedEv != null) break;
            Thread.sleep(20);
        }

        assertThat(stripeCreatedEv).withFailMessage("Expected StripeCustomerCreatedEvent to be published within %d ms", createEvTimeoutMs).isNotNull();

        // Simulate inbox processing / saga event handling manually to avoid transactional synchronization behavior
        @SuppressWarnings("unchecked")
        Function<SagaId, Maybe<AbstractSaga<?>>> sagaLoader = (Function<SagaId, Maybe<AbstractSaga<?>>>) ctx.getBean("sagaLoader");
        @SuppressWarnings("unchecked")
        Function<AbstractSaga<?>, Result<SagaId>> sagaSaver = (Function<AbstractSaga<?>, Result<SagaId>>) ctx.getBean("sagaSaver");
        CommandDispatcher dispatcher = ctx.getBean(CommandDispatcher.class);

        Maybe<AbstractSaga<?>> maybe = sagaLoader.apply(sagaId);
        assertThat(maybe.isPresent()).isTrue();
        AbstractSaga<?> saga = maybe.getValue();

        // Let the saga process the event
        saga.handle((com.bcbs239.regtech.core.saga.SagaMessage) stripeCreatedEv);

        // Snapshot and persist
        var commandsSnapshot = saga.peekCommandsToDispatch();
        sagaSaver.apply(saga);
        // Consume in-memory commands
        saga.getCommandsToDispatch();

        // Dispatch commands immediately (synchronously) in test
        for (var cmd : commandsSnapshot) {
            dispatcher.dispatchNow(cmd);
        }

        // Wait (poll) up to timeout for the expected command to be published
        long timeoutMs = 2000;
        long deadline = System.nanoTime() + timeoutMs * 1_000_000L;
        Object found = null;
        while (System.nanoTime() < deadline) {
            synchronized (collector.published) {
                for (Object ev : collector.published) {
                    if (ev instanceof CreateStripeSubscriptionCommand) {
                        found = ev;
                        break;
                    }
                }
            }
            if (found != null) break;
            Thread.sleep(20);
        }

        assertThat(found).withFailMessage("Expected CreateStripeSubscriptionCommand to be published within %d ms", timeoutMs).isNotNull();

        // Verify command contents
        CreateStripeSubscriptionCommand cmd = (CreateStripeSubscriptionCommand) found;
        assertThat(cmd.getSagaId()).isEqualTo(sagaId);
        assertThat(cmd.getUserId()).isEqualTo(data.getUserId());
        assertThat(cmd.getStripeCustomerId()).isNotNull();

        ctx.close();
    }
}
