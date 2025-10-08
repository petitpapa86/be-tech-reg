package com.bcbs239.regtech.billing.infrastructure.scheduling;

import com.bcbs239.regtech.billing.domain.aggregates.DunningCase;
import com.bcbs239.regtech.billing.domain.invoices.Invoice;
import com.bcbs239.regtech.billing.domain.valueobjects.*;
import com.bcbs239.regtech.billing.infrastructure.repositories.JpaDunningCaseRepository;
import com.bcbs239.regtech.billing.infrastructure.repositories.JpaInvoiceRepository;
import com.bcbs239.regtech.core.shared.Maybe;
import com.bcbs239.regtech.core.shared.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Test for DunningProcessScheduler dunning automation functionality.
 */
@ExtendWith(MockitoExtension.class)
class DunningProcessSchedulerTest {

    @Mock
    private JpaInvoiceRepository invoiceRepository;

    @Mock
    private JpaDunningCaseRepository dunningCaseRepository;

    @Mock
    private DunningActionExecutor dunningActionExecutor;

    private DunningProcessScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new DunningProcessScheduler(
            invoiceRepository,
            dunningCaseRepository,
            dunningActionExecutor
        );
    }

    @Test
    void shouldCreateDunningCasesForOverdueInvoices() {
        // Given
        Invoice overdueInvoice = createTestOverdueInvoice();
        List<Invoice> overdueInvoices = List.of(overdueInvoice);

        when(invoiceRepository.findOverdueInvoicesWithoutDunningCases())
            .thenReturn(overdueInvoices);
        when(dunningCaseRepository.dunningCaseByInvoiceIdFinder())
            .thenReturn(id -> Maybe.none()); // No existing dunning case
        when(dunningCaseRepository.dunningCaseSaver())
            .thenReturn(dunningCase -> Result.success("dunning-case-123"));

        // When
        DunningProcessScheduler.DunningProcessResult result = scheduler.triggerDunningProcess();

        // Then
        assertNotNull(result);
        assertEquals(1, result.newCasesCreated());
        assertEquals(0, result.actionsExecuted());
        assertEquals(0, result.failures());
        assertFalse(result.hasFailures());

        // Verify dunning case was saved
        verify(dunningCaseRepository.dunningCaseSaver()).apply(any(DunningCase.class));
    }

    @Test
    void shouldSkipInvoicesWithExistingDunningCases() {
        // Given
        Invoice overdueInvoice = createTestOverdueInvoice();
        List<Invoice> overdueInvoices = List.of(overdueInvoice);
        DunningCase existingCase = createTestDunningCase();

        when(invoiceRepository.findOverdueInvoicesWithoutDunningCases())
            .thenReturn(overdueInvoices);
        when(dunningCaseRepository.dunningCaseByInvoiceIdFinder())
            .thenReturn(id -> Maybe.some(existingCase)); // Existing dunning case

        // When
        DunningProcessScheduler.DunningProcessResult result = scheduler.triggerDunningProcess();

        // Then
        assertNotNull(result);
        assertEquals(0, result.newCasesCreated()); // Should skip existing case
        assertEquals(0, result.actionsExecuted());
        assertEquals(0, result.failures());

        // Verify no new dunning case was saved
        verify(dunningCaseRepository.dunningCaseSaver(), never()).apply(any(DunningCase.class));
    }

    @Test
    void shouldProcessReadyDunningCases() {
        // Given
        DunningCase readyCase = createTestDunningCase();
        List<DunningCase> readyCases = List.of(readyCase);

        when(invoiceRepository.findOverdueInvoicesWithoutDunningCases())
            .thenReturn(List.of()); // No new overdue invoices
        when(dunningCaseRepository.findReadyForAction())
            .thenReturn(readyCases);

        DunningProcessScheduler.DunningActionResult actionResult = 
            new DunningProcessScheduler.DunningActionResult("EMAIL_SENT", "Reminder sent", true);
        when(dunningActionExecutor.executeAction(any(), any(), any()))
            .thenReturn(actionResult);

        // When
        DunningProcessScheduler.DunningProcessResult result = scheduler.triggerDunningProcess();

        // Then
        assertNotNull(result);
        assertEquals(0, result.newCasesCreated());
        assertEquals(1, result.actionsExecuted());
        assertEquals(0, result.failures());

        // Verify action was executed
        verify(dunningActionExecutor).executeAction(
            eq(readyCase.getCurrentStep()),
            eq(readyCase.getInvoiceId()),
            eq(readyCase.getBillingAccountId())
        );
    }

    @Test
    void shouldHandleNoOverdueInvoicesOrReadyCases() {
        // Given
        when(invoiceRepository.findOverdueInvoicesWithoutDunningCases())
            .thenReturn(List.of());
        when(dunningCaseRepository.findReadyForAction())
            .thenReturn(List.of());

        // When
        DunningProcessScheduler.DunningProcessResult result = scheduler.triggerDunningProcess();

        // Then
        assertNotNull(result);
        assertEquals(0, result.newCasesCreated());
        assertEquals(0, result.actionsExecuted());
        assertEquals(0, result.failures());
        assertFalse(result.hasFailures());
    }

    @Test
    void shouldResolveDunningCaseWhenPaymentReceived() {
        // Given
        String invoiceId = "invoice-123";
        String resolutionReason = "Payment received";
        DunningCase activeDunningCase = createTestDunningCase();

        when(dunningCaseRepository.dunningCaseByInvoiceIdFinder())
            .thenReturn(id -> Maybe.some(activeDunningCase));
        when(dunningCaseRepository.dunningCaseSaver())
            .thenReturn(dunningCase -> Result.success("dunning-case-123"));

        // When
        scheduler.resolveDunningCasesForInvoice(invoiceId, resolutionReason);

        // Then
        // Verify the dunning case was resolved and saved
        verify(dunningCaseRepository.dunningCaseSaver()).apply(any(DunningCase.class));
    }

    @Test
    void shouldGetDunningStatistics() {
        // Given
        List<DunningCase> activeCases = List.of(
            createTestDunningCase(),
            createTestDunningCase()
        );
        List<DunningCase> readyCases = List.of(createTestDunningCase());

        when(dunningCaseRepository.findByStatus(DunningCaseStatus.IN_PROGRESS))
            .thenReturn(activeCases);
        when(dunningCaseRepository.findReadyForAction())
            .thenReturn(readyCases);

        // When
        DunningProcessScheduler.DunningStatistics stats = scheduler.getDunningStatistics();

        // Then
        assertNotNull(stats);
        assertEquals(2, stats.totalActiveCases());
        assertEquals(1, stats.casesReadyForAction());
    }

    private Invoice createTestOverdueInvoice() {
        BillingAccountId billingAccountId = BillingAccountId.fromString("test-billing-account").getValue().get();
        StripeInvoiceId stripeInvoiceId = StripeInvoiceId.fromString("in_test").getValue().get();
        Money subscriptionAmount = Money.of(new BigDecimal("500.00"), Currency.getInstance("EUR"));
        Money overageAmount = Money.of(new BigDecimal("250.00"), Currency.getInstance("EUR"));
        BillingPeriod billingPeriod = BillingPeriod.forMonth(java.time.YearMonth.of(2024, 1));

        return Invoice.create(
            billingAccountId,
            stripeInvoiceId,
            subscriptionAmount,
            overageAmount,
            billingPeriod,
            () -> java.time.Instant.now(),
            () -> LocalDate.now().minusDays(10) // Due 10 days ago (overdue)
        ).getValue().get();
    }

    private DunningCase createTestDunningCase() {
        InvoiceId invoiceId = InvoiceId.fromString("test-invoice-123").getValue().get();
        BillingAccountId billingAccountId = BillingAccountId.fromString("test-billing-account").getValue().get();

        return DunningCase.create(invoiceId, billingAccountId);
    }
}