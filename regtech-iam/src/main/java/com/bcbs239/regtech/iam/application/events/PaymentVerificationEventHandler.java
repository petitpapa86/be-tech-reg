package com.bcbs239.regtech.iam.application.events;

import com.bcbs239.regtech.core.events.PaymentVerifiedEvent;
import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.iam.domain.users.User;
import com.bcbs239.regtech.iam.domain.users.UserId;
import com.bcbs239.regtech.iam.infrastructure.database.repositories.JpaUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.function.Function;

/**
 * Event handler for payment verification events from the billing context.
 * Activates users when their payment is successfully verified.
 * Now called by IamInboxProcessor instead of being a DomainEventHandler.
 */
@Component("iamPaymentVerificationEventHandler")
public class PaymentVerificationEventHandler {

    private static final Logger logger = LoggerFactory.getLogger(PaymentVerificationEventHandler.class);

    private final JpaUserRepository userRepository;

    public PaymentVerificationEventHandler(JpaUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Handles PaymentVerifiedEvent by activating the user account.
     * Called by IamInboxProcessor for asynchronous processing.
     */
    @Transactional
    public boolean handle(PaymentVerifiedEvent event) {
        logger.info("Handling PaymentVerifiedEvent for user: {} with correlation: {}", event.getUserId(), event.getCorrelationId());

        Result<Void> result = activateUserWithClosures(
            UserId.fromString(event.getUserId()),
            userRepository.userLoader(),
            userRepository.userSaver(),
            event.getCorrelationId()
        );

        if (result.isFailure()) {
            logger.error("Failed to activate user {} after payment verification: {}", event.getUserId(), result.getError().get().getMessage());
            return false;
        }

        logger.info("Successfully activated user {} after payment verification with correlation: {}", event.getUserId(), event.getCorrelationId());
        return true;
    }

    /**
     * Pure function for user activation with closure-based dependencies.
     */
    static Result<Void> activateUserWithClosures(
            UserId userId,
            Function<UserId, Result<User>> userLoader,
            Function<User, Result<UserId>> userSaver,
            String correlationId) {

        // Load the user
        Result<User> userResult = userLoader.apply(userId);
        if (userResult.isFailure()) {
            logger.error("Failed to load user {} for payment activation: {}", userId, userResult.getError().get().getMessage());
            return Result.failure(userResult.getError().get());
        }

        User user = userResult.getValue().get();
        
        // Activate the user
        user.activate();

        // Save the updated user
        Result<UserId> saveResult = userSaver.apply(user);
        if (saveResult.isFailure()) {
            logger.error("Failed to save activated user {}: {}", userId, saveResult.getError().get().getMessage());
            return Result.failure(saveResult.getError().get());
        }

        logger.debug("User {} activated successfully with correlation: {}", userId, correlationId);
        return Result.success(null);
    }
}