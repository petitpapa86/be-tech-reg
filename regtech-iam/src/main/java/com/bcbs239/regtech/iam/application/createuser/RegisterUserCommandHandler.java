package com.bcbs239.regtech.iam.application.createuser;

import com.bcbs239.regtech.core.shared.ErrorDetail;
import com.bcbs239.regtech.core.shared.Maybe;
import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.core.config.LoggingConfiguration;
import com.bcbs239.regtech.iam.domain.users.*;
import com.bcbs239.regtech.iam.infrastructure.database.repositories.JpaUserRepository;
import com.bcbs239.regtech.iam.infrastructure.database.repositories.OutboxEventRepository;
import com.bcbs239.regtech.iam.infrastructure.database.entities.OutboxEventEntity;
import com.bcbs239.regtech.core.events.UserRegisteredEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

/**
 * Command handler for user registration with transactional outbox pattern for reliable event publishing.
 * Saves user and outbox event in the same transaction, then scheduled processor handles publication.
 */
@Component
public class RegisterUserCommandHandler {

    private static final Logger logger = LoggerFactory.getLogger(RegisterUserCommandHandler.class);

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
        long startTime = System.currentTimeMillis();

        try {
            logger.info("Starting user registration", LoggingConfiguration.createStructuredLog("USER_REGISTRATION_START", Map.of(
                "email", command.email(),
                "bankId", command.bankId()
            )));

            // Call the pure function with repository closures and outbox event saving
            Result<RegisterUserResponse> result = registerUser(
                command,
                userRepository.emailLookup(),
                userRepository.userSaver(),
                this::saveUserRegisteredEventToOutbox,
                userRepository.userRoleSaver()
            );

            long duration = System.currentTimeMillis() - startTime;
            if (result.isSuccess()) {
                RegisterUserResponse response = result.getValue().get();
                logger.info("User registration completed successfully", LoggingConfiguration.createStructuredLog("USER_REGISTRATION_SUCCESS", Map.of(
                    "userId", response.userId().getValue(),
                    "correlationId", response.correlationId(),
                    "duration", duration
                )));
            } else {
                ErrorDetail error = result.getError().get();
                LoggingConfiguration.logError("user_registration", "REGISTRATION_FAILED", error.getMessage(), null, Map.of(
                    "errorCode", error.getCode(),
                    "duration", duration
                ));

                logger.error("User registration failed", LoggingConfiguration.createStructuredLog("USER_REGISTRATION_FAILED", Map.of(
                    "errorCode", error.getCode(),
                    "errorMessage", error.getMessage(),
                    "duration", duration
                )));
            }

            return result;

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            LoggingConfiguration.logError("user_registration", "UNEXPECTED_ERROR", e.getMessage(), e, Map.of(
                "email", command.email(),
                "duration", duration
            ));

            logger.error("Unexpected error during user registration", LoggingConfiguration.createStructuredLog("USER_REGISTRATION_ERROR", Map.of(
                "email", command.email(),
                "error", e.getMessage(),
                "duration", duration
            )), e);

            return Result.failure(ErrorDetail.of("UNEXPECTED_ERROR", "An unexpected error occurred", "error.unexpected"));
        }
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

        logger.info("Generated correlation ID for user registration",
                LoggingConfiguration.createStructuredLog("USER_REGISTRATION_CORRELATION_ID", Map.of(
                        "correlationId", correlationId,
                        "email", command.email()
                )));

        try {
            // Step 1: Validate and create email
            logger.debug("Validating email address", LoggingConfiguration.createStructuredLog("USER_REGISTRATION_EMAIL_VALIDATION", Map.of(
                "correlationId", correlationId,
                "email", command.email()
            )));

            Result<Email> emailResult = Email.create(command.email());
            if (emailResult.isFailure()) {
                logger.warn("Email validation failed", LoggingConfiguration.createStructuredLog("USER_REGISTRATION_EMAIL_INVALID", Map.of(
                    "correlationId", correlationId,
                    "email", command.email(),
                    "error", emailResult.getError().get().getMessage()
                )));
                return Result.failure(emailResult.getError().get());
            }
            Email email = emailResult.getValue().get();

            logger.debug("Email validation successful", LoggingConfiguration.createStructuredLog("USER_REGISTRATION_EMAIL_VALID", Map.of(
                "correlationId", correlationId,
                "email", command.email()
            )));

            // Step 2: Check email uniqueness
            logger.debug("Checking email uniqueness", LoggingConfiguration.createStructuredLog("USER_REGISTRATION_EMAIL_UNIQUENESS_CHECK", Map.of(
                "correlationId", correlationId,
                "email", command.email()
            )));

            Maybe<User> existingUser = emailLookup.apply(email);
            if (existingUser.isPresent()) {
                logger.warn("Email already exists", LoggingConfiguration.createStructuredLog("USER_REGISTRATION_EMAIL_EXISTS", Map.of(
                    "correlationId", correlationId,
                    "email", command.email(),
                    "existingUserId", existingUser.getValue().getId().getValue()
                )));
                return Result.failure(ErrorDetail.of("EMAIL_ALREADY_EXISTS",
                    "Email already exists", "error.email.exists"));
            }

            logger.debug("Email is unique", LoggingConfiguration.createStructuredLog("USER_REGISTRATION_EMAIL_UNIQUE", Map.of(
                "correlationId", correlationId,
                "email", command.email()
            )));

            // Step 3: Validate and create password
            logger.debug("Validating password", LoggingConfiguration.createStructuredLog("USER_REGISTRATION_PASSWORD_VALIDATION", Map.of(
                "correlationId", correlationId
            )));

            Result<Password> passwordResult = Password.create(command.password());
            if (passwordResult.isFailure()) {
                logger.warn("Password validation failed", LoggingConfiguration.createStructuredLog("USER_REGISTRATION_PASSWORD_INVALID", Map.of(
                    "correlationId", correlationId,
                    "error", passwordResult.getError().get().getMessage()
                )));
                return Result.failure(passwordResult.getError().get());
            }
            Password password = passwordResult.getValue().get();

            logger.debug("Password validation successful", LoggingConfiguration.createStructuredLog("USER_REGISTRATION_PASSWORD_VALID", Map.of(
                "correlationId", correlationId
            )));

            // Step 4: Validate names using Maybe to avoid nulls
            logger.debug("Validating user names", LoggingConfiguration.createStructuredLog("USER_REGISTRATION_NAME_VALIDATION", Map.of(
                "correlationId", correlationId,
                "firstName", command.firstName(),
                "lastName", command.lastName()
            )));

            com.bcbs239.regtech.core.shared.Maybe<String> maybeFirst = com.bcbs239.regtech.core.shared.ValidationUtils.validateName(command.firstName());
            if (maybeFirst.isEmpty()) {
                logger.warn("First name validation failed", LoggingConfiguration.createStructuredLog("USER_REGISTRATION_FIRST_NAME_INVALID", Map.of(
                    "correlationId", correlationId,
                    "firstName", command.firstName()
                )));
                return Result.failure(ErrorDetail.of("INVALID_FIRST_NAME",
                    "First name is required and cannot be empty", "error.firstName.required"));
            }
            String firstName = maybeFirst.getValue();

            com.bcbs239.regtech.core.shared.Maybe<String> maybeLast = com.bcbs239.regtech.core.shared.ValidationUtils.validateName(command.lastName());
            if (maybeLast.isEmpty()) {
                logger.warn("Last name validation failed", LoggingConfiguration.createStructuredLog("USER_REGISTRATION_LAST_NAME_INVALID", Map.of(
                    "correlationId", correlationId,
                    "lastName", command.lastName()
                )));
                return Result.failure(ErrorDetail.of("INVALID_LAST_NAME",
                    "Last name is required and cannot be empty", "error.lastName.required"));
            }
            String lastName = maybeLast.getValue();

            logger.debug("Name validation successful", LoggingConfiguration.createStructuredLog("USER_REGISTRATION_NAMES_VALID", Map.of(
                "correlationId", correlationId,
                "firstName", firstName,
                "lastName", lastName
            )));

            // Step 5: Create user aggregate with PENDING_PAYMENT status and bank assignment
            logger.debug("Creating user aggregate", LoggingConfiguration.createStructuredLog("USER_REGISTRATION_USER_CREATION", Map.of(
                "correlationId", correlationId,
                "email", command.email(),
                "bankId", command.bankId(),
                "firstName", firstName,
                "lastName", lastName
            )));

            User newUser = User.createWithBank(email, password, firstName, lastName, command.bankId());

            logger.debug("User aggregate created successfully", LoggingConfiguration.createStructuredLog("USER_REGISTRATION_USER_CREATED", Map.of(
                "correlationId", correlationId,
                "userStatus", newUser.getStatus().toString(),
                "bankId", command.bankId()
            )));

            // Step 6: Save user
            logger.debug("Saving user to database", LoggingConfiguration.createStructuredLog("USER_REGISTRATION_USER_SAVE_START", Map.of(
                "correlationId", correlationId,
                "email", command.email()
            )));

            Result<UserId> saveResult = userSaver.apply(newUser);
            if (saveResult.isFailure()) {
                logger.error("User save failed", LoggingConfiguration.createStructuredLog("USER_REGISTRATION_USER_SAVE_FAILED", Map.of(
                    "correlationId", correlationId,
                    "email", command.email(),
                    "error", saveResult.getError().get().getMessage()
                )));
                return Result.failure(saveResult.getError().get());
            }

            UserId userId = saveResult.getValue().get();

            logger.info("User saved successfully", LoggingConfiguration.createStructuredLog("USER_REGISTRATION_USER_SAVED", Map.of(
                "correlationId", correlationId,
                "userId", userId.getValue(),
                "email", command.email()
            )));

            // Step 7: Persist default ADMIN role for new users if a saver was provided
            if (userRoleSaver != null) {
                logger.debug("Assigning default admin role", LoggingConfiguration.createStructuredLog("USER_REGISTRATION_ROLE_ASSIGNMENT_START", Map.of(
                    "correlationId", correlationId,
                    "userId", userId.getValue()
                )));

                com.bcbs239.regtech.iam.domain.users.UserRole adminRole = com.bcbs239.regtech.iam.domain.users.UserRole.create(
                    userId, com.bcbs239.regtech.core.security.authorization.Role.ADMIN, "default-org"
                );
                Result<String> roleSaveResult = userRoleSaver.apply(adminRole);
                if (roleSaveResult.isFailure()) {
                    logger.error("Role assignment failed", LoggingConfiguration.createStructuredLog("USER_REGISTRATION_ROLE_ASSIGNMENT_FAILED", Map.of(
                        "correlationId", correlationId,
                        "userId", userId.getValue(),
                        "role", "ADMIN",
                        "error", roleSaveResult.getError().get().getMessage()
                    )));
                    return Result.failure(roleSaveResult.getError().get());
                }

                logger.debug("Admin role assigned successfully", LoggingConfiguration.createStructuredLog("USER_REGISTRATION_ROLE_ASSIGNED", Map.of(
                    "correlationId", correlationId,
                    "userId", userId.getValue(),
                    "role", "ADMIN",
                    "organization", "default-org"
                )));
            }

            // Step 8: Save user registered event to outbox (transactional outbox pattern)
            logger.debug("Saving user registration event to outbox", LoggingConfiguration.createStructuredLog("USER_REGISTRATION_OUTBOX_START", Map.of(
                "correlationId", correlationId,
                "userId", userId.getValue()
            )));

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
                logger.error("Outbox event save failed", LoggingConfiguration.createStructuredLog("USER_REGISTRATION_OUTBOX_FAILED", Map.of(
                    "correlationId", correlationId,
                    "userId", userId.getValue(),
                    "error", outboxResult.getError().get().getMessage()
                )));
                return Result.failure(outboxResult.getError().get());
            }

            logger.info("User registration event saved to outbox", LoggingConfiguration.createStructuredLog("USER_REGISTRATION_OUTBOX_SUCCESS", Map.of(
                "correlationId", correlationId,
                "userId", userId.getValue(),
                "eventType", "UserRegisteredEvent"
            )));

            // Return success response
            RegisterUserResponse response = new RegisterUserResponse(userId, correlationId);
            logger.info("User registration workflow completed successfully", LoggingConfiguration.createStructuredLog("USER_REGISTRATION_WORKFLOW_SUCCESS", Map.of(
                "correlationId", correlationId,
                "userId", userId.getValue(),
                "email", command.email(),
                "bankId", command.bankId()
            )));

            return Result.success(response);

        } catch (Exception e) {
            logger.error("Unexpected error in user registration workflow", LoggingConfiguration.createStructuredLog("USER_REGISTRATION_WORKFLOW_ERROR", Map.of(
                "correlationId", correlationId,
                "email", command.email(),
                "error", e.getMessage()
            )), e);
            throw e;
        } finally {
            // No MDC cleanup needed - using explicit structured logging instead
        }
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
            logger.debug("Creating UserRegisteredEvent for outbox", LoggingConfiguration.createStructuredLog("USER_REGISTRATION_EVENT_CREATION", Map.of(
                "correlationId", data.correlationId(),
                "userId", data.userId().getValue(),
                "email", data.email()
            )));

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

            logger.debug("Serializing UserRegisteredEvent to JSON", LoggingConfiguration.createStructuredLog("USER_REGISTRATION_EVENT_SERIALIZATION", Map.of(
                "correlationId", data.correlationId(),
                "userId", data.userId().getValue(),
                "eventType", "UserRegisteredEvent"
            )));

            // Serialize event to JSON
            String eventData = objectMapper.writeValueAsString(event);

            logger.debug("Creating OutboxEventEntity", LoggingConfiguration.createStructuredLog("USER_REGISTRATION_OUTBOX_ENTITY_CREATION", Map.of(
                "correlationId", data.correlationId(),
                "userId", data.userId().getValue(),
                "eventType", "UserRegisteredEvent",
                "payloadSize", eventData.length()
            )));

            // Create outbox event entity
            OutboxEventEntity outboxEvent = new OutboxEventEntity(
                "UserRegisteredEvent",
                "User",
                data.userId().getValue(),
                eventData,
                Instant.now()
            );

            logger.debug("Saving OutboxEventEntity to database", LoggingConfiguration.createStructuredLog("USER_REGISTRATION_OUTBOX_SAVE_START", Map.of(
                "correlationId", data.correlationId(),
                "userId", data.userId().getValue(),
                "eventType", "UserRegisteredEvent"
            )));

            // Save to outbox
            Result<String> saveResult = outboxEventRepository.eventSaver().apply(outboxEvent);
            if (saveResult.isFailure()) {
                LoggingConfiguration.logError("user_registration", "OUTBOX_SAVE_FAILED", saveResult.getError().get().getMessage(), null, Map.of(
                    "userId", data.userId().getValue(),
                    "correlationId", data.correlationId()
                ));
                return Result.failure(saveResult.getError().get());
            }

            logger.debug("User registration event saved to outbox successfully", LoggingConfiguration.createStructuredLog("USER_REGISTRATION_OUTBOX_SAVED", Map.of(
                "userId", data.userId().getValue(),
                "correlationId", data.correlationId(),
                "eventType", "UserRegisteredEvent",
                "outboxId", saveResult.getValue().get()
            )));

            return Result.success(null);
        } catch (Exception e) {
            LoggingConfiguration.logError("user_registration", "OUTBOX_SERIALIZATION_FAILED", e.getMessage(), e, Map.of(
                "userId", data.userId().getValue(),
                "correlationId", data.correlationId()
            ));

            logger.error("Failed to save user registration event to outbox", LoggingConfiguration.createStructuredLog("USER_REGISTRATION_OUTBOX_ERROR", Map.of(
                "userId", data.userId().getValue(),
                "correlationId", data.correlationId(),
                "error", e.getMessage()
            )), e);

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