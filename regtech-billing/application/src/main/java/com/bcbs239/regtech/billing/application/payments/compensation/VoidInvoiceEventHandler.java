package com.bcbs239.regtech.billing.application.payments.compensation;

import com.bcbs239.regtech.billing.domain.payments.PaymentService;
import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(VoidInvoiceEventHandler.class);
    private final PaymentService paymentService;

    public VoidInvoiceEventHandler(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @EventListener
    @Async("sagaTaskExecutor")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void handle(VoidInvoiceEvent event) {
        log.info("VOID_INVOICE_COMPENSATION_STARTED; details={}", Map.of(
            "sagaId", event.sagaId(),
            "invoiceId", event.stripeInvoiceId(),
            "userId", event.userId(),
            "reason", event.reason()
        ));

        try {
            Result<Void> voidResult = paymentService.voidInvoice(event.stripeInvoiceId());
            
            if (voidResult.isSuccess()) {
                log.info("INVOICE_VOIDED_SUCCESSFULLY; details={}", Map.of(
                    "sagaId", event.sagaId(),
                    "invoiceId", event.stripeInvoiceId()
                ));
            } else {
                log.error("INVOICE_VOID_FAILED; details={}", Map.of(
                    "sagaId", event.sagaId(),
                    "invoiceId", event.stripeInvoiceId(),
                    "error", voidResult.getError().map(ErrorDetail::getMessage).orElse("Unknown error")
                ));
            }

        } catch (Exception e) {
            log.error("INVOICE_VOID_EXCEPTION; details={}", Map.of(
                "sagaId", event.sagaId(),
                "invoiceId", event.stripeInvoiceId()
            ), e);
        }
    }
}
