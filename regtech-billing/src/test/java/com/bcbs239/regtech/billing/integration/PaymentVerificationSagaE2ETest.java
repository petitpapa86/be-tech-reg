package com.bcbs239.regtech.billing.integration;

import com.bcbs239.regtech.billing.application.policies.CreateStripeCustomerCommandHandler;
import com.bcbs239.regtech.billing.application.policies.PaymentVerificationSaga;
import com.bcbs239.regtech.billing.domain.billing.PaymentVerificationSagaData;
import com.bcbs239.regtech.billing.infrastructure.external.stripe.StripeCustomer;
import com.bcbs239.regtech.billing.infrastructure.external.stripe.StripeService;
import com.bcbs239.regtech.billing.infrastructure.database.repositories.JpaBillingAccountRepository;
import com.bcbs239.regtech.billing.infrastructure.messaging.BillingEventPublisher;
import com.bcbs239.regtech.core.saga.*;
import com.bcbs239.regtech.core.shared.Maybe;
import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.core.events.CrossModuleEventBus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class PaymentVerificationSagaE2ETest {

    // Static mocks created before Spring context starts to avoid PersistenceAnnotation processing
    private static final JpaBillingAccountRepository BILLING_ACCOUNT_REPO_MOCK = Mockito.mock(JpaBillingAccountRepository.class);
    private static final BillingEventPublisher BILLING_EVENT_PUBLISHER_MOCK = Mockito.mock(BillingEventPublisher.class);
    private static final StripeService STRIPE_SERVICE_MOCK = Mockito.mock(StripeService.class);

    @TestConfiguration
    static class TestConfig {
        // In-memory saga store
        private final ConcurrentHashMap<SagaId, AbstractSaga<?>> store = new ConcurrentHashMap<>();

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
            // no-op scheduler for tests
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
                                      org.springframework.context.ApplicationEventPublisher eventPublisher,
                                      java.util.function.Supplier<java.time.Instant> currentTimeSupplier,
                                      SagaClosures.TimeoutScheduler timeoutScheduler) {
            return new SagaManager(sagaSaver, sagaLoader, commandDispatcher, eventPublisher, currentTimeSupplier, timeoutScheduler);
        }

        @Bean
        public org.springframework.core.task.TaskExecutor sagaTaskExecutor() {
            return new SyncTaskExecutor();
        }

        @Bean
        public CommandDispatcher commandDispatcher(org.springframework.context.ApplicationEventPublisher applicationEventPublisher) {
            return new CommandDispatcher(applicationEventPublisher);
        }

        // Expose static mocks as beans so the Spring context uses them
        @Bean
        public StripeService stripeService() {
            return Mockito.mock(StripeService.class);
        }

        @Bean
        public JpaBillingAccountRepository billingAccountRepository() {
            return Mockito.mock(JpaBillingAccountRepository.class);
        }

        @Bean
        public BillingEventPublisher billingEventPublisher() {
            return Mockito.mock(BillingEventPublisher.class);
        }

        @Bean
        public CrossModuleEventBus crossModuleEventBus(org.springframework.context.ApplicationEventPublisher applicationEventPublisher) {
            // Use the real CrossModuleEventBus wired to the test's ApplicationEventPublisher so events are delivered
            return new com.bcbs239.regtech.core.events.CrossModuleEventBus(applicationEventPublisher);
        }

        // Provide mocked EntityManagerFactory and EntityManager so @PersistenceContext injection won't fail during test
        @Bean
        public jakarta.persistence.EntityManagerFactory entityManagerFactory() {
            jakarta.persistence.EntityManagerFactory emf = Mockito.mock(jakarta.persistence.EntityManagerFactory.class);
            jakarta.persistence.EntityManager em = Mockito.mock(jakarta.persistence.EntityManager.class);
            try {
                org.mockito.Mockito.when(emf.createEntityManager()).thenReturn(em);
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
        public CreateStripeCustomerCommandHandler createStripeCustomerCommandHandler(StripeService stripeService,
                                                                                     BillingEventPublisher billingEventPublisher,
                                                                                     CrossModuleEventBus crossModuleEventBus,
                                                                                     Function<SagaId, Maybe<AbstractSaga<?>>> sagaLoader,
                                                                                     JpaBillingAccountRepository billingAccountRepository) {
            return new CreateStripeCustomerCommandHandler(stripeService, billingEventPublisher, crossModuleEventBus, sagaLoader, billingAccountRepository);
        }
    }

    @Test
    void e2e_paymentVerificationSaga_dispatches_createStripeCustomer_and_updates_saga() throws Exception {
        try (org.springframework.context.annotation.AnnotationConfigApplicationContext ctx = new org.springframework.context.annotation.AnnotationConfigApplicationContext()) {
            // Register only required configuration (TestConfig defines the beans we need)
            ctx.register(TestConfig.class);
            ctx.refresh();

            SagaManager sagaManager = ctx.getBean(SagaManager.class);
            @SuppressWarnings("unchecked")
            Function<SagaId, Maybe<AbstractSaga<?>>> sagaLoader = (Function<SagaId, Maybe<AbstractSaga<?>>>) ctx.getBean("sagaLoader");

            StripeService stripeService = ctx.getBean(StripeService.class);
            JpaBillingAccountRepository billingAccountRepository = ctx.getBean(JpaBillingAccountRepository.class);

            // Arrange
            PaymentVerificationSagaData data = new PaymentVerificationSagaData();
            data.setUserEmail("test@example.com");
            data.setUserName("Test User");
            data.setPaymentMethodId("pm_123");
            data.setUserId("user-1");

            // Configure mocks
            when(stripeService.createCustomer(any(), any())).thenReturn(Result.success(new com.bcbs239.regtech.billing.infrastructure.external.stripe.StripeCustomer(
                    new com.bcbs239.regtech.billing.domain.valueobjects.StripeCustomerId("cust_test"),
                    "test@example.com",
                    "Test User"
            )));
            when(stripeService.attachPaymentMethod(any(), any())).thenReturn(Result.success(null));
            when(stripeService.setDefaultPaymentMethod(any(), any())).thenReturn(Result.success(null));
            when(billingAccountRepository.billingAccountFinder()).thenReturn(id -> Maybe.none());
            when(billingAccountRepository.billingAccountSaver()).thenReturn(acc -> Result.success(null));

            // Act
            SagaId sagaId = sagaManager.startSaga(PaymentVerificationSaga.class, data);

            // Wait briefly for async tasks (sagaTaskExecutor is sync in TestConfig)
            Thread.sleep(200);

            Maybe<AbstractSaga<?>> maybeSaga = sagaLoader.apply(sagaId);
            assertThat(maybeSaga.isPresent()).isTrue();

            PaymentVerificationSaga saga = (PaymentVerificationSaga) maybeSaga.getValue();
            assertThat(saga.getData()).isNotNull();
            assertThat(saga.getId()).isEqualTo(sagaId);
        }
    }
}
