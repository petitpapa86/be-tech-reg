package com.bcbs239.regtech.iam.application.integration;

import com.bcbs239.regtech.core.events.BillingAccountStatusChangedEvent;
import com.bcbs239.regtech.core.events.SubscriptionCancelledEvent;
import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.iam.domain.users.User;
import com.bcbs239.regtech.iam.domain.users.UserId;
import com.bcbs239.regtech.iam.domain.users.UserRepository;
import com.bcbs239.regtech.iam.domain.users.UserStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.function.Function;

/**
 * Event handler for billing account status changes from the billing context.
 * Updates user status based on billing account changes.
 */
@Component("iamBillingAccountEventHandler")
@Transactional
public class BillingAccountEventHandler {

    private static final Logger logger = LoggerFactory.getLogger(BillingAccountEventHandler.class);

    private final UserRepository userRepository;

    public BillingAccountEventHandler(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Handles BillingAccountStatusChangedEvent by updating user status accordingly.
     */
    @EventListener
    public void handleBillingAccountStatusChanged(BillingAccountStatusChangedEvent event) {
        logger.info("Received BillingAccountStatusChangedEvent for user: {} - {} -> {} with correlation: {}", 
            event.getUserId(), event.getPreviousStatus(), event.getNewStatus(), event.getCorrelationId());

        UserStatus newUserStatus = mapBillingStatusToUserStatus(event.getNewStatus());
        if (newUserStatus == null) {
            logger.debug("No user status change required for billing status: {}", event.getNewStatus());
            return;
        }

        Result<Void> result = updateUserStatusWithClosures(
            UserId.fromString(event.getUserId()),
            newUserStatus,
            userRepository.userLoader(),
            userRepository.userSaver(),
            event.getCorrelationId()
        );

        if (result.isFailure()) {
            logger.error("Failed to update user {} status after billing account change: {}", 
                event.getUserId(), result.getError().get().getMessage());
        } else {
            logger.info("Successfully updated user {} status to {} after billing account change with correlation: {}", 
                event.getUserId(), newUserStatus, event.getCorrelationId());
        }
    }

    /**
     * Handles SubscriptionCancelledEvent by suspending the user account.
     */
    @EventListener
    public void handleSubscriptionCancelled(SubscriptionCancelledEvent event) {
        logger.info("Received SubscriptionCancelledEvent for user: {} with correlation: {}", 
            event.getUserId(), event.getCorrelationId());

        Result<Void> result = updateUserStatusWithClosures(
            UserId.fromString(event.getUserId()),
            UserStatus.CANCELLED,
            userRepository.userLoader(),
            userRepository.userSaver(),
            event.getCorrelationId()
        );

        if (result.isFailure()) {
            logger.error("Failed to cancel user {} after subscription cancellation: {}", 
                event.getUserId(), result.getError().get().getMessage());
        } else {
            logger.info("Successfully cancelled user {} after subscription cancellation with correlation: {}", 
                event.getUserId(), event.getCorrelationId());
        }
    }

    /**
     * Pure function for user status update with closure-based dependencies.
     */
    static Result<Void> updateUserStatusWithClosures(
            UserId userId,
            UserStatus newStatus,
            Function<UserId, Result<User>> userLoader,
            Function<User, Result<UserId>> userSaver,
            String correlationId) {

        // Load the user
        Result<User> userResult = userLoader.apply(userId);
        if (userResult.isFailure()) {
            logger.error("Failed to load user {} for status update: {}", userId, userResult.getError().get().getMessage());
            return Result.failure(userResult.getError().get());
        }

        User user = userResult.getValue().get();
        
        // Check if status change is needed
        if (user.getStatus() == newStatus) {
            logger.debug("User {} already has status {}, no update needed", userId, newStatus);
            return Result.success(null);
        }

        // Update user status based on the new status
        switch (newStatus) {
            case ACTIVE -> user.activate();
            case SUSPENDED -> user.suspend();
            case CANCELLED -> user.cancel();
            case PENDING_PAYMENT -> {
                // This shouldn't happen in normal flow, but handle it gracefully
                logger.warn("Unexpected status change to PENDING_PAYMENT for user {}", userId);
                user.setPendingPayment();
            }
        }

        // Save the updated user
        Result<UserId> saveResult = userSaver.apply(user);
        if (saveResult.isFailure()) {
            logger.error("Failed to save user {} with new status {}: {}", userId, newStatus, saveResult.getError().get().getMessage());
            return Result.failure(saveResult.getError().get());
        }

        logger.debug("User {} status updated to {} successfully with correlation: {}", userId, newStatus, correlationId);
        return Result.success(null);
    }

    /**
     * Maps billing account status to user status.
     */
    private UserStatus mapBillingStatusToUserStatus(String billingStatus) {
        return switch (billingStatus) {
            case "ACTIVE" -> UserStatus.ACTIVE;
            case "SUSPENDED" -> UserStatus.SUSPENDED;
            case "CANCELLED" -> UserStatus.CANCELLED;
            case "PAST_DUE" -> UserStatus.SUSPENDED; // Past due accounts are suspended
            case "PENDING_VERIFICATION" -> null; // No user status change needed
            default -> null;
        };
    }
}