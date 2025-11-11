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
 * Handles payment refund events during saga compensation.
 * Executes asynchronously to refund payments via Stripe API.
 */
@Component
public class RefundPaymentEventHandler {

    private final ILogger asyncLogger;
    private final PaymentService paymentService;

    public RefundPaymentEventHandler(ILogger asyncLogger, PaymentService paymentService) {
        this.asyncLogger = asyncLogger;
        this.paymentService = paymentService;
    }

    @EventListener
    @Async("sagaTaskExecutor")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void handle(RefundPaymentEvent event) {
        asyncLogger.asyncStructuredLog("REFUND_PAYMENT_COMPENSATION_STARTED", Map.of(
            "sagaId", event.sagaId(),
            "paymentIntentId", event.stripePaymentIntentId(),
            "userId", event.userId(),
            "reason", event.reason()
        ));

        try {
            Result<PaymentService.RefundResult> refundResult = paymentService.refundPayment(
                event.stripePaymentIntentId(), 
                event.reason()
            );
            
            if (refundResult.isSuccess()) {
                PaymentService.RefundResult refund = refundResult.getValue().get();
                asyncLogger.asyncStructuredLog("PAYMENT_REFUNDED_SUCCESSFULLY", Map.of(
                    "sagaId", event.sagaId(),
                    "paymentIntentId", event.stripePaymentIntentId(),
                    "refundId", refund.refundId(),
                    "status", refund.status(),
                    "amount", refund.amount()
                ));
            } else {
                asyncLogger.asyncStructuredErrorLog("PAYMENT_REFUND_FAILED", null, Map.of(
                    "sagaId", event.sagaId(),
                    "paymentIntentId", event.stripePaymentIntentId(),
                    "error", refundResult.getError().map(ErrorDetail::getMessage).orElse("Unknown error")
                ));
            }

        } catch (Exception e) {
            asyncLogger.asyncStructuredErrorLog("PAYMENT_REFUND_EXCEPTION", e, Map.of(
                "sagaId", event.sagaId(),
                "paymentIntentId", event.stripePaymentIntentId()
            ));
        }
    }
}
