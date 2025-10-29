package com.bcbs239.regtech.billing.domain.billing;

import com.bcbs239.regtech.billing.domain.valueobjects.BillingAccountId;
import com.bcbs239.regtech.billing.domain.invoices.StripeInvoiceId;
import com.bcbs239.regtech.billing.domain.invoices.Invoice;
import com.bcbs239.regtech.billing.domain.valueobjects.Money;
import com.bcbs239.regtech.billing.domain.valueobjects.BillingPeriod;
import com.bcbs239.regtech.billing.domain.valueobjects.InvoiceStatus;
import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.core.shared.Maybe;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Currency;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

class InvoiceTest {

    private final Supplier<Instant> testClock = () -> Instant.parse("2024-01-15T10:00:00Z");
    private final Supplier<LocalDate> testDateSupplier = () -> LocalDate.of(2024, 1, 15);

    @Test
    void shouldCreateInvoiceWithLineItems() {
        // Given
        BillingAccountId billingAccountId = BillingAccountId.generate("test");
        StripeInvoiceId stripeInvoiceId = StripeInvoiceId.fromString("in_test123").getValue().get();
        Money subscriptionAmount = Money.of(new BigDecimal("500.00"), Currency.getInstance("EUR"));
        Money overageAmount = Money.of(new BigDecimal("25.00"), Currency.getInstance("EUR"));
        BillingPeriod billingPeriod = BillingPeriod.current();

        // When
        Result<Invoice> result = Invoice.create(Maybe.some(billingAccountId), stripeInvoiceId, subscriptionAmount, 
                                              overageAmount, billingPeriod, testClock, testDateSupplier);

        // Then
        assertTrue(result.isSuccess());
        Invoice invoice = result.getValue().get();
        
        assertNotNull(invoice.getId());
        assertNotNull(invoice.getInvoiceNumber());
        assertEquals(InvoiceStatus.DRAFT, invoice.getStatus());
        assertEquals(subscriptionAmount, invoice.getSubscriptionAmount());
        assertEquals(overageAmount, invoice.getOverageAmount());
        assertEquals(2, invoice.getLineItems().size()); // Subscription + overage
        assertEquals(LocalDate.of(2024, 1, 29), invoice.getDueDate()); // 14 days from test date
    }

    @Test
    void shouldMarkInvoiceAsPaid() {
        // Given
        Invoice invoice = createTestInvoice();
        Instant paidAt = Instant.parse("2024-01-20T12:00:00Z");

        // When
        Result<Void> result = invoice.markAsPaid(paidAt, testClock);

        // Then
        assertTrue(result.isSuccess());
        assertEquals(InvoiceStatus.PAID, invoice.getStatus());
        assertEquals(paidAt, invoice.getPaidAt());
    }

    @Test
    void shouldMarkInvoiceAsOverdue() {
        // Given
        Invoice invoice = createTestInvoice();

        // When
        Result<Void> result = invoice.markAsOverdue(testClock);

        // Then
        assertTrue(result.isSuccess());
        assertEquals(InvoiceStatus.OVERDUE, invoice.getStatus());
    }

    @Test
    void shouldDetectOverdueInvoice() {
        // Given
        Invoice invoice = createTestInvoice();
        
        // When - invoice is not overdue yet (due in 14 days from test date)
        boolean isOverdue = invoice.isOverdue(testDateSupplier);
        
        // Then
        assertFalse(isOverdue);
    }

    @Test
    void shouldDetectOverdueInvoiceWhenPastDueDate() {
        // Given
        Invoice invoice = createTestInvoice();
        Supplier<LocalDate> futureDateSupplier = () -> LocalDate.of(2024, 2, 1); // Past due date
        
        // When
        boolean isOverdue = invoice.isOverdue(futureDateSupplier);
        
        // Then
        assertTrue(isOverdue);
    }

    private Invoice createTestInvoice() {
        BillingAccountId billingAccountId = BillingAccountId.generate("test");
        StripeInvoiceId stripeInvoiceId = StripeInvoiceId.fromString("in_test123").getValue().get();
        Money subscriptionAmount = Money.of(new BigDecimal("500.00"), Currency.getInstance("EUR"));
        Money overageAmount = Money.zero(Currency.getInstance("EUR"));
        BillingPeriod billingPeriod = BillingPeriod.current();

        return Invoice.create(Maybe.some(billingAccountId), stripeInvoiceId, subscriptionAmount, overageAmount, 
                            billingPeriod, testClock, testDateSupplier).getValue().get();
    }
}
