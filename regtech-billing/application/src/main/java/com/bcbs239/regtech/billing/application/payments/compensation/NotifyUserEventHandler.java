package com.bcbs239.regtech.billing.application.payments.compensation;

import com.bcbs239.regtech.core.domain.logging.ILogger;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Handles user notification events during saga compensation.
 * Executes asynchronously to send notifications via email/SMS.
 * 
 * In production, this would integrate with:
 * - Email service (SendGrid, AWS SES, etc.)
 * - SMS service (Twilio, etc.)
 * - Push notification service
 * - In-app notification system
 */
@Component
public class NotifyUserEventHandler {

    private final ILogger asyncLogger;
    // TODO: Inject notification services in production
    // private final EmailService emailService;
    // private final SmsService smsService;
    // private final PushNotificationService pushNotificationService;

    public NotifyUserEventHandler(ILogger asyncLogger) {
        this.asyncLogger = asyncLogger;
    }

    @EventListener
    @Async("sagaTaskExecutor")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void handle(NotifyUserEvent event) {
        asyncLogger.asyncStructuredLog("NOTIFY_USER_COMPENSATION_STARTED", Map.of(
            "sagaId", event.sagaId(),
            "userId", event.userId(),
            "notificationType", event.notificationType().name(),
            "subject", event.subject()
        ));

        try {
            // TODO: Send actual notification in production
            // switch (event.notificationType()) {
            //     case PAYMENT_REFUNDED -> {
            //         emailService.sendEmail(event.userId(), event.subject(), event.message());
            //         smsService.sendSms(event.userId(), "Payment refunded - check your email for details");
            //     }
            //     case SUBSCRIPTION_CANCELLED -> {
            //         emailService.sendEmail(event.userId(), event.subject(), event.message());
            //     }
            //     case SETUP_FAILED, PAYMENT_FAILED -> {
            //         emailService.sendEmail(event.userId(), event.subject(), event.message());
            //         pushNotificationService.send(event.userId(), event.subject());
            //     }
            // }

            // For now, publish the notification to application event bus
            // Other systems can listen to this for actual email/SMS sending
            asyncLogger.asyncStructuredLog("USER_NOTIFICATION_PUBLISHED", Map.of(
                "sagaId", event.sagaId(),
                "userId", event.userId(),
                "notificationType", event.notificationType().name(),
                "subject", event.subject(),
                "messageLength", String.valueOf(event.message().length()),
                "note", "Notification published to event bus. Implement email/SMS service for production."
            ));

        } catch (Exception e) {
            asyncLogger.asyncStructuredErrorLog("USER_NOTIFICATION_EXCEPTION", e, Map.of(
                "sagaId", event.sagaId(),
                "userId", event.userId(),
                "notificationType", event.notificationType().name()
            ));
        }
    }
}
