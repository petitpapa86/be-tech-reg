package com.bcbs239.regtech.billing.infrastructure.jobs;

import com.bcbs239.regtech.billing.application.policies.MonthlyBillingSaga;
import com.bcbs239.regtech.billing.application.policies.MonthlyBillingSagaData;
import com.bcbs239.regtech.billing.domain.billing.BillingAccount;
import com.bcbs239.regtech.billing.domain.subscriptions.Subscription;
import com.bcbs239.regtech.billing.domain.valueobjects.BillingAccountId;
import com.bcbs239.regtech.billing.domain.valueobjects.StripeCustomerId;
import com.bcbs239.regtech.billing.domain.subscriptions.SubscriptionStatus;
import com.bcbs239.regtech.billing.domain.valueobjects.BillingPeriod;
import com.bcbs239.regtech.billing.domain.subscriptions.StripeSubscriptionId;
import com.bcbs239.regtech.billing.domain.subscriptions.SubscriptionTier;
import com.bcbs239.regtech.billing.infrastructure.database.repositories.JpaBillingAccountRepository;
import com.bcbs239.regtech.billing.infrastructure.database.repositories.JpaSubscriptionRepository;
import com.bcbs239.regtech.core.saga.SagaOrchestrator;
import com.bcbs239.regtech.core.saga.SagaResult;
import com.bcbs239.regtech.core.shared.Maybe;
import com.bcbs239.regtech.iam.domain.users.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Currency;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Test for MonthlyBillingScheduler saga orchestration and scheduling functionality.
 */
@ExtendWith(MockitoExtension.class)
class MonthlyBillingSchedulerTest {

    @Mock
    private SagaOrchestrator sagaOrchestrator;

    @Mock
    private MonthlyBillingSaga monthlyBillingSaga;

    @Mock
    private JpaSubscriptionRepository subscriptionRepository;

    @Mock
    private JpaBillingAccountRepository billingAccountRepository;

    private MonthlyBillingScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new MonthlyBillingScheduler(
            sagaOrchestrator,
            monthlyBillingSaga,
            subscriptionRepository,
            billingAccountRepository
        );
    }

    @Test
    void shouldStartSagasForActiveSubscriptions() {
        // Given
        YearMonth billingMonth = YearMonth.of(2024, 1);
        
        // Create test subscription
        Subscription subscription = createTestSubscription();
        List<Subscription> activeSubscriptions = List.of(subscription);
        
        // Create test billing account
        BillingAccount billingAccount = createTestBillingAccount();
        
        // Mock repository calls
        when(subscriptionRepository.findByStatusIn(List.of(SubscriptionStatus.ACTIVE)))
            .thenReturn(activeSubscriptions);
        when(billingAccountRepository.billingAccountFinder())
            .thenReturn(id -> Maybe.some(billingAccount));
        
        // Mock saga orchestrator
        when(sagaOrchestrator.startSaga(eq(monthlyBillingSaga), any(MonthlyBillingSagaData.class)))
            .thenReturn(CompletableFuture.completedFuture(SagaResult.success()));

        // When
        MonthlyBillingScheduler.MonthlyBillingResult result = scheduler.triggerMonthlyBilling(billingMonth);

        // Then
        assertNotNull(result);
        assertEquals(1, result.totalSubscriptions());
        assertEquals(1, result.successfulSagas());
        assertEquals(0, result.failedSagas());
        assertFalse(result.hasFailures());
        assertEquals(1.0, result.getSuccessRate());

        // Verify saga was started with correct correlation ID
        verify(sagaOrchestrator).startSaga(eq(monthlyBillingSaga), any(MonthlyBillingSagaData.class));
    }

    @Test
    void shouldHandleNoActiveSubscriptions() {
        // Given
        YearMonth billingMonth = YearMonth.of(2024, 1);
        
        when(subscriptionRepository.findByStatusIn(List.of(SubscriptionStatus.ACTIVE)))
            .thenReturn(List.of());

        // When
        MonthlyBillingScheduler.MonthlyBillingResult result = scheduler.triggerMonthlyBilling(billingMonth);

        // Then
        assertNotNull(result);
        assertEquals(0, result.totalSubscriptions());
        assertEquals(0, result.successfulSagas());
        assertEquals(0, result.failedSagas());
        assertFalse(result.hasFailures());
        assertEquals(1.0, result.getSuccessRate());

        // Verify no saga was started
        verify(sagaOrchestrator, never()).startSaga(any(), any());
    }

    @Test
    void shouldHandleBillingAccountNotFound() {
        // Given
        YearMonth billingMonth = YearMonth.of(2024, 1);
        
        Subscription subscription = createTestSubscription();
        List<Subscription> activeSubscriptions = List.of(subscription);
        
        when(subscriptionRepository.findByStatusIn(List.of(SubscriptionStatus.ACTIVE)))
            .thenReturn(activeSubscriptions);
        when(billingAccountRepository.billingAccountFinder())
            .thenReturn(id -> Maybe.none()); // Billing account not found

        // When
        MonthlyBillingScheduler.MonthlyBillingResult result = scheduler.triggerMonthlyBilling(billingMonth);

        // Then
        assertNotNull(result);
        assertEquals(1, result.totalSubscriptions());
        assertEquals(0, result.successfulSagas());
        assertEquals(1, result.failedSagas());
        assertTrue(result.hasFailures());
        assertEquals(0.0, result.getSuccessRate());

        // Verify no saga was started
        verify(sagaOrchestrator, never()).startSaga(any(), any());
    }

    @Test
    void shouldGenerateCorrectCorrelationId() {
        // Given
        YearMonth billingMonth = YearMonth.of(2024, 1);
        UserId userId = UserId.fromString("test-user-123");
        BillingPeriod billingPeriod = BillingPeriod.forMonth(billingMonth);
        
        // When
        String correlationId = scheduler.generateCorrelationId(userId, billingPeriod);

        // Then
        assertEquals("test-user-123-2024-01", correlationId);
    }

    private Subscription createTestSubscription() {
        BillingAccountId billingAccountId = BillingAccountId.fromString("test-billing-account").getValue().get();
        StripeSubscriptionId stripeSubscriptionId = StripeSubscriptionId.fromString("sub_test").getValue().get();
        
        return Subscription.create(billingAccountId, stripeSubscriptionId, SubscriptionTier.STARTER);
    }

    private BillingAccount createTestBillingAccount() {
        UserId userId = UserId.fromString("test-user-123");
        StripeCustomerId stripeCustomerId = StripeCustomerId.fromString("cus_test").getValue().get();
        
        return BillingAccount.create(userId, stripeCustomerId);
    }
}
