package com.bcbs239.regtech.iam.application.createuser;

import com.bcbs239.regtech.core.shared.ErrorDetail;
import com.bcbs239.regtech.core.shared.Maybe;
import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.iam.domain.users.*;
import com.bcbs239.regtech.iam.infrastructure.database.repositories.JpaUserRepository;
import com.bcbs239.regtech.core.events.UserRegisteredEvent;
import com.bcbs239.regtech.core.events.CrossModuleEventBus;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.function.Function;

/**
 * Command handler for user registration with cross-module event-driven billing integration
 */
@Component
public class RegisterUserCommandHandler {

    private final JpaUserRepository userRepository;
    private final CrossModuleEventBus crossModuleEventBus;

    public RegisterUserCommandHandler(
            JpaUserRepository userRepository,
            CrossModuleEventBus crossModuleEventBus) {
        this.userRepository = userRepository;
        this.crossModuleEventBus = crossModuleEventBus;
    }

    /**
     * Handles the register user command with event-driven billing integration
     */
    public Result<RegisterUserResponse> handle(RegisterUserCommand command) {
        // Call the pure function with repository closures and event publishing
        return registerUser(
            command, 
            userRepository.emailLookup(), 
            userRepository.userSaver(),
            this::publishUserRegisteredEvent
        );
    }

    /**
     * Pure function for user registration with event-driven billing integration
     *
     * @param command The registration command
     * @param emailLookup Function to check if email exists
     * @param userSaver Function to save the user
     * @param eventPublisher Function to publish user registered event
     * @return Result of registration attempt
     */
    static Result<RegisterUserResponse> registerUser(
            RegisterUserCommand command,
            Function<Email, Maybe<User>> emailLookup,
            Function<User, Result<UserId>> userSaver,
            Function<UserRegistrationData, Result<Void>> eventPublisher
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

        // Validate names
        String firstName = validateName(command.firstName(), "firstName");
        if (firstName == null) {
            return Result.failure(ErrorDetail.of("INVALID_FIRST_NAME",
                "First name is required and cannot be empty", "error.firstName.required"));
        }

        String lastName = validateName(command.lastName(), "lastName");
        if (lastName == null) {
            return Result.failure(ErrorDetail.of("INVALID_LAST_NAME",
                "Last name is required and cannot be empty", "error.lastName.required"));
        }

        // Create user aggregate with PENDING_PAYMENT status and bank assignment
        User newUser = User.createWithBank(email, password, firstName, lastName, command.bankId());

        // Save user
        Result<UserId> saveResult = userSaver.apply(newUser);
        if (saveResult.isFailure()) {
            return Result.failure(saveResult.getError().get());
        }

        UserId userId = saveResult.getValue().get();

        // Publish user registered event for billing context to handle
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

        Result<Void> eventResult = eventPublisher.apply(registrationData);
        if (eventResult.isFailure()) {
            // Event publishing failed, but user is saved with PENDING_PAYMENT status
            // The billing integration can be retried later
            return Result.failure(eventResult.getError().get());
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
     * Publish user registered event for billing context to handle via cross-module event bus
     */
    private Result<Void> publishUserRegisteredEvent(UserRegistrationData data) {
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
            
            crossModuleEventBus.publishEvent(event);
            return Result.success(null);
        } catch (Exception e) {
            return Result.failure(ErrorDetail.of("EVENT_PUBLISHING_FAILED", 
                "Failed to publish user registered event: " + e.getMessage(), 
                "event.publishing.failed"));
        }
    }

    /**
     * Data class for user registration information
     */
    private record UserRegistrationData(
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