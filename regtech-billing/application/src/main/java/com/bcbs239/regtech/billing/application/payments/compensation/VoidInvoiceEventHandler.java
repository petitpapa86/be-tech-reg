package com.bcbs239.regtech.billing.application.payments.compensation;

import com.bcbs239.regtech.billing.domain.payments.PaymentService;
import com.bcbs239.regtech.core.domain.logging.ILogger;
import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.Result;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Handles invoice voiding events during saga compensation.
 * Executes asynchronously to void invoices via Stripe API.
 */
@Component
public class VoidInvoiceEventHandler {

    private final ILogger asyncLogger;
    private final PaymentService paymentService;

    public VoidInvoiceEventHandler(ILogger asyncLogger, PaymentService paymentService) {
        this.asyncLogger = asyncLogger;
        this.paymentService = paymentService;
    }

    @EventListener
    @Async("sagaTaskExecutor")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void handle(VoidInvoiceEvent event) {
        asyncLogger.asyncStructuredLog("VOID_INVOICE_COMPENSATION_STARTED", Map.of(
            "sagaId", event.sagaId(),
            "invoiceId", event.stripeInvoiceId(),
            "userId", event.userId(),
            "reason", event.reason()
        ));

        try {
            Result<Void> voidResult = paymentService.voidInvoice(event.stripeInvoiceId());
            
            if (voidResult.isSuccess()) {
                asyncLogger.asyncStructuredLog("INVOICE_VOIDED_SUCCESSFULLY", Map.of(
                    "sagaId", event.sagaId(),
                    "invoiceId", event.stripeInvoiceId()
                ));
            } else {
                asyncLogger.asyncStructuredErrorLog("INVOICE_VOID_FAILED", null, Map.of(
                    "sagaId", event.sagaId(),
                    "invoiceId", event.stripeInvoiceId(),
                    "error", voidResult.getError().map(ErrorDetail::getMessage).orElse("Unknown error")
                ));
            }

        } catch (Exception e) {
            asyncLogger.asyncStructuredErrorLog("INVOICE_VOID_EXCEPTION", e, Map.of(
                "sagaId", event.sagaId(),
                "invoiceId", event.stripeInvoiceId()
            ));
        }
    }
}
