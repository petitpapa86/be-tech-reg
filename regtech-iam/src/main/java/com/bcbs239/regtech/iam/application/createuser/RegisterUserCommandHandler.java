package com.bcbs239.regtech.iam.application.createuser;

import com.bcbs239.regtech.core.shared.ErrorDetail;
import com.bcbs239.regtech.core.shared.Maybe;
import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.iam.domain.users.*;
import com.bcbs239.regtech.iam.infrastructure.database.repositories.JpaUserRepository;
import com.bcbs239.regtech.iam.infrastructure.database.repositories.OutboxEventRepository;
import com.bcbs239.regtech.iam.infrastructure.database.entities.OutboxEventEntity;
import com.bcbs239.regtech.core.events.UserRegisteredEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;
import java.util.function.Function;

/**
 * Command handler for user registration with transactional outbox pattern for reliable event publishing.
 * Saves user and outbox event in the same transaction, then scheduled processor handles publication.
 */
@Component
public class RegisterUserCommandHandler {

    private final JpaUserRepository userRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public RegisterUserCommandHandler(
            JpaUserRepository userRepository,
            OutboxEventRepository outboxEventRepository,
            ObjectMapper objectMapper) {
        this.userRepository = userRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Handles the register user command with transactional outbox pattern
     */
    @Transactional
    public Result<RegisterUserResponse> handle(RegisterUserCommand command) {
        // Call the pure function with repository closures and outbox event saving
        return registerUser(
            command,
            userRepository.emailLookup(),
            userRepository.userSaver(),
            this::saveUserRegisteredEventToOutbox,
            userRepository.userRoleSaver()
        );
    }

    /**
     * Pure function for user registration with transactional outbox pattern
     *
     * @param command The registration command
     * @param emailLookup Function to check if email exists
     * @param userSaver Function to save the user
     * @param eventSaver Function to save user registered event to outbox
     * @return Result of registration attempt
     */
    @Transactional
    static Result<RegisterUserResponse> registerUser(
        RegisterUserCommand command,
        Function<Email, Maybe<User>> emailLookup,
        Function<User, Result<UserId>> userSaver,
        Function<UserRegistrationData, Result<Void>> eventSaver
    ) {
    // Fallback to overload that doesn't persist roles (keeps existing tests working)
    return registerUser(command, emailLookup, userSaver, eventSaver, null);
    }

    @Transactional
    static Result<RegisterUserResponse> registerUser(
        RegisterUserCommand command,
        Function<Email, Maybe<User>> emailLookup,
        Function<User, Result<UserId>> userSaver,
        Function<UserRegistrationData, Result<Void>> eventSaver,
        java.util.function.Function<com.bcbs239.regtech.iam.domain.users.UserRole, Result<String>> userRoleSaver
    ) {
        // Generate correlation ID for saga tracking with user data embedded
        String correlationId = "user-registration-" + UUID.randomUUID().toString();

        // Validate and create email
        Result<Email> emailResult = Email.create(command.email());
        if (emailResult.isFailure()) {
            return Result.failure(emailResult.getError().get());
        }
        Email email = emailResult.getValue().get();

        // Check email uniqueness
        Maybe<User> existingUser = emailLookup.apply(email);
        if (existingUser.isPresent()) {
            return Result.failure(ErrorDetail.of("EMAIL_ALREADY_EXISTS",
                "Email already exists", "error.email.exists"));
        }

        // Validate and create password
        Result<Password> passwordResult = Password.create(command.password());
        if (passwordResult.isFailure()) {
            return Result.failure(passwordResult.getError().get());
        }
        Password password = passwordResult.getValue().get();

        // Validate names using Maybe to avoid nulls
        com.bcbs239.regtech.core.shared.Maybe<String> maybeFirst = com.bcbs239.regtech.core.shared.ValidationUtils.validateName(command.firstName());
        if (maybeFirst.isEmpty()) {
            return Result.failure(ErrorDetail.of("INVALID_FIRST_NAME",
                "First name is required and cannot be empty", "error.firstName.required"));
        }
        String firstName = maybeFirst.getValue();

        com.bcbs239.regtech.core.shared.Maybe<String> maybeLast = com.bcbs239.regtech.core.shared.ValidationUtils.validateName(command.lastName());
        if (maybeLast.isEmpty()) {
            return Result.failure(ErrorDetail.of("INVALID_LAST_NAME",
                "Last name is required and cannot be empty", "error.lastName.required"));
        }
        String lastName = maybeLast.getValue();

        // Create user aggregate with PENDING_PAYMENT status and bank assignment
        User newUser = User.createWithBank(email, password, firstName, lastName, command.bankId());

        // Save user
        Result<UserId> saveResult = userSaver.apply(newUser);
        if (saveResult.isFailure()) {
            return Result.failure(saveResult.getError().get());
        }

        UserId userId = saveResult.getValue().get();

        // Persist default ADMIN role for new users if a saver was provided
        if (userRoleSaver != null) {
            com.bcbs239.regtech.iam.domain.users.UserRole adminRole = com.bcbs239.regtech.iam.domain.users.UserRole.create(
                userId, com.bcbs239.regtech.core.security.authorization.Role.ADMIN, "default-org"
            );
            Result<String> roleSaveResult = userRoleSaver.apply(adminRole);
            if (roleSaveResult.isFailure()) {
                return Result.failure(roleSaveResult.getError().get());
            }
        }

        // Save user registered event to outbox (transactional outbox pattern)
        UserRegistrationData registrationData = new UserRegistrationData(
            userId,
            command.paymentMethodId(),
            correlationId,
            email.value(),
            firstName + " " + lastName,
            command.bankId(),
            command.phone(),
            command.address()
        );

        Result<Void> outboxResult = eventSaver.apply(registrationData);
        if (outboxResult.isFailure()) {
            return Result.failure(outboxResult.getError().get());
        }

        // Return success response
        RegisterUserResponse response = new RegisterUserResponse(userId, correlationId);
        return Result.success(response);
    }

    private static String validateName(String name, String fieldName) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }
        return name.trim();
    }

    /**
     * Save user registered event to outbox using transactional outbox pattern
     */
    private Result<Void> saveUserRegisteredEventToOutbox(UserRegistrationData data) {
        try {
            UserRegisteredEvent event = new UserRegisteredEvent(
                data.userId().getValue(),
                data.email(),
                data.name(),
                data.bankId(),
                data.paymentMethodId(),
                data.phone(),
                data.address() != null ? new UserRegisteredEvent.AddressInfo(
                    data.address().line1(),
                    data.address().line2(),
                    data.address().city(),
                    data.address().state(),
                    data.address().postalCode(),
                    data.address().country()
                ) : null,
                data.correlationId()
            );

            // Serialize event to JSON
            String eventData = objectMapper.writeValueAsString(event);

            // Create outbox event entity
            OutboxEventEntity outboxEvent = new OutboxEventEntity(
                "UserRegisteredEvent",
                "User",
                data.userId().getValue(),
                eventData,
                Instant.now()
            );

            // Save to outbox
            Result<String> saveResult = outboxEventRepository.eventSaver().apply(outboxEvent);
            if (saveResult.isFailure()) {
                return Result.failure(saveResult.getError().get());
            }

            return Result.success(null);
        } catch (Exception e) {
            return Result.failure(ErrorDetail.of("OUTBOX_SAVE_FAILED",
                "Failed to save event to outbox: " + e.getMessage(), "error.outbox.saveFailed"));
        }
    }

    /**
     * Data class for user registration information
     */
    public record UserRegistrationData(
        UserId userId,
        String paymentMethodId,
        String correlationId,
        String email,
        String name,
        String bankId,
        String phone,
        RegisterUserCommand.AddressInfo address
    ) {}
}