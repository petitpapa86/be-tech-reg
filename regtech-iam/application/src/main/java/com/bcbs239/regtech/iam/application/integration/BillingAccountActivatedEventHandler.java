package com.bcbs239.regtech.iam.application.integration;

import com.bcbs239.regtech.core.domain.eventprocessing.EventProcessingFailure;
import com.bcbs239.regtech.core.domain.eventprocessing.IEventProcessingFailureRepository;
import com.bcbs239.regtech.core.domain.events.integration.BillingAccountActivatedEvent;
import com.bcbs239.regtech.core.domain.logging.ILogger;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.iam.domain.users.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Event handler for BillingAccountActivatedEvent.
 * Updates user status to ACTIVE and activates user roles when billing account is activated.
 */
@Component("iamBillingAccountActivatedEventHandler")
public class BillingAccountActivatedEventHandler {

    private final UserRepository userRepository;
    private final ILogger logger;
    private final IEventProcessingFailureRepository failureRepository;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @Autowired
    public BillingAccountActivatedEventHandler(
            UserRepository userRepository,
            ILogger logger,
            IEventProcessingFailureRepository failureRepository,
            com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
        this.userRepository = userRepository;
        this.logger = logger;
        this.failureRepository = failureRepository;
        this.objectMapper = objectMapper;
    }

    @EventListener
    public void handle(BillingAccountActivatedEvent event) {

        logger.asyncStructuredLog("BILLING_ACCOUNT_ACTIVATED_EVENT_RECEIVED", Map.of(
                "eventType", "BillingAccountActivatedEvent",
                "userId", event.getUserId()
        ));

        UserId userId = UserId.fromString(event.getUserId());

    // Find the user
    var userMaybe = userRepository.userLoader(userId);
    if (userMaybe.isEmpty()) {
        String errorMsg = "User with ID " + event.getUserId() + " not found";
        logger.asyncStructuredErrorLog("USER_NOT_FOUND", new IllegalStateException(errorMsg), Map.of(
            "eventType", "BillingAccountActivatedEvent",
            "userId", event.getUserId()
        ));

        // Persist a failure record for retry processing
        try {
        String eventPayload = objectMapper.writeValueAsString(event);
        EventProcessingFailure failure = EventProcessingFailure.create(
            event.getClass().getName(),
            eventPayload,
            errorMsg,
            errorMsg,
            Map.of(
                "userId", event.getUserId(),
                "correlationId", event.getCorrelationId() != null ? event.getCorrelationId() : "",
                "eventId", event.getEventId() != null ? event.getEventId() : ""
            ),
            5
        );
        failureRepository.save(failure);
        } catch (Exception saveEx) {
        logger.asyncStructuredErrorLog("EVENT_PROCESSING_FAILURE_SAVE_ERROR", saveEx, Map.of(
            "eventType", "BillingAccountActivatedEvent",
            "userId", event.getUserId()
        ));
        }

        throw new IllegalStateException(errorMsg);
    }

        User user = userMaybe.getValue();

        boolean madeChange = false;

        if (user.getStatus() != UserStatus.ACTIVE) {
            user.activate();
            madeChange = true;
        }

        List<UserRole> userRoles = userRepository.userRolesFinder(userId);
        for (UserRole userRole : userRoles) {
            if (userRole.isActive()) continue;

            userRole.activate();
            madeChange = true;
        Result<String> roleSaveResult = userRepository.userRoleSaver(userRole);
        if (roleSaveResult.isFailure()) {
        String errorMsg = "Failed to save user role: " + roleSaveResult.getError().get().getMessage();
        logger.asyncStructuredErrorLog("USER_ROLE_SAVE_FAILED", new IllegalStateException(errorMsg), Map.of(
            "eventType", "BillingAccountActivatedEvent",
            "userId", event.getUserId(),
            "error", roleSaveResult.getError().get().getMessage()
        ));

        // Persist failure for retry
        try {
            String eventPayload = objectMapper.writeValueAsString(event);
            EventProcessingFailure failure = EventProcessingFailure.create(
                event.getClass().getName(),
                eventPayload,
                errorMsg,
                errorMsg,
                Map.of(
                    "userId", event.getUserId(),
                    "correlationId", event.getCorrelationId() != null ? event.getCorrelationId() : "",
                    "eventId", event.getEventId() != null ? event.getEventId() : ""
                ),
                5
            );
            failureRepository.save(failure);
        } catch (Exception saveEx) {
            logger.asyncStructuredErrorLog("EVENT_PROCESSING_FAILURE_SAVE_ERROR", saveEx, Map.of(
                "eventType", "BillingAccountActivatedEvent",
                "userId", event.getUserId()
            ));
        }

        throw new IllegalStateException(errorMsg);
        }
        }

    Result<UserId> saveResult = userRepository.userSaver(user);
    if (saveResult.isFailure()) {
        String errorMsg = "Failed to save user: " + saveResult.getError().get().getMessage();
        logger.asyncStructuredErrorLog("USER_ACTIVATION_FAILED", new IllegalStateException(errorMsg), Map.of(
            "eventType", "BillingAccountActivatedEvent",
            "userId", event.getUserId(),
            "error", saveResult.getError().get().getMessage()
        ));

        // Persist failure for retry
        try {
        String eventPayload = objectMapper.writeValueAsString(event);
        EventProcessingFailure failure = EventProcessingFailure.create(
            event.getClass().getName(),
            eventPayload,
            errorMsg,
            errorMsg,
            Map.of(
                "userId", event.getUserId(),
                "correlationId", event.getCorrelationId() != null ? event.getCorrelationId() : "",
                "eventId", event.getEventId() != null ? event.getEventId() : ""
            ),
            5
        );
        failureRepository.save(failure);
        } catch (Exception saveEx) {
        logger.asyncStructuredErrorLog("EVENT_PROCESSING_FAILURE_SAVE_ERROR", saveEx, Map.of(
            "eventType", "BillingAccountActivatedEvent",
            "userId", event.getUserId()
        ));
        }

        throw new IllegalStateException(errorMsg);
    }

        if (madeChange) {
            logger.asyncStructuredLog("USER_ACTIVATED_SUCCESSFULLY", Map.of(
                    "eventType", "BillingAccountActivatedEvent",
                    "userId", event.getUserId(),
                    "newStatus", UserStatus.ACTIVE.name()
            ));
        }
    }
}