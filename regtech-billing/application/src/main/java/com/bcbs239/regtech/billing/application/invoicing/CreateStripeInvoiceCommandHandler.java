package com.bcbs239.regtech.billing.application.invoicing;

import com.bcbs239.regtech.billing.domain.payments.PaymentService;
import com.bcbs239.regtech.billing.domain.payments.StripeCustomerId;
import com.bcbs239.regtech.billing.domain.repositories.InvoiceRepository;
import com.bcbs239.regtech.core.domain.shared.Result;
import org.springframework.stereotype.Component;

/**
 * Command handler for creating Stripe invoices.
 * Simplified version using PaymentService domain interface.
 */
@Component
public class CreateStripeInvoiceCommandHandler {

    private final InvoiceRepository invoiceRepository;
    private final PaymentService paymentService;

    public CreateStripeInvoiceCommandHandler(
            InvoiceRepository invoiceRepository,
            PaymentService paymentService) {
        this.invoiceRepository = invoiceRepository;
        this.paymentService = paymentService;
    }

    /**
     * Handle invoice creation command
     */
    public Result<CreateStripeInvoiceResponse> handle(CreateStripeInvoiceCommand command) {
        // Convert string to StripeCustomerId
        Result<StripeCustomerId> customerIdResult =
            StripeCustomerId.fromString(command.getCustomerId());
        if (customerIdResult.isFailure()) {
            return Result.failure(customerIdResult.getError().get());
        }
        
        // Create invoice using PaymentService
        PaymentService.InvoiceCreationRequest request = new PaymentService.InvoiceCreationRequest(
            customerIdResult.getValue().get(),
            command.getAmount(),
            command.getDescription()
        );

        Result<PaymentService.InvoiceCreationResult> invoiceResult = paymentService.createInvoice(request);
        if (invoiceResult.isFailure()) {
            return Result.failure(invoiceResult.getError().get());
        }

        PaymentService.InvoiceCreationResult stripeInvoice = invoiceResult.getValue().get();

        // Create domain invoice (simplified - would need proper domain object creation)
        // This would typically involve more complex domain logic
        
        return Result.success(new CreateStripeInvoiceResponse(
            stripeInvoice.invoiceId(),
            stripeInvoice.status(),
            stripeInvoice.amount()
        ));
    }

    // Helper record for response
    public record CreateStripeInvoiceResponse(
        String invoiceId,
        String status,
        String amount
    ) {}
}

