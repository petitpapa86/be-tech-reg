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
 * Handles payment refund events during saga compensation.
 * Executes asynchronously to refund payments via Stripe API.
 */
@Component
public class RefundPaymentEventHandler {

    private static final Logger log = LoggerFactory.getLogger(RefundPaymentEventHandler.class);
    private final PaymentService paymentService;

    public RefundPaymentEventHandler(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @EventListener
    @Async("sagaTaskExecutor")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void handle(RefundPaymentEvent event) {
        log.info("REFUND_PAYMENT_COMPENSATION_STARTED; details={}", Map.of(
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
                log.info("PAYMENT_REFUNDED_SUCCESSFULLY; details={}", Map.of(
                    "sagaId", event.sagaId(),
                    "paymentIntentId", event.stripePaymentIntentId(),
                    "refundId", refund.refundId(),
                    "status", refund.status(),
                    "amount", refund.amount()
                ));
            } else {
                log.error("PAYMENT_REFUND_FAILED; details={}", Map.of(
                    "sagaId", event.sagaId(),
                    "paymentIntentId", event.stripePaymentIntentId(),
                    "error", refundResult.getError().map(ErrorDetail::getMessage).orElse("Unknown error")
                ));
            }

        } catch (Exception e) {
            log.error("PAYMENT_REFUND_EXCEPTION; details={}", Map.of(
                "sagaId", event.sagaId(),
                "paymentIntentId", event.stripePaymentIntentId()
            ), e);
        }
    }
}
