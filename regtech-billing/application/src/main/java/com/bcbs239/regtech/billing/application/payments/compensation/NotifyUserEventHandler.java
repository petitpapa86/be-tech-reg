package com.bcbs239.regtech.billing.application.payments.compensation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(NotifyUserEventHandler.class);
    // TODO: Inject notification services in production
    // private final EmailService emailService;
    // private final SmsService smsService;
    // private final PushNotificationService pushNotificationService;

    public NotifyUserEventHandler() {
    }

    @EventListener
    @Async("sagaTaskExecutor")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void handle(NotifyUserEvent event) {
        log.info("NOTIFY_USER_COMPENSATION_STARTED; details={}", Map.of(
            "sagaId", event.sagaId(),
            "userId", event.userId(),
            "notificationType", event.notificationType().name(),
            "subject", event.subject()
        ));

        try {
            // TODO: Send actual notification in production

            log.info("USER_NOTIFICATION_PUBLISHED; details={}", Map.of(
                "sagaId", event.sagaId(),
                "userId", event.userId(),
                "notificationType", event.notificationType().name(),
                "subject", event.subject(),
                "messageLength", String.valueOf(event.message().length()),
                "note", "Notification published to event bus. Implement email/SMS service for production."
            ));

        } catch (Exception e) {
            log.error("USER_NOTIFICATION_EXCEPTION; details={}", Map.of(
                "sagaId", event.sagaId(),
                "userId", event.userId(),
                "notificationType", event.notificationType().name()
            ), e);
        }
    }
}
