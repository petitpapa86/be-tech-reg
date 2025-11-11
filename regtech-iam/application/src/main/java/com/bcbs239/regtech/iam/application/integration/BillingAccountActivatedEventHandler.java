package com.bcbs239.regtech.iam.application.integration;

import com.bcbs239.regtech.core.domain.events.integration.BillingAccountActivatedEvent;
import com.bcbs239.regtech.core.domain.logging.ILogger;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.iam.domain.users.User;
import com.bcbs239.regtech.iam.domain.users.UserId;
import com.bcbs239.regtech.iam.domain.users.UserRepository;
import com.bcbs239.regtech.iam.domain.users.UserRole;
import com.bcbs239.regtech.iam.domain.users.UserStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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

    @Autowired
    public BillingAccountActivatedEventHandler(UserRepository userRepository, ILogger logger) {
        this.userRepository = userRepository;
        this.logger = logger;
    }

    @EventListener
    @Transactional
    public void handle(BillingAccountActivatedEvent event) {
        try {
            logger.asyncStructuredLog("BILLING_ACCOUNT_ACTIVATED_EVENT_RECEIVED", Map.of(
                "eventType", "BillingAccountActivatedEvent",
                "userId", event.getUserId(),
                "billingAccountId", event.getBillingAccountId(),
                "subscriptionTier", event.getSubscriptionTier(),
                "activatedAt", event.getActivatedAt().toString()
            ));

            UserId userId = UserId.fromString(event.getUserId());

            // Find the user
            var userMaybe = userRepository.userLoader(userId);
            if (userMaybe.isEmpty()) {
                logger.asyncStructuredLog("USER_NOT_FOUND_FOR_BILLING_ACTIVATION", Map.of(
                    "eventType", "BillingAccountActivatedEvent",
                    "userId", event.getUserId(),
                    "error", "User not found"
                ));
                return;
            }

            User user = userMaybe.getValue();

            // Update user status to ACTIVE
            user.activate();

            // Activate user roles
            List<UserRole> userRoles = userRepository.userRolesFinder(userId);
            for (UserRole userRole : userRoles) {
                if (!userRole.isActive()) {
                    userRole.activate();
                    Result<String> roleSaveResult = userRepository.userRoleSaver(userRole);
                    if (roleSaveResult.isFailure()) {
                        logger.asyncStructuredLog("USER_ROLE_ACTIVATION_FAILED", Map.of(
                            "eventType", "BillingAccountActivatedEvent",
                            "userId", event.getUserId(),
                            "roleId", userRole.getId(),
                            "error", roleSaveResult.getError().get().getMessage()
                        ));
                    } else {
                        logger.asyncStructuredLog("USER_ROLE_ACTIVATED", Map.of(
                            "eventType", "BillingAccountActivatedEvent",
                            "userId", event.getUserId(),
                            "roleId", userRole.getId(),
                            "role", userRole.getBcbs239Role().name()
                        ));
                    }
                }
            }

            // Save the updated user
            Result<UserId> saveResult = userRepository.userSaver(user);
            if (saveResult.isFailure()) {
                logger.asyncStructuredLog("USER_ACTIVATION_FAILED", Map.of(
                    "eventType", "BillingAccountActivatedEvent",
                    "userId", event.getUserId(),
                    "error", saveResult.getError().get().getMessage()
                ));
                return;
            }

            logger.asyncStructuredLog("USER_ACTIVATED_SUCCESSFULLY", Map.of(
                "eventType", "BillingAccountActivatedEvent",
                "userId", event.getUserId(),
                "newStatus", UserStatus.ACTIVE.name()
            ));

        } catch (Exception e) {
            logger.asyncStructuredErrorLog("BILLING_ACCOUNT_ACTIVATED_EVENT_PROCESSING_FAILED", e, Map.of(
                "eventType", "BillingAccountActivatedEvent",
                "userId", event.getUserId()
            ));
            throw e; // Re-throw to trigger transaction rollback
        }
    }
}