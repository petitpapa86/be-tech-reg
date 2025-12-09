package com.bcbs239.regtech.iam.application.integration;

import com.bcbs239.regtech.core.domain.eventprocessing.EventProcessingFailure;
import com.bcbs239.regtech.core.domain.eventprocessing.IEventProcessingFailureRepository;
import com.bcbs239.regtech.core.domain.events.integration.BillingAccountActivatedEvent;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.iam.domain.users.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
public class BillingAccountActivatedEventHandler extends com.bcbs239.regtech.core.application.eventprocessing.IntegrationEventHandler<BillingAccountActivatedEvent> {

    private final UserRepository userRepository;
    private final IEventProcessingFailureRepository failureRepository;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;
    private static final Logger log = LoggerFactory.getLogger(BillingAccountActivatedEventHandler.class);

    @Autowired
    public BillingAccountActivatedEventHandler(
            UserRepository userRepository,
            IEventProcessingFailureRepository failureRepository,
            com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
        this.userRepository = userRepository;
        this.failureRepository = failureRepository;
        this.objectMapper = objectMapper;
    }

    @EventListener
    public void handle(BillingAccountActivatedEvent event) {
        handleIntegrationEvent(event, this::processEvent);
    }
    
    private void processEvent(BillingAccountActivatedEvent event) {
        UserId userId = UserId.fromString(event.getUserId());

    // Find the user
    var userMaybe = userRepository.userLoader(userId);
    if (userMaybe.isEmpty()) {
        String errorMsg = "User with ID " + event.getUserId() + " not found";
        log.error("USER_NOT_FOUND; details={}", Map.of(
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
        log.error("EVENT_PROCESSING_FAILURE_SAVE_ERROR; details={}", Map.of(
            "eventType", "BillingAccountActivatedEvent",
            "userId", event.getUserId()
        ), saveEx);
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
        log.error("USER_ROLE_SAVE_FAILED; details={}", Map.of(
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
            log.error("EVENT_PROCESSING_FAILURE_SAVE_ERROR; details={}", Map.of(
                "eventType", "BillingAccountActivatedEvent",
                "userId", event.getUserId()
            ), saveEx);
        }

        throw new IllegalStateException(errorMsg);
        }
        }

    Result<UserId> saveResult = userRepository.userSaver(user);
    if (saveResult.isFailure()) {
        String errorMsg = "Failed to save user: " + saveResult.getError().get().getMessage();
        log.error("USER_ACTIVATION_FAILED; details={}", Map.of(
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
        log.error("EVENT_PROCESSING_FAILURE_SAVE_ERROR; details={}", Map.of(
            "eventType", "BillingAccountActivatedEvent",
            "userId", event.getUserId()
        ), saveEx);
        }

        throw new IllegalStateException(errorMsg);
    }

        if (madeChange) {
            log.info("USER_ACTIVATED_SUCCESSFULLY; details={}", Map.of(
                    "eventType", "BillingAccountActivatedEvent",
                    "userId", event.getUserId(),
                    "newStatus", UserStatus.ACTIVE.name()
            ));
        }
    }
}